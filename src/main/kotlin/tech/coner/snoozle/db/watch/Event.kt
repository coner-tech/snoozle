package tech.coner.snoozle.db.watch

sealed class Event<ID : Any, C : Any> {

    abstract val origin: Origin

    enum class Origin {
        WATCH,
        NEW_DIRECTORY_SCAN
    }

    sealed class Record<ID : Any, C : Any> : Event<ID, C>() {
        abstract val recordId: ID
    }

    data class Exists<ID : Any, C : Any>(
        override val recordId: ID,
        val recordContent: C,
        override val origin: Origin
    ) : Record<ID, C>()

    data class Deleted<ID : Any, C : Any>(
        override val recordId: ID,
        override val origin: Origin
    ) : Record<ID, C>()

    class Overflow<ID : Any, C : Any> : Event<ID, C>() {
        override val origin = Origin.WATCH
    }
}