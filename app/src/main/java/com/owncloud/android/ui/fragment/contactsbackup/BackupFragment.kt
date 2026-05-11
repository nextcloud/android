/*
 * Nextcloud - Android Client
 *
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
import com.owncloud.android.utils.PermissionUtil
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
    private lateinit var binding: BackupFragmentBinding

    @Inject
    lateinit var backgroundJobManager: BackgroundJobManager

    @Inject
    lateinit var themeUtils: ThemeUtils

    @Inject
    lateinit var arbitraryDataProvider: ArbitraryDataProvider

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    private var selectedDate: Calendar? = null
    private var calendarPickerOpen = false
    private var datePickerDialog: DatePickerDialog? = null
    private lateinit var contactsCheckedListener: CompoundButton.OnCheckedChangeListener
    private lateinit var calendarCheckedListener: CompoundButton.OnCheckedChangeListener
    private lateinit var user: User
    private var showSidebar = true

    // flag to check if calendar backup should be shown and backup should be done or not
    private var showCalendarBackup = true
    private var isCalendarBackupEnabled: Boolean
        get() = arbitraryDataProvider.getBooleanValue(user, PREFERENCE_CALENDAR_BACKUP_ENABLED)
        set(enabled) {
            arbitraryDataProvider.storeOrUpdateKeyValue(
                user.accountName,
                PREFERENCE_CALENDAR_BACKUP_ENABLED,
                enabled
            )
        }

    private var isContactsBackupEnabled: Boolean
        get() = arbitraryDataProvider.getBooleanValue(user, PREFERENCE_CONTACTS_BACKUP_ENABLED)
        set(enabled) {
            arbitraryDataProvider.storeOrUpdateKeyValue(
                user.accountName,
                PREFERENCE_CONTACTS_BACKUP_ENABLED,
                enabled
            )
        }

    private lateinit var contactsBackupFolderPath: String
    private lateinit var calendarBackupFolderPath: String

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // use grey as fallback for elements where custom theming is not available
        if (themeUtils.themingEnabled(context)) {
            requireContext().theme.applyStyle(R.style.FallbackThemingTheme, true)
        }

        binding = BackupFragmentBinding.inflate(inflater, container, false)

        contactsBackupFolderPath = getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR
        calendarBackupFolderPath = getString(R.string.calendar_backup_folder) + OCFile.PATH_SEPARATOR

        val view: View = binding.root

        if (arguments != null) {
            showSidebar = requireArguments().getBoolean(ARG_SHOW_SIDEBAR)
        }

        showCalendarBackup = resources.getBoolean(R.bool.show_calendar_backup)

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
        return view
    }

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
        binding.dailyBackup.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
            if (checkAndAskForContactsReadPermission()) {
                setAutomaticBackup(isChecked)
            }
        }

        initContactsCheckedListener()
        binding.contacts.setOnCheckedChangeListener(contactsCheckedListener)

        initCalendarCheckedListener()
        binding.calendar.setOnCheckedChangeListener(calendarCheckedListener)
    }

    private fun initContactsCheckedListener() {
        contactsCheckedListener =
            CompoundButton.OnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                isContactsBackupEnabled = isChecked && checkAndAskForContactsReadPermission()
                setBackupNowButtonVisibility()
                setAutomaticBackup(binding.dailyBackup.isChecked)
            }
    }

    private fun initCalendarCheckedListener() {
        calendarCheckedListener =
            CompoundButton.OnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                isCalendarBackupEnabled = isChecked && checkAndAskForCalendarReadPermission()
                setBackupNowButtonVisibility()
                setAutomaticBackup(binding.dailyBackup.isChecked)
            }
    }

    private fun setBackupNowButtonVisibility() {
        binding.run {
            backupNow.isEnabled = (contacts.isChecked || calendar.isChecked)
        }
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
        } else {
            binding.lastBackupWithDate.text = String.format(
                getString(R.string.last_backup),
                DisplayUtils.getRelativeTimestamp(contactsPreferenceActivity, lastBackupTimestamp)
            )
        }
    }

    private fun applyUserColorToActionBar(contactsPreferenceActivity: ContactsPreferenceActivity) {
        contactsPreferenceActivity.supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(true)
            viewThemeUtils.files.themeActionBar(
                requireContext(),
                actionBar,
                if (showCalendarBackup) R.string.backup_title else R.string.contact_backup_title
            )
        }
    }

    private fun setupDates(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_CALENDAR_PICKER_OPEN, false)) {
            savedInstanceState.getSerializableArgument(KEY_CALENDAR_DATE, Calendar::class.java)?.let {
                selectedDate = it
            }
            calendarPickerOpen = true
        }
    }

    private fun applyUserColor() {
        viewThemeUtils.androidx.colorSwitchCompat(binding.contacts)
        viewThemeUtils.androidx.colorSwitchCompat(binding.calendar)
        viewThemeUtils.androidx.colorSwitchCompat(binding.dailyBackup)

        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(binding.backupNow)
        viewThemeUtils.material.colorMaterialButtonPrimaryOutlined(binding.contactsDatepicker)

        viewThemeUtils.platform.colorTextView(binding.dataToBackUpTitle)
        viewThemeUtils.platform.colorTextView(binding.backupSettingsTitle)
    }

    override fun onResume() {
        super.onResume()

        if (calendarPickerOpen) {
            openDate(selectedDate)
        }

        (activity as? ContactsPreferenceActivity)?.let {
            refreshBackupFolder(it.storageManager)
        }
    }

    private fun refreshBackupFolder(storageManager: FileDataStorageManager) {
        lifecycleScope.launch {
            val backupFiles = listOf(calendarBackupFolderPath, contactsBackupFolderPath)
                .mapNotNull { path -> storageManager.getFileByDecryptedRemotePath(path) }
                .flatMap { folder -> fetchBackupFiles(folder, storageManager) }
                .toMutableList()
                .also {
                    it.sortWith(AlphanumComparator())
                }

            withContext(Dispatchers.Main) {
                binding.contactsDatepicker.setVisibleIf(backupFiles.isNotEmpty())
            }
        }
    }

    /**
     * Returns backup files from database
     */
    private fun getBackupFiles(): List<OCFile> {
        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?
        val storageManager = contactsPreferenceActivity?.storageManager ?: return listOf()
        val contactsBackupFolder = storageManager.getFileByDecryptedRemotePath(contactsBackupFolderPath)
        val calendarBackupFolder = storageManager.getFileByDecryptedRemotePath(calendarBackupFolderPath)

        val backupFiles = storageManager.getFolderContent(contactsBackupFolder, false)
        backupFiles.addAll(storageManager.getFolderContent(calendarBackupFolder, false))
        return backupFiles
    }

    /**
     * Refreshes the folder and returns updated backup files
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchBackupFiles(folder: OCFile, storageManager: FileDataStorageManager): List<OCFile> =
        withContext(Dispatchers.IO) {
            try {
                val operation = RefreshFolderOperation(
                    folder,
                    System.currentTimeMillis(),
                    false,
                    false,
                    storageManager,
                    user,
                    context
                )

                @Suppress("DEPRECATION")
                val result = operation.execute(user, context)

                if (result.isSuccess) {
                    Log_OC.d(TAG, "Backup files fetched")
                    storageManager.getFolderContent(folder, false)
                } else {
                    Log_OC.d(TAG, "Backup files cannot be fetched")
                    listOf()
                }
            } catch (e: Exception) {
                Log_OC.d(TAG, "Exception fetchBackupFiles: $e")
                listOf()
            }
        }

    private fun addMenuProvider() {
        requireActivity().addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    val activity = getTypedActivity(ContactsPreferenceActivity::class.java)

                    if (menuItem.itemId == android.R.id.home) {
                        if (showSidebar) {
                            if (activity?.isDrawerOpen == true) {
                                activity.closeDrawer()
                            } else {
                                activity?.openDrawer()
                            }
                        } else if (activity != null) {
                            activity.finish()
                        } else {
                            val settingsIntent = Intent(context, SettingsActivity::class.java)
                            startActivity(settingsIntent)
                        }
                        return true
                    }

                    return false
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED
        )
    }

    // region permission handling
    private fun checkAndAskForContactsReadPermission(): Boolean {
        return if (checkSelfPermission(requireActivity(), Manifest.permission.READ_CONTACTS)) {
            true
        } else {
            requestContactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            false
        }
    }

    private fun checkAndAskForCalendarReadPermission(): Boolean {
        return if (checkCalendarBackupPermission(requireActivity())) {
            true
        } else {
            requestCalendarPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                )
            )
            false
        }
    }

    private val requestContactsPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                isContactsBackupEnabled = true
            } else {
                binding.contacts.setOnCheckedChangeListener(null)
                binding.contacts.isChecked = false
                binding.contacts.setOnCheckedChangeListener(contactsCheckedListener)
            }
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
                binding.calendar.setOnCheckedChangeListener(null)
                binding.calendar.isChecked = false
                binding.calendar.setOnCheckedChangeListener(calendarCheckedListener)
            }
            setBackupNowButtonVisibility()
            setAutomaticBackup(binding.dailyBackup.isChecked)
        }
    // endregion

    @Deprecated("Deprecated in Java")
    @Suppress("NestedBlockDepth")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PermissionUtil.PERMISSIONS_READ_CONTACTS_AUTOMATIC) {
            for (index in permissions.indices) {
                if (Manifest.permission.READ_CONTACTS.equals(permissions[index], ignoreCase = true)) {
                    if (grantResults[index] >= 0) {
                        // if approved, exit for loop
                        isContactsBackupEnabled = true
                        break
                    }

                    // if not accepted, disable again
                    binding.contacts.setOnCheckedChangeListener(null)
                    binding.contacts.isChecked = false
                    binding.contacts.setOnCheckedChangeListener(contactsCheckedListener)
                }
            }
        }
        if (requestCode == PermissionUtil.PERMISSIONS_READ_CALENDAR_AUTOMATIC) {
            var readGranted = false
            var writeGranted = false
            for (index in permissions.indices) {
                if (Manifest.permission.WRITE_CALENDAR.equals(
                        permissions[index],
                        ignoreCase = true
                    ) &&
                    grantResults[index] >= 0
                ) {
                    writeGranted = true
                } else if (Manifest.permission.READ_CALENDAR.equals(
                        permissions[index],
                        ignoreCase = true
                    ) &&
                    grantResults[index] >= 0
                ) {
                    readGranted = true
                }
            }
            if (!readGranted || !writeGranted) {
                // if not accepted, disable again
                binding.calendar.setOnCheckedChangeListener(null)
                binding.calendar.isChecked = false
                binding.calendar.setOnCheckedChangeListener(calendarCheckedListener)
            } else {
                isCalendarBackupEnabled = true
            }
        }
        setBackupNowButtonVisibility()
        setAutomaticBackup(binding.dailyBackup.isChecked)
    }

    private fun backupNow() {
        val activity = getTypedActivity(ContactsPreferenceActivity::class.java) ?: return
        val user = activity.user?.takeIf { it.isPresent }?.get() ?: return

        if (isContactsBackupEnabled && checkContactBackupPermission()) {
            backgroundJobManager.startImmediateContactsBackup(user)
        }

        if (showCalendarBackup && isCalendarBackupEnabled && checkCalendarBackupPermission(requireContext())) {
            backgroundJobManager.startImmediateCalendarBackup(user)
        }

        DisplayUtils.showSnackMessage(
            this,
            R.string.contacts_preferences_backup_scheduled
        )
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

    private fun checkCalendarBackupPermission(context: Context): Boolean =
        checkSelfPermission(context, Manifest.permission.READ_CALENDAR) &&
            checkSelfPermission(
                context,
                Manifest.permission.WRITE_CALENDAR
            )

    private fun checkContactBackupPermission(): Boolean =
        checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)

    private fun openCleanDate() {
        if (checkAndAskForCalendarReadPermission() && checkAndAskForContactsReadPermission()) {
            openDate(null)
        }
    }

    private fun openDate(savedDate: Calendar?) {
        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?
        if (contactsPreferenceActivity == null) {
            activity?.let {
                DisplayUtils.showSnackMessage(it, R.string.error_choosing_date)
            }
            return
        }

        val backupFiles = getBackupFiles().toMutableList()
        backupFiles.sortBy { it.modificationTimestamp }

        if (backupFiles.isNotEmpty() && backupFiles.last() != null) {
            val cal = savedDate ?: Calendar.getInstance()

            datePickerDialog = DatePickerDialog(
                contactsPreferenceActivity,
                this,
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )

            datePickerDialog?.apply {
                datePicker.maxDate = backupFiles.last().modificationTimestamp
                datePicker.minDate = backupFiles.first().modificationTimestamp
                setOnDismissListener { selectedDate = null }
                setTitle("")
                show()
            }

            viewThemeUtils.platform.colorTextButtons(
                datePickerDialog!!.getButton(DatePickerDialog.BUTTON_NEGATIVE),
                datePickerDialog!!.getButton(DatePickerDialog.BUTTON_POSITIVE)
            )
        } else {
            DisplayUtils.showSnackMessage(
                requireView().findViewById<View>(R.id.contacts_linear_layout),
                R.string.contacts_preferences_something_strange_happened
            )
        }
    }

    override fun onStop() {
        super.onStop()

        datePickerDialog?.dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        datePickerDialog?.let { dialog ->
            outState.putBoolean(KEY_CALENDAR_PICKER_OPEN, dialog.isShowing)

            if (dialog.isShowing) {
                dialog.datePicker.let {
                    outState.putSerializable(KEY_CALENDAR_DATE, GregorianCalendar(it.year, it.month, it.dayOfMonth))
                }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth", "ComplexMethod", "LongMethod", "MagicNumber")
    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?
        if (contactsPreferenceActivity == null) {
            activity?.let {
                DisplayUtils.showSnackMessage(it, R.string.error_choosing_date)
            }
            return
        }

        selectedDate = GregorianCalendar(year, month, dayOfMonth)
        val backupFiles = getBackupFiles()

        // find file with modification with date and time between 00:00 and 23:59
        // if more than one file exists, take oldest
        val date = Calendar.getInstance()
        date[year, month] = dayOfMonth

        // start
        date[Calendar.HOUR] = 0
        date[Calendar.MINUTE] = 0
        date[Calendar.SECOND] = 1
        date[Calendar.MILLISECOND] = 0
        date[Calendar.AM_PM] = Calendar.AM
        val start = date.timeInMillis

        // end
        date[Calendar.HOUR] = 23
        date[Calendar.MINUTE] = 59
        date[Calendar.SECOND] = 59
        val end = date.timeInMillis
        var contactsBackupToRestore: OCFile? = null
        val calendarBackupsToRestore: MutableList<OCFile> = ArrayList()
        for (file in backupFiles) {
            if (file.modificationTimestamp in (start + 1)..<end) {
                // contact
                if (MimeTypeUtil.isVCard(file)) {
                    if (contactsBackupToRestore == null) {
                        contactsBackupToRestore = file
                    } else if (contactsBackupToRestore.modificationTimestamp < file.modificationTimestamp) {
                        contactsBackupToRestore = file
                    }
                }

                // calendars
                if (showCalendarBackup && MimeTypeUtil.isCalendar(file)) {
                    calendarBackupsToRestore.add(file)
                }
            }
        }
        val backupToRestore: MutableList<OCFile> = ArrayList()
        if (contactsBackupToRestore != null) {
            backupToRestore.add(contactsBackupToRestore)
        }
        backupToRestore.addAll(calendarBackupsToRestore)
        if (backupToRestore.isEmpty()) {
            DisplayUtils.showSnackMessage(
                requireView().findViewById<View>(R.id.contacts_linear_layout),
                R.string.contacts_preferences_no_file_found
            )
        } else {
            val user = contactsPreferenceActivity.user.orElseThrow { RuntimeException() }
            val contactListFragment = BackupListFragment.newInstance(backupToRestore.toTypedArray(), user)

            contactsPreferenceActivity.supportFragmentManager.beginTransaction()
                .replace(R.id.frame_container, contactListFragment, BackupListFragment.TAG)
                .addToBackStack(ContactsPreferenceActivity.BACKUP_TO_LIST)
                .commit()
        }
    }

    companion object {
        val TAG: String = BackupFragment::class.java.simpleName
        private const val ARG_SHOW_SIDEBAR = "SHOW_SIDEBAR"
        private const val KEY_CALENDAR_PICKER_OPEN = "IS_CALENDAR_PICKER_OPEN"
        private const val KEY_CALENDAR_DATE = "CALENDAR_DATE"
        const val PREFERENCE_CONTACTS_BACKUP_ENABLED = "PREFERENCE_CONTACTS_BACKUP_ENABLED"
        const val PREFERENCE_CALENDAR_BACKUP_ENABLED = "PREFERENCE_CALENDAR_BACKUP_ENABLED"

        @JvmStatic
        fun create(showSidebar: Boolean): BackupFragment = BackupFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_SHOW_SIDEBAR, showSidebar)
            }
        }
    }
}
