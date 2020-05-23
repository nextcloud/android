package com.nextcloud.client.files

import android.content.Context
import android.net.Uri
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager


class DeepLinkHandler(
    private val context: Context,
    private val userAccountManager: UserAccountManager,
    private val onUserChoiceRequired: (users: List<User>, fileId: String)->Unit
) {

    /**
     * Open deep link.
     *
     * If deep link can be opened immediately, new activity is launched.
     * If link can be handled by multiple users, [onUserChoiceRequired] callback
     * is invoked with list of matching users.
     *
     * @param uri Deep link received in incoming [Intent]
     * @return true if deep link can be handled
     */
    fun openDeepLink(uri: Uri): Boolean {
        throw NotImplementedError()
    }
}
