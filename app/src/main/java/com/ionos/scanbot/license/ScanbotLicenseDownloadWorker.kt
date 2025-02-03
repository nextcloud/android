/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ionos.scanbot.initializer.TryToInitScanbotSdk
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.download.DownloadNotificationManager
import com.owncloud.android.R
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.security.SecureRandom

class ScanbotLicenseDownloadWorker(
    licenseUrl: String,
    viewThemeUtils: ViewThemeUtils,
    private val accountManager: UserAccountManager,
    private val licenseKeyStore: LicenseKeyStore,
    private val tryToInitScanbotSdk: TryToInitScanbotSdk,
    private val context: Context,
    params: WorkerParameters,
): Worker(context, params){

    companion object {
        const val SCANBOT_LICENSE_DOWNLOAD_WORKER = "SCANBOT_LICENSE_DOWNLOAD_WORKER"
    }

    private val operation = DownloadLicenseRemoteOperation(licenseUrl)
    private var notificationManager = DownloadNotificationManager(
        SecureRandom().nextInt(),
        context,
        viewThemeUtils
    )

    override fun doWork(): Result {
        return try {
            val ocAccount = accountManager.user.toOwnCloudAccount()
            val downloadClient =
                OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context)

            val result = operation.execute(downloadClient)

            if (result.isSuccess) {
                operation.license?.let {
                    licenseKeyStore.saveLicenseKey(it)
                    tryToInitScanbotSdk(it)
                }
                Result.success()
            }
            else Result.retry()
        }catch (e: Exception){
            Result.failure()
        }
    }

    override fun getForegroundInfo(): ForegroundInfo {
        notificationManager.notificationBuilder.run {
            setProgress(100, 0, true)
            setContentTitle(context.getString(R.string.downloader_download_in_progress_ticker))
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            ForegroundInfo(
                notificationManager.getId(),
                notificationManager.getNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else{
            ForegroundInfo(
                notificationManager.getId(),
                notificationManager.getNotification(),
            )
        }
    }

}