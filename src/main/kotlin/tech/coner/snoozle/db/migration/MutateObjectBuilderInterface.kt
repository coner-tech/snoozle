package tech.coner.snoozle.db.migration

interface MutateObjectBuilderInterface {
    
    val tasks: MutableList<MutateObjectTask>

    fun addBooleanProperty(name: String, defaultValue: Boolean? = null) {
        tasks += MutateObjectTask.AddProperty.boolean(name = name, defaultValue = defaultValue)
    }

    fun addStringProperty(name: String, defaultValue: String? = null) {
        tasks += MutateObjectTask.AddProperty.string(name = name, defaultValue = defaultValue)
    }

    fun addIntProperty(name: String, defaultValue: Int? = null) {
        tasks += MutateObjectTask.AddProperty.int(name = name, defaultValue = defaultValue)
    }

    fun addFloatProperty(name: String, defaultValue: Float? = null) {
        tasks += MutateObjectTask.AddProperty.float(name = name, defaultValue = defaultValue)
    }

    fun addDoubleProperty(name: String, defaultValue: Double? = null) {
        tasks += MutateObjectTask.AddProperty.double(name = name, defaultValue = defaultValue)
    }

    fun addLongProperty(name: String, defaultValue: Long? = null) {
        tasks += MutateObjectTask.AddProperty.long(name = name, defaultValue = defaultValue)
    }

    fun addShortProperty(name: String, defaultValue: Short? = null) {
        tasks += MutateObjectTask.AddProperty.short(name = name, defaultValue = defaultValue)
    }

    fun addArray(name: String) {
        tasks += MutateObjectTask.AddProperty.array(name = name)
    }

    fun addObject(name: String) {
        tasks += MutateObjectTask.AddProperty.objectValue(name = name)
    }

    fun addNull(name: String) {
        tasks += MutateObjectTask.AddProperty.nullValue(name = name)
    }

    fun removeProperty(name: String) {
        tasks += MutateObjectTask.RemoveProperty(name = name)
    }

    fun removeProperties(vararg names: String) {
        tasks += names.map { MutateObjectTask.RemoveProperty(name = it) }
    }

    fun onNode(
        jsonPointerString: String,
        allowMissing: Boolean = false,
        op: MutateObjectTask.SelectObjectTask.Builder.() -> Unit
    ) {
        tasks += MutateObjectTask.SelectObjectTask.Builder(
            jsonPointerString = jsonPointerString,
            allowMissing = allowMissing
        )
            .apply(op)
            .build()
    }

    fun onArrayObjects(
        name: String,
        op: MutateObjectTask.IterateArrayTask.Builder.() -> Unit
    ) {
        tasks += MutateObjectTask.IterateArrayTask.Builder(
            name = name
        )
            .apply(op)
            .build()
    }
}