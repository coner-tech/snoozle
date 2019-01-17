# Changelog

## 0.1.1-SNAPSHOT

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