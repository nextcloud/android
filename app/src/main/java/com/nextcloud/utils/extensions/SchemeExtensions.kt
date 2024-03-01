/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2024 Alper Ozturk
 * Copyright (C) 2024 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.utils.extensions

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.vanniktech.ui.color
import scheme.Scheme

fun Scheme.toColorScheme(): ColorScheme {
    return ColorScheme(
        primary = Color(primary.color.argb),
        onPrimary = Color(onPrimary.color.argb),
        primaryContainer = Color(primaryContainer.color.argb),
        onPrimaryContainer = Color(onPrimaryContainer.color.argb),
        inversePrimary = Color(inversePrimary.color.argb),
        secondary = Color(secondary.color.argb),
        onSecondary = Color(onSecondary.color.argb),
        secondaryContainer = Color(secondaryContainer.color.argb),
        onSecondaryContainer = Color(onSecondaryContainer.color.argb),
        tertiary = Color(tertiary.color.argb),
        onTertiary = Color(onTertiary.color.argb),
        tertiaryContainer = Color(tertiaryContainer.color.argb),
        onTertiaryContainer = Color(onTertiaryContainer.color.argb),
        background = Color(background.color.argb),
        onBackground = Color(onBackground.color.argb),
        surface = Color(surface.color.argb),
        onSurface = Color(onSurface.color.argb),
        surfaceVariant = Color(surfaceVariant.color.argb),
        onSurfaceVariant = Color(onSurfaceVariant.color.argb),
        surfaceTint = Color(surfaceVariant.color.argb),
        inverseSurface = Color(inverseSurface.color.argb),
        inverseOnSurface = Color(inverseOnSurface.color.argb),
        error = Color(error.color.argb),
        onError = Color(onError.color.argb),
        errorContainer = Color(errorContainer.color.argb),
        onErrorContainer = Color(onErrorContainer.color.argb),
        outline = Color(outline.color.argb),
        outlineVariant = Color(outlineVariant.color.argb),
        scrim = Color(scrim.color.argb)
    )
}
