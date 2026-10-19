package com.shorashim.core.common

import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object TimeFormat {
    /**
     * Parses Postgres/Supabase timestamps. `Instant.parse` only accepts a trailing `Z`, but PostgREST returns offsets
     * like `2026-10-19T09:30:00.123456+00:00`, which made every chat timestamp silently render as blank.
     */
    fun parseInstant(iso: String?): Instant? {
        if (iso.isNullOrBlank()) return null
        return runCatching { OffsetDateTime.parse(iso, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() }
            .recoverCatching { Instant.parse(iso) }
            // Timestamps without any zone ("timestamp" columns) are stored in UTC.
            .recoverCatching { java.time.LocalDateTime.parse(iso).toInstant(java.time.ZoneOffset.UTC) }
            .getOrNull()
    }
}
