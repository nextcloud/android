/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.ui_components

import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ionos.scanbot.R
import com.ionos.scanbot.util.context.isActivityFinishing
import kotlin.getValue

class NoFreeSpaceMessageDialogWrapper(context: Context) {

    private val dialog by lazy {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.scanbot_no_enough_free_device_space_title)
            .setMessage(R.string.scanbot_no_enough_free_device_space_message)
            .setPositiveButton(R.string.scanbot_ok_btn_title) { dialog, _ -> dialog.dismiss() }
            .create()
    }

    fun show(context: Context) {
        if (!context.isActivityFinishing()) {
            dialog.show()
        }
    }
}
