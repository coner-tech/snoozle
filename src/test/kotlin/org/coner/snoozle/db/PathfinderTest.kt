package org.coner.snoozle.db

import assertk.assert
import assertk.assertions.*
import assertk.catch
import org.coner.snoozle.db.sample.Subwidget
import org.coner.snoozle.db.sample.Widget
import org.junit.Test

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
        val exception = catch { Pathfinder(EntityLackingEntityPathAnnotation::class) }
        assert(exception).isNotNull {
            it.isInstanceOf(EntityDefinitionException::class)
            it.message().isNotNull {
                it.startsWith(EntityLackingEntityPathAnnotation::class.qualifiedName!!)
                it.contains("lacks")
                it.contains(EntityPath::class.qualifiedName!!)
                it.endsWith("annotation")
            }
        }
    }

}

private class EntityLackingEntityPathAnnotation : Entity