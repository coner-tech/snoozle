package org.coner.snoozle.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

fun snoozleJacksonObjectMapper() = jacksonObjectMapper()
        .findAndRegisterModules()
        .configure(SerializationFeature.INDENT_OUTPUT, true)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)