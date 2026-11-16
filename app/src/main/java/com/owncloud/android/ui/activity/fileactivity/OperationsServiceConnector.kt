/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activity.fileactivity

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.services.OperationsService
import com.owncloud.android.services.OperationsService.OperationsServiceBinder

class OperationsServiceConnector(
    private val activity: Activity,
    private val listener: OnRemoteOperationListener,
    private val onBoundWhileResumed: (OperationsServiceBinder) -> Unit
) : ServiceConnection {

    var binder: OperationsServiceBinder? = null
        private set

    private var isBound = false
    private var isActivityResumed = false

    fun bind() {
        activity.bindService(Intent(activity, OperationsService::class.java), this, Context.BIND_AUTO_CREATE)
        isBound = true
    }

    fun unbind() {
        if (!isBound) return
        activity.unbindService(this)
        binder = null
    }

    fun onResume() {
        isActivityResumed = true
        binder?.let(onBoundWhileResumed)
    }

    fun onPause() {
        binder?.removeOperationListener(listener)
        isActivityResumed = false
    }

    override fun onServiceConnected(component: ComponentName, service: IBinder) {
        if (!component.isOperationsService()) return

        Log_OC.d(TAG, "Operations service connected")
        val connectedBinder = service as OperationsServiceBinder
        binder = connectedBinder
        if (isActivityResumed) {
            onBoundWhileResumed(connectedBinder)
        }
    }

    override fun onServiceDisconnected(component: ComponentName) {
        if (!component.isOperationsService()) return

        Log_OC.d(TAG, "Operations service disconnected")
        binder = null
    }

    private fun ComponentName.isOperationsService(): Boolean =
        this == ComponentName(activity, OperationsService::class.java)

    companion object {
        private const val TAG = "OperationsServiceConnector"
    }
}
