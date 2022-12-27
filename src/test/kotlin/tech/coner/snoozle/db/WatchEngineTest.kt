package tech.coner.snoozle.db

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
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

    private val rootTxtPattern by lazy { Pattern.compile("^\\w*.txt$") }
    private val rootTxtFile by lazy { root.resolve("file.txt") }
    private val rootJsonFile by lazy { root.resolve("file.json") }
    private val subfolderTxtPattern by lazy { Pattern.compile("^subfolder/\\w*.txt$") }
    private val subfolder by lazy { root.resolve("subfolder") }
    private val subfolderTxtFile by lazy { subfolder.resolve("file.txt") }
    private val subfolderJsonFile by lazy { subfolderTxtFile.resolve("file.json") }

    @BeforeEach
    fun before() {
        watchEngine = WatchEngine(
            coroutineContext = coroutineContext,
            root = root
        )
    }

    @AfterEach
    fun after() = runTest {
        watchEngine.shutDown()
        cancel()
    }

    @Test
    fun `It should emit when matching file created`() = runBlocking {
        val token = watchEngine.createToken()
        val channel = Channel<WatchEngine.Event>()
        token.events.onEach { channel.send(it) }
        watchEngine.registerRootDirectory(token)
        watchEngine.registerRecordPattern(token, rootTxtPattern)
        launch {
            delay(500)
            rootTxtFile.writeText("text")
        }
        val event = withTimeout(1000000) { channel.receive() }
        assertThat(event)
            .isRecordExistsInstance()
            .record()
            .isEqualTo(
                WatchEngine.Event.Record.Exists(rootTxtFile)
            )
    }

    @Test
    fun `It should not emit when non-matching file created`() {
        TODO()
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
}