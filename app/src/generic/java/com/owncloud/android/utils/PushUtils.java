/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils;

import android.content.Context;

import android.accounts.Account;

import com.google.gson.Gson;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.preferences.AppPreferencesImpl;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.PushConfigurationState;
import com.owncloud.android.datamodel.SignatureVerification;
import com.nextcloud.unifiedpush.UnifiedPush;
import com.owncloud.android.ui.activity.DrawerActivity;

import java.security.Key;

public final class PushUtils {
    public static final String KEY_PUSH = "push";

    private PushUtils() {
    }

    public static void updateRegistrationsWithServer(final DrawerActivity activity,
                                                     final UserAccountManager accountManager,
                                                     final String pushToken) {
        for (Account account : accountManager.getAccounts()) {
            PushConfigurationState accountPushData = new Gson().fromJson(
                new ArbitraryDataProviderImpl(MainApp.getAppContext()).getValue(account.name, KEY_PUSH),
                PushConfigurationState.class
            );
            if ((accountPushData == null) || (accountPushData.isShouldBeDeleted() == false)) {
                UnifiedPush.Companion.registerForPushMessaging(activity, account.name);
            } else {
                UnifiedPush.Companion.unregisterForPushMessaging(account.name);
            }
        }
    }

    public static void updateRegistrationsWithServerNoUI(final UserAccountManager accountManager,
                                                         final String pushToken) {
        updateRegistrationsWithServer(null, accountManager, pushToken);
    }

    public static void reinitKeys(UserAccountManager accountManager) {
        Context context = MainApp.getAppContext();
        AppPreferencesImpl.fromContext(context).setKeysReInitEnabled();
    }

    public static Key readKeyFromFile(boolean readPublicKey) {
        return null;
    }

    public static SignatureVerification verifySignature(
        final Context context,
        final UserAccountManager accountManager,
        final byte[] signatureBytes,
        final byte[] subjectBytes
    ) {
        return null;
    }

}
