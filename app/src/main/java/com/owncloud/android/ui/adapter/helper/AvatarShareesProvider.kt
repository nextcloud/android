/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.adapter.helper

import android.content.Context
import android.content.res.Resources
import com.nextcloud.android.common.ui.network.auth.ServerCredentials
import com.nextcloud.android.common.ui.share.avatar.ShareAvatarRepository
import com.nextcloud.android.common.ui.share.model.api.share.Share
import com.nextcloud.client.account.User
import com.nextcloud.utils.extensions.supportsUnifiedShare
import com.nextcloud.utils.extensions.toServerCredentials
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AvatarShareesProvider {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val cache = object : LinkedHashMap<Long, List<ShareeUser>>(
        CACHE_INITIAL_CAPACITY,
        CACHE_LOAD_FACTOR,
        true
    ) {
        override fun removeEldestEntry(eldest: Map.Entry<Long, List<ShareeUser>>): Boolean = size > CACHE_MAX_SIZE
    }

    private val pendingRequests = mutableMapOf<Long, MutableList<(List<ShareeUser>) -> Unit>>()

    private var unifiedShareCredentials: Deferred<ServerCredentials?>? = null

    fun invalidate(file: OCFile) {
        cache.remove(file.fileId)
    }

    fun get(file: OCFile, user: User?, userId: String?, onSharees: (List<ShareeUser>) -> Unit) {
        val fileId = file.fileId

        cache[fileId]?.let { cached ->
            onSharees(cached)
            return
        }

        val localSharees = file.toLocalSharees(userId)
        onSharees(localSharees)

        resolveRemoteSharees(file, user, localSharees, onSharees)
    }

    private fun resolveRemoteSharees(
        file: OCFile,
        user: User?,
        localSharees: List<ShareeUser>,
        onSharees: (List<ShareeUser>) -> Unit
    ) {
        val fileId = file.fileId

        if (user == null) {
            cache[fileId] = localSharees
            return
        }

        pendingRequests[fileId]?.let { waitingCallbacks ->
            waitingCallbacks.add(onSharees)
            return
        }
        pendingRequests[fileId] = mutableListOf(onSharees)

        val credentials = unifiedShareCredentials
            ?: scope.async { resolveUnifiedShareCredentials(user) }.also { unifiedShareCredentials = it }

        scope.launch {
            val sharees = credentials.await()?.let { fetchRemoteSharees(it, file) } ?: localSharees

            withContext(Dispatchers.Main) {
                cache[fileId] = sharees
                pendingRequests.remove(fileId)?.forEach { callback -> callback(sharees) }
            }
        }
    }

    fun cleanup() {
        cache.clear()
        pendingRequests.clear()
        unifiedShareCredentials = null
        scope.coroutineContext.cancelChildren()
    }

    private suspend fun resolveUnifiedShareCredentials(user: User): ServerCredentials? = runCatching {
        user.toServerCredentials()?.takeIf { it.supportsUnifiedShare() }
    }.getOrNull()

    private suspend fun fetchRemoteSharees(credentials: ServerCredentials, file: OCFile): List<ShareeUser>? =
        runCatching {
            ShareAvatarRepository(credentials)
                .fetchShareAvatars(file.localId.toString())
                ?.toAvatarSharees()
        }.getOrNull()

    private fun OCFile.toLocalSharees(userId: String?): List<ShareeUser> {
        val ownerSharee = ownerId
            ?.takeIf { it.isNotEmpty() && it != userId }
            ?.let { ShareeUser(it, ownerDisplayName, ShareType.USER) }
            ?.takeIf { it !in sharees }

        return listOfNotNull(ownerSharee) + sharees.asReversed()
    }

    private fun List<Share>.toAvatarSharees(): List<ShareeUser> = asSequence()
        .flatMap { share -> share.invitedRecipients }
        .distinctBy { recipient -> recipient.value }
        .map { recipient -> ShareeUser(recipient.value, recipient.displayName, ShareType.USER) }
        .toList()

    companion object {
        private const val CACHE_MAX_SIZE = 500
        private const val CACHE_INITIAL_CAPACITY = 64
        private const val CACHE_LOAD_FACTOR = 0.75f

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
