package com.owncloud.android.ui.activity;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.VideoView;

import com.owncloud.android.R;

public class VideoActivity extends Activity implements OnCompletionListener, OnPreparedListener {
      
   public static final String EXTRA_PATH = "PATH";
   
   private VideoView mVideoPlayer;
   private String mPathToFile;
      
   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.video_layout);

      mPathToFile = getIntent().getExtras().getString(EXTRA_PATH);
      
      mVideoPlayer = (VideoView) findViewById(R.id.videoPlayer);   
      mVideoPlayer.setOnPreparedListener(this);
      mVideoPlayer.setOnCompletionListener(this);
      mVideoPlayer.setKeepScreenOn(true);    
      mVideoPlayer.setVideoPath(mPathToFile);
   }

   /** This callback will be invoked when the file is ready to play */
   @Override
   public void onPrepared(MediaPlayer vp) {
      mVideoPlayer.start();
   }
   
   /** This callback will be invoked when the file is finished playing */
   @Override
   public void onCompletion(MediaPlayer  mp) {
      this.finish(); 
   }
   
   /**  Use screen touches to toggle the video between playing and paused. */
   @Override
   public boolean onTouchEvent (MotionEvent ev){ 
      if(ev.getAction() == MotionEvent.ACTION_DOWN){
         if(mVideoPlayer.isPlaying()){
                  mVideoPlayer.pause();
         } else {
                  mVideoPlayer.start();
         }
         return true;        
      } else {
         return false;
      }
   }
}