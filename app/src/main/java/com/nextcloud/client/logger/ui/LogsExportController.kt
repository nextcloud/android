/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.logger.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.preference.Preference
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.nextcloud.client.logger.LogsExporter
import com.nextcloud.utils.mdm.MDMConfig
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException

@Suppress("DEPRECATION")
class LogsExportController(
    private val activity: Activity,
    private val exporter: LogsExporter,
    private val preference: Preference
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun export() {
        if (!preference.isEnabled || !MDMConfig.isLogEnabled(activity)) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            activity.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), STORAGE_PERMISSION_REQUEST)
            return
        }

        preference.isEnabled = false
        scope.launch {
            try {
                val message = if (exporter.export()) {
                    R.string.logs_export_to_downloads_success
                } else {
                    R.string.logs_export_to_downloads_empty
                }
                Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                showError(e)
            } catch (e: SecurityException) {
                showError(e)
            } finally {
                preference.isEnabled = true
            }
        }
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode != STORAGE_PERMISSION_REQUEST) return
        if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            export()
        } else {
            Toast.makeText(activity, R.string.logs_export_storage_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    fun close() {
        scope.cancel()
    }

    private fun showError(exception: Exception) {
        Log_OC.e(TAG, "Could not export logs", exception)
        Toast.makeText(activity, R.string.logs_export_to_downloads_failed, Toast.LENGTH_LONG).show()
    }

    companion object {
        private const val TAG = "LogsExportController"
        private const val STORAGE_PERMISSION_REQUEST = 101
    }
}
