/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.nextcloud.client.player.media3.PlaybackService
import com.nextcloud.client.player.ui.PlayerActivity
import com.nextcloud.client.player.ui.PlayerProgressIndicator
import com.nextcloud.client.player.ui.audio.AudioFileFragment
import com.nextcloud.client.player.ui.audio.AudioPlayerView
import com.nextcloud.client.player.ui.control.PlayerControlView
import com.nextcloud.client.player.ui.video.VideoFileFragment
import com.nextcloud.client.player.ui.video.VideoPlayerView
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import java.io.File
import javax.inject.Singleton

private const val PLAYER_CACHE_DIR_NAME = "player"
private const val PLAYER_CACHE_SIZE = 300 * 1024 * 1024L

@Module(includes = [PlayerModule.AndroidInjector::class])
class PlayerModule {

    @Provides
    @Singleton
    @UnstableApi
    fun provideCache(context: Context): Cache = SimpleCache(
        File(context.cacheDir, PLAYER_CACHE_DIR_NAME),
        LeastRecentlyUsedCacheEvictor(PLAYER_CACHE_SIZE)
    )

    @Module
    abstract class AndroidInjector {

        @UnstableApi
        @ContributesAndroidInjector
        abstract fun playbackService(): PlaybackService

        @ContributesAndroidInjector
        abstract fun playerActivity(): PlayerActivity

        @ContributesAndroidInjector
        abstract fun audioPlayerView(): AudioPlayerView

        @ContributesAndroidInjector
        abstract fun videoPlayerView(): VideoPlayerView

        @ContributesAndroidInjector
        abstract fun playerControlView(): PlayerControlView

        @ContributesAndroidInjector
        abstract fun playerProgressIndicator(): PlayerProgressIndicator

        @ContributesAndroidInjector
        abstract fun audioFileFragment(): AudioFileFragment

        @ContributesAndroidInjector
        abstract fun videoFileFragment(): VideoFileFragment
    }
}
