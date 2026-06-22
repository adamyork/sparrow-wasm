package com.github.adamyork.sparrow.wasm

import io.github.oshai.kotlinlogging.Formatter
import io.github.oshai.kotlinlogging.KLoggingEvent
import io.github.oshai.kotlinlogging.KotlinLoggingConfiguration
import io.github.oshai.kotlinlogging.Level
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

object LogConfig {

    fun initialize(minimumLevel: Level = Level.INFO) {
        KotlinLoggingConfiguration.direct.logLevel = minimumLevel
        KotlinLoggingConfiguration.direct.formatter = object : Formatter {
            override fun formatMessage(loggingEvent: KLoggingEvent): String {
                val levelStr = loggingEvent.level.name
                val cleanName = loggingEvent.loggerName.substringAfterLast('.')
                val instant: kotlin.time.Instant = kotlin.time.Clock.System.now()
                val zone: TimeZone = TimeZone.currentSystemDefault()
                val localDateTime: LocalDateTime = instant.toLocalDateTime(zone)
                return "[$localDateTime] [sparrow-wasm] [kotlin] [${levelStr.lowercase()}] [${cleanName.lowercase()}] -> ${loggingEvent.message?.lowercase()}"
            }
        }
    }

}
