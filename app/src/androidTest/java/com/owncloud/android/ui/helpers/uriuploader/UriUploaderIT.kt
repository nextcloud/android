/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.helpers.uriuploader

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import androidx.core.net.toUri
import androidx.test.core.app.launchActivity
import com.nextcloud.client.jobs.upload.FileUploadWorker
import com.nextcloud.test.TestActivity
import com.owncloud.android.AbstractIT
import com.owncloud.android.R
import com.owncloud.android.ui.helpers.uriuploader.model.UriUploaderResultCode
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
        // Represents "file:///data/data/com.nextcloud.client/shared_prefs/preferences.xml"
        val sharedPreferencesUri = fileUri("/data/data/${activity.packageName}/shared_prefs/preferences.xml")

        assertRejected(activity, sharedPreferencesUri)
    }

    @Test
    fun testDatabaseInDataDirIsRejected() = withActivity { activity ->
        // Represents "file:///data/user/0/com.nextcloud.client/databases/nextcloud"
        val databaseUri = Uri.fromFile(File(activity.dataDir, "databases/nextcloud"))

        assertRejected(activity, databaseUri)
    }

    @Test
    fun testDefaultSyncFolderInInternalFilesDirIsRejected() = withActivity { activity ->
        // Represents "file:///data/user/0/com.nextcloud.client/files/nextcloud/admin/welcome.txt"
        val syncedFileUri = Uri.fromFile(File(activity.filesDir, "nextcloud/admin/welcome.txt"))

        assertRejected(activity, syncedFileUri)
    }

    @Test
    fun testDeviceProtectedStorageIsRejected() = withActivity { activity ->
        val deviceProtectedDataDir = activity.createDeviceProtectedStorageContext().dataDir

        // Represents "file:///data/user_de/0/com.nextcloud.client/shared_prefs/preferences.xml"
        val deviceProtectedUri = Uri.fromFile(File(deviceProtectedDataDir, "shared_prefs/preferences.xml"))

        assertRejected(activity, deviceProtectedUri)
    }

    @Test
    fun testSyncFolderInExternalMediaDirIsRejected() = withActivity { activity ->
        val mediaDir = activity.externalMediaDirs.first()

        // Represents "file:///storage/emulated/0/Android/media/com.nextcloud.client/nextcloud/test/welcome.txt"
        val syncedMediaFileUri = Uri.fromFile(File(mediaDir, "nextcloud/test/welcome.txt"))

        assertRejected(activity, syncedMediaFileUri)
    }

    @Test
    fun testExternalFilesDirIsRejected() = withActivity { activity ->
        // Represents "file:///storage/emulated/0/Android/data/com.nextcloud.client/files/nextcloud/welcome.txt"
        val externalFileUri = Uri.fromFile(File(activity.getExternalFilesDir(null), "nextcloud/welcome.txt"))

        assertRejected(activity, externalFileUri)
    }

    @Test
    fun testExternalCacheDirIsRejected() = withActivity { activity ->
        // Represents "file:///storage/emulated/0/Android/data/com.nextcloud.client/cache/thumbnail.png"
        val externalCacheUri = Uri.fromFile(File(activity.externalCacheDir, "thumbnail.png"))

        assertRejected(activity, externalCacheUri)
    }

    @Test
    fun testSdcardAliasOfExternalFilesDirIsRejected() = withActivity { activity ->
        // Represents "file:///sdcard/Android/data/com.nextcloud.client/files/welcome.txt"
        val sdcardAliasUri = fileUri("/sdcard/Android/data/${activity.packageName}/files/welcome.txt")

        assertRejected(activity, sdcardAliasUri)
    }

    @Test
    fun testPercentEncodedPackageNameIsRejected() = withActivity { activity ->
        val encodedPackageName = activity.packageName.replace(".", ENCODED_DOT)

        // Represents "file:///data/data/com%2Enextcloud%2Eclient/shared_prefs/preferences.xml"
        val encodedPackageUri = fileUri("/data/data/$encodedPackageName/shared_prefs/preferences.xml")

        assertRejected(activity, encodedPackageUri)
    }

    @Test
    fun testParentDirectoryTraversalIntoDataDirIsRejected() = withActivity { activity ->
        // Represents "file:///data/user/0/com.nextcloud.client/cache/../shared_prefs/preferences.xml"
        val traversalUri = fileUri("${activity.cacheDir.absolutePath}/../shared_prefs/preferences.xml")

        assertRejected(activity, traversalUri)
    }

    @Test
    fun testProcSelfRootAliasOfDataDirIsRejected() = withActivity { activity ->
        // Represents "file:///proc/self/root/data/data/com.nextcloud.client/shared_prefs/x.xml"
        val procRootUri = fileUri("/proc/self/root/data/data/${activity.packageName}/shared_prefs/x.xml")

        assertRejected(activity, procRootUri)
    }

    @Test
    fun testProcSelfFdPointingToPrivateFileIsRejected() = withActivity { activity ->
        val privateFile = File(activity.filesDir, PRIVATE_FILE_NAME).apply { writeText(PRIVATE_FILE_CONTENT) }
        try {
            ParcelFileDescriptor.open(privateFile, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                // Represents "file:///proc/self/fd/<N>"
                val openFileUri = fileUri("/proc/self/fd/${descriptor.fd}")

                assertRejected(activity, openFileUri)
            }
        } finally {
            privateFile.delete()
        }
    }

    @Test
    fun testShareFromOtherClient() = withActivity { activity ->
        val path = "content://com.example.android.webdav.files/attachments/logs.txt"
        assertAllowed(activity, path.toUri())
    }

    @Test
    fun testOwnFileProviderUriIsRejected() = withActivity { activity ->
        // Represents "content://org.nextcloud.files/log/log.txt"
        val fileProviderUri = contentUri(activity.getString(R.string.file_provider_authority), "/log/log.txt")

        assertAllowed(activity, fileProviderUri)
    }

    @Test
    fun testOwnContentProviderUriIsRejected() = withActivity { activity ->
        // Represents "content://org.nextcloud/file/1"
        val contentProviderUri = contentUri(activity.getString(R.string.authority), "/file/1")

        assertRejected(activity, contentProviderUri)
    }

    @Test
    fun testOwnImageCacheProviderUriIsRejected() = withActivity { activity ->
        // Represents "content://org.nextcloud.imageCache.provider/1"
        val imageCacheUri = contentUri(activity.getString(R.string.image_cache_provider_authority), "/1")

        assertRejected(activity, imageCacheUri)
    }

    @Test
    fun testContentUriWithoutAuthorityIsRejected() = withActivity { activity ->
        // Represents "content:/log/log.txt"
        val noAuthorityUri = Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).path("/log/log.txt").build()

        assertRejected(activity, noAuthorityUri)
    }

    @Test
    fun testBatchWithASinglePrivateUriIsRejectedEntirely() = withActivity { activity ->
        // Represents "file:///storage/emulated/0/Download/photo.jpg"
        val publicUri = fileUri("/storage/emulated/0/Download/photo.jpg")

        // Represents "file:///data/user/0/com.nextcloud.client/shared_prefs/preferences.xml"
        val privateUri = Uri.fromFile(File(activity.dataDir, "shared_prefs/preferences.xml"))

        assertRejected(activity, publicUri, privateUri)
    }

    @Test
    fun testNullAndNonUriEntriesDoNotHideAPrivateUri() = withActivity { activity ->
        // Represents "file:///data/user/0/com.nextcloud.client/shared_prefs/preferences.xml"
        val privateUri = Uri.fromFile(File(activity.dataDir, "shared_prefs/preferences.xml"))

        assertRejected(activity, null, Bundle(), privateUri)
    }

    @Test
    fun testSchemeLessPrivatePathIsNeverUploaded() = withActivity { activity ->
        // Represents "/data/data/com.nextcloud.client/shared_prefs/preferences.xml"
        val schemeLessUri = Uri.parse("/data/data/${activity.packageName}/shared_prefs/preferences.xml")
        val result = createUploader(activity, schemeLessUri).uploadUris()

        assertEquals(UriUploaderResultCode.ERROR_NO_FILE_TO_UPLOAD, result)
    }

    @Test
    fun testPublicDownloadFileIsAllowed() = withActivity { activity ->
        // Represents "file:///storage/emulated/0/Download/photo.jpg"
        val publicFileUri = fileUri("/storage/emulated/0/Download/photo.jpg")

        assertAllowed(activity, publicFileUri)
    }

    @Test
    fun testFileNameContainingPackageNameIsAllowed() = withActivity { activity ->
        // Represents "file:///storage/emulated/0/Download/com.nextcloud.client-backup.zip"
        val publicFileUri = fileUri("/storage/emulated/0/Download/${activity.packageName}-backup.zip")

        assertAllowed(activity, publicFileUri)
    }

    @Test
    fun testFileNamedAsPackageNameIsAllowed() = withActivity { activity ->
        // Represents "file:///storage/emulated/0/Download/com.nextcloud.client.txt"
        val publicFileUri = fileUri("/storage/emulated/0/Download/${activity.packageName}.txt")

        assertAllowed(activity, publicFileUri)
    }

    @Test
    fun testPublicFolderNamedAsPackageNameIsAllowed() = withActivity { activity ->
        // Represents "file:///storage/emulated/0/Download/com.nextcloud.client/logs.txt"
        val publicFolderFileUri = fileUri("/storage/emulated/0/Download/${activity.packageName}/logs.txt")

        assertAllowed(activity, publicFolderFileUri)
    }

    @Test
    fun testFolderSharingPackageNamePrefixIsAllowed() = withActivity { activity ->
        val mediaRoot = activity.externalMediaDirs.first().parentFile

        // Represents "file:///storage/emulated/0/Android/media/com.nextcloud.client2/photo.jpg"
        val otherAppMediaUri = Uri.fromFile(File(mediaRoot, "${activity.packageName}2/photo.jpg"))

        assertAllowed(activity, otherAppMediaUri)
    }

    @Test
    fun testDataFolderStartingWithPackageNameIsAllowed() = withActivity { activity ->
        // Represents "file:///data/data/com.nextcloud.client.somethingsomething/logs.txt"
        val otherAppDataUri = fileUri("/data/data/${activity.packageName}.somethingsomething/logs.txt")

        assertAllowed(activity, otherAppDataUri)
    }

    @Test
    fun testContentUriWithPackageNameAsAuthorityIsAllowed() = withActivity { activity ->
        // Represents "content://com.nextcloud.client/attachments/logs.txt"
        val packageNameAuthorityUri = contentUri(activity.packageName, "/attachments/logs.txt")

        assertAllowed(activity, packageNameAuthorityUri)
    }

    @Test
    fun testMediaStoreUriIsAllowed() = withActivity { activity ->
        // Represents "content://media/external/images/media/1"
        val mediaStoreUri = Uri.parse("content://media/external/images/media/1")

        assertAllowed(activity, mediaStoreUri)
    }

    @Test
    fun testExternalStorageDocumentsUriIsAllowed() = withActivity { activity ->
        // Represents "content://com.android.externalstorage.documents/document/primary%3ADownload%2Fphoto.jpg"
        val systemPickerUri =
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2Fphoto.jpg")

        assertAllowed(activity, systemPickerUri)
    }

    @Test
    fun testOwnDocumentsProviderUriIsAllowed() = withActivity { activity ->
        // Represents "content://org.nextcloud.documents/document/1"
        val documentsProviderUri = contentUri(activity.getString(R.string.document_provider_authority), "/document/1")

        assertAllowed(activity, documentsProviderUri)
    }

    @Test
    fun testUriOfUnknownProviderIsAllowed() = withActivity { activity ->
        // Represents "content://com.example.unknown.provider/photo.jpg"
        val unknownProviderUri = contentUri("com.example.unknown.provider", "/photo.jpg")

        assertAllowed(activity, unknownProviderUri)
    }

    @Test
    fun testUriWithOtherSchemeIsNotTreatedAsSensitive() = withActivity { activity ->
        // Represents "https://example.com/photo.jpg"
        val httpsUri = Uri.parse("https://example.com/photo.jpg")

        assertAllowed(activity, httpsUri)
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

        assertEquals(UriUploaderResultCode.ERROR_SENSITIVE_PATH, result)
    }

    private fun assertAllowed(activity: TestActivity, uri: Uri) {
        assertFalse(UriChecker(activity).pointsToAppPrivateData(uri))
    }
}
