package tech.coner.snoozle.db

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
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

open class FileWatchEngine(
    override val coroutineContext: CoroutineContext,
    private val root: AbsolutePath
) : CoroutineScope {

    private val mutex = Mutex()
    protected var nextScopeId = Int.MIN_VALUE
    protected val scopes = mutableMapOf<TokenImpl, ScopeImpl>()
    protected var service: WatchService? = null
    protected var pollLoopScope: CoroutineContext? = null
    protected var pollLoopJob: Job? = null

    suspend fun createToken(): Token = mutex.withLock {
        val token = TokenImpl(nextScopeId++)
            .apply { engine = this@FileWatchEngine }
        scopes[token] = ScopeImpl(
            token = token,
            directoryPatterns = emptyList(),
            filePatterns = emptyList(),
            directoryWatchKeyEntries = emptyList()
        )
        startService()
        token
    }

    private suspend fun startService() = coroutineScope {
        if (service == null) {
            service = runInterruptible { root.value.fileSystem.newWatchService() }
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
                    launch {
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
    }

    private suspend fun handlePathWatchEvent(
        takenWatchKey: WatchKey,
        event: WatchEvent<Path>
    ) {
        val directoryWatchKeyEntries = findDirectoryWatchKeyEntries(takenWatchKey)
        val firstDirectoryWatchKeyEntry = directoryWatchKeyEntries.firstOrNull()
            ?: return // can't process if no directory watch key entry found
        val eventContextAsAbsolutePath = event.contextAsAbsolutePath(firstDirectoryWatchKeyEntry)
        if (
            event.kind() == StandardWatchEventKinds.ENTRY_CREATE
            && eventContextAsAbsolutePath.value.isDirectory(LinkOption.NOFOLLOW_LINKS)
        ) {
            handleDirectoryCreated(firstDirectoryWatchKeyEntry, event)
        } else if (
            event.kind() == StandardWatchEventKinds.ENTRY_DELETE
            && contextIsWatchedDirectory(directoryWatchKeyEntry, eventContextAsAbsolutePath)
        ) {
            handleDirectoryDeleted(directoryWatchKeyEntry, event)
        } else {
            handleFileEvent(directoryWatchKeyEntry, event)
        }
    }

    private suspend fun handleDirectoryCreated(
        firstDirectoryWatchKeyEntry: Scope.DirectoryWatchKeyEntry,
        event: WatchEvent<Path>
    ) = coroutineScope {
        if (
            event.kind() != StandardWatchEventKinds.ENTRY_CREATE
            && !event.context().exists(LinkOption.NOFOLLOW_LINKS)
        ) {
            return@coroutineScope // handler was called inappropriately
        }
        val service = service
            ?: return@coroutineScope // handler was called inappropriately
        val newDirectoryAbsolutePath = firstDirectoryWatchKeyEntry.absoluteDirectory.value
            .resolve(event.context())
            .asAbsolute()
        val newDirectoryRelativePath = root.value.relativize(newDirectoryAbsolutePath.value).asRelative()
        newDirectoryRelativePath.value.toString()
            .let { pathAsString ->
                scopes.values.filter { scope ->
                    scope.directoryPatterns.any { it.matcher(pathAsString).matches() }
                }
            }
            .map { scope ->
                val watchKey = newDirectoryAbsolutePath.value.register(service, watchEventKinds)
                scope.copyAndAddDirectoryWatchKeyEntry(
                    directoryWatchKeyEntryFactory(
                        token = scope.token,
                        absoluteDirectory = newDirectoryAbsolutePath,
                        watchKey = watchKey
                    )
                )
            }
            .onEach { newScope -> scopes[newScope.token] = newScope }
            .also { scanNewWatchedDirectory(newDirectoryAbsolutePath) }
    }

    private suspend fun handleDirectoryDeleted(
        firstDirectoryWatchKeyEntry: Scope.DirectoryWatchKeyEntry,
        event: WatchEvent<Path>
    ): Unit = coroutineScope {
        mutex.withLock {
            val service = service ?: return@withLock
            scopes.values
                .mapNotNull { scope ->
                    val directoryWatchKeyEntriesToCancel = scope.directoryWatchKeyEntries
                        .filter { it.absoluteDirectory == firstDirectoryWatchKeyEntry.absoluteDirectory }
                    if (directoryWatchKeyEntriesToCancel.isNotEmpty()) {
                        directoryWatchKeyEntriesToCancel.forEach { it.watchKey.cancel() }
                        scope
                            .let {
                                TODO("remove directory watch key entries for subdirectories")
                                directoryWatchKeyEntriesToCancel

                            }
                            .copyAndRemoveDirectoryWatchKeyEntries(directoryWatchKeyEntriesToCancel)
                    } else {
                        null
                    }
                }
                .onEach {  }
            directoryWatchKeyEntry.watchKey.cancel()
            scopes[directoryWatchKeyEntry.token]
        }
    }

    private suspend fun scanNewWatchedDirectory(newDirectory: AbsolutePath): Unit = coroutineScope {
        launch {
            /*
            new directories might have new files. by the time we register to watch the new directory,
            it's likely too late for any new watches to receive events about files created
            within the new directory. therefore we intentionally wait for things to settle and
            list/emit events for files that match registered patterns.
            */

            delay(100)
            mutex.withLock {
                val service = service
                if (!isActive || service == null || scopes.isEmpty()) {
                    return@withLock
                }
                newDirectory.value
                    .listDirectoryEntries()
                    .forEach { newDirectoryEntryCandidate ->
                        val newFileCandidateAbsolute: AbsolutePath = newDirectory.value.resolve(newDirectoryEntryCandidate).asAbsolute()
                        val newFileCandidateRelative: RelativePath = root.value.relativize(newFileCandidateAbsolute.value).asRelative()
                        val newFileCandidateRelativeAsString = newFileCandidateRelative.value.toString()
                        if (newFileCandidateAbsolute.value.isDirectory(LinkOption.NOFOLLOW_LINKS)) {
                            val newDirectoryWatchKeyEntriesToAdd = mutableMapOf<TokenImpl, MutableList<ScopeImpl.DirectoryWatchKeyEntryImpl>>()
                            scopes.values.forEach { scope ->
                                if (scope.directoryPatterns.any { it.matcher(newFileCandidateRelativeAsString).matches() }) {
                                    newFileCandidateRelative.value.register(service, watchEventKinds)
                                        .also { watchKey ->
                                            newDirectoryWatchKeyEntriesToAdd[scope.token] = (newDirectoryWatchKeyEntriesToAdd[scope.token] ?: mutableListOf())
                                                .apply {
                                                    add(
                                                        directoryWatchKeyEntryFactory(
                                                            token = scope.token,
                                                            absoluteDirectory = newFileCandidateAbsolute,
                                                            watchKey = watchKey
                                                        )
                                                    )
                                                }
                                        }
                                }
                            }
                            newDirectoryWatchKeyEntriesToAdd.forEach { (token, directoryWatchKeyEntriesToAdd) ->
                                directoryWatchKeyEntriesToAdd.forEach { entryToAdd ->
                                    scopes[token]
                                        ?.copyAndAddDirectoryWatchKeyEntry(entryToAdd)
                                        ?.also { scopes[token] = it }
                                        ?.also { scanNewWatchedDirectory(entryToAdd.absoluteDirectory) }
                                }
                            }
                        } else if (newFileCandidateAbsolute.value.isRegularFile(LinkOption.NOFOLLOW_LINKS)) {
                            scopes.values.forEach { scope ->
                                if (scope.filePatterns.any { it.matcher(newFileCandidateRelativeAsString).matches() }) {
                                    scope.token.events.emit(Event.File.Exists(newFileCandidateRelative, Event.File.Origin.NEW_DIRECTORY_SCAN))
                                }
                            }
                        }

                    }
            }
        }
    }

    private suspend fun handleFileEvent(
        directoryWatchKeyEntry: Scope.DirectoryWatchKeyEntry,
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
        val fileCandidateRelativePath = event.contextAsRelativePath(directoryWatchKeyEntry)
            ?: return@coroutineScope // no watch key in a scope matched taken watch key, ignore
        val fileCandidateRelativePathAsString = fileCandidateRelativePath.value.toString()
        scopes.values.forEach { scope ->
            scope.filePatterns.forEach { filePattern ->
                if (filePattern.matcher(fileCandidateRelativePathAsString).matches()) {
                    when (event.kind()) {
                        StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY -> Event.File.Exists(fileCandidateRelativePath, Event.File.Origin.WATCH)
                        StandardWatchEventKinds.ENTRY_DELETE -> Event.File.DoesNotExist(fileCandidateRelativePath, Event.File.Origin.WATCH)
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

    private suspend fun registerDirectoryPattern(
        token: TokenImpl,
        directoryPattern: Pattern
    ) {
        mutex.withLock {
            val service = this@FileWatchEngine.service ?: return@withLock Unit
            scopes[token]
                ?.copyAndAddDirectoryPattern(directoryPattern)
                ?.also { scopes[token] = it }
            Files.walkFileTree(root.value, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val dirAsAbsolute = dir.asAbsolute()
                    val dirAsRelative = root.value.relativize(dir).asRelative()
                    if (directoryPattern.matcher(dirAsRelative.value.toString()).matches()) {
                        val watchKey = dir.register(service, watchEventKinds)
                        scopes[token]
                            ?.copyAndAddDirectoryWatchKeyEntry(
                                directoryWatchKeyEntryFactory(token, dirAsAbsolute, watchKey)
                            )
                            ?.also { scopes[token] = it }
                    }
                    return FileVisitResult.CONTINUE
                }
            })
            Files.walkFileTree(root.value, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val dirAsAbsolute = dir.asAbsolute()
                    val dirAsRelative = root.value.relativize(dir).asRelative()
                    if (dirAsAbsolute != root) {
                        val parentDirAsAbsolute = dirAsAbsolute.value.parent.asAbsolute()
                        val entryToMutate = scopes[token]
                            ?.directoryWatchKeyEntries
                            ?.firstOrNull { it.absoluteDirectory == parentDirAsAbsolute }
                        if (entryToMutate != null) {
                            scopes[token]
                                ?.copyAndAddWatchedSubdirectory(
                                    watchedSubdirectory = Scope.WatchedSubdirectoryEntry(
                                        absolutePath = dirAsAbsolute,
                                        relativePath = dirAsRelative
                                    ),
                                    watchKey = entryToMutate.watchKey
                                )
                                ?.also { scopes[token] = it }
                        }
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        }
    }

    private suspend fun registerRootDirectory(token: TokenImpl) = registerDirectoryPattern(token, StandardPatterns.root)

    private suspend fun unregisterRootDirectory(token: TokenImpl) = unregisterDirectoryPattern(token, StandardPatterns.root)

    private suspend fun unregisterDirectoryPattern(
        token: TokenImpl,
        directoryPattern: Pattern
    ) {
        mutex.withLock {
            service ?: return@withLock
            scopes[token]
                ?.let { scope ->
                    scope.directoryWatchKeyEntries
                    .filter { directoryPattern.matcher(it.relativeDirectory.value.toString()).matches() }
                    .onEach { it.watchKey.cancel() }
                    .let { scope.copyAndRemoveDirectoryWatchKeyEntries(it) }
                        .copyAndRemoveDirectoryPattern(directoryPattern)
                }
                ?.also { scopes[token] = it }
        }
    }

    private suspend fun registerFilePattern(
        token: TokenImpl,
        filePattern: Pattern
    ) {
        mutex.withLock {
            scopes[token]
                ?.also { it.checkDirectoryPatternsNotEmpty() }
                ?.also { require(!it.filePatterns.contains(filePattern)) { "Scope already has file pattern: $filePattern" } }
                ?.copyAndAddFilePattern(filePattern)
                ?.also { scopes[token] = it }
        }
    }

    private suspend fun unregisterFilePattern(
        token: TokenImpl,
        filePattern: Pattern
    ) {
        mutex.withLock {
            scopes[token]
                ?.also { it.checkDirectoryPatternsNotEmpty() }
                ?.also { require(it.filePatterns.contains(filePattern)) { "Scope does not contain file pattern: $filePattern" } }
                ?.copyAndRemoveFilePattern(filePattern)
                ?.also { scopes[token] = it }
        }
    }

    private fun Scope<*>.checkDirectoryPatternsNotEmpty() = check(directoryPatterns.isNotEmpty()) { "Scope must have a directory pattern registered" }

    private suspend fun destroyToken(token: TokenImpl) = mutex.withLock {
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

    suspend fun shutDown() {
        coroutineScope {
            mutex.withLock {
                scopes.values.forEach { scope ->
                    scope.directoryWatchKeyEntries.forEach { entry ->
                        entry.watchKey.cancel()
                    }
                }
                scopes.clear()
                processScopesForClose()
            }
        }
        coroutineContext.cancel()
    }

    private fun findDirectoryWatchKeyEntries(takenWatchKey: WatchKey): Collection<Scope.DirectoryWatchKeyEntry> {
        return scopes.values.mapNotNull { scope ->
            scope.directoryWatchKeyEntries.firstNotNullOfOrNull { directoryWatchKeyEntry ->
                if (directoryWatchKeyEntry.watchKey == takenWatchKey) {
                    directoryWatchKeyEntry
                } else {
                    null
                }
            }
        }
    }

    private fun WatchEvent<Path>.contextAsRelativePath(directoryWatchKeyEntry: Scope.DirectoryWatchKeyEntry): RelativePath {
        return directoryWatchKeyEntry.absoluteDirectory.value
            .resolve(context())
            .let { root.value.relativize(it) }
            .asRelative()
    }

    private fun WatchEvent<Path>.contextAsAbsolutePath(directoryWatchKeyEntry: Scope.DirectoryWatchKeyEntry): AbsolutePath {
        return directoryWatchKeyEntry.absoluteDirectory.value
            .resolve(context())
            .asAbsolute()
    }

    private fun contextIsWatchedDirectory(
        directoryWatchKeyEntry: Scope.DirectoryWatchKeyEntry,
        contextAsAbsolutePath: AbsolutePath
    ): Boolean {
        return directoryWatchKeyEntry.watchedSubdirectories
            .any { it.absolutePath == contextAsAbsolutePath }
    }

    interface Scope<DWKE : Scope.DirectoryWatchKeyEntry> {
        val token: Token
        val directoryPatterns: List<Pattern>
        val filePatterns: List<Pattern>
        val directoryWatchKeyEntries: List<DWKE>

        fun copyAndAddDirectoryPattern(directoryPattern: Pattern): Scope<DWKE>
        fun copyAndRemoveDirectoryPattern(directoryPattern: Pattern): Scope<DWKE>
        fun copyAndAddFilePattern(filePattern: Pattern): Scope<DWKE>
        fun copyAndRemoveFilePattern(filePattern: Pattern): Scope<DWKE>

        fun copyAndAddDirectoryWatchKeyEntry(entry: DWKE): Scope<DWKE>
        fun copyAndRemoveDirectoryWatchKeyEntry(entry: DWKE): Scope<DWKE>
        fun copyAndRemoveDirectoryWatchKeyEntries(entries: Collection<DWKE>): Scope<DWKE>

        interface DirectoryWatchKeyEntry {
            val token: Token

            val absoluteDirectory: AbsolutePath
            val relativeDirectory: RelativePath
            val watchKey: WatchKey
            val watchedSubdirectories: List<WatchedSubdirectoryEntry>
            fun copyAndAddWatchedSubdirectory(watchedSubdirectory: WatchedSubdirectoryEntry): DirectoryWatchKeyEntry
            fun copyAndRemoveWatchedSubdirectory(watchedSubdirectory: WatchedSubdirectoryEntry): DirectoryWatchKeyEntry
        }

        data class WatchedSubdirectoryEntry(
            val absolutePath: AbsolutePath,
            val relativePath: RelativePath
        )
    }

    protected data class ScopeImpl(
        override val token: TokenImpl,
        override val directoryPatterns: List<Pattern>,
        override val filePatterns: List<Pattern>,
        override val directoryWatchKeyEntries: List<DirectoryWatchKeyEntryImpl>
    ) : Scope<ScopeImpl.DirectoryWatchKeyEntryImpl> {
        override fun copyAndAddDirectoryPattern(directoryPattern: Pattern) = copy(
            directoryPatterns = directoryPatterns
                .toMutableList()
                .apply { add(directoryPattern) }
        )

        override fun copyAndRemoveDirectoryPattern(directoryPattern: Pattern) = copy(
            directoryPatterns = directoryPatterns
                .toMutableList()
                .apply { remove(directoryPattern) }
        )

        override fun copyAndAddFilePattern(filePattern: Pattern) = copy(
            filePatterns = filePatterns
                .toMutableList()
                .apply { add(filePattern) }
        )

        override fun copyAndRemoveFilePattern(filePattern: Pattern) = copy(
            filePatterns = filePatterns
                .toMutableList()
                .apply { remove(filePattern) }
        )
        
        override fun copyAndAddDirectoryWatchKeyEntry(entry: DirectoryWatchKeyEntryImpl) = copy(
            directoryWatchKeyEntries = directoryWatchKeyEntries
                .toMutableList()
                .apply { add(entry) }
        )

        override fun copyAndRemoveDirectoryWatchKeyEntry(entry: DirectoryWatchKeyEntryImpl) = copy(
            directoryWatchKeyEntries = directoryWatchKeyEntries
                .toMutableList()
                .apply { remove(entry) }
        )

        override fun copyAndRemoveDirectoryWatchKeyEntries(entries: Collection<DirectoryWatchKeyEntryImpl>) = copy(
            directoryWatchKeyEntries = directoryWatchKeyEntries
                .toMutableList()
                .apply { removeAll(entries) }
        )

        fun copyAndAddWatchedSubdirectory(watchedSubdirectory: Scope.WatchedSubdirectoryEntry, watchKey: WatchKey)
        = copyAndMutateWatchedSubdirectory(watchedSubdirectory, watchKey) { it.copyAndAddWatchedSubdirectory(watchedSubdirectory) }

        fun copyAndRemoveWatchedSubdirectory(watchedSubdirectory: Scope.WatchedSubdirectoryEntry, watchKey: WatchKey)
        = copyAndMutateWatchedSubdirectory(watchedSubdirectory, watchKey) { it.copyAndRemoveWatchedSubdirectory(watchedSubdirectory) }

        private fun copyAndMutateWatchedSubdirectory(
            watchedSubdirectory: Scope.WatchedSubdirectoryEntry,
            watchKey: WatchKey,
            block: (DirectoryWatchKeyEntryImpl) -> DirectoryWatchKeyEntryImpl
        ) = copy(
            directoryWatchKeyEntries = directoryWatchKeyEntries
                .toMutableList()
                .let { directoryWatchKeyEntries ->
                    val indexToChange = directoryWatchKeyEntries.indexOfFirst { it.watchKey == watchKey }
                    val directoryWatchKeyEntryToChange = directoryWatchKeyEntries[indexToChange]
                    block(directoryWatchKeyEntryToChange)
                        .let {
                            directoryWatchKeyEntries[indexToChange] = it
                            directoryWatchKeyEntries
                        }
                }
        )

        data class DirectoryWatchKeyEntryImpl(
            override val token: Token,
            override val absoluteDirectory: AbsolutePath,
            override val relativeDirectory: RelativePath,
            override val watchKey: WatchKey,
            override val watchedSubdirectories: List<Scope.WatchedSubdirectoryEntry>
        ) : Scope.DirectoryWatchKeyEntry {

            override fun copyAndAddWatchedSubdirectory(watchedSubdirectory: Scope.WatchedSubdirectoryEntry): DirectoryWatchKeyEntryImpl = copy(
                watchedSubdirectories = watchedSubdirectories
                    .toMutableList()
                    .apply { add(watchedSubdirectory) }
            )

            override fun copyAndRemoveWatchedSubdirectory(watchedSubdirectory: Scope.WatchedSubdirectoryEntry): DirectoryWatchKeyEntryImpl = copy(
                watchedSubdirectories = watchedSubdirectories
                    .toMutableList()
                    .apply { remove(watchedSubdirectory) }
            )
        }
    }

    private fun directoryWatchKeyEntryFactory(
        token: Token,
        absoluteDirectory: AbsolutePath,
        watchKey: WatchKey
    ) = ScopeImpl.DirectoryWatchKeyEntryImpl(
        token = token,
        absoluteDirectory = absoluteDirectory,
        relativeDirectory = root.value.relativize(absoluteDirectory.value).asRelative(),
        watchKey = watchKey,
        watchedSubdirectories = emptyList()
    )

    interface Token {
        val events: Flow<Event>

        suspend fun registerDirectoryPattern(pattern: Pattern)
        suspend fun registerRootDirectory()
        suspend fun unregisterRootDirectory()
        suspend fun unregisterDirectoryPattern(pattern: Pattern)

        suspend fun registerFilePattern(pattern: Pattern)
        suspend fun unregisterFilePattern(pattern: Pattern)
    }

    protected data class TokenImpl(private val id: Int) : Token {
        lateinit var engine: FileWatchEngine
        override val events = MutableSharedFlow<Event>()

        override suspend fun registerDirectoryPattern(pattern: Pattern) {
            engine.registerDirectoryPattern(this, pattern)
        }

        override suspend fun registerRootDirectory() {
            engine.registerRootDirectory(this)
        }

        override suspend fun unregisterRootDirectory() {
            engine.unregisterRootDirectory(this)
        }

        override suspend fun unregisterDirectoryPattern(pattern: Pattern) {
            engine.unregisterDirectoryPattern(this, pattern)
        }

        override suspend fun registerFilePattern(pattern: Pattern) {
            engine.registerFilePattern(this, pattern)
        }

        override suspend fun unregisterFilePattern(pattern: Pattern) {
            engine.unregisterFilePattern(this, pattern)
        }
    }

    sealed class Event {
        sealed class File : Event() {
            abstract val file: RelativePath
            abstract val origin: Origin

            data class Exists(
                override val file: RelativePath,
                override val origin: Origin
            ) : File()

            data class DoesNotExist(
                override val file: RelativePath,
                override val origin: Origin
            ) : File()

            enum class Origin {
                WATCH,
                NEW_DIRECTORY_SCAN
            }
        }
        object Overflow : Event()
    }

    object StandardPatterns {
        val root: Pattern by lazy { Pattern.compile("^$") }
    }
}