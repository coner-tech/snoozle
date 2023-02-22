package tech.coner.snoozle.db.watch

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
import tech.coner.snoozle.db.path.AbsolutePath
import tech.coner.snoozle.db.path.RelativePath
import tech.coner.snoozle.db.path.asAbsolute
import tech.coner.snoozle.db.path.asRelative
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
    private val root: AbsolutePath,
) : CoroutineScope {

    private val mutex = Mutex()
    protected var watchServiceFactoryFn: (AbsolutePath) -> WatchService = {
        it.value.fileSystem.newWatchService()
    }
    protected var watchKeyFactoryFn: (AbsolutePath, WatchService) -> WatchKey = { dir, watchService ->
        dir.value.register(watchService, watchEventKinds)
    }
    protected var watchStoreFactoryFn: () -> WatchStore<RelativePath, Unit, TokenImpl, Scope> = {
        WatchStore()
    }

    protected val watchStore: WatchStore<RelativePath, Unit, TokenImpl, Scope> by lazy {
        watchStoreFactoryFn()
    }

    protected var service: WatchService? = null
    protected var pollLoopScope: CoroutineContext? = null
    protected var pollLoopJob: Job? = null

    suspend fun getOrCreateToken(): Token = mutex.withLock {
        watchStore.allTokens.firstOrNull()
            ?: watchStore.create(
                tokenFactory = { id -> TokenImpl(id) },
                scopeFactory = { token ->
                    token.engine = this
                    Scope(
                        token = token,
                        directoryPatterns = emptyList(),
                        filePatterns = emptyList(),
                        directoryWatchKeyEntries = emptyList()
                    )
                }
            )
                .also { startService() }
    }

    private suspend fun startService() = coroutineScope {
        if (service == null) {
            service = runInterruptible { watchServiceFactoryFn(root) /*root.value.fileSystem.newWatchService() */ }
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

                            StandardWatchEventKinds.OVERFLOW -> handleOverflowEvent(
                                takenWatchKey = watchKey,
                                event = event as WatchEvent<Any>
                            )

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
            && contextIsWatchedDirectory(firstDirectoryWatchKeyEntry, eventContextAsAbsolutePath)
        ) {
            handleDirectoryDeleted(firstDirectoryWatchKeyEntry, event)
        } else {
            handleFileEvent(firstDirectoryWatchKeyEntry, event)
        }
    }

    private suspend fun handleDirectoryCreated(
        firstDirectoryWatchKeyEntry: Scope.DirectoryWatchKeyEntry,
        event: WatchEvent<Path>
    ) = coroutineScope {
        if (
            event.kind() != StandardWatchEventKinds.ENTRY_CREATE
            || !firstDirectoryWatchKeyEntry.absoluteDirectory.value.exists(LinkOption.NOFOLLOW_LINKS)
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
                watchStore.allScopes.filter { scope ->
                    scope.directoryPatterns.any { it.matcher(pathAsString).matches() }
                }
            }
            .map { scope ->
                val watchKey = watchKeyFactoryFn(newDirectoryAbsolutePath, service)
                scope.copyAndAddDirectoryWatchKeyEntry(
                    directoryWatchKeyEntryFactory(
                        token = scope.token,
                        absoluteDirectory = newDirectoryAbsolutePath,
                        watchKey = watchKey
                    )
                )
            }
            .onEach { newScope -> watchStore[newScope.token] = newScope }
            .also { scanNewWatchedDirectory(newDirectoryAbsolutePath) }
    }

    private suspend fun handleDirectoryDeleted(
        firstDirectoryWatchKeyEntry: Scope.DirectoryWatchKeyEntry,
        event: WatchEvent<Path>
    ): Unit = coroutineScope {
        if (event.kind() != StandardWatchEventKinds.ENTRY_DELETE) {
            return@coroutineScope // handler was called inappropriately
        }
        val service = service ?: return@coroutineScope // handler was called inappropriately
        val deletedDirectoryAbsolute = event.contextAsAbsolutePath(firstDirectoryWatchKeyEntry)
        fun Scope.findDirectoryWatchKeyEntriesToCancel() = directoryWatchKeyEntries
            .filter { it.absoluteDirectory == deletedDirectoryAbsolute }

        fun Scope.findDirectoryWatchKeysToRemoveWatchedSubdirectory() = directoryWatchKeyEntries
            .filter { deletedDirectoryAbsolute.value.parent == it.absoluteDirectory.value }
        watchStore.allScopes
            .mapNotNull { scope ->
                val directoryWatchKeyEntriesToCancel = scope.findDirectoryWatchKeyEntriesToCancel()
                if (directoryWatchKeyEntriesToCancel.isNotEmpty()) {
                    directoryWatchKeyEntriesToCancel.forEach { it.watchKey.cancel() }
                    directoryWatchKeyEntriesToCancel.fold(scope) { accScope, directoryWatchKeyEntry ->
                        accScope.copyAndRemoveDirectoryWatchKeyEntry(directoryWatchKeyEntry)
                    }
                        .let { watchKeyCanceledScope ->
                            watchKeyCanceledScope.findDirectoryWatchKeysToRemoveWatchedSubdirectory()
                                .fold(watchKeyCanceledScope) { accScope, directoryWatchKeyEntry ->
                                    accScope.copyAndRemoveWatchedSubdirectory(
                                        watchedSubdirectory = directoryWatchKeyEntry.watchedSubdirectories.single {
                                            it.absolutePath.value == deletedDirectoryAbsolute.value
                                        },
                                        watchKey = directoryWatchKeyEntry.watchKey
                                    )
                                }
                        }
                } else {
                    null
                }
            }
            .onEach { watchStore[it.token] = it }
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
                if (!isActive || service == null || watchStore.isEmpty()) {
                    return@withLock
                }
                newDirectory.value
                    .listDirectoryEntries()
                    .forEach { newDirectoryEntryCandidate ->
                        val newFileCandidateAbsolute: AbsolutePath =
                            newDirectory.value.resolve(newDirectoryEntryCandidate).asAbsolute()
                        val newFileCandidateRelative: RelativePath =
                            root.value.relativize(newFileCandidateAbsolute.value).asRelative()
                        val newFileCandidateRelativeAsString = newFileCandidateRelative.value.toString()
                        if (newFileCandidateAbsolute.value.isDirectory(LinkOption.NOFOLLOW_LINKS)) {
                            val newDirectoryWatchKeyEntriesToAdd =
                                mutableMapOf<TokenImpl, MutableList<Scope.DirectoryWatchKeyEntry>>()
                            watchStore.allScopes.forEach { scope ->
                                if (scope.directoryPatterns.any {
                                        it.matcher(newFileCandidateRelativeAsString).matches()
                                    }) {
                                    watchKeyFactoryFn(newFileCandidateAbsolute, service)
                                        .also { watchKey ->
                                            newDirectoryWatchKeyEntriesToAdd[scope.token] =
                                                (newDirectoryWatchKeyEntriesToAdd[scope.token] ?: mutableListOf())
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
                                    watchStore[token]
                                        ?.copyAndAddDirectoryWatchKeyEntry(entryToAdd)
                                        ?.also { watchStore[token] = it }
                                        ?.also { scanNewWatchedDirectory(entryToAdd.absoluteDirectory) }
                                }
                            }
                        } else if (newFileCandidateAbsolute.value.isRegularFile(LinkOption.NOFOLLOW_LINKS)) {
                            watchStore.allScopes.forEach { scope ->
                                if (scope.filePatterns.any { it.matcher(newFileCandidateRelativeAsString).matches() }) {
                                    scope.token.events.emit(
                                        Event.Exists(
                                            recordId = newFileCandidateRelative,
                                            recordContent = Unit,
                                            origin = Event.Origin.NEW_DIRECTORY_SCAN
                                        )
                                    )
                                }
                            }
                        }
                    }
            }
        }
    }

    private suspend fun handleFileEvent(
        firstDirectoryWatchKeyEntry: Scope.DirectoryWatchKeyEntry,
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
        val fileCandidateRelativePath = event.contextAsRelativePath(firstDirectoryWatchKeyEntry)
        val fileCandidateRelativePathAsString = fileCandidateRelativePath.value.toString()
        watchStore.allScopes.forEach { scope ->
            scope.filePatterns.forEach { filePattern ->
                if (filePattern.matcher(fileCandidateRelativePathAsString).matches()) {
                    when (event.kind()) {
                        StandardWatchEventKinds.ENTRY_CREATE,
                        StandardWatchEventKinds.ENTRY_MODIFY -> Event.Exists(
                            recordId = fileCandidateRelativePath,
                            recordContent = Unit,
                            origin = Event.Origin.WATCH
                        )
                        StandardWatchEventKinds.ENTRY_DELETE -> Event.Deleted(
                            recordId = fileCandidateRelativePath,
                            origin = Event.Origin.WATCH
                        )
                        else -> null
                    }
                        ?.also { scope.token.events.emit(it) }
                }
            }
        }
    }

    private suspend fun handleOverflowEvent(
        takenWatchKey: WatchKey,
        event: WatchEvent<Any>
    ) {
        watchStore.allScopes
            .filter { scope -> scope.directoryWatchKeyEntries.any { it.watchKey === takenWatchKey } }
            .forEach { scope -> scope.token.events.emit(Event.Overflow()) }
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
            watchStore[token]
                ?.copyAndAddDirectoryPattern(directoryPattern)
                ?.also { watchStore[token] = it }
            Files.walkFileTree(root.value, object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val dirAsAbsolute = dir.asAbsolute()
                    val dirAsRelative = root.value.relativize(dir).asRelative()
                    if (directoryPattern.matcher(dirAsRelative.value.toString()).matches()) {
                        val watchKey = watchKeyFactoryFn(dirAsAbsolute, service)
                        watchStore[token]
                            ?.copyAndAddDirectoryWatchKeyEntry(
                                directoryWatchKeyEntryFactory(token, dirAsAbsolute, watchKey)
                            )
                            ?.also { watchStore[token] = it }
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
                        val entryToMutate = watchStore[token]
                            ?.directoryWatchKeyEntries
                            ?.firstOrNull { it.absoluteDirectory == parentDirAsAbsolute }
                        if (entryToMutate != null) {
                            watchStore[token]
                                ?.copyAndAddWatchedSubdirectory(
                                    watchedSubdirectory = Scope.WatchedSubdirectoryEntry(
                                        absolutePath = dirAsAbsolute,
                                        relativePath = dirAsRelative
                                    ),
                                    watchKey = entryToMutate.watchKey
                                )
                                ?.also { watchStore[token] = it }
                        }
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        }
    }

    private suspend fun registerRootDirectory(token: TokenImpl) = registerDirectoryPattern(token, StandardPatterns.root)

    private suspend fun unregisterRootDirectory(token: TokenImpl) =
        unregisterDirectoryPattern(token, StandardPatterns.root)

    private suspend fun unregisterDirectoryPattern(
        token: TokenImpl,
        directoryPattern: Pattern
    ) {
        mutex.withLock {
            service ?: return@withLock
            watchStore[token]
                ?.let { scope ->
                    scope.directoryWatchKeyEntries
                        .filter { directoryPattern.matcher(it.relativeDirectory.value.toString()).matches() }
                        .onEach { it.watchKey.cancel() }
                        .let { scope.copyAndRemoveDirectoryWatchKeyEntries(it) }
                        .copyAndRemoveDirectoryPattern(directoryPattern)
                }
                ?.also { watchStore[token] = it }
        }
    }

    private suspend fun registerFilePattern(
        token: TokenImpl,
        filePattern: Pattern
    ) {
        mutex.withLock {
            watchStore[token]
                ?.also { it.checkDirectoryPatternsNotEmpty() }
                ?.also { require(!it.filePatterns.contains(filePattern)) { "Scope already has file pattern: $filePattern" } }
                ?.copyAndAddFilePattern(filePattern)
                ?.also { watchStore[token] = it }
        }
    }

    private suspend fun unregisterFilePattern(
        token: TokenImpl,
        filePattern: Pattern
    ) {
        mutex.withLock {
            watchStore[token]
                ?.also { it.checkDirectoryPatternsNotEmpty() }
                ?.also { require(it.filePatterns.contains(filePattern)) { "Scope does not contain file pattern: $filePattern" } }
                ?.copyAndRemoveFilePattern(filePattern)
                ?.also { watchStore[token] = it }
        }
    }

    private fun Scope.checkDirectoryPatternsNotEmpty() =
        check(directoryPatterns.isNotEmpty()) { "Scope must have a directory pattern registered" }

    suspend fun destroyToken(token: TokenImpl) = mutex.withLock {
        watchStore.destroy(
            token = token,
            afterDestroyFn = { scope ->
                processScopesForClose()
                scope.directoryWatchKeyEntries.forEach { destroyedDirectoryWatchKeyEntry ->
                    if (watchStore.allScopes.none { otherScope ->
                            otherScope.directoryWatchKeyEntries.any {
                                it.watchKey == destroyedDirectoryWatchKeyEntry.watchKey
                            }
                        }) {
                        destroyedDirectoryWatchKeyEntry.watchKey.cancel()
                    }
                }
            }
        )
    }

    private fun processScopesForClose() {
        if (watchStore.isEmpty()) {
            pollLoopJob?.cancel()
            pollLoopJob = null
            pollLoopScope?.cancel()
            pollLoopScope = null
            service?.close()
            service = null
            watchStore.destroyAll()
        }
    }

    suspend fun shutDown() {
        coroutineScope {
            mutex.withLock {
                watchStore.destroyAll()
                processScopesForClose()
            }
        }
        coroutineContext.cancel()
    }

    private fun findDirectoryWatchKeyEntries(takenWatchKey: WatchKey): Collection<Scope.DirectoryWatchKeyEntry> {
        return watchStore.allScopes.mapNotNull { scope ->
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

    data class Scope(
        override val token: TokenImpl,
        val directoryPatterns: List<Pattern>,
        val filePatterns: List<Pattern>,
        val directoryWatchKeyEntries: List<DirectoryWatchKeyEntry>
    ) : StorableWatchScope<RelativePath, Unit, TokenImpl> {
        fun copyAndAddDirectoryPattern(directoryPattern: Pattern) = copy(
            directoryPatterns = directoryPatterns
                .toMutableList()
                .apply { add(directoryPattern) }
        )

        fun copyAndRemoveDirectoryPattern(directoryPattern: Pattern) = copy(
            directoryPatterns = directoryPatterns
                .toMutableList()
                .apply { remove(directoryPattern) }
        )

        fun copyAndAddFilePattern(filePattern: Pattern) = copy(
            filePatterns = filePatterns
                .toMutableList()
                .apply { add(filePattern) }
        )

        fun copyAndRemoveFilePattern(filePattern: Pattern) = copy(
            filePatterns = filePatterns
                .toMutableList()
                .apply { remove(filePattern) }
        )

        fun copyAndAddDirectoryWatchKeyEntry(entry: DirectoryWatchKeyEntry) = copy(
            directoryWatchKeyEntries = directoryWatchKeyEntries
                .toMutableList()
                .apply { add(entry) }
        )

        fun copyAndRemoveDirectoryWatchKeyEntry(entry: DirectoryWatchKeyEntry) = copy(
            directoryWatchKeyEntries = directoryWatchKeyEntries
                .toMutableList()
                .apply { remove(entry) }
        )

        fun copyAndRemoveDirectoryWatchKeyEntries(entries: Collection<DirectoryWatchKeyEntry>) = copy(
            directoryWatchKeyEntries = directoryWatchKeyEntries
                .toMutableList()
                .apply { removeAll(entries) }
        )

        fun copyAndAddWatchedSubdirectory(watchedSubdirectory: WatchedSubdirectoryEntry, watchKey: WatchKey) =
            copyAndMutateWatchedSubdirectory(watchKey) { it.copyAndAddWatchedSubdirectory(watchedSubdirectory) }

        fun copyAndRemoveWatchedSubdirectory(watchedSubdirectory: WatchedSubdirectoryEntry, watchKey: WatchKey) =
            copyAndMutateWatchedSubdirectory(watchKey) { it.copyAndRemoveWatchedSubdirectory(watchedSubdirectory) }

        private fun copyAndMutateWatchedSubdirectory(
            watchKey: WatchKey,
            block: (DirectoryWatchKeyEntry) -> DirectoryWatchKeyEntry
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

        data class DirectoryWatchKeyEntry(
            val token: TokenImpl,
            val absoluteDirectory: AbsolutePath,
            val relativeDirectory: RelativePath,
            val watchKey: WatchKey,
            val watchedSubdirectories: Set<WatchedSubdirectoryEntry>
        ) {

            fun copyAndAddWatchedSubdirectory(watchedSubdirectory: WatchedSubdirectoryEntry): DirectoryWatchKeyEntry =
                copy(
                    watchedSubdirectories = watchedSubdirectories
                        .toMutableSet()
                        .apply { add(watchedSubdirectory) }
                )

            fun copyAndRemoveWatchedSubdirectory(watchedSubdirectory: WatchedSubdirectoryEntry): DirectoryWatchKeyEntry =
                copy(
                    watchedSubdirectories = watchedSubdirectories
                        .toMutableSet()
                        .apply { remove(watchedSubdirectory) }
                )
        }

        data class WatchedSubdirectoryEntry(
            val absolutePath: AbsolutePath,
            val relativePath: RelativePath
        )
    }

    private fun directoryWatchKeyEntryFactory(
        token: TokenImpl,
        absoluteDirectory: AbsolutePath,
        watchKey: WatchKey
    ) = Scope.DirectoryWatchKeyEntry(
        token = token,
        absoluteDirectory = absoluteDirectory,
        relativeDirectory = root.value.relativize(absoluteDirectory.value).asRelative(),
        watchKey = watchKey,
        watchedSubdirectories = emptySet()
    )

    interface Token : WatchToken<RelativePath, Unit> {

        suspend fun registerDirectoryPattern(pattern: Pattern)
        suspend fun registerRootDirectory()
        suspend fun unregisterRootDirectory()
        suspend fun unregisterDirectoryPattern(pattern: Pattern)
        suspend fun registerFilePattern(pattern: Pattern)
        suspend fun unregisterFilePattern(pattern: Pattern)
    }

    data class TokenImpl(override val id: Int) : Token, StorableWatchToken<RelativePath, Unit> {
        lateinit var engine: FileWatchEngine
        override val events = MutableSharedFlow<Event<RelativePath, Unit>>()
        override var destroyed: Boolean = false

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

        override suspend fun destroy() {
            engine.destroyToken(this)
        }
    }

    object StandardPatterns {
        val root: Pattern by lazy { Pattern.compile("^$") }
    }

}

typealias FileWatchEvent = Event<RelativePath, Unit>
