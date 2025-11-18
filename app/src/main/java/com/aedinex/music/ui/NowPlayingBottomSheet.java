/*
 * AediNex Music - Android Client
 *
 * SPDX-FileCopyrightText: 2025 AediNex
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.aedinex.music.ui;

import android.app.Activity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.aedinex.music.model.AudioMetadata;
import com.aedinex.music.playback.MusicPlayerController;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.owncloud.android.R;

/**
 * Bottom sheet for Now Playing mini player.
 * Shows at the bottom of the screen when music is playing.
 */
public class NowPlayingBottomSheet implements MusicPlayerController.PlaybackListener {

    private final Activity activity;
    private final View bottomSheet;
    private final BottomSheetBehavior<View> bottomSheetBehavior;

    private final ImageView albumArt;
    private final TextView trackTitle;
    private final TextView trackArtist;
    private final ImageButton playPauseButton;
    private final ImageButton nextButton;

    private final MusicPlayerController playerController;

    public NowPlayingBottomSheet(Activity activity, View bottomSheetView) {
        this.activity = activity;
        this.bottomSheet = bottomSheetView;
        this.playerController = MusicPlayerController.getInstance(activity);

        // Initialize views
        albumArt = bottomSheet.findViewById(R.id.img_mini_album_art);
        trackTitle = bottomSheet.findViewById(R.id.txt_mini_track_title);
        trackArtist = bottomSheet.findViewById(R.id.txt_mini_track_artist);
        playPauseButton = bottomSheet.findViewById(R.id.btn_mini_play_pause);
        nextButton = bottomSheet.findViewById(R.id.btn_mini_next);

        // Setup bottom sheet behavior
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehavior.setHideable(true);

        setupListeners();
        playerController.addPlaybackListener(this);
    }

    private void setupListeners() {
        // Click on bottom sheet opens full Now Playing
        bottomSheet.setOnClickListener(v -> openNowPlaying());

        // Play/Pause button
        playPauseButton.setOnClickListener(v -> playerController.togglePlayPause());

        // Next button
        nextButton.setOnClickListener(v -> playerController.skipToNext());
    }

    /**
     * Update UI with current track
     */
    public void updateTrack(AudioMetadata track) {
        if (track != null) {
            trackTitle.setText(track.getTitle());
            trackArtist.setText(track.getArtist());
            // TODO: Load album art
            show();
        } else {
            hide();
        }
    }

    /**
     * Update play/pause button state
     */
    public void updatePlaybackState(boolean isPlaying) {
        if (isPlaying) {
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    /**
     * Show bottom sheet
     */
    public void show() {
        if (bottomSheetBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN) {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    /**
     * Hide bottom sheet
     */
    public void hide() {
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    /**
     * Open full Now Playing activity
     */
    private void openNowPlaying() {
        // TODO: Launch NowPlayingActivity
        // Intent intent = new Intent(activity, NowPlayingActivity.class);
        // activity.startActivity(intent);
    }

    /**
     * Clean up
     */
    public void destroy() {
        playerController.removePlaybackListener(this);
    }

    // MusicPlayerController.PlaybackListener implementation

    @Override
    public void onPlaybackStateChanged(boolean isPlaying) {
        activity.runOnUiThread(() -> updatePlaybackState(isPlaying));
    }

    @Override
    public void onTrackChanged(AudioMetadata track) {
        activity.runOnUiThread(() -> updateTrack(track));
    }

    @Override
    public void onServiceConnected() {
        activity.runOnUiThread(() -> {
            AudioMetadata currentTrack = playerController.getCurrentTrack();
            if (currentTrack != null) {
                updateTrack(currentTrack);
                updatePlaybackState(playerController.isPlaying());
            }
        });
    }

    @Override
    public void onServiceDisconnected() {
        activity.runOnUiThread(this::hide);
    }
}
