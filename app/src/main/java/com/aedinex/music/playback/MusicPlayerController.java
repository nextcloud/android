/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.playback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.aedinex.music.model.AudioMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton controller for interacting with MusicPlayerService.
 * Provides a simple interface for Activities to control music playback.
 */
public class MusicPlayerController {

    private static MusicPlayerController instance;
    private final Context context;

    private MusicPlayerService musicService;
    private boolean serviceBound = false;

    private final List<PlaybackListener> listeners = new ArrayList<>();

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicPlayerService.MusicBinder binder = (MusicPlayerService.MusicBinder) service;
            musicService = binder.getService();
            serviceBound = true;

            // Set service listener
            musicService.setPlaybackListener(new MusicPlayerService.PlaybackListener() {
                @Override
                public void onPlaybackStateChanged(boolean isPlaying) {
                    notifyPlaybackStateChanged(isPlaying);
                }

                @Override
                public void onTrackChanged(AudioMetadata track) {
                    notifyTrackChanged(track);
                }
            });

            notifyServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
            musicService = null;
            notifyServiceDisconnected();
        }
    };

    private MusicPlayerController(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Get singleton instance
     */
    public static synchronized MusicPlayerController getInstance(Context context) {
        if (instance == null) {
            instance = new MusicPlayerController(context);
        }
        return instance;
    }

    /**
     * Bind to MusicPlayerService
     */
    public void bindService() {
        if (!serviceBound) {
            Intent intent = new Intent(context, MusicPlayerService.class);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    /**
     * Unbind from service
     */
    public void unbindService() {
        if (serviceBound) {
            context.unbindService(serviceConnection);
            serviceBound = false;
            musicService = null;
        }
    }

    /**
     * Check if service is bound
     */
    public boolean isServiceBound() {
        return serviceBound;
    }

    /**
     * Play track
     */
    public void play() {
        if (serviceBound && musicService != null) {
            musicService.play();
        }
    }

    /**
     * Pause playback
     */
    public void pause() {
        if (serviceBound && musicService != null) {
            musicService.pause();
        }
    }

    /**
     * Toggle play/pause
     */
    public void togglePlayPause() {
        if (serviceBound && musicService != null) {
            if (musicService.isPlaying()) {
                musicService.pause();
            } else {
                musicService.play();
            }
        }
    }

    /**
     * Stop playback
     */
    public void stop() {
        if (serviceBound && musicService != null) {
            musicService.stop();
        }
    }

    /**
     * Skip to next track
     */
    public void skipToNext() {
        if (serviceBound && musicService != null) {
            musicService.skipToNext();
        }
    }

    /**
     * Skip to previous track
     */
    public void skipToPrevious() {
        if (serviceBound && musicService != null) {
            musicService.skipToPrevious();
        }
    }

    /**
     * Seek to position
     */
    public void seekTo(long position) {
        if (serviceBound && musicService != null) {
            musicService.seekTo(position);
        }
    }

    /**
     * Play track from queue
     */
    public void playTrack(int index) {
        if (serviceBound && musicService != null) {
            musicService.playTrack(index);
        }
    }

    /**
     * Set queue and start playback
     */
    public void playQueue(List<AudioMetadata> queue, int startIndex) {
        if (serviceBound && musicService != null) {
            musicService.setQueue(queue, startIndex);
        } else {
            // Start service if not bound
            Intent intent = new Intent(context, MusicPlayerService.class);
            context.startService(intent);
            bindService();
            // Queue will be set when service connects
        }
    }

    /**
     * Add track to queue
     */
    public void addToQueue(AudioMetadata track) {
        if (serviceBound && musicService != null) {
            musicService.addToQueue(track);
        }
    }

    /**
     * Toggle shuffle
     */
    public void toggleShuffle() {
        if (serviceBound && musicService != null) {
            musicService.toggleShuffle();
        }
    }

    /**
     * Cycle repeat mode
     */
    public void cycleRepeatMode() {
        if (serviceBound && musicService != null) {
            musicService.cycleRepeatMode();
        }
    }

    /**
     * Get current track
     */
    public AudioMetadata getCurrentTrack() {
        if (serviceBound && musicService != null) {
            return musicService.getCurrentTrack();
        }
        return null;
    }

    /**
     * Get current position
     */
    public long getCurrentPosition() {
        if (serviceBound && musicService != null) {
            return musicService.getCurrentPosition();
        }
        return 0;
    }

    /**
     * Get duration
     */
    public long getDuration() {
        if (serviceBound && musicService != null) {
            return musicService.getDuration();
        }
        return 0;
    }

    /**
     * Check if playing
     */
    public boolean isPlaying() {
        if (serviceBound && musicService != null) {
            return musicService.isPlaying();
        }
        return false;
    }

    /**
     * Get queue
     */
    public List<AudioMetadata> getQueue() {
        if (serviceBound && musicService != null) {
            return musicService.getQueue();
        }
        return new ArrayList<>();
    }

    /**
     * Check if shuffle enabled
     */
    public boolean isShuffleEnabled() {
        if (serviceBound && musicService != null) {
            return musicService.isShuffleEnabled();
        }
        return false;
    }

    /**
     * Get repeat mode
     */
    public int getRepeatMode() {
        if (serviceBound && musicService != null) {
            return musicService.getRepeatMode();
        }
        return 0;
    }

    // Listener management

    public void addPlaybackListener(PlaybackListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removePlaybackListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    private void notifyPlaybackStateChanged(boolean isPlaying) {
        for (PlaybackListener listener : listeners) {
            listener.onPlaybackStateChanged(isPlaying);
        }
    }

    private void notifyTrackChanged(AudioMetadata track) {
        for (PlaybackListener listener : listeners) {
            listener.onTrackChanged(track);
        }
    }

    private void notifyServiceConnected() {
        for (PlaybackListener listener : listeners) {
            listener.onServiceConnected();
        }
    }

    private void notifyServiceDisconnected() {
        for (PlaybackListener listener : listeners) {
            listener.onServiceDisconnected();
        }
    }

    /**
     * Listener interface for playback events
     */
    public interface PlaybackListener {
        default void onPlaybackStateChanged(boolean isPlaying) {}
        default void onTrackChanged(AudioMetadata track) {}
        default void onServiceConnected() {}
        default void onServiceDisconnected() {}
    }
}
