/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.android.common.ui.util.extensions.applyEdgeToEdgeWithSystemBarPadding
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.databinding.ActivityManageSpaceBinding
import com.owncloud.android.lib.common.utils.Log_OC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.system.exitProcess

class ManageSpaceActivity :
    AppCompatActivity(),
    Injectable {

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var userAccountManager: UserAccountManager

    private lateinit var binding: ActivityManageSpaceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        applyEdgeToEdgeWithSystemBarPadding()
        super.onCreate(savedInstanceState)

        binding = ActivityManageSpaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.run {
            manageActivityToolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            generalDescription.text = getString(R.string.manage_space_description, getString(R.string.app_name))
            clearDataButton.setOnClickListener {
                lifecycleScope.launch {
                    clearData()
                }
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun clearData() {
        withContext(Dispatchers.IO) {
            val lockPref = preferences.lockPreference
            val passCodeEnable = SettingsActivity.LOCK_PASSCODE == lockPref
            var passCodeDigits = arrayOfNulls<String>(4)
            if (passCodeEnable) {
                passCodeDigits = preferences.passCode
            }

            // Clear preferences data
            preferences.clear()

            // Recover passcode
            if (passCodeEnable) {
                preferences.setPassCode(
                    passCodeDigits[0],
                    passCodeDigits[1],
                    passCodeDigits[2],
                    passCodeDigits[3]
                )
            }
            preferences.lockPreference = lockPref
            userAccountManager.removeAllAccounts()

            // Clear app data
            val result = clearApplicationData()
            withContext(Dispatchers.Main) {
                if (result) {
                    finishAndRemoveTask()
                    exitProcess(0)
                } else {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.manage_space_clear_data,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private fun clearApplicationData(): Boolean {
        var clearResult = true

        cacheDir.parent?.let { parentCacheDirPath ->
            val appDir = File(parentCacheDirPath)
            if (appDir.exists()) {
                val children = appDir.list()
                if (children != null) {
                    children.filter { it != LIB_FOLDER }.forEach { s ->
                        val fileToDelete = File(appDir, s)
                        clearResult = clearResult && deleteDir(fileToDelete)
                        Log_OC.d(TAG, "Clear Application Data, File: " + fileToDelete.name + " DELETED *****")
                    }
                } else {
                    clearResult = false
                }
            }
        }

        return clearResult
    }

    @Suppress("ReturnCount")
    private fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            dir.list()?.forEach { child ->
                val success = deleteDir(File(dir, child))
                if (!success) {
                    Log_OC.w(TAG, "File NOT deleted $child")
                    return false
                } else {
                    Log_OC.d(TAG, "File deleted $child")
                }
            } ?: return false
        }
        return dir?.delete() ?: false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> {
            finish()
            true
        }
        else -> {
            Log_OC.w(TAG, "Unknown menu item triggered")
            super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private val TAG = ManageSpaceActivity::class.java.simpleName
        private const val LIB_FOLDER = "lib"
    }
}
