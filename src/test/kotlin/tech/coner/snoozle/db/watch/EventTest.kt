package tech.coner.snoozle.db.watch

import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotInstanceOf
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class EventTest {

    @Nested
    inner class Created {

        private val actualUnit = Event.Created(
            recordId = Unit,
            recordContent = Unit,
            origin = Event.Origin.WATCH
        )

        @Test
        fun `It should be a Record instance`() {
            assertThat(actualUnit).isInstanceOf(Event.Record::class)
        }

        @Test
        fun `It should be an Exists instance`() {
            assertThat(actualUnit).isInstanceOf(Event.Exists::class)
        }
    }

    @Nested
    inner class Modified {

        private val actualUnit = Event.Modified(
            recordId = Unit,
            recordContent = Unit,
            origin = Event.Origin.WATCH
        )

        @Test
        fun `It should be a Record instance`() {
            assertThat(actualUnit).isInstanceOf(Event.Record::class)
        }

        @Test
        fun `It should be an Exists instance`() {
            assertThat(actualUnit).isInstanceOf(Event.Exists::class)
        }
    }

    @Nested
    inner class Deleted {

        private val actualUnit = Event.Deleted<Unit, Unit>(
            recordId = Unit,
            origin = Event.Origin.WATCH
        )

        @Test
        fun `It should be a Record instance`() {
            assertThat(actualUnit).isInstanceOf(Event.Record::class)
        }

        @Test
        fun `It should not be an Exists instance`() {
            assertThat(actualUnit).isNotInstanceOf(Event.Exists::class)
        }
    }

    @Nested
    inner class Overflow {

        private val actual = Event.Overflow<Unit, Unit>()

        @Test
        fun `It should not be a Record instance`() {
            assertThat(actual).isNotInstanceOf(Event.Record::class)
        }

        @Test
        fun `It should not be an exists instance`() {
            assertThat(actual).isNotInstanceOf(Event.Exists::class)
        }
    }
}
