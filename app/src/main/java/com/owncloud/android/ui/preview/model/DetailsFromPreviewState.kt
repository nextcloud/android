/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.preview.model

import android.content.Intent
import com.owncloud.android.datamodel.OCFile

sealed interface DetailsFromPreviewState {
    val fileBeforeDetails: OCFile?

    data class ShowingDetails(val detailsIntent: Intent, override val fileBeforeDetails: OCFile?) :
        DetailsFromPreviewState

    data class ReturnedToPreview(override val fileBeforeDetails: OCFile?) : DetailsFromPreviewState
}
