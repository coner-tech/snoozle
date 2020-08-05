# Snoozle

Snoozle is a toolkit for building curiously simple distributed software applications at personal and small scale.

Where others go to lengths to support operation at internet scale, Snoozle keeps it simple so individuals and small organization can keep control of their systems and data.

## Components

There are a few common themes throughout Snoozle's components:

- Nodes read/write only to their own local filesystem.
- Nodes rely on separate [file synchronization software](https://en.wikipedia.org/wiki/Comparison_of_file_synchronization_software) to replicate files to all participating nodes.
- Records use UUIDs as unique identifiers.

### Snoozle Database

__Status: In Development__

Snoozle Database is a filesystem-based object database.

Features:
- Simplistic data structure: one file per record 
- Path-based storage: simple alternative to traditional indices
- Entity storage
    - Automatic serialization/deserialization of entity objects to/from JSON
    - Optional: automatic versioning
- Blob storage
    - Store files containing arbitrary data
- Convenient DSL

Refer to `org.coner.snoozle.db.sample.SampleDatabase` in the test source set for more detail.

```kotlin
class SampleDatabase(root: Path) : Database(root) {

    override val types = registerTypes {
        entity<Widget> {
            path = "widgets" / { id } + ".json"
        }
        entity<Subwidget> {
            path = "widgets" / { widgetId } / "subwidgets" / { id } + ".json"
        }
        entity<Gadget> {
            path = "gadgets" / { id } + ".json"
            versioning = EntityVersioningStrategy.AutomaticInternalVersioning
        }
        blob<GadgetPhoto> {
            path = "gadgets" / { gadgetId } / "photos" / string { id } + "." + string { extension }
            factory = GadgetPhoto.Factory()
        }
        blob<GadgetPhotoCitation> {
            path = "gadgets" / { gadgetId } / "photos" / "citations" / string { id } + ".citation"
            factory = GadgetPhotoCitation.Factory()
        }
    }
}
```

### Snoozle Queue

Snoozle Queue is a filesystem-based event queue. Status: Queued ;)