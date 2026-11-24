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
import androidx.annotation.IdRes
import androidx.core.view.ViewCompat
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.di.ViewModelFactory
import com.nextcloud.ui.fileactions.FileActionHandler
import com.nextcloud.ui.fileactions.FileActionHost
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.fragment.FileFragment
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class AudioPlayerActivity :
    FileActivity(),
    FileFragment.ContainerActivity,
    Injectable {

    companion object {
        fun createIntent(context: Context): Intent = Intent(context, AudioPlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    @Inject
    lateinit var fileActionHandler: FileActionHandler

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
            is AudioPlayerScreenEvent.ShowFileExportStartedMessage -> showFileExportStartedMessage()
        }
    }

    private fun showFileActions(file: OCFile, actionsToHide: List<Int>) {
        FileActionsBottomSheet.newInstance(file, false, actionsToHide)
            .setResultListener(supportFragmentManager, this) { itemId -> onFileActionChosen(file, itemId) }
            .show(supportFragmentManager, "actions")
    }

    private fun onFileActionChosen(file: OCFile, @IdRes itemId: Int) {
        viewModel.onFileActionChosen(itemId)

        val host = FileActionHost(this, supportFragmentManager, audioPlayerView, null)
        fileActionHandler.handle(itemId, listOf(file), host)
    }

    private fun showFileExportStartedMessage() {
        val message = resources.getQuantityString(R.plurals.export_start, 1, 1)
        SnackbarUtil.show(audioPlayerView, message)
    }

    override fun showDetails(file: OCFile) {
        val intent = Intent(this, FileDisplayActivity::class.java).apply {
            action = FileDisplayActivity.ACTION_DETAILS
            putExtra(EXTRA_FILE, file)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
        finish()
    }

    override fun showDetails(file: OCFile, activeTab: Int) = showDetails(file)

    override fun onBrowsedDownTo(folder: OCFile) = Unit

    override fun onTransferStateChanged(file: OCFile, downloading: Boolean, uploading: Boolean) = Unit
}
