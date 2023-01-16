package tech.coner.snoozle.db.watch

import assertk.all
import assertk.assertAll
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.each
import assertk.assertions.exactly
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.key
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.justRun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.io.TempDir
import tech.coner.snoozle.db.path.AbsolutePath
import tech.coner.snoozle.db.path.RelativePath
import tech.coner.snoozle.db.path.asAbsolute
import tech.coner.snoozle.db.path.asRelative
import tech.coner.snoozle.util.isValid
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.util.regex.Pattern
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
        fileWatchEngine.testWatchStoreFactoryFn = {
            TestWatchStore()
        }
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
            assertThat(fileWatchEngine)
                .watchStore()
                .nextTokenId().isEqualTo(Int.MIN_VALUE)
        }

        @Test
        fun `Its destroyed token ID ranges should be empty initially`() {
            assertThat(fileWatchEngine)
                .watchStore()
                .destroyedTokenIdRanges().isEmpty()
        }

        @Test
        fun `It should not have any scopes initially`() {
            assertThat(fileWatchEngine)
                .watchStore().isEmpty()
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
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl

            assertThat(fileWatchEngine).all {
                watchStore().all {
                    nextTokenId().isEqualTo(Int.MIN_VALUE + 1)
                    scopes().all {
                        hasSize(1)
                        key(token).all {
                            directoryPatterns().isEmpty()
                            filePatterns().isEmpty()
                            directoryWatchKeyEntries().isEmpty()
                        }
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

            assertThat(fileWatchEngine)
                .watchStore().
                nextTokenId().isEqualTo(Int.MIN_VALUE + 2)
        }

        @Test
        fun `It should create a second sequential token`() {
            runBlocking {
                repeat(2) {
                    val token = fileWatchEngine.createToken()
                    token.destroy()
                }
            }

            assertThat(fileWatchEngine)
                .watchStore().all {
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

            token.destroy()

            assertThat(fileWatchEngine).all {
                watchStore().isEmpty()
                service().isNull()
                pollLoopScope().isNull()
                pollLoopJob().isNull()
                watchStore().all {
                    destroyedTokenIdRanges().all {
                        hasSize(1)
                        exactly(1) { it.isEqualTo(Int.MIN_VALUE..Int.MIN_VALUE) }
                    }
                    nextTokenId().isEqualTo(Int.MIN_VALUE + 1)
                }
            }
        }

        @Test
        fun `It should destroy one of many tokens`() = runBlocking {
            val token1 = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            val token2 = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            val token3 = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl

            token1.destroy()

            assertThat(fileWatchEngine).all {
                watchStore().all {
                    scopes().all {
                        hasSize(2)
                        key(token2).all {
                            directoryWatchKeyEntries().isEmpty()
                        }
                        key(token3).all {
                            directoryWatchKeyEntries().isEmpty()
                        }
                    }
                    destroyedTokenIdRanges().all {
                        hasSize(1)
                        exactly(1) { it.isEqualTo(Int.MIN_VALUE..Int.MIN_VALUE) }
                    }
                    nextTokenId().isEqualTo(Int.MIN_VALUE + 3)
                }
                service().isNotNull()
                pollLoopScope().isNotNull()
                pollLoopJob().isNotNull()
            }
        }

        @Test
        fun `It should destroy two sequential tokens into single range`() = runBlocking {
            val token1 = fileWatchEngine.createToken()
            val token2 = fileWatchEngine.createToken()

            token1.destroy()
            token2.destroy()

            assertThat(fileWatchEngine).all {
                watchStore().isEmpty()
                service().isNull()
                pollLoopScope().isNull()
                pollLoopJob().isNull()
                watchStore().all {
                    destroyedTokenIdRanges().all {
                        hasSize(1)
                        exactly(1) { it.isEqualTo(Int.MIN_VALUE..(Int.MIN_VALUE + 1)) }
                    }
                    nextTokenId().isEqualTo(Int.MIN_VALUE + 2)
                }
            }
        }

        @Test
        fun `It should destroy last token created`() = runBlocking {
            val token1 = fileWatchEngine.createToken()
            val token2 = fileWatchEngine.createToken()
            val token3 = fileWatchEngine.createToken()

            token3.destroy()

            assertThat(fileWatchEngine)
                .watchStore().all {
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

            token2.destroy()

            assertThat(fileWatchEngine)
                .watchStore().all {
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

            token1.destroy()
            token2.destroy()

            assertThat(fileWatchEngine)
                .watchStore().all {
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

            token2.destroy()
            token1.destroy()

            assertThat(fileWatchEngine)
                .watchStore().all {
                    destroyedTokenIdRanges().all {
                        hasSize(1)
                        exactly(1) { it.isEqualTo(Int.MIN_VALUE..(Int.MIN_VALUE + 1)) }
                    }
                }
        }

        @Test
        fun `When multiple tokens watch the same directory and one is closed the other watch key should remain valid`() = runBlocking {
            subfolder1.createDirectory()
            val token1 = fileWatchEngine.createToken()
            val token2 = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            arrayOf(token1, token2)
                .forEach { token ->
                    token.registerRootDirectory()
                    token.registerDirectoryPattern(subfolderDirectoryPattern)
                    token.registerFilePattern(subfolderAnyTxtPattern)
                }

            token1.destroy()

            assertThat(fileWatchEngine).isNotNull().all {
                watchStore().all {
                    scopes().all {
                        hasSize(1)
                        key(token2).all {
                            directoryWatchKeyEntries().all {
                                hasSize(2)
                                each { it.watchKey().isValid() }
                            }
                        }
                    }
                }
            }
        }
    }

    @Nested
    inner class RegisterDirectoryPattern {

        @Test
        fun `It should register root directory`() = runBlocking {
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl

            token.registerRootDirectory()

            assertThat(fileWatchEngine)
                .watchStore()
                .scopes()
                .all {
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
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerRootDirectory()

            token.unregisterRootDirectory()

            assertThat(fileWatchEngine)
                .watchStore()
                .scopes().all {
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
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerDirectoryPattern(subfolderDirectoryPattern)

            assertThat(fileWatchEngine)
                .watchStore()
                .scopes().all {
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
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerDirectoryPattern(subfolderDirectoryPattern)

            assertThat(fileWatchEngine)
                .watchStore()
                .scopes().all {
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
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerDirectoryPattern(subfolderDirectoryPattern)

            token.unregisterDirectoryPattern(subfolderDirectoryPattern)

            assertThat(fileWatchEngine)
                .watchStore()
                .scopes().all {
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
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerDirectoryPattern(subfolderDirectoryPattern)
            token.unregisterDirectoryPattern(subfolderDirectoryPattern)

            assertThat(fileWatchEngine)
                .watchStore()
                .scopes().all {
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
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerDirectoryPattern(subfolderDirectoryPattern)
            token.registerRootDirectory()

            assertThat(fileWatchEngine)
                .watchStore()
                .scopes().all {
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

        @Test
        fun `When multiple tokens match the same directory it should reuse the watchToken`() = runBlocking {
            subfolder1.createDirectory()
            val token1 = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            val token2 = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl

            token1.registerDirectoryPattern(subfolderDirectoryPattern)
            token2.registerDirectoryPattern(subfolderDirectoryPattern)

            assertThat(fileWatchEngine).all {
                watchStore()
                    .scopes().all {
                        hasSize(2)
                        transform("scopes distinct watchKeys") { scopes ->
                            scopes.values
                                .flatMap { it.directoryWatchKeyEntries }
                                .map { it.watchKey }
                                .distinct()
                        }
                            .hasSize(1)
                    }
            }
        }
    }

    @Nested
    inner class RegisterFilePattern {

        @Test
        fun `It should register file pattern`() = runBlocking {
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerRootDirectory()

            token.registerFilePattern(rootAnyTxtPattern)

            assertThat(fileWatchEngine)
                .watchStore()
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
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerRootDirectory()
            token.registerFilePattern(rootAnyTxtPattern)

            token.unregisterFilePattern(rootAnyTxtPattern)

            assertThat(fileWatchEngine)
                .watchStore()
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
                .isCreatedTypedInstance<RelativePath>()
                .all {
                    record().isEqualTo(subfolder1FileDotTxt.relative)
                    origin().isEqualTo(Event.Origin.NEW_DIRECTORY_SCAN)
                }
        }
    }

    @Nested
    inner class DirectoryDeleted {

        @Test
        fun `When watched directory deleted it should remove watch entry`() = runBlocking {
            subfolder1.createDirectory()
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerRootDirectory()
            token.registerDirectoryPattern(subfolderDirectoryPattern)
            delay(defaultTimeoutMillis)

            subfolder1.deleteExisting()
            delay(defaultTimeoutMillis)

            assertThat(fileWatchEngine)
                .watchStore()
                .scopes()
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
            val token = fileWatchEngine.createToken() as FileWatchEngine.TokenImpl
            token.registerRootDirectory()
            token.registerDirectoryPattern(subfolderDirectoryPattern)
            delay(defaultTimeoutMillis)

            subfolder1.deleteExisting()
            delay(defaultTimeoutMillis)

            assertThat(fileWatchEngine)
                .watchStore()
                .scopes()
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
        fun `It should emit file created when matching file created`() = runBlocking {
            val token = fileWatchEngine.createToken()
            token.registerRootDirectory()
            token.registerFilePattern(rootAnyTxtPattern)

            launch { rootFileDotTxt.absolute.value.writeText("text") }
            val event = withTimeout(defaultTimeoutMillis) { token.events.first() }

            assertThat(event)
                .isCreatedTypedInstance<RelativePath>()
                .all {
                    record().isEqualTo(rootFileDotTxt.relative)
                    origin().isEqualTo(Event.Origin.WATCH)
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
                .isModifiedTypedInstance<RelativePath>()
                .all {
                    record().isEqualTo(rootFileDotTxt.relative)
                    origin().isEqualTo(Event.Origin.WATCH)
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
                .isDeletedTypedInstance<RelativePath>()
                .all {
                    record().isEqualTo(rootFileDotTxt.relative)
                    origin().isEqualTo(Event.Origin.WATCH)
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

    @Nested
    @ExtendWith(MockKExtension::class)
    inner class Overflow {

        @MockK lateinit var watchService: WatchService
        @MockK lateinit var rootWatchKey: WatchKey
        @MockK lateinit var subfolder1WatchKey: WatchKey
        @MockK lateinit var subfolder2WatchKey: WatchKey
        @MockK lateinit var rootOverflowEvent: WatchEvent<Any>
        @MockK lateinit var subfolder1OverflowEvent: WatchEvent<Any>
        @MockK lateinit var subfolder2OverflowEvent: WatchEvent<Any>

        @BeforeEach
        fun before (): Unit = runBlocking {
            subfolder1.createDirectory()
            subfolder2.createDirectory()
            fileWatchEngine.testWatchServiceFactoryFn = { watchService }
            fileWatchEngine.testWatchKeyFactoryFn = { absolutePath, _ ->
                when (absolutePath) {
                    root.asAbsolute() -> rootWatchKey
                    subfolder1.asAbsolute() -> subfolder1WatchKey
                    subfolder2.asAbsolute() -> subfolder2WatchKey
                    else -> throw IllegalArgumentException()
                }
            }

            every { rootOverflowEvent.kind() } returns StandardWatchEventKinds.OVERFLOW
            every { subfolder1OverflowEvent.kind() } returns  StandardWatchEventKinds.OVERFLOW
            every { subfolder2OverflowEvent.kind() } returns StandardWatchEventKinds.OVERFLOW
            justRun { rootWatchKey.cancel() }
            justRun { subfolder1WatchKey.cancel() }
            justRun { subfolder2WatchKey.cancel() }
            justRun { watchService.close() }
        }

        @Test
        fun `It should emit overflow to sole token`(): Unit = runBlocking {
            every { watchService.take() } returns rootWatchKey
            every { rootWatchKey.pollEvents() } returns listOf(rootOverflowEvent)
            val token = fileWatchEngine.createToken()
            token.registerRootDirectory()
            token.registerFilePattern(rootAnyTxtPattern)

            val event = withTimeout(defaultTimeoutMillis) { token.events.first() }
            assertThat(event)
                .isOverflowInstance()
        }

        @Test
        fun `It should emit overflow to relevant token`(): Unit = runBlocking {
            every { watchService.take() } returnsMany listOf(
                rootWatchKey,
                subfolder1WatchKey,
                subfolder2WatchKey
            )
            every { rootWatchKey.pollEvents() } returns listOf(rootOverflowEvent)
            every { subfolder1WatchKey.pollEvents() } returns listOf(subfolder1OverflowEvent)
            every { subfolder2WatchKey.pollEvents() } returns listOf(subfolder2OverflowEvent)
            val rootToken = fileWatchEngine.createToken()
            rootToken.registerRootDirectory()
            val subfolder1Token = fileWatchEngine.createToken()
            subfolder1Token.registerDirectoryPattern(Pattern.compile("^subfolder1$"))
            val subfolder2Token = fileWatchEngine.createToken()
            subfolder2Token.registerDirectoryPattern(Pattern.compile("^subfolder2$"))

            val rootEvent = async { withTimeoutOrNull(defaultTimeoutMillis) { rootToken.events.first() } }
            val subfolder1Event = async { withTimeoutOrNull(defaultTimeoutMillis) { subfolder1Token.events.first() } }
            val subfolder2Event = async { withTimeoutOrNull(defaultTimeoutMillis) { subfolder2Token.events.first() } }

            assertAll {
                assertThat(rootEvent.await()).isNull()
                assertThat(subfolder1Event.await()).isNull()
                assertThat(subfolder2Event.await())
                    .isNotNull()
                    .isOverflowInstance()
            }
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
