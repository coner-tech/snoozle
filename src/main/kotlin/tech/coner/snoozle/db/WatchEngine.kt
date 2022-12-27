package tech.coner.snoozle.db

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries

class WatchEngine(
    override val coroutineContext: CoroutineContext,
    private val root: Path
) : CoroutineScope {

    private val mutex = Mutex()
    private var nextScopeId = Int.MIN_VALUE
    private val scopes = mutableMapOf<Token, Scope>()
    private var service: WatchService? = null
    private var pollLoopScope: CoroutineContext? = null
    private var pollLoopJob: Job? = null

    suspend fun createToken(): Token = mutex.withLock {
        val token = Token(nextScopeId++)
        scopes[token] = Scope(
            token = token,
            directoryPatterns = emptyList(),
            recordPatterns = emptyList(),
            directoryWatchKeyEntries = emptyList()
        )
        startService()
        token
    }

    private suspend fun startService() = coroutineScope {
        if (service == null) {
            service = runInterruptible { root.fileSystem.newWatchService() }
            pollLoopJob = launch((Dispatchers.IO + Job()).also { pollLoopScope = it }) {
                pollLoop()
            }
        }
    }

    private suspend fun pollLoop() = coroutineScope {
        while (isActive && service != null) {
            mutex.withLock {
                val watchKey = service?.awaitTake()
                if (!isActive) return@withLock
                watchKey?.pollEvents()?.forEach { event ->
                    when (event.kind()) {
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY,
                        StandardWatchEventKinds.ENTRY_DELETE -> handlePathWatchEvent(
                            takenWatchKey = watchKey,
                            event = event as WatchEvent<Path>
                        )
                        StandardWatchEventKinds.OVERFLOW -> TODO("need to handle overflow case")
                        else -> {
                            TODO("need to handle unknown case")
                        }
                    }
                }
            }
        }
    }

    private suspend fun handlePathWatchEvent(
        takenWatchKey: WatchKey,
        event: WatchEvent<Path>
    ) {
        if (
            event.kind() == StandardWatchEventKinds.ENTRY_CREATE
            && event.context().isDirectory(LinkOption.NOFOLLOW_LINKS)
        ) {
            handleDirectoryCreated(takenWatchKey, event)
        } else if (
            event.kind() == StandardWatchEventKinds.ENTRY_DELETE
            && event.contextIsWatchedDirectory(takenWatchKey)
        ) {
            handleDirectoryDeleted(takenWatchKey, event)
        } else if (event.context().isRegularFile(LinkOption.NOFOLLOW_LINKS)) {
            handleFileEvent(takenWatchKey, event)
        }
    }

    private suspend fun handleDirectoryCreated(
        takenWatchKey: WatchKey,
        event: WatchEvent<Path>
    ) = coroutineScope {
        if (
            event.kind() != StandardWatchEventKinds.ENTRY_CREATE
            && !event.context().exists(LinkOption.NOFOLLOW_LINKS)
        ) {
            // handler was called inappropriately
            return@coroutineScope
        }
        val directoryWatchKeyEntry = findDirectoryWatchKeyEntry(takenWatchKey)
            ?: return@coroutineScope // directory no longer watched, avoid race condition
        val newDirectoryAbsolutePath = directoryWatchKeyEntry.absoluteDirectory.value.resolve(event.context()).asAbsolute()
        newDirectoryAbsolutePath.toString()
            .let { pathAsString ->
                scopes.values.firstOrNull { scope ->
                    scope.directoryPatterns.any { directoryPattern ->
                        directoryPattern.matcher(pathAsString).matches()
                    }
                }
            }
            ?.let { scope ->
                service
                    ?.let { newDirectoryAbsolutePath.register(it, watchEventKinds) }
                    ?.let {
                        scope
                            .copyAndAddDirectoryWatchKeyEntry(
                                directory = newDirectoryAbsolutePath,
                                watchKey = it
                            )
                    }
            }
            ?.also { newScope -> scopes[newScope.token] = newScope }
            ?.also { scanNewWatchedDirectory(newDirectoryAbsolutePath) }
    }

    private suspend fun handleDirectoryDeleted(
        takenWatchKey: WatchKey,
        event: WatchEvent<Path>
    ): Unit = coroutineScope {
        TODO("unregister watchkey")
        TODO("remove directory watch key entry")
        TODO("remove directory watch key entries for subdirectories")
    }

    private suspend fun scanNewWatchedDirectory(newDirectory: Path): Unit = coroutineScope {
        launch {
            /*
            new directories might have new files. by the time we register to watch the new directory,
            it's likely too late for any new watches to receive events about records created
            within the new directory. therefore we intentionally wait for things to settle and
            list/emit events for records that match registered patterns.
            */

            delay(250)
            mutex.withLock {
                val service = service
                if (!isActive || service == null || scopes.isEmpty()) {
                    return@withLock
                }
                newDirectory
                    .listDirectoryEntries()
                    .forEach { newDirectoryEntryCandidate ->
                        val newRecordCandidateAbsolute: AbsolutePath = newDirectory.resolve(newDirectoryEntryCandidate).asAbsolute()
                        val newRecordCandidateRelative: RelativePath = newDirectory.resolve(newDirectoryEntryCandidate).asRelative()
                        val newRecordCandidateRelativeAsString = newRecordCandidateRelative.toString()
                        val newDirectoryWatchKeyEntriesToAdd = mutableMapOf<Token, MutableList<Pair<Path, WatchKey>>>()
                        scopes.values.forEach { scope ->
                            if (newRecordCandidateRelative.value.isDirectory(LinkOption.NOFOLLOW_LINKS)) {
                                if (scope.directoryPatterns.any { it.matcher(newRecordCandidateRelativeAsString).matches() }) {
                                    newRecordCandidateRelative.value.register(service, watchEventKinds)
                                        .also { watchKey ->
                                            newDirectoryWatchKeyEntriesToAdd[scope.token] = (newDirectoryWatchKeyEntriesToAdd[scope.token] ?: mutableListOf())
                                                .apply { add(newRecordCandidateRelative to watchKey) }
                                        }
                                }
                            } else if (newRecordCandidateRelative.value.isRegularFile(LinkOption.NOFOLLOW_LINKS)) {
                                if (scope.recordPatterns.any { it.matcher(newRecordCandidateRelativeAsString).matches() }) {
                                    scope.token.events.emit(Event.Record.Exists(newRecordCandidateRelative))
                                }
                            }
                        }
                        newDirectoryWatchKeyEntriesToAdd.entries.forEach { (token, directoryWatchKeyEntryToAdd) ->
                            directoryWatchKeyEntryToAdd.forEach { (directoryToAdd, watchKey) ->
                                scopes[token]
                                    ?.also { scanNewWatchedDirectory(directoryToAdd) }
                                    ?.copyAndAddDirectoryWatchKeyEntry(directoryToAdd, watchKey)
                                    ?.also { scopes[token] = it }
                            }
                        }
                    }
            }
        }
    }

    private suspend fun handleFileEvent(
        takenWatchKey: WatchKey,
        event: WatchEvent<Path>
    ) = coroutineScope {
        if (
            event.kind() != StandardWatchEventKinds.ENTRY_CREATE
            && event.kind() != StandardWatchEventKinds.ENTRY_MODIFY
            && event.kind() != StandardWatchEventKinds.ENTRY_DELETE
        ) {
            // guard unknown / not handled event kinds
            return@coroutineScope
        }
        val recordCandidateRelativePath = event.findRelativePath(takenWatchKey)
            ?: return@coroutineScope // no watch key in a scope matched taken watch key, ignore
        val recordCandidateRelativePathAsString = recordCandidateRelativePath.toString()
        scopes.values.forEach { scope ->
            scope.recordPatterns.forEach { recordPattern ->
                if (recordPattern.matcher(recordCandidateRelativePathAsString).matches()) {
                    when (event.kind()) {
                        StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY -> Event.Record.Exists(recordCandidateRelativePath)
                        StandardWatchEventKinds.ENTRY_DELETE -> Event.Record.DoesNotExist(recordCandidateRelativePath)
                        else -> null
                    }
                        ?.also { scope.token.events.emit(it) }
                }
            }
        }
    }

    private suspend fun WatchService.awaitTake(timeoutMillis: Long = 10) = coroutineScope {
        withTimeoutOrNull(timeoutMillis) {
            runInterruptible { take() }
        }
    }
    
    private val watchEventKinds = arrayOf(
        StandardWatchEventKinds.ENTRY_CREATE,
        StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_MODIFY
    )

    suspend fun registerDirectoryPattern(
        token: Token,
        directoryPattern: Pattern
    ) {
        mutex.withLock {
            val service = this@WatchEngine.service ?: return@withLock Unit
            scopes[token]
                ?.copyAndAddDirectoryPattern(directoryPattern)
                ?.also { newScope -> scopes[token] = newScope }
            Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val relativePath = dir.relativize(root)
                    if (directoryPattern.matcher(relativePath.toString()).matches()) {
                        val watchKey = dir.register(service, watchEventKinds)
                        scopes[token]
                            ?.copyAndAddDirectoryWatchKeyEntry(dir, watchKey)
                            ?.also { newScope -> scopes[token] = newScope }
                    }
                    // TODO register subdirectories
                    return FileVisitResult.CONTINUE
                }
            })
        }
    }

    suspend fun registerRootDirectory(token: Token) = registerDirectoryPattern(token, StandardPatterns.root)

    suspend fun unregisterDirectoryPattern(
        token: Token,
        directoryPattern: Pattern
    ) {
        mutex.withLock {
            TODO()
        }
    }

    suspend fun registerRecordPattern(
        token: Token,
        recordPattern: Pattern
    ) {
        mutex.withLock {
            scopes[token]
                ?.copyAndAddRecordPattern(recordPattern)
                ?.also { newScope -> scopes[token] = newScope }
        }
    }

    suspend fun unregisterRecordPattern(
        token: Token,
        recordPattern: Pattern
    ) {
        mutex.withLock {
            scopes[token]
                ?.copyAndRemoveRecordPattern(recordPattern)
        }
    }

    suspend fun destroyToken(token: Token) = mutex.withLock {
        val scope = scopes[token]
        scope?.directoryWatchKeyEntries?.forEach { it.watchKey.cancel() }
        scopes.remove(token)
        processScopesForClose()
    }

    private fun processScopesForClose() {
        if (scopes.isEmpty()) {
            pollLoopJob?.cancel()
            pollLoopJob = null
            pollLoopScope?.cancel()
            pollLoopScope = null
            service?.close()
            service = null
            scopes.clear()
        }
    }

    suspend fun shutDown() = coroutineScope {
        mutex.withLock {
            scopes.values.forEach { scope ->
                scope.directoryWatchKeyEntries.forEach { entry ->
                    entry.watchKey.cancel()
                }
            }
            scopes.clear()
            processScopesForClose()
        }
        cancel()
    }

    private fun findDirectoryWatchKeyEntry(takenWatchKey: WatchKey): Scope.DirectoryWatchKeyEntry? {
        return scopes.values.firstNotNullOfOrNull { scope ->
            scope.directoryWatchKeyEntries.firstNotNullOfOrNull { directoryWatchKeyEntry ->
                if (directoryWatchKeyEntry.watchKey == takenWatchKey) {
                    directoryWatchKeyEntry
                } else {
                    null
                }
            }
        }
    }

    private fun WatchEvent<Path>.findRelativePath(takenWatchKey: WatchKey): com.sun.tools.javac.file.RelativePath? {
        return findDirectoryWatchKeyEntry(takenWatchKey)
            ?.absoluteDirectory?.resolve(context())
            ?.toRelativePath()
    }

    private fun WatchEvent<Path>.contextIsWatchedDirectory(takenWatchKey: WatchKey): Boolean {
        val directoryWatchKeyEntry = findDirectoryWatchKeyEntry(takenWatchKey)
        val relativePath = directoryWatchKeyEntry?.absoluteDirectory?.resolve(context())
        return directoryWatchKeyEntry?.watchedSubdirectories?.contains(relativePath) == true
    }

    private data class Scope(
        val token: Token,
        val directoryPatterns: List<Pattern>,
        val recordPatterns: List<Pattern>,
        val directoryWatchKeyEntries: List<DirectoryWatchKeyEntry>
    ) {
        fun copyAndAddDirectoryPattern(directoryPattern: Pattern) = copy(
            directoryPatterns = directoryPatterns
                .toMutableList()
                .apply { add(directoryPattern) }
        )

        fun copyAndAddRecordPattern(recordPattern: Pattern) = copy(
            recordPatterns = recordPatterns
                .toMutableList()
                .apply { add(recordPattern) }
        )

        fun copyAndRemoveRecordPattern(recordPattern: Pattern) = copy(
            recordPatterns = recordPatterns
                .toMutableList()
                .apply { remove(recordPattern) }
        )
        
        fun copyAndAddDirectoryWatchKeyEntry(directory: Path, watchKey: WatchKey) = copy(
            directoryWatchKeyEntries = directoryWatchKeyEntries
                .toMutableList()
                .apply { add(DirectoryWatchKeyEntry(directory, watchKey, emptyList())) }
        )

        fun copyAndAddWatchedSubdirectory(watchedSubdirectory: Path, watchKey: WatchKey)
        = copyAndMutateWatchedSubdirectory(watchedSubdirectory, watchKey) { it.copyAndAddWatchedSubdirectory(watchedSubdirectory) }

        fun copyAndRemoveWatchedSubdirectory(watchedSubdirectory: Path, watchKey: WatchKey)
        = copyAndMutateWatchedSubdirectory(watchedSubdirectory, watchKey) { it.copyAndRemoveWatchedSubdirectory(watchedSubdirectory) }

        private fun copyAndMutateWatchedSubdirectory(
            watchedSubdirectory: Path,
            watchKey: WatchKey,
            block: (DirectoryWatchKeyEntry) -> DirectoryWatchKeyEntry
        ) = copy(
            directoryWatchKeyEntries = directoryWatchKeyEntries
                .toMutableList()
                .let { directoryWatchKeyEntries ->
                    val indexToChange = directoryWatchKeyEntries.indexOfFirst { it.watchKey == watchKey }
                    val directoryWatchKeyEntryToChange = directoryWatchKeyEntries[indexToChange]
                    block(directoryWatchKeyEntryToChange)
                    directoryWatchKeyEntryToChange.copyAndRemoveWatchedSubdirectory(watchedSubdirectory)
                        .let {
                            directoryWatchKeyEntries[indexToChange] = it
                            directoryWatchKeyEntries
                        }
                }
        )

        data class DirectoryWatchKeyEntry(
            val absoluteDirectory: AbsolutePath,
            val relativeDirectory: RelativePath,
            val watchKey: WatchKey,
            val watchedSubdirectories: List<WatchedSubdirectoryEntry>
        ) {
            fun copyAndAddWatchedSubdirectory(watchedSubdirectory: Path) = copy(
                watchedSubdirectories = watchedSubdirectories
                    .toMutableList()
                    .apply { add(watchedSubdirectory) }
            )

            fun copyAndRemoveWatchedSubdirectory(watchedSubdirectory: Path) = copy(
                watchedSubdirectories = watchedSubdirectories
                    .toMutableList()
                    .apply { remove(watchedSubdirectory) }
            )
        }

        data class WatchedSubdirectoryEntry(
            val absolutePath: AbsolutePath,
            val relativePath: RelativePath
        )
    }

    data class Token(private val id: Int) {
        val events = MutableSharedFlow<Event>()
    }

    internal sealed class Event {
        sealed class Record : Event() {
            abstract val record: RelativePath
            data class Exists(override val record: RelativePath) : Record()
            data class DoesNotExist(override val record: RelativePath) : Record()
        }
        object Overflow : Event()
    }

    object StandardPatterns {
        val root: Pattern by lazy { Pattern.compile("^$") }
    }
}