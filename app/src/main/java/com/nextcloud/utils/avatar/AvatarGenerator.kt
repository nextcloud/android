/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.avatar

import android.accounts.AccountManager
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.account.User
import com.nextcloud.client.di.ApplicationScope
import com.nextcloud.client.di.IoDispatcher
import com.nextcloud.client.di.MainDispatcher
import com.owncloud.android.R
import com.owncloud.android.lib.common.accounts.AccountUtils
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.TextDrawable
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
class AvatarGenerator @Inject constructor(
    private val context: Context,
    private val accountManager: AccountManager,
    private val currentAccountProvider: CurrentAccountProvider,
    private val avatarLoader: AvatarLoader,
    @param:ApplicationScope private val scope: CoroutineScope,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:MainDispatcher private val mainDispatcher: CoroutineDispatcher
) {

    companion object {
        private const val TAG = "AvatarGenerator"
        private const val MAX_PARALLEL_DOWNLOADS = 4
    }

    private val downloadDispatcher = ioDispatcher.limitedParallelism(MAX_PARALLEL_DOWNLOADS)

    fun setAccountAvatar(user: User, listener: AvatarGenerationListener, avatarRadius: Float, callContext: Any) {
        val userId = accountManager.getUserData(user.toPlatformAccount(), AccountUtils.Constants.KEY_USER_ID)

        if (userId == null) {
            Log_OC.e(TAG, "user id is null, cannot set avatar")
            return
        }

        setUserAvatar(userId, listener, avatarRadius, callContext, user = user)
    }

    @JvmOverloads
    fun setUserAvatar(
        userId: String,
        listener: AvatarGenerationListener,
        avatarRadius: Float,
        callContext: Any,
        displayName: String? = userId,
        user: User = currentAccountProvider.user,
        avatarBorder: Int = 0
    ) {
        (callContext as? View)?.contentDescription = user.toPlatformAccount().hashCode().toString()

        val request = AvatarRequest(user, userId, displayName, avatarRadius)
        val hasUserId = userId.isNotEmpty()

        val placeholder = if (hasUserId) {
            createInitialAvatar(displayName, avatarRadius - avatarBorder)
        } else {
            createLinkAvatar(callContext)
        }
        listener.avatarGenerated(placeholder, callContext)

        val listenerReference = WeakReference(listener)
        scope.launch(ioDispatcher) {
            if (hasUserId) {
                avatarLoader.loadCached(request)?.let { deliver(listenerReference, request, it, callContext) }
            }

            val avatar = withContext(downloadDispatcher) { avatarLoader.load(request) } ?: return@launch
            deliver(listenerReference, request, avatar, callContext)
        }
    }

    private suspend fun deliver(
        listenerReference: WeakReference<AvatarGenerationListener>,
        request: AvatarRequest,
        avatar: Drawable,
        callContext: Any
    ) = withContext(mainDispatcher) {
        val listener = listenerReference.get() ?: return@withContext

        if (listener.shouldCallGeneratedCallback(request.accountName, callContext)) {
            listener.avatarGenerated(avatar, callContext)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun createInitialAvatar(displayName: String?, radius: Float): Drawable? = try {
        TextDrawable.createAvatarByUserId(displayName, radius)
    } catch (e: Exception) {
        Log_OC.e(TAG, "Error calculating RGB value for active account icon.", e)
        ResourcesCompat.getDrawable(context.resources, R.drawable.account_circle_white, null)
    }

    private fun createLinkAvatar(callContext: Any): Drawable? {
        val themedContext = (callContext as? View)?.context ?: context

        return ContextCompat.getDrawable(themedContext, R.drawable.ic_link)?.apply {
            setTint(ContextCompat.getColor(themedContext, R.color.icon_on_nc_grey))
        }
    }
}
