/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.network.local

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.utils.PermissionUtil
import kotlinx.coroutines.launch

class LocalNetworkPermissionRequester(
    private val activity: AppCompatActivity,
    private val onPermissionGranted: () -> Unit
) {
    private var pendingResult: ((Boolean) -> Unit)? = null
    private val routes by lazy { LocalNetworkRoutes(activity) }

    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission(), ::deliverResult)

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
            if (serverUrls.none { LocalNetworkAddress.isLocalServer(it, routes) }) {
                onGranted()
                return@launch
            }

            launchRequest(onGranted, onDenied)
        }
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun launchRequest(onGranted: () -> Unit, onDenied: () -> Unit) {
        pendingResult = { isGranted -> handleResult(isGranted, onGranted, onDenied) }
        permissionLauncher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun handleResult(isGranted: Boolean, onGranted: () -> Unit, onDenied: () -> Unit) {
        if (isGranted) {
            onPermissionGranted()
            onGranted()
            return
        }

        showDeniedSnackbar(onGranted, onDenied)
        onDenied()
    }

    private fun deliverResult(isGranted: Boolean) {
        val callback = pendingResult ?: return
        pendingResult = null
        callback(isGranted)
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun showDeniedSnackbar(onGranted: () -> Unit, onDenied: () -> Unit) {
        val canShowSystemDialog =
            activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_LOCAL_NETWORK)
        val message = if (canShowSystemDialog) {
            R.string.local_network_permission_denied
        } else {
            R.string.local_network_permission_settings_path
        }
        val snackbar = SnackbarUtil.create(activity.findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG)
            ?: return

        if (canShowSystemDialog) {
            snackbar.setAction(R.string.permission_allow) { launchRequest(onGranted, onDenied) }
        } else {
            snackbar.setAction(R.string.actionbar_settings) { openAppSettings() }
        }

        snackbar.show()
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
