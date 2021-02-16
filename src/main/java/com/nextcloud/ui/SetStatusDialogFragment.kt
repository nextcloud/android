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

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogSetStatusBinding
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.resources.users.ClearAt
import com.owncloud.android.lib.resources.users.PredefinedStatus
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.lib.resources.users.StatusType
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.adapter.PredefinedStatusClickListener
import com.owncloud.android.ui.adapter.PredefinedStatusListAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.ThemeUtils
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.android.synthetic.main.dialog_set_status.*
import java.util.ArrayList
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

private const val ARG_CURRENT_USER_PARAM = "currentUser"
private const val ARG_CURRENT_STATUS_PARAM = "currentStatus"

private const val POS_DONT_CLEAR = 0
private const val POS_HALF_AN_HOUR = 1
private const val POS_AN_HOUR = 2
private const val POS_FOUR_HOURS = 3
private const val POS_TODAY = 4
private const val POS_END_OF_WEEK = 5

private const val ONE_SECOND_IN_MILLIS = 1000
private const val ONE_MINUTE_IN_SECONDS = 60
private const val THIRTY_MINUTES = 30
private const val FOUR_HOURS = 4
private const val LAST_HOUR_OF_DAY = 23
private const val LAST_MINUTE_OF_HOUR = 59
private const val LAST_SECOND_OF_MINUTE = 59

class SetStatusDialogFragment :
    DialogFragment(),
    PredefinedStatusClickListener,
    Injectable {

    private lateinit var binding: DialogSetStatusBinding

    private var currentUser: User? = null
    private var currentStatus: Status? = null
    private lateinit var accountManager: UserAccountManager
    private lateinit var predefinedStatus: ArrayList<PredefinedStatus>
    private lateinit var adapter: PredefinedStatusListAdapter
    private var selectedPredefinedMessageId: String? = null
    private var clearAt: Long? = -1
    private lateinit var popup: EmojiPopup

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

        EmojiManager.install(GoogleEmojiProvider())
    }

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogSetStatusBinding.inflate(LayoutInflater.from(context))

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountManager = (activity as BaseActivity).userAccountManager

        currentStatus?.let {
            emoji.setText(it.icon)
            binding.customStatusInput.text?.clear()
            binding.customStatusInput.setText(it.message)
            visualizeStatus(it.status)

            if (it.clearAt > 0) {
                clearStatusAfterSpinner.visibility = View.GONE
                remainingClearTime.apply {
                    clearStatusMessageTextView.text = getString(R.string.clear_status_message)
                    visibility = View.VISIBLE
                    text = DisplayUtils.getRelativeTimestamp(context, it.clearAt * ONE_SECOND_IN_MILLIS, true)
                        .toString()
                        .decapitalize(Locale.getDefault())
                    setOnClickListener {
                        visibility = View.GONE
                        clearStatusAfterSpinner.visibility = View.VISIBLE
                        clearStatusMessageTextView.text = getString(R.string.clear_status_message_after)
                    }
                }
            }
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
        emoji.setOnClickListener { openEmojiPopup() }

        popup = EmojiPopup.Builder
            .fromRootView(view)
            .setOnEmojiClickListener { _, _ ->
                popup.dismiss()
                emoji.clearFocus()
                val imm: InputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as
                    InputMethodManager
                imm.hideSoftInputFromWindow(emoji.windowToken, 0)
            }
            .build(emoji)
        emoji.disableKeyboardInput(popup)
        emoji.forceSingleEmoji()

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapter.add(getString(R.string.dontClear))
        adapter.add(getString(R.string.thirtyMinutes))
        adapter.add(getString(R.string.oneHour))
        adapter.add(getString(R.string.fourHours))
        adapter.add(getString(R.string.today))
        adapter.add(getString(R.string.thisWeek))

        clearStatusAfterSpinner.apply {
            this.adapter = adapter
            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    setClearStatusAfterValue(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // nothing to do
                }
            }
        }

        clearStatus.setTextColor(ThemeUtils.primaryColor(context, true))
        ThemeUtils.colorPrimaryButton(setStatus, context)
        ThemeUtils.colorTextInput(
            binding.customStatusInputContainer,
            binding.customStatusInput,
            ThemeUtils.primaryColor(activity)
        )
    }

    private fun setClearStatusAfterValue(item: Int) {
        when (item) {
            POS_DONT_CLEAR -> {
                // don't clear
                clearAt = null
            }

            POS_HALF_AN_HOUR -> {
                // 30 minutes
                clearAt = System.currentTimeMillis() / ONE_SECOND_IN_MILLIS + THIRTY_MINUTES * ONE_MINUTE_IN_SECONDS
            }

            POS_AN_HOUR -> {
                // one hour
                clearAt =
                    System.currentTimeMillis() / ONE_SECOND_IN_MILLIS + ONE_MINUTE_IN_SECONDS * ONE_MINUTE_IN_SECONDS
            }

            POS_FOUR_HOURS -> {
                // four hours
                clearAt =
                    System.currentTimeMillis() / ONE_SECOND_IN_MILLIS
                +FOUR_HOURS * ONE_MINUTE_IN_SECONDS * ONE_MINUTE_IN_SECONDS
            }

            POS_TODAY -> {
                // today
                val date = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, LAST_HOUR_OF_DAY)
                    set(Calendar.MINUTE, LAST_MINUTE_OF_HOUR)
                    set(Calendar.SECOND, LAST_SECOND_OF_MINUTE)
                }
                clearAt = date.timeInMillis / ONE_SECOND_IN_MILLIS
            }

            POS_END_OF_WEEK -> {
                // end of week
                val date = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, LAST_HOUR_OF_DAY)
                    set(Calendar.MINUTE, LAST_MINUTE_OF_HOUR)
                    set(Calendar.SECOND, LAST_SECOND_OF_MINUTE)
                }

                while (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    date.add(Calendar.DAY_OF_YEAR, 1)
                }

                clearAt = date.timeInMillis / ONE_SECOND_IN_MILLIS
            }
        }
    }

    @Suppress("ReturnCount")
    private fun clearAtToUnixTime(clearAt: ClearAt?): Long {
        if (clearAt != null) {
            if (clearAt.type.equals("period")) {
                return System.currentTimeMillis() / ONE_SECOND_IN_MILLIS + clearAt.time.toLong()
            } else if (clearAt.type.equals("end-of")) {
                if (clearAt.time.equals("day")) {
                    val date = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, LAST_HOUR_OF_DAY)
                        set(Calendar.MINUTE, LAST_MINUTE_OF_HOUR)
                        set(Calendar.SECOND, LAST_SECOND_OF_MINUTE)
                    }
                    return date.timeInMillis / ONE_SECOND_IN_MILLIS
                }
            }
        }

        return -1
    }

    private fun openEmojiPopup() {
        popup.show()
    }

    private fun clearStatus() {
        asyncRunner.postQuickTask(
            ClearStatusTask(accountManager.currentOwnCloudAccount?.savedAccount, context),
            { dismiss(it) }
        )
    }

    private fun setStatus(statusType: StatusType) {
        visualizeStatus(statusType)

        asyncRunner.postQuickTask(
            SetStatusTask(
                statusType,
                accountManager.currentOwnCloudAccount?.savedAccount,
                context
            ),
            {
                if (!it) {
                    clearTopStatus()
                }
            },
            { clearTopStatus() }
        )
    }

    private fun visualizeStatus(statusType: StatusType) {
        when (statusType) {
            StatusType.ONLINE -> {
                clearTopStatus()
                onlineStatus.setBackgroundColor(ThemeUtils.primaryColor(context))
            }
            StatusType.AWAY -> {
                clearTopStatus()
                awayStatus.setBackgroundColor(ThemeUtils.primaryColor(context))
            }
            StatusType.DND -> {
                clearTopStatus()
                dndStatus.setBackgroundColor(ThemeUtils.primaryColor(context))
            }
            StatusType.INVISIBLE -> {
                clearTopStatus()
                invisibleStatus.setBackgroundColor(ThemeUtils.primaryColor(context))
            }
            else -> clearTopStatus()
        }
    }

    private fun clearTopStatus() {
        context?.let {
            val grey = it.resources.getColor(R.color.grey_200)
            onlineStatus.setBackgroundColor(grey)
            awayStatus.setBackgroundColor(grey)
            dndStatus.setBackgroundColor(grey)
            invisibleStatus.setBackgroundColor(grey)
        }
    }

    private fun setStatusMessage() {
        if (selectedPredefinedMessageId != null) {
            asyncRunner.postQuickTask(
                SetPredefinedCustomStatusTask(
                    selectedPredefinedMessageId!!,
                    clearAt,
                    accountManager.currentOwnCloudAccount?.savedAccount,
                    context
                ),
                { dismiss(it) }
            )
        } else {
            asyncRunner.postQuickTask(
                SetUserDefinedCustomStatusTask(
                    customStatusInput.text.toString(),
                    emoji.text.toString(),
                    clearAt,
                    accountManager.currentOwnCloudAccount?.savedAccount,
                    context
                ),
                { dismiss(it) }
            )
        }
    }

    private fun dismiss(boolean: Boolean) {
        if (boolean) {
            dismiss()
        }
    }

    /**
     * Fragment creator
     */
    companion object {
        @JvmStatic
        fun newInstance(user: User, status: Status?): SetStatusDialogFragment {
            val args = Bundle()
            args.putParcelable(ARG_CURRENT_USER_PARAM, user)
            args.putParcelable(ARG_CURRENT_STATUS_PARAM, status)
            val dialogFragment = SetStatusDialogFragment()
            dialogFragment.arguments = args
            dialogFragment.setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog)
            return dialogFragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return binding.root
    }

    override fun onClick(predefinedStatus: PredefinedStatus) {
        selectedPredefinedMessageId = predefinedStatus.id
        clearAt = clearAtToUnixTime(predefinedStatus.clearAt)
        emoji.setText(predefinedStatus.icon)
        binding.customStatusInput.text?.clear()
        binding.customStatusInput.text?.append(predefinedStatus.message)

        remainingClearTime.visibility = View.GONE
        clearStatusAfterSpinner.visibility = View.VISIBLE
        clearStatusMessageTextView.text = getString(R.string.clear_status_message_after)

        if (predefinedStatus.clearAt == null) {
            clearStatusAfterSpinner.setSelection(0)
        } else {
            val clearAt = predefinedStatus.clearAt!!
            if (clearAt.type.equals("period")) {
                when (clearAt.time) {
                    "1800" -> clearStatusAfterSpinner.setSelection(POS_HALF_AN_HOUR)
                    "3600" -> clearStatusAfterSpinner.setSelection(POS_AN_HOUR)
                    "14400" -> clearStatusAfterSpinner.setSelection(POS_FOUR_HOURS)
                    else -> clearStatusAfterSpinner.setSelection(POS_DONT_CLEAR)
                }
            } else if (clearAt.type.equals("end-of")) {
                when (clearAt.time) {
                    "day" -> clearStatusAfterSpinner.setSelection(POS_TODAY)
                    "week" -> clearStatusAfterSpinner.setSelection(POS_END_OF_WEEK)
                    else -> clearStatusAfterSpinner.setSelection(POS_DONT_CLEAR)
                }
            }
        }
        setClearStatusAfterValue(clearStatusAfterSpinner.selectedItemPosition)
    }

    @VisibleForTesting
    fun setPredefinedStatus(predefinedStatus: ArrayList<PredefinedStatus>) {
        adapter.list = predefinedStatus
        predefinedStatusList.adapter?.notifyDataSetChanged()
    }
}
