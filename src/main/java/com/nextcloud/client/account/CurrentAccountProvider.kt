package com.nextcloud.client.account

import android.accounts.Account

/**
 * This interface provides access to currently selected user.
 *
 * @see UserAccountManager
 */
interface CurrentAccountProvider {
    /**
     * Get currently active account.
     *
     * @return Currently selected [Account] or first valid [Account] registered in OS or null, if not available at all.
     */
    @get:Deprecated("Replaced by getUser()", replaceWith = ReplaceWith("user"))
    val currentAccount: Account?

    /**
     * Get currently active user profile. If there is no active user, anonymous user is returned.
     *
     * @return User profile. Profile is never null.
     */
    val user: User
        get() = AnonymousUser("dummy")
}
