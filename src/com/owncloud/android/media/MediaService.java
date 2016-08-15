/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2016 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import java.io.IOException;


/**
 * Service that handles media playback, both audio and video. 
 * 
 * Waits for Intents which signal the service to perform specific operations: Play, Pause,
 * Rewind, etc.
 */
public class MediaService extends Service implements OnCompletionListener, OnPreparedListener,
                OnErrorListener, AudioManager.OnAudioFocusChangeListener {

    private static final String TAG = MediaService.class.getSimpleName();

    private static final String MY_PACKAGE = MediaService.class.getPackage() != null ?
            MediaService.class.getPackage().getName() : "com.owncloud.android.media";
    
    /// Intent actions that we are prepared to handle
    public static final String ACTION_PLAY_FILE = MY_PACKAGE + ".action.PLAY_FILE";
    public static final String ACTION_STOP_ALL = MY_PACKAGE + ".action.STOP_ALL";

    /// Keys to add extras to the action
    public static final String EXTRA_FILE = MY_PACKAGE + ".extra.FILE";
    public static final String EXTRA_ACCOUNT = MY_PACKAGE + ".extra.ACCOUNT";
    public static String EXTRA_START_POSITION = MY_PACKAGE + ".extra.START_POSITION";
    public static final String EXTRA_PLAY_ON_LOAD = MY_PACKAGE + ".extra.PLAY_ON_LOAD";


    /** Error code for specific messages - see regular error codes at {@link MediaPlayer} */
    public static final int OC_MEDIA_ERROR = 0;

    /** Time To keep the control panel visible when the user does not use it */
    public static final int MEDIA_CONTROL_SHORT_LIFE = 4000;
    
    /** Time To keep the control panel visible when the user does not use it */
    public static final int MEDIA_CONTROL_PERMANENT = 0;
    
    /** Volume to set when audio focus is lost and ducking is allowed */
    private static final float DUCK_VOLUME = 0.1f;

    /** Media player instance */
    private MediaPlayer mPlayer = null;
    
    /** Reference to the system AudioManager */
    private AudioManager mAudioManager = null;

    
    /** Values to indicate the state of the service */
    enum State {
        STOPPED,
        PREPARING,      
        PLAYING,        
        PAUSED 
    };
    

    /** Current state */
    private State mState = State.STOPPED;
    
    /** Possible focus values */
    enum AudioFocus {
        NO_FOCUS,            
        NO_FOCUS_CAN_DUCK,   
        FOCUS           
    }
    
    /** Current focus state */
    private AudioFocus mAudioFocus = AudioFocus.NO_FOCUS;
    

    /** 'True' when the current song is streaming from the network */
    private boolean mIsStreaming = false;

    /** Wifi lock kept to prevents the device from shutting off the radio when streaming a file. */
    private WifiLock mWifiLock;
    
    private static final String MEDIA_WIFI_LOCK_TAG = MY_PACKAGE + ".WIFI_LOCK";

    /** Notification to keep in the notification bar while a song is playing */
    private NotificationManager mNotificationManager;

    /** File being played */
    private OCFile mFile;
    
    /** Account holding the file being played */
    private Account mAccount;

    /** Flag signaling if the audio should be played immediately when the file is prepared */ 
    protected boolean mPlayOnPrepared;

    /** Position, in miliseconds, where the audio should be started */
    private int mStartPosition;
    
    /** Interface to access the service through binding */
    private IBinder mBinder;

    /** Control panel shown to the user to control the playback, to register through binding */
    private MediaControlView mMediaController;

    /** Notification builder to create notifications, new reuse way since Android 6 */
    private NotificationCompat.Builder mNotificationBuilder;
    
    /**
     * Helper method to get an error message suitable to show to users for errors occurred in media playback,
     * 
     * @param context   A context to access string resources.
     * @param what      See {@link MediaPlayer.OnErrorListener#onError(MediaPlayer, int, int)
     * @param extra     See {@link MediaPlayer.OnErrorListener#onError(MediaPlayer, int, int)
     * @return          Message suitable to users.
     */
    public static String getMessageForMediaError(Context context, int what, int extra) {
        int messageId;
        
        if (what == OC_MEDIA_ERROR) {
            messageId = extra;
                
        } else if (extra == MediaPlayer.MEDIA_ERROR_UNSUPPORTED) {
            /*  Added in API level 17
                Bitstream is conforming to the related coding standard or file spec,
                but the media framework does not support the feature.
                Constant Value: -1010 (0xfffffc0e)
             */
            messageId = R.string.media_err_unsupported;

        } else if (extra == MediaPlayer.MEDIA_ERROR_IO) {
            /*  Added in API level 17
                File or network related operation errors.
                Constant Value: -1004 (0xfffffc14) 
             */
            messageId = R.string.media_err_io;
            
        } else if (extra == MediaPlayer.MEDIA_ERROR_MALFORMED) {
            /*  Added in API level 17
                Bitstream is not conforming to the related coding standard or file spec.
                Constant Value: -1007 (0xfffffc11) 
             */
            messageId = R.string.media_err_malformed;
            
        } else if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
            /*  Added in API level 17
                Some operation takes too long to complete, usually more than 3-5 seconds.
                Constant Value: -110 (0xffffff92)
            */
            messageId = R.string.media_err_timeout;

        } else if (what == MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK) {
            /*  Added in API level 3
                The video is streamed and its container is not valid for progressive playback i.e the video's index
                (e.g moov atom) is not at the start of the file.
                Constant Value: 200 (0x000000c8)
            */
            messageId = R.string.media_err_invalid_progressive_playback;
                
        } else {
            /*  MediaPlayer.MEDIA_ERROR_UNKNOWN
                Added in API level 1
                Unspecified media player error.
                Constant Value: 1 (0x00000001)
            */
            /*  MediaPlayer.MEDIA_ERROR_SERVER_DIED)
                Added in API level 1
                Media server died. In this case, the application must release the MediaPlayer
                object and instantiate a new one.
                Constant Value: 100 (0x00000064) 
             */
            messageId = R.string.media_err_unknown;
        }
        return context.getString(messageId);
    }

    public static AlertDialog.Builder streamWithExternalApp(final Uri uri, final Activity activity){
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(activity.getString(R.string.stream_expose_password))
                .setPositiveButton(activity.getString(R.string.common_yes),
                                   new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            Intent i = new Intent(Intent.ACTION_VIEW);
                                            i.setData(uri);
                                            activity.startActivity(i);
                                        }
                                    })
                .setNegativeButton(activity.getString(R.string.common_no), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                });
        return builder;
    }


    
    /**
     * Initialize a service instance
     * 
     * {@inheritDoc}
     */
    @Override
    public void onCreate() {
        super.onCreate();
        Log_OC.d(TAG, "Creating ownCloud media service");

        mWifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).
                createWifiLock(WifiManager.WIFI_MODE_FULL, MEDIA_WIFI_LOCK_TAG);

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationBuilder = new NotificationCompat.Builder(this);
        mNotificationBuilder.setColor(this.getResources().getColor(R.color.primary));
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        mBinder = new MediaServiceBinder(this);
    }

    
    /**
     * Entry point for Intents requesting actions, sent here via startService.
     * 
     * {@inheritDoc}
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (action.equals(ACTION_PLAY_FILE)) { 
            processPlayFileRequest(intent);
            
        } else if (action.equals(ACTION_STOP_ALL)) {
            processStopRequest(true);
        }

        return START_NOT_STICKY; // don't want it to restart in case it's killed.
    }


    /**
     * Processes a request to play a media file received as a parameter
     * 
     * TODO If a new request is received when a file is being prepared, it is ignored. Is this what we want? 
     * 
     * @param intent    Intent received in the request with the data to identify the file to play. 
     */
    private void processPlayFileRequest(Intent intent) {
        if (mState != State.PREPARING) {
            mFile = intent.getExtras().getParcelable(EXTRA_FILE);
            mAccount = intent.getExtras().getParcelable(EXTRA_ACCOUNT);
            mPlayOnPrepared = intent.getExtras().getBoolean(EXTRA_PLAY_ON_LOAD, false);
            mStartPosition = intent.getExtras().getInt(EXTRA_START_POSITION, 0);
            tryToGetAudioFocus();
            playMedia();
        }
    }

    
    /**
     * Processes a request to play a media file.
     */
    protected void processPlayRequest() {
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
     * if needed. reset the existing media player if one already exists.
     */
    protected void createMediaPlayerIfNeeded() {
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
     * Processes a request to pause the current playback 
     */
    protected void processPauseRequest() {
        if (mState == State.PLAYING) {
            mState = State.PAUSED;
            mPlayer.pause();
            releaseResources(false); // retain media player in pause
            // TODO polite audio focus, instead of keep it owned; or not?
        }
    }
    
    
    /**
     * Processes a request to stop the playback.
     * 
     * @param   force       When 'true', the playback is stopped no matter the value of mState
     */
    protected void processStopRequest(boolean force) {
        if (mState != State.PREPARING || force) {
            mState = State.STOPPED;
            mFile = null;
            mAccount = null;
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
    protected void releaseResources(boolean releaseMediaPlayer) {
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
    protected void configAndStartMediaPlayer() {
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

    
    /**
     * Starts playing the current media file. 
     */
    protected void playMedia() {
        mState = State.STOPPED;
        releaseResources(false); // release everything except MediaPlayer

        try {
            if (mAccount == null) {
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

            mIsStreaming = false;
            
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
            Log_OC.e(TAG, "SecurityException playing " + mAccount.name + mFile.getRemotePath(), e);
            Toast.makeText(this, String.format(getString(R.string.media_err_security_ex), mFile.getFileName()),
                    Toast.LENGTH_LONG).show();
            processStopRequest(true);
            
        } catch (IOException e) {
            Log_OC.e(TAG, "IOException playing " + mAccount.name + mFile.getRemotePath(), e);
            Toast.makeText(this, String.format(getString(R.string.media_err_io_ex), mFile.getFileName()),
                    Toast.LENGTH_LONG).show();
            processStopRequest(true);
            
        } catch (IllegalStateException e) {
            Log_OC.e(TAG, "IllegalStateException " + mAccount.name + mFile.getRemotePath(), e);
            Toast.makeText(this, String.format(getString(R.string.media_err_unexpected), mFile.getFileName()),
                    Toast.LENGTH_LONG).show();
            processStopRequest(true);
            
        } catch (IllegalArgumentException e) {
            Log_OC.e(TAG, "IllegalArgumentException " + mAccount.name + mFile.getRemotePath(), e);
            Toast.makeText(this, String.format(getString(R.string.media_err_unexpected), mFile.getFileName()),
                    Toast.LENGTH_LONG).show();
            processStopRequest(true);
        } catch (AccountUtils.AccountNotFoundException e) {
            e.printStackTrace();
        }
    }

    
    /** Called when media player is done playing current song. */
    public void onCompletion(MediaPlayer player) {
        Toast.makeText(this, String.format(getString(R.string.media_event_done, mFile.getFileName())), Toast.LENGTH_LONG).show();
        if (mMediaController != null) {
            // somebody is still bound to the service
            player.seekTo(0);
            processPauseRequest();
            mMediaController.updatePausePlay();
        } else {
            // nobody is bound
            processStopRequest(true);
        }
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
        if (mMediaController != null) {
            mMediaController.setEnabled(true);
        }
        player.seekTo(mStartPosition);
        configAndStartMediaPlayer();
        if (!mPlayOnPrepared) {
            processPauseRequest();
        }
        
        if (mMediaController != null) {
            mMediaController.updatePausePlay();
        }
    }
    

    /** 
     * Updates the status notification
     */
    private void updateNotification(String content) {
        String ticker = String.format(getString(R.string.media_notif_ticker), getString(R.string.app_name));

        // TODO check if updating the Intent is really necessary
        Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, mFile);
        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, mAccount);
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(getApplicationContext(),
                (int) System.currentTimeMillis(),
                showDetailsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT));
        mNotificationBuilder.setWhen(System.currentTimeMillis());
        mNotificationBuilder.setTicker(ticker);
        mNotificationBuilder.setContentTitle(ticker);
        mNotificationBuilder.setContentText(content);

        mNotificationManager.notify(R.string.media_notif_ticker, mNotificationBuilder.build());
    }

    
    /**
     * Configures the service as a foreground service.
     * 
     * The system will avoid finishing the service as much as possible when resources as low.
     * 
     * A notification must be created to keep the user aware of the existance of the service.
     */
    private void setUpAsForeground(String content) {
        String ticker = String.format(getString(R.string.media_notif_ticker), getString(R.string.app_name));

        /// creates status notification
        // TODO put a progress bar to follow the playback progress
        mNotificationBuilder.setSmallIcon(R.drawable.ic_play_arrow);
        //mNotification.tickerText = text;
        mNotificationBuilder.setWhen(System.currentTimeMillis());
        mNotificationBuilder.setOngoing(true);
        
        /// includes a pending intent in the notification showing the details view of the file
        Intent showDetailsIntent = new Intent(this, FileDisplayActivity.class);
        showDetailsIntent.putExtra(FileActivity.EXTRA_FILE, mFile);
        showDetailsIntent.putExtra(FileActivity.EXTRA_ACCOUNT, mAccount);
        showDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        mNotificationBuilder.setContentIntent(PendingIntent.getActivity(getApplicationContext(),
                (int) System.currentTimeMillis(),
                showDetailsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT));
        mNotificationBuilder.setContentTitle(ticker);
        mNotificationBuilder.setContentText(content);

        startForeground(R.string.media_notif_ticker, mNotificationBuilder.build());
    }

    /**
     * Called when there's an error playing media. 
     * 
     * Warns the user about the error and resets the media player.
     */
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log_OC.e(TAG, "Error in audio playback, what = " + what + ", extra = " + extra);
        
        String message = getMessageForMediaError(this, what, extra);
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        
        processStopRequest(true);
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
            mAudioFocus = AudioFocus.FOCUS;
            // restart media player with new focus settings
            if (mState == State.PLAYING)
                configAndStartMediaPlayer();
            
        } else if (focusChange < 0) {
            // focus loss; check AudioManager.AUDIOFOCUS_* values
            boolean canDuck = AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK == focusChange;
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
        stopForeground(true);
        super.onDestroy();
    }
    

    /**
     * Provides a binder object that clients can use to perform operations on the MediaPlayer managed by the MediaService. 
     */
    @Override
    public IBinder onBind(Intent arg) {
        return mBinder;
    }
    
    
    /**
     * Called when ALL the bound clients were onbound.
     * 
     * The service is destroyed if playback stopped or paused
     */
    @Override
    public boolean onUnbind(Intent intent) {
        if (mState == State.PAUSED || mState == State.STOPPED)  {
            processStopRequest(false);
        }
        return false;   // not accepting rebinding (default behaviour)
    }


    /**
     * Accesses the current MediaPlayer instance in the service.
     * 
     * To be handled carefully. Visibility is protected to be accessed only 
     * 
     * @return Current MediaPlayer instance handled by MediaService.
     */
    protected MediaPlayer getPlayer() {
        return mPlayer;
    }


    /**
     * Accesses the current OCFile loaded in the service.
     * 
     * @return  The current OCFile loaded in the service.
     */
    protected OCFile getCurrentFile() {
        return mFile;
    }

    
    /**
     * Accesses the current {@link State} of the MediaService.
     * 
     * @return  The current {@link State} of the MediaService.
     */
    protected State getState() {
        return mState;
    }


    protected void setMediaContoller(MediaControlView mediaController) {
        mMediaController = mediaController;
    }

    protected MediaControlView getMediaController() {
        return mMediaController;
    }

}
