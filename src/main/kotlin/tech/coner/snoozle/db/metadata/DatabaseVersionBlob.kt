package tech.coner.snoozle.db.metadata

import tech.coner.snoozle.db.blob.Blob
import tech.coner.snoozle.db.blob.BlobResource

object DatabaseVersionBlob : Blob

typealias DatabaseVersionResource = BlobResource<DatabaseVersionBlob>