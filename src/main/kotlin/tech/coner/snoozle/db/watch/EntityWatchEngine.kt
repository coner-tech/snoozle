package tech.coner.snoozle.db.watch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.coner.snoozle.db.Key
import tech.coner.snoozle.db.entity.Entity
import tech.coner.snoozle.db.path.Pathfinder
import kotlin.coroutines.CoroutineContext

class EntityWatchEngine<K : Key, E : Entity<K>>(
    override val coroutineContext: CoroutineContext,
    private val fileWatchEngine: FileWatchEngine,
    private val pathfinder: Pathfinder<K, E>
) : CoroutineScope {

    private val mutex = Mutex()

    protected var watchStoreFactoryFn: () -> WatchStore<TokenImpl<K, E>, Scope<K, E>> = {
        WatchStore()
    }
    protected val watchStore: WatchStore<TokenImpl<K, E>, Scope<K, E>> by lazy {
        watchStoreFactoryFn()
    }

    suspend fun createToken(): Token<K, E> = mutex.withLock {
        val (token, _) = watchStore.create(
            tokenFactory = { id -> TokenImpl(id) },
            scopeFactory = { token ->
                Scope(
                    coroutineContext = coroutineContext + Job(),
                    token = token
                )
                    .also { scope ->
                        token.engine = this
                        scope.fileWatchEngineToken = fileWatchEngine.createToken()
                            .also { fileWatchEngineToken ->
                                fileWatchEngineToken.registerRootDirectory()
                                fileWatchEngineToken.events
                                    .onEach {
                                        mutex.withLock {
                                            handleFileWatchEvent(scope, it)
                                        }
                                    }
                                    .launchIn(scope)
                            }
                    }
            }
        )
        token
    }

    private suspend fun handleFileWatchEvent(scope: Scope<K, E>, event: Event) {
        when (event) {
            is Event.Record<*> -> handleRecordEvent(scope, event)
            is Event.Overflow -> handleOverflowEvent(scope, event)
        }
    }

    private suspend fun handleRecordEvent(scope: Scope<K, E>, event: Event.Record<*>) {
        TODO()
    }

    private suspend fun handleOverflowEvent(scope: Scope<K, E>, event: Event.Overflow) {
        TODO()
    }

    suspend fun registerAll(token: TokenImpl<K, E>) = mutex.withLock {
        val fileWatchEngineToken = watchStore[token].fileWatchEngineToken
        fileWatchEngineToken.registerDirectoryPattern(pathfinder.recordParentCandidatePath)
        fileWatchEngineToken.registerDirectoryPattern(pathfinder.recordParentCandidatePath)
    }
    
    suspend fun unregisterAll(token: TokenImpl<K, E>) = mutex.withLock { 
        watchStore[token]
            .fileWatchEngineToken
            .unregisterDirectoryPattern(pathfinder.recordParentCandidatePath)
    }

    suspend fun destroyToken(token: TokenImpl<K, E>) = mutex.withLock {
        watchStore.destroy(token) {
            it.cancel()
            it.fileWatchEngineToken.destroy()
        }
    }

    interface Token<K : Key, E : Entity<K>> : WatchToken {

        suspend fun registerAll()
        suspend fun unregisterAll()
    }

    data class TokenImpl<K : Key, E : Entity<K>>(
        override val id: Int
    ) : Token<K, E>, StorableWatchToken {
        lateinit var engine: EntityWatchEngine<K, E>
        override val events = MutableSharedFlow<Event>()
        override var destroyed: Boolean = false

        override suspend fun registerAll() {
            engine.registerAll(this)
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
    ) : StorableWatchScope<TokenImpl<K, E>>,
        CoroutineScope by CoroutineScope(coroutineContext) {
        lateinit var fileWatchEngineToken: FileWatchEngine.Token
    }

}