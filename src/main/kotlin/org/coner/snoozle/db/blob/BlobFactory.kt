package org.coner.snoozle.db.blob

import java.nio.file.Path

interface BlobFactory<B : Blob> {

    fun factory(path: Path): B
}