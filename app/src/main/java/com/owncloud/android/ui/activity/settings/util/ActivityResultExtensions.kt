/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.settings.util

import android.app.Activity
import android.content.Intent
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.owncloud.android.ui.activity.RequestCredentialsActivity

fun ActivityResultCaller.registerForResult(onResult: (ActivityResult) -> Unit): ActivityResultLauncher<Intent> =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult(), onResult)

val ActivityResult.isOk: Boolean
    get() = resultCode == Activity.RESULT_OK

val Intent.isCredentialCheckSuccessful: Boolean
    get() = getIntExtra(
        RequestCredentialsActivity.KEY_CHECK_RESULT,
        RequestCredentialsActivity.KEY_CHECK_RESULT_FALSE
    ) == RequestCredentialsActivity.KEY_CHECK_RESULT_TRUE
