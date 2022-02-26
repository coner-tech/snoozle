# Snoozle

Snoozle is a toolkit for building curiously simple distributed software applications for personal or small scale operation.

Where others go to lengths to support operation at internet scale, Snoozle keeps it simple so individuals and small organizations can keep control of their systems and data.

## Components

There are a few common themes throughout Snoozle's components:

- Nodes only ever read/write their own local filesystem.
- Nodes rely on separate [file synchronization software](https://en.wikipedia.org/wiki/Comparison_of_file_synchronization_software) to replicate files to all participating nodes.
- Records use UUIDs as unique identifiers.

### Snoozle Database

__Status: In Development__

Snoozle Database is a filesystem-based object database.

Features:
- Simple data structure with one file per record 
- Type-safe, bidirectional key/path mapping
- Entities stored as JSON with automatic (de)serialization
- Blobs stored with arbitrary text or data
- Convenient DSL

```kotlin
entity<Widget.Key, Widget> {
    path = "widgets" / { id } + ".json"
    keyFromPath = { Widget.Key(id = uuidAt(0)) }
    keyFromEntity = { Widget.Key(id = id) }
}
```
Refer to `tech.coner.snoozle.db.sample.SampleDatabase` in the test source set for more detail.

### Snoozle Queue

Snoozle Queue is a filesystem-based event queue. Status: Queued ;)