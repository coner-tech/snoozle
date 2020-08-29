/*
Based on rxfilewatcher by Christian Helmbold
https://github.com/helmbold/rxfilewatcher
 */
package org.coner.snoozle.util

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

/**
 * Creates an observable that watches the given directory and all its subdirectories. Directories
 * that are created after subscription are watched, too.
 * @param recursive Root directory to be watched
 * @return Observable that emits an event for each filesystem event.
 * @throws IOException
 */
@Throws(IOException::class)
fun Path.watch(recursive: Boolean = false): Observable<PathWatchEvent> {
    return createPathWatchObservable(this, recursive)
}

object PathObservables {

    @Deprecated(
            message = "Use java.nio.Path extension function `watch` instead",
            replaceWith = ReplaceWith(
                    expression = "path.watch(recursive = false)"
            )
    )
    fun watchNonRecursive(path: Path) = path.watch(recursive = false)

    @Deprecated(
            message = "Use java.nio.Path extension function `watch` instead",
            replaceWith = ReplaceWith(
                    expression = "path.watch(recursive = true)"
            )
    )
    fun watchRecursive(path: Path) = path.watch(recursive = true)
}

internal fun createPathWatchObservable(
        directory: Path,
        recursive: Boolean
): Observable<PathWatchEvent> {

    val directoriesByKey: MutableMap<WatchKey, Path> = HashMap()

    @Throws(IOException::class)
    fun register(dir: Path, watcher: WatchService) {
        val key = dir.register(watcher, arrayOf(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY))
        directoriesByKey[key] = dir
    }

    @Throws(IOException::class)
    fun registerAll(rootDirectory: Path, watcher: WatchService) {
        Files.walkFileTree(rootDirectory, object : SimpleFileVisitor<Path>() {
            @Throws(IOException::class)
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                register(dir, watcher)
                return FileVisitResult.CONTINUE
            }
        })
    }

    fun registerNewDirectory(
            subscriber: ObservableEmitter<PathWatchEvent>,
            dir: Path?,
            watcher: WatchService,
            event: WatchEvent<*>) {
        val kind = event.kind()
        if (recursive && kind == StandardWatchEventKinds.ENTRY_CREATE) { // Context for directory entry event is the file name of entry
            val eventWithPath = event as WatchEvent<Path>
            val name = eventWithPath.context()
            val child = dir!!.resolve(name)
            try {
                if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                    registerAll(child, watcher)
                }
            } catch (exception: IOException) {
                subscriber.onError(exception)
            }
        }
    }
    return Observable.create { subscriber: ObservableEmitter<PathWatchEvent> ->
        var errorFree = true
        directory.fileSystem.newWatchService().use { watcher ->
            try {
                if (recursive) {
                    registerAll(directory, watcher)
                } else {
                    register(directory, watcher)
                }
            } catch (exception: IOException) {
                subscriber.onError(exception)
                errorFree = false
            }
            while (errorFree && !subscriber.isDisposed) {
                val key: WatchKey
                try {
                    key = watcher.take()
                } catch (exception: InterruptedException) {
                    if (!subscriber.isDisposed) {
                        subscriber.onError(exception)
                    }
                    errorFree = false
                    break
                }
                val dir = directoriesByKey[key] ?: continue
                for (event in key.pollEvents()) {
                    val pathWatchEvent = PathWatchEvent(
                            file = dir.resolve(event.context() as Path),
                            kind = event.kind() as WatchEvent.Kind<Path>
                    )
                    subscriber.onNext(pathWatchEvent)
                    registerNewDirectory(subscriber, dir, watcher, event)
                }
                // reset key and remove from set if directory is no longer accessible
                val valid = key.reset()
                if (!valid) {
                    directoriesByKey.remove(key)
                    // nothing to be watched
                    if (directoriesByKey.isEmpty()) {
                        break
                    }
                }
            }
        }
        if (errorFree) {
            subscriber.onComplete()
        }
    }
}

class PathWatchEvent(
        val file: Path,
        val kind: WatchEvent.Kind<Path>
)