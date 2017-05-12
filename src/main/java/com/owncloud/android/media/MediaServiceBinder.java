/**
 * ownCloud Android client application
 *
 * @author David A. Velasco
 * Copyright (C) 2016 ownCloud Inc.
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


import android.accounts.Account;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.widget.MediaController;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.media.MediaService.State;


/**
 *  Binder allowing client components to perform operations on on the MediaPlayer managed by a MediaService instance.
 *
 *  Provides the operations of {@link MediaController.MediaPlayerControl}, and an extra method to check if
 *  an {@link OCFile} instance is handled by the MediaService.
 */
public class MediaServiceBinder extends Binder implements MediaController.MediaPlayerControl {

    private static final String TAG = MediaServiceBinder.class.getSimpleName();
    /**
     * {@link MediaService} instance to access with the binder
     */
    private MediaService mService = null;

    /**
     * Public constructor
     *
     * @param service       A {@link MediaService} instance to access with the binder 
     */
    public MediaServiceBinder(MediaService service) {
        if (service == null) {
            throw new IllegalArgumentException("Argument 'service' can not be null");
        }
        mService = service;
    }


    public boolean isPlaying(OCFile mFile) {
        return (mFile != null && mFile.equals(mService.getCurrentFile()));
    }


    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getBufferPercentage() {
        MediaPlayer currentPlayer = mService.getPlayer();
        if (currentPlayer != null) {
            return 100;
            // TODO update for streamed playback; add OnBufferUpdateListener in MediaService
        } else {
            return 0;
        }
    }

    @Override
    public int getCurrentPosition() {
        MediaPlayer currentPlayer = mService.getPlayer();
        if (currentPlayer != null) {
            return currentPlayer.getCurrentPosition();
        } else {
            return 0;
        }
    }

    @Override
    public int getDuration() {
        MediaPlayer currentPlayer = mService.getPlayer();
        if (currentPlayer != null) {
            return currentPlayer.getDuration();
        } else {
            return 0;
        }
    }


    /**
     * Reports if the MediaService is playing a file or not.
     *
     * Considers that the file is being played when it is in preparation because the expected
     * client of this method is a {@link MediaController} , and we do not want that the 'play'
     * button is shown when the file is being prepared by the MediaService.
     */
    @Override
    public boolean isPlaying() {
        MediaService.State currentState = mService.getState();
        return (currentState == State.PLAYING || (currentState == State.PREPARING && mService.mPlayOnPrepared));
    }


    @Override
    public void pause() {
        Log_OC.d(TAG, "Pausing through binder...");
        mService.processPauseRequest();
    }

    @Override
    public void seekTo(int pos) {
        Log_OC.d(TAG, "Seeking " + pos + " through binder...");
        MediaPlayer currentPlayer = mService.getPlayer();
        MediaService.State currentState = mService.getState();
        if (currentPlayer != null && currentState != State.PREPARING && currentState != State.STOPPED) {
            currentPlayer.seekTo(pos);
        }
    }

    @Override
    public void start() {
        Log_OC.d(TAG, "Starting through binder...");
        mService.processPlayRequest();  // this will finish the service if there is no file preloaded to play
    }

    public void start(Account account, OCFile file, boolean playImmediately, int position) {
        Log_OC.d(TAG, "Loading and starting through binder...");
        Intent i = new Intent(mService, MediaService.class);
        i.putExtra(MediaService.EXTRA_ACCOUNT, account);
        i.putExtra(MediaService.EXTRA_FILE, file);
        i.putExtra(MediaService.EXTRA_PLAY_ON_LOAD, playImmediately);
        i.putExtra(MediaService.EXTRA_START_POSITION, position);
        i.setAction(MediaService.ACTION_PLAY_FILE);
        mService.startService(i);
    }


    public void registerMediaController(MediaControlView mediaController) {
        mService.setMediaContoller(mediaController);
    }

    public void unregisterMediaController(MediaControlView mediaController) {
        if (mediaController != null && mediaController == mService.getMediaController()) {
            mService.setMediaContoller(null);
        }

    }

    public boolean isInPlaybackState() {
        MediaService.State currentState = mService.getState();
        return (currentState == MediaService.State.PLAYING || currentState == MediaService.State.PAUSED);
    }


    @Override
    public int getAudioSessionId() {
        return 1; // not really used
    }

}


