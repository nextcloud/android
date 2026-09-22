/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils

import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC

object SnackbarUtil {

    private val TAG = SnackbarUtil::class.java.simpleName
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    fun show(view: View?, @StringRes messageRes: Int, vararg formatArgs: Any?) {
        show(view, view.message(messageRes, formatArgs))
    }

    @JvmStatic
    fun show(view: View?, message: CharSequence?) {
        create(view, message, Snackbar.LENGTH_LONG)?.let { snackbar -> onMainThread(snackbar::show) }
    }

    @JvmStatic
    fun show(activity: Activity?, @StringRes messageRes: Int, vararg formatArgs: Any?) {
        val host = activity.hostView()
        show(host, host.message(messageRes, formatArgs))
    }

    @JvmStatic
    fun show(activity: Activity?, message: CharSequence?) {
        show(activity.hostView(), message)
    }

    @JvmStatic
    fun show(fragment: Fragment?, @StringRes messageRes: Int, vararg formatArgs: Any?) {
        val host = fragment?.activity.hostView()
        show(host, host.message(messageRes, formatArgs))
    }

    @JvmStatic
    fun showIndefinite(fragment: Fragment?, @StringRes messageRes: Int): Snackbar? =
        create(fragment?.activity.hostView(), messageRes, Snackbar.LENGTH_INDEFINITE)
            ?.also { snackbar -> onMainThread(snackbar::show) }

    @JvmStatic
    fun showServerOutdated(activity: Activity?, duration: Int) {
        create(activity.hostView(), R.string.outdated_server, duration)
            ?.setAction(R.string.dismiss) { }
            ?.let { snackbar -> onMainThread(snackbar::show) }
    }

    @JvmStatic
    fun dismiss(snackbar: Snackbar?) {
        snackbar?.let { onMainThread(snackbar::dismiss) }
    }

    @JvmStatic
    fun create(view: View?, message: CharSequence?, duration: Int): Snackbar? {
        if (view == null || message == null) {
            Log_OC.e(TAG, "Snackbar cannot be shown, host view or message is null")
            return null
        }

        return Snackbar.make(view, message, duration).anchoredToFloatingActionButton(view)
    }

    @JvmStatic
    fun create(view: View?, @StringRes messageRes: Int, duration: Int): Snackbar? =
        create(view, view?.context?.getText(messageRes), duration)

    @Suppress("SpreadOperator")
    private fun View?.message(@StringRes messageRes: Int, formatArgs: Array<out Any?>): CharSequence? {
        val context = this?.context ?: return null
        return when {
            formatArgs.isEmpty() -> context.getText(messageRes)
            else -> context.getString(messageRes, *formatArgs)
        }
    }

    private fun Activity?.hostView(): View? =
        this?.takeUnless { it.isFinishing || it.isDestroyed }?.findViewById(android.R.id.content)

    private fun Snackbar.anchoredToFloatingActionButton(host: View): Snackbar = apply {
        val fab = host.rootView.findViewById<View>(R.id.fab_main)
        if (fab != null && fab.isVisible && fab.isAttachedToWindow) {
            anchorView = fab
        }
    }

    private fun onMainThread(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainThreadHandler.post(action)
        }
    }
}
