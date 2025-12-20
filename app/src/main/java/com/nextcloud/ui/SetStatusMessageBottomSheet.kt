/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */

package com.nextcloud.ui

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nextcloud.client.account.User
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.core.AsyncRunner
import com.nextcloud.client.di.Injectable
import com.owncloud.android.R
import com.owncloud.android.databinding.SetStatusMessageBottomSheetBinding
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.lib.resources.users.ClearAt
import com.owncloud.android.lib.resources.users.PredefinedStatus
import com.owncloud.android.lib.resources.users.Status
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.adapter.PredefinedStatusClickListener
import com.owncloud.android.ui.adapter.PredefinedStatusListAdapter
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.EmojiPopup
import com.vanniktech.emoji.google.GoogleEmojiProvider
import com.vanniktech.emoji.installDisableKeyboardInput
import com.vanniktech.emoji.installForceSingleEmoji
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

private const val POS_DONT_CLEAR = 0
private const val POS_FIFTEEN_MINUTES = 1
private const val POS_HALF_AN_HOUR = 2
private const val POS_AN_HOUR = 3
private const val POS_FOUR_HOURS = 4
private const val POS_TODAY = 5
private const val POS_END_OF_WEEK = 6

private const val ONE_SECOND_IN_MILLIS = 1000
private const val ONE_MINUTE_IN_SECONDS = 60
private const val THIRTY_MINUTES = 30
private const val FIFTEEN_MINUTES = 15
private const val FOUR_HOURS = 4
private const val LAST_HOUR_OF_DAY = 23
private const val LAST_MINUTE_OF_HOUR = 59
private const val LAST_SECOND_OF_MINUTE = 59

private const val CLEAR_AT_TYPE_PERIOD = "period"
private const val CLEAR_AT_TYPE_END_OF = "end-of"

class SetStatusMessageBottomSheet(val user: User, val currentStatus: Status?) :
    BottomSheetDialogFragment(R.layout.set_status_message_bottom_sheet),
    PredefinedStatusClickListener,
    Injectable {

    private lateinit var binding: SetStatusMessageBottomSheetBinding

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
    lateinit var viewThemeUtils: ViewThemeUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val json = arbitraryDataProvider.getValue(user, ArbitraryDataProvider.PREDEFINED_STATUS)

        if (json.isNotEmpty()) {
            val myType = object : TypeToken<ArrayList<PredefinedStatus>>() {}.type
            predefinedStatus = Gson().fromJson(json, myType)
        }

        EmojiManager.install(GoogleEmojiProvider())
    }

    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        accountManager = (activity as BaseActivity).userAccountManager

        currentStatus?.let {
            updateCurrentStatusViews(it)
        }

        adapter = PredefinedStatusListAdapter(this, requireContext())
        if (this::predefinedStatus.isInitialized) {
            adapter.list = predefinedStatus
        }
        binding.predefinedStatusList.adapter = adapter
        binding.predefinedStatusList.layoutManager = LinearLayoutManager(context)

        binding.clearStatus.setOnClickListener { clearStatus() }
        binding.setStatus.setOnClickListener { setStatusMessage() }
        binding.emoji.setOnClickListener { popup.show() }

        popup = EmojiPopup(view, binding.emoji, onEmojiClickListener = { _ ->
            popup.dismiss()
            binding.emoji.clearFocus()
            val imm: InputMethodManager = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as
                InputMethodManager
            imm.hideSoftInputFromWindow(binding.emoji.windowToken, 0)
        })
        binding.emoji.installForceSingleEmoji()
        binding.emoji.installDisableKeyboardInput(popup)

        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        adapter.add(getString(R.string.dontClear))
        adapter.add(getString(R.string.fifteenMinutes))
        adapter.add(getString(R.string.thirtyMinutes))
        adapter.add(getString(R.string.oneHour))
        adapter.add(getString(R.string.fourHours))
        adapter.add(getString(R.string.today))
        adapter.add(getString(R.string.thisWeek))

        binding.clearStatusAfterSpinner.apply {
            this.adapter = adapter
            onItemSelectedListener = object : OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    setClearStatusAfterValue(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // nothing to do
                }
            }
        }

        viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(binding.clearStatus)
        viewThemeUtils.material.colorMaterialButtonPrimaryTonal(binding.setStatus)
        viewThemeUtils.material.colorTextInputLayout(binding.customStatusInputContainer)

        viewThemeUtils.platform.themeDialog(binding.root)
    }

    private fun updateCurrentStatusViews(it: Status) {
        if (it.icon.isNullOrBlank()) {
            binding.emoji.setText("😀")
        } else {
            binding.emoji.setText(it.icon)
        }

        binding.customStatusInput.text?.clear()
        binding.customStatusInput.setText(it.message)

        if (it.clearAt > 0) {
            binding.clearStatusAfterSpinner.visibility = View.GONE
            binding.remainingClearTime.apply {
                binding.clearStatusMessageTextView.text = getString(R.string.clear)
                visibility = View.VISIBLE
                text = DisplayUtils.getRelativeTimestamp(context, it.clearAt * ONE_SECOND_IN_MILLIS, true)
                    .toString()
                    .replaceFirstChar { it.lowercase(Locale.getDefault()) }
                setOnClickListener {
                    visibility = View.GONE
                    binding.clearStatusAfterSpinner.visibility = View.VISIBLE
                    binding.clearStatusMessageTextView.text = getString(R.string.clear_status_after)
                }
            }
        }
    }

    private fun setClearStatusAfterValue(item: Int) {
        clearAt = when (item) {
            POS_DONT_CLEAR -> null // don't clear
            POS_FIFTEEN_MINUTES -> {
                // 15 minutes
                System.currentTimeMillis() / ONE_SECOND_IN_MILLIS + FIFTEEN_MINUTES * ONE_MINUTE_IN_SECONDS
            }
            POS_HALF_AN_HOUR -> {
                // 30 minutes
                System.currentTimeMillis() / ONE_SECOND_IN_MILLIS + THIRTY_MINUTES * ONE_MINUTE_IN_SECONDS
            }

            POS_AN_HOUR -> {
                // one hour
                System.currentTimeMillis() / ONE_SECOND_IN_MILLIS + ONE_MINUTE_IN_SECONDS * ONE_MINUTE_IN_SECONDS
            }

            POS_FOUR_HOURS -> {
                // four hours
                System.currentTimeMillis() / ONE_SECOND_IN_MILLIS +
                    FOUR_HOURS * ONE_MINUTE_IN_SECONDS * ONE_MINUTE_IN_SECONDS
            }

            POS_TODAY -> {
                // today
                val date = getLastSecondOfToday()
                dateToSeconds(date)
            }

            POS_END_OF_WEEK -> {
                // end of week
                val date = getLastSecondOfToday()
                while (date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    date.add(Calendar.DAY_OF_YEAR, 1)
                }
                dateToSeconds(date)
            }

            else -> clearAt
        }
    }

    private fun clearAtToUnixTime(clearAt: ClearAt?): Long = when {
        clearAt?.type == CLEAR_AT_TYPE_PERIOD -> {
            System.currentTimeMillis() / ONE_SECOND_IN_MILLIS + clearAt.time.toLong()
        }

        clearAt?.type == CLEAR_AT_TYPE_END_OF && clearAt.time == "day" -> {
            val date = getLastSecondOfToday()
            dateToSeconds(date)
        }

        else -> -1
    }

    private fun getLastSecondOfToday(): Calendar {
        val date = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, LAST_HOUR_OF_DAY)
            set(Calendar.MINUTE, LAST_MINUTE_OF_HOUR)
            set(Calendar.SECOND, LAST_SECOND_OF_MINUTE)
        }
        return date
    }

    private fun dateToSeconds(date: Calendar) = date.timeInMillis / ONE_SECOND_IN_MILLIS

    private fun clearStatus() {
        asyncRunner.postQuickTask(
            ClearStatusTask(accountManager.currentOwnCloudAccount?.savedAccount, context),
            { dismiss(it) }
        )
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
                    binding.customStatusInput.text.toString(),
                    binding.emoji.text.toString(),
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
        } else {
            DisplayUtils.showSnackMessage(view, view?.resources?.getString(R.string.error_setting_status_message))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SetStatusMessageBottomSheetBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onClick(predefinedStatus: PredefinedStatus) {
        val oldListener = binding.clearStatusAfterSpinner.onItemSelectedListener
        binding.clearStatusAfterSpinner.onItemSelectedListener = null

        selectedPredefinedMessageId = predefinedStatus.id
        clearAt = clearAtToUnixTime(predefinedStatus.clearAt)
        binding.emoji.setText(predefinedStatus.icon)
        binding.customStatusInput.text?.clear()
        binding.customStatusInput.text?.append(predefinedStatus.message)

        binding.remainingClearTime.visibility = View.GONE
        binding.clearStatusAfterSpinner.visibility = View.VISIBLE
        binding.clearStatusMessageTextView.text = getString(R.string.clear_status_after)

        val clearAt = predefinedStatus.clearAt
        if (clearAt == null) {
            binding.clearStatusAfterSpinner.setSelection(0)
        } else {
            when (clearAt.type) {
                CLEAR_AT_TYPE_PERIOD -> updateClearAtViewsForPeriod(clearAt)
                CLEAR_AT_TYPE_END_OF -> updateClearAtViewsForEndOf(clearAt)
            }
        }

        setClearStatusAfterValue(binding.clearStatusAfterSpinner.selectedItemPosition)
        binding.clearStatusAfterSpinner.onItemSelectedListener = oldListener
    }

    private fun updateClearAtViewsForPeriod(clearAt: ClearAt) {
        when (clearAt.time) {
            "900" -> binding.clearStatusAfterSpinner.setSelection(POS_FIFTEEN_MINUTES)
            "1800" -> binding.clearStatusAfterSpinner.setSelection(POS_HALF_AN_HOUR)
            "3600" -> binding.clearStatusAfterSpinner.setSelection(POS_AN_HOUR)
            "14400" -> binding.clearStatusAfterSpinner.setSelection(POS_FOUR_HOURS)
            else -> binding.clearStatusAfterSpinner.setSelection(POS_DONT_CLEAR)
        }
    }

    private fun updateClearAtViewsForEndOf(clearAt: ClearAt) {
        when (clearAt.time) {
            "day" -> binding.clearStatusAfterSpinner.setSelection(POS_TODAY)
            "week" -> binding.clearStatusAfterSpinner.setSelection(POS_END_OF_WEEK)
            else -> binding.clearStatusAfterSpinner.setSelection(POS_DONT_CLEAR)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @VisibleForTesting
    fun setPredefinedStatus(predefinedStatus: ArrayList<PredefinedStatus>) {
        adapter.list = predefinedStatus
        binding.predefinedStatusList.adapter?.notifyDataSetChanged()
    }
}
