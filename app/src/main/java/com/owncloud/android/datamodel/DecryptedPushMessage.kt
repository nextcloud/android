/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Unpublished <unpublished@users.noreply.github.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.datamodel

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

/*
 * Push data from server, https://github.com/nextcloud/notifications/blob/master/docs/push-v2.md#encrypted-subject-data
 */
@Parcelize
data class DecryptedPushMessage(
    val app: String,
    val type: String,
    val subject: String,
    val id: String,
    val nid: Int,
    val delete: Boolean,
    @SerializedName("delete-all")
    val deleteAll: Boolean
) : Parcelable
