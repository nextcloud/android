/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 XeniaCloud
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.ecosystem

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.util.Log
import android.view.View
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.core.utils.ecosystem.AccountReceiverCallback
import com.nextcloud.android.common.core.utils.ecosystem.EcosystemManager
import com.owncloud.android.R
import java.util.regex.Pattern

/**
 * Opens the XeniaCloud companion apps from the drawer's ecosystem tiles and from the
 * file-list "open in" button.
 *
 * This exists because android-common's [EcosystemManager] hardcodes upstream's package
 * names in its `EcosystemApp` enum, which is a plain enum with no string-resource seam
 * and no open member to subclass. There is nothing to override, so the sending half of
 * the logic is reimplemented here against [XeniaEcosystemApp] instead.
 *
 * Only the sending half. [receiveAccount] delegates straight to the library, which
 * carries no package names or branding on that path - it only reads the account name out
 * of an incoming intent.
 *
 * The wire contract is deliberately unchanged: the intent action is still
 * `com.nextcloud.intent.OPEN_ECOSYSTEM_APP` and the extra key is still `KEY_ACCOUNT`.
 * Both are opaque identifiers that Files, Talk and Notes have to agree on, not branding,
 * and renaming either would force a synchronised three-app release for no user benefit.
 */
class XeniaEcosystemManager(private val activity: Activity) {
    companion object {
        private const val TAG = "XeniaEcosystemManager"

        // Must match android-common's EcosystemManager, and the intent-filter in this
        // app's manifest. See the class doc.
        private const val ECOSYSTEM_INTENT_ACTION = "com.nextcloud.intent.OPEN_ECOSYSTEM_APP"
        private const val EXTRA_KEY_ACCOUNT = "KEY_ACCOUNT"
    }

    private val accountNamePattern = Pattern.compile(EcosystemManager.ACCOUNT_NAME_PATTERN_REGEX)

    // Only ever touched by receiveAccount; built lazily so the send path does not
    // pay for a second copy of the account-name Pattern.
    private val delegate by lazy { EcosystemManager(activity) }

    /**
     * Opens a XeniaCloud companion app, handing it the account to switch to.
     *
     * Unlike the library version, a missing app does not redirect to the Play Store:
     * XeniaCloud has no store listings yet, so that redirect would land the user on a
     * "not found" page. The user gets a snackbar and nothing else happens.
     *
     * @param app the companion app to open
     * @param accountName the account to hand over, e.g. "user@cloud.example.com"
     */
    fun openApp(app: XeniaEcosystemApp, accountName: String?) {
        if (accountName.isNullOrBlank()) {
            Log.w(TAG, "no account name given for ${app.name}")
            showSnackbar(R.string.xenia_ecosystem_account_missing)
            return
        }

        if (!accountNamePattern.matcher(accountName).matches()) {
            Log.w(TAG, "account name is not in the expected form")
            showSnackbar(R.string.xenia_ecosystem_account_invalid)
            return
        }

        val installedPackage = app.packageNames.firstOrNull { packageName ->
            activity.packageManager.getLaunchIntentForPackage(packageName) != null
        }

        if (installedPackage == null) {
            Log.w(TAG, "none of ${app.packageNames} is installed")
            showSnackbar(R.string.xenia_ecosystem_app_not_installed)
            return
        }

        val launchIntent = Intent(ECOSYSTEM_INTENT_ACTION).apply {
            setPackage(installedPackage)
            putExtra(EXTRA_KEY_ACCOUNT, accountName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        try {
            activity.startActivity(launchIntent)
        } catch (e: ActivityNotFoundException) {
            // The package is installed but does not handle the ecosystem action - an
            // older build, or one of the companion apps shipped without the filter.
            Log.e(TAG, "$installedPackage does not handle $ECOSYSTEM_INTENT_ACTION", e)
            showSnackbar(R.string.xenia_ecosystem_app_not_installed)
        }
    }

    /**
     * Handles an incoming ecosystem intent. Delegates to android-common unchanged.
     */
    fun receiveAccount(intent: Intent?, callback: AccountReceiverCallback) {
        delegate.receiveAccount(intent, callback)
    }

    private fun showSnackbar(messageRes: Int) {
        val rootContent = activity.findViewById<View>(android.R.id.content) ?: return
        Snackbar.make(rootContent, activity.getString(messageRes), Snackbar.LENGTH_LONG).show()
    }
}
