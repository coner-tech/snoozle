# Changelog

# 0.5.2 (2022-03-16)

- Rename package to tech.coner.snoozle
- Change the Maven coordinates to
  - Group ID: tech.coner.snoozle
  - Artifact ID: snoozle
- Upgrade dependencies
- [#27](https://github.com/coner-tech/snoozle/issues/27) Database versioning
- Admin and Data Sessions

# 0.4.1 (2020-09-02)
- [#36](https://github.com/caeos/snoozle/issues/36) Return an empty stream instead of throwing NoSuchFileException when streaming resource with no records

# 0.4.0 (2020-08-29)
- [#33](https://github.com/caeos/snoozle/issues/33) Use parameterized key types for lookups instead of vararg Any
    - Eliminated entity versioning
- [#25](https://github.com/caeos/snoozle/issues/25) Revise the database path definition to use receiver functions

# 0.3.4 (2020-08-04)

- [#24](https://github.com/caeos/snoozle/issues/24) Fix atomic entity write strategy with single-filesystem approach

## 0.3.3 (2020-02-03)

- [#19](https://github.com/caeos/snoozle/issues/19) Remove rxfilewatcher dependency

## 0.3.2 (2020-01-04)

- Expose absolute path to blobs

## 0.3.1 (2020-01-01)

- Fix writing blobs into new databases

## 0.3.0 (2019-12-29)

- Replaced annotation/reflection-based entity definition with DSL
- EntityEvent emitted by watchListing() removes raw WatchEvent in favor of State (exists, deleted, or overflow)
- [#18](https://github.com/caeos/snoozle/issues/18) Blob types
- Upgrade dependencies

## 0.2.1 (2019-06-25)

- [#16](https://github.com/caeos/snoozle/issues/16) Explicitly register Jackson Java Time module

## 0.2.0 (2019-05-24)

- [#11](https://github.com/caeos/snoozle/issues/11) Upgrade to JUnit 5
- [#10](https://github.com/caeos/snoozle/issues/10) Switch to java.nio.Path
- [#8](https://github.com/caeos/snoozle/issues/8) Optional automatic entity versioning
- [#7](https://github.com/caeos/snoozle/issues/7) Store entities at rest wrapped in an object

## 0.1.2 (2019-01-18)

Fixes:
- [#6](https://github.com/caeos/snoozle/issues/6) Db entity listing auto-creates parent folder

## 0.1.1 (2019-01-17)

Enhancements:
- [#3](https://github.com/caeos/snoozle/issues/3) Auto-create entity folders
- [#4](https://github.com/caeos/snoozle/issues/4) Implement Jackson performance tips

## 0.1.0 (2018-12-03)

Initial release.

Features:
- Database
    - Create, read, update, delete entities
    - Watch for entity changes in listing
    
Known issues:
- Watching for entity changes on Windows in a Syncthing folder produces exceptions due to filesystem locks