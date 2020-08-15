package org.coner.snoozle.db

interface Record<K : Key> {
    val key: K
}