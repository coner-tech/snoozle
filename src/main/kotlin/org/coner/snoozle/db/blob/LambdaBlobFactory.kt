package org.coner.snoozle.db.blob

import java.nio.file.Path

open class LambdaBlobFactory<B : Blob>(
        private val lambda: (Path) -> B
) : BlobFactory<B> {
    override fun factory(path: Path): B {
        return lambda.invoke(path)
    }
}
