/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.e2ee

import com.nextcloud.utils.e2ee.model.E2EEDialog
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment.ConfirmationDialogFragmentListener
import com.owncloud.android.ui.fragment.OCFileListFragment

class E2EEDialogPresenter(private val fragment: OCFileListFragment) {

    companion object {
        private const val TAG = "E2EEDialogPresenter"
        private const val ENCRYPTION_KEY_ALERT_DIALOG_TAG = "ENCRYPTION_KEY_HANDLER_DIALOG"
        private const val NO_BUTTON = -1
    }

    fun show(dialog: E2EEDialog) {
        if (fragment.parentFragmentManager.findFragmentByTag(ENCRYPTION_KEY_ALERT_DIALOG_TAG) != null) {
            return
        }

        ConfirmationDialogFragment
            .newInstance(
                titleResId = dialog.titleId,
                titleIconId = R.drawable.ic_lock_open_white,
                messageResId = dialog.descriptionId,
                positiveButtonTextId = R.string.common_ok,
                neutralButtonTextId = NO_BUTTON,
                negativeButtonTextId = NO_BUTTON,
                messageArguments = null
            ).apply {
                setOnConfirmationListener(dismissListener(dialog))
            }.show(fragment.parentFragmentManager, ENCRYPTION_KEY_ALERT_DIALOG_TAG)
    }

    private fun dismissListener(dialog: E2EEDialog): ConfirmationDialogFragmentListener =
        object : ConfirmationDialogFragmentListener {
            override fun onConfirmation(callerTag: String?) = Log_OC.d(TAG, "$dialog acknowledged")
            override fun onNeutral(callerTag: String?) = Unit
            override fun onCancel(callerTag: String?) = Unit
        }
}
