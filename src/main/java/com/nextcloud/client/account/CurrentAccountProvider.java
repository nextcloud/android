package com.nextcloud.client.account;

import android.accounts.Account;

import androidx.annotation.Nullable;

/**
 * This interface provides access to currently selected user Account.
 * @see UserAccountManager
 */
@FunctionalInterface
public interface CurrentAccountProvider {
    /**
     *  Get currently active account.
     *
     * @return Currently selected {@link Account} or first valid {@link Account} registered in OS or null, if not available at all.
     */
    @Nullable
    Account getCurrentAccount();
}
