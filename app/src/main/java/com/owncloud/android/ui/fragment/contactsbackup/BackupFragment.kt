/*
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * @author TSI-mc
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH.
 * Copyright (C) 2023 TSI-mc
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
package com.owncloud.android.ui.fragment.contactsbackup

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DatePickerDialog.OnDateSetListener
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.DatePicker
import android.widget.Toast
import com.nextcloud.client.account.User
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.jobs.BackgroundJobManager
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
import third_parties.daveKoeller.AlphanumComparator
import java.util.Calendar
import java.util.Collections
import java.util.Date
import javax.inject.Inject

@Suppress("TooManyFunctions")
class BackupFragment : FileFragment(), OnDateSetListener, Injectable {
    private lateinit var binding: BackupFragmentBinding

    @JvmField
    @Inject
    var backgroundJobManager: BackgroundJobManager? = null

    @JvmField
    @Inject
    var themeUtils: ThemeUtils? = null

    @JvmField
    @Inject
    var arbitraryDataProvider: ArbitraryDataProvider? = null

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    private var selectedDate: Date? = null
    private var calendarPickerOpen = false
    private var datePickerDialog: DatePickerDialog? = null
    private var contactsCheckedListener: CompoundButton.OnCheckedChangeListener? = null
    private var calendarCheckedListener: CompoundButton.OnCheckedChangeListener? = null
    private var user: User? = null
    private var showSidebar = true

    // flag to check if calendar backup should be shown and backup should be done or not
    private var showCalendarBackup = true
    private var isCalendarBackupEnabled: Boolean
        get() = user?.let { arbitraryDataProvider?.getBooleanValue(it, PREFERENCE_CALENDAR_BACKUP_ENABLED) } ?: false
        private set(enabled) {
            arbitraryDataProvider!!.storeOrUpdateKeyValue(
                user!!.accountName,
                PREFERENCE_CALENDAR_BACKUP_ENABLED,
                enabled
            )
        }

    private var isContactsBackupEnabled: Boolean
        get() = user?.let { arbitraryDataProvider?.getBooleanValue(it, PREFERENCE_CONTACTS_BACKUP_ENABLED) } ?: false
        private set(enabled) {
            arbitraryDataProvider!!.storeOrUpdateKeyValue(
                user!!.accountName,
                PREFERENCE_CONTACTS_BACKUP_ENABLED,
                enabled
            )
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // use grey as fallback for elements where custom theming is not available
        if (themeUtils?.themingEnabled(context) == true) {
            requireContext().theme.applyStyle(R.style.FallbackThemingTheme, true)
        }

        binding = BackupFragmentBinding.inflate(inflater, container, false)
        val view: View = binding.root

        setHasOptionsMenu(true)

        if (arguments != null) {
            showSidebar = requireArguments().getBoolean(ARG_SHOW_SIDEBAR)
        }

        showCalendarBackup = requireContext().resources.getBoolean(R.bool.show_calendar_backup)

        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?
        user = contactsPreferenceActivity?.user?.orElseThrow { RuntimeException() }

        setupSwitches(user)

        setupCheckListeners()
        setBackupNowButtonVisibility()

        setOnClickListeners()

        contactsPreferenceActivity?.let {
            displayLastBackup(it)
            applyUserColorToActionBar(it)
        }

        setupDates(savedInstanceState)
        applyUserColor()

        return view
    }

    private fun setupSwitches(user: User?) {
        user?.let {
            binding.dailyBackup.isChecked = arbitraryDataProvider?.getBooleanValue(
                it,
                ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP
            ) ?: false
        }

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
                if (isChecked) {
                    if (checkAndAskForContactsReadPermission()) {
                        isContactsBackupEnabled = true
                    }
                } else {
                    isContactsBackupEnabled = false
                }
                setBackupNowButtonVisibility()
                setAutomaticBackup(binding.dailyBackup.isChecked)
            }
    }

    private fun initCalendarCheckedListener() {
        calendarCheckedListener =
            CompoundButton.OnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
                if (isChecked) {
                    if (checkAndAskForCalendarReadPermission()) {
                        isCalendarBackupEnabled = true
                    }
                } else {
                    isCalendarBackupEnabled = false
                }
                setBackupNowButtonVisibility()
                setAutomaticBackup(binding.dailyBackup.isChecked)
            }
    }

    private fun setBackupNowButtonVisibility() {
        binding.backupNow.visibility =
            if (binding.contacts.isChecked || binding.calendar.isChecked) View.VISIBLE else View.INVISIBLE
    }

    private fun setOnClickListeners() {
        binding.backupNow.setOnClickListener { backupNow() }
        binding.contactsDatepicker.setOnClickListener { openCleanDate() }
    }

    private fun displayLastBackup(contactsPreferenceActivity: ContactsPreferenceActivity) {
        val lastBackupTimestamp = user?.let {
            arbitraryDataProvider?.getLongValue(
                it,
                ContactsPreferenceActivity.PREFERENCE_CONTACTS_LAST_BACKUP
            )
        } ?: -1L

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
        val actionBar = contactsPreferenceActivity.supportActionBar

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true)
            viewThemeUtils?.files?.themeActionBar(
                requireContext(),
                actionBar,
                if (showCalendarBackup) R.string.backup_title else R.string.contact_backup_title
            )
        }
    }

    private fun setupDates(savedInstanceState: Bundle?) {
        if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_CALENDAR_PICKER_OPEN, false)) {
            if (savedInstanceState.getInt(KEY_CALENDAR_YEAR, -1) != -1 && savedInstanceState.getInt(
                    KEY_CALENDAR_MONTH,
                    -1
                ) != -1 && savedInstanceState.getInt(
                    KEY_CALENDAR_DAY, -1
                ) != -1
            ) {
                val cal = Calendar.getInstance()
                cal[Calendar.YEAR] = savedInstanceState.getInt(KEY_CALENDAR_YEAR)
                cal[Calendar.MONTH] = savedInstanceState.getInt(KEY_CALENDAR_MONTH)
                cal[Calendar.DAY_OF_MONTH] = savedInstanceState.getInt(KEY_CALENDAR_DAY)
                selectedDate = cal.time
            }
            calendarPickerOpen = true
        }
    }

    private fun applyUserColor() {
        viewThemeUtils?.androidx?.colorSwitchCompat(binding.contacts)
        viewThemeUtils?.androidx?.colorSwitchCompat(binding.calendar)
        viewThemeUtils?.androidx?.colorSwitchCompat(binding.dailyBackup)

        viewThemeUtils?.material?.colorMaterialButtonPrimaryFilled(binding.backupNow)
        viewThemeUtils?.material?.colorMaterialButtonPrimaryOutlined(binding.contactsDatepicker)

        viewThemeUtils?.platform?.colorTextView(binding.dataToBackUpTitle)
        viewThemeUtils?.platform?.colorTextView(binding.backupSettingsTitle)
    }

    override fun onResume() {
        super.onResume()

        if (calendarPickerOpen) {
            if (selectedDate != null) {
                openDate(selectedDate)
            } else {
                openDate(null)
            }
        }

        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?
        if (contactsPreferenceActivity != null) {
            val backupFolderPath = resources.getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR
            refreshBackupFolder(
                backupFolderPath,
                contactsPreferenceActivity.applicationContext,
                contactsPreferenceActivity.storageManager
            )
        }
    }

    private fun refreshBackupFolder(
        backupFolderPath: String,
        context: Context,
        storageManager: FileDataStorageManager
    ) {
        val task: AsyncTask<String, Int, Boolean> =
            @SuppressLint("StaticFieldLeak")
            object : AsyncTask<String, Int, Boolean>() {
                @Deprecated("Deprecated in Java")
                override fun doInBackground(vararg path: String): Boolean {
                    val folder = storageManager.getFileByPath(path[0])
                    return if (folder != null) {
                        val operation = RefreshFolderOperation(
                            folder,
                            System.currentTimeMillis(),
                            false,
                            false,
                            storageManager,
                            user,
                            context
                        )
                        val result = operation.execute(user, context)
                        result.isSuccess
                    } else {
                        java.lang.Boolean.FALSE
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onPostExecute(result: Boolean) {
                    if (result) {
                        val backupFolder = storageManager.getFileByPath(backupFolderPath)
                        val backupFiles = storageManager
                            .getFolderContent(backupFolder, false)
                        Collections.sort(backupFiles, AlphanumComparator())
                        if (backupFiles == null || backupFiles.isEmpty()) {
                            binding.contactsDatepicker.visibility = View.INVISIBLE
                        } else {
                            binding.contactsDatepicker.visibility = View.VISIBLE
                        }
                    }
                }
            }

        task.execute(backupFolderPath)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?

        val retval: Boolean
        when (item.itemId) {
            android.R.id.home -> {
                if (showSidebar) {
                    if (contactsPreferenceActivity!!.isDrawerOpen) {
                        contactsPreferenceActivity.closeDrawer()
                    } else {
                        contactsPreferenceActivity.openDrawer()
                    }
                } else if (activity != null) {
                    requireActivity().finish()
                } else {
                    val settingsIntent = Intent(context, SettingsActivity::class.java)
                    settingsIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(settingsIntent)
                }
                retval = true
            }

            else -> retval = super.onOptionsItemSelected(item)
        }
        return retval
    }

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
                    ) && grantResults[index] >= 0
                ) {
                    writeGranted = true
                } else if (Manifest.permission.READ_CALENDAR.equals(
                        permissions[index],
                        ignoreCase = true
                    ) && grantResults[index] >= 0
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
        if (isContactsBackupEnabled && checkContactBackupPermission()) {
            startContactsBackupJob()
        }
        if (showCalendarBackup && isCalendarBackupEnabled && checkCalendarBackupPermission(requireContext())) {
            startCalendarBackupJob()
        }
        DisplayUtils.showSnackMessage(
            requireView().findViewById<View>(R.id.contacts_linear_layout),
            R.string.contacts_preferences_backup_scheduled
        )
    }

    private fun startContactsBackupJob() {
        val activity = activity as ContactsPreferenceActivity?
        if (activity != null) {
            val optionalUser = activity.user
            if (optionalUser.isPresent) {
                backgroundJobManager!!.startImmediateContactsBackup(optionalUser.get())
            }
        }
    }

    private fun startCalendarBackupJob() {
        val activity = activity as ContactsPreferenceActivity?
        if (activity != null) {
            val optionalUser = activity.user
            if (optionalUser.isPresent) {
                backgroundJobManager!!.startImmediateCalendarBackup(optionalUser.get())
            }
        }
    }

    private fun setAutomaticBackup(enabled: Boolean) {
        val activity = activity as ContactsPreferenceActivity? ?: return
        val optionalUser = activity.user
        if (!optionalUser.isPresent) {
            return
        }
        val user = optionalUser.get()
        if (enabled) {
            if (isContactsBackupEnabled) {
                Log_OC.d(TAG, "Scheduling contacts backup job")
                backgroundJobManager?.schedulePeriodicContactsBackup(user)
            } else {
                Log_OC.d(TAG, "Cancelling contacts backup job")
                backgroundJobManager?.cancelPeriodicContactsBackup(user)
            }
            if (isCalendarBackupEnabled) {
                Log_OC.d(TAG, "Scheduling calendar backup job")
                backgroundJobManager?.schedulePeriodicCalendarBackup(user)
            } else {
                Log_OC.d(TAG, "Cancelling calendar backup job")
                backgroundJobManager?.cancelPeriodicCalendarBackup(user)
            }
        } else {
            Log_OC.d(TAG, "Cancelling all backup jobs")
            backgroundJobManager?.cancelPeriodicContactsBackup(user)
            backgroundJobManager?.cancelPeriodicCalendarBackup(user)
        }
        arbitraryDataProvider?.storeOrUpdateKeyValue(
            user.accountName,
            ContactsPreferenceActivity.PREFERENCE_CONTACTS_AUTOMATIC_BACKUP,
            enabled.toString()
        )
    }

    private fun checkAndAskForContactsReadPermission(): Boolean {
        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?

        // check permissions
        return if (checkSelfPermission(contactsPreferenceActivity!!, Manifest.permission.READ_CONTACTS)) {
            true
        } else {
            // No explanation needed, request the permission.
            requestPermissions(
                arrayOf(Manifest.permission.READ_CONTACTS),
                PermissionUtil.PERMISSIONS_READ_CONTACTS_AUTOMATIC
            )
            false
        }
    }

    private fun checkAndAskForCalendarReadPermission(): Boolean {
        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?

        // check permissions
        return if (contactsPreferenceActivity?.let { checkCalendarBackupPermission(it) } == true) {
            true
        } else {
            // No explanation needed, request the permission.
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_CALENDAR,
                    Manifest.permission.WRITE_CALENDAR
                ),
                PermissionUtil.PERMISSIONS_READ_CALENDAR_AUTOMATIC
            )
            false
        }
    }

    private fun checkCalendarBackupPermission(context: Context): Boolean {
        return checkSelfPermission(requireContext(), Manifest.permission.READ_CALENDAR) && checkSelfPermission(
            context, Manifest.permission.WRITE_CALENDAR
        )
    }

    private fun checkContactBackupPermission(): Boolean {
        return checkSelfPermission(requireContext(), Manifest.permission.READ_CONTACTS)
    }

    private fun openCleanDate() {
        if (checkAndAskForCalendarReadPermission() && checkAndAskForContactsReadPermission()) {
            openDate(null)
        }
    }

    private fun openDate(savedDate: Date?) {
        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?
        if (contactsPreferenceActivity == null) {
            Toast.makeText(context, getString(R.string.error_choosing_date), Toast.LENGTH_LONG).show()
            return
        }

        val contactsBackupFolderString = resources.getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR
        val calendarBackupFolderString = resources.getString(R.string.calendar_backup_folder) + OCFile.PATH_SEPARATOR
        val storageManager = contactsPreferenceActivity.storageManager
        val contactsBackupFolder = storageManager.getFileByDecryptedRemotePath(contactsBackupFolderString)
        val calendarBackupFolder = storageManager.getFileByDecryptedRemotePath(calendarBackupFolderString)

        val backupFiles = storageManager.getFolderContent(contactsBackupFolder, false)
        backupFiles.addAll(storageManager.getFolderContent(calendarBackupFolder, false))
        backupFiles.sortWith { o1: OCFile?, o2: OCFile? ->
            if (o1 != null && o2 != null) {
                o1.modificationTimestamp.compareTo(o2.modificationTimestamp)
            } else {
                -1
            }
        }

        val cal = Calendar.getInstance()
        val year: Int
        val month: Int
        val day: Int
        if (savedDate == null) {
            year = cal[Calendar.YEAR]
            month = cal[Calendar.MONTH] + 1
            day = cal[Calendar.DAY_OF_MONTH]
        } else {
            year = savedDate.year
            month = savedDate.month
            day = savedDate.day
        }
        if (backupFiles.size > 0 && backupFiles[backupFiles.size - 1] != null) {
            datePickerDialog = DatePickerDialog(contactsPreferenceActivity, this, year, month, day)
            datePickerDialog?.datePicker?.maxDate = backupFiles[backupFiles.size - 1]!!
                .modificationTimestamp
            datePickerDialog?.datePicker?.minDate = backupFiles[0]!!.modificationTimestamp
            datePickerDialog?.setOnDismissListener { selectedDate = null }
            datePickerDialog?.setTitle("")
            datePickerDialog?.show()

            viewThemeUtils?.platform?.colorTextButtons(
                datePickerDialog!!.getButton(DatePickerDialog.BUTTON_NEGATIVE),
                datePickerDialog!!.getButton(DatePickerDialog.BUTTON_POSITIVE)
            )

            // set background to transparent
            datePickerDialog?.getButton(DatePickerDialog.BUTTON_NEGATIVE)?.setBackgroundColor(0x00000000)
            datePickerDialog?.getButton(DatePickerDialog.BUTTON_POSITIVE)?.setBackgroundColor(0x00000000)
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

        datePickerDialog?.let {
            outState.putBoolean(KEY_CALENDAR_PICKER_OPEN, it.isShowing)

            if (it.isShowing) {
                outState.putInt(KEY_CALENDAR_DAY, it.datePicker.dayOfMonth)
                outState.putInt(KEY_CALENDAR_MONTH, it.datePicker.month)
                outState.putInt(KEY_CALENDAR_YEAR, it.datePicker.year)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught", "NestedBlockDepth", "ComplexMethod", "LongMethod", "MagicNumber")
    override fun onDateSet(view: DatePicker, year: Int, month: Int, dayOfMonth: Int) {
        val contactsPreferenceActivity = activity as ContactsPreferenceActivity?
        if (contactsPreferenceActivity == null) {
            Toast.makeText(context, getString(R.string.error_choosing_date), Toast.LENGTH_LONG).show()
            return
        }

        selectedDate = Date(year, month, dayOfMonth)
        val contactsBackupFolderString = resources.getString(R.string.contacts_backup_folder) + OCFile.PATH_SEPARATOR
        val calendarBackupFolderString = resources.getString(R.string.calendar_backup_folder) + OCFile.PATH_SEPARATOR
        val storageManager = contactsPreferenceActivity.storageManager
        val contactsBackupFolder = storageManager.getFileByDecryptedRemotePath(contactsBackupFolderString)
        val calendarBackupFolder = storageManager.getFileByDecryptedRemotePath(calendarBackupFolderString)
        val backupFiles = storageManager.getFolderContent(contactsBackupFolder, false)
        backupFiles.addAll(storageManager.getFolderContent(calendarBackupFolder, false))

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
            if (start < file.modificationTimestamp && end > file.modificationTimestamp) {
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
        private const val KEY_CALENDAR_DAY = "CALENDAR_DAY"
        private const val KEY_CALENDAR_MONTH = "CALENDAR_MONTH"
        private const val KEY_CALENDAR_YEAR = "CALENDAR_YEAR"
        const val PREFERENCE_CONTACTS_BACKUP_ENABLED = "PREFERENCE_CONTACTS_BACKUP_ENABLED"
        const val PREFERENCE_CALENDAR_BACKUP_ENABLED = "PREFERENCE_CALENDAR_BACKUP_ENABLED"

        @JvmStatic
        fun create(showSidebar: Boolean): BackupFragment {
            val fragment = BackupFragment()
            val bundle = Bundle()
            bundle.putBoolean(ARG_SHOW_SIDEBAR, showSidebar)
            fragment.arguments = bundle
            return fragment
        }
    }
}
