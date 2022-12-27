package tech.coner.snoozle.db.session

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.coner.snoozle.db.metadata.MetadataRepository
import java.io.Closeable
import java.util.*

abstract class Session internal constructor(
    val id: UUID,
    protected val metadataRepository: MetadataRepository,
    private val onClose: (sessionToClose: Session) -> Unit
) : Closeable {

    private val closedMutex = Mutex()
    var closed: Boolean = false
        private set

    override fun close() = execute {
        onClose(this)
        closed = true
    }
        .getOrThrow()

    protected fun <T> execute(fn: () -> T): Result<T> = runBlocking {
        closedMutex.withLock {
            if (closed) {
                Result.failure<T>(SessionException.AlreadyClosed())
            }
            try {
                fn()
                    .let { Result.success(it) }
            } catch (t: Throwable) {
                Result.failure(t)
            }
        }
    }
}