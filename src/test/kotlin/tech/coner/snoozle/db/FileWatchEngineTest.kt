package tech.coner.snoozle.db

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.exactly
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.key
import assertk.assertions.prop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.WatchService
import java.util.regex.Pattern
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.createDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.writeText

class FileWatchEngineTest : CoroutineScope {

    private lateinit var fileWatchEngine: TestFileWatchEngine

    @TempDir lateinit var root: Path

    override val coroutineContext = Dispatchers.IO + Job()

    private val rootAnyTxtPattern by lazy { Pattern.compile("^\\w*.txt$") }
    private val rootFileDotTxt by lazy { testPath(root.resolve("file.txt")) }
    private val rootNotFileDotTxt by lazy { testPath(root.resolve("notfile.txt")) }
    private val rootFileDotJson by lazy { testPath(root.resolve("file.json")) }
    private val subfolder1 by lazy { root.resolve("subfolder1") }
    private val subfolderDirectoryPattern by lazy { Pattern.compile("^subfolder\\d+$") }
    private val subfolderAnyTxtPattern by lazy { Pattern.compile("^subfolder\\d+/\\w*.txt$") }
    private val subfolder1FileDotTxt by lazy { testPath(subfolder1.resolve("file.txt")) }
    private val subfolder1FileDotJson by lazy { testPath(subfolder1.resolve("file.json")) }
    private val subfolder2 by lazy { root.resolve("subfolder2") }
    private val subfolder2FileDotTxt by lazy { testPath(subfolder2.resolve("file.txt")) }

    private val defaultTimeoutMillis: Long = 250

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
        fun `Its next token ID should be integer minimum value initially`() {
            assertThat(fileWatchEngine).nextTokenId().isEqualTo(Int.MIN_VALUE)
        }

        @Test
        fun `Its destroyed token ID ranges should be empty initially`() {
            assertThat(fileWatchEngine).destroyedTokenIdRanges().isEmpty()
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
                nextTokenId().isEqualTo(Int.MIN_VALUE + 1)
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

        @Test
        fun `It should create a second concurrent token`() = runBlocking {
            repeat(2) {
                fileWatchEngine.createToken()
            }

            assertThat(fileWatchEngine).all {
                nextTokenId().isEqualTo(Int.MIN_VALUE + 2)
            }
        }

        @Test
        fun `It should create a second sequential token`() {
            runBlocking {
                repeat(2) {
                    val token = fileWatchEngine.createToken()
                    fileWatchEngine.destroyToken(token)
                }
            }

            assertThat(fileWatchEngine).all {
                nextTokenId().isEqualTo(Int.MIN_VALUE + 1)
                destroyedTokenIdRanges().all {
                    hasSize(1)
                    exactly(1) { it.isEqualTo(Int.MIN_VALUE..(Int.MIN_VALUE)) }
                }
            }
        }
    }

    @Nested
    inner class DestroyToken {

        @Test
        fun `It should destroy sole token`() = runBlocking {
            val token = fileWatchEngine.createToken()

            fileWatchEngine.destroyToken(token)

            assertThat(fileWatchEngine).all {
                scopes().isEmpty()
                service().isNull()
                pollLoopScope().isNull()
                pollLoopJob().isNull()
                destroyedTokenIdRanges().all {
                    hasSize(1)
                    exactly(1) { it.isEqualTo(Int.MIN_VALUE..Int.MIN_VALUE) }
                }
                nextTokenId().isEqualTo(Int.MIN_VALUE + 1)
            }
        }

        @Test
        fun `It should destroy one of many tokens`() = runBlocking {
            val token1 = fileWatchEngine.createToken()
            val token2 = fileWatchEngine.createToken()
            val token3 = fileWatchEngine.createToken()

            fileWatchEngine.destroyToken(token1)

            assertThat(fileWatchEngine).all {
                scopes().all {
                    hasSize(2)
                    key(token2).all {
                        directoryWatchKeyEntries().isEmpty()
                    }
                    key(token3).all {
                        directoryWatchKeyEntries().isEmpty()
                    }
                }
                service().isNotNull()
                pollLoopScope().isNotNull()
                pollLoopJob().isNotNull()
                destroyedTokenIdRanges().all {
                    hasSize(1)
                    exactly(1) { it.isEqualTo(Int.MIN_VALUE..Int.MIN_VALUE) }
                }
                nextTokenId().isEqualTo(Int.MIN_VALUE + 3)
            }
        }

        @Test
        fun `It should destroy two sequential tokens into single range`() = runBlocking {
            val token1 = fileWatchEngine.createToken()
            val token2 = fileWatchEngine.createToken()

            fileWatchEngine.destroyToken(token1)
            fileWatchEngine.destroyToken(token2)

            assertThat(fileWatchEngine).all {
                scopes().isEmpty()
                service().isNull()
                pollLoopScope().isNull()
                pollLoopJob().isNull()
                destroyedTokenIdRanges().all {
                    hasSize(1)
                    exactly(1) { it.isEqualTo(Int.MIN_VALUE..(Int.MIN_VALUE + 1)) }
                }
                nextTokenId().isEqualTo(Int.MIN_VALUE + 2)
            }
        }

        @Test
        fun `It should destroy last token created`() = runBlocking {
            val token1 = fileWatchEngine.createToken()
            val token2 = fileWatchEngine.createToken()
            val token3 = fileWatchEngine.createToken()

            fileWatchEngine.destroyToken(token3)

            assertThat(fileWatchEngine).all {
                destroyedTokenIdRanges().all {
                    hasSize(1)
                    val expectedTokenId = Int.MIN_VALUE + 2
                    exactly(1) { it.isEqualTo(expectedTokenId..expectedTokenId) }
                }
            }
        }

        @Test
        fun `It should destroy intermediate token`() = runBlocking {
            val token1 = fileWatchEngine.createToken()
            val token2 = fileWatchEngine.createToken()
            val token3 = fileWatchEngine.createToken()

            fileWatchEngine.destroyToken(token2)

            assertThat(fileWatchEngine).all {
                destroyedTokenIdRanges().all {
                    hasSize(1)
                    val expectedTokenId = Int.MIN_VALUE + 1
                    exactly(1) { it.isEqualTo(expectedTokenId..expectedTokenId) }
                }
            }
        }

        @Test
        fun `It should append destroyed token ID into destroyed range`() = runBlocking {
            val token1 = fileWatchEngine.createToken()
            val token2 = fileWatchEngine.createToken()

            fileWatchEngine.destroyToken(token1)
            fileWatchEngine.destroyToken(token2)

            assertThat(fileWatchEngine).all {
                destroyedTokenIdRanges().all {
                    hasSize(1)
                    exactly(1) { it.isEqualTo(Int.MIN_VALUE..(Int.MIN_VALUE + 1)) }
                }
            }
        }

        @Test
        fun `It should prepend destroyed token into destroyed range`() = runBlocking {
            val token1 = fileWatchEngine.createToken()
            val token2 = fileWatchEngine.createToken()

            fileWatchEngine.destroyToken(token2)
            fileWatchEngine.destroyToken(token1)

            assertThat(fileWatchEngine).all {
                destroyedTokenIdRanges().all {
                    hasSize(1)
                    exactly(1) { it.isEqualTo(Int.MIN_VALUE..(Int.MIN_VALUE + 1)) }
                }
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
        fun `It should unregister root directory`() = runBlocking {
            val token = fileWatchEngine.createToken()
            token.registerRootDirectory()

            token.unregisterRootDirectory()

            assertThat(fileWatchEngine).scopes().all {
                hasSize(1)
                key(token).all {
                    directoryPatterns().isEmpty()
                    filePatterns().isEmpty()
                    directoryWatchKeyEntries().isEmpty()
                }
            }
        }

        @Test
        fun `It should register arbitrary directory pattern when no matching directories exist`() = runBlocking {
            val token = fileWatchEngine.createToken()
            token.registerDirectoryPattern(subfolderDirectoryPattern)

            assertThat(fileWatchEngine).scopes().all {
                hasSize(1)
                key(token).all {
                    directoryPatterns().containsExactly(subfolderDirectoryPattern)
                    filePatterns().isEmpty()
                    directoryWatchKeyEntries().isEmpty()
                }
            }
        }

        @Test
        fun `It should register arbitrary directory pattern when matching subdirectories exist`() = runBlocking {
            subfolder1.createDirectory()
            val token = fileWatchEngine.createToken()
            token.registerDirectoryPattern(subfolderDirectoryPattern)

            assertThat(fileWatchEngine).scopes().all {
                hasSize(1)
                key(token).all {
                    directoryPatterns().containsExactly(subfolderDirectoryPattern)
                    filePatterns().isEmpty()
                    directoryWatchKeyEntries().all {
                        hasSize(1)
                        index(0).all {
                            absoluteDirectory().isEqualTo(subfolder1.asAbsolute())
                            relativeDirectory().isEqualTo(root.relativize(subfolder1).asRelative())
                            watchKey().isNotNull()
                            watchedSubdirectories().isEmpty()
                        }
                    }
                }
            }
        }

        @Test
        fun `It should unregister arbitrary directory pattern for matching directory does not exist`() = runBlocking {
            val token = fileWatchEngine.createToken()
            token.registerDirectoryPattern(subfolderDirectoryPattern)

            token.unregisterDirectoryPattern(subfolderDirectoryPattern)

            assertThat(fileWatchEngine).scopes().all {
                hasSize(1)
                key(token).all {
                    directoryPatterns().isEmpty()
                    filePatterns().isEmpty()
                    directoryWatchKeyEntries().isEmpty()
                }
            }
        }

        @Test
        fun `It should unregister arbitrary directory pattern with matching directory exists`() = runBlocking {
            subfolder1.createDirectory()
            val token = fileWatchEngine.createToken()
            token.registerDirectoryPattern(subfolderDirectoryPattern)
            token.unregisterDirectoryPattern(subfolderDirectoryPattern)

            assertThat(fileWatchEngine).scopes().all {
                hasSize(1)
                key(token).all {
                    directoryPatterns().isEmpty()
                    filePatterns().isEmpty()
                    directoryWatchKeyEntries().isEmpty()
                }
            }
        }

        @Test
        fun `When it registers a directory pattern matching a subdirectory of another registered directory it should add watched subdirectory to parent entry`() = runBlocking {
            subfolder1.createDirectory()
            val token = fileWatchEngine.createToken()
            token.registerDirectoryPattern(subfolderDirectoryPattern)
            token.registerRootDirectory()

            assertThat(fileWatchEngine).scopes().all {
                hasSize(1)
                key(token).all {
                    directoryPatterns().all {
                        hasSize(2)
                        index(0).isEqualTo(subfolderDirectoryPattern)
                        index(1).isEqualTo(FileWatchEngine.StandardPatterns.root)
                    }
                    filePatterns().isEmpty()
                    directoryWatchKeyEntries().all {
                        hasSize(2)
                        index(0).all {
                            absoluteDirectory().isEqualTo(subfolder1.asAbsolute())
                            watchKey().isNotNull()
                            watchedSubdirectories().isEmpty()
                        }
                        index(1).all {
                            absoluteDirectory().isEqualTo(root.asAbsolute())
                            watchKey().isNotNull()
                            watchedSubdirectories().all {
                                hasSize(1)
                                exactly(1) {
                                    it.absolutePath().isEqualTo(subfolder1.asAbsolute())
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Nested
    inner class RegisterFilePattern {

        @Test
        fun `It should register file pattern`() = runBlocking {
            val token = fileWatchEngine.createToken()
            token.registerRootDirectory()

            token.registerFilePattern(rootAnyTxtPattern)

            assertThat(fileWatchEngine)
                .scopes()
                .key(token)
                .filePatterns().isEqualTo(listOf(rootAnyTxtPattern))
        }

        @Test
        fun `It should throw when registering duplicate file pattern`() = runBlocking {
            val token = fileWatchEngine.createToken()
            token.registerRootDirectory()
            token.registerFilePattern(rootAnyTxtPattern) // first invocation ok

            val actual = assertThrows<IllegalArgumentException> { token.registerFilePattern(rootAnyTxtPattern) }

            assertThat(actual).hasMessage("Scope already has file pattern: $rootAnyTxtPattern")
        }

        @Test
        fun `When no directory pattern registered it should not permit to register file pattern`() = runBlocking {
            val token = fileWatchEngine.createToken()

            val actual = assertThrows<IllegalStateException> { token.registerFilePattern(rootAnyTxtPattern) }

            assertThat(actual).hasMessage("Scope must have a directory pattern registered")
        }

        @Test
        fun `It should unregister file pattern`() = runBlocking {
            val token = fileWatchEngine.createToken()
            token.registerRootDirectory()
            token.registerFilePattern(rootAnyTxtPattern)

            token.unregisterFilePattern(rootAnyTxtPattern)

            assertThat(fileWatchEngine)
                .scopes()
                .key(token)
                .filePatterns().isEmpty()
        }

        @Test
        fun `When no directory pattern registered it should not permit to unregister file pattern`() = runBlocking {
            val token = fileWatchEngine.createToken()

            val actual = assertThrows<IllegalStateException> { token.unregisterFilePattern(rootAnyTxtPattern) }

            assertThat(actual).hasMessage("Scope must have a directory pattern registered")
        }
    }

    @Nested
    inner class DirectoryCreated {

        @Test
        fun `When new watched directory created it should scan and emit files created`() = runBlocking {
            val token = fileWatchEngine.createToken()
            token.registerRootDirectory()
            token.registerDirectoryPattern(subfolderDirectoryPattern)
            token.registerFilePattern(subfolderAnyTxtPattern)

            subfolder1.createDirectory()
            subfolder1FileDotTxt.absolute.value.writeText("text")
            val event = withTimeout(defaultTimeoutMillis) { token.events.first() }

            assertThat(event)
                .isFileExistsInstance()
                .all {
                    file().isEqualTo(subfolder1FileDotTxt.relative)
                    origin().isEqualTo(FileWatchEngine.Event.File.Origin.NEW_DIRECTORY_SCAN)
                }
        }
    }

    @Nested
    inner class DirectoryDeleted {

        @Test
        fun `When watched directory deleted it should remove watch entry`() = runBlocking {
            subfolder1.createDirectory()
            val token = fileWatchEngine.createToken()
            token.registerRootDirectory()
            token.registerDirectoryPattern(subfolderDirectoryPattern)
            delay(defaultTimeoutMillis)

            subfolder1.deleteExisting()
            delay(defaultTimeoutMillis)

            assertThat(fileWatchEngine).scopes()
                .key(token)
                .directoryWatchKeyEntries().all {
                    hasSize(1)
                    index(0).all {
                        absoluteDirectory().isEqualTo(root.asAbsolute())
                        watchedSubdirectories().isEmpty()
                        watchKey().isNotNull()
                    }
                }
        }

        @Test
        fun `When one of many watched subdirectories deleted it should remove watch entry`() = runBlocking {
            subfolder1.createDirectory()
            subfolder2.createDirectory()
            val token = fileWatchEngine.createToken()
            token.registerRootDirectory()
            token.registerDirectoryPattern(subfolderDirectoryPattern)
            delay(defaultTimeoutMillis)

            subfolder1.deleteExisting()
            delay(defaultTimeoutMillis)

            assertThat(fileWatchEngine).scopes()
                .key(token)
                .directoryWatchKeyEntries().all {
                    hasSize(2)
                    index(0).all {
                        absoluteDirectory().isEqualTo(root.asAbsolute())
                        watchedSubdirectories().all {
                            hasSize(1)
                            exactly(1) {
                                it.absolutePath().isEqualTo(subfolder2.asAbsolute())
                            }
                        }
                        watchKey().isNotNull()
                    }
                    index(1).all {
                        absoluteDirectory().isEqualTo(subfolder2.asAbsolute())
                        watchedSubdirectories().isEmpty()
                        watchKey().isNotNull()
                    }
                }
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
                .isFileExistsInstance()
                .all {
                    file().isEqualTo(rootFileDotTxt.relative)
                    origin().isEqualTo(FileWatchEngine.Event.File.Origin.WATCH)
                }
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
                .isFileExistsInstance()
                .all {
                    file().isEqualTo(rootFileDotTxt.relative)
                    origin().isEqualTo(FileWatchEngine.Event.File.Origin.WATCH)
                }
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
                .isFileDoesNotExistInstance()
                .all {
                    file().isEqualTo(rootFileDotTxt.relative)
                    origin().isEqualTo(FileWatchEngine.Event.File.Origin.WATCH)
                }
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
    val testNextTokenId: Int
        get() = nextTokenId
    val testDestroyedTokenIdRanges: Set<ClosedRange<Int>>
        get() = destroyedTokenIdRanges
    val testScopes: Map<TokenImpl, ScopeImpl>
        get() = scopes
    val testService: WatchService?
        get() = service
    val testPollLoopScope: CoroutineContext?
        get() = pollLoopScope
    val testPollLoopJob: Job?
        get() = pollLoopJob
}

private fun Assert<TestFileWatchEngine>.nextTokenId() = prop("nextTokenId") { it.testNextTokenId }
private fun Assert<TestFileWatchEngine>.destroyedTokenIdRanges() = prop("destroyedTokendRanges") { it.testDestroyedTokenIdRanges }
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

private fun Assert<FileWatchEngine.Scope.WatchedSubdirectoryEntry>.absolutePath() = prop(FileWatchEngine.Scope.WatchedSubdirectoryEntry::absolutePath)
private fun Assert<FileWatchEngine.Scope.WatchedSubdirectoryEntry>.relativePath() = prop(FileWatchEngine.Scope.WatchedSubdirectoryEntry::relativePath)


private fun Assert<FileWatchEngine.Event>.isFileExistsInstance() = isInstanceOf(FileWatchEngine.Event.File.Exists::class)
private fun Assert<FileWatchEngine.Event>.isFileDoesNotExistInstance() = isInstanceOf(FileWatchEngine.Event.File.DoesNotExist::class)
private fun Assert<FileWatchEngine.Event>.isOverflowInstance() = isInstanceOf(FileWatchEngine.Event.Overflow::class)
private fun Assert<FileWatchEngine.Event.File>.file() = prop(FileWatchEngine.Event.File::file)
private fun Assert<FileWatchEngine.Event.File>.origin() = prop(FileWatchEngine.Event.File::origin)
