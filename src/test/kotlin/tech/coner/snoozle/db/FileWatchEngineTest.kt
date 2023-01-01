package tech.coner.snoozle.db

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isSameAs
import assertk.assertions.key
import assertk.assertions.prop
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.WatchService
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.writeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FileWatchEngineTest : CoroutineScope {

    private lateinit var fileWatchEngine: TestFileWatchEngine

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
        fileWatchEngine = TestFileWatchEngine(
            coroutineContext = coroutineContext,
            root = root.asAbsolute()
        )
    }

    @AfterEach
    fun after() {
        runBlocking { fileWatchEngine.shutDown() }
        coroutineContext.cancel()
    }

    @Nested
    inner class InitialState {

        @Test
        fun `Its next scope ID should be integer minimum value initally`() {
            assertThat(fileWatchEngine).nextScopeId().isEqualTo(Int.MIN_VALUE)
        }

        @Test
        fun `It should not have any scopes initially`() {
            assertThat(fileWatchEngine).scopes().isEmpty()
        }

        @Test
        fun `It should not have any WatchService initially`() {
            assertThat(fileWatchEngine).service().isNull()
        }

        @Test
        fun `It should not have any poll loop initially`() {
            assertThat(fileWatchEngine).all {
                pollLoopScope().isNull()
                pollLoopJob().isNull()
            }
        }
    }

    @Nested
    inner class CreateToken {

        @Test
        fun `It should create token`() = runBlocking {
            val token = fileWatchEngine.createToken()

            assertThat(fileWatchEngine).all {
                nextScopeId().isEqualTo(Int.MIN_VALUE + 1)
                scopes().all {
                    hasSize(1)
                    key(token).all {
                        directoryPatterns().isEmpty()
                        filePatterns().isEmpty()
                        directoryWatchKeyEntries().isEmpty()
                    }
                }
                service().isNotNull()
                pollLoopScope().isNotNull()
                pollLoopJob().isNotNull()
            }
        }
    }

    @Nested
    inner class RegisterDirectoryPattern {

        @Test
        fun `It should register root directory`() = runBlocking {
            val token = fileWatchEngine.createToken()

            token.registerRootDirectory()

            assertThat(fileWatchEngine).scopes().all {
                hasSize(1)
                key(token).all {
                    directoryPatterns().containsExactly(FileWatchEngine.StandardPatterns.root)
                    filePatterns().isEmpty()
                    directoryWatchKeyEntries().all {
                        hasSize(1)
                        index(0).all {
                            absoluteDirectory().isEqualTo(root.asAbsolute())
                            relativeDirectory().isEqualTo(root.relativize(root).asRelative())
                            watchKey().isNotNull()
                            watchedSubdirectories().isEmpty()
                        }
                    }
                }
            }
        }

        @Test
        fun `It should unregister root directory`() {
            TODO()
        }
    }

    @Nested
    inner class RegisterFilePattern {

        @Test
        fun `It should register file pattern`() {
            TODO()
        }

        @Test
        fun `It should unregister file pattern`() {
            TODO()
        }

        @Test
        fun `When no directory pattern registered it should not permit to register file pattern`() {
            TODO()
        }
    }

    @Nested
    inner class EventFileCreatedEmissions {

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
        fun `When no directory pattern registered it should not emit when file created`() {
            TODO()
        }
    }

    @Nested
    inner class EventFileModifiedEmissions {

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

    }

    @Nested
    inner class EventFileDeletedEmissions {

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

private class TestFileWatchEngine(
    coroutineContext: CoroutineContext,
    root: AbsolutePath
) : FileWatchEngine(coroutineContext, root) {
    val testNextScopeId: Int
        get() = nextScopeId
    val testScopes: Map<TokenImpl, ScopeImpl>
        get() = scopes
    val testService: WatchService?
        get() = service
    val testPollLoopScope: CoroutineContext?
        get() = pollLoopScope
    val testPollLoopJob: Job?
        get() = pollLoopJob
}

private fun Assert<TestFileWatchEngine>.nextScopeId() = prop("nextScopeId") { it.testNextScopeId }
private fun Assert<TestFileWatchEngine>.scopes() = prop("scopes") { it.testScopes as Map<FileWatchEngine.Token, FileWatchEngine.Scope<FileWatchEngine.Scope.DirectoryWatchKeyEntry>> }
private fun Assert<TestFileWatchEngine>.service() = prop("service") { it.testService }
private fun Assert<TestFileWatchEngine>.pollLoopScope() = prop("pollLoopScope") { it.testPollLoopScope }
private fun Assert<TestFileWatchEngine>.pollLoopJob() = prop("pollLoopJob") { it.testPollLoopJob}

private fun Assert<FileWatchEngine.Scope<*>>.token() = prop(FileWatchEngine.Scope<*>::token)
private fun Assert<FileWatchEngine.Scope<*>>.directoryPatterns() = prop(FileWatchEngine.Scope<*>::directoryPatterns)
private fun Assert<FileWatchEngine.Scope<*>>.filePatterns() = prop(FileWatchEngine.Scope<*>::filePatterns)
private fun Assert<FileWatchEngine.Scope<*>>.directoryWatchKeyEntries() = prop(FileWatchEngine.Scope<*>::directoryWatchKeyEntries)

private fun Assert<FileWatchEngine.Scope.DirectoryWatchKeyEntry>.absoluteDirectory() = prop(FileWatchEngine.Scope.DirectoryWatchKeyEntry::absoluteDirectory)
private fun Assert<FileWatchEngine.Scope.DirectoryWatchKeyEntry>.relativeDirectory() = prop(FileWatchEngine.Scope.DirectoryWatchKeyEntry::relativeDirectory)
private fun Assert<FileWatchEngine.Scope.DirectoryWatchKeyEntry>.watchKey() = prop(FileWatchEngine.Scope.DirectoryWatchKeyEntry::watchKey)
private fun Assert<FileWatchEngine.Scope.DirectoryWatchKeyEntry>.watchedSubdirectories() = prop(FileWatchEngine.Scope.DirectoryWatchKeyEntry::watchedSubdirectories)