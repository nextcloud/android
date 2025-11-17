/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.nextcloud.client.player.media3.ExoPlayerFactory
import com.nextcloud.client.player.media3.Media3PlaybackModel
import com.nextcloud.client.player.media3.PlaybackService
import com.nextcloud.client.player.media3.common.PlayerFactory
import com.nextcloud.client.player.media3.controller.DefaultMediaControllerFactory
import com.nextcloud.client.player.media3.controller.MediaControllerFactory
import com.nextcloud.client.player.media3.datasource.DefaultDataSourceFactory
import com.nextcloud.client.player.media3.session.DefaultMediaSessionFactory
import com.nextcloud.client.player.media3.session.MediaSessionFactory
import com.nextcloud.client.player.media3.session.MediaSessionHolder
import com.nextcloud.client.player.model.GlideThumbnailLoader
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.ThumbnailLoader
import com.nextcloud.client.player.model.error.DefaultPlaybackErrorStrategy
import com.nextcloud.client.player.model.error.PlaybackErrorStrategy
import com.nextcloud.client.player.ui.PlayerActivity
import com.nextcloud.client.player.ui.PlayerProgressIndicator
import com.nextcloud.client.player.ui.audio.AudioFileFragment
import com.nextcloud.client.player.ui.audio.AudioPlayerView
import com.nextcloud.client.player.ui.control.PlayerControlView
import com.nextcloud.client.player.ui.video.VideoFileFragment
import com.nextcloud.client.player.ui.video.VideoPlayerView
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.android.ContributesAndroidInjector
import java.io.File
import javax.inject.Singleton

private const val PLAYER_CACHE_DIR_NAME = "player"
private const val PLAYER_CACHE_SIZE = 300 * 1024 * 1024L

@Module(includes = [PlayerModule.Bindings::class, PlayerModule.AndroidInjector::class])
class PlayerModule {

    @Provides
    @Singleton
    @UnstableApi
    fun provideCache(context: Context): Cache = SimpleCache(
        File(context.cacheDir, PLAYER_CACHE_DIR_NAME),
        LeastRecentlyUsedCacheEvictor(PLAYER_CACHE_SIZE)
    )

    @Module
    abstract class Bindings {

        @Binds
        @Singleton
        @UnstableApi
        abstract fun playbackModel(model: Media3PlaybackModel): PlaybackModel

        @Binds
        @Singleton
        @UnstableApi
        abstract fun mediaSessionHolder(playbackModel: Media3PlaybackModel): MediaSessionHolder

        @Binds
        @UnstableApi
        abstract fun mediaSessionFactory(sessionFactory: DefaultMediaSessionFactory): MediaSessionFactory

        @Binds
        @UnstableApi
        abstract fun mediaControllerFactory(controllerFactory: DefaultMediaControllerFactory): MediaControllerFactory

        @Binds
        @UnstableApi
        abstract fun playerFactory(playbackFactory: ExoPlayerFactory): PlayerFactory

        @Binds
        @UnstableApi
        abstract fun dataSourceFactory(dataSourceFactory: DefaultDataSourceFactory): DataSource.Factory

        @Binds
        abstract fun playbackErrorStrategy(strategy: DefaultPlaybackErrorStrategy): PlaybackErrorStrategy

        @Binds
        abstract fun thumbnailLoader(thumbnailLoader: GlideThumbnailLoader): ThumbnailLoader
    }

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
