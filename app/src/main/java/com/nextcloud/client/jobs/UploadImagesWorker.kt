/*
 * Nextcloud Android client application
 *
 * @author TSI-mc
 * Copyright (C) 2022 TSI-mc
 * Copyright (C) 2022 NextCloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.jobs

import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.owncloud.android.R
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.utils.FileUtil
import com.owncloud.android.utils.theme.ThemeColorUtils
import java.io.File
import java.security.SecureRandom

/**
 * worker to upload the files in background when app is not running or app is killed
 * right now we are using it for to save rotated images
 */
class UploadImagesWorker constructor(
    private val context: Context,
    params: WorkerParameters,
    private val notificationManager: NotificationManager,
    private val accountManager: UserAccountManager,
    private val themeColorUtils: ThemeColorUtils,
) : Worker(context, params) {

    private val savedFiles = mutableListOf<String>()
    private val remotePaths = mutableListOf<String>()

    companion object {
        const val TAG = "UploadImagesWorkerJob"
        const val IMAGE_COMPRESSION_PERCENTAGE = 100
    }

    override fun doWork(): Result {

        val bitmapHashMap: HashMap<Int, PreviewImageFragment.LoadImage> = HashMap(PreviewImageActivity.bitmapHashMap)

        // clear the static bitmap once the images are stored in work manager instance
        PreviewImageActivity.bitmapHashMap.clear()

        val randomId = SecureRandom()
        val pushNotificationId = randomId.nextInt()
        showNotification(pushNotificationId)

        for ((_, value) in bitmapHashMap) {
            val fileName = value.ocFile.fileName
            // get the file extension
            val extension: String = fileName.substring(fileName.lastIndexOf("."))
            // get the file name without extension
            val fileNameWithoutExt: String = fileName.replace(extension, "")

            // if extension is jpg then save the image as jpg
            if (extension == ".jpg" || extension == ".jpeg") {
                val jpgFile =
                    FileUtil.saveJpgImage(context, value.bitmap, fileNameWithoutExt, IMAGE_COMPRESSION_PERCENTAGE)

                // if file is available on local then rewrite the file as well
                if (value.ocFile.isDown) {
                    FileUtil.saveJpgImage(
                        context,
                        value.bitmap,
                        File(value.ocFile.storagePath),
                        IMAGE_COMPRESSION_PERCENTAGE
                    )
                }
                onImageSaveSuccess(value, jpgFile)

                // if extension is png then save the image as png
            } else if (extension == ".png") {
                val pngFile =
                    FileUtil.savePngImage(context, value.bitmap, fileNameWithoutExt, IMAGE_COMPRESSION_PERCENTAGE)

                // if file is available on local then rewrite the file as well
                if (value.ocFile.isDown) {
                    FileUtil.savePngImage(
                        context,
                        value.bitmap,
                        File(value.ocFile.storagePath),
                        IMAGE_COMPRESSION_PERCENTAGE
                    )
                }
                onImageSaveSuccess(value, pngFile)
            }
        }

        notificationManager.cancel(pushNotificationId)

        // upload image files
        if (!savedFiles.isNullOrEmpty() && !remotePaths.isNullOrEmpty()) {
            uploadImageFiles()
        }

        return Result.success()
    }

    private fun onImageSaveSuccess(
        value: PreviewImageFragment.LoadImage,
        imageFile: File
    ) {
        savedFiles.add(imageFile.path)
        remotePaths.add(value.ocFile.remotePath)
    }

    private fun showNotification(pushNotificationId: Int) {
        val notificationBuilder =
            NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_UPLOAD)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
                .setColor(themeColorUtils.primaryColor(context, true))
                .setContentTitle(context.resources.getString(R.string.app_name))
                .setContentText(context.resources.getString(R.string.foreground_service_save))
                .setAutoCancel(false)

        notificationManager.notify(pushNotificationId, notificationBuilder.build())
    }

    private fun uploadImageFiles() {
        FileUploader.uploadNewFile(
            context,
            accountManager.user,
            savedFiles.toTypedArray(),
            remotePaths.toTypedArray(),
            null, // MIME type will be detected from file name
            FileUploader.LOCAL_BEHAVIOUR_DELETE,
            false, // do not create parent folder if not existent
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.OVERWRITE, // overwrite the images
            true
        )
    }
}
