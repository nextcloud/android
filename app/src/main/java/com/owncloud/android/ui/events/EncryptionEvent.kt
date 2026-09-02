/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.events

import androidx.lifecycle.lifecycleScope
import com.nextcloud.utils.e2ee.model.E2EEAction
import com.nextcloud.utils.e2ee.model.E2EEKeyCheck
import com.nextcloud.utils.extensions.showEncryptionDialog
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.launch

/**
 * Event for set folder as encrypted/decrypted
 */
class EncryptionEvent(val localId: Long, val remoteId: String, val remotePath: String, val shouldBeEncrypted: Boolean) {
    fun onResult(fragment: OCFileListFragment) {
        fragment.lifecycleScope.launch {
            when (val state = fragment.e2eeActionResolver.checkKeys()) {
                E2EEKeyCheck.NO_NETWORK, E2EEKeyCheck.E2EE_UNAVAILABLE, E2EEKeyCheck.CHECK_FAILED -> {
                    state.getMessageId(E2EEAction.ENCRYPT)?.let { DisplayUtils.showSnackMessage(fragment, it) }
                }

                E2EEKeyCheck.ONLY_ON_SERVER, E2EEKeyCheck.MISSING_EVERYWHERE -> {
                    fragment.showEncryptionDialog(remotePath)
                }

                E2EEKeyCheck.ONLY_ON_DEVICE, E2EEKeyCheck.DIFFERS_FROM_SERVER -> {
                    state.getDialog(E2EEAction.ENCRYPT)?.let { fragment.e2eeDialogPresenter.show(it) }
                }

                E2EEKeyCheck.SAME_AS_SERVER -> {
                    fragment.folderEncryption.toggle(this@EncryptionEvent)
                }
            }
        }
    }
}
