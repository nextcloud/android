/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.contactsbackup

import android.Manifest
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.DatePicker
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
import com.nextcloud.utils.extensions.getSerializableArgument
import com.nextcloud.utils.extensions.getTypedActivity
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.BackupFragmentBinding
import com.owncloud.android.datamodel.ArbitraryDataProvider
import com.owncloud.android.datamodel.FileDataStorageManager
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.operations.RefreshFolderOperation
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.fragment.FileFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.MimeTypeUtil
import com.owncloud.android.utils.PermissionUtil.checkSelfPermission
import com.owncloud.android.utils.theme.ThemeUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import third_parties.daveKoeller.AlphanumComparator
import java.util.Calendar
import java.util.GregorianCalendar
import javax.inject.Inject

@Suppress("TooManyFunctions")
class BackupFragment :
    FileFragment(),
    OnDateSetListener,
    Injectable {

    @Inject lateinit var backgroundJobManager: BackgroundJobManager

    @Inject lateinit var themeUtils: ThemeUtils

    @Inject lateinit var arbitraryDataProvider: ArbitraryDataProvider

    @Inject lateinit var viewThemeUtils: ViewThemeUtils

    private lateinit var binding: BackupFragmentBinding
    private lateinit var user: User
    private lateinit var contactsBackupFolderPath: String
    private lateinit var calendarBackupFolderPath: String
    private lateinit var contactsCheckedListener: CompoundButton.OnCheckedChangeListener
    private lateinit var calendarCheckedListener: CompoundButton.OnCheckedChangeListener

    private var selectedDate: Calendar? = null
    private var calendarPickerOpen = false
    private var datePickerDialog: DatePickerDialog? = null
    private var showSidebar = true
    private var showCalendarBackup = true

    private var isCalendarBackupEnabled: Boolean
        get() = arbitraryDataProvider.getBooleanValue(user, PREFERENCE_CALENDAR_BACKUP_ENABLED)
        set(enabled) = arbitraryDataProvider.storeOrUpdateKeyValue(
            user.accountName,
            PREFERENCE_CALENDAR_BACKUP_ENABLED,
            enabled
        )

    private var isContactsBackupEnabled: Boolean
        get() = arbitraryDataProvider.getBooleanValue(user, PREFERENCE_CONTACTS_BACKUP_ENABLED)
        set(enabled) = arbitraryDataProvider.storeOrUpdateKeyValue(
            user.accountName,
            PREFERENCE_CONTACTS_BACKUP_ENABLED,
            enabled
        )

    //region Lifecycle

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        if (themeUtils.themingEnabled(context)) requireContext().theme.applyStyle(R.style.FallbackThemingTheme, true)

        binding = BackupFragmentBinding.inflate(inflater, container, false)
        contactsBackupFolderPath = getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR
        calendarBackupFolderPath = getString(R.string.calendar_backup_folder) + OCFile.PATH_SEPARATOR
        showSidebar = arguments?.getBoolean(ARG_SHOW_SIDEBAR) ?: true
        showCalendarBackup = resources.getBoolean(R.bool.show_calendar_backup)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val contactsPreferenceActivity = requireActivity() as ContactsPreferenceActivity
        user = contactsPreferenceActivity.user.orElseThrow { RuntimeException() }

        setupSwitches(user)
        setupCheckListeners()
        setBackupNowButtonVisibility()
        setOnClickListeners()
        displayLastBackup(contactsPreferenceActivity)
        applyUserColorToActionBar(contactsPreferenceActivity)
        setupDates(savedInstanceState)
        applyUserColor()
        addMenuProvider()
    }

    override fun onResume() {
        super.onResume()
        if (calendarPickerOpen) openDate(selectedDate)
        (activity as? ContactsPreferenceActivity)?.let { refreshBackupFolder(it.storageManager) }
    }

    override fun onStop() {
        super.onStop()
        datePickerDialog?.dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val dialog = datePickerDialog?.takeIf { it.isShowing } ?: return
        outState.putBoolean(KEY_CALENDAR_PICKER_OPEN, true)
        dialog.datePicker.let {
            outState.putSerializable(KEY_CALENDAR_DATE, GregorianCalendar(it.year, it.month, it.dayOfMonth))
        }
    }

    //endregion

    //region Setup

    private fun setupSwitches(user: User) {
        binding.dailyBackup.isChecked = arbitraryDataProvider.getBooleanValue(
            user,
            ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP
        )
        binding.contacts.isChecked = isContactsBackupEnabled && checkContactBackupPermission()
        binding.calendar.isChecked = isCalendarBackupEnabled && checkCalendarBackupPermission(requireContext())
        binding.calendar.visibility = if (showCalendarBackup) View.VISIBLE else View.GONE
    }

    private fun setupCheckListeners() {
        binding.dailyBackup.setOnCheckedChangeListener { _, isChecked ->
            if (checkAndAskForContactsReadPermission()) setAutomaticBackup(isChecked)
        }
        contactsCheckedListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            isContactsBackupEnabled = isChecked && checkAndAskForContactsReadPermission()
            setBackupNowButtonVisibility()
            setAutomaticBackup(binding.dailyBackup.isChecked)
        }
        calendarCheckedListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
            isCalendarBackupEnabled = isChecked && checkAndAskForCalendarReadPermission()
            setBackupNowButtonVisibility()
            setAutomaticBackup(binding.dailyBackup.isChecked)
        }
        binding.contacts.setOnCheckedChangeListener(contactsCheckedListener)
        binding.calendar.setOnCheckedChangeListener(calendarCheckedListener)
    }

    private fun setBackupNowButtonVisibility() {
        binding.backupNow.isEnabled = binding.contacts.isChecked || binding.calendar.isChecked
    }

    private fun setOnClickListeners() {
        binding.backupNow.setOnClickListener { backupNow() }
        binding.contactsDatepicker.setOnClickListener { openCleanDate() }
    }

    private fun displayLastBackup(contactsPreferenceActivity: ContactsPreferenceActivity) {
        val lastBackupTimestamp = arbitraryDataProvider.getLongValue(
            user,
            ContactsPreferenceActivity.PREFERENCE_CONTACTS_LAST_BACKUP
        )
        if (lastBackupTimestamp == -1L) {
            binding.lastBackupWithDate.visibility = View.GONE
            return
        }
        binding.lastBackupWithDate.text = getString(
            R.string.last_backup,
            DisplayUtils.getRelativeTimestamp(contactsPreferenceActivity, lastBackupTimestamp)
        )
    }

    private fun applyUserColorToActionBar(activity: ContactsPreferenceActivity) {
        activity.supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            viewThemeUtils.files.themeActionBar(
                requireContext(),
                actionBar,
                if (showCalendarBackup) R.string.backup_title else R.string.contact_backup_title
            )
        }
    }

    private fun setupDates(savedInstanceState: Bundle?) {
        savedInstanceState?.takeIf { it.getBoolean(KEY_CALENDAR_PICKER_OPEN, false) }?.let {
            selectedDate = it.getSerializableArgument(KEY_CALENDAR_DATE, Calendar::class.java)
            calendarPickerOpen = true
        }
    }

    private fun applyUserColor() {
        viewThemeUtils.androidx.run {
            colorSwitchCompat(binding.contacts)
            colorSwitchCompat(binding.calendar)
            colorSwitchCompat(binding.dailyBackup)
        }

        viewThemeUtils.material.run {
            colorMaterialButtonPrimaryFilled(binding.backupNow)
            colorMaterialButtonPrimaryOutlined(binding.contactsDatepicker)
        }

        viewThemeUtils.platform.run {
            colorTextView(binding.dataToBackUpTitle)
            colorTextView(binding.backupSettingsTitle)
        }
    }

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) = Unit
                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    if (menuItem.itemId != android.R.id.home) return false
                    return handleHomeMenuAction(getTypedActivity(ContactsPreferenceActivity::class.java))
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    private fun handleHomeMenuAction(activity: ContactsPreferenceActivity?): Boolean {
        when {
            showSidebar -> if (activity?.isDrawerOpen == true) activity.closeDrawer() else activity?.openDrawer()
            activity != null -> activity.finish()
            else -> startActivity(Intent(context, SettingsActivity::class.java))
        }
        return true
    }

    //endregion

    //region Backup operations

    private fun backupNow() {
        val activity = getTypedActivity(ContactsPreferenceActivity::class.java) ?: return
        val user = activity.user?.takeIf { it.isPresent }?.get() ?: return

        if (isContactsBackupEnabled && checkContactBackupPermission()) {
            backgroundJobManager.startImmediateContactsBackup(user)
        }
        if (showCalendarBackup && isCalendarBackupEnabled && checkCalendarBackupPermission(requireContext())) {
            backgroundJobManager.startImmediateCalendarBackup(user)
        }
        DisplayUtils.showSnackMessage(this, R.string.contacts_preferences_backup_scheduled)
    }

    private fun setAutomaticBackup(enabled: Boolean) {
        val activity = getTypedActivity(ContactsPreferenceActivity::class.java) ?: return
        val user = activity.user?.takeIf { it.isPresent }?.get() ?: return

        if (enabled) {
            if (isContactsBackupEnabled) {
                Log_OC.d(TAG, "Scheduling contacts backup job")
                backgroundJobManager.schedulePeriodicContactsBackup(user)
            } else {
                Log_OC.d(TAG, "Cancelling contacts backup job")
                backgroundJobManager.cancelPeriodicContactsBackup(user)
            }
            if (isCalendarBackupEnabled) {
                Log_OC.d(TAG, "Scheduling calendar backup job")
                backgroundJobManager.schedulePeriodicCalendarBackup(user)
            } else {
                Log_OC.d(TAG, "Cancelling calendar backup job")
                backgroundJobManager.cancelPeriodicCalendarBackup(user)
            }
        } else {
            Log_OC.d(TAG, "Cancelling all backup jobs")
            backgroundJobManager.cancelPeriodicContactsBackup(user)
            backgroundJobManager.cancelPeriodicCalendarBackup(user)
        }
        arbitraryDataProvider.storeOrUpdateKeyValue(
            user.accountName,
            ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
            enabled.toString()
        )
    }

    private fun refreshBackupFolder(storageManager: FileDataStorageManager) {
        lifecycleScope.launch {
            val backupFiles = listOf(calendarBackupFolderPath, contactsBackupFolderPath)
                .mapNotNull { path -> storageManager.getFileByDecryptedRemotePath(path) }
                .flatMap { folder -> fetchBackupFiles(folder, storageManager) }
                .sortedWith(AlphanumComparator())
            withContext(Dispatchers.Main) {
                binding.contactsDatepicker.setVisibleIf(backupFiles.isNotEmpty())
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchBackupFiles(folder: OCFile, storageManager: FileDataStorageManager): List<OCFile> =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val result = RefreshFolderOperation(
                    folder,
                    System.currentTimeMillis(),
                    false,
                    false,
                    storageManager,
                    user,
                    context
                ).execute(user, context)
                if (result.isSuccess) storageManager.getFolderContent(folder, false) else emptyList()
            } catch (e: Exception) {
                Log_OC.d(TAG, "Exception fetchBackupFiles: $e")
                emptyList()
            }
        }

    private fun getBackupFiles(): List<OCFile> {
        val storageManager = (activity as? ContactsPreferenceActivity)?.storageManager ?: return emptyList()
        val contactsFolder = storageManager.getFileByDecryptedRemotePath(contactsBackupFolderPath)
        val calendarFolder = storageManager.getFileByDecryptedRemotePath(calendarBackupFolderPath)
        return buildList {
            addAll(storageManager.getFolderContent(contactsFolder, false))
            addAll(storageManager.getFolderContent(calendarFolder, false))
        }
    }

    //endregion

    //region Date picker

    private fun openCleanDate() {
        if (checkAndAskForCalendarReadPermission() && checkAndAskForContactsReadPermission()) openDate(null)
    }

    private fun openDate(savedDate: Calendar?) {
        val contactsPreferenceActivity = activity as? ContactsPreferenceActivity ?: run {
            activity?.let { DisplayUtils.showSnackMessage(it, R.string.error_choosing_date) }
            return
        }
        val backupFiles = getBackupFiles().sortedBy { it.modificationTimestamp }
        if (backupFiles.isEmpty()) {
            DisplayUtils.showSnackMessage(
                this,
                R.string.contacts_preferences_something_strange_happened
            )
            return
        }
        val cal = savedDate ?: Calendar.getInstance()
        datePickerDialog = DatePickerDialog(
            contactsPreferenceActivity,
            this,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.maxDate = backupFiles.last().modificationTimestamp
            datePicker.minDate = backupFiles.first().modificationTimestamp
            setOnDismissListener { selectedDate = null }
            setTitle("")
            show()

            viewThemeUtils.platform.colorTextButtons(
                getButton(DatePickerDialog.BUTTON_NEGATIVE),
                getButton(DatePickerDialog.BUTTON_POSITIVE)
            )
        }
    }

    @Suppress("ComplexMethod", "MagicNumber")
    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        val contactsPreferenceActivity = activity as? ContactsPreferenceActivity ?: run {
            activity?.let { DisplayUtils.showSnackMessage(it, R.string.error_choosing_date) }
            return
        }
        selectedDate = GregorianCalendar(year, month, dayOfMonth)
        val backupFiles = getBackupFiles()
        val (start, end) = calculateDayRange(year, month, dayOfMonth)
        val backupToRestore = collectFilesForRestore(backupFiles, start, end)

        if (backupToRestore.isEmpty()) {
            DisplayUtils.showSnackMessage(
                this,
                R.string.contacts_preferences_no_file_found
            )
            return
        }
        val user = contactsPreferenceActivity.user.orElseThrow { RuntimeException() }
        val fragment = BackupListFragment.newInstance(backupToRestore.toTypedArray(), user)
        contactsPreferenceActivity.supportFragmentManager.beginTransaction()
            .replace(R.id.frame_container, fragment, BackupListFragment.TAG)
            .addToBackStack(ContactsPreferenceActivity.BACKUP_TO_LIST)
            .commit()
    }

    @Suppress("MagicNumber")
    private fun calculateDayRange(year: Int, month: Int, dayOfMonth: Int): Pair<Long, Long> {
        val date = Calendar.getInstance().apply { set(year, month, dayOfMonth) }.apply {
            set(Calendar.HOUR, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 1)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.AM_PM, Calendar.AM)
        }

        val start = date.timeInMillis

        date.set(Calendar.HOUR, 23)
        date.set(Calendar.MINUTE, 59)
        date.set(Calendar.SECOND, 59)

        return start to date.timeInMillis
    }

    private fun collectFilesForRestore(backupFiles: List<OCFile>, start: Long, end: Long): List<OCFile> {
        val inRange = backupFiles.filter { it.modificationTimestamp in (start + 1)..<end }
        val contactBackup = inRange.filter { MimeTypeUtil.isVCard(it) }.maxByOrNull { it.modificationTimestamp }
        val calendarBackups = if (showCalendarBackup) inRange.filter { MimeTypeUtil.isCalendar(it) } else emptyList()
        return listOfNotNull(contactBackup) + calendarBackups
    }

    //endregion

    //region Permissions

    private fun checkAndAskForContactsReadPermission(): Boolean {
        if (checkSelfPermission(requireActivity(), Manifest.permission.READ_CONTACTS)) return true
        requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        return false
    }

    private fun checkAndAskForCalendarReadPermission(): Boolean {
        if (checkCalendarBackupPermission(requireActivity())) return true
        requestCalendarPermissionLauncher.launch(
            arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        )
        return false
    }

    private fun checkCalendarBackupPermission(context: Context): Boolean =
        checkSelfPermission(context, Manifest.permission.READ_CALENDAR) &&
            checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR)

    private fun checkContactBackupPermission(): Boolean =
        checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)

    private fun resetSwitch(switch: CompoundButton, listener: CompoundButton.OnCheckedChangeListener) {
        switch.setOnCheckedChangeListener(null)
        switch.isChecked = false
        switch.setOnCheckedChangeListener(listener)
    }

    private val requestContactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) isContactsBackupEnabled = true else resetSwitch(binding.contacts, contactsCheckedListener)
            setBackupNowButtonVisibility()
            setAutomaticBackup(binding.dailyBackup.isChecked)
        }

    private val requestCalendarPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val readGranted = permissions[Manifest.permission.READ_CALENDAR] == true
            val writeGranted = permissions[Manifest.permission.WRITE_CALENDAR] == true
            if (readGranted && writeGranted) {
                isCalendarBackupEnabled = true
            } else {
                resetSwitch(binding.calendar, calendarCheckedListener)
            }
            setBackupNowButtonVisibility()
            setAutomaticBackup(binding.dailyBackup.isChecked)
        }

    //endregion

    companion object {
        val TAG: String = BackupFragment::class.java.simpleName
        private const val ARG_SHOW_SIDEBAR = "SHOW_SIDEBAR"
        private const val KEY_CALENDAR_PICKER_OPEN = "IS_CALENDAR_PICKER_OPEN"
        private const val KEY_CALENDAR_DATE = "CALENDAR_DATE"
        const val PREFERENCE_CONTACTS_BACKUP_ENABLED = "PREFERENCE_CONTACTS_BACKUP_ENABLED"
        const val PREFERENCE_CALENDAR_BACKUP_ENABLED = "PREFERENCE_CALENDAR_BACKUP_ENABLED"

        @JvmStatic
        fun create(showSidebar: Boolean): BackupFragment = BackupFragment().apply {
            arguments = Bundle().apply { putBoolean(ARG_SHOW_SIDEBAR, showSidebar) }
        }
    }
}
