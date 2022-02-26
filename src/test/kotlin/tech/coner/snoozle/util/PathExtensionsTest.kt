package tech.coner.snoozle.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class PathExtensionsTest {

    @Test
    fun `It should resolve array of paths`(@TempDir dir: Path) {
        val actual = dir.resolve("foo", "bar", "baz")

        val expected = dir.resolve("foo").resolve("bar").resolve("baz")
        assertThat(actual).isEqualTo(expected)
    }
}