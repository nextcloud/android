package com.nmc.android.jobs

import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.UserAccountManager
import com.nmc.android.utils.FileUtils
import com.owncloud.android.R
import com.owncloud.android.datamodel.ThumbnailsCacheManager
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
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
    private val accountManager: UserAccountManager
) : Worker(context, params) {

    private val savedFiles = mutableListOf<String>()
    private val remotePaths = mutableListOf<String>()

    companion object {
        const val TAG = "UploadImagesWorkerJob"
        const val IMAGE_COMPRESSION_PERCENTAGE = 100
    }

    override fun doWork(): Result {

        val bitmapHashMap: HashMap<Int, PreviewImageFragment.LoadImage> = PreviewImageActivity.bitmapHashMap

        val randomId = SecureRandom()
        val pushNotificationId = randomId.nextInt()
        showNotification(pushNotificationId)

        for ((_, value) in bitmapHashMap) {
            val fileName = value.ocFile.fileName
            //get the file extension
            val extension: String = fileName.substring(fileName.lastIndexOf("."))
            //get the file name without extension
            val fileNameWithoutExt: String = fileName.replace(extension, "")

            //if extension is jpg then save the image as jpg
            if (extension == ".jpg") {
                val jpgFile = FileUtils.saveJpgImage(context, value.bitmap, fileNameWithoutExt, IMAGE_COMPRESSION_PERCENTAGE)

                //if file is available on local then rewrite the file as well
                if (value.ocFile.isDown) {
                  FileUtils.saveJpgImage(context, value.bitmap, File(value.ocFile.storagePath), IMAGE_COMPRESSION_PERCENTAGE)
                }
                onImageSaveSuccess(value, jpgFile)

                //if extension is png then save the image as png
            } else if (extension == ".png") {
                val pngFile = FileUtils.savePngImage(context, value.bitmap, fileNameWithoutExt, IMAGE_COMPRESSION_PERCENTAGE)

                //if file is available on local then rewrite the file as well
                if (value.ocFile.isDown) {
                    FileUtils.savePngImage(context, value.bitmap, File(value.ocFile.storagePath), IMAGE_COMPRESSION_PERCENTAGE)
                }
                onImageSaveSuccess(value, pngFile)
            }

            //remove the cache for the existing image
            ThumbnailsCacheManager.removeBitmapFromCache(ThumbnailsCacheManager.PREFIX_THUMBNAIL + value.ocFile.remoteId)
        }

        notificationManager.cancel(pushNotificationId)

        //upload image files
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
            NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_SCAN_DOC_SAVE)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.notification_icon))
                .setColor(ThemeColorUtils.primaryColor(context, true))
                .setContentTitle(context.resources.getString(R.string.app_name))
                .setContentText(context.resources.getString(R.string.foreground_service_save))
                .setAutoCancel(false)

        notificationManager.notify(pushNotificationId, notificationBuilder.build())
    }

    private fun uploadImageFiles() {
        FileUploader.uploadNewFile(
            context,
            accountManager.currentAccount,
            savedFiles.toTypedArray(),
            remotePaths.toTypedArray(),
            null,  // MIME type will be detected from file name
            FileUploader.LOCAL_BEHAVIOUR_DELETE,
            false,  // do not create parent folder if not existent
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            FileUploader.NameCollisionPolicy.OVERWRITE //overwrite the images
        )
    }
}
