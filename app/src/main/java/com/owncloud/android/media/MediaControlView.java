/*
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2015 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.media;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.LinearLayout;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.MediaControlBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.Formatter;
import java.util.Locale;

import javax.inject.Inject;


/**
 * View containing controls for a {@link MediaPlayer}.
 * <p>
 * Holds buttons "play / pause", "rewind", "fast forward" and a progress slider.
 * <p>
 * It synchronizes itself with the state of the {@link MediaPlayer}.
 */
public class MediaControlView extends LinearLayout implements OnClickListener, OnSeekBarChangeListener {
    private static final String TAG = MediaControlView.class.getSimpleName();
    private static final int SHOW_PROGRESS = 1;

    private MediaPlayerControl playerControl;
    private final MediaControlBinding binding;
    private boolean isDragging;

    @Inject
    ViewThemeUtils viewThemeUtils;


    public MediaControlView(Context context,
                            AttributeSet attrs) {
        super(context, attrs);

        MainApp.getAppComponent().inject(this);

        LayoutInflater inflate = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        binding = MediaControlBinding.inflate(inflate, this, true);
        initControllerView();

        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        requestFocus();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
    }

    public void setMediaPlayer(MediaPlayerControl player) {
        playerControl = player;
        handler.sendEmptyMessage(SHOW_PROGRESS);
        handler.postDelayed(() -> {
            updatePausePlay();
            setProgress();
        }, 100);
    }

    public void stopMediaPlayerMessages() {
        handler.removeMessages(SHOW_PROGRESS);
    }

    private void initControllerView() {
        binding.playBtn.requestFocus();
        binding.playBtn.setOnClickListener(this);

        binding.forwardBtn.setOnClickListener(this);

        binding.rewindBtn.setOnClickListener(this);

        viewThemeUtils.platform.themeHorizontalSeekBar(binding.progressBar);
        binding.progressBar.setOnSeekBarChangeListener(this);
        binding.progressBar.setMax(1000);
    }

    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
            if (binding != null) {
                if (!playerControl.canPause()) {
                    binding.playBtn.setEnabled(false);
                }
                if (!playerControl.canSeekBackward()) {
                    binding.rewindBtn.setEnabled(false);
                }
                if (!playerControl.canSeekForward()) {
                    binding.forwardBtn.setEnabled(false);
                }
            }

        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
            Log_OC.i(TAG, "Old media interface detected");
        }
    }

    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SHOW_PROGRESS) {
                updatePausePlay();
                int pos = setProgress();
                if (!isDragging) {
                    sendMessageDelayed(obtainMessage(SHOW_PROGRESS), 1000 - (pos % 1000));
                }
            }
        }
    };

    private String formatTime(int timeMs) {
        int totalSeconds = timeMs / 1000;

        int seconds = totalSeconds % 60;
        int minutes = (totalSeconds / 60) % 60;
        int hours = totalSeconds / 3600;

        final StringBuilder mFormatBuilder = new StringBuilder();
        final Formatter mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
        if (hours > 0) {
            return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
        } else {
            return mFormatter.format("%02d:%02d", minutes, seconds).toString();
        }
    }

    private int setProgress() {
        if (playerControl == null || isDragging) {
            return 0;
        }
        int position = playerControl.getCurrentPosition();
        int duration = playerControl.getDuration();
        if (binding != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                binding.progressBar.setProgress((int) pos);
            }
            int percent = playerControl.getBufferPercentage();
            binding.progressBar.setSecondaryProgress(percent * 10);

            String endTime = duration > 0 ? formatTime(duration) : "--:--";
            binding.totalTimeText.setText(endTime);
            binding.currentTimeText.setText(formatTime(position));
        }

        return position;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        final boolean uniqueDown = event.getRepeatCount() == 0
                && event.getAction() == KeyEvent.ACTION_DOWN;
        if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (uniqueDown) {
                doPauseResume();
                //show(sDefaultTimeout);
                if (binding != null) {
                    binding.playBtn.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !playerControl.isPlaying()) {
                playerControl.start();
                updatePausePlay();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && playerControl.isPlaying()) {
                playerControl.pause();
                updatePausePlay();
            }
            return true;
        }

        return super.dispatchKeyEvent(event);
    }

    public void updatePausePlay() {
        if (binding == null) {
            return;
        }

        if (playerControl.isPlaying()) {
            binding.playBtn.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            binding.playBtn.setImageResource(android.R.drawable.ic_media_play);
        }

        final boolean canSeekFfd = playerControl.canSeekForward();
        if (canSeekFfd) {
            binding.forwardBtn.setVisibility(View.VISIBLE);
        } else {
            binding.forwardBtn.setVisibility(View.INVISIBLE);
        }

        final boolean canSeekBwd = playerControl.canSeekBackward();
        if (canSeekBwd) {
            binding.rewindBtn.setVisibility(View.VISIBLE);
        } else {
            binding.rewindBtn.setVisibility(View.INVISIBLE);
        }
    }

    private void doPauseResume() {
        if (playerControl.isPlaying()) {
            playerControl.pause();
        } else {
            playerControl.start();
        }
        updatePausePlay();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if(binding!=null){
            binding.playBtn.setEnabled(enabled);
            binding.forwardBtn.setEnabled(enabled);
            binding.rewindBtn.setEnabled(enabled);
            binding.progressBar.setEnabled(enabled);
        }

        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        int pos;
        boolean playing = playerControl.isPlaying();
        int id = v.getId();

        if (id == R.id.playBtn) {
            doPauseResume();
        } else if (id == R.id.rewindBtn) {
            pos = playerControl.getCurrentPosition();
            pos -= 5000;
            playerControl.seekTo(pos);
            if (!playing) {
                playerControl.pause();  // necessary in some 2.3.x devices
            }
            setProgress();
        } else if (id == R.id.forwardBtn) {
            pos = playerControl.getCurrentPosition();
            pos += 15000;
            playerControl.seekTo(pos);
            if (!playing) {
                playerControl.pause(); // necessary in some 2.3.x devices
            }
            setProgress();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            // We're not interested in programmatically generated changes to
            // the progress bar's position.
            return;
        }

        long duration = playerControl.getDuration();
        long newPosition = (duration * progress) / 1000L;
        playerControl.seekTo((int) newPosition);
        binding.currentTimeText.setText(formatTime((int) newPosition));
    }

    /**
     * Called in devices with touchpad when the user starts to adjust the position of the seekbar's thumb.
     *
     * Will be followed by several onProgressChanged notifications.
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isDragging = true;                           // monitors the duration of dragging
        handler.removeMessages(SHOW_PROGRESS);     // grants no more updates with media player progress while dragging
    }

    /**
     * Called in devices with touchpad when the user finishes the adjusting of the seekbar.
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        isDragging = false;
        setProgress();
        updatePausePlay();
        handler.sendEmptyMessage(SHOW_PROGRESS);    // grants future updates with media player progress
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        event.setClassName(MediaControlView.class.getName());
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.setClassName(MediaControlView.class.getName());
    }
}
