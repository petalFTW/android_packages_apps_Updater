/*
 * SPDX-FileCopyrightText: The petalOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.android.settingslib.spa.framework.theme.SettingsTheme

// Roles from vendor/petal/docs/DESIGN_GUIDANCE.md, the same ones petalos.xyz is built on.
private fun ColorScheme.petalDark() = copy(
    primary = Color(0xFFB9A5FF),
    onPrimary = Color(0xFF140E2B),
    primaryContainer = Color(0xFF3B3156),
    onPrimaryContainer = Color(0xFFF5F5F8),
    secondaryContainer = Color(0xFF3B3156),
    onSecondaryContainer = Color(0xFFF5F5F8),
    background = Color(0xFF0A0B10),
    surface = Color(0xFF0A0B10),
    surfaceContainerLowest = Color(0xFF0A0B10),
    surfaceContainerLow = Color(0xFF0A0B10),
    surfaceContainer = Color(0xFF0A0B10),
    surfaceBright = Color(0xFF191B23),
    surfaceContainerHigh = Color(0xFF22242E),
    surfaceContainerHighest = Color(0xFF22242E),
    onBackground = Color(0xFFF5F5F8),
    onSurface = Color(0xFFF5F5F8),
    onSurfaceVariant = Color(0xFFB9BAC4),
    outline = Color(0x33FFFFFF),
    outlineVariant = Color(0x1AFFFFFF),
    error = Color(0xFFFFB4AB),
)

private fun ColorScheme.petalLight() = copy(
    // the accent text role: white on the lighter #7C5CFF misses 4.5:1
    primary = Color(0xFF6844D6),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFECE7FF),
    onPrimaryContainer = Color(0xFF171921),
    secondaryContainer = Color(0xFFECE7FF),
    onSecondaryContainer = Color(0xFF171921),
    background = Color(0xFFF6F6FA),
    surface = Color(0xFFF6F6FA),
    surfaceContainerLowest = Color(0xFFF6F6FA),
    surfaceContainerLow = Color(0xFFF6F6FA),
    surfaceContainer = Color(0xFFF6F6FA),
    surfaceBright = Color(0xFFFCFCFF),
    surfaceContainerHigh = Color(0xFFECECF3),
    surfaceContainerHighest = Color(0xFFECECF3),
    onBackground = Color(0xFF171921),
    onSurface = Color(0xFF171921),
    onSurfaceVariant = Color(0xFF646A79),
    outline = Color(0x1F171921),
    outlineVariant = Color(0x14171921),
    error = Color(0xFFB3261E),
)

/** SettingsTheme keeps the type and spacing, the wallpaper-derived colours give way to petalOS. */
@Composable
fun PetalTheme(content: @Composable () -> Unit) {
    SettingsTheme {
        val isDark = isSystemInDarkTheme()
        val base = MaterialTheme.colorScheme
        val colorScheme = remember(base, isDark) {
            if (isDark) base.petalDark() else base.petalLight()
        }
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides colorScheme.onSurface,
                content = content,
            )
        }
    }
}
