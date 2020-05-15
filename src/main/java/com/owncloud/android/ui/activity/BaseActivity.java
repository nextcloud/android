package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.mixins.MixinRegistry;
import com.nextcloud.client.mixins.SessionMixin;
import com.nextcloud.client.preferences.AppPreferences;
import com.nextcloud.client.preferences.DarkMode;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OCCapability;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
        super.onCreate(savedInstanceState);
        sessionMixin = new SessionMixin(this,
                                        getContentResolver(),
                                        accountManager);
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
        mixinRegistry.onResume();
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
        mixinRegistry.onRestart();
    }

    private void onThemeSettingsModeChanged() {
        if (paused) {
            themeChangePending = true;
        } else {
            recreate();
        }
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

    /**
     * Launches the account creation activity.
     */
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
        return sessionMixin.getStorageManager();
    }
}
