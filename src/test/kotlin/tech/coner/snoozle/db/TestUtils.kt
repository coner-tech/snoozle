package tech.coner.snoozle.db

import assertk.assertThat
import assertk.assertions.isSuccess
import tech.coner.snoozle.db.session.Session

fun Session.closeAndAssertSuccess() {
   assertThat(close(), "close session").isSuccess()
}