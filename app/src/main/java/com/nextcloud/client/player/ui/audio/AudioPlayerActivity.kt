/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.audio

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class AudioPlayerActivity :
    FileActivity(),
    Injectable {

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, AudioPlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val viewModel by viewModels<AudioPlayerViewModel> { viewModelFactory }

    private lateinit var audioPlayerView: AudioPlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets -> windowInsets }

        audioPlayerView = AudioPlayerView(this)
        audioPlayerView.onMoreClick = { viewModel.onMoreButtonClick() }
        setContentView(audioPlayerView)

        viewModel.eventFlow
            .flowWithLifecycle(lifecycle)
            .onEach { handleEvent(it) }
            .launchIn(lifecycleScope)

        onBackPressedDispatcher.addCallback(this) {
            file = file?.parentId?.let { storageManager.getFileById(it) }
            finish()
        }

        volumeControlStream = AudioManager.STREAM_MUSIC
    }

    override fun onStart() {
        super.onStart()
        audioPlayerView.onStart()
    }

    override fun onStop() {
        super.onStop()
        audioPlayerView.onStop()
    }

    private fun handleEvent(event: AudioPlayerScreenEvent) {
        when (event) {
            is AudioPlayerScreenEvent.ShowFileActions -> showFileActions(event.file, event.actionsToHide)

            is AudioPlayerScreenEvent.ShowFileDetails -> showFileDetails(event.file)

            is AudioPlayerScreenEvent.ShowFileExportStartedMessage -> showFileExportStartedMessage()

            is AudioPlayerScreenEvent.ShowShareFileDialog -> fileOperationsHelper.sendShareFile(event.file)

            is AudioPlayerScreenEvent.ShowRemoveFileDialog -> showRemoveFileDialog(event.file)

            is AudioPlayerScreenEvent.LaunchOpenFileIntent -> fileOperationsHelper.openFile(event.file)

            is AudioPlayerScreenEvent.LaunchStreamFileIntent -> fileOperationsHelper.streamMediaFile(event.file)

            is AudioPlayerScreenEvent.ToggleFileLock -> fileOperationsHelper.toggleFileLock(
                event.file,
                event.shouldBeLocked
            )

            is AudioPlayerScreenEvent.AddFileToAlbum -> fileOperationsHelper.addFileToAlbum(listOf(event.file))
        }
    }

    private fun showFileActions(file: OCFile, actionsToHide: List<Int>) {
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
        SnackbarUtil.show(audioPlayerView, message)
    }

    private fun showRemoveFileDialog(file: OCFile) {
        RemoveFilesDialogFragment.newInstance(file)
            .show(supportFragmentManager, ConfirmationDialogFragment.FTAG_CONFIRMATION)
    }
}
