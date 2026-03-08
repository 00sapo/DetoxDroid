package com.flx_apps.digitaldetox.features

import androidx.compose.runtime.Composable
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.flx_apps.digitaldetox.R
import com.flx_apps.digitaldetox.data.DataStore
import com.flx_apps.digitaldetox.data.DataStoreProperty
import com.flx_apps.digitaldetox.data.loadValue
import com.flx_apps.digitaldetox.data.persistValue
import com.flx_apps.digitaldetox.feature_types.Feature
import com.flx_apps.digitaldetox.feature_types.FeatureId
import com.flx_apps.digitaldetox.feature_types.FeatureTexts
import com.flx_apps.digitaldetox.ui.screens.feature.anti_uninstall.AntiUninstallFeatureSettingsSection
import java.util.concurrent.TimeUnit

val AntiUninstallFeatureId: FeatureId = Feature.createId(AntiUninstallFeature::class.java)

/**
 * A feature that prevents DetoxDroid from being uninstalled until a configured date has passed.
 *
 * When the feature is activated and a time-based lock is set, the user cannot:
 * - uninstall DetoxDroid via the app's UI
 * - revoke the Device Admin permission
 *
 * The lock expires automatically once the configured date is reached.
 */
object AntiUninstallFeature : Feature() {
    override val texts: FeatureTexts = FeatureTexts(
        title = R.string.feature_antiUninstall,
        subtitle = R.string.feature_antiUninstall_subtitle,
        description = R.string.feature_antiUninstall_description,
    )
    override val iconRes: Int = R.drawable.ic_disable_app
    override val settingsContent: @Composable () -> Unit = {
        AntiUninstallFeatureSettingsSection()
    }

    private val _isActivatedKey = booleanPreferencesKey("${id}_isActivated")
    private var _isActivated: Boolean = DataStore.loadValue<Boolean>(_isActivatedKey) ?: false

    /**
     * Whether the feature is activated. Overridden to prevent deactivation while the lock is
     * active.
     */
    override var isActivated: Boolean
        get() = _isActivated
        set(value) {
            // Prevent deactivating when the time-based lock is currently active
            if (!value && isCurrentlyLocked()) return
            _isActivated = value
            DataStore.persistValue(_isActivatedKey, value)
        }

    /**
     * The timestamp (in milliseconds since epoch) until which uninstallation is blocked.
     * A value of 0 means no time-based lock is set.
     */
    var lockedUntil: Long by DataStoreProperty(
        longPreferencesKey("${id}_lockedUntil"), 0L
    )

    /**
     * Returns whether the time-based uninstall protection lock is currently active.
     */
    fun isCurrentlyLocked(): Boolean = _isActivated && System.currentTimeMillis() < lockedUntil

    /**
     * Sets the lock to expire after the given number of days from now.
     * Has no effect if the lock is already active.
     * @param days The number of days before the lock expires.
     */
    fun setLockForDays(days: Int) {
        if (isCurrentlyLocked()) return
        lockedUntil = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
    }
}
