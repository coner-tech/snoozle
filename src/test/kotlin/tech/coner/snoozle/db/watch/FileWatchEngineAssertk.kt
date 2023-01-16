package tech.coner.snoozle.db.watch

import assertk.Assert
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.prop
import java.nio.file.Path


fun Assert<TestFileWatchEngine>.watchStore() = prop("watchStore") { it.testWatchStore }
fun Assert<TestFileWatchEngine>.service() = prop("service") { it.testService }
fun Assert<TestFileWatchEngine>.pollLoopScope() = prop("pollLoopScope") { it.testPollLoopScope }
fun Assert<TestFileWatchEngine>.pollLoopJob() = prop("pollLoopJob") { it.testPollLoopJob}

fun Assert<FileWatchEngine.Scope>.token() = prop(FileWatchEngine.Scope::token)
fun Assert<FileWatchEngine.Scope>.directoryPatterns() = prop(FileWatchEngine.Scope::directoryPatterns)
fun Assert<FileWatchEngine.Scope>.filePatterns() = prop(FileWatchEngine.Scope::filePatterns)
fun Assert<FileWatchEngine.Scope>.directoryWatchKeyEntries() = prop(FileWatchEngine.Scope::directoryWatchKeyEntries)

fun Assert<FileWatchEngine.Scope.DirectoryWatchKeyEntry>.absoluteDirectory() = prop(FileWatchEngine.Scope.DirectoryWatchKeyEntry::absoluteDirectory)
fun Assert<FileWatchEngine.Scope.DirectoryWatchKeyEntry>.relativeDirectory() = prop(FileWatchEngine.Scope.DirectoryWatchKeyEntry::relativeDirectory)
fun Assert<FileWatchEngine.Scope.DirectoryWatchKeyEntry>.watchKey() = prop(FileWatchEngine.Scope.DirectoryWatchKeyEntry::watchKey)
fun Assert<FileWatchEngine.Scope.DirectoryWatchKeyEntry>.watchedSubdirectories() = prop(FileWatchEngine.Scope.DirectoryWatchKeyEntry::watchedSubdirectories)

fun Assert<FileWatchEngine.Scope.WatchedSubdirectoryEntry>.absolutePath() = prop(FileWatchEngine.Scope.WatchedSubdirectoryEntry::absolutePath)
fun Assert<FileWatchEngine.Scope.WatchedSubdirectoryEntry>.relativePath() = prop(FileWatchEngine.Scope.WatchedSubdirectoryEntry::relativePath)

