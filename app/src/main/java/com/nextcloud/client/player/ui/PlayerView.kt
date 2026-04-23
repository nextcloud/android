/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.jobs.download.FileDownloadHelper
import com.nextcloud.client.player.model.PlaybackModel
import com.nextcloud.client.player.model.error.SourceException
import com.nextcloud.client.player.model.file.PlaybackFile
import com.nextcloud.client.player.model.state.PlaybackState
import com.nextcloud.client.player.ui.control.PlayerControlView
import com.nextcloud.client.player.ui.pager.PlayerPager
import com.nextcloud.client.player.ui.pager.PlayerPagerFragmentFactory
import com.nextcloud.client.player.ui.pager.PlayerPagerMode
import com.nextcloud.client.player.util.WindowWrapper
import com.owncloud.android.R
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.DownloadFileOperation
import com.owncloud.android.utils.DisplayUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.jvm.optionals.getOrNull

abstract class PlayerView @JvmOverloads constructor(
    private val context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr),
    PlaybackModel.Listener {

    companion object {
        private const val TAG = "PlayerView"
    }

    @Inject
    lateinit var playbackModel: PlaybackModel

    @Inject
    lateinit var userAccountManager: UserAccountManager

    @get:LayoutRes
    protected abstract val layoutRes: Int

    protected abstract val fragmentFactory: PlayerPagerFragmentFactory<PlaybackFile>

    protected val activity: AppCompatActivity by lazy { context as AppCompatActivity }
    protected val windowWrapper: WindowWrapper by lazy { WindowWrapper(activity.window) }

    protected val topBar: View by lazy { findViewById(R.id.topBar) }
    protected val titleTextView: TextView by lazy { findViewById(R.id.title) }
    protected val playerPager: PlayerPager<PlaybackFile> by lazy { findViewById(R.id.playerPager) }
    protected val playerControlView: PlayerControlView by lazy { findViewById(R.id.playerControlView) }

    init {
        inflate(context, layoutRes, this)
        if (!isInEditMode) {
            inject(context)
            playerPager.initialize(activity.supportFragmentManager, PlayerPagerMode.INFINITE, fragmentFactory)
            playerPager.setPlayerPagerListener { playbackModel.switchToFile(it) }
            findViewById<View>(R.id.back).setOnClickListener { activity.onBackPressedDispatcher.onBackPressed() }
        }
    }

    protected abstract fun inject(context: Context)

    @CallSuper
    open fun onStart() {
        val state = playbackModel.state.getOrNull()
        if (state == null) {
            activity.finish()
            return
        }

        render(state)
        playbackModel.addListener(this)
        playerControlView.onStart()
    }

    @CallSuper
    open fun onStop() {
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
            DisplayUtils.showSnackMessage(this, R.string.common_error_unknown)
        }
    }

    private fun downloadFile() {
        val currentFile = playbackModel.state.getOrNull()?.currentItemState?.file
        val storageManager = FileDataStorageManager(userAccountManager.user, context.contentResolver)
        val file = currentFile?.id?.toLong()?.let { storageManager.getFileByLocalId(it) }

        activity.lifecycleScope.launch(Dispatchers.IO) {
            val operation = DownloadFileOperation(userAccountManager.user, file, context)
            val client = OwnCloudClientManagerFactory.getDefaultSingleton()
                .getClientFor(userAccountManager.currentOwnCloudAccount, context)
            val result = operation.execute(client)
            if (result.isSuccess) {
                Log_OC.d(TAG, "file is successfully downloaded")
                val helper = FileDownloadHelper()
                file?.let { helper.saveFile(it, operation, storageManager) }
            } else {
                Log_OC.e(TAG, "cannot download file")
                withContext(Dispatchers.Main) {
                    DisplayUtils.showSnackMessage(this@PlayerView, R.string.player_error_source_not_found)
                }
            }
        }
    }

    private fun render(state: PlaybackState) {
        val currentFiles = state.currentFiles
        if (state.currentFiles.isEmpty()) {
            activity.finish()
            return
        }

        if (playerPager.getItems() != currentFiles) {
            playerPager.setItems(currentFiles)
        }

        if (state.currentItemState != null) {
            val file = state.currentItemState.file
            titleTextView.text = file.getNameWithoutExtension()
            playerPager.setCurrentItem(file)
        } else {
            titleTextView.text = ""
        }
    }
}
