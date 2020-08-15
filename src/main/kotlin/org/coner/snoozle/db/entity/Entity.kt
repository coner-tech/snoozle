package org.coner.snoozle.db.entity

import org.coner.snoozle.db.Key
import org.coner.snoozle.db.Record

interface Entity<K : Key> : Record<K>