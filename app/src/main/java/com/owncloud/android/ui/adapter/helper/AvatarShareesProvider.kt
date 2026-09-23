/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.helper

import android.content.Context
import android.content.res.Resources
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AvatarShareesProvider {

    fun get(file: OCFile, userId: String?): List<ShareeUser> {
        val sharees = file.sharees

        val ownerSharee = file.ownerId
            ?.takeIf { it.isNotEmpty() && it != userId }
            ?.let { ShareeUser(it, file.ownerDisplayName, ShareType.USER) }
            ?.takeIf { it !in sharees }

        return listOfNotNull(ownerSharee) + sharees.asReversed()
    }

    companion object {
        private val cachedAvatarScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        @JvmStatic
        @Suppress("LongParameterList")
        fun showCachedAvatar(
            userId: String,
            serverName: String,
            listener: AvatarGenerationListener,
            resources: Resources,
            callContext: Any,
            context: Context
        ) {
            val accountName = "$userId@$serverName"

            cachedAvatarScope.launch {
                val eTag = ArbitraryDataProviderImpl(context)
                    .getValue(accountName, ThumbnailsCacheManager.AVATAR)
                val cachedBitmap = ThumbnailsCacheManager
                    .getBitmapFromDiskCache("a_${userId}_${serverName}_$eTag") ?: return@launch
                val avatar = BitmapUtils.bitmapToCircularBitmapDrawable(resources, cachedBitmap)

                withContext(Dispatchers.Main) {
                    if (listener.shouldCallGeneratedCallback(accountName, callContext)) {
                        listener.avatarGenerated(avatar, callContext)
                    }
                }
            }
        }
    }
}
