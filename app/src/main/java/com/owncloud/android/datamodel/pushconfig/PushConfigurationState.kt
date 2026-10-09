/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.datamodel.pushconfig

import com.nextcloud.utils.serialization.AppJson
import kotlinx.serialization.Serializable

@Serializable
data class PushConfigurationState(
    val pushToken: String,
    val deviceIdentifier: String,
    val deviceIdentifierSignature: String,
    val userPublicKey: String,
    var shouldBeDeleted: Boolean
) {
    companion object {
        @JvmStatic
        fun fromJson(value: String): PushConfigurationState = AppJson.instance.decodeFromString(value)

        @JvmStatic
        fun toJson(value: PushConfigurationState): String = AppJson.instance.encodeToString(value)
    }
}
