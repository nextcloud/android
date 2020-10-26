/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.ui

import android.accounts.Account
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.common.OwnCloudClientFactory
import com.owncloud.android.lib.resources.users.ClearStatusMessageRemoteOperation
import com.owncloud.android.lib.resources.users.PredefinedStatus
import com.owncloud.android.lib.resources.users.SetPredefinedCustomStatusMessageRemoteOperation
import com.owncloud.android.lib.resources.users.SetStatusRemoteOperation
import com.owncloud.android.lib.resources.users.SetUserDefinedCustomStatusMessageRemoteOperation
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.adapter.PredefinedStatusClickListener
import com.owncloud.android.ui.adapter.PredefinedStatusListAdapter
import kotlinx.android.synthetic.main.dialog_set_status.*
import java.util.ArrayList
import javax.inject.Inject

private const val ARG_CURRENT_USER_PARAM = "currentUser"
private const val ARG_CURRENT_STATUS_PARAM = "currentStatus"

class SetStatusDialogFragment : DialogFragment(),
    PredefinedStatusClickListener,
    Injectable {
    private lateinit var dialogView: View
    private var currentUser: User? = null
    private var currentStatus: Status? = null
    private lateinit var accountManager: UserAccountManager
    private lateinit var predefinedStatus: ArrayList<PredefinedStatus>
    private lateinit var adapter: PredefinedStatusListAdapter
    private var selectedPredefinedMessageId: String? = null

    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    @Inject
    lateinit var asyncRunner: AsyncRunner

    @Inject
    lateinit var clientFactory: ClientFactory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentUser = it.getParcelable(ARG_CURRENT_USER_PARAM)
            currentStatus = it.getParcelable(ARG_CURRENT_STATUS_PARAM)

            val json = arbitraryDataProvider.getValue(currentUser, ArbitraryDataProvider.PREDEFINED_STATUS)

            if (json.isNotEmpty()) {
                val myType = object : TypeToken<ArrayList<PredefinedStatus>>() {}.type
                predefinedStatus = Gson().fromJson(json, myType)
            }
        }
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_status, null)
        return MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountManager = (activity as BaseActivity).userAccountManager

        currentStatus?.let {
            emoji.text = it.icon
            customStatusInput.text.clear()
            customStatusInput.text.append(it.message)
        }

        adapter = PredefinedStatusListAdapter(this, requireContext())
        if (this::predefinedStatus.isInitialized) {
            adapter.list = predefinedStatus
        }
        predefinedStatusList.adapter = adapter
        predefinedStatusList.layoutManager = LinearLayoutManager(context)

        onlineStatus.setOnClickListener { setStatus(StatusType.ONLINE) }
        dndStatus.setOnClickListener { setStatus(StatusType.DND) }
        awayStatus.setOnClickListener { setStatus(StatusType.AWAY) }
        invisibleStatus.setOnClickListener { setStatus(StatusType.INVISIBLE) }

        clearStatus.setOnClickListener { clearStatus() }
        setStatus.setOnClickListener { setStatusMessage() }
    }

    private fun clearStatus() {
        asyncRunner.postQuickTask(ClearStatusTask(accountManager.currentOwnCloudAccount?.savedAccount, context),
            { dismiss(it) })
    }

    private fun setStatus(statusType: StatusType) {
        asyncRunner.postQuickTask(
            SetStatusTask(
                statusType,
                accountManager.currentOwnCloudAccount?.savedAccount,
                context)
        )
    }

    private fun setStatusMessage() {
        if (selectedPredefinedMessageId != null) {
            asyncRunner.postQuickTask(
                SetPredefinedCustomStatusTask(
                    selectedPredefinedMessageId!!,
                    1603719631,
                    accountManager.currentOwnCloudAccount?.savedAccount,
                    context),
                { dismiss(it) }
            )
        } else {
            asyncRunner.postQuickTask(
                SetUserDefinedCustomStatusTask(
                    customStatusInput.text.toString(),
                    emoji.text.toString(),
                    1603719631,
                    accountManager.currentOwnCloudAccount?.savedAccount,
                    context),
                { dismiss(it) }
            )
        }
    }

    private fun dismiss(boolean: Boolean) {
        if (boolean) {
            dismiss()
        }
    }

    private class SetPredefinedCustomStatusTask(val messageId: String,
                                                val clearAt: Long,
                                                val account: Account?,
                                                val context: Context?) : Function0<Boolean> {
        override fun invoke(): Boolean {
            val client = OwnCloudClientFactory.createNextcloudClient(account, context)

            return SetPredefinedCustomStatusMessageRemoteOperation(messageId, clearAt).execute(client).isSuccess
        }
    }

    private class SetUserDefinedCustomStatusTask(val message: String,
                                                 val icon: String,
                                                 val clearAt: Long,
                                                 val account: Account?,
                                                 val context: Context?) : Function0<Boolean> {
        override fun invoke(): Boolean {
            val client = OwnCloudClientFactory.createNextcloudClient(account, context)

            return SetUserDefinedCustomStatusMessageRemoteOperation(message, icon, clearAt).execute(client).isSuccess
        }
    }

    private class SetStatusTask(val statusType: StatusType,
                                val account: Account?,
                                val context: Context?) : Function0<Boolean> {
        override fun invoke(): Boolean {
            val client = OwnCloudClientFactory.createNextcloudClient(account, context)

            return SetStatusRemoteOperation(statusType).execute(client).isSuccess
        }
    }

    private class ClearStatusTask(val account: Account?, val context: Context?) : Function0<Boolean> {
        override fun invoke(): Boolean {
            val client = OwnCloudClientFactory.createNextcloudClient(account, context)

            return ClearStatusMessageRemoteOperation().execute(client).isSuccess
        }
    }

    /**
     * Fragment creator
     */
    companion object {
        @JvmStatic
        fun newInstance(user: User, status: Status?) =
            SetStatusDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CURRENT_USER_PARAM, user)
                    putParcelable(ARG_CURRENT_STATUS_PARAM, status)
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return dialogView
    }

    override fun onClick(predefinedStatus: PredefinedStatus) {
        selectedPredefinedMessageId = predefinedStatus.id
        emoji.text = predefinedStatus.icon
        customStatusInput.text.clear()
        customStatusInput.text.append(predefinedStatus.message)
    }

    @VisibleForTesting
    fun setPredefinedStatus(predefinedStatus: ArrayList<PredefinedStatus>) {
        adapter.list = predefinedStatus
        predefinedStatusList.adapter?.notifyDataSetChanged()
    }
}
