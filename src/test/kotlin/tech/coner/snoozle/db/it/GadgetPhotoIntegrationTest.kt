package tech.coner.snoozle.db.it

import assertk.all
import assertk.assertThat
import assertk.assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.blob.BlobResource
import tech.coner.snoozle.db.closeAndAssertSuccess
import tech.coner.snoozle.db.sample.GadgetPhoto
import tech.coner.snoozle.db.sample.SampleDatabaseFixture
import tech.coner.snoozle.db.session.data.DataSession
import java.nio.file.Path
import javax.imageio.ImageIO

class GadgetPhotoIntegrationTest {

    @TempDir
    lateinit var root: Path

    private lateinit var session: DataSession
    private lateinit var resource: BlobResource<GadgetPhoto>

    @BeforeEach
    fun before() {
        session = SampleDatabaseFixture
            .factory(
                root = root,
                version = SampleDatabaseFixture.VERSION_HIGHEST
            )
            .openDataSession()
            .getOrThrow()
        resource = session.blob()
    }

    @AfterEach
    fun after() {
        session.closeAndAssertSuccess()
    }

    @Test
    fun `It should stream GadgetPhotos`() {
        val actualGadgetPhotos = resource.stream()
                .toList()
                .sortedBy { it.id }

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
        assertThat(actualGadgetPhotoZeroPath.value).all {
            toStringFun().all {
                startsWith(root.toString())
                endsWith("/gadgets/${actualGadgetPhotos[0].gadgetId}/photos/${actualGadgetPhotos[0].id}.${actualGadgetPhotos[0].extension}")
            }
            isRegularFile()
            isReadable()
        }

        val actualGadgetPhotoOnePath = resource.getAbsolutePathTo(actualGadgetPhotos[1])
        assertThat(actualGadgetPhotoOnePath.value).all {
            toStringFun().all {
                startsWith(root.toString())
                endsWith("/gadgets/${actualGadgetPhotos[1].gadgetId}/photos/${actualGadgetPhotos[1].id}.${actualGadgetPhotos[1].extension}")
            }
            isRegularFile()
            isReadable()
        }

    }

}