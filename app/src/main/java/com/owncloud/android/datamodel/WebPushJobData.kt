/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import androidx.work.Data
import com.nextcloud.client.jobs.UnifiedPushWork

sealed class WebPushJobData {
    abstract val inputData: Data

    data class Register(
        val accountName: String,
        val url: String,
        val uaPublicKey: String,
        val auth: String
    ): WebPushJobData() {
        override val inputData = Data.Builder()
            .putString(UnifiedPushWork.ACTION, UnifiedPushWork.ACTION_REGISTER)
            .putString(UnifiedPushWork.EXTRA_ACCOUNT, accountName)
            .putString(UnifiedPushWork.EXTRA_URL, url)
            .putString(UnifiedPushWork.EXTRA_UA_PUBKEY, uaPublicKey)
            .putString(UnifiedPushWork.EXTRA_AUTH, auth)
            .build()
    }
    data class Activate(val accountName: String, val token: String): WebPushJobData() {
        override val inputData = Data.Builder()
            .putString(UnifiedPushWork.ACTION, UnifiedPushWork.ACTION_ACTIVATE)
            .putString(UnifiedPushWork.EXTRA_ACCOUNT, accountName)
            .putString(UnifiedPushWork.EXTRA_TOKEN, token)
            .build()
    }
    data class Unregister(val accountName: String): WebPushJobData() {
        override val inputData = Data.Builder()
            .putString(UnifiedPushWork.ACTION, UnifiedPushWork.ACTION_UNREGISTER)
            .putString(UnifiedPushWork.EXTRA_ACCOUNT, accountName)
            .build()
    }
}