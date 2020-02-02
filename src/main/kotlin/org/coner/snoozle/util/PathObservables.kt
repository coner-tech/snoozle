package org.coner.snoozle.util

import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

object PathObservables {
    /**
     * Creates an observable that watches the given directory and all its subdirectories. Directories
     * that are created after subscription are watched, too.
     * @param path Root directory to be watched
     * @return Observable that emits an event for each filesystem event.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun watchRecursive(path: Path): Observable<WatchEvent<*>> {
        val recursive = true
        return ObservableFactory(path, recursive).create()
    }

    /**
     * Creates an observable that watches the given path but not its subdirectories.
     * @param path Path to be watched
     * @return Observable that emits an event for each filesystem event.
     */
    fun watchNonRecursive(path: Path): Observable<WatchEvent<*>> {
        val recursive = false
        return ObservableFactory(path, recursive).create()
    }

    internal class ObservableFactory constructor(
            private val directory: Path,
            private val recursive: Boolean
    ) {
        private val directoriesByKey: MutableMap<WatchKey, Path> = HashMap()
        fun create(): Observable<WatchEvent<*>> {
            return Observable.create { subscriber: ObservableEmitter<WatchEvent<*>> ->
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
                        val dir = directoriesByKey[key]
                        for (event in key.pollEvents()) {
                            subscriber.onNext(event)
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

        /**
         * Register the rootDirectory, and all its sub-directories.
         */
        @Throws(IOException::class)
        private fun registerAll(rootDirectory: Path, watcher: WatchService) {
            Files.walkFileTree(rootDirectory, object : SimpleFileVisitor<Path>() {
                @Throws(IOException::class)
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    register(dir, watcher)
                    return FileVisitResult.CONTINUE
                }
            })
        }

        @Throws(IOException::class)
        private fun register(dir: Path, watcher: WatchService) {
            val key = dir.register(watcher, arrayOf(StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY))
            directoriesByKey[key] = dir
        }

        // register newly created directory to watching in recursive mode
        private fun registerNewDirectory(
                subscriber: ObservableEmitter<WatchEvent<*>>,
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

    }
}