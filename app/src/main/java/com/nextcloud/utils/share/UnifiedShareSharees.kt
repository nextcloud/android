/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.share

import com.nextcloud.android.common.ui.network.auth.ServerCredentials
import com.nextcloud.android.common.ui.share.avatar.ShareAvatarRepository
import com.nextcloud.android.common.ui.share.model.api.share.Share
import com.nextcloud.client.account.User
import com.nextcloud.client.core.ClockImpl
import com.nextcloud.utils.extensions.supportsUnifiedShare
import com.nextcloud.utils.extensions.toServerCredentials
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.lib.resources.shares.ShareeUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * Replaces the sharees PROPFIND reported with the ones the unified share API reports, at the point where the files
 * are written, so that readers take them straight from the [OCFile].
 *
 * This is the preferred approach only while the unified share system does not expose the legacy sharee data through
 * PROPFIND. Once it is backward compatible and ships the sharees with the file listing again, the listing alone
 * carries everything the UI needs and these extra requests can go away.
 */
object UnifiedShareSharees {
    private const val MAX_CONCURRENT_REQUESTS = 8
    private val UNIFIED_SHARE_SUPPORT_CACHE_TIME = 10.minutes

    private val clock = ClockImpl()
    private val unifiedShareSupport = ConcurrentHashMap<String, Pair<Duration, Boolean>>()

    suspend fun fill(user: User, files: List<OCFile>) {
        if (files.isEmpty()) {
            return
        }

        withContext(Dispatchers.IO) {
            val credentials = user.toServerCredentials() ?: return@withContext
            if (!supportsUnifiedShare(user.accountName, credentials)) {
                return@withContext
            }

            val repository = ShareAvatarRepository(credentials)
            val requestLimit = Semaphore(MAX_CONCURRENT_REQUESTS)

            files
                .map { file ->
                    async {
                        requestLimit.withPermit {
                            runCatching { repository.fetchSharees(file) }
                        }
                    }
                }
                .awaitAll()
        }
    }

    @JvmStatic
    fun fillBlocking(user: User, files: List<OCFile>) {
        runCatching {
            runBlocking { fill(user, files) }
        }
    }

    private suspend fun supportsUnifiedShare(accountName: String, credentials: ServerCredentials): Boolean =
        cachedUnifiedShareSupport(accountName)
            ?: runCatching { credentials.supportsUnifiedShare() }
                .getOrNull()
                ?.also { unifiedShareSupport[accountName] = Pair(timeSinceBoot(), it) }
            ?: false

    private fun cachedUnifiedShareSupport(accountName: String): Boolean? {
        val (checkedAt, isSupported) = unifiedShareSupport[accountName] ?: return null
        val isExpired = timeSinceBoot() - checkedAt >= UNIFIED_SHARE_SUPPORT_CACHE_TIME
        return if (isExpired) null else isSupported
    }

    private fun timeSinceBoot(): Duration = clock.millisSinceBoot.milliseconds

    private suspend fun ShareAvatarRepository.fetchSharees(file: OCFile) {
        val shares = fetchShareAvatars(file.localId.toString()) ?: return
        file.sharees = shares.toAvatarSharees()
    }

    private fun List<Share>.toAvatarSharees(): List<ShareeUser> = asSequence()
        .flatMap { share -> share.invitedRecipients }
        .distinctBy { recipient -> recipient.value }
        .map { recipient -> ShareeUser(recipient.value, recipient.displayName, ShareType.USER) }
        .toList()
}
