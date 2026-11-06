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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.core.network.LocalNetworkPermission
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import kotlinx.coroutines.launch

class LocalNetworkPermissionRequester(
    private val activity: AppCompatActivity,
    private val hasLocalServer: suspend (List<String>) -> Boolean,
    private val onPermissionGranted: () -> Unit
) {
    private var pendingResult: ((Boolean) -> Unit)? = null
    private var pendingSettingsGrant: (() -> Unit)? = null

    private val permissionLauncher =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission(), ::deliverResult)

    init {
        activity.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    deliverSettingsResult()
                }
            }
        )
    }

    fun requestIfNeeded(serverUrls: List<String>, onGranted: () -> Unit = {}, onDenied: () -> Unit = {}) {
        if (!LocalNetworkPermission.isRequired(activity)) {
            onGranted()
            return
        }

        requestForLocalServers(serverUrls, onGranted, onDenied)
    }

    @RequiresApi(Build.VERSION_CODES.CINNAMON_BUN)
    private fun requestForLocalServers(serverUrls: List<String>, onGranted: () -> Unit, onDenied: () -> Unit) {
        if (LocalNetworkPermission.isGranted(activity)) {
            onGranted()
            return
        }

        activity.lifecycleScope.launch {
            if (!hasLocalServer(serverUrls)) {
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

    private fun deliverSettingsResult() {
        val onGranted = pendingSettingsGrant ?: return
        pendingSettingsGrant = null

        if (!LocalNetworkPermission.isRequired(activity) ||
            !LocalNetworkPermission.isGranted(activity)
        ) {
            return
        }

        onPermissionGranted()
        onGranted()
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
        val duration = if (canShowSystemDialog) Snackbar.LENGTH_LONG else Snackbar.LENGTH_INDEFINITE
        val snackbar = SnackbarUtil.create(activity.findViewById(android.R.id.content), message, duration)
            ?: return

        if (canShowSystemDialog) {
            snackbar.setAction(R.string.permission_allow) { launchRequest(onGranted, onDenied) }
        } else {
            snackbar.setAction(R.string.actionbar_settings) { openAppSettings(onGranted) }
        }

        snackbar.show()
    }

    private fun openAppSettings(onGranted: () -> Unit) {
        pendingSettingsGrant = onGranted
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
