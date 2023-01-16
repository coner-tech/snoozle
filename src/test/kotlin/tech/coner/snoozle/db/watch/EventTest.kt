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
            record = Unit,
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
            record = Unit,
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

        private val actualUnit = Event.Deleted(
            record = Unit,
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

        @Test
        fun `It should not be a Record instance`() {
            assertThat(Event.Overflow).isNotInstanceOf(Event.Record::class)
        }

        @Test
        fun `It should not be an exists instance`() {
            assertThat(Event.Overflow).isNotInstanceOf(Event.Exists::class)
        }
    }
}
