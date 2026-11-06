/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.deviceinfo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsShape.CornerExtraLarge1
import com.android.settingslib.spa.framework.theme.SettingsSpace
import org.lineageos.updater.R
import org.lineageos.updater.ui.BloomState
import org.lineageos.updater.ui.PetalBloom
import org.lineageos.updater.ui.PetalTheme

private val BloomHeight = 148.dp
private val BloomGap = 12.dp
private val ScreenInset = 20.dp

/** The state of the phone's update, in words first and in the bloom second. */
data class UpdateHeadline(
    val title: String,
    val detail: String? = null,
    val bloom: BloomState,
)

@Composable
fun UpdaterCard(
    headline: UpdateHeadline,
    buildVersion: String,
    androidVersion: String,
    buildDate: String,
    securityPatch: String,
    modifier: Modifier = Modifier,
    shape: Shape = CornerExtraLarge1,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceBright,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = ScreenInset, top = ScreenInset, end = ScreenInset),
            verticalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall2),
        ) {
            Text(
                text = stringResource(R.string.header_build_version, buildVersion),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            // Nothing sits on the art, so this stays readable in every bloom state
            Text(
                text = headline.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
            headline.detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        PetalBloom(
            state = headline.bloom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = BloomGap)
                .height(BloomHeight),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ScreenInset),
            horizontalArrangement = Arrangement.spacedBy(SettingsSpace.small1),
        ) {
            InfoColumn(
                label = stringResource(R.string.header_android_label),
                value = androidVersion,
                modifier = Modifier.weight(1f),
            )
            InfoColumn(
                label = stringResource(R.string.build_date),
                value = buildDate,
                modifier = Modifier.weight(1f),
            )
            InfoColumn(
                label = stringResource(R.string.header_security_label),
                value = securityPatch,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun InfoColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall2),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@UiModePreviews
@Composable
private fun UpdaterCardPreview() {
    PetalTheme {
        UpdaterCard(
            headline = UpdateHeadline(
                title = "Downloading",
                detail = "42%",
                bloom = BloomState.Progress(0.42f),
            ),
            buildVersion = "1.0.1",
            androidVersion = "16",
            buildDate = "Oct 29",
            securityPatch = "Aug 2026",
        )
    }
}
