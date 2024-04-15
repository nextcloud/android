/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.utils.enterpriseReporter

import android.content.Context
import androidx.enterprise.feedback.KeyedAppState
import androidx.enterprise.feedback.KeyedAppStatesCallback
import androidx.enterprise.feedback.KeyedAppStatesCallback.STATUS_EXCEEDED_BUFFER_ERROR
import androidx.enterprise.feedback.KeyedAppStatesCallback.STATUS_SUCCESS
import androidx.enterprise.feedback.KeyedAppStatesCallback.STATUS_TRANSACTION_TOO_LARGE_ERROR
import androidx.enterprise.feedback.KeyedAppStatesCallback.STATUS_UNKNOWN_ERROR
import androidx.enterprise.feedback.KeyedAppStatesReporter
import com.owncloud.android.lib.common.utils.Log_OC

fun Context.enterpriseFeedback(
    messageId: Int,
    severity: Int = KeyedAppState.SEVERITY_ERROR
) {
    val keyedAppStatesReporter = KeyedAppStatesReporter.create(this)
    val keyedAppStateMessage = KeyedAppState.builder()
        .setSeverity(severity)
        .setMessage(getString(messageId))
        .build()
    val list: MutableList<KeyedAppState> = ArrayList()
    list.add(keyedAppStateMessage)
    keyedAppStatesReporter.setStates(list, Callback())
}

internal class Callback : KeyedAppStatesCallback {
    override fun onResult(state: Int, throwable: Throwable?) {
        when (state) {
            STATUS_SUCCESS ->
                Log_OC.i("ErrorReporter", "KeyedAppStatesCallback status: SUCCESS ")
            STATUS_UNKNOWN_ERROR ->
                Log_OC.i("ErrorReporter", "KeyedAppStatesCallback status: UNKNOWN_ERROR ")
            STATUS_TRANSACTION_TOO_LARGE_ERROR ->
                Log_OC.i("ErrorReporter", "KeyedAppStatesCallback status: TRANSACTION_TOO_LARGE_ERROR ")
            STATUS_EXCEEDED_BUFFER_ERROR ->
                Log_OC.i("ErrorReporter", "KeyedAppStatesCallback status: EXCEEDED_BUFFER_ERROR ")
            else ->
                Log_OC.i("ErrorReporter", "KeyedAppStatesCallback status: $state ")
        }
    }
}
