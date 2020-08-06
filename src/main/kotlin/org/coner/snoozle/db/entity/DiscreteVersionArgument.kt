package org.coner.snoozle.db.entity

sealed class DiscreteVersionArgument {

    abstract val value: String

    class Specific(version: Int) : DiscreteVersionArgument() {
        override val value = version.toString()
    }
    object Highest : DiscreteVersionArgument() {
        override val value = "DiscreteVersionArgument.Highest"
    }
    object New : DiscreteVersionArgument() {
        override val value = "DiscreteVersionArgument.New"
    }
}