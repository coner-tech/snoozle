package tech.coner.snoozle.db.watch

sealed class Event {

    enum class Origin {
        WATCH,
        NEW_DIRECTORY_SCAN
    }

    interface Exists

    sealed class Record<R> : Event() {
        abstract val record: R
        abstract val origin: Origin
    }

    data class Created<R>(
        override val record: R,
        override val origin: Origin
    ) : Record<R>(), Exists

    data class Modified<R>(
        override val record: R,
        override val origin: Origin
    ) : Record<R>(), Exists

    data class Deleted<R>(
        override val record: R,
        override val origin: Origin
    ) : Record<R>()

    object Overflow : Event()
}