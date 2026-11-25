/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.util

import android.content.Intent
import com.owncloud.android.ui.activity.RequestCredentialsActivity

val Intent.isCredentialCheckSuccessful: Boolean
    get() = getIntExtra(
        RequestCredentialsActivity.KEY_CHECK_RESULT,
        RequestCredentialsActivity.KEY_CHECK_RESULT_FALSE
    ) == RequestCredentialsActivity.KEY_CHECK_RESULT_TRUE
