package org.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.coner.snoozle.db.blob.BlobResource
import org.coner.snoozle.db.sample.GadgetPhoto
import org.coner.snoozle.db.sample.SampleDatabase
import org.coner.snoozle.db.sample.SampleDb
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.streams.toList

class GadgetPhotoIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var database: SampleDatabase
    private lateinit var resource: BlobResource<GadgetPhoto>

    @BeforeEach
    fun before() {
        database = SampleDb.factory(root)
        resource = database.blob()
    }

    @Test
    fun `It should stream GadgetPhotos`() {
        val actualGadgetPhotos = resource.stream().toList()

        assertThat(actualGadgetPhotos).all {
            index(0).all {
                prop(GadgetPhoto::id).isEqualTo("close-up-photography-of-smartphone-beside-binder-clip-1841841")
                prop(GadgetPhoto::extension).isEqualTo("jpg")
            }
            index(1).all {
                prop(GadgetPhoto::id).isEqualTo("view-of-vintage-camera-325153")
                prop(GadgetPhoto::extension).isEqualTo("jpg")
            }
            hasSize(2)
        }

        val images = actualGadgetPhotos
                .map { resource.getAsInputStream(it) }
                .map { ImageIO.read(it) }
        assertThat(images).all {
            index(0).all {
                transform { it.width }.isEqualTo(640)
                transform { it.height }.isEqualTo(426)
            }
            index(1).all {
                transform { it.width }.isEqualTo(640)
                transform { it.height }.isEqualTo(345)
            }
        }

        val actualGadgetPhotoZeroPath = resource.getAbsolutePathTo(actualGadgetPhotos[0])
        assertThat(actualGadgetPhotoZeroPath).all {
            toStringFun().all {
                startsWith(root.toString())
                endsWith("/gadgets/${actualGadgetPhotos[0].gadgetId}/photos/${actualGadgetPhotos[0].id}.${actualGadgetPhotos[0].extension}")
            }
            isRegularFile()
            isReadable()
        }

        val actualGadgetPhotoOnePath = resource.getAbsolutePathTo(actualGadgetPhotos[1])
        assertThat(actualGadgetPhotoOnePath).all {
            toStringFun().all {
                startsWith(root.toString())
                endsWith("/gadgets/${actualGadgetPhotos[1].gadgetId}/photos/${actualGadgetPhotos[1].id}.${actualGadgetPhotos[1].extension}")
            }
            isRegularFile()
            isReadable()
        }

    }

}