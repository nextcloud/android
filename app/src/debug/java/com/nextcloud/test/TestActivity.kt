/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.test

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.nextcloud.client.network.Connectivity
import com.nextcloud.client.network.ConnectivityService
import com.nextcloud.utils.EditorUtils
import com.owncloud.android.R
import com.owncloud.android.databinding.TestLayoutBinding
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.files.services.FileDownloader
import com.owncloud.android.files.services.FileUploader
import com.owncloud.android.lib.resources.status.OCCapability
import com.owncloud.android.lib.resources.status.OwnCloudVersion
import com.owncloud.android.services.OperationsService
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.OnEnforceableRefreshListener
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.ui.helpers.FileOperationsHelper

class TestActivity :
    FileActivity(),
    FileFragment.ContainerActivity,
    SwipeRefreshLayout.OnRefreshListener,
    OnEnforceableRefreshListener {
    lateinit var fragment: Fragment
    lateinit var secondaryFragment: Fragment

    private lateinit var storage: FileDataStorageManager
    private lateinit var fileOperation: FileOperationsHelper
    private lateinit var binding: TestLayoutBinding

    val connectivityServiceMock: ConnectivityService = object : ConnectivityService {
        override fun isInternetWalled(): Boolean {
            return false
        }

        override fun getConnectivity(): Connectivity {
            return Connectivity.CONNECTED_WIFI
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = TestLayoutBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun addFragment(fragment: Fragment) {
        this.fragment = fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.main_fragment, fragment)
        transaction.commit()
    }

    /**
     * Adds a secondary fragment to the activity with the given tag.
     *
     * If you have to use this, your Fragments are coupled, and you should feel bad.
     */
    fun addSecondaryFragment(fragment: Fragment, tag: String) {
        this.secondaryFragment = fragment
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.secondary_fragment, fragment, tag)
        transaction.commit()
    }

    /**
     * Adds a View to the activity.
     *
     * If you have to use this, your Fragment is coupled to your Activity and you should feel bad.
     */
    fun addView(view: View) {
        handler.post {
            binding.rootLayout.addView(view)
        }
    }

    override fun onBrowsedDownTo(folder: OCFile?) {
        TODO("Not yet implemented")
    }

    override fun getOperationsServiceBinder(): OperationsService.OperationsServiceBinder? {
        return null
    }

    override fun showSortListGroup(show: Boolean) {
        // not needed
    }

    override fun showDetails(file: OCFile?) {
        TODO("Not yet implemented")
    }

    override fun showDetails(file: OCFile?, activeTab: Int) {
        TODO("Not yet implemented")
    }

    override fun getFileUploaderBinder(): FileUploader.FileUploaderBinder? {
        return null
    }

    override fun getFileDownloaderBinder(): FileDownloader.FileDownloaderBinder? {
        return null
    }

    override fun getStorageManager(): FileDataStorageManager {
        if (!this::storage.isInitialized) {
            storage = FileDataStorageManager(user.get(), contentResolver)

            if (!storage.capabilityExistsForAccount(account.name)) {
                val ocCapability = OCCapability()
                ocCapability.versionMayor = OwnCloudVersion.nextcloud_20.majorVersionNumber
                storage.saveCapabilities(ocCapability)
            }
        }

        return storage
    }

    override fun getFileOperationsHelper(): FileOperationsHelper {
        if (!this::fileOperation.isInitialized) {
            fileOperation = FileOperationsHelper(
                this,
                userAccountManager,
                connectivityServiceMock,
                EditorUtils(
                    ArbitraryDataProviderImpl(baseContext)
                )
            )
        }

        return fileOperation
    }

    override fun onTransferStateChanged(file: OCFile?, downloading: Boolean, uploading: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onRefresh(enforced: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onRefresh() {
        TODO("Not yet implemented")
    }
}
