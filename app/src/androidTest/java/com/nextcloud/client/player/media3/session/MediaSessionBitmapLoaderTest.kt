/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.media3.session

import android.graphics.Bitmap
import android.graphics.Bitmap.Config.ARGB_8888
import androidx.media3.common.MediaMetadata
import androidx.test.platform.app.InstrumentationRegistry
import com.nextcloud.client.player.media3.common.setExtras
import com.nextcloud.client.player.model.ThumbnailLoader
import com.nextcloud.client.player.model.file.PlaybackFile
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

class MediaSessionBitmapLoaderTest {

    private lateinit var thumbnailLoader: ThumbnailLoader
    private lateinit var bitmapLoader: MediaSessionBitmapLoader
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val playbackFile = PlaybackFile(
        id = "123",
        uri = "/remote.php/dav/files/user/song.mp3",
        name = "song.mp3",
        mimeType = "audio/mpeg",
        contentLength = 1L,
        lastModified = 0L,
        isFavorite = false
    )

    @Before
    fun setup() {
        thumbnailLoader = mockk(relaxed = true)
        bitmapLoader = MediaSessionBitmapLoader(context, thumbnailLoader)
    }

    @Test
    fun loads_from_artworkData() {
        val expected = bitmap(0xFF00FF00.toInt())
        every {
            thumbnailLoader.load(context, any<Any>(), playbackFile.id, any(), any())
        } returns completed(expected)

        val future = bitmapLoader.loadBitmapFromMetadata(metadata(artworkData = byteArrayOf(1, 2, 3)))!!
        assertSame(expected, future.get())
    }

    @Test
    fun loads_from_file_when_no_artwork() {
        val expected = bitmap(0xFFFF0000.toInt())
        every {
            thumbnailLoader.load(context, playbackFile, any(), any())
        } returns completed(expected)

        val future = bitmapLoader.loadBitmapFromMetadata(metadata())!!
        assertSame(expected, future.get())
    }

    @Test
    fun falls_back_to_default_icon_when_null() {
        every {
            thumbnailLoader.load(context, playbackFile, any(), any())
        } returns completed(null)

        val future = bitmapLoader.loadBitmapFromMetadata(metadata())!!
        val result = future.get()
        assertNotNull(result)
        assertTrue(result.width > 0 && result.height > 0)
    }

    @Test
    fun returns_cached_future_for_same_request() {
        val bitmap = bitmap(0xFF112233.toInt())
        every {
            thumbnailLoader.load(context, any<Any>(), playbackFile.id, any(), any())
        } returns completed(bitmap)

        val metadata = metadata(artworkData = byteArrayOf(9))
        val future1 = bitmapLoader.loadBitmapFromMetadata(metadata)!!
        val future2 = bitmapLoader.loadBitmapFromMetadata(metadata)!!
        assertSame(future1, future2)
    }

    @Test
    fun different_artworkData_invalidates_cache() {
        val bitmap1 = bitmap(0xFF010101.toInt())
        val bitmap2 = bitmap(0xFF020202.toInt())
        every {
            thumbnailLoader.load(context, any<Any>(), playbackFile.id, any(), any())
        } returnsMany listOf(completed(bitmap1), completed(bitmap2))

        val future1 = bitmapLoader.loadBitmapFromMetadata(metadata(artworkData = byteArrayOf(1)))!!
        val future2 = bitmapLoader.loadBitmapFromMetadata(metadata(artworkData = byteArrayOf(2)))!!
        assertNotSame(future1, future2)
        assertNotSame(future1.get(), future2.get())
    }

    private fun bitmap(color: Int): Bitmap = Bitmap.createBitmap(intArrayOf(color), 1, 1, ARGB_8888)

    private fun completed(bitmap: Bitmap?): Future<Bitmap> = CompletableFuture.completedFuture(bitmap)

    private fun metadata(artworkData: ByteArray? = null): MediaMetadata = MediaMetadata.Builder()
        .setExtras(playbackFile)
        .setArtworkData(artworkData, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        .build()
}
