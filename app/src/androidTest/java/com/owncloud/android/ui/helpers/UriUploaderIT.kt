/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.helpers

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import androidx.test.core.app.launchActivity
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class UriUploaderIT : AbstractIT() {

    companion object {
        private const val PRIVATE_FILE_NAME = "uri_uploader_it_private.txt"
        private const val PRIVATE_FILE_CONTENT = "private"
        private const val ENCODED_DOT = "%2E"
    }

    @Test
    fun testSharedPreferencesFileIsRejected() = withActivity { activity ->
        assertRejected(activity, fileUri("/data/data/${activity.packageName}/shared_prefs/preferences.xml"))
    }

    @Test
    fun testDatabaseInDataDirIsRejected() = withActivity { activity ->
        assertRejected(activity, Uri.fromFile(File(activity.dataDir, "databases/nextcloud")))
    }

    @Test
    fun testDefaultSyncFolderInInternalFilesDirIsRejected() = withActivity { activity ->
        assertRejected(activity, Uri.fromFile(File(activity.filesDir, "nextcloud/admin/welcome.txt")))
    }

    @Test
    fun testDeviceProtectedStorageIsRejected() = withActivity { activity ->
        val deviceProtectedDataDir = activity.createDeviceProtectedStorageContext().dataDir

        assertRejected(activity, Uri.fromFile(File(deviceProtectedDataDir, "shared_prefs/preferences.xml")))
    }

    @Test
    fun testSyncFolderInExternalMediaDirIsRejected() = withActivity { activity ->
        val mediaDir = activity.externalMediaDirs.first()

        assertRejected(activity, Uri.fromFile(File(mediaDir, "nextcloud/test/welcome.txt")))
    }

    @Test
    fun testExternalFilesDirIsRejected() = withActivity { activity ->
        assertRejected(activity, Uri.fromFile(File(activity.getExternalFilesDir(null), "nextcloud/welcome.txt")))
    }

    @Test
    fun testExternalCacheDirIsRejected() = withActivity { activity ->
        assertRejected(activity, Uri.fromFile(File(activity.externalCacheDir, "thumbnail.png")))
    }

    @Test
    fun testSdcardAliasOfExternalFilesDirIsRejected() = withActivity { activity ->
        assertRejected(activity, fileUri("/sdcard/Android/data/${activity.packageName}/files/welcome.txt"))
    }

    @Test
    fun testPercentEncodedPackageNameIsRejected() = withActivity { activity ->
        val encodedPackageName = activity.packageName.replace(".", ENCODED_DOT)

        assertRejected(activity, fileUri("/data/data/$encodedPackageName/shared_prefs/preferences.xml"))
    }

    @Test
    fun testParentDirectoryTraversalIntoDataDirIsRejected() = withActivity { activity ->
        assertRejected(activity, fileUri("${activity.cacheDir.absolutePath}/../shared_prefs/preferences.xml"))
    }

    @Test
    fun testProcSelfRootAliasOfDataDirIsRejected() = withActivity { activity ->
        assertRejected(activity, fileUri("/proc/self/root/data/data/${activity.packageName}/shared_prefs/x.xml"))
    }

    @Test
    fun testProcSelfFdPointingToPrivateFileIsRejected() = withActivity { activity ->
        val privateFile = File(activity.filesDir, PRIVATE_FILE_NAME).apply { writeText(PRIVATE_FILE_CONTENT) }
        try {
            ParcelFileDescriptor.open(privateFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                assertRejected(activity, fileUri("/proc/self/fd/${descriptor.fd}"))
            }
        } finally {
            privateFile.delete()
        }
    }

    @Test
    fun testOwnFileProviderUriIsRejected() = withActivity { activity ->
        assertRejected(activity, contentUri(activity.getString(R.string.file_provider_authority), "/log/log.txt"))
    }

    @Test
    fun testOwnContentProviderUriIsRejected() = withActivity { activity ->
        assertRejected(activity, contentUri(activity.getString(R.string.authority), "/file/1"))
    }

    @Test
    fun testOwnImageCacheProviderUriIsRejected() = withActivity { activity ->
        assertRejected(activity, contentUri(activity.getString(R.string.image_cache_provider_authority), "/1"))
    }

    @Test
    fun testContentUriWithoutAuthorityIsRejected() = withActivity { activity ->
        val uri = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).path("/log/log.txt").build()

        assertRejected(activity, uri)
    }

    @Test
    fun testBatchWithASinglePrivateUriIsRejectedEntirely() = withActivity { activity ->
        val publicUri = fileUri("/storage/emulated/0/Download/photo.jpg")
        val privateUri = Uri.fromFile(File(activity.dataDir, "shared_prefs/preferences.xml"))

        assertRejected(activity, publicUri, privateUri)
    }

    @Test
    fun testNullAndNonUriEntriesDoNotHideAPrivateUri() = withActivity { activity ->
        val privateUri = Uri.fromFile(File(activity.dataDir, "shared_prefs/preferences.xml"))

        assertRejected(activity, null, Bundle(), privateUri)
    }

    @Test
    fun testSchemeLessPrivatePathIsNeverUploaded() = withActivity { activity ->
        val uri = Uri.parse("/data/data/${activity.packageName}/shared_prefs/preferences.xml")
        val result = createUploader(activity, uri).uploadUris()

        assertEquals(UriUploader.UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD, result)
    }

    @Test
    fun testPublicDownloadFileIsAllowed() = withActivity { activity ->
        assertAllowed(activity, fileUri("/storage/emulated/0/Download/photo.jpg"))
    }

    @Test
    fun testFileNameContainingPackageNameIsAllowed() = withActivity { activity ->
        assertAllowed(activity, fileUri("/storage/emulated/0/Download/${activity.packageName}-backup.zip"))
    }

    @Test
    fun testFolderSharingPackageNamePrefixIsAllowed() = withActivity { activity ->
        val mediaRoot = activity.externalMediaDirs.first().parentFile

        assertAllowed(activity, Uri.fromFile(File(mediaRoot, "${activity.packageName}2/photo.jpg")))
    }

    @Test
    fun testMediaStoreUriIsAllowed() = withActivity { activity ->
        assertAllowed(activity, Uri.parse("content://media/external/images/media/1"))
    }

    @Test
    fun testExternalStorageDocumentsUriIsAllowed() = withActivity { activity ->
        assertAllowed(
            activity,
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2Fphoto.jpg")
        )
    }

    @Test
    fun testOwnDocumentsProviderUriIsAllowed() = withActivity { activity ->
        assertAllowed(activity, contentUri(activity.getString(R.string.document_provider_authority), "/document/1"))
    }

    @Test
    fun testUriOfUnknownProviderIsAllowed() = withActivity { activity ->
        assertAllowed(activity, contentUri("com.example.unknown.provider", "/photo.jpg"))
    }

    @Test
    fun testUriWithOtherSchemeIsNotTreatedAsSensitive() = withActivity { activity ->
        assertAllowed(activity, Uri.parse("https://example.com/photo.jpg"))
    }

    private fun withActivity(block: (TestActivity) -> Unit) {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity(block)
        }
    }

    private fun fileUri(path: String): Uri = Uri.parse("${ContentResolver.SCHEME_FILE}://$path")

    private fun contentUri(authority: String, path: String): Uri = Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(authority)
        .path(path)
        .build()

    private fun createUploader(activity: TestActivity, vararg uris: Parcelable?): UriUploader = UriUploader(
        activity,
        uris.toList(),
        "",
        activity.user.orElseThrow(::RuntimeException),
        FileUploadWorker.LOCAL_BEHAVIOUR_MOVE,
        false,
        null
    )

    private fun assertRejected(activity: TestActivity, vararg uris: Parcelable?) {
        val result = createUploader(activity, *uris).uploadUris()

        assertEquals(UriUploader.UriUploaderResultCode.ERROR_SENSITIVE_PATH, result)
    }

    private fun assertAllowed(activity: TestActivity, uri: Uri) {
        assertFalse(createUploader(activity, uri).isSensitiveUri(uri))
    }
}
