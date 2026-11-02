/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.utils.PermissionUtil
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.launch

class LocalNetworkPermissionRequester(
    private val activity: AppCompatActivity,
    private val viewThemeUtils: () -> ViewThemeUtils,
    private val onPermissionGranted: () -> Unit
) {
    private var pendingResult: ((Boolean) -> Unit)? = null

    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission(), ::deliverResult)

    private val appName: String
        get() = activity.getString(R.string.app_name)

    fun requestIfNeeded(serverUrls: List<String>, onGranted: () -> Unit = {}, onDenied: () -> Unit = {}) {
        if (!PermissionUtil.isLocalNetworkPermissionRequired(activity)) {
            onGranted()
            return
        }

        requestForLocalServers(serverUrls, onGranted, onDenied)
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun requestForLocalServers(serverUrls: List<String>, onGranted: () -> Unit, onDenied: () -> Unit) {
        if (PermissionUtil.hasLocalNetworkPermission(activity)) {
            onGranted()
            return
        }

        activity.lifecycleScope.launch {
            if (serverUrls.none { LocalNetworkAddress.isLocalServer(it) }) {
                onGranted()
                return@launch
            }

            pendingResult = { isGranted ->
                if (isGranted) {
                    onPermissionGranted()
                    onGranted()
                } else {
                    showDeniedSnackbar()
                    onDenied()
                }
            }

            if (activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_LOCAL_NETWORK)) {
                showRationale()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun showRationale() {
        val builder = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.local_network_permission_title)
            .setMessage(activity.getString(R.string.local_network_permission_rationale, appName))
            .setPositiveButton(R.string.common_ok) { _, _ ->
                permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }
            .setNegativeButton(R.string.common_cancel) { _, _ -> deliverResult(isGranted = false) }
            .setOnCancelListener { deliverResult(isGranted = false) }

        viewThemeUtils().dialog.colorMaterialAlertDialogBackground(activity, builder)
        builder.show()
    }

    private fun deliverResult(isGranted: Boolean) {
        val callback = pendingResult ?: return
        pendingResult = null
        callback(isGranted)
    }

    private fun showDeniedSnackbar() {
        val message = activity.getString(R.string.local_network_permission_denied, appName)
        SnackbarUtil.create(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            ?.setAction(R.string.actionbar_settings) { openAppSettings() }
            ?.show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts(PACKAGE_SCHEME, activity.packageName, null)
        )
        activity.startActivity(intent)
    }

    companion object {
        private const val PACKAGE_SCHEME = "package"
    }
}
