/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.ui_components

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ionos.scanbot.R
import com.ionos.scanbot.databinding.ScanbotImportProgressDialogBinding
import com.ionos.scanbot.util.context.getActivity
import java.text.NumberFormat

internal class ImportProgressDialog(
	private val context: Context,
	private val onCancelClick: () -> Unit,
) {
    private val dialog by lazy { createDialog(context) }
    private val viewBinding by lazy { ScanbotImportProgressDialogBinding.inflate(LayoutInflater.from(context)) }

    private val progressPercentFormat by lazy {
        NumberFormat.getPercentInstance().apply {
            setMaximumFractionDigits(0)
        }
    }

	fun show(progress: Int) {
        updateProgress(progress)
        if (!dialog.isShowing) {
            val activity = context.getActivity()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                dialog.show()
            }
        }
	}

	fun dismiss() {
        if (dialog.isShowing) {
            val activity = context.getActivity()
            if (activity != null && !activity.isFinishing && !activity.isDestroyed) {
                dialog.dismiss()
            }
        }
	}

    private fun createDialog(context: Context): AlertDialog {
        return MaterialAlertDialogBuilder(context)
            .setTitle(R.string.scanbot_processing_title)
            .setView(viewBinding.root)
            .setNegativeButton(R.string.scanbot_cancel_btn_title) { it, _ ->
                it.dismiss()
                onCancelClick()
            }
            .setCancelable(false)
            .create()
    }

    @SuppressLint("SetTextI18n")
    private fun updateProgress(progress: Int) {
        val maxProgress = viewBinding.progressBar.max
        val percent = progress.toDouble() / maxProgress.toDouble()
        viewBinding.progressBar.progress = progress
        viewBinding.progressPercentTextView.text = progressPercentFormat.format(percent)
        viewBinding.progressNumberTextView.text = "$progress/$maxProgress"
    }
}
