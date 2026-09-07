/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.client.player.model.file.PlaybackFileType
import com.nextcloud.client.player.ui.audio.AudioPlayerView
import com.nextcloud.client.player.ui.video.VideoPlayerView
import com.nextcloud.ui.fileactions.FileAction
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class PlayerActivity :
    FileActivity(),
    Injectable {

    companion object {
        private const val PLAYBACK_FILE_TYPE: String = "PLAYBACK_FILE_TYPE"

        fun createIntent(context: Context, playbackFileType: PlaybackFileType): Intent =
            Intent(context, PlayerActivity::class.java).apply {
                putExtra(PLAYBACK_FILE_TYPE, playbackFileType)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val viewModel by viewModels<PlayerViewModel> { viewModelFactory }

    private lateinit var playbackFileType: PlaybackFileType

    private lateinit var playerView: PlayerView

    private val pictureInPicture by lazy { VideoPictureInPicture(this, playbackModel, autoEnter = true) }

    private var onBackPressedCallback: OnBackPressedCallback? = null

    private var keepPlaybackAliveOnFinish = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets -> windowInsets }

        playbackFileType = intent.getPlaybackFileType()
        createPlayerView()

        viewModel.eventFlow
            .flowWithLifecycle(lifecycle)
            .onEach { handleEvent(it) }
            .launchIn(lifecycleScope)

        onBackPressedCallback = onBackPressedDispatcher.addCallback(this) {
            if (canUsePictureInPictureMode() && pictureInPicture.enter(playerView)) {
                return@addCallback
            }

            file = file?.parentId?.let { storageManager.getFileById(it) }
            finish()
        }

        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        playbackFileType = intent.getPlaybackFileType()
        recreatePlayerView()
    }

    private fun createPlayerView() {
        playerView = when (playbackFileType) {
            PlaybackFileType.AUDIO -> AudioPlayerView(this)
            PlaybackFileType.VIDEO -> VideoPlayerView(this)
        }
        val moreButton = playerView.findViewById<View>(R.id.more)
        moreButton.setOnClickListener { viewModel.onMoreButtonClick() }
        setContentView(playerView)
    }

    private fun recreatePlayerView() {
        playerView.onStop()
        playerView.release()
        createPlayerView()
        playerView.onStart()
    }

    private fun Intent.getPlaybackFileType(): PlaybackFileType =
        getSerializableArgument(PLAYBACK_FILE_TYPE, PlaybackFileType::class.java)
            ?: throw IllegalStateException("Playback file type was not defined")

    override fun onStart() {
        super.onStart()
        playerView.onStart()
    }

    override fun onStop() {
        super.onStop()
        playerView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackModel.onPictureInPictureClose = null
        if (isFinishing && !keepPlaybackAliveOnFinish && playbackFileType == PlaybackFileType.VIDEO) {
            playbackModel.release()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val videoPlayerView = playerView as? VideoPlayerView ?: return
        if (isInPictureInPictureMode) {
            videoPlayerView.hideControls()
        } else {
            videoPlayerView.showControls()
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (canUsePictureInPictureMode()) {
            pictureInPicture.enter(playerView)
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)

        playbackModel.onPictureInPictureClose = if (isInPictureInPictureMode) {
            {
                keepPlaybackAliveOnFinish = true
                finish()
            }
        } else {
            null
        }

        if (!isInPictureInPictureMode && lifecycle.currentState == Lifecycle.State.CREATED) {
            finish() // Finish the activity if the user closes the PIP window
            return
        }

        if (!isInPictureInPictureMode) {
            (playerView as? VideoPlayerView)?.showControls()
        }
    }

    private fun canUsePictureInPictureMode(): Boolean =
        playbackFileType == PlaybackFileType.VIDEO && pictureInPicture.isAllowed

    private fun handleEvent(event: PlayerScreenEvent) {
        when (event) {
            is PlayerScreenEvent.ShowFileActions -> showFileActions(event.file, event.actionIds)
            is PlayerScreenEvent.ShowFileDetails -> showFileDetails(event.file)
            is PlayerScreenEvent.ShowFileExportStartedMessage -> showFileExportStartedMessage()
            is PlayerScreenEvent.ShowShareFileDialog -> fileOperationsHelper.sendShareFile(event.file)
            is PlayerScreenEvent.ShowRemoveFileDialog -> showRemoveFileDialog(event.file)
            is PlayerScreenEvent.LaunchOpenFileIntent -> fileOperationsHelper.openFile(event.file)
            is PlayerScreenEvent.LaunchStreamFileIntent -> fileOperationsHelper.streamMediaFile(event.file)
        }
    }

    private fun showFileActions(file: OCFile, actionIds: List<Int>) {
        val actionsToHide = FileAction.entries.map(FileAction::id).filter { it !in actionIds }
        FileActionsBottomSheet.newInstance(file, false, actionsToHide)
            .setResultListener(supportFragmentManager, this) { viewModel.onFileActionChosen(file, it) }
            .show(supportFragmentManager, "actions")
    }

    private fun showFileDetails(file: OCFile) {
        val intent = Intent(this, FileDisplayActivity::class.java).apply {
            action = FileDisplayActivity.ACTION_DETAILS
            putExtra(EXTRA_FILE, file)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }

    private fun showFileExportStartedMessage() {
        val message = resources.getQuantityString(R.plurals.export_start, 1, 1)
        DisplayUtils.showSnackMessage(playerView, message)
    }

    private fun showRemoveFileDialog(file: OCFile) {
        RemoveFilesDialogFragment.newInstance(file)
            .show(supportFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
    }
}
