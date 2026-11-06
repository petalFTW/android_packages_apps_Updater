/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.android.settingslib.spa.widget.ui.Category
import org.lineageos.updater.controller.UpdaterController
import org.lineageos.updater.data.Update
import org.lineageos.updater.data.UpdateStatus
import org.lineageos.updater.deviceinfo.DeviceInfoBanner
import org.lineageos.updater.deviceinfo.UpdateHeadline
import org.lineageos.updater.preferences.PreferencesActivity
import org.lineageos.updater.ui.BloomState
import org.lineageos.updater.ui.PetalTheme
import org.lineageos.updater.updates.UpdateList
import org.lineageos.updater.updates.action.AlertDialogState
import org.lineageos.updater.updates.action.UpdateActionDialog
import org.lineageos.updater.updates.action.UpdateActionHandler
import org.lineageos.updater.updates.state.UpdateItemStateMapper
import org.lineageos.updater.updatescheck.UpdatesCheck
import org.lineageos.updater.updatescheck.UpdatesCheckModel
import org.lineageos.updater.updatescheck.UpdatesCheckState
import org.lineageos.updater.updatescheck.rememberUpdatesCheckUiState

abstract class UpdatesScaffoldActivity : ComponentActivity() {
    private val viewModel by viewModels<UpdatesViewModel>()
    private var activeUpdaterController: UpdaterController? by mutableStateOf(null)
    private var controllerStateVersion: Int by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun setupCompose() {
        setContent {
            val navController = remember {
                object : NavControllerWrapper {
                    override fun navigate(route: String, popUpCurrent: Boolean) {}
                    override fun navigateBack() = finish()
                }
            }

            CompositionLocalProvider(LocalNavController provides navController) {
                PetalTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    UpdatesScaffoldContent(
                        uiState = uiState,
                        updaterController = activeUpdaterController,
                        controllerStateVersion = controllerStateVersion,
                        onRefreshClick = { onRefreshClick() },
                        onLocalUpdateClick = { onLocalUpdateClick() },
                        onPreferencesClick = {
                            startActivity(
                                Intent(
                                    this@UpdatesScaffoldActivity,
                                    PreferencesActivity::class.java,
                                )
                            )
                        },
                        onControllerStateChanged = { notifyControllerStateChanged() },
                    )
                }
            }
        }
    }

    protected fun setUpdaterController(controller: UpdaterController?) {
        activeUpdaterController = controller
        notifyControllerStateChanged()
    }

    protected fun notifyControllerStateChanged() {
        controllerStateVersion++
    }

    open fun onRefreshClick() {}
    open fun onLocalUpdateClick() {}
    open fun exportUpdate(update: Update) {}
}

@Composable
private fun UpdatesScaffoldContent(
    uiState: UpdatesViewModel.UiState,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onControllerStateChanged: () -> Unit,
) {
    val title = getTitleForUpdateStatus(uiState.updates)
    val headline = getUpdateHeadline(uiState.updates, uiState.updatesCheckModel.state)

    val actionDialogState = remember { mutableStateOf<AlertDialogState?>(null) }
    actionDialogState.value?.let { dialog ->
        UpdateActionDialog(
            dialog = dialog,
            onDismiss = { actionDialogState.value = null },
        )
    }

    SettingsScaffold(title = title) { paddingValues ->
        if (isWideScreen()) {
            WideUpdatesScaffold(
                paddingValues = paddingValues,
                headline = headline,
                updatesCheckModel = uiState.updatesCheckModel,
                updates = uiState.updates,
                updaterController = updaterController,
                controllerStateVersion = controllerStateVersion,
                showDialog = { actionDialogState.value = it },
                onControllerStateChanged = onControllerStateChanged,
                onRefreshClick = onRefreshClick,
                onLocalUpdateClick = onLocalUpdateClick,
                onPreferencesClick = onPreferencesClick,
            )
        } else {
            UpdatesScaffold(
                paddingValues = paddingValues,
                headline = headline,
                updatesCheckModel = uiState.updatesCheckModel,
                updates = uiState.updates,
                updaterController = updaterController,
                controllerStateVersion = controllerStateVersion,
                showDialog = { actionDialogState.value = it },
                onControllerStateChanged = onControllerStateChanged,
                onRefreshClick = onRefreshClick,
                onLocalUpdateClick = onLocalUpdateClick,
                onPreferencesClick = onPreferencesClick,
            )
        }
    }
}

@Composable
private fun WideUpdatesScaffold(
    paddingValues: PaddingValues,
    headline: UpdateHeadline,
    updatesCheckModel: UpdatesCheckModel,
    updates: List<Update>,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    showDialog: (AlertDialogState) -> Unit,
    onControllerStateChanged: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
            )
    ) {
        UpdatesInformationPane(
            headline = headline,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = paddingValues.calculateBottomPadding()),
        )
        UpdatesActionPane(
            model = updatesCheckModel,
            updates = updates,
            updaterController = updaterController,
            controllerStateVersion = controllerStateVersion,
            showDialog = showDialog,
            onControllerStateChanged = onControllerStateChanged,
            onRefreshClick = onRefreshClick,
            onLocalUpdateClick = onLocalUpdateClick,
            onPreferencesClick = onPreferencesClick,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = paddingValues.calculateBottomPadding()),
        )
    }
}

@Composable
private fun UpdatesScaffold(
    paddingValues: PaddingValues,
    headline: UpdateHeadline,
    updatesCheckModel: UpdatesCheckModel,
    updates: List<Update>,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    showDialog: (AlertDialogState) -> Unit,
    onControllerStateChanged: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        UpdatesInformationPane(headline = headline)
        UpdatesActionPane(
            model = updatesCheckModel,
            updates = updates,
            updaterController = updaterController,
            controllerStateVersion = controllerStateVersion,
            showDialog = showDialog,
            onControllerStateChanged = onControllerStateChanged,
            onRefreshClick = onRefreshClick,
            onLocalUpdateClick = onLocalUpdateClick,
            onPreferencesClick = onPreferencesClick,
        )
    }
}

@Composable
private fun UpdatesInformationPane(
    headline: UpdateHeadline,
    modifier: Modifier = Modifier,
) {
    DeviceInfoBanner(headline = headline, modifier = modifier)
}

@Composable
private fun UpdatesActionPane(
    model: UpdatesCheckModel,
    updates: List<Update>,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    showDialog: (AlertDialogState) -> Unit,
    onControllerStateChanged: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as UpdatesScaffoldActivity
    val networkMonitor =
        remember { (context.applicationContext as UpdaterApplication).networkMonitor }
    val networkState by networkMonitor.networkState.collectAsState(
        initial = networkMonitor.currentNetworkState,
    )
    val userPreferencesRepository =
        remember { (context.applicationContext as UpdaterApplication).userPreferencesRepository }
    val streamUpdatesEnabled by userPreferencesRepository.streamUpdatesFlow.collectAsState(
        initial = true,
    )

    val updateItems = remember(
        updates,
        updaterController,
        networkState,
        streamUpdatesEnabled,
        controllerStateVersion,
    ) {
        val controller = updaterController ?: return@remember emptyList()
        val mapper = UpdateItemStateMapper(context, controller, streamUpdatesEnabled)
        updates.mapNotNull { update ->
            controller.getUpdate(update.downloadId)?.let {
                mapper.map(it, networkState)
            }
        }
    }

    val actionHandler = remember(updaterController) {
        updaterController?.let { controller ->
            UpdateActionHandler(
                activity = activity,
                updaterController = controller,
                exportUpdate = { update -> activity.exportUpdate(update) },
                showDialog = showDialog,
            )
        }
    }
    val updatesCheckUiState = rememberUpdatesCheckUiState(model.state)

    Column(modifier = modifier) {
        UpdatesCheck(
            model = model,
            uiState = updatesCheckUiState,
            onCheckClick = onRefreshClick,
        )
        UpdateList(
            items = updateItems,
            isUpdatesCheckStatusVisible = updatesCheckUiState.isStatusVisible,
            onAction = { action, downloadId ->
                val controller = updaterController ?: return@UpdateList
                val update = controller.getUpdate(downloadId) ?: return@UpdateList
                actionHandler?.perform(action, update)
                onControllerStateChanged()
            },
        )
        UpdatesFooter(
            onLocalUpdateClick = onLocalUpdateClick,
            onPreferencesClick = onPreferencesClick,
        )
    }
}

@Composable
private fun isWideScreen(): Boolean {
    val minWideScreenWidth = 600.dp
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize

    return with(density) { windowSize.width.toDp() >= minWideScreenWidth }
}

@Composable
private fun UpdatesFooter(
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
) {
    val localUpdateSummary = stringResource(R.string.local_update_import_summary)
    val preferencesSummary = stringResource(R.string.preferences_summary)

    Category {
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.local_update_import)
            override val summary = { localUpdateSummary }
            override val onClick = onLocalUpdateClick
        })
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.menu_preferences)
            override val summary = { preferencesSummary }
            override val onClick = onPreferencesClick
        })
    }
}

/** One plain sentence for what's happening, and the bloom state that shows the same thing. */
@Composable
private fun getUpdateHeadline(updates: List<Update>, check: UpdatesCheckState): UpdateHeadline {
    val active = updates.firstOrNull { it.status.isInProgress }
    val percent = active?.let { stringResource(R.string.header_percent, it.progress) }
    val failed = updates.any {
        it.status == UpdateStatus.INSTALLATION_FAILED ||
                it.status == UpdateStatus.VERIFICATION_FAILED
    }

    return when {
        updates.any { it.status == UpdateStatus.UPDATED_NEED_REBOOT } -> UpdateHeadline(
            title = stringResource(R.string.installing_update_finished),
            bloom = BloomState.Finished,
        )

        failed -> UpdateHeadline(
            title = stringResource(R.string.installing_update_error),
            bloom = BloomState.Failed,
        )

        active != null -> UpdateHeadline(
            title = stringResource(
                when (active.status) {
                    UpdateStatus.INSTALLING,
                    UpdateStatus.INSTALLATION_SUSPENDED -> R.string.installing_update

                    UpdateStatus.VERIFYING -> R.string.header_verifying
                    UpdateStatus.PAUSED,
                    UpdateStatus.PAUSED_ERROR -> R.string.header_paused

                    else -> R.string.header_downloading
                },
            ),
            detail = percent,
            bloom = BloomState.Progress(
                fraction = active.progress / 100f,
                paused = active.status == UpdateStatus.PAUSED ||
                        active.status == UpdateStatus.PAUSED_ERROR ||
                        active.status == UpdateStatus.INSTALLATION_SUSPENDED,
            ),
        )

        check == UpdatesCheckState.Checking -> UpdateHeadline(
            title = stringResource(R.string.checking_for_updates),
            bloom = BloomState.Searching,
        )

        check == UpdatesCheckState.NoInternet -> UpdateHeadline(
            title = stringResource(R.string.check_your_internet_connection),
            bloom = BloomState.Failed,
        )

        check == UpdatesCheckState.Error -> UpdateHeadline(
            title = stringResource(R.string.updates_check_failed),
            bloom = BloomState.Failed,
        )

        updates.isNotEmpty() -> UpdateHeadline(
            title = stringResource(R.string.header_update_ready),
            bloom = BloomState.Ready,
        )

        else -> UpdateHeadline(
            title = stringResource(R.string.updates_zero_state_title),
            bloom = BloomState.Resting,
        )
    }
}

@Composable
private fun getTitleForUpdateStatus(updates: List<Update>): String = when {
    updates.any { it.status == UpdateStatus.UPDATED_NEED_REBOOT } ->
        stringResource(R.string.installing_update_finished)

    updates.any { it.status == UpdateStatus.INSTALLATION_FAILED } ->
        stringResource(R.string.installing_update_error)

    updates.any {
        it.status == UpdateStatus.INSTALLING ||
                it.status == UpdateStatus.INSTALLATION_SUSPENDED
    } -> stringResource(R.string.installing_update)

    else -> stringResource(R.string.display_name)
}
