package com.flx_apps.digitaldetox.feature_types

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A rule for when a feature should be active.  A rule consists of a time range and a set of
 * days of the week.  The feature will be active during the time range on the specified day(s) of
 * the week.  If multiple rules apply, the feature will be active if at least one of them is active.
 *
 * For convenience, rules are stored as strings in the data store.  This class therefore also
 * provides serialisation/deserialisation helpers.
 *
 * @param daysOfWeek Days on which the rule applies.  An empty list means "every day".
 * @param start      Start of the active time window.
 * @param end        End   of the active time window.  If [end] is before [start] the rule spans
 *                   midnight (e.g. 22:00–04:00).
 *
 * @see FeatureScheduleRule.fromString
 * @see FeatureScheduleRule.toString
 */
data class FeatureScheduleRule(
    val daysOfWeek: List<DayOfWeek>, val start: LocalTime, val end: LocalTime
) {
    companion object {
        /**
         * Converts a string to a rule, e.g. "1|3|5,09:00,17:00" to a rule that is active on
         * Monday, Wednesday and Friday from 09:00 to 17:00.  If the string is invalid, `null`
         * is returned.
         *
         * @param string The string to convert.
         */
        fun fromString(string: String): FeatureScheduleRule? {
            kotlin.runCatching {
                val parts = string.split(",")
                val daysOfWeek = parts[0].takeIf { it.isNotBlank() }?.split("|")
                    ?.map { DayOfWeek.of(it.toInt()) } ?: emptyList()
                val start = LocalTime.parse(parts[1])
                val end = LocalTime.parse(parts[2])
                return FeatureScheduleRule(daysOfWeek, start, end)
            }
            return null
        }
    }

    /**
     * Whether the rule is currently active. If [end] is before [start], the rule is active from
     * [start] to midnight and from midnight to [end] (the next day).
     */
    fun isActive(atDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
        var dayOfWeek = atDateTime.dayOfWeek
        val atTime = atDateTime.toLocalTime()

        // copy fromTime and toTime and leave original object alone
        var fromTime = start
        var toTime = end

        if (toTime.isBefore(fromTime)) {
            // we have a rule that spans midnight, e.g. 20:00-04:00 (next day)
            if (atTime.isBefore(toTime)) {
                // timeOfDay is e.g. 03:00, so set fromTime to 00:00 and imagine dayOfWeek as still yesterday
                fromTime = LocalTime.of(0, 0)
                dayOfWeek = dayOfWeek.minus(1)
            } else if (atTime.isAfter(fromTime)) {
                // timeOfDay is e.g. 21:00, so set toTime to 23:59:59 (midnight)
                toTime = LocalTime.of(23, 59, 59, 999999999)
            }
        }

        // the rule is active if the current day of week is in the list of days of week and the
        // current time is between fromTime and toTime or fromTime == toTime (then the whole day is
        // considered active)
        return (daysOfWeek.isEmpty() || daysOfWeek.contains(dayOfWeek)) && ((atTime.isAfter(fromTime) && atTime.isBefore(
            toTime
        )) || (fromTime == toTime))
    }

    fun copyWith(
        daysOfWeek: List<DayOfWeek>? = null, start: LocalTime? = null, end: LocalTime? = null
    ): FeatureScheduleRule {
        return FeatureScheduleRule(
            daysOfWeek ?: this.daysOfWeek, start ?: this.start, end ?: this.end
        )
    }

    /**
     * Converts the rule to a string, e.g. "1|3|5,09:00,17:00".
     */
    override fun toString(): String {
        return "${daysOfWeek.joinToString("|") { it.value.toString() }},$start,$end"
    }
}
