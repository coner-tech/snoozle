package tech.coner.snoozle.db.watch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.coner.snoozle.db.Key
import tech.coner.snoozle.db.KeyMapper
import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.entity.EntityResource
import tech.coner.snoozle.db.path.PathPart
import tech.coner.snoozle.db.path.Pathfinder
import tech.coner.snoozle.db.path.RelativePath
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

    suspend fun registerAll(token: TokenImpl<K, E>) = mutex.withLock {
        val scope = watchStore[token]
        val fileWatchEngineToken = scope.fileWatchEngineToken
        fileWatchEngineToken.registerDirectoryPattern(pathfinder.recordParentCandidatePath)
        fileWatchEngineToken.registerFilePattern(pathfinder.recordCandidatePath)
    }
    
    suspend fun unregisterAll(token: TokenImpl<K, E>) = mutex.withLock { 
        val fileWatchEngineToken = watchStore[token].fileWatchEngineToken
        fileWatchEngineToken.unregisterDirectoryPattern(pathfinder.recordParentCandidatePath)
        fileWatchEngineToken.unregisterFilePattern(pathfinder.recordCandidatePath)
    }

    suspend fun registerMatching(token: TokenImpl<K, E>, keyFilter: (K) -> Boolean): Nothing = mutex.withLock {
        val scope = watchStore[token]
        TODO("register keyFilter")
        TODO("attach keyFilter")
    }

    suspend fun unregisterMatching(token: TokenImpl<K, E>, keyFilter: (K) -> Boolean): Nothing = mutex.withLock {
        val scope = watchStore[token]
        TODO("unregister keyFilter")
    }

    fun onResourceCreatedEntity(key: K, entity: E) {
        val event = Event.Exists(key, entity, Event.Origin.RESOURCE_CREATED)
        watchStore.allScopes
            // TODO: filter interested scopes
            .forEach { it.token.events.tryEmit(event) }
    }

    fun onResourceModifiedEntity(key: K, entity: E) {
        val event = Event.Exists(key, entity, Event.Origin.RESOURCE_UPDATED)
        watchStore.allScopes
            // TODO: filter interested scopes
            .forEach { it.token.events.tryEmit(event) }
    }

    fun onResourceDeletedEntity(key: K) {
        val event = Event.Deleted<K, E>(key, Event.Origin.RESOURCE_DELETED)
        watchStore.allScopes
            // TODO: filter interested scopes
            .forEach { it.token.events.tryEmit(event) }
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

        suspend fun registerAll()
        suspend fun unregisterAll()
        suspend fun registerKeyFilter()
    }

    data class TokenImpl<K : Key, E : Entity<K>>(
        override val id: Int
    ) : Token<K, E>, StorableWatchToken<K, E> {
        lateinit var engine: EntityWatchEngine<K, E>
        override val events = MutableSharedFlow<Event<K, E>>(replay = 100)
        override var destroyed: Boolean = false

        override suspend fun registerAll() {
            engine.registerAll(this)
        }

        override suspend fun unregisterAll() {
            engine.unregisterAll(this)
        }

        override suspend fun registerKeyFilter() {
            TODO("Not yet implemented")
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
    }

    fun createKeyFilter(factoryFn: KeyFilterFactory.() -> Unit): KeyFilter {

    }

    interface KeyFilterFactory {

        suspend fun build(): KeyFilter
    }

    inner class KeyFilterFactoryImpl<K : Key>(
        val segmentFilters: List<Pattern>
    ) : KeyFilterFactory {

        override suspend fun build(): KeyFilter {
            val countOfVariableExtractors = resource.definition.path
                .count { it is PathPart.VariableExtractor<*, *> }
            check(countOfVariableExtractors == segmentFilters.size)
            var nextVariableExtractorIndex = 0
            return KeyFilter(
                id = mutex.withLock { nextKeyFilterId++ },
                pattern = resource.definition.path.joinToString { pathPart ->
                    when (pathPart) {
                        is PathPart.StaticExtractor<*> -> pathPart.regex.pattern()
                        is PathPart.VariableExtractor<*, *> -> {
                            val segmentFilterIndex = nextVariableExtractorIndex
                            segmentFilters[segmentFilterIndex]
                                .also { nextVariableExtractorIndex++ }
                                .pattern()
                        }

                        else -> throw IllegalStateException("Encountered pathPart that is neither a static nor variable extractor")
                    }
                }
                    .toPattern()
            )
        }
    }

    class KeyFilter(
        val id: Int,
        val pattern: Pattern
    )
}