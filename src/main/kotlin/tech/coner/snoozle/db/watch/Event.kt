package tech.coner.snoozle.db.watch

sealed class Event {

    enum class Origin {
        WATCH,
        NEW_DIRECTORY_SCAN
    }

    interface Exists

    interface Record<R> {
        val record: R
        val origin: Origin
    }

    data class Created<R>(
        override val record: R,
        override val origin: Origin
    ) : Event(), Record<R>, Exists

    data class Modified<R>(
        override val record: R,
        override val origin: Origin
    ) : Event(), Record<R>, Exists

    data class Deleted<R>(
        override val record: R,
        override val origin: Origin
    ) : Event(), Record<R>

    object Overflow : Event()
}