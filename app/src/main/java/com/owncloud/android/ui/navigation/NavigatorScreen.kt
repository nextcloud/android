/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.navigation

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class NavigatorScreen : Parcelable {

    @Parcelize
    object Community : NavigatorScreen()
}
