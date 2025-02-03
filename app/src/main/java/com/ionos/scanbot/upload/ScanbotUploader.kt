/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.upload

import com.ionos.scanbot.upload.use_case.Uploader
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.operations.UploadFileOperation
import java.io.File
import javax.inject.Inject

class ScanbotUploader @Inject constructor(
    private val currentAccountProvider: CurrentAccountProvider
): Uploader {
    override fun upload(uploadFolder: String, pageList: List<String>) {
        val uploadPaths = pageList.map {
            File(uploadFolder, File(it).name).path
        }.toTypedArray()

        FileUploadHelper.instance().uploadNewFiles(
            currentAccountProvider.user,
            pageList.toTypedArray(),
            uploadPaths,
            FileUploadWorker.LOCAL_BEHAVIOUR_DELETE,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.RENAME
        )
    }
}