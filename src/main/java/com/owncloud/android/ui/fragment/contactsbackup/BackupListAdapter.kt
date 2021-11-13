/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
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
package com.owncloud.android.ui.fragment.contactsbackup

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.database.Cursor
import android.graphics.BitmapFactory
import android.graphics.PorterDuff
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
import com.bumptech.glide.request.animation.GlideAnimation
import com.bumptech.glide.request.target.SimpleTarget
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.network.ClientFactory
import com.owncloud.android.R
import com.owncloud.android.databinding.BackupListItemHeaderBinding
import com.owncloud.android.databinding.CalendarlistListItemBinding
import com.owncloud.android.databinding.ContactlistListItemBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.TextDrawable
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment.getDisplayName
import com.owncloud.android.utils.BitmapUtils
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ThemeColorUtils
import ezvcard.VCard
import ezvcard.property.Photo
import third_parties.sufficientlysecure.AndroidCalendar

@Suppress("LongParameterList", "TooManyFunctions")
class BackupListAdapter(
    val accountManager: UserAccountManager,
    val clientFactory: ClientFactory,
    private val checkedVCards: HashSet<Int> = HashSet(),
    private val checkedCalendars: HashMap<String, Int> = HashMap(),
    val backupListFragment: BackupListFragment,
    val context: Context
) : SectionedRecyclerViewAdapter<SectionedViewHolder>() {
    private val calendarFiles = arrayListOf<OCFile>()
    private val contacts = arrayListOf<VCard>()
    private var availableContactAccounts = listOf<ContactsAccount>()

    companion object {
        const val SECTION_CALENDAR = 0
        const val SECTION_CONTACTS = 1

        const val VIEW_TYPE_CALENDAR = 2
        const val VIEW_TYPE_CONTACTS = 3

        const val SINGLE_SELECTION = 1

        const val SINGLE_ACCOUNT = 1
    }

    init {
        shouldShowHeadersForEmptySections(false)
        shouldShowFooters(false)
        availableContactAccounts = getAccountForImport()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SectionedViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                BackupListHeaderViewHolder(
                    BackupListItemHeaderBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ),
                    context
                )
            }
            VIEW_TYPE_CONTACTS -> {
                ContactItemViewHolder(
                    ContactlistListItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    )
                )
            }
            else -> {
                CalendarItemViewHolder(
                    CalendarlistListItemBinding.inflate(
                        LayoutInflater.from(parent.context), parent, false
                    ),
                    context
                )
            }
        }
    }

    override fun onBindViewHolder(
        holder: SectionedViewHolder?,
        section: Int,
        relativePosition: Int,
        absolutePosition: Int
    ) {
        if (section == SECTION_CALENDAR) {
            bindCalendarViewHolder(holder as CalendarItemViewHolder, relativePosition)
        }

        if (section == SECTION_CONTACTS) {
            bindContactViewHolder(holder as ContactItemViewHolder, relativePosition)
        }
    }

    override fun getItemCount(section: Int): Int {
        return if (section == SECTION_CALENDAR) {
            calendarFiles.size
        } else {
            contacts.size
        }
    }

    override fun getSectionCount(): Int {
        return 2
    }

    override fun getItemViewType(section: Int, relativePosition: Int, absolutePosition: Int): Int {
        return if (section == SECTION_CALENDAR) {
            VIEW_TYPE_CALENDAR
        } else {
            VIEW_TYPE_CONTACTS
        }
    }

    override fun onBindHeaderViewHolder(holder: SectionedViewHolder?, section: Int, expanded: Boolean) {
        val headerViewHolder = holder as BackupListHeaderViewHolder

        headerViewHolder.binding.name.setTextColor(ThemeColorUtils.primaryColor(context))

        if (section == SECTION_CALENDAR) {
            headerViewHolder.binding.name.text = context.resources.getString(R.string.calendars)
            headerViewHolder.binding.spinner.visibility = View.GONE
        } else {
            headerViewHolder.binding.name.text = context.resources.getString(R.string.contacts)
            if (checkedVCards.isNotEmpty()) {
                headerViewHolder.binding.spinner.visibility = View.VISIBLE

                holder.binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        backupListFragment.setSelectedAccount(availableContactAccounts[position])
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        backupListFragment.setSelectedAccount(null)
                    }
                }

                headerViewHolder.setContactsAccount(availableContactAccounts)
            }
        }
    }

    override fun onBindFooterViewHolder(holder: SectionedViewHolder?, section: Int) {
        // not needed
    }

    fun addCalendar(file: OCFile) {
        calendarFiles.add(file)
        notifyItemInserted(calendarFiles.size - 1)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun replaceVcards(vCards: MutableList<VCard>) {
        contacts.clear()
        contacts.addAll(vCards)
        notifyDataSetChanged()
    }

    fun bindContactViewHolder(holder: ContactItemViewHolder, position: Int) {
        val vCard = contacts[position]

        setChecked(checkedVCards.contains(position), holder.binding.name)

        holder.binding.name.text = getDisplayName(vCard)

        // photo
        if (vCard.photos.size > 0) {
            setPhoto(holder.binding.icon, vCard.photos[0])
        } else {
            try {
                holder.binding.icon.setImageDrawable(
                    TextDrawable.createNamedAvatar(
                        holder.binding.name.text.toString(),
                        context.resources.getDimension(R.dimen.list_item_avatar_icon_radius)
                    )
                )
            } catch (e: Resources.NotFoundException) {
                holder.binding.icon.setImageResource(R.drawable.ic_user)
            }
        }

        holder.setVCardListener { toggleVCard(holder, position) }
    }

    private fun setChecked(checked: Boolean, checkedTextView: CheckedTextView) {
        checkedTextView.isChecked = checked
        if (checked) {
            checkedTextView.checkMarkDrawable
                .setColorFilter(ThemeColorUtils.primaryColor(context), PorterDuff.Mode.SRC_ATOP)
        } else {
            checkedTextView.checkMarkDrawable.clearColorFilter()
        }
    }

    private fun toggleVCard(holder: ContactItemViewHolder, position: Int) {
        holder.binding.name.isChecked = !holder.binding.name.isChecked
        if (holder.binding.name.isChecked) {
            holder.binding.name.checkMarkDrawable.setColorFilter(
                ThemeColorUtils.primaryColor(context),
                PorterDuff.Mode.SRC_ATOP
            )
            checkedVCards.add(position)
        } else {
            holder.binding.name.checkMarkDrawable.clearColorFilter()
            checkedVCards.remove(position)
        }

        showRestoreButton()
        notifySectionChanged(SECTION_CONTACTS)
    }

    private fun setPhoto(imageView: ImageView, firstPhoto: Photo) {
        val url = firstPhoto.url
        val data = firstPhoto.data
        if (data != null && data.isNotEmpty()) {
            val thumbnail = BitmapFactory.decodeByteArray(data, 0, data.size)
            val drawable = BitmapUtils.bitmapToCircularBitmapDrawable(
                context.resources,
                thumbnail
            )
            imageView.setImageDrawable(drawable)
        } else if (url != null) {
            val target = object : SimpleTarget<Drawable>() {
                override fun onResourceReady(resource: Drawable?, glideAnimation: GlideAnimation<in Drawable>?) {
                    imageView.setImageDrawable(resource)
                }

                override fun onLoadFailed(e: java.lang.Exception?, errorDrawable: Drawable?) {
                    super.onLoadFailed(e, errorDrawable)
                    imageView.setImageDrawable(errorDrawable)
                }
            }

            DisplayUtils.downloadIcon(
                accountManager,
                clientFactory,
                context,
                url,
                target,
                R.drawable.ic_user,
                imageView.width,
                imageView.height
            )
        }
    }

    private fun bindCalendarViewHolder(holder: CalendarItemViewHolder, position: Int) {
        val ocFile: OCFile = calendarFiles[position]

        setChecked(checkedCalendars.containsValue(position), holder.binding.name)
        val name = ocFile.fileName
        val calendarName = name.substring(0, name.indexOf("_"))
        val date = name.substring(name.lastIndexOf("_") + 1).replace(".ics", "").replace("-", ":")
        holder.binding.name.text = context.resources.getString(R.string.calendar_name_linewrap, calendarName, date)
        holder.setCalendars(ArrayList(AndroidCalendar.loadAll(context.contentResolver)))
        holder.binding.spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, calendarPosition: Int, id: Long) {
                checkedCalendars[calendarFiles[position].storagePath] = calendarPosition
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                checkedCalendars[calendarFiles[position].storagePath] = -1
            }
        }

        holder.setListener { toggleCalendar(holder, position) }
    }

    private fun toggleCalendar(holder: CalendarItemViewHolder, position: Int) {
        val checkedTextView = holder.binding.name
        checkedTextView.isChecked = !checkedTextView.isChecked
        if (checkedTextView.isChecked) {
            checkedTextView.checkMarkDrawable.setColorFilter(
                ThemeColorUtils.primaryColor(context),
                PorterDuff.Mode.SRC_ATOP
            )
            holder.showCalendars(true)
            checkedCalendars[calendarFiles[position].storagePath] = 0
        } else {
            checkedTextView.checkMarkDrawable.clearColorFilter()
            checkedCalendars.remove(calendarFiles[position].storagePath)
            holder.showCalendars(false)
        }

        showRestoreButton()
    }

    private fun showRestoreButton() {
        val checkedEmpty = checkedCalendars.isEmpty() && checkedVCards.isEmpty()
        val noCalendarAvailable =
            checkedCalendars.isNotEmpty() && AndroidCalendar.loadAll(context.contentResolver).isEmpty()

        if (checkedEmpty || noCalendarAvailable) {
            backupListFragment.showRestoreButton(false)
        } else {
            backupListFragment.showRestoreButton(true)
        }
    }

    fun getCheckedCalendarStringArray(): Array<String> {
        return checkedCalendars.keys.toTypedArray()
    }

    fun getCheckedContactsIntArray(): IntArray {
        return checkedVCards.toIntArray()
    }

    fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            contacts.forEachIndexed { index, _ -> checkedVCards.add(index) }
        } else {
            checkedVCards.clear()
            checkedCalendars.clear()
        }

        showRestoreButton()
    }

    fun getCheckedCalendarPathsArray(): Map<String, Int> {
        return checkedCalendars
    }

    fun hasCalendarEntry(): Boolean {
        return calendarFiles.isNotEmpty()
    }

    @Suppress("NestedBlockDepth", "TooGenericExceptionCaught")
    private fun getAccountForImport(): List<ContactsAccount> {
        val contactsAccounts = ArrayList<ContactsAccount>()

        // add local one
        contactsAccounts.add(ContactsAccount("Local contacts", null, null))

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                ContactsContract.RawContacts.CONTENT_URI,
                arrayOf(
                    ContactsContract.RawContacts.ACCOUNT_NAME,
                    ContactsContract.RawContacts.ACCOUNT_TYPE
                ),
                null,
                null,
                null
            )
            if (cursor != null && cursor.count > 0) {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME))
                    val type = cursor.getString(cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE))
                    val account = ContactsAccount(name, name, type)
                    if (!contactsAccounts.contains(account)) {
                        contactsAccounts.add(account)
                    }
                }
                cursor.close()
            }
        } catch (e: Exception) {
            Log_OC.d(BackupListFragment.TAG, e.message)
        } finally {
            cursor?.close()
        }

        return contactsAccounts
    }
}
