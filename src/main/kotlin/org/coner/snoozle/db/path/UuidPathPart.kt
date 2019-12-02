package org.coner.snoozle.db.path

import java.util.*

class UuidPathPart<E>(val extractor: (E) -> UUID) : PathPart<E>