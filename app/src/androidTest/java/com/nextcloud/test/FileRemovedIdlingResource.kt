/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Philipp Hasper <vcs@hasper.info>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.test

import androidx.test.espresso.IdlingResource
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * IdlingResource that can be reused to watch the removal of different file ids sequentially.
 *
 * Use setFileId(fileId) before triggering the deletion. The resource will call the Espresso callback
 * once the file no longer exists. Call unregister from IdlingRegistry in @After.
 */
class FileRemovedIdlingResource(private val storageManager: FileDataStorageManager) : IdlingResource {
    private var resourceCallback: IdlingResource.ResourceCallback? = null

    // null means "no file set"
    private var currentFile = AtomicReference<OCFile>(null)

    override fun getName(): String = "${this::class.java.simpleName}"

    override fun isIdleNow(): Boolean {
        val file = currentFile.get()
        // If no file set, consider idle. If file set, idle only if it doesn't exist.
        val idle = file == null || (!storageManager.fileExists(file.fileId) && !file.exists())
        if (idle && file != null) {
            // if we detect it's already removed, notify and clear
            resourceCallback?.onTransitionToIdle()
            currentFile.set(null)
        }
        return idle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback) {
        this.resourceCallback = callback
    }

    /**
     * Start watching the given file. Call this right before performing the UI action that triggers deletion.
     */
    fun setFile(file: OCFile) {
        currentFile.set(file)
    }
}
