package tech.coner.snoozle.db.watch

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.coner.snoozle.db.Key
import tech.coner.snoozle.db.KeyMapper
import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.path.PathPart
import tech.coner.snoozle.db.path.Pathfinder
import tech.coner.snoozle.db.path.RelativePath
import tech.coner.snoozle.util.hasUuidPattern
import java.util.*
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext

class EntityWatchEngine<K : Key, E : Entity<K>>(
    override val coroutineContext: CoroutineContext,
    private val fileWatchEngine: FileWatchEngine,
    private val keyMapper: KeyMapper<K, E>,
    private val resource: EntityResource<K, E>,
    private val pathfinder: Pathfinder<K, E>
) : CoroutineScope {

    private val mutex = Mutex()
    private var nextKeyFilterId: Int = Int.MIN_VALUE

    protected var watchStoreFactoryFn: () -> WatchStore<K, E, TokenImpl<K, E>, Scope<K, E>> = {
        WatchStore()
    }
    protected val watchStore: WatchStore<K, E, TokenImpl<K, E>, Scope<K, E>> by lazy {
        watchStoreFactoryFn()
    }

    suspend fun createToken(): Token<K, E> = mutex.withLock {
        watchStore.create(
            tokenFactory = { id -> TokenImpl(id) },
            scopeFactory = { token ->
                Scope(
                    coroutineContext = coroutineContext + Job(),
                    token = token
                )
                    .also { scope ->
                        token.engine = this
                        scope.fileWatchEngineToken = fileWatchEngine.getOrCreateToken()
                            .also { fileWatchEngineToken ->
                                fileWatchEngineToken.registerRootDirectory()
                                fileWatchEngineToken.events
                                    .onEach {
                                        scope.launch {
                                            mutex.withLock {
                                                handleFileWatchEvent(scope, it)
                                            }
                                        }
                                    }
                                    .launchIn(scope)
                            }
                    }
            }
        )
    }

    private suspend fun handleFileWatchEvent(scope: Scope<K, E>, event: FileWatchEvent) {
        when (event) {
            is Event.Record -> handleRecordEvent(scope, event)
            is Event.Overflow -> handleOverflowEvent(scope, event)
        }
    }

    private suspend fun handleRecordEvent(scope: Scope<K, E>, event: Event.Record<RelativePath, Unit>) {
        val emit = event
            .let {
                val key = keyMapper.fromRelativeRecord(it.recordId)
                when (it) {
                    is Event.Exists<RelativePath, Unit> -> Event.Exists(
                        recordId = key,
                        recordContent = resource.read(key),
                        origin = it.origin
                    )
                    is Event.Deleted<RelativePath, Unit> -> Event.Deleted(
                        recordId = key,
                        origin = it.origin
                    )
                }
            }
        scope.token.events.emit(emit)
    }

    private suspend fun handleOverflowEvent(scope: Scope<K, E>, event: Event.Overflow<RelativePath, Unit>) {
        val emit = Event.Overflow<K, E>()
        scope.token.events.emit(emit)
    }

    suspend fun register(token: TokenImpl<K, E>, watch: Watch<K>) = mutex.withLock {
        watchStore[token].register(watch)
    }

    suspend fun unregister(token: TokenImpl<K, E>, watch: Watch<K>) = mutex.withLock {
        watchStore[token].unregister(watch)
    }

    suspend fun unregisterAll(token: TokenImpl<K, E>) = mutex.withLock {
        watchStore[token].unregisterAll()
    }

    suspend fun destroyToken(token: TokenImpl<K, E>) = mutex.withLock {
        watchStore.destroy(token, ::afterDestroyTokenFn)
    }

    suspend fun destroyAllTokens() = mutex.withLock {
        watchStore.destroyAll(::afterDestroyTokenFn)
    }

    private suspend fun afterDestroyTokenFn(scope: Scope<K, E>) {
        scope.cancel()
        scope.fileWatchEngineToken.destroy()
    }

    interface Token<K : Key, E : Entity<K>> : WatchToken<K, E> {

        suspend fun register(watch: Watch<K>)
        suspend fun unregister(watch: Watch<K>)
        suspend fun unregisterAll()
    }

    data class TokenImpl<K : Key, E : Entity<K>>(
        override val id: Int
    ) : Token<K, E>, StorableWatchToken<K, E> {
        lateinit var engine: EntityWatchEngine<K, E>
        override val events = MutableSharedFlow<Event<K, E>>(replay = 100)
        override var destroyed: Boolean = false

        override suspend fun register(watch: Watch<K>) {
            engine.register(this, watch)
        }

        override suspend fun unregister(watch: Watch<K>) {
            engine.unregister(this, watch)
        }

        override suspend fun unregisterAll() {
            engine.unregisterAll(this)
        }

        override suspend fun destroy() {
            engine.destroyToken(this)
        }
    }

    class Scope<K : Key, E : Entity<K>>(
        coroutineContext: CoroutineContext,
        override val token: TokenImpl<K, E>,
    ) : StorableWatchScope<K, E, TokenImpl<K, E>>,
        CoroutineScope by CoroutineScope(coroutineContext) {
        lateinit var fileWatchEngineToken: FileWatchEngine.Token
        private var watches: Set<Watch<K>> = emptySet()

        suspend fun register(watch: Watch<K>) {
            check(!watches.contains(watch)) { "Watch already registered: $watch" }
            if (watches.none { it.directoryPattern == watch.directoryPattern }) {
                fileWatchEngineToken.registerDirectoryPattern(watch.directoryPattern)
            }
            if (watches.none { it.filePattern == watch.filePattern }) {
                fileWatchEngineToken.registerFilePattern(watch.filePattern)
            }
            watches = watches
                .toMutableSet()
                .apply { add(watch) }
        }

        suspend fun unregister(watch: Watch<K>) {
            TODO()
        }

        suspend fun unregisterAll() {
            TODO()
        }
    }

    fun createWatch(builderDslFn: WatchBuilderDsl.() -> Unit): Watch<K> {
        return WatchBuilderDslImpl()
            .apply(builderDslFn)
            .build()
    }

    class Watch<K : Key>(
        val id: Int,
        val directoryPattern: Pattern,
        val filePattern: Pattern
    )

    interface WatchBuilderDsl {
        fun uuidIsAny()
        fun uuidIsEqualTo(uuid: UUID)
        fun uuidIsOneOf(uuids: Collection<UUID>)
    }

    inner class WatchBuilderDslImpl(
    ) : WatchBuilderDsl {

        private val segmentFilters: MutableList<Pattern> = mutableListOf()

        override fun uuidIsAny() {
            segmentFilters += hasUuidPattern
        }

        override fun uuidIsEqualTo(uuid: UUID) {
            segmentFilters += Pattern.compile(uuid.toString())
        }

        override fun uuidIsOneOf(uuids: Collection<UUID>) {
            segmentFilters += Pattern.compile(uuids.joinToString(prefix = "(", separator = "|", postfix = ")"))
        }

        fun build(): Watch<K> {
            val countOfVariableExtractors = resource.definition.path
                .count { it is PathPart.VariableExtractor<*, *> }
            check(countOfVariableExtractors == segmentFilters.size)
            var nextDirectoryPatternVariableExtractorIndex = 0
            var nextFilePatternVariableExtractorIndex = 0
            fun handleUnknownPartPartType(): String {
                throw IllegalStateException("Encountered pathPart that is neither a static nor variable extractor")
            }
            return Watch(
                id = runBlocking { mutex.withLock { nextKeyFilterId++ } },
                directoryPattern = resource.definition.pathParent
                    .joinToString(prefix = "^", postfix = "$") { pathPart ->
                        when (pathPart) {
                            is PathPart.StaticExtractor<*> -> {
                                val pattern = pathPart.regex.pattern()
                                pattern
                            }
                            is PathPart.VariableExtractor<*, *> -> {
                                val segmentFilterIndex = nextDirectoryPatternVariableExtractorIndex
                                val pattern = segmentFilters[segmentFilterIndex]
                                    .also { nextDirectoryPatternVariableExtractorIndex++ }
                                    .pattern()
                                pattern
                            }
                            else -> handleUnknownPartPartType()
                        }
                    }
                    .let(Pattern::compile),
                filePattern = resource.definition.path
                    .joinToString(prefix = "^", separator = "", postfix = "$") { pathPart ->
                        when (pathPart) {
                            is PathPart.StaticExtractor<*> -> pathPart.regex.pattern()
                            is PathPart.VariableExtractor<*, *> -> {
                                val segmentFilterIndex = nextFilePatternVariableExtractorIndex
                                segmentFilters[segmentFilterIndex]
                                    .also { nextFilePatternVariableExtractorIndex++ }
                                    .pattern()
                            }
                            else -> handleUnknownPartPartType()
                        }
                    }
                    .let(Pattern::compile)
            )
        }
    }
}