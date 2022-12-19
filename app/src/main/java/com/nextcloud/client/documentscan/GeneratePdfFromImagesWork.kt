/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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

package com.nextcloud.client.documentscan

import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.nextcloud.client.account.AnonymousUser
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.logger.Logger
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.operations.UploadFileOperation
import com.owncloud.android.ui.notifications.NotificationUtils
import com.owncloud.android.utils.MimeType
import com.owncloud.android.utils.theme.ViewThemeUtils
import java.io.File
import java.security.SecureRandom

@Suppress("Detekt.LongParameterList") // constructed only from factory method and tests
class GeneratePdfFromImagesWork(
    private val appContext: Context,
    private val generatePdfUseCase: GeneratePDFUseCase,
    private val viewThemeUtils: ViewThemeUtils,
    private val notificationManager: NotificationManager,
    private val userAccountManager: UserAccountManager,
    private val logger: Logger,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        val inputPaths = inputData.getStringArray(INPUT_IMAGE_FILE_PATHS)?.toList()
        val outputFilePath = inputData.getString(INPUT_OUTPUT_FILE_PATH)
        val uploadFolder = inputData.getString(INPUT_UPLOAD_FOLDER)
        val accountName = inputData.getString(INPUT_UPLOAD_ACCOUNT)

        @Suppress("Detekt.ComplexCondition") // not that complex
        require(!inputPaths.isNullOrEmpty() && outputFilePath != null && uploadFolder != null && accountName != null) {
            "PDF generation work started with missing parameters:" +
                " inputPaths: $inputPaths, outputFilePath: $outputFilePath," +
                " uploadFolder: $uploadFolder, accountName: $accountName"
        }

        val user = userAccountManager.getUser(accountName)
        require(user.isPresent && user.get() !is AnonymousUser) { "Invalid or not found user" }

        logger.d(
            TAG,
            "PDF generation work started with parameters: inputPaths=$inputPaths," +
                "outputFilePath=$outputFilePath, uploadFolder=$uploadFolder, accountName=$accountName"
        )

        val notificationId = showNotification(R.string.document_scan_pdf_generation_in_progress)
        val result = generatePdfUseCase.execute(inputPaths, outputFilePath)
        notificationManager.cancel(notificationId)
        if (result) {
            uploadFile(user.get(), uploadFolder, outputFilePath)
            cleanupImages(inputPaths)
        } else {
            logger.w(TAG, "PDF generation failed")
            showNotification(R.string.document_scan_pdf_generation_failed)
            return Result.failure()
        }

        logger.d(TAG, "PDF generation work finished")
        return Result.success()
    }

    private fun cleanupImages(inputPaths: List<String>) {
        inputPaths.forEach {
            val deleted = File(it).delete()
            logger.d(TAG, "Deleted $it: success = $deleted")
        }
    }

    private fun showNotification(@StringRes messageRes: Int): Int {
        val notificationId = SecureRandom().nextInt()
        val message = appContext.getString(messageRes)

        val notificationBuilder = NotificationCompat.Builder(
            appContext,
            NotificationUtils.NOTIFICATION_CHANNEL_GENERAL
        )
            .setSmallIcon(R.drawable.notification_icon)
            .setLargeIcon(BitmapFactory.decodeResource(appContext.resources, R.drawable.notification_icon))
            .setContentText(message)
            .setAutoCancel(true)

        viewThemeUtils.androidx.themeNotificationCompatBuilder(appContext, notificationBuilder)

        notificationManager.notify(notificationId, notificationBuilder.build())

        return notificationId
    }

    private fun uploadFile(user: User, uploadFolder: String, pdfPath: String) {
        val uploadPath = uploadFolder + OCFile.PATH_SEPARATOR + File(pdfPath).name

        FileUploader.uploadNewFile(
            appContext,
            user,
            pdfPath,
            uploadPath,
            FileUploader.LOCAL_BEHAVIOUR_DELETE, // MIME type will be detected from file name
            MimeType.PDF,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.ASK_USER
        )
    }

    companion object {
        const val INPUT_IMAGE_FILE_PATHS = "input_image_file_paths"
        const val INPUT_OUTPUT_FILE_PATH = "input_output_file_path"
        const val INPUT_UPLOAD_FOLDER = "input_upload_folder"
        const val INPUT_UPLOAD_ACCOUNT = "input_upload_account"
        private const val TAG = "GeneratePdfFromImagesWo"
    }
}
