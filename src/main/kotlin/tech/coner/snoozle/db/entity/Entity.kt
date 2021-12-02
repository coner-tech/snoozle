package tech.coner.snoozle.db.entity

import tech.coner.snoozle.db.Key
import tech.coner.snoozle.db.Record

interface Entity<K : Key> : Record<K>