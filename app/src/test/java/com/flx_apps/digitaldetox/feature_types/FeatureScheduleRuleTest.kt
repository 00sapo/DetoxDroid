package com.flx_apps.digitaldetox.feature_types

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests for [FeatureScheduleRule].
 *
 * [FeatureScheduleRule] is a pure-Kotlin data class with no Android-framework
 * dependencies, so all tests run on the JVM.
 *
 * These tests are particularly relevant to the "exceptions from pauses" feature:
 * blocked apps that have a schedule attached to [DisableAppsFeature] must still
 * be blocked according to that schedule even while DetoxDroid is paused.
 */
class FeatureScheduleRuleTest {

    // Helper: build a LocalDateTime for a given day/hour/minute
    private fun at(day: DayOfWeek, hour: Int, minute: Int = 0): LocalDateTime {
        // Use an arbitrary date that lands on the requested day of week.
        // DayOfWeek.MONDAY == 1, so offset from 2024-01-01 (which is a Monday).
        val base = LocalDateTime.of(2024, 1, 1, hour, minute) // Monday
        val daysToAdd = (day.value - DayOfWeek.MONDAY.value).toLong()
        return base.plusDays(daysToAdd)
    }

    // -----------------------------------------------------------------------
    // isActive() – time-range matching (no day-of-week restriction)
    // -----------------------------------------------------------------------

    @Test
    fun isActive_withinTimeRange_returnsTrue() {
        val rule = FeatureScheduleRule(
            daysOfWeek = emptyList(),
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0)
        )
        assertTrue(rule.isActive(at(DayOfWeek.WEDNESDAY, 12, 0)))
    }

    @Test
    fun isActive_beforeTimeRangeStart_returnsFalse() {
        val rule = FeatureScheduleRule(
            daysOfWeek = emptyList(),
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0)
        )
        assertFalse(rule.isActive(at(DayOfWeek.WEDNESDAY, 8, 59)))
    }

    @Test
    fun isActive_afterTimeRangeEnd_returnsFalse() {
        val rule = FeatureScheduleRule(
            daysOfWeek = emptyList(),
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0)
        )
        assertFalse(rule.isActive(at(DayOfWeek.WEDNESDAY, 17, 1)))
    }

    @Test
    fun isActive_exactlyAtStartBoundary_returnsFalse() {
        // The implementation uses strictly-after: start < time < end
        val rule = FeatureScheduleRule(
            daysOfWeek = emptyList(),
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0)
        )
        assertFalse(rule.isActive(at(DayOfWeek.WEDNESDAY, 9, 0)))
    }

    @Test
    fun isActive_exactlyAtEndBoundary_returnsFalse() {
        // The implementation uses strictly-before: start < time < end
        val rule = FeatureScheduleRule(
            daysOfWeek = emptyList(),
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0)
        )
        assertFalse(rule.isActive(at(DayOfWeek.WEDNESDAY, 17, 0)))
    }

    // -----------------------------------------------------------------------
    // isActive() – day-of-week filtering
    // -----------------------------------------------------------------------

    @Test
    fun isActive_withMatchingDayOfWeek_returnsTrue() {
        val rule = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            start = LocalTime.of(8, 0),
            end = LocalTime.of(20, 0)
        )
        assertTrue(rule.isActive(at(DayOfWeek.WEDNESDAY, 12, 0)))
    }

    @Test
    fun isActive_withNonMatchingDayOfWeek_returnsFalse() {
        val rule = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            start = LocalTime.of(8, 0),
            end = LocalTime.of(20, 0)
        )
        assertFalse(rule.isActive(at(DayOfWeek.TUESDAY, 12, 0)))
    }

    @Test
    fun isActive_withEmptyDaysOfWeek_matchesEveryDay() {
        // An empty days-of-week list means "every day"
        val rule = FeatureScheduleRule(
            daysOfWeek = emptyList(),
            start = LocalTime.of(8, 0),
            end = LocalTime.of(20, 0)
        )
        DayOfWeek.entries.forEach { day ->
            assertTrue("Expected active on $day", rule.isActive(at(day, 12, 0)))
        }
    }

    // -----------------------------------------------------------------------
    // isActive() – all-day rule (start == end)
    // -----------------------------------------------------------------------

    @Test
    fun isActive_whenStartEqualsEnd_isActiveAllDay() {
        // start == end means the feature is active 24 hours a day
        val allDay = LocalTime.of(0, 0)
        val rule = FeatureScheduleRule(
            daysOfWeek = emptyList(),
            start = allDay,
            end = allDay
        )
        assertTrue(rule.isActive(at(DayOfWeek.SATURDAY, 3, 0)))
        assertTrue(rule.isActive(at(DayOfWeek.SATURDAY, 14, 30)))
        assertTrue(rule.isActive(at(DayOfWeek.SATURDAY, 23, 59)))
    }

    // -----------------------------------------------------------------------
    // isActive() – midnight-spanning rules (end < start)
    // -----------------------------------------------------------------------

    @Test
    fun isActive_midnightSpanning_activeInEveningOnStartDay() {
        // Rule: 20:00–04:00 on MONDAY
        val rule = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            start = LocalTime.of(20, 0),
            end = LocalTime.of(4, 0)
        )
        // 21:00 Monday → should be active
        assertTrue(rule.isActive(at(DayOfWeek.MONDAY, 21, 0)))
    }

    @Test
    fun isActive_midnightSpanning_activeInMorningOnNextDay() {
        // Rule: 20:00–04:00 on MONDAY (wraps to TUESDAY morning)
        val rule = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            start = LocalTime.of(20, 0),
            end = LocalTime.of(4, 0)
        )
        // 03:00 Tuesday → the rule treats this as still-Monday night, so active
        assertTrue(rule.isActive(at(DayOfWeek.TUESDAY, 3, 0)))
    }

    @Test
    fun isActive_midnightSpanning_inactiveAtMiddayOnStartDay() {
        // Rule: 20:00–04:00 on MONDAY
        val rule = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            start = LocalTime.of(20, 0),
            end = LocalTime.of(4, 0)
        )
        // 12:00 Monday is outside both sub-ranges → inactive
        assertFalse(rule.isActive(at(DayOfWeek.MONDAY, 12, 0)))
    }

    @Test
    fun isActive_midnightSpanning_inactiveAtMiddayOnNextDay() {
        // Rule: 20:00–04:00 on MONDAY
        val rule = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            start = LocalTime.of(20, 0),
            end = LocalTime.of(4, 0)
        )
        // 12:00 Tuesday is well after the 04:00 end → inactive
        assertFalse(rule.isActive(at(DayOfWeek.TUESDAY, 12, 0)))
    }

    // -----------------------------------------------------------------------
    // fromString() – parsing
    // -----------------------------------------------------------------------

    @Test
    fun fromString_validMultiDayRule_parsesCorrectly() {
        val rule = FeatureScheduleRule.fromString("1|3|5,09:00,17:00")
        assertNotNull(rule)
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), rule!!.daysOfWeek)
        assertEquals(LocalTime.of(9, 0), rule.start)
        assertEquals(LocalTime.of(17, 0), rule.end)
    }

    @Test
    fun fromString_emptyDaysOfWeek_parsesAsAllDays() {
        val rule = FeatureScheduleRule.fromString(",00:00,08:00")
        assertNotNull(rule)
        assertTrue("daysOfWeek should be empty", rule!!.daysOfWeek.isEmpty())
        assertEquals(LocalTime.of(0, 0), rule.start)
        assertEquals(LocalTime.of(8, 0), rule.end)
    }

    @Test
    fun fromString_singleDay_parsesCorrectly() {
        val rule = FeatureScheduleRule.fromString("2,08:30,16:45")
        assertNotNull(rule)
        assertEquals(listOf(DayOfWeek.TUESDAY), rule!!.daysOfWeek)
        assertEquals(LocalTime.of(8, 30), rule.start)
        assertEquals(LocalTime.of(16, 45), rule.end)
    }

    @Test
    fun fromString_invalidInput_returnsNull() {
        assertNull(FeatureScheduleRule.fromString("not-a-valid-rule"))
    }

    @Test
    fun fromString_emptyString_returnsNull() {
        assertNull(FeatureScheduleRule.fromString(""))
    }

    @Test
    fun fromString_tooFewParts_returnsNull() {
        assertNull(FeatureScheduleRule.fromString("1,09:00"))
    }

    @Test
    fun fromString_invalidDayNumber_returnsNull() {
        // DayOfWeek.of(8) throws an exception → fromString should return null
        assertNull(FeatureScheduleRule.fromString("8,09:00,17:00"))
    }

    @Test
    fun fromString_invalidTimeFormat_returnsNull() {
        assertNull(FeatureScheduleRule.fromString("1,9am,5pm"))
    }

    // -----------------------------------------------------------------------
    // toString() – serialisation
    // -----------------------------------------------------------------------

    @Test
    fun toString_singleDayRule_producesCorrectFormat() {
        val rule = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.TUESDAY),  // value = 2
            start = LocalTime.of(8, 30),
            end = LocalTime.of(16, 45)
        )
        assertEquals("2,08:30,16:45", rule.toString())
    }

    @Test
    fun toString_multiDayRule_producesCorrectFormat() {
        val rule = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0)
        )
        assertEquals("1|3|5,09:00,17:00", rule.toString())
    }

    @Test
    fun toString_emptyDaysOfWeek_producesEmptyPrefix() {
        val rule = FeatureScheduleRule(
            daysOfWeek = emptyList(),
            start = LocalTime.of(0, 0),
            end = LocalTime.of(8, 0)
        )
        assertEquals(",00:00,08:00", rule.toString())
    }

    // -----------------------------------------------------------------------
    // Round-trip: fromString ↔ toString
    // -----------------------------------------------------------------------

    @Test
    fun roundTrip_multiDayRule_isLossless() {
        val original = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0)
        )
        val serialised = original.toString()
        val restored = FeatureScheduleRule.fromString(serialised)
        assertEquals(original, restored)
    }

    @Test
    fun roundTrip_midnightSpanningRule_isLossless() {
        val original = FeatureScheduleRule(
            daysOfWeek = listOf(DayOfWeek.SATURDAY),
            start = LocalTime.of(22, 0),
            end = LocalTime.of(6, 0)
        )
        val serialised = original.toString()
        val restored = FeatureScheduleRule.fromString(serialised)
        assertEquals(original, restored)
    }

    @Test
    fun roundTrip_allDayRule_isLossless() {
        val original = FeatureScheduleRule(
            daysOfWeek = emptyList(),
            start = LocalTime.of(0, 0),
            end = LocalTime.of(0, 0)
        )
        val serialised = original.toString()
        val restored = FeatureScheduleRule.fromString(serialised)
        assertEquals(original, restored)
    }
}
