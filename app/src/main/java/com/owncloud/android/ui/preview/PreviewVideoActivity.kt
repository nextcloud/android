/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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
package com.owncloud.android.ui.preview

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.media.ExoplayerListener
import com.nextcloud.client.media.NextcloudExoPlayer.createNextcloudExoplayer
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityPreviewVideoBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.MimeTypeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity implementing a basic video player.
 *
 */
@Suppress("TooManyFunctions")
class PreviewVideoActivity :
    FileActivity(),
    Player.Listener,
    Injectable {

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var accountManager: UserAccountManager

    private var mSavedPlaybackPosition: Long = -1 // in the unit time handled by MediaPlayer.getCurrentPosition()
    private var mAutoplay = false // when 'true', the playback starts immediately with the activity
    private var exoPlayer: ExoPlayer? = null // view to play the file; both performs and show the playback
    private var mStreamUri: Uri? = null
    private lateinit var binding: ActivityPreviewVideoBinding

    private lateinit var pauseButton: View

    private lateinit var playButton: View

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log_OC.v(TAG, "onCreate")

        binding = ActivityPreviewVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val extras = intent.extras

        if (savedInstanceState == null && extras != null) {
            mSavedPlaybackPosition = extras.getLong(EXTRA_START_POSITION)
            mAutoplay = extras.getBoolean(EXTRA_AUTOPLAY)
            mStreamUri = extras[EXTRA_STREAM_URL] as Uri?
        } else if (savedInstanceState != null) {
            mSavedPlaybackPosition = savedInstanceState.getLong(EXTRA_START_POSITION)
            mAutoplay = savedInstanceState.getBoolean(EXTRA_AUTOPLAY)
            mStreamUri = savedInstanceState[EXTRA_STREAM_URL] as Uri?
        }

        supportActionBar?.hide()
    }

    private fun setupPlayerView() {
        binding.videoPlayer.player = exoPlayer
        exoPlayer!!.addListener(this)

        binding.root.findViewById<View>(R.id.exo_exit_fs).setOnClickListener { onBackPressed() }

        pauseButton = binding.root.findViewById(R.id.exo_pause)
        pauseButton.setOnClickListener { exoPlayer!!.pause() }

        playButton = binding.root.findViewById(R.id.exo_play)
        playButton.setOnClickListener { exoPlayer!!.play() }

        if (mSavedPlaybackPosition >= 0) {
            exoPlayer?.seekTo(mSavedPlaybackPosition)
        }

        onIsPlayingChanged(exoPlayer!!.isPlaying)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        if (isPlaying) {
            playButton.visibility = View.GONE
            pauseButton.visibility = View.VISIBLE
        } else {
            playButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
        }
    }

    /**
     * {@inheritDoc}
     */
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(EXTRA_START_POSITION, currentPosition())
        outState.putBoolean(EXTRA_AUTOPLAY, isPlaying())
        outState.putParcelable(EXTRA_STREAM_URL, mStreamUri)
    }

    override fun onBackPressed() {
        Log_OC.v(TAG, "onBackPressed")
        val i = Intent()
        i.putExtra(EXTRA_AUTOPLAY, isPlaying())
        i.putExtra(EXTRA_START_POSITION, currentPosition())
        setResult(RESULT_OK, i)
        exoPlayer?.stop()
        exoPlayer?.release()
        super.onBackPressed()
    }

    private fun isPlaying() = exoPlayer?.isPlaying ?: false
    private fun currentPosition() = exoPlayer?.currentPosition ?: 0

    private fun play(item: MediaItem) {
        exoPlayer?.addMediaItem(item)
        exoPlayer?.prepare()

        if (mAutoplay) {
            exoPlayer?.play()
        }
    }

    override fun onStart() {
        super.onStart()
        if (account != null) {
            require(file != null) { throw IllegalStateException("Instanced with a NULL OCFile") }
            var fileToPlay: OCFile? = file

            // Validate handled file  (first image to preview)
            require(MimeTypeUtil.isVideo(fileToPlay)) { "Non-video file passed as argument" }

            fileToPlay = storageManager.getFileById(fileToPlay!!.fileId)
            if (fileToPlay != null) {
                val mediaItem = when {
                    fileToPlay.isDown -> MediaItem.fromUri(fileToPlay.storageUri)
                    else -> MediaItem.fromUri(mStreamUri!!)
                }
                if (exoPlayer != null) {
                    setupPlayerView()
                    play(mediaItem)
                } else {
                    val context = this
                    CoroutineScope(Dispatchers.IO).launch {
                        val client = clientFactory.createNextcloudClient(accountManager.user)
                        CoroutineScope(Dispatchers.Main).launch {
                            exoPlayer = createNextcloudExoplayer(context, client).also {
                                it.addListener(ExoplayerListener(context, it))
                            }
                            setupPlayerView()
                            play(mediaItem)
                        }
                    }
                }
            } else {
                finish()
            }
        } else {
            finish()
        }
    }

    override fun onStop() {
        super.onStop()
        if (exoPlayer?.isPlaying == true) {
            exoPlayer?.pause()
        }
    }

    companion object {
        /** Key to receive a flag signaling if the video should be started immediately  */
        const val EXTRA_AUTOPLAY = "AUTOPLAY"

        /** Key to receive the position of the playback where the video should be put at start  */
        const val EXTRA_START_POSITION = "START_POSITION"
        const val EXTRA_STREAM_URL = "STREAM_URL"
        private val TAG = PreviewVideoActivity::class.java.simpleName
    }
}
