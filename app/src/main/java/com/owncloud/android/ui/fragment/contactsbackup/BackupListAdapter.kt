/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.contactsbackup

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.CheckedTextView
import android.widget.ImageView
import com.afollestad.sectionedrecyclerview.SectionedRecyclerViewAdapter
import com.afollestad.sectionedrecyclerview.SectionedViewHolder
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.utils.GlideHelper
import com.owncloud.android.R
import com.owncloud.android.databinding.BackupListItemHeaderBinding
import com.owncloud.android.databinding.CalendarlistListItemBinding
import com.owncloud.android.databinding.ContactlistListItemBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.TextDrawable
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment.getDisplayName
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import ezvcard.VCard
import ezvcard.property.Photo
import third_parties.sufficientlysecure.AndroidCalendar

@Suppress("LongParameterList", "TooManyFunctions")
class BackupListAdapter(
    val accountManager: UserAccountManager,
    val clientFactory: ClientFactory,
    private val checkedVCards: MutableSet<Int> = mutableSetOf(),
    private val checkedCalendars: MutableMap<String, Int> = mutableMapOf(),
    val backupListFragment: BackupListFragment,
    val context: Context,
    private val viewThemeUtils: ViewThemeUtils
) : SectionedRecyclerViewAdapter<SectionedViewHolder>() {

    private val calendarFiles = mutableListOf<OCFile>()
    private val contacts = mutableListOf<VCard>()
    private val availableContactAccounts: List<ContactsAccount> = getAccountsForImport()

    companion object {
        const val SECTION_CALENDAR = 0
        const val SECTION_CONTACTS = 1
        const val VIEW_TYPE_CALENDAR = 2
        const val VIEW_TYPE_CONTACTS = 3
    }

    init {
        shouldShowHeadersForEmptySections(false)
        shouldShowFooters(false)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder = when (viewType) {
        VIEW_TYPE_HEADER -> BackupListHeaderViewHolder(
            BackupListItemHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            context
        )

        VIEW_TYPE_CONTACTS -> ContactItemViewHolder(
            ContactlistListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

        else -> CalendarItemViewHolder(
            CalendarlistListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            context
        )
    }

    override fun onBindViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        when (section) {
            SECTION_CALENDAR -> bindCalendarViewHolder(holder as CalendarItemViewHolder, relativePosition)
            SECTION_CONTACTS -> bindContactViewHolder(holder as ContactItemViewHolder, relativePosition)
        }
    }

    override fun getItemCount(section: Int): Int = when (section) {
        SECTION_CALENDAR -> calendarFiles.size
        else -> contacts.size
    }

    override fun getSectionCount(): Int = 2

    override fun getItemViewType(section: Int, relativePosition: Int, absolutePosition: Int): Int =
        if (section == SECTION_CALENDAR) VIEW_TYPE_CALENDAR else VIEW_TYPE_CONTACTS

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder?, section: Int, expanded: Boolean) {
        (holder as? BackupListHeaderViewHolder)?.let { headerHolder ->
            viewThemeUtils.platform.colorTextView(headerHolder.binding.name, ColorRole.PRIMARY)
            when (section) {
                SECTION_CALENDAR -> bindCalendarHeader(headerHolder)
                SECTION_CONTACTS -> bindContactsHeader(headerHolder)
            }
        }
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder?, section: Int) = Unit

    fun addCalendar(file: OCFile) {
        calendarFiles.add(file)
        notifyItemInserted(calendarFiles.lastIndex)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun replaceVcards(vCards: List<VCard>) {
        contacts.clear()
        contacts.addAll(vCards)
        notifyDataSetChanged()
    }

    fun getCheckedCalendarStringArray(): Array<String> = checkedCalendars.keys.toTypedArray()

    fun getCheckedContactsIntArray(): IntArray = checkedVCards.toIntArray()

    fun getCheckedCalendarPathsArray(): Map<String, Int> = checkedCalendars

    fun hasCalendarEntry(): Boolean = calendarFiles.isNotEmpty()

    fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            checkedVCards.addAll(contacts.indices)
        } else {
            checkedVCards.clear()
            checkedCalendars.clear()
        }
        showRestoreButton()
    }

    private fun bindCalendarHeader(holder: BackupListHeaderViewHolder) {
        holder.binding.name.text = context.getString(R.string.calendars)
        holder.binding.spinner.visibility = View.GONE
    }

    private fun bindContactsHeader(holder: BackupListHeaderViewHolder) {
        holder.binding.name.text = context.getString(R.string.contacts)

        if (checkedVCards.isNotEmpty()) {
            holder.binding.spinner.visibility = View.VISIBLE
            holder.binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    backupListFragment.setSelectedAccount(availableContactAccounts[position])
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    backupListFragment.setSelectedAccount(null)
                }
            }
            holder.setContactsAccount(availableContactAccounts)
        } else {
            holder.binding.spinner.visibility = View.GONE
        }
    }

    private fun bindContactViewHolder(holder: ContactItemViewHolder, position: Int) {
        val vCard = contacts[position]
        val rawName = getDisplayName(vCard)
        val displayName = rawName?.takeIf { it.isNotBlank() } ?: context.getString(android.R.string.unknownName)

        setChecked(checkedVCards.contains(position), holder.binding.name)
        holder.binding.name.text = displayName
        viewThemeUtils.platform.themeCheckedTextView(holder.binding.name)

        if (vCard.photos.isNotEmpty()) {
            loadContactPhoto(holder.binding.icon, vCard.photos.first())
        } else {
            loadPlaceholderAvatar(holder.binding.icon, displayName)
        }

        holder.setVCardListener { toggleVCard(holder, position) }
    }

    private fun bindCalendarViewHolder(holder: CalendarItemViewHolder, position: Int) {
        val ocFile = calendarFiles[position]
        val storagePath = ocFile.storagePath

        setChecked(checkedCalendars.containsKey(storagePath), holder.binding.name)

        val fileName = ocFile.fileName
        val calendarName = fileName.substringBefore("_")
        val date = fileName.substringAfterLast("_").replace(".ics", "").replace("-", ":")

        holder.binding.name.text = context.getString(R.string.calendar_name_linewrap, calendarName, date)
        viewThemeUtils.platform.themeCheckedTextView(holder.binding.name)
        holder.setCalendars(AndroidCalendar.loadAll(context.contentResolver))

        holder.binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, calendarPosition: Int, id: Long) {
                checkedCalendars[storagePath] = calendarPosition
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                checkedCalendars[storagePath] = -1
            }
        }

        holder.setListener { toggleCalendar(holder, position) }
    }

    private fun loadContactPhoto(imageView: ImageView, firstPhoto: Photo) {
        firstPhoto.data?.takeIf { it.isNotEmpty() }?.let { data ->
            val thumbnail = BitmapFactory.decodeByteArray(data, 0, data.size)
            imageView.setImageDrawable(BitmapUtils.bitmapToCircularBitmapDrawable(context.resources, thumbnail))
            return
        }

        firstPhoto.url?.let { url ->
            val target = object : CustomTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    imageView.setImageDrawable(resource)
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                    imageView.setImageDrawable(placeholder)
                }

                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    imageView.setImageDrawable(errorDrawable)
                }
            }

            GlideHelper.loadIntoTarget(
                backupListFragment.requireActivity(),
                accountManager.currentOwnCloudAccount,
                url,
                target,
                R.drawable.ic_user_outline
            )
        }
    }

    private fun loadPlaceholderAvatar(imageView: ImageView, displayName: String) {
        try {
            imageView.setImageDrawable(
                TextDrawable.createNamedAvatar(
                    displayName,
                    context.resources.getDimension(R.dimen.list_item_avatar_icon_radius)
                )
            )
        } catch (_: Resources.NotFoundException) {
            imageView.setImageResource(R.drawable.ic_user_outline)
        }
    }

    private fun toggleVCard(holder: ContactItemViewHolder, position: Int) {
        val isChecked = !holder.binding.name.isChecked
        holder.binding.name.isChecked = isChecked

        if (isChecked) {
            checkedVCards.add(position)
        } else {
            checkedVCards.remove(position)
        }

        showRestoreButton()
        notifySectionChanged(SECTION_CONTACTS)
    }

    private fun toggleCalendar(holder: CalendarItemViewHolder, position: Int) {
        val isChecked = !holder.binding.name.isChecked
        val storagePath = calendarFiles[position].storagePath

        holder.binding.name.isChecked = isChecked
        holder.showCalendars(isChecked)

        if (isChecked) {
            checkedCalendars[storagePath] = 0
        } else {
            checkedCalendars.remove(storagePath)
        }

        showRestoreButton()
    }

    private fun setChecked(checked: Boolean, checkedTextView: CheckedTextView) {
        checkedTextView.isChecked = checked
    }

    private fun showRestoreButton() {
        val hasSelection = checkedCalendars.isNotEmpty() || checkedVCards.isNotEmpty()
        val hasAvailableCalendar =
            checkedCalendars.isEmpty() || AndroidCalendar.loadAll(context.contentResolver).isNotEmpty()

        backupListFragment.showRestoreButton(hasSelection && hasAvailableCalendar)
    }

    @Suppress("NestedBlockDepth", "TooGenericExceptionCaught")
    private fun getAccountsForImport(): List<ContactsAccount> {
        val accounts =
            mutableListOf(ContactsAccount(context.getString(R.string.backup_list_adapter_local_contacts), null, null))

        try {
            context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE),
                null,
                null,
                null
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_NAME)
                val typeIndex = cursor.getColumnIndexOrThrow(ContactsContract.RawContacts.ACCOUNT_TYPE)

                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    val type = cursor.getString(typeIndex)
                    accounts.add(ContactsAccount(name, name, type))
                }
            }
        } catch (e: Exception) {
            Log_OC.d(BackupListFragment.TAG, e.message)
        }

        return accounts.distinct()
    }
}
