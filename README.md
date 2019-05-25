# Snoozle

Snoozle is a toolkit for building curiously simple distributed software applications.

Snoozle aims to meet the data storage needs of the autocross event operations software [Coner](https://github.com/caeos).

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
        val id: UUID = UUID.randomUUID(),
        val name: String
) : Entity

@EntityPath("/widgets/{widgetId}/subwidgets/{id}")
data class Subwidget(
        val id: UUID = UUID.randomUUID(),
        val widgetId: UUID,
        val name: String
) : Entity
```

Safely, compactly, but expressively create, read, update, and delete entities:

```kotlin
val db = SampleDatabase(path, objectMapper)

// create/update
val widget = Widget(
    id = uuid("1f30d7b6-0296-489a-9615-55868aeef78a"),
    name = "Widget One"
)
database.put(widget)

// read
val widgetOne = db.get(Widget::id to widget.id)
val subwidget = db.get(
        Subwidget::widgetId to widgetOne.id,
        Subwidget::id to uuid("220460be-27d4-4e6d-8ac3-34cf5139b229")
)

// list all widgets
val widgets = db.list<Widget>()

// list all subwidgets under widgetOne
val subwidgets = db.list(Subwidget::widgetId to widgetOne.id)

// delete
db.delete(widget)
```

Opt into automatic entity verisioning with `@AutomaticVersionedEntity`. Snoozle Database will automatically increment a version number, attach a timestamp, and retain prior versions of the entity.

```kotlin
@EntityPath("/gadgets/{id}")
@AutomaticVersionedEntity
data class Gadget(
        val id: UUID = UUID.randomUUID(),
        var name: String? = null,
        var silly: ZonedDateTime? = null
) : Entity

val db = SampleDatabase(path, objectMapper)
val gadget = Gadget(name = "Original")
db.put(gadget)
var record = db.getWholeRecord(Gadget::id to gadget.id)
println(record.currentVersion.version) // 0
gadget.name = "Revised"
db.put(gadget)
record = db.getWholeRecord(Gadget::id to gadget.id)
println(record.currentVersion.version) // 1
println(record.entity.name) // Revised
println(record.history[0].version) // 0
println(record.history[0].entity.name) // Original
```

### Snoozle Queue

Snoozle Queue is a filesystem-based event queue. Status: Queued ;)