# Snoozle

Snoozle is a toolkit for building simple distributed applications.

- Snoozle Database
    - Filesystem-based JSON object database with a layout inspired by REST URLs
- Snoozle Queue
    - Filesystem-based event queue

There are a few rules used throughout Snoozle:

- Nodes read and write only to their own local filesystem. Use your favorite [file synchronization software](https://en.wikipedia.org/wiki/Comparison_of_file_synchronization_software) to replicate the cluster to all your nodes.
- Records use UUIDs as unique identifiers

# Snoozle Database 

Folder structures inspired by REST APIs:

```no-highlight
sample-db/widgets/1f30d7b6-0296-489a-9615-55868aeef78a.json
sample-db/widgets/1f30d7b6-0296-489a-9615-55868aeef78a/subwidgets/220460be-27d4-4e6d-8ac3-34cf5139b229.json
```

Simple, type-safe I/O using [Kotlin reified types](https://kotlinlang.org/docs/reference/inline-functions.html#reified-type-parameters):

```kotlin
@EntityPath("/widgets/{id}")
data class Widget(
        val id: UUID,
        val name: String
) : Entity

@EntityPath("/widgets/{widgetId}/subwidgets/{id}")
data class Subwidget(
        val id: UUID,
        val widgetId: UUID,
        val name: String
) : Entity

val db = Database(File("/sample-db"), Widget::class, Subwidget::class)
val widget = db.get(Widget::id to UUID.fromString("1f30d7b6-0296-489a-9615-55868aeef78a"))
val subwidget = db.get(
        Subwidget::widgetId to UUID.fromString("1f30d7b6-0296-489a-9615-55868aeef78a"),
        Subwidget::id to UUID.fromString("220460be-27d4-4e6d-8ac3-34cf5139b229")
)
val widgets = db.list<Widget>()
db.delete(widget)
db.put(widget)

```

# Snoozle Queue

Publishers write a file containing their event into a folder representing a topic. Subscribers watch a folder representing a topic for events.