package tech.coner.snoozle.db

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.writeText

@OptIn(ExperimentalCoroutinesApi::class)
class WatchEngineTest : CoroutineScope {

    lateinit var watchEngine: WatchEngine

    @TempDir lateinit var root: Path

    override val coroutineContext = Dispatchers.IO + Job()

    private val rootAnyTxtPattern by lazy { Pattern.compile("^\\w*.txt$") }
    private val rootFileDotTxt by lazy { testPath(root.resolve("file.txt")) }
    private val rootNotFileDotTxt by lazy { testPath(root.resolve("notfile.txt")) }
    private val rootFileDotJson by lazy { testPath(root.resolve("file.json")) }
    private val subfolderAnyTxtPattern by lazy { Pattern.compile("^subfolder/\\w*.txt$") }
    private val subfolder by lazy { root.resolve("subfolder") }
    private val subfolderFileDotTxt by lazy { testPath(subfolder.resolve("file.txt")) }
    private val subfolderFileDotJson by lazy { testPath(subfolder.resolve("file.json")) }

    @BeforeEach
    fun before() {
        watchEngine = WatchEngine(
            coroutineContext = coroutineContext,
            root = root
        )
    }

    @AfterEach
    fun after() {
        runBlocking { watchEngine.shutDown() }
        coroutineContext.cancel()
    }

    @Test
    fun `It should emit record exists when matching file created`() = runBlocking {
        val token = watchEngine.createToken()
        watchEngine.registerRootDirectory(token)
        watchEngine.registerRecordPattern(token, rootAnyTxtPattern)

        launch { rootFileDotTxt.absolute.value.writeText("text") }
        val event = withTimeout(1000) { token.events.first() }

        assertThat(event)
            .isRecordExistsInstance()
            .record()
            .isEqualTo(rootFileDotTxt.relative)
    }

    @Test
    fun `It should not emit when non matching file created`() = runBlocking {
        val token = watchEngine.createToken()
        watchEngine.registerRootDirectory(token)
        watchEngine.registerRecordPattern(token, rootAnyTxtPattern)

        launch { rootFileDotJson.absolute.value.writeText("""{ "json": "object" }""") }
        val event = withTimeoutOrNull(1000) { token.events.firstOrNull() }

        
    }

    @Test
    fun `It should emit when matching file modified`() {
        TODO()
    }

    @Test
    fun `It should not emit when non-matching file modified`() {
        TODO()
    }

    @Test
    fun `It should emit when matching file deleted`() {
        TODO()
    }

    @Test
    fun `It should not emit when non-matching file deleted`() {
        TODO()
    }

    private data class TestPath(
        val absolute: AbsolutePath,
        val relative: RelativePath
    )

    private fun testPath(path: Path): TestPath {
        require(path.isAbsolute) { "Must be called with absolute path but was: $path" }
        return TestPath(
            absolute = path.asAbsolute(),
            relative = root.relativize(path).asRelative()
        )
    }
}
