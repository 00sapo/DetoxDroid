package com.flx_apps.digitaldetox.feature_types

import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.flx_apps.digitaldetox.data.DataStoreProperty
import com.flx_apps.digitaldetox.data.DataStorePropertyTransformer
import java.time.LocalDateTime

/**
 * A feature that supports scheduling. This means that the feature can be scheduled to be active
 * only at certain times of the day and/or on certain days of the week.
 * @see FeatureScheduleRule
 * @see isScheduled
 */
interface SupportsScheduleFeature {
    /**
     * Holds the schedule rules for a feature.
     */
    var scheduleRules: Set<FeatureScheduleRule>

    /**
     * Returns whether the feature is scheduled at the given date and time.
     */
    fun isScheduled(atDateTime: LocalDateTime = LocalDateTime.now()): Boolean

    /**
     * The implementation of [SupportsAppExceptionsFeature].
     * @param featureId The [FeatureId] is needed in order to properly
     */
    class Impl(private val featureId: FeatureId) : SupportsScheduleFeature {
        override var scheduleRules: Set<FeatureScheduleRule> by DataStoreProperty(
            stringSetPreferencesKey("${featureId}_scheduleRules"),
            setOf(),
            dataTransformer = DataStorePropertyTransformer.SetStorePropertyTransformer(
                itemFromString = {
                    FeatureScheduleRule.fromString(it)
                },
                itemToString = { it.toString() })
        )

        override fun isScheduled(atDateTime: LocalDateTime): Boolean {
            return scheduleRules.isEmpty() || scheduleRules.any {
                it.isActive(atDateTime)
            }
        }
    }
}
