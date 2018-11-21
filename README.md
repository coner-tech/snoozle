# Snoozle

Snoozle is a toolkit for building curiously simple distributed software applications.

## Goals

Snoozle seeks to fulfill the goals below. The goals are given in order of importance, the most important one being the first.

### 1. Cater to individuals and small groups

Snoozle should empower individuals and small groups to build, own, and operate distributed software applications by themselves and for themselves.

### 2. Simplicity

Snoozle should make it easy for its target audience to build, own, and operate their systems upon it.

### 3. Reliability

Snoozle should allow distributed software applications built upon it to be resilient in the face of power failures and network outages.

> It should describe the conventions it relies upon to maintain system integrity.

> It should implement passive safeguards through careful design to make it simple for developers to build reliable systems.
 
> It should implement automatic detections of problematic scenarios where passive safeguards are infeasible in order to help operators use their systems correctly and with confidence, within reason.


## Components

There are a few common themes throughout Snoozle's components:

- Nodes read and write only to their own local filesystem.
  - Use your favorite [file synchronization software](https://en.wikipedia.org/wiki/Comparison_of_file_synchronization_software) to replicate those files to all the participating nodes.
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