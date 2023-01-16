package tech.coner.snoozle.db.watch

import kotlinx.coroutines.Job
import tech.coner.snoozle.db.path.AbsolutePath
import java.nio.file.WatchKey
import java.nio.file.WatchService
import kotlin.coroutines.CoroutineContext

class TestFileWatchEngine(
    coroutineContext: CoroutineContext,
    root: AbsolutePath
) : FileWatchEngine(coroutineContext, root) {
    var testWatchServiceFactoryFn: (AbsolutePath) -> WatchService = watchServiceFactoryFn
        get() = watchServiceFactoryFn
        set(value) {
            watchServiceFactoryFn = value
            field = value
        }
    var testWatchKeyFactoryFn: (AbsolutePath, WatchService) -> WatchKey = watchKeyFactoryFn
        get() = watchKeyFactoryFn
        set(value) {
            watchKeyFactoryFn = value
            field = value
        }
    var testWatchStoreFactoryFn: () -> WatchStore<TokenImpl, Scope> = watchStoreFactoryFn
        get() = watchStoreFactoryFn
        set(value) {
            watchStoreFactoryFn = value
            field = value
        }
    val testWatchStore: TestWatchStore<TokenImpl, Scope>
        get() = watchStore as TestWatchStore<TokenImpl, Scope>
    val testService: WatchService?
        get() = service
    val testPollLoopScope: CoroutineContext?
        get() = pollLoopScope
    val testPollLoopJob: Job?
        get() = pollLoopJob
}