/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.playback;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.aedinex.music.model.AudioMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Foreground service for music playback using ExoPlayer.
 * Handles background playback, media session, and playback notifications.
 */
public class MusicPlayerService extends Service {

    private static final String TAG = "MusicPlayerService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "aedinex_music_playback";

    // ExoPlayer instance
    private ExoPlayer exoPlayer;

    // Media session for system integration
    private MediaSessionCompat mediaSession;

    // Playback queue management
    private final List<AudioMetadata> playbackQueue = new ArrayList<>();
    private int currentTrackIndex = -1;

    // Playback state
    private boolean isPlaying = false;
    private boolean shuffleEnabled = false;
    private int repeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;

    // Service binder
    private final IBinder binder = new MusicBinder();

    // Playback listener
    private PlaybackListener playbackListener;

    public class MusicBinder extends Binder {
        public MusicPlayerService getService() {
            return MusicPlayerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initializePlayer();
        initializeMediaSession();
    }

    /**
     * Initialize ExoPlayer
     */
    private void initializePlayer() {
        exoPlayer = new ExoPlayer.Builder(this).build();

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    handleTrackEnded();
                }
                updatePlaybackState();
            }

            @Override
            public void onIsPlayingChanged(boolean playing) {
                isPlaying = playing;
                updatePlaybackState();
                if (playbackListener != null) {
                    playbackListener.onPlaybackStateChanged(playing);
                }
            }
        });
    }

    /**
     * Initialize MediaSession for system integration
     */
    private void initializeMediaSession() {
        mediaSession = new MediaSessionCompat(this, TAG);
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );

        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                play();
            }

            @Override
            public void onPause() {
                pause();
            }

            @Override
            public void onSkipToNext() {
                skipToNext();
            }

            @Override
            public void onSkipToPrevious() {
                skipToPrevious();
            }

            @Override
            public void onSeekTo(long pos) {
                seekTo(pos);
            }

            @Override
            public void onStop() {
                stop();
            }
        });

        mediaSession.setActive(true);
        updatePlaybackState();
    }

    /**
     * Update media session playback state
     */
    private void updatePlaybackState() {
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY |
                PlaybackStateCompat.ACTION_PAUSE |
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT |
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS |
                PlaybackStateCompat.ACTION_SEEK_TO |
                PlaybackStateCompat.ACTION_STOP
            )
            .setState(
                isPlaying ? PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_PAUSED,
                exoPlayer.getCurrentPosition(),
                1.0f
            );

        mediaSession.setPlaybackState(stateBuilder.build());
    }

    /**
     * Play current track
     */
    public void play() {
        if (exoPlayer != null) {
            exoPlayer.play();
            isPlaying = true;
            startForeground(NOTIFICATION_ID, createNotification());
        }
    }

    /**
     * Pause playback
     */
    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.pause();
            isPlaying = false;
        }
    }

    /**
     * Stop playback and service
     */
    public void stop() {
        pause();
        exoPlayer.seekTo(0);
        stopForeground(true);
        stopSelf();
    }

    /**
     * Skip to next track
     */
    public void skipToNext() {
        if (currentTrackIndex < playbackQueue.size() - 1) {
            playTrack(currentTrackIndex + 1);
        } else if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) {
            playTrack(0);
        }
    }

    /**
     * Skip to previous track
     */
    public void skipToPrevious() {
        if (exoPlayer.getCurrentPosition() > 3000) {
            // If more than 3 seconds, restart current track
            exoPlayer.seekTo(0);
        } else if (currentTrackIndex > 0) {
            playTrack(currentTrackIndex - 1);
        }
    }

    /**
     * Seek to position
     */
    public void seekTo(long position) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(position);
        }
    }

    /**
     * Play specific track from queue
     */
    public void playTrack(int index) {
        if (index >= 0 && index < playbackQueue.size()) {
            currentTrackIndex = index;
            AudioMetadata track = playbackQueue.get(index);

            MediaItem mediaItem = MediaItem.fromUri(track.getFilePath());
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.play();

            if (playbackListener != null) {
                playbackListener.onTrackChanged(track);
            }
        }
    }

    /**
     * Set playback queue
     */
    public void setQueue(List<AudioMetadata> queue, int startIndex) {
        playbackQueue.clear();
        playbackQueue.addAll(queue);
        currentTrackIndex = startIndex;

        if (!queue.isEmpty() && startIndex >= 0 && startIndex < queue.size()) {
            playTrack(startIndex);
        }
    }

    /**
     * Add track to queue
     */
    public void addToQueue(AudioMetadata track) {
        playbackQueue.add(track);
    }

    /**
     * Toggle shuffle mode
     */
    public void toggleShuffle() {
        shuffleEnabled = !shuffleEnabled;
        // TODO: Implement shuffle logic
    }

    /**
     * Cycle repeat mode
     */
    public void cycleRepeatMode() {
        switch (repeatMode) {
            case PlaybackStateCompat.REPEAT_MODE_NONE:
                repeatMode = PlaybackStateCompat.REPEAT_MODE_ALL;
                break;
            case PlaybackStateCompat.REPEAT_MODE_ALL:
                repeatMode = PlaybackStateCompat.REPEAT_MODE_ONE;
                break;
            case PlaybackStateCompat.REPEAT_MODE_ONE:
                repeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
                break;
        }
    }

    /**
     * Handle track end
     */
    private void handleTrackEnded() {
        if (repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE) {
            exoPlayer.seekTo(0);
            exoPlayer.play();
        } else {
            skipToNext();
        }
    }

    /**
     * Create notification for foreground service
     */
    private Notification createNotification() {
        // TODO: Implement proper notification with MediaStyle
        // For now, return a basic notification
        return new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AediNex Music")
            .setContentText("Playing...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build();
    }

    // Getters
    public boolean isPlaying() {
        return isPlaying;
    }

    public AudioMetadata getCurrentTrack() {
        if (currentTrackIndex >= 0 && currentTrackIndex < playbackQueue.size()) {
            return playbackQueue.get(currentTrackIndex);
        }
        return null;
    }

    public long getCurrentPosition() {
        return exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return exoPlayer != null ? exoPlayer.getDuration() : 0;
    }

    public List<AudioMetadata> getQueue() {
        return new ArrayList<>(playbackQueue);
    }

    public boolean isShuffleEnabled() {
        return shuffleEnabled;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public void setPlaybackListener(PlaybackListener listener) {
        this.playbackListener = listener;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }
    }

    /**
     * Listener interface for playback events
     */
    public interface PlaybackListener {
        void onPlaybackStateChanged(boolean isPlaying);
        void onTrackChanged(AudioMetadata track);
    }
}
