package tech.coner.snoozle.db.migration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.io.File
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.regex.Pattern
import java.util.stream.Stream
import kotlin.io.path.*
import tech.coner.snoozle.db.path.AbsolutePath

sealed class MigrationTask {

    abstract fun migrate(root: AbsolutePath)

    protected fun Path.find(matchers: List<MigrationPathMatcher>): Stream<Path> {
        val root = this
        val matcher = matchers.joinToString("") { it.regex.pattern() }
        val pattern = Pattern.compile("^$matcher$")
        return Files.find(this, Int.MAX_VALUE, { candidate, attrs ->
            val relativeCandidate = root.relativize(candidate).toString()
            pattern.matcher(relativeCandidate).matches()
        })
    }

    class Move(
        private val from: List<MigrationPathMatcher>,
        private val to: List<MigrationPathMatcher>
    ) : MigrationTask() {

        private val matchers = from.zip(to)

        init {
            require(from.size == to.size) {
                "size mismatch on from and to"
            }
            require(matchers.all { (from, to) -> from::class == to::class }) {
                "type mismatch on from and to"
            }
        }

        override fun migrate(root: AbsolutePath) {
            val fromPaths = root.value.find(from)
                .map { root.value.relativize(it) }
            for (fromPath in fromPaths) {
                val fromPathAsString = fromPath.toString()
                val toPathAsString = buildString {
                    val uuidMatcher = MigrationPathMatcher.OnUuid.regex.matcher(fromPathAsString)
                    to.forEach { toMatcher ->
                        append(
                            when (toMatcher) {
                                is MigrationPathMatcher.OnString -> toMatcher.value
                                MigrationPathMatcher.OnDirectorySeparator -> File.separator
                                MigrationPathMatcher.OnUuid -> uuidMatcher.let { it.find(); it.group() }
                            }
                        )
                    }
                }
                val toPath = Paths.get(toPathAsString)
                val fromPathAbsolute = root.value.resolve(fromPath)
                val toPathAbsolute = root.value.resolve(toPath)
                toPathAbsolute.parent.also {
                    if (it.notExists()) {
                        it.createDirectories()
                    }
                }
                fromPathAbsolute.moveTo(toPathAbsolute, StandardCopyOption.ATOMIC_MOVE)
            }
        }
    }

    class DeleteDirectories(
        private val matching: List<MigrationPathMatcher>
    ) : MigrationTask() {

        override fun migrate(root: AbsolutePath) {
            val onDirectories = root.value.find(matching)
            for (onDirectory in onDirectories) {
                if (!onDirectory.isDirectory()) {
                    throw MigrationException("Matched path must be a directory")
                }
                Files.walkFileTree(onDirectory, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (attrs.isRegularFile) {
                            try {
                                Files.delete(file)
                            } catch (t: Throwable) {
                                throw MigrationException("Failed to delete stray file within directory", t)
                            }
                        }
                        return FileVisitResult.CONTINUE
                    }
                })
                try {
                    Files.delete(onDirectory)
                } catch (t: Throwable) {
                    throw MigrationException("Failed to delete directory", t)
                }
            }
        }
    }

    class DeleteDirectory(
        private val matching: List<MigrationPathMatcher>
    )

    class MutateEntities(
        private val objectMapper: ObjectMapper,
        val on: List<MigrationPathMatcher>,
        val tasks: MutableList<MutateObjectTask>
    ) : MigrationTask() {

        override fun migrate(root: AbsolutePath) {
            val onEntityPaths = root.value.find(on)
            for (onEntityPath in onEntityPaths) {
                val jsonNode = onEntityPath
                    .bufferedReader()
                    .use { reader -> objectMapper.readTree(reader) }
                if (!jsonNode.isObject) {
                    throw MigrationException("Node must be object")
                }
                val jsonObject = jsonNode as ObjectNode
                for (task in tasks) {
                    task.mutate(jsonObject)
                }
                onEntityPath
                    .bufferedWriter()
                    .use { writer -> objectMapper.writeValue(writer, jsonObject) }
            }
        }

        class Builder(
            private val objectMapper: ObjectMapper,
            private val on: List<MigrationPathMatcher>
        ) : MutateObjectBuilderInterface {
            override val tasks = mutableListOf<MutateObjectTask>()

            fun build() = MutateEntities(
                objectMapper = objectMapper,
                on = on,
                tasks = tasks
            )
        }
    }

}
