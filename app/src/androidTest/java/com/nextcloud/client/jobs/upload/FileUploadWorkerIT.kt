/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Raphael Vieira raphaelecv.projects@gmail.com
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.jobs.upload

import androidx.work.WorkManager
import com.owncloud.android.AbstractOnServerIT
import com.owncloud.android.files.services.NameCollisionPolicy
import com.owncloud.android.lib.resources.files.ReadFileRemoteOperation
import com.owncloud.android.operations.UploadFileOperation
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FileUploadWorkerIT : AbstractOnServerIT() {

    private lateinit var workManager: WorkManager
    private lateinit var fileUploadHelper: FileUploadHelper

    @Before
    fun setUp() {
        workManager = WorkManager.getInstance(targetContext)
        fileUploadHelper = FileUploadHelper.instance()
    }

    @Test
    fun multipleFilesUploadBatch() {
        val file1 = getDummyFile("empty.txt")
        val file2 = getDummyFile("nonEmpty.txt")
        val remotePath1 = "/batch_upload_1_${System.currentTimeMillis()}.txt"
        val remotePath2 = "/batch_upload_2_${System.currentTimeMillis()}.txt"

        fileUploadHelper.uploadNewFiles(
            user,
            arrayOf(file1.absolutePath, file2.absolutePath),
            arrayOf(remotePath1, remotePath2),
            FileUploadWorker.LOCAL_BEHAVIOUR_COPY,
            true,
            UploadFileOperation.CREATED_BY_USER,
            false,
            false,
            NameCollisionPolicy.DEFAULT
        )

        // waiting for the upload jobs to finish
        val tag = "files_upload" + user.accountName
        var isFinished = false
        val startTime = System.currentTimeMillis()
        val timeout = 60000L

        while (!isFinished && System.currentTimeMillis() - startTime < timeout) {
            val workInfos = workManager.getWorkInfosByTag(tag).get()
            if (workInfos.isNotEmpty() && workInfos.all { it.state.isFinished }) {
                isFinished = true
            } else {
                Thread.sleep(1000)
            }
        }

        assertTrue("Batch upload jobs did not finish within timeout", isFinished)

        // Verifying both files are uploaded to the server
        val result1 = ReadFileRemoteOperation(remotePath1).execute(client)
        assertTrue("File 1 should be on server", result1.isSuccess)

        val result2 = ReadFileRemoteOperation(remotePath2).execute(client)
        assertTrue("File 2 should be on server", result2.isSuccess)
    }
}
