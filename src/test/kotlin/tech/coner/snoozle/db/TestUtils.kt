package tech.coner.snoozle.db

import org.junit.jupiter.api.assertDoesNotThrow
import tech.coner.snoozle.db.session.Session

fun Session.closeAndAssertSuccess() {
   assertDoesNotThrow {
      close()
   }
}