/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license

import android.content.Context
import androidx.work.WorkerParameters
import com.ionos.scanbot.di.qualifiers.ScanbotLicenseKeyUrl
import com.ionos.scanbot.initializer.TryToInitScanbotSdk
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject
import javax.inject.Provider

class ScanbotLicenseJobFactory @Inject constructor(
    @ScanbotLicenseKeyUrl private val scanbotLicenseUrl: String,
    private val accountManager: UserAccountManager,
    private val licenseKeyStore: LicenseKeyStore,
    private val tryToInitScanbotSdk: TryToInitScanbotSdk,
    private val viewThemeUtils: Provider<ViewThemeUtils>,
) {

    fun create(context: Context, params: WorkerParameters): ScanbotLicenseDownloadWorker {
        return ScanbotLicenseDownloadWorker(
            scanbotLicenseUrl,
            viewThemeUtils.get(),
            accountManager,
            licenseKeyStore,
            tryToInitScanbotSdk,
            context,
            params
        )
    }

}