/*
 *   ownCloud Android client application
 *
 *   @author masensio
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
package com.owncloud.android.ui.activity

import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import java.io.File
import javax.inject.Inject
import kotlin.system.exitProcess

class ManageSpaceActivity : AppCompatActivity(), Injectable {

    @Inject
    lateinit var preferences: AppPreferences

    @Inject
    lateinit var userAccountManager: UserAccountManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_space)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setTitle(R.string.manage_space_title)
        }
        val descriptionTextView = findViewById<TextView>(R.id.general_description)
        descriptionTextView.text = getString(R.string.manage_space_description, getString(R.string.app_name))
        val clearDataButton = findViewById<Button>(R.id.clearDataButton)
        clearDataButton.setOnClickListener {
            val clearDataTask = ClearDataAsyncTask()
            clearDataTask.execute()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var retval = true
        when (item.itemId) {
            android.R.id.home -> finish()
            else -> {
                Log_OC.w(TAG, "Unknown menu item triggered")
                retval = super.onOptionsItemSelected(item)
            }
        }
        return retval
    }

    /**
     * AsyncTask for Clear Data, saving the passcode
     */
    private inner class ClearDataAsyncTask : AsyncTask<Void, Void, Boolean>() {
        private val preferences = this@ManageSpaceActivity.preferences
        private val userAccountManager = this@ManageSpaceActivity.userAccountManager

        @Suppress("MagicNumber")
        override fun doInBackground(vararg params: Void): Boolean {
            val lockPref = preferences.lockPreference
            val passCodeEnable = SettingsActivity.LOCK_PASSCODE == lockPref
            var passCodeDigits = arrayOfNulls<String>(4)
            if (passCodeEnable) {
                passCodeDigits = preferences.passCode
            }

            // Clear data
            preferences.clear()
            val result = clearApplicationData()

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
            return result
        }

        override fun onPostExecute(result: Boolean) {
            super.onPostExecute(result)
            if (!result) {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    R.string.manage_space_clear_data,
                    Snackbar.LENGTH_LONG
                ).show()
            } else {
                finishAndRemoveTask()
                exitProcess(0)
            }
        }

        fun clearApplicationData(): Boolean {
            var clearResult = true
            val appDir = File(cacheDir.parent!!)
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
            return clearResult
        }

        @Suppress("ReturnCount")
        fun deleteDir(dir: File?): Boolean {
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
    }

    companion object {
        private val TAG = ManageSpaceActivity::class.java.simpleName
        private const val LIB_FOLDER = "lib"
    }
}
