# Changelog

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