/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2016-2024 Nextcloud GmbH and Nextcloud contributors
 * SPDX-FileCopyrightText: 2024 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.mixins.MixinRegistry;
import com.nextcloud.client.mixins.SessionMixin;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.DarkMode;
import com.nextcloud.utils.extensions.ActivityExtensionsKt;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OCCapability;

import java.util.Optional;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

/**
 * Base activity with common behaviour for activities dealing with ownCloud {@link Account}s .
 */
public abstract class BaseActivity extends AppCompatActivity implements Injectable {

    private static final String TAG = BaseActivity.class.getSimpleName();

    /**
     * Tracks whether the activity should be recreate()'d after a theme change
     */
    private boolean themeChangePending;
    private boolean paused;
    protected boolean enableAccountHandling = true;

    private MixinRegistry mixinRegistry = new MixinRegistry();
    private SessionMixin sessionMixin;

    @Inject UserAccountManager accountManager;
    @Inject AppPreferences preferences;
    @Inject FileDataStorageManager fileDataStorageManager;

    private AppPreferences.Listener onPreferencesChanged = new AppPreferences.Listener() {
        @Override
        public void onDarkThemeModeChanged(DarkMode mode) {
            onThemeSettingsModeChanged();
        }
    };

    public UserAccountManager getUserAccountManager() {
        return accountManager;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        addBottomMarginIfNavBarActive();
        super.onCreate(savedInstanceState);
        sessionMixin = new SessionMixin(this, accountManager);
        mixinRegistry.add(sessionMixin);

        if (enableAccountHandling) {
            mixinRegistry.onCreate(savedInstanceState);
        }
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        preferences.addListener(onPreferencesChanged);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mixinRegistry.onDestroy();
        preferences.removeListener(onPreferencesChanged);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mixinRegistry.onPause();
        paused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (enableAccountHandling) {
            mixinRegistry.onResume();
        }
        paused = false;

        if (themeChangePending) {
            recreate();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        mixinRegistry.onNewIntent(intent);
    }

    @Override
    protected void onRestart() {
        Log_OC.v(TAG, "onRestart() start");
        super.onRestart();
        if (enableAccountHandling) {
            mixinRegistry.onRestart();
        }
    }

    private void onThemeSettingsModeChanged() {
        if (paused) {
            themeChangePending = true;
        } else {
            recreate();
        }
    }

    private void addBottomMarginIfNavBarActive() {
        ViewCompat.setOnApplyWindowInsetsListener(getWindow().getDecorView(), (view, windowInsetsCompat) -> {
            View contentView = findViewById(android.R.id.content);

            if (contentView != null && contentView.getLayoutParams() instanceof ViewGroup.MarginLayoutParams params) {
                params.bottomMargin = ActivityExtensionsKt.navBarHeight(this, windowInsetsCompat);
                contentView.setLayoutParams(params);
            }

            return windowInsetsCompat;
        });
    }

    /**
     * Sets and validates the ownCloud {@link Account} associated to the Activity.
     *
     * If not valid, tries to swap it for other valid and existing ownCloud {@link Account}.
     *
     * @param account      New {@link Account} to set.
     * @param savedAccount When 'true', account was retrieved from a saved instance state.
     */
    @Deprecated
    protected void setAccount(Account account, boolean savedAccount) {
        sessionMixin.setAccount(account);
    }

    protected void setUser(User user) {
        sessionMixin.setUser(user);
    }

    protected void startAccountCreation() {
        sessionMixin.startAccountCreation();
    }

    /**
     * Getter for the capabilities of the server where the current OC account lives.
     *
     * @return Capabilities of the server where the current OC account lives. Null if the account is not
     * set yet.
     */
    public OCCapability getCapabilities() {
        return sessionMixin.getCapabilities();
    }

    /**
     * Getter for the ownCloud {@link Account} where the main {@link OCFile} handled by the activity
     * is located.
     *
     * @return OwnCloud {@link Account} where the main {@link OCFile} handled by the activity
     * is located.
     */
    public Account getAccount() {
        return sessionMixin.getCurrentAccount();
    }

    public Optional<User> getUser() {
        return sessionMixin.getUser();
    }

    public FileDataStorageManager getStorageManager() {
        return fileDataStorageManager;
    }
}
