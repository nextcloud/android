/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2023 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.documentscan

import androidx.activity.result.contract.ActivityResultContract

abstract class AppScanOptionalFeature {
    /**
     * Check [isAvailable] before calling this method.
     */
    abstract fun getScanContract(): ActivityResultContract<Unit, String?>
    open val isAvailable: Boolean = true

    /**
     * Use this in variants where the feature is not available
     */
    @Suppress("unused") // used only in some variants
    object Stub : AppScanOptionalFeature() {
        override fun getScanContract(): ActivityResultContract<Unit, String?> =
            throw UnsupportedOperationException("Document scan is not available")

        override val isAvailable = false
    }
}
