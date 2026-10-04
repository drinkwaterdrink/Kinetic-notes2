package com.example.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeTest {

    private val now = 1_700_000_000_000L
    private val minute = 60_000L
    private val hour = 60 * minute
    private val day = 24 * hour

    @Test
    fun formatsRecentEdits() {
        assertEquals("just now", relativeTimeLabel(now - 5_000, now))
        assertEquals("1m ago", relativeTimeLabel(now - 50_000, now))
        assertEquals("5m ago", relativeTimeLabel(now - 5 * minute, now))
    }

    @Test
    fun formatsHoursAndDays() {
        assertEquals("2h ago", relativeTimeLabel(now - 2 * hour, now))
        assertEquals("23h ago", relativeTimeLabel(now - 23 * hour, now))
        assertEquals("3d ago", relativeTimeLabel(now - 3 * day, now))
    }

    @Test
    fun formatsWeeksAndYears() {
        assertEquals("1w ago", relativeTimeLabel(now - 8 * day, now))
        assertEquals("1y ago", relativeTimeLabel(now - 400 * day, now))
    }

    @Test
    fun clockSkewDoesNotProduceNegativeLabels() {
        assertEquals("just now", relativeTimeLabel(now + 5_000, now))
    }
}
