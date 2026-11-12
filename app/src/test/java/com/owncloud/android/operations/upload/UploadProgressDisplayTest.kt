/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.operations.upload

import android.widget.ProgressBar
import com.owncloud.android.ui.adapter.progressListener.UploadProgressListener
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class UploadProgressDisplayTest {
    @Test
    fun firstTransferReplacesPreparationSpinnerWithActualByteProgress() {
        val bar: ProgressBar = mock()
        whenever(bar.post(any())).thenAnswer {
            it.getArgument<Runnable>(0).run()
            true
        }

        UploadProgressListener(null, bar).onTransferProgress(1, 75, 100, "photo.jpg")

        verify(bar).isIndeterminate = false
        verify(bar).progress = 75
    }
}
