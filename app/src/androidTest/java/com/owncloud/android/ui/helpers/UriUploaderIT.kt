package com.owncloud.android.ui.helpers

import android.net.Uri
import androidx.test.core.app.launchActivity
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.files.services.FileUploader
import org.junit.Assert
import org.junit.Test

class UriUploaderIT : AbstractIT() {

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
            FileUploader.LOCAL_BEHAVIOUR_MOVE,
            false,
            null
        )
        val uploadResult = sut.uploadUris()
        Assert.assertEquals(
            "Wrong result code",
            UriUploader.UriUploaderResultCode.ERROR_SENSITIVE_PATH,
            uploadResult
        )
    }
}
