package org.coner.snoozle.db

import assertk.assert
import assertk.assertions.*
import assertk.catch
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.Widget
import org.junit.Test
import java.util.*
import kotlin.reflect.full.findAnnotation

class PathfinderTest {

    @Test
    fun itShouldInitWithCorrectEntityPathFormatForWidget() {
        val pathfinder = Pathfinder(Widget::class)

        assert(pathfinder)
                .prop(Pathfinder<Widget>::entityPathFormat)
                .isEqualTo("/widgets/{id}")
    }

    @Test
    fun itShouldInitWithCorrectEntityPathFormatForSubwidget() {
        val pathfinder = Pathfinder(Subwidget::class)

        assert(pathfinder)
                .prop(Pathfinder<Widget>::entityPathFormat)
                .isEqualTo("/widgets/{widgetId}/subwidgets/{id}")
    }

    @Test
    fun itShouldThrowWhenInitWithEntityLackingEntityPathAnnotation() {
        val exception = catch { Pathfinder(LackingEntityPathAnnotation::class) }
        assert(exception).isNotNull {
            it.isInstanceOf(EntityDefinitionException::class)
            it.message().isNotNull {
                it.startsWith(LackingEntityPathAnnotation::class.qualifiedName!!)
                it.contains("lacks")
                it.contains(EntityPath::class.qualifiedName!!)
                it.endsWith("annotation")
            }
        }
    }

    @Test
    fun itShouldThrowWhenInitWithEntityPathValueBracketCountsNotEqual() {
        val exception = catch { Pathfinder(MalformedEntityPathValue::class) }
        assert(exception).isNotNull {
            it.isInstanceOf(EntityDefinitionException::class)
            it.message().isNotNull {
                it.startsWith(MalformedEntityPathValue::class.qualifiedName!!)
                it.contains("has a malformed EntityPath")
                it.contains(MalformedEntityPathValue::class.findAnnotation<EntityPath>()!!.value)
                it.contains("1 open bracket(s)")
                it.contains("0 close bracket(s)")
            }
        }
    }

    @Test
    fun itShouldThrowWhenInitWithEntityPathValueReferencingMissingProperty() {
        val exception = catch { Pathfinder(ReferencingMissingProperty::class) }
        assert(exception).isNotNull {
            it.isInstanceOf(EntityDefinitionException::class)
            it.message().isNotNull {
                it.startsWith(ReferencingMissingProperty::class.qualifiedName!!)
                it.contains("has a malformed EntityPath")
                it.contains(ReferencingMissingProperty::class.findAnnotation<EntityPath>()!!.value)
                it.contains("No such property: id")
            }
        }
    }

    @Test
    fun itShouldThrowWhenInitWithEntityPathValueReferencingNonUuidProperty() {
        val exception = catch { Pathfinder(ReferencingNonUuidProperty::class ) }
        assert(exception).isNotNull {
            it.isInstanceOf(EntityDefinitionException::class)
            it.message().isNotNull {
                it.startsWith(ReferencingNonUuidProperty::class.qualifiedName!!)
                it.contains("has an invalid EntityPath")
                it.contains(ReferencingNonUuidProperty::class.findAnnotation<EntityPath>()!!.value)
                it.contains("References property with unexpected type: ${String::class.qualifiedName!!}")
                it.endsWith("Only ${UUID::class.qualifiedName} is supported.")
            }
        }
    }

}

private class LackingEntityPathAnnotation : Entity

@EntityPath("/foo/{id")
private class MalformedEntityPathValue(
        val id: UUID
) : Entity

@EntityPath("/foo/{id}")
private class ReferencingMissingProperty(
        val eyedee: UUID
) : Entity

@EntityPath("/foo/{id}")
private class ReferencingNonUuidProperty (
        val id: String
) : Entity