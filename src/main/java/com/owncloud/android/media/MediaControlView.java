/**
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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.utils.ThemeUtils;

import java.util.Formatter;
import java.util.Locale;


/**
 * View containing controls for a {@link MediaPlayer}. 
 *
 * Holds buttons "play / pause", "rewind", "fast forward" 
 * and a progress slider. 
 *
 * It synchronizes itself with the state of the 
 * {@link MediaPlayer}.
 */

public class MediaControlView extends FrameLayout /* implements OnLayoutChangeListener, OnTouchListener */ implements OnClickListener, OnSeekBarChangeListener {

    private MediaPlayerControl mPlayer;
    private Context mContext;
    private View mRoot;
    private ProgressBar mProgress;
    private TextView mEndTime, mCurrentTime;
    private boolean mDragging;
    private static final int SHOW_PROGRESS = 1;
    private ImageButton mPauseButton;
    private ImageButton mFfwdButton;
    private ImageButton mRewButton;

    public MediaControlView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        FrameLayout.LayoutParams frameParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mRoot = inflate.inflate(R.layout.media_control, null);
        initControllerView(mRoot);
        addView(mRoot, frameParams);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        requestFocus();
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        /*
        if (mRoot != null)
            initControllerView(mRoot);
         */
    }

    /* TODO REMOVE
    public MediaControlView(Context context, boolean useFastForward) {
        super(context);
        mContext = context;
        mUseFastForward = useFastForward;
        initFloatingWindowLayout();
        //initFloatingWindow();
    }
    */

    /* TODO REMOVE
    public MediaControlView(Context context) {
        this(context, true);
    }
    */
    
    /* T
    private void initFloatingWindow() {
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindow = PolicyManager.makeNewWindow(mContext);
        mWindow.setWindowManager(mWindowManager, null, null);
        mWindow.requestFeature(Window.FEATURE_NO_TITLE);
        mDecor = mWindow.getDecorView();
        mDecor.setOnTouchListener(mTouchListener);
        mWindow.setContentView(this);
        mWindow.setBackgroundDrawableResource(android.R.color.transparent);
        
        // While the media controller is up, the volume control keys should
        // affect the media stream type
        mWindow.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        setFocusable(true);
        setFocusableInTouchMode(true);
        setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        requestFocus();
    }
    */

    /*
    // Allocate and initialize the static parts of mDecorLayoutParams. Must
    // also call updateFloatingWindowLayout() to fill in the dynamic parts
    // (y and width) before mDecorLayoutParams can be used.
    private void initFloatingWindowLayout() {
        mDecorLayoutParams = new WindowManager.LayoutParams();
        WindowManager.LayoutParams p = mDecorLayoutParams;
        p.gravity = Gravity.TOP;
        p.height = LayoutParams.WRAP_CONTENT;
        p.x = 0;
        p.format = PixelFormat.TRANSLUCENT;
        p.type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL;
        p.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH;
        p.token = null;
        p.windowAnimations = 0; // android.R.style.DropDownAnimationDown;
    }
    */

    // Update the dynamic parts of mDecorLayoutParams
    // Must be called with mAnchor != NULL.
    /*
    private void updateFloatingWindowLayout() {
        int [] anchorPos = new int[2];
        mAnchor.getLocationOnScreen(anchorPos);

        WindowManager.LayoutParams p = mDecorLayoutParams;
        p.width = mAnchor.getWidth();
        p.y = anchorPos[1] + mAnchor.getHeight();
    }
    */

    /*
    // This is called whenever mAnchor's layout bound changes
    public void onLayoutChange(View v, int left, int top, int right,
            int bottom, int oldLeft, int oldTop, int oldRight,
            int oldBottom) {
        //updateFloatingWindowLayout();
        if (mShowing) {
            mWindowManager.updateViewLayout(mDecor, mDecorLayoutParams);
        }
    }
    */
    
    /*
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (mShowing) {
                hide();
            }
        }
            return false;
    }
    */


    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        updatePausePlay();
    }

    public void stopMediaPlayerMessages() {
        mHandler.removeMessages(SHOW_PROGRESS);
    }


    private void initControllerView(View v) {
        mPauseButton = (ImageButton) v.findViewById(R.id.playBtn);
        if (mPauseButton != null) {
            mPauseButton.requestFocus();
            mPauseButton.setOnClickListener(this);
        }

        mFfwdButton = (ImageButton) v.findViewById(R.id.forwardBtn);
        if (mFfwdButton != null) {
            mFfwdButton.setOnClickListener(this);
        }

        mRewButton = (ImageButton) v.findViewById(R.id.rewindBtn);
        if (mRewButton != null) {
            mRewButton.setOnClickListener(this);
        }

        mProgress = (ProgressBar) v.findViewById(R.id.progressBar);
        if (mProgress != null) {
            if (mProgress instanceof SeekBar) {
                SeekBar seeker = (SeekBar) mProgress;
                ThemeUtils.colorHorizontalSeekBar(seeker);
                seeker.setOnSeekBarChangeListener(this);
            } else {
                ThemeUtils.colorHorizontalProgressBar(mProgress, ThemeUtils.primaryAccentColor());
            }
            mProgress.setMax(1000);
        }

        mEndTime = (TextView) v.findViewById(R.id.totalTimeText);
        mCurrentTime = (TextView) v.findViewById(R.id.currentTimeText);
    }


    /**
     * Disable pause or seek buttons if the stream cannot be paused or seeked.
     * This requires the control interface to be a MediaPlayerControlExt
     */
    private void disableUnsupportedButtons() {
        try {
            if (mPauseButton != null && !mPlayer.canPause()) {
                mPauseButton.setEnabled(false);
            }
            if (mRewButton != null && !mPlayer.canSeekBackward()) {
                mRewButton.setEnabled(false);
            }
            if (mFfwdButton != null && !mPlayer.canSeekForward()) {
                mFfwdButton.setEnabled(false);
            }
        } catch (IncompatibleClassChangeError ex) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
        }
    }


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            int pos;
            if (msg.what == SHOW_PROGRESS) {
                pos = setProgress();
                if (!mDragging) {
                    msg = obtainMessage(SHOW_PROGRESS);
                    sendMessageDelayed(msg, 1000 - (pos % 1000));
                }
            }
        }
    };

    private String stringForTime(int timeMs) {
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
        if (mPlayer == null || mDragging) {
            return 0;
        }
        int position = mPlayer.getCurrentPosition();
        int duration = mPlayer.getDuration();
        if (mProgress != null) {
            if (duration > 0) {
                // use long to avoid overflow
                long pos = 1000L * position / duration;
                mProgress.setProgress((int) pos);
            }
            int percent = mPlayer.getBufferPercentage();
            mProgress.setSecondaryProgress(percent * 10);
        }

        if (mEndTime != null) {
            mEndTime.setText(stringForTime(duration));
        }
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime(position));
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
                if (mPauseButton != null) {
                    mPauseButton.requestFocus();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
            if (uniqueDown && !mPlayer.isPlaying()) {
                mPlayer.start();
                updatePausePlay();
                //show(sDefaultTimeout);
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
            if (uniqueDown && mPlayer.isPlaying()) {
                mPlayer.pause();
                updatePausePlay();
                //show(sDefaultTimeout);
            }
            return true;
        }

        //show(sDefaultTimeout);
        return super.dispatchKeyEvent(event);
    }

    public void updatePausePlay() {
        if (mRoot == null || mPauseButton == null) {
            return;
        }

        if (mPlayer.isPlaying()) {
            mPauseButton.setImageResource(android.R.drawable.ic_media_pause);
        } else {
            mPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
    }

    private void doPauseResume() {
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
        } else {
            mPlayer.start();
        }
        updatePausePlay();
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (mPauseButton != null) {
            mPauseButton.setEnabled(enabled);
        }
        if (mFfwdButton != null) {
            mFfwdButton.setEnabled(enabled);
        }
        if (mRewButton != null) {
            mRewButton.setEnabled(enabled);
        }
        if (mProgress != null) {
            mProgress.setEnabled(enabled);
        }
        disableUnsupportedButtons();
        super.setEnabled(enabled);
    }

    @Override
    public void onClick(View v) {
        int pos;
        boolean playing = mPlayer.isPlaying();
        switch (v.getId()) {

            case R.id.playBtn:
                doPauseResume();
                break;

            case R.id.rewindBtn:
                pos = mPlayer.getCurrentPosition();
                pos -= 5000;
                mPlayer.seekTo(pos);
                if (!playing) {
                    mPlayer.pause();  // necessary in some 2.3.x devices
                }
                setProgress();
                break;

            case R.id.forwardBtn:
                pos = mPlayer.getCurrentPosition();
                pos += 15000;
                mPlayer.seekTo(pos);
                if (!playing) {
                    mPlayer.pause(); // necessary in some 2.3.x devices
                }
                setProgress();
                break;
        }
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            // We're not interested in programmatically generated changes to
            // the progress bar's position.
            return;
        }

        long duration = mPlayer.getDuration();
        long newposition = (duration * progress) / 1000L;
        mPlayer.seekTo((int) newposition);
        if (mCurrentTime != null) {
            mCurrentTime.setText(stringForTime((int) newposition));
        }
    }

    /**
     * Called in devices with touchpad when the user starts to adjust the 
     * position of the seekbar's thumb.
     *
     * Will be followed by several onProgressChanged notifications.
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mDragging = true;                           // monitors the duration of dragging 
        mHandler.removeMessages(SHOW_PROGRESS);     // grants no more updates with media player progress while dragging 
    }


    /**
     * Called in devices with touchpad when the user finishes the
     * adjusting of the seekbar.
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mDragging = false;
        setProgress();
        updatePausePlay();
        mHandler.sendEmptyMessage(SHOW_PROGRESS);    // grants future updates with media player progress 
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
