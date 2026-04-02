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

@Suppress("TooManyFunctions")
class NotificationListAdapter(
    private val client: NextcloudClient?,
    private val fragment: NotificationsFragment,
    private val viewThemeUtils: ViewThemeUtils
) : RecyclerView.Adapter<NotificationListAdapter.NotificationViewHolder>() {

    private val styleSpanBold = StyleSpan(Typeface.BOLD)
    private val foregroundColorSpanBlack = ForegroundColorSpan(
        ContextCompat.getColor(fragment.requireContext(), R.color.text_color)
    )
    private val notificationsList = ArrayList<Notification>()

    // region Adapter overrides

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = NotificationViewHolder(
        NotificationListItemBinding.inflate(LayoutInflater.from(fragment.requireContext()))
    )

    override fun getItemCount() = notificationsList.size

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationsList[position]
        with(holder.binding) {
            datetime.text = DisplayUtils.getRelativeTimestamp(
                fragment.requireContext(),
                notification.getDatetime().time
            )
        }
        bindSubject(holder, notification)
        bindMessage(holder, notification)
        bindIcon(holder, notification)
        colorViewHolder(holder)
        bindButtons(holder, notification)
    }

    // endregion

    // region Bind helpers

    private fun bindSubject(holder: NotificationViewHolder, notification: Notification) {
        val file = notification.subjectRichParameters[FILE]
        if (file == null && !TextUtils.isEmpty(notification.getLink())) {
            val subject = "${notification.getSubject()} ↗"
            holder.binding.subject.apply {
                setTypeface(typeface, Typeface.BOLD)
                text = subject
                setOnClickListener {
                    DisplayUtils.startLinkIntent(fragment.requireActivity(), notification.getLink())
                }
            }
        } else {
            holder.binding.subject.apply {
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
        val message = notification.getMessage()
        holder.binding.message.apply {
            if (!message.isNullOrEmpty()) {
                text = message
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

    // endregion

    // region Button binding

    fun bindButtons(holder: NotificationViewHolder, notification: Notification) {
        holder.binding.dismiss.setOnClickListener {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val result = DeleteNotificationRemoteOperation(notification.notificationId).execute(client!!)
                withContext(Dispatchers.Main) { fragment.onRemovedNotification(result.isSuccess) }
            }
        }

        val actions = notification.getActions()
        holder.binding.buttons.apply {
            removeAllViews()
            setVisibleIf(actions.isNotEmpty())
            if (actions.isEmpty()) return
        }

        val params = buttonLayoutParams()

        if (actions.size > 2) {
            val overflowActions = ArrayList<Action>()
            for (action in actions) {
                if (action.primary) {
                    addPrimaryButton(holder, action, notification, params)
                } else {
                    overflowActions.add(action)
                }
            }
            val moreButton =
                buildButton(transparent = true, label = fragment.getString(R.string.more), params = params) {
                    showOverflowMenu(it, overflowActions, holder, notification)
                }
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(moreButton)
            holder.binding.buttons.addView(moreButton)
        } else {
            for (action in actions) {
                val button = buildButton(transparent = !action.primary, label = action.label, params = params) {
                    onActionClicked(holder, action, notification)
                }
                if (action.primary) {
                    viewThemeUtils.material.colorMaterialButtonPrimaryFilled(button)
                } else {
                    viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(button)
                }
                holder.binding.buttons.addView(button)
            }
        }
    }

    private fun addPrimaryButton(
        holder: NotificationViewHolder,
        action: Action,
        notification: Notification,
        params: LinearLayout.LayoutParams
    ) {
        val button = buildButton(transparent = false, label = action.label, params = params) {
            onActionClicked(holder, action, notification)
        }
        viewThemeUtils.material.colorMaterialButtonPrimaryFilled(button)
        holder.binding.buttons.addView(button)
    }

    private fun buildButton(
        transparent: Boolean,
        label: String?,
        params: LinearLayout.LayoutParams,
        onClick: (View) -> Unit
    ): MaterialButton = MaterialButton(fragment.requireContext()).apply {
        if (transparent) {
            setBackgroundColor(ResourcesCompat.getColor(resources, android.R.color.transparent, null))
        }
        setAllCaps(false)
        text = label
        setCornerRadiusResource(R.dimen.button_corner_radius)
        layoutParams = params
        setGravity(Gravity.CENTER)
        setOnClickListener(onClick)
    }

    private fun showOverflowMenu(
        anchor: View,
        overflowActions: List<Action>,
        holder: NotificationViewHolder,
        notification: Notification
    ) {
        PopupMenu(fragment.requireContext(), anchor).apply {
            for (action in overflowActions) {
                menu.add(action.label).setOnMenuItemClickListener {
                    onActionClicked(holder, action, notification)
                    true
                }
            }
            show()
        }
    }

    private fun onActionClicked(holder: NotificationViewHolder, action: Action, notification: Notification) {
        setButtonEnabled(holder, false)
        if (ACTION_TYPE_WEB == action.type) {
            fragment.requireActivity().startActivity(
                Intent(Intent.ACTION_VIEW).apply { data = action.link?.toUri() }
            )
        } else {
            NotificationExecuteActionTask(client!!, holder, notification, fragment).execute(action)
        }
    }

    private fun buttonLayoutParams() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        val resources = fragment.resources
        setMargins(
            resources.getDimensionPixelOffset(R.dimen.standard_quarter_margin),
            0,
            resources.getDimensionPixelOffset(R.dimen.standard_half_margin),
            0
        )
    }

    // endregion

    // region Data manipulation

    @SuppressLint("NotifyDataSetChanged")
    fun setNotificationItems(notificationItems: List<Notification>) {
        notificationsList.clear()
        notificationsList.addAll(notificationItems)
        notifyDataSetChanged()
    }

    fun removeNotification(holder: NotificationViewHolder) {
        val position = holder.bindingAdapterPosition
        if (position in 0 until notificationsList.size) {
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
        for (i in 0 until holder.binding.buttons.size) {
            holder.binding.buttons.getChildAt(i).isEnabled = enabled
        }
    }

    // endregion

    private fun makeSpecialPartsBold(notification: Notification): SpannableStringBuilder {
        var text = notification.getSubjectRich()
        val ssb = SpannableStringBuilder(text)
        var openingBrace = text.indexOf('{')
        while (openingBrace != -1) {
            val closingBrace = text.indexOf('}', openingBrace) + 1
            val key = text.substring(openingBrace + 1, closingBrace - 1)
            val richObject = notification.subjectRichParameters[key]
            if (richObject != null) {
                val name = richObject.name ?: ""
                ssb.replace(openingBrace, closingBrace, name)
                text = ssb.toString()
                val end = openingBrace + name.length
                ssb.setSpan(styleSpanBold, openingBrace, end, 0)
                ssb.setSpan(foregroundColorSpanBlack, openingBrace, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                openingBrace = text.indexOf('{', end)
            } else {
                openingBrace = text.indexOf('{', closingBrace)
            }
        }
        return ssb
    }

    class NotificationViewHolder(var binding: NotificationListItemBinding) :
        RecyclerView.ViewHolder(binding.root)

    companion object {
        private const val FILE = "file"
        private const val ACTION_TYPE_WEB = "WEB"
    }
}
