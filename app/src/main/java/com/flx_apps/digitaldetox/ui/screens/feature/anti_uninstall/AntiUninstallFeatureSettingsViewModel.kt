package com.flx_apps.digitaldetox.ui.screens.feature.anti_uninstall

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.flx_apps.digitaldetox.features.AntiUninstallFeature
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

/**
 * The view model for the anti-uninstall feature settings section.
 */
@HiltViewModel
class AntiUninstallFeatureSettingsViewModel @Inject constructor(
    application: Application
) : AndroidViewModel(application) {

    private val _isLocked = MutableStateFlow(AntiUninstallFeature.isCurrentlyLocked())
    val isLocked = _isLocked.asStateFlow()

    private val _lockedUntilFormatted = MutableStateFlow(formatLockedUntil())
    val lockedUntilFormatted = _lockedUntilFormatted.asStateFlow()

    private val _showDayPickerDialog = MutableStateFlow(false)
    val showDayPickerDialog = _showDayPickerDialog.asStateFlow()

    /**
     * Sets the lock to expire after the given number of days from now, then refreshes UI state.
     */
    fun setLockForDays(days: Int) {
        AntiUninstallFeature.setLockForDays(days)
        _isLocked.value = AntiUninstallFeature.isCurrentlyLocked()
        _lockedUntilFormatted.value = formatLockedUntil()
    }

    fun setShowDayPickerDialog(show: Boolean) {
        _showDayPickerDialog.value = show
    }

    private fun formatLockedUntil(): String {
        val lockedUntil = AntiUninstallFeature.lockedUntil
        if (lockedUntil == 0L) return ""
        val dateTime =
            LocalDateTime.ofInstant(Instant.ofEpochMilli(lockedUntil), ZoneId.systemDefault())
        return dateTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    }
}
