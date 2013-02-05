/* ownCloud Android client application
 *   Copyright 2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.media;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.ui.activity.FileDetailActivity;
import com.owncloud.android.ui.fragment.FileDetailFragment;

/**
 * Service that handles media playback, both audio and video. 
 * 
 * Waits for Intents which signal the service to perform specific operations: Play, Pause,
 * Rewind, etc.
 * 
 * @author David A. Velasco
 */
public class MediaService extends Service implements OnCompletionListener, OnPreparedListener,
                OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = MediaService.class.getSimpleName();

    private static final String MY_PACKAGE = MediaService.class.getPackage() != null ? MediaService.class.getPackage().getName() : "com.owncloud.android.media";
    
    /// Intent actions that we are prepared to handle
    public static final String ACTION_TOGGLE_PLAYBACK = MY_PACKAGE + ".action.TOGGLE_PLAYBACK";
    public static final String ACTION_PLAY = MY_PACKAGE + ".action.PLAY";
    public static final String ACTION_PAUSE = MY_PACKAGE + ".android.media.action.PAUSE";
    public static final String ACTION_STOP = MY_PACKAGE + ".android.media.action.STOP";
    public static final String ACTION_REWIND = MY_PACKAGE + ".android.media.action.REWIND";
    public static final String ACTION_PLAY_FILE = MY_PACKAGE + ".android.media.action.URL";

    /// Keys to add extras to the action
    public static final String EXTRA_FILE = MY_PACKAGE + ".extra.FILE";
    public static final String EXTRA_ACCOUNT = MY_PACKAGE + ".extra.ACCOUNT";
    
    /**
     * Volume to set when audio focus is lost and ducking is allowed
     */
    private static final float DUCK_VOLUME = 0.1f;

    /**
     * Media player instance
     */
    private MediaPlayer mPlayer = null;

    
    // our AudioFocusHelper object, if it's available (it's available on SDK level >= 8)
    // If not available, this will be null. Always check for null before using!
    //AudioFocusHelper mAudioFocusHelper = null;
    /**
     * Reference to the system AudioManager
     */
    private AudioManager mAudioManager = null;

    
    /**
     * Values to indicate the state of the service
     */
    enum State {
        STOPPED,
        PREPARING,      
        PLAYING,        
        PAUSED 
    };
    

    /**
     * Current state
     */
    private State mState = State.STOPPED;
    
    
    enum PauseReason {
        UserRequest,  // paused by user request
        FocusLoss,    // paused because of audio focus loss
    };

    
    /**
     * Possible focus values
     */
    enum AudioFocus {
        NO_FOCUS,            
        NO_FOCUS_CAN_DUCK,   
        FOCUS           
    }
    
    /**
     * Current focus state
     */
    private AudioFocus mAudioFocus = AudioFocus.NO_FOCUS;
    

    /**
     * 'True' when the current song is streaming from the network
     */
    private boolean mIsStreaming = false;

    /**
     * Wifi lock kept to prevents the device from shutting off the radio when streaming a file.
     */
    private WifiLock mWifiLock;
    private static final String MEDIA_WIFI_LOCK_TAG = MY_PACKAGE + ".WIFI_LOCK";



    /**
     * Id for the notification to keep in the notification bar while a song is playing
     */
    private final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    private Notification mNotification = null;
    
    private OCFile mFile;
    private Account mAccount;
    
    

    /**
     * Initialize a service instance
     * 
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        Log.d(TAG, "Creating ownCloud media service");

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).
                createWifiLock(WifiManager.WIFI_MODE_FULL, MEDIA_WIFI_LOCK_TAG);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

    }

    
    /**
     * Entry point for Intents requesting actions, sent here via startService.
     * 
     * TODO maybe, replace by an API based in binding
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_PLAY_FILE)) { 
            processPlayFileRequest(intent);
        } else if (action.equals(ACTION_PLAY)) { 
            processPlayRequest();
        } else if (action.equals(ACTION_TOGGLE_PLAYBACK))  { 
            processTogglePlaybackRequest();
        } else if (action.equals(ACTION_PAUSE)) { 
            processPauseRequest();
        } else if (action.equals(ACTION_STOP)) { 
            processStopRequest();
        } else if (action.equals(ACTION_REWIND)) { 
            processRewindRequest();
        }

        return START_NOT_STICKY; // don't want it to restart in case it's killed.
    }


    /**
     * Processes a request to play a media file received as a parameter
     * 
     * @param intent    Intent received in the request with the data to identify the file to play. 
     */
    private void processPlayFileRequest(Intent intent) {
        if (mState == State.PLAYING || mState == State.PAUSED || mState == State.STOPPED) {
            mFile = intent.getExtras().getParcelable(EXTRA_FILE);
            mAccount = intent.getExtras().getParcelable(EXTRA_ACCOUNT);
            tryToGetAudioFocus();
            playMedia();
        }
        // TODO think what happens if mState == State.PREPARING
    }

    
    /**
     * Processes a request to play a media file.
     */
    void processPlayRequest() {
        // request audio focus
        tryToGetAudioFocus();

        // actually play the song
        if (mState == State.STOPPED) {
            // (re)start playback
            playMedia();
            
        } else if (mState == State.PAUSED) {
            // continue playback
            mState = State.PLAYING;
            setUpAsForeground(String.format(getString(R.string.media_state_playing), mFile.getFileName()));
            configAndStartMediaPlayer();
            
        }
    }

    
    /**
     * Makes sure the media player exists and has been reset. This will create the media player
     * if needed, or reset the existing media player if one already exists.
     */
    void createMediaPlayerIfNeeded() {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();

            // make sure the CPU won't go to sleep while media is playing
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);

            // the media player will notify the service when it's ready preparing, and when it's done playing
            mPlayer.setOnPreparedListener(this);
            mPlayer.setOnCompletionListener(this);
            mPlayer.setOnErrorListener(this);
            
        } else {
            mPlayer.reset();
        }
    }

    /**
     * Processes a request to toggle from PLAY to PAUSE, or from PAUSE to PLAY
     */
    private void processTogglePlaybackRequest() {
        if (mState == State.PAUSED || mState == State.STOPPED) {
            processPlayRequest();
            
        } else {
            processPauseRequest();
        }
    }

    /**
     * Processes a request to pause the current playback 
     */
    private void processPauseRequest() {
        if (mState == State.PLAYING) {
            mState = State.PAUSED;
            mPlayer.pause();
            releaseResources(false); // retain media player in pause
            // TODO polite audio focus, instead of keep it owned; or not?
        }
    }
    
    
    /**
     * Process a request to rewind the current media playback to the start point.
     */
    private void processRewindRequest() {
        if (mState == State.PLAYING || mState == State.PAUSED) {
            mPlayer.seekTo(0);
        }
    }

    /**
     * Processes a request to stop the playback
     */
    private void processStopRequest() {
        processStopRequest(false);
    }

    
    /**
     * Processes a request to stop the playback.
     * 
     * @param   force       When 'true', the playback is stopped no matter the value of mState
     */
    void processStopRequest(boolean force) {
        if (mState == State.PLAYING || mState == State.PAUSED || force) {
            mState = State.STOPPED;

            releaseResources(true);
            giveUpAudioFocus();
            stopSelf();     // service is no longer necessary
        }
    }
    

    /**
     * Releases resources used by the service for playback. This includes the "foreground service"
     * status and notification, the wake locks and possibly the MediaPlayer.
     *
     * @param releaseMediaPlayer    Indicates whether the Media Player should also be released or not
     */
    void releaseResources(boolean releaseMediaPlayer) {
        // stop being a foreground service
        stopForeground(true);

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mPlayer != null) {
            mPlayer.reset();
            mPlayer.release();
            mPlayer = null;
        }

        // release the Wifi lock, if holding it
        if (mWifiLock.isHeld()) {
            mWifiLock.release();
        }
    }

    
    /**
     * Fully releases the audio focus.
     */
    private void giveUpAudioFocus() {
        if (mAudioFocus == AudioFocus.FOCUS 
                && mAudioManager != null  
                && AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.abandonAudioFocus(this))  {
            
            mAudioFocus = AudioFocus.NO_FOCUS;
        }
    }

    
    /**
     * Reconfigures MediaPlayer according to audio focus settings and starts/restarts it. 
     */
    void configAndStartMediaPlayer() {
        if (mPlayer == null) {
            throw new IllegalStateException("mPlayer is NULL");
        }
        
        if (mAudioFocus == AudioFocus.NO_FOCUS) {
            if (mPlayer.isPlaying()) {
                mPlayer.pause();        // have to be polite; but mState is not changed, to resume when focus is received again
            }
            
        }  else { 
            if (mAudioFocus == AudioFocus.NO_FOCUS_CAN_DUCK) {
                mPlayer.setVolume(DUCK_VOLUME, DUCK_VOLUME);
                
            } else {
                mPlayer.setVolume(1.0f, 1.0f); // full volume
            }
    
            if (!mPlayer.isPlaying()) {
                mPlayer.start();
            }
        }
    }

    
    /**
     * Requests the audio focus to the Audio Manager 
     */
    private void tryToGetAudioFocus() {
        if (mAudioFocus != AudioFocus.FOCUS 
                && mAudioManager != null 
                && (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAudioManager.requestAudioFocus( this,
                                                                                                AudioManager.STREAM_MUSIC, 
                                                                                                AudioManager.AUDIOFOCUS_GAIN))
                ) {
            mAudioFocus = AudioFocus.FOCUS;
        }
    }

    
    public static class Item {
        long id;
        String artist;
        String title;
        String album;
        long duration;

        public Item(long id, String artist, String title, String album, long duration) {
            this.id = id;
            this.artist = artist;
            this.title = title;
            this.album = album;
            this.duration = duration;
        }

        public long getId() {
            return id;
        }

        public String getArtist() {
            return artist;
        }

        public String getTitle() {
            return title;
        }

        public String getAlbum() {
            return album;
        }

        public long getDuration() {
            return duration;
        }

        public Uri getURI() {
            return ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
        }
    }
    
    
    
    /**
     * Starts playing the current media file. 
     */
    void playMedia() {
        mState = State.STOPPED;
        releaseResources(false); // release everything except MediaPlayer

        try {
            if (mFile == null) { 
                Toast.makeText(this, R.string.media_err_nothing_to_play, Toast.LENGTH_LONG).show();
                processStopRequest(true);
                return;
                
            } else if (mAccount == null) {
                Toast.makeText(this, R.string.media_err_not_in_owncloud, Toast.LENGTH_LONG).show();
                processStopRequest(true);
                return;
            }

            createMediaPlayerIfNeeded();
            mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            String url = mFile.getStoragePath();
            if (url == null || url.length() <= 0) {
                url = AccountUtils.constructFullURLForAccount(this, mAccount) + mFile.getRemotePath();
            }
            mIsStreaming = url.startsWith("http:") || url.startsWith("https:");
            
            mPlayer.setDataSource(url);

            mState = State.PREPARING;
            setUpAsForeground(String.format(getString(R.string.media_state_loading), mFile.getFileName()));

            // starts preparing the media player in background
            mPlayer.prepareAsync();

            // prevent the Wifi from going to sleep when streaming
            if (mIsStreaming) { 
                mWifiLock.acquire();
            } else if (mWifiLock.isHeld()) {
                mWifiLock.release();
            }
            
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException playing " + mAccount.name + mFile.getRemotePath(), e);
            // TODO message to the user
            
        } catch (IOException e) {
            Log.e(TAG, "IOException playing " + mAccount.name + mFile.getRemotePath(), e);
            // TODO message to the user
            
        } catch (IllegalStateException e) {
            Log.e(TAG, "IllegalStateException " + mAccount.name + mFile.getRemotePath(), e);
            
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "IllegalArgumentException " + mAccount.name + mFile.getRemotePath(), e);
            e.printStackTrace();
        }
    }

    
    /** Called when media player is done playing current song. */
    public void onCompletion(MediaPlayer player) {
        Toast.makeText(this, String.format(getString(R.string.media_event_done, mFile.getFileName())), Toast.LENGTH_LONG).show();
        processStopRequest(true);
        return;
    }
    

    /** 
     * Called when media player is done preparing. 
     *
     * Time to start.
     */
    public void onPrepared(MediaPlayer player) {
        mState = State.PLAYING;
        updateNotification(String.format(getString(R.string.media_state_playing), mFile.getFileName()));
        configAndStartMediaPlayer();
    }
    

    /** 
     * Updates the status notification
     */
    @SuppressWarnings("deprecation")
    private void updateNotification(String content) {
        // TODO check if updating the Intent is really necessary
        Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, mFile);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, mAccount);
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 
                                                                (int)System.currentTimeMillis(), 
                                                                showDetailsIntent, 
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
        mNotification.when = System.currentTimeMillis();
        //mNotification.contentView.setTextViewText(R.id.status_text, content);
        String ticker = "ownCloud MusicPlayer";
        mNotification.setLatestEventInfo(getApplicationContext(), ticker, content, mNotification.contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }

    
    /**
     * Configures the service as a foreground service.
     * 
     * The system will avoid finishing the service as much as possible when resources as low.
     * 
     * A notification must be created to keep the user aware of the existance of the service.
     */
    @SuppressWarnings("deprecation")
    private void setUpAsForeground(String content) {
        /// creates status notification
        // TODO put a progress bar to follow the playback progress
        mNotification = new Notification();
        mNotification.icon = android.R.drawable.ic_media_play;
        //mNotification.tickerText = text;
        mNotification.when = System.currentTimeMillis();
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        //mNotification.contentView.setTextViewText(R.id.status_text, "ownCloud Music Player");     // NULL POINTER
        //mNotification.contentView.setTextViewText(R.id.status_text, getString(R.string.downloader_download_in_progress_content));

        
        /// includes a pending intent in the notification showing the details view of the file
        Intent showDetailsIntent = new Intent(this, FileDetailActivity.class);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_FILE, mFile);
        showDetailsIntent.putExtra(FileDetailFragment.EXTRA_ACCOUNT, mAccount);
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 
                                                                (int)System.currentTimeMillis(), 
                                                                showDetailsIntent, 
                                                                PendingIntent.FLAG_UPDATE_CURRENT);
        
        
        //mNotificationManager.notify(R.string.downloader_download_in_progress_ticker, mNotification);
        String ticker = "ownCloud MusicPlayer";
        mNotification.setLatestEventInfo(getApplicationContext(), ticker, content, mNotification.contentIntent);
        startForeground(NOTIFICATION_ID, mNotification);
        
    }

    /**
     * Called when there's an error playing media. 
     * 
     * Warns the user about the error and resets the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        // TODO FOLLOW HERE!!!!!!
        
        Toast.makeText(getApplicationContext(), "Media player error! Resetting.",
            Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Error: what=" + String.valueOf(what) + ", extra=" + String.valueOf(extra));

        mState = State.STOPPED;
        releaseResources(true);
        giveUpAudioFocus();
        return true; 
    }

    
    /**
     * Called by the system when another app tries to play some sound.
     * 
     * {@inheritDoc}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange > 0) {
            // focus gain; check AudioManager.AUDIOFOCUS_* values
            Toast.makeText(getApplicationContext(), "gained audio focus.", Toast.LENGTH_SHORT).show();
            mAudioFocus = AudioFocus.FOCUS;

            // restart media player with new focus settings
            if (mState == State.PLAYING)
                configAndStartMediaPlayer();
            
        } else if (focusChange < 0) {
            // focus loss; check AudioManager.AUDIOFOCUS_* values
            boolean canDuck = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK == focusChange;
            Toast.makeText(getApplicationContext(), "lost audio focus." + (canDuck ? "can duck" :
                    "no duck"), Toast.LENGTH_SHORT).show();
                mAudioFocus = canDuck ? AudioFocus.NO_FOCUS_CAN_DUCK : AudioFocus.NO_FOCUS;

                // start/restart/pause media player with new focus settings
                if (mPlayer != null && mPlayer.isPlaying())
                    configAndStartMediaPlayer();
        }
        
    }

    /**
     * Called when the service is finished for final clean-up.
     * 
     * {@inheritDoc}
     */
    @Override
    public void onDestroy() {
        mState = State.STOPPED;
        releaseResources(true);
        giveUpAudioFocus();
    }
    

    @Override
    public IBinder onBind(Intent arg0) {
        // TODO provide a binding API? may we use a service to play VIDEO?
        return null;
    }

}
