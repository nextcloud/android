/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.core

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder

/**
 * This is a local service connection providing a foundation for service
 * communication logic.
 *
 * One can subclass it to create own service interaction API.
 */
abstract class LocalConnection<S : Service>(protected val context: Context) : ServiceConnection {

    private var serviceBinder: LocalBinder<S>? = null
    val service: S? get() = serviceBinder?.service
    val isConnected: Boolean get() {
        return serviceBinder != null
    }

    /**
     * Override this method to create custom binding intent.
     * Default implementation returns null, which disables binding.
     *
     * @see [bind]
     */
    protected open fun createBindIntent(): Intent? = null

    /**
     * Bind local service. If [createBindIntent] returns null, it no-ops.
     */
    fun bind() {
        createBindIntent()?.let {
            context.bindService(it, this, Context.BIND_AUTO_CREATE)
        }
    }

    /**
     * Unbind service if it is bound.
     * If service is not bound, it no-ops.
     */
    fun unbind() {
        if (isConnected) {
            onUnbind()
            context.unbindService(this)
            serviceBinder = null
        }
    }

    /**
     * Callback called when connection binds to a service.
     * Any actions taken on service connection can be taken here.
     */
    protected open fun onBound(binder: IBinder) {
        // default no-op
    }

    /**
     * Callback called when service is about to be unbound.
     * Binder is still valid at this stage and can be used to
     * perform cleanups. After exiting this method, service will
     * no longer be available.
     */
    protected open fun onUnbind() {
        // default no-op
    }

    final override fun onServiceConnected(name: ComponentName, binder: IBinder) {
        if (binder !is LocalBinder<*>) {
            throw IllegalStateException("Binder is not extending ${LocalBinder::class.java.name}")
        }
        serviceBinder = binder as LocalBinder<S>
        onBound(binder)
    }

    final override fun onServiceDisconnected(name: ComponentName) {
        serviceBinder = null
    }
}
