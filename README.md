# Snoozle

Snoozle is a toolkit for building curiously simple distributed software applications.

Why does the world need yet another database, event queue, etc? Snoozle aims to meet the data storage needs of the autocross event operations software [Coner](https://github.com/caeos):

- Single primary nodes
    - Core business logic must continue working uninterrupted in the face of low and/or no connectivity to other participating nodes
    - Role ownership (as the primary node) must not be tied to any physical device
    - Expected to be powered off for weeks or months at a time when not in use
- Unlimited secondary nodes
    - Needs to access data owned by primaries 24/7 on a read-only basis, regardless of primary node availability
    - Participate in business logic functions in the face of low and/or no connectivity for other participant nodes
- Relatively low storage and performance needs
- Sheer simplicity
    - Simple to understand
    - Simple to develop applications
    - Simple to own and operate

## Components

There are a few common themes throughout Snoozle's components:

- Nodes read/write only to their own local filesystem.
- Nodes rely on separate [file synchronization software](https://en.wikipedia.org/wiki/Comparison_of_file_synchronization_software) to replicate files to all participating nodes.
- Records use UUIDs as unique identifiers.

### Snoozle Database

Snoozle Database is a filesystem-based JSON object database with a structure inspired by REST URLs. Status: In Development

Simply define classes for your entities with an annotation describing where to store them:

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
```

Safely, compactly, but expressively create, read, update, and delete entities:

```kotlin
val db = Database(File("/sample-db"), Widget::class, Subwidget::class)

// create/update
val widget = Widget(
    id = UUID.fromString("1f30d7b6-0296-489a-9615-55868aeef78a"),
    name = "Widget One"
)
database.put(widget)

// read
val widgetOne = db.get(Widget::id to widget.id)
val subwidget = db.get(
        Subwidget::widgetId to widgetOne.id,
        Subwidget::id to UUID.fromString("220460be-27d4-4e6d-8ac3-34cf5139b229")
)

// list all widgets
val widgets = db.list<Widget>()

// list all subwidgets under widgetOne
val subwidgets = db.list(Subwidget::widgetId to widgetOne.id)

// delete
db.delete(widget)
```

Prior to the delete call, the example above would write these files to disk. 

```no-highlight
sample-db/widgets/1f30d7b6-0296-489a-9615-55868aeef78a.json
sample-db/widgets/1f30d7b6-0296-489a-9615-55868aeef78a/subwidgets/220460be-27d4-4e6d-8ac3-34cf5139b229.json
```

### Snoozle Queue

Snoozle Queue is a filesystem-based event queue. Status: Queued ;)