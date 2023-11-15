/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.activity.OnBackPressedCallback
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentManager
import com.nextcloud.client.account.User
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.fragment.contactsbackup.BackupFragment
import com.owncloud.android.ui.fragment.contactsbackup.BackupFragment.Companion.create
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment

/**
 * This activity shows all settings for contact backup/restore
 */
class ContactsPreferenceActivity : FileActivity(), FileFragment.ContainerActivity {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.contacts_preference)

        setupToolbar()

        // TODO needed?
        // setupDrawer(R.id.nav_contacts);

        val sidebar = showSidebar()

        val intent = intent
        if (savedInstanceState == null) {
            val transaction = supportFragmentManager.beginTransaction()
            if (intent?.getParcelableExtra<Parcelable?>(EXTRA_FILE) == null ||
                intent.getParcelableExtra<Parcelable?>(EXTRA_USER) == null
            ) {
                val fragment = create(sidebar)
                transaction.add(R.id.frame_container, fragment)
            } else {
                val file = intent.getParcelableExtra<OCFile>(EXTRA_FILE)
                val user = intent.getParcelableExtra<User>(EXTRA_USER)
                val contactListFragment = BackupListFragment.newInstance(file, user)
                transaction.add(R.id.frame_container, contactListFragment)
            }
            transaction.commit()
        }

        handleOnBackPressed()
    }

    private fun handleOnBackPressed() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (supportFragmentManager.findFragmentByTag(BackupListFragment.TAG) != null) {
                        supportFragmentManager.popBackStack(BACKUP_TO_LIST, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    } else {
                        finish()
                    }
                }
            }
        )
    }

    private fun showSidebar(): Boolean {
        val showSidebar = intent.getBooleanExtra(EXTRA_SHOW_SIDEBAR, true)
        if (!showSidebar) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mDrawerToggle?.isDrawerIndicatorEnabled = false
        }
        return showSidebar
    }

    override fun showDetails(file: OCFile) {
        // not needed
    }

    override fun showDetails(file: OCFile, activeTab: Int) {
        // not needed
    }

    override fun onBrowsedDownTo(folder: OCFile) {
        // not needed
    }

    override fun onTransferStateChanged(file: OCFile, downloading: Boolean, uploading: Boolean) {
        // not needed
    }

    companion object {
        val TAG = ContactsPreferenceActivity::class.java.simpleName
        const val EXTRA_FILE = "FILE"
        const val EXTRA_USER = "USER"

        /**
         * Warning: default for this extra is different between this activity and [BackupFragment]
         */
        const val EXTRA_SHOW_SIDEBAR = "SHOW_SIDEBAR"
        const val PREFERENCE_CONTACTS_AUTOMATIC_BACKUP = "PREFERENCE_CONTACTS_AUTOMATIC_BACKUP"
        const val PREFERENCE_CONTACTS_LAST_BACKUP = "PREFERENCE_CONTACTS_LAST_BACKUP"
        const val BACKUP_TO_LIST = "BACKUP_TO_LIST"

        fun startActivity(context: Context) {
            val intent = Intent(context, ContactsPreferenceActivity::class.java)
            context.startActivity(intent)
        }

        @JvmStatic
        fun startActivityWithContactsFile(context: Context, user: User?, file: OCFile?) {
            val intent = Intent(context, ContactsPreferenceActivity::class.java)
            intent.putExtra(EXTRA_FILE, file)
            intent.putExtra(EXTRA_USER, user)
            context.startActivity(intent)
        }

        @JvmStatic
        fun startActivityWithoutSidebar(context: Context) {
            val intent = Intent(context, ContactsPreferenceActivity::class.java)
            intent.putExtra(EXTRA_SHOW_SIDEBAR, false)
            context.startActivity(intent)
        }
    }
}
