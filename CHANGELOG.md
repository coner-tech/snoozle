# Changelog

## 0.3.0 (pending)

- Replaced annotation/reflection-based entity definition with DSL
- EntityEvent emitted by watchListing() removes raw WatchEvent in favor of State (exists, deleted, or overflow)
- [#18](https://github.com/caeos/snoozle/issues/18) Blob types

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