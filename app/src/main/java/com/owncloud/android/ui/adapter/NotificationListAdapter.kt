/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.view.size
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.GlideHelper
import com.nextcloud.utils.extensions.setVisibleIf
import com.owncloud.android.R
import com.owncloud.android.databinding.NotificationListItemBinding
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.lib.resources.notifications.DeleteNotificationRemoteOperation
import com.owncloud.android.lib.resources.notifications.models.Action
import com.owncloud.android.lib.resources.notifications.models.Notification
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.asynctasks.NotificationExecuteActionTask
import com.owncloud.android.ui.fragment.notifications.NotificationsFragment
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationListAdapter(
    private val client: NextcloudClient?,
    private val fragment: NotificationsFragment,
    private val viewThemeUtils: ViewThemeUtils
) : RecyclerView.Adapter<NotificationListAdapter.NotificationViewHolder>() {
    private val styleSpanBold = StyleSpan(Typeface.BOLD)
    private val foregroundColorSpanBlack: ForegroundColorSpan = ForegroundColorSpan(
        ContextCompat.getColor(fragment.requireContext(), R.color.text_color)
    )

    private val notificationsList: ArrayList<Notification> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun setNotificationItems(notificationItems: List<Notification>) {
        notificationsList.clear()
        notificationsList.addAll(notificationItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        return NotificationViewHolder(
            NotificationListItemBinding.inflate(LayoutInflater.from(fragment.requireContext()))
        )
    }

    override fun getItemCount(): Int {
        return notificationsList.size
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationsList[position]
        holder.binding.datetime.text = DisplayUtils.getRelativeTimestamp(
            fragment.requireContext(),
            notification.getDatetime().time
        )

        bindSubject(holder, notification)
        bindMessage(holder, notification)
        bindIcon(holder, notification)
        colorViewHolder(holder)
        bindButtons(holder, notification)
    }

    private fun bindSubject(holder: NotificationViewHolder, notification: Notification) {
        val file = notification.subjectRichParameters[FILE]
        if (file == null && !TextUtils.isEmpty(notification.getLink())) {
            val subject = "${notification.getSubject()} ↗"
            holder.binding.subject.run {
                setTypeface(typeface, Typeface.BOLD)
                text = subject
                setOnClickListener {
                    DisplayUtils.startLinkIntent(fragment.requireActivity(), notification.getLink())
                }
            }
        } else {
            holder.binding.subject.run {
                text = if (!TextUtils.isEmpty(notification.subjectRich)) {
                    makeSpecialPartsBold(notification)
                } else {
                    notification.getSubject()
                }
                if (file?.id?.isNotEmpty() == true) {
                    setOnClickListener {
                        val intent = Intent(fragment.requireActivity(), FileDisplayActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            putExtra(FileDisplayActivity.KEY_FILE_ID, file.id)
                        }
                        fragment.requireActivity().startActivity(intent)
                    }
                }
            }
        }
    }

    private fun bindMessage(holder: NotificationViewHolder, notification: Notification) {
        holder.binding.message.run {
            if (notification.getMessage() != null && !notification.getMessage().isEmpty()) {
                text = notification.getMessage()
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    private fun bindIcon(holder: NotificationViewHolder, notification: Notification) {
        if (notification.getIcon().isNullOrEmpty()) return

        fragment.lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                withContext(Dispatchers.Main) {
                    GlideHelper.loadIntoImageView(
                        fragment.requireContext(),
                        client,
                        notification.getIcon(),
                        holder.binding.icon,
                        R.drawable.ic_notification,
                        false
                    )
                }
            }.onFailure { e ->
                Log_OC.e("RichDocumentsTemplateAdapter", "exception setData: $e")
            }
        }
    }

    private fun colorViewHolder(holder: NotificationViewHolder) {
        viewThemeUtils.platform.run {
            colorImageView(holder.binding.icon, ColorRole.ON_SURFACE_VARIANT)
            colorImageView(holder.binding.dismiss, ColorRole.ON_SURFACE_VARIANT)
            colorTextView(holder.binding.subject, ColorRole.ON_SURFACE)
            colorTextView(holder.binding.message, ColorRole.ON_SURFACE_VARIANT)
            colorTextView(holder.binding.datetime, ColorRole.ON_SURFACE_VARIANT)
        }
    }

    private fun getPrimaryButton(
        holder: NotificationViewHolder,
        action: Action,
        notification: Notification,
        params: LinearLayout.LayoutParams
    ): MaterialButton {
        return MaterialButton(fragment.requireContext()).apply {
            setAllCaps(false)
            text = action.label
            setCornerRadiusResource(R.dimen.button_corner_radius)
            setLayoutParams(params)
            setGravity(Gravity.CENTER)
            setOnClickListener {
                onPrimaryAction(holder, action, notification)
            }
        }
    }

    private fun onPrimaryAction(holder: NotificationViewHolder, action: Action, notification: Notification) {
        setButtonEnabled(holder, false)
        if (ACTION_TYPE_WEB == action.type) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setData(action.link?.toUri())
            fragment.requireActivity().startActivity(intent)
        } else {
            NotificationExecuteActionTask(
                client!!,
                holder,
                notification,
                fragment
            ).execute(action)
        }
    }

    private fun getMoreButton(
        overflowActions: ArrayList<Action>,
        params: LinearLayout.LayoutParams,
        holder: NotificationViewHolder,
        notification: Notification
    ): MaterialButton {
        return MaterialButton(fragment.requireContext()).apply {
            setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    android.R.color.transparent,
                    null
                )
            )
            setAllCaps(false)
            setText(R.string.more)
            setCornerRadiusResource(R.dimen.button_corner_radius)
            setLayoutParams(params)
            setGravity(Gravity.CENTER)
            setOnClickListener {
                val popup = PopupMenu(fragment.requireContext(), this)
                for (action in overflowActions) {
                    popup.menu.add(action.label)
                        .setOnMenuItemClickListener {
                            onPrimaryAction(holder, action, notification)
                            true
                        }
                }
                popup.show()
            }
        }
    }

    private fun getButton(
        action: Action,
        holder: NotificationViewHolder,
        params: LinearLayout.LayoutParams,
        notification: Notification
    ): MaterialButton {
        return MaterialButton(fragment.requireContext()).apply {
            if (action.primary) {
                viewThemeUtils.material.colorMaterialButtonPrimaryFilled(this)
            } else {
                setBackgroundColor(
                    ResourcesCompat.getColor(
                        resources,
                        android.R.color.transparent,
                        null
                    )
                )
                viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(this)
            }
            setAllCaps(false)
            text = action.label
            setCornerRadiusResource(R.dimen.button_corner_radius)
            setLayoutParams(params)
            setOnClickListener {
                onPrimaryAction(holder, action, notification)
            }
        }
    }

    fun bindButtons(holder: NotificationViewHolder, notification: Notification) {
        holder.binding.dismiss.setOnClickListener {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val result = DeleteNotificationRemoteOperation(notification.notificationId)
                    .execute(client!!)
                withContext(Dispatchers.Main) {
                    fragment.onRemovedNotification(result.isSuccess)
                }
            }
        }

        holder.binding.buttons.removeAllViews()

        val resources = fragment.resources
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(resources.getDimensionPixelOffset(R.dimen.standard_quarter_margin), 0, resources.getDimensionPixelOffset(R.dimen.standard_half_margin), 0)
        val overflowActions = ArrayList<Action>()
        holder.binding.buttons.setVisibleIf(notification.getActions().isNotEmpty())

        if (notification.getActions().size > 2) {
            for (action in notification.getActions()) {
                if (action.primary) {
                    val button = getPrimaryButton(holder, action, notification, params)
                    viewThemeUtils.material.colorMaterialButtonPrimaryFilled(button)
                    holder.binding.buttons.addView(button)
                } else {
                    overflowActions.add(action)
                }
            }

            val moreButton = getMoreButton(overflowActions, params, holder, notification)
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(moreButton)
            holder.binding.buttons.addView(moreButton)
        } else {
            for (action in notification.getActions()) {
                val button = getButton(action, holder, params, notification)
                holder.binding.buttons.addView(button)
            }
        }
    }

    private fun makeSpecialPartsBold(notification: Notification): SpannableStringBuilder {
        var text = notification.getSubjectRich()
        val ssb = SpannableStringBuilder(text)

        var openingBrace = text.indexOf('{')
        var closingBrace: Int
        var replaceablePart: String?
        while (openingBrace != -1) {
            closingBrace = text.indexOf('}', openingBrace) + 1
            replaceablePart = text.substring(openingBrace + 1, closingBrace - 1)

            val richObject = notification.subjectRichParameters.get(replaceablePart)
            if (richObject != null) {
                val name = richObject.name
                ssb.replace(openingBrace, closingBrace, name)
                text = ssb.toString()
                closingBrace = openingBrace + name!!.length

                ssb.setSpan(styleSpanBold, openingBrace, closingBrace, 0)
                ssb.setSpan(foregroundColorSpanBlack, openingBrace, closingBrace, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            openingBrace = text.indexOf('{', closingBrace)
        }

        return ssb
    }

    fun removeNotification(holder: NotificationViewHolder) {
        val position = holder.bindingAdapterPosition

        if (position >= 0 && position < notificationsList.size) {
            notificationsList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, notificationsList.size)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeAllNotifications() {
        notificationsList.clear()
        notifyDataSetChanged()
    }

    fun setButtonEnabled(holder: NotificationViewHolder, enabled: Boolean) {
        for (i in 0..<holder.binding.buttons.size) {
            holder.binding.buttons.getChildAt(i).setEnabled(enabled)
        }
    }

    class NotificationViewHolder(var binding: NotificationListItemBinding) :
        RecyclerView.ViewHolder(
            binding.getRoot()
        )

    companion object {
        private const val FILE = "file"
        private const val ACTION_TYPE_WEB = "WEB"
    }
}
