package tech.coner.snoozle.db

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.nio.file.Files
import java.nio.file.Path
import java.util.regex.Pattern
import kotlin.io.path.writeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

@OptIn(ExperimentalCoroutinesApi::class)
class FileWatchEngineTest : CoroutineScope {

    lateinit var fileWatchEngine: FileWatchEngine

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

    private val defaultTimeoutMillis = 250L

    @BeforeEach
    fun before() {
        fileWatchEngine = FileWatchEngine(
            coroutineContext = coroutineContext,
            root = root
        )
    }

    @AfterEach
    fun after() {
        runBlocking { fileWatchEngine.shutDown() }
        coroutineContext.cancel()
    }

    @Test
    fun `It should emit record exists when matching file created`() = runBlocking {
        val token = fileWatchEngine.createToken()
        token.registerRootDirectory()
        token.registerFilePattern(rootAnyTxtPattern)

        launch { rootFileDotTxt.absolute.value.writeText("text") }
        val event = withTimeout(defaultTimeoutMillis) { token.events.first() }

        assertThat(event)
            .isRecordExistsInstance()
            .record()
            .isEqualTo(rootFileDotTxt.relative)
    }

    @Test
    fun `It should not emit when non matching file created`() = runBlocking {
        val token = fileWatchEngine.createToken()
        token.registerRootDirectory()
        token.registerFilePattern(rootAnyTxtPattern)

        launch { rootFileDotJson.absolute.value.writeText("""{ "json": "string" }""") }
        val event = withTimeoutOrNull(defaultTimeoutMillis) { token.events.first() }

        assertThat(event).isNull()
    }

    @Test
    fun `It should emit when matching file modified`() = runBlocking {
        rootFileDotTxt.absolute.value.writeText("text")
        val token = fileWatchEngine.createToken()
        token.registerRootDirectory()
        token.registerFilePattern(rootAnyTxtPattern)

        launch { rootFileDotTxt.absolute.value.writeText("changed content") }
        val event = withTimeout(defaultTimeoutMillis) { token.events.first() }

        assertThat(event)
            .isRecordExistsInstance()
            .record()
            .isEqualTo(rootFileDotTxt.relative)
    }

    @Test
    fun `It should not emit when non-matching file modified`() = runBlocking {
        rootFileDotJson.absolute.value.writeText("""{ "json": "string" }""")
        val token = fileWatchEngine.createToken()
        token.registerRootDirectory()
        token.registerFilePattern(rootAnyTxtPattern)

        launch { rootFileDotJson.absolute.value.writeText("""{ "json": null }""") }
        val event = withTimeoutOrNull(defaultTimeoutMillis) { token.events.first() }

        assertThat(event).isNull()
    }

    @Test
    fun `It should emit when matching file deleted`() = runBlocking {
        rootFileDotTxt.absolute.value.writeText("text")
        val token = fileWatchEngine.createToken()
        token.registerRootDirectory()
        token.registerFilePattern(rootAnyTxtPattern)

        launch {
            withContext(Dispatchers.IO) {
                Files.delete(rootFileDotTxt.absolute.value)
            }
        }
        val event = withTimeout(defaultTimeoutMillis) { token.events.first() }

        assertThat(event)
            .isRecordDoesNotExistsInstance()
            .record()
            .isEqualTo(rootFileDotTxt.relative)
    }

    @Test
    fun `It should not emit when non-matching file deleted`() = runBlocking {
        rootFileDotJson.absolute.value.writeText("""{ "json": "string" }""")
        val token = fileWatchEngine.createToken()
        token.registerRootDirectory()
        token.registerFilePattern(rootAnyTxtPattern)

        launch {
            withContext(Dispatchers.IO) {
                Files.delete(rootFileDotJson.absolute.value)
            }
        }
        val event = withTimeoutOrNull(defaultTimeoutMillis) { token.events.first() }

        assertThat(event).isNull()
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
