package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key

interface VersionedEntity<K : Key> : Entity<K> {
}