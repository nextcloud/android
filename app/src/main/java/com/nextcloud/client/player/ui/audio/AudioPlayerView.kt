/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.audio

import android.content.Context
import android.view.WindowInsets
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.player.media3.PlaybackModel
import com.nextcloud.client.player.model.error.SourceException
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.ui.control.PlayerControlView
import com.nextcloud.utils.SnackbarUtil
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import dagger.android.HasAndroidInjector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AudioPlayerView(context: Context) :
    LinearLayout(context),
    PlaybackModel.Listener {

    companion object {
        private const val TAG = "AudioPlayerView"
    }

    private val activity = context as AppCompatActivity

    @Inject
    lateinit var playbackModel: PlaybackModel

    @Inject
    lateinit var userAccountManager: UserAccountManager

    private val systemBars by lazy { AudioPlayerSystemBars(activity.window) }

    private val topBar: MaterialToolbar by lazy { findViewById(R.id.topBar) }
    private val playerPager: AudioPlayerPager by lazy { findViewById(R.id.playerPager) }
    private val playerControlView: PlayerControlView by lazy { findViewById(R.id.playerControlView) }

    var onMoreClick: (() -> Unit)? = null

    init {
        inflate(activity, R.layout.player_audio_view, this)
        (activity.applicationContext as HasAndroidInjector).androidInjector().inject(this)
        playerPager.initialize(activity.supportFragmentManager, activity.lifecycle)
        playerPager.onItemSelected = { playbackModel.switchToFile(it) }
        playerControlView.navigator = playerPager
        topBar.setNavigationOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
        topBar.setOnMenuItemClickListener { menuItem ->
            val isMore = menuItem.itemId == R.id.action_more
            if (isMore) {
                onMoreClick?.invoke()
            }
            isMore
        }
    }

    override fun onApplyWindowInsets(windowInsets: WindowInsets): WindowInsets? {
        val windowInsetsCompat = WindowInsetsCompat.toWindowInsetsCompat(windowInsets)
        val insets = windowInsetsCompat.getInsets(Type.systemBars() or Type.displayCutout())

        val actionInset = resources.getDimensionPixelSize(R.dimen.standard_quarter_padding)
        topBar.setPadding(insets.left, insets.top, insets.right + actionInset, 0)
        playerPager.setPadding(insets.left, 0, insets.right, 0)
        playerControlView.setPadding(insets.left, 0, insets.right, insets.bottom)

        systemBars.setStatusBarBackground(R.color.player_background_color)
        systemBars.setNavigationBarBackground(R.color.player_background_color)

        return WindowInsetsCompat.CONSUMED.toWindowInsets()
    }

    fun onStart() {
        val state = playbackModel.state
        if (state == null) {
            activity.finish()
            return
        }

        systemBars.show()
        render(state)
        playbackModel.addListener(this)
        playerControlView.onStart()
    }

    fun onStop() {
        playbackModel.removeListener(this)
        playerControlView.onStop()
    }

    override fun onPlaybackUpdate(state: PlaybackState) {
        render(state)
    }

    override fun onPlaybackError(error: Throwable) {
        if (error is SourceException) {
            downloadFile()
        } else {
            SnackbarUtil.show(this, R.string.common_error_unknown)
        }
    }

    private fun downloadFile() {
        val currentFile = playbackModel.state?.currentItemState?.file
        val storageManager = FileDataStorageManager(userAccountManager.user, context.contentResolver)
        val file = currentFile?.id?.toLong()?.let { storageManager.getFileByLocalId(it) } ?: return

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val operation = DownloadFileOperation(userAccountManager.user, file, context)
            val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                .getClientFor(userAccountManager.currentOwnCloudAccount, context)
            val result = operation.execute(client)
            if (result.isSuccess) {
                Log_OC.d(TAG, "file is successfully downloaded")
                val helper = FileDownloadHelper()
                helper.saveFile(file, operation, storageManager)
            } else {
                Log_OC.e(TAG, "cannot download file")
                withContext(Dispatchers.Main) {
                    SnackbarUtil.show(this@AudioPlayerView, R.string.player_error_source_not_found)
                }
            }
        }
    }

    private fun render(state: PlaybackState) {
        val currentFiles = state.currentFiles
        if (currentFiles.isEmpty()) {
            activity.finish()
            return
        }

        if (playerPager.getItems() != currentFiles) {
            playerPager.setItems(currentFiles)
        }

        val file = state.currentItemState?.file
        topBar.title = file?.getNameWithoutExtension().orEmpty()
        file?.let(playerPager::setCurrentItem)
    }
}
