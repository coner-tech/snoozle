package tech.coner.snoozle.db.migration

import com.fasterxml.jackson.core.JsonPointer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode

sealed class MutateObjectTask {

    abstract fun mutate(root: ObjectNode)

    class AddProperty(
        private val op: (ObjectNode) -> Unit
    ) : MutateObjectTask() {
        override fun mutate(root: ObjectNode) {
            op(root)
        }

        companion object {
            fun boolean(name: String, defaultValue: Boolean?) = AddProperty { it.put(name, defaultValue) }
            fun string(name: String, defaultValue: String?) = AddProperty { it.put(name, defaultValue) }
            fun int(name: String, defaultValue: Int?) = AddProperty { it.put(name, defaultValue) }
            fun float(name: String, defaultValue: Float?) = AddProperty { it.put(name, defaultValue) }
            fun double(name: String, defaultValue: Double?) = AddProperty { it.put(name, defaultValue) }
            fun long(name: String, defaultValue: Long?) = AddProperty { it.put(name, defaultValue) }
            fun short(name: String, defaultValue: Short?) = AddProperty { it.put(name, defaultValue) }
            fun array(name: String) = AddProperty { it.putArray(name) }
            fun objectValue(name: String) = AddProperty { it.putObject(name) }
            fun nullValue(name: String) = AddProperty { it.putNull(name) }
        }
    }

    class RemoveProperty(
        val name: String
    ) : MutateObjectTask() {

        override fun mutate(root: ObjectNode) {
            root.remove(name)
        }
    }

    class SelectObjectTask(
        jsonPointerString: String,
        private val tasks: List<MutateObjectTask>,
        private val allowMissing: Boolean = false
    ) : MutateObjectTask() {

        private val jsonPointer: JsonPointer = JsonPointer.compile(jsonPointerString)

        override fun mutate(root: ObjectNode) {
            val selectedNode = root.at(jsonPointer)
            if (selectedNode.isMissingNode) {
                if (allowMissing) return
                else throw MutationException("Node not found")
            }
            if (!selectedNode.isObject) {
                throw MutationException("Selected node is not an object")
            }
            val selectedObject = selectedNode as ObjectNode
            for (mutateSelectedNodeTask in tasks) {
                mutateSelectedNodeTask.mutate(selectedObject)
            }
        }

        class Builder(
            private val jsonPointerString: String,
            private val allowMissing: Boolean
        ) : MutateObjectBuilderInterface {
            override val tasks = mutableListOf<MutateObjectTask>()

            fun build() = SelectObjectTask(
                jsonPointerString = jsonPointerString,
                tasks = tasks,
                allowMissing = allowMissing
            )
        }
    }

    class IterateArrayTask(
        private val name: String,
        private val tasks: List<MutateObjectTask>
    ) : MutateObjectTask() {
        override fun mutate(root: ObjectNode) {
            val selectedNode: JsonNode = root.get(name)
            if (selectedNode.isMissingNode) {
                throw MutationException("Node not found")
            }
            if (!selectedNode.isArray) {
                throw MutationException("Node is not an array")
            }
            val arrayNode = selectedNode as ArrayNode
            for (childNode in arrayNode) {
                if (!childNode.isObject) {
                    throw MutationException("Child nodes must be objects")
                }
                val childObject = childNode as ObjectNode
                for (task in tasks) {
                    task.mutate(childObject)
                }
            }
        }

        class Builder(
            private val name: String
        ) : MutateObjectBuilderInterface {
            override val tasks = mutableListOf<MutateObjectTask>()

            fun build() = IterateArrayTask(
                name = name,
                tasks = tasks
            )
        }
    }
}