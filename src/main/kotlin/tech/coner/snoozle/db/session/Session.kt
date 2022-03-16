package tech.coner.snoozle.db.session

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.coner.snoozle.db.metadata.MetadataRepository
import java.util.*

abstract class Session internal constructor(
    val id: UUID,
    protected val metadataRepository: MetadataRepository,
    private val onClose: (sessionToClose: Session) -> Unit
) {

    private val closedMutex = Mutex()
    var closed: Boolean = false
        private set

    fun close() = execute {
        onClose(this)
        closed = true
    }

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