/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.helpers

import android.net.Uri
import androidx.test.core.app.launchActivity
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.lib.common.utils.Log_OC
import org.junit.Assert
import org.junit.Test

class UriUploaderIT : AbstractIT() {

    private val tag = "UriUploaderIT"

    @Test
    fun testUploadPrivatePathSharedPreferences() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val packageName = activity.packageName
                val path = "file:///data/data/$packageName/shared_prefs/com.nextcloud.client_preferences.xml"
                testPrivatePath(activity, path)
            }
        }
    }

    @Test
    fun testUploadPrivatePathUserFile() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val packageName = activity.packageName
                val path = "file:///storage/emulated/0/Android/media/$packageName/nextcloud/test/welcome.txt"
                testPrivatePath(activity, path)
            }
        }
    }

    private fun testPrivatePath(activity: TestActivity, path: String) {
        val sut = UriUploader(
            activity,
            listOf(Uri.parse(path)),
            "",
            activity.user.orElseThrow(::RuntimeException),
            FileUploadWorker.LOCAL_BEHAVIOUR_MOVE,
            false,
            null
        )
        val uploadResult = sut.uploadUris()

        Log_OC.d(tag, "Upload Result: ${uploadResult.name}")

        Assert.assertEquals(
            "Wrong result code",
            UriUploader.UriUploaderResultCode.ERROR_SENSITIVE_PATH,
            uploadResult
        )
    }
}
