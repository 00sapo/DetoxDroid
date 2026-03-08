package com.flx_apps.digitaldetox.ui.screens.feature.anti_uninstall

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flx_apps.digitaldetox.R
import com.flx_apps.digitaldetox.ui.widgets.NumberPickerDialog
import com.flx_apps.digitaldetox.ui.widgets.SimpleListTile

/**
 * The settings section for the anti-uninstall feature.
 * Shows current lock status and allows the user to set a time-based lock.
 */
@Composable
fun AntiUninstallFeatureSettingsSection(
    viewModel: AntiUninstallFeatureSettingsViewModel = viewModel()
) {
    val isLocked = viewModel.isLocked.collectAsState().value
    val lockedUntilFormatted = viewModel.lockedUntilFormatted.collectAsState().value
    val context = LocalContext.current

    if (viewModel.showDayPickerDialog.collectAsState().value) {
        NumberPickerDialog(
            titleText = stringResource(id = R.string.feature_antiUninstall_setLock),
            label = { context.getString(R.string.feature_antiUninstall_setLock_days, it) },
            initialValue = 1,
            onValueSelected = { viewModel.setLockForDays(it) },
            onDismissRequest = { viewModel.setShowDayPickerDialog(false) },
            range = 1..365
        )
    }

    SimpleListTile(
        leadingIcon = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
        titleText = stringResource(id = R.string.feature_antiUninstall_lock),
        subtitleText = if (isLocked) {
            stringResource(
                id = R.string.feature_antiUninstall_lock_lockedUntil,
                lockedUntilFormatted
            )
        } else {
            stringResource(id = R.string.feature_antiUninstall_lock_notLocked)
        },
        trailing = {
            if (!isLocked) {
                Text(text = stringResource(id = R.string.action_configuration))
            }
        },
        onClick = { if (!isLocked) viewModel.setShowDayPickerDialog(true) },
    )
}
