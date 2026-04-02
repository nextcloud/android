/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
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

/**
 * This Adapter populates a RecyclerView with all notifications for an account within the app.
 */
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

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notificationsList[position]
        holder.binding.datetime.text = DisplayUtils.getRelativeTimestamp(
            fragment.requireContext(),
            notification.getDatetime().time
        )

        val file = notification.subjectRichParameters[FILE]
        var subject = notification.getSubject()
        if (file == null && !TextUtils.isEmpty(notification.getLink())) {
            subject = "$subject ↗"
            holder.binding.subject.setTypeface(
                holder.binding.subject.typeface,
                Typeface.BOLD
            )
            holder.binding.subject.setOnClickListener {
                DisplayUtils.startLinkIntent(
                    fragment.requireActivity(),
                    notification.getLink()
                )
            }
            holder.binding.subject.text = subject
        } else {
            if (!TextUtils.isEmpty(notification.subjectRich)) {
                holder.binding.subject.text = makeSpecialPartsBold(notification)
            } else {
                holder.binding.subject.text = subject
            }

            if (file != null && !TextUtils.isEmpty(file.id)) {
                holder.binding.subject.setOnClickListener {
                    val intent = Intent(fragment.requireActivity(), FileDisplayActivity::class.java)
                    intent.setAction(Intent.ACTION_VIEW)
                    intent.putExtra(FileDisplayActivity.KEY_FILE_ID, file.id)
                    fragment.requireActivity().startActivity(intent)
                }
            }
        }

        if (notification.getMessage() != null && !notification.getMessage().isEmpty()) {
            holder.binding.message.text = notification.getMessage()
            holder.binding.message.visibility = View.VISIBLE
        } else {
            holder.binding.message.visibility = View.GONE
        }

        if (!TextUtils.isEmpty(notification.getIcon())) {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    withContext(Dispatchers.Main) {
                        GlideHelper
                            .loadIntoImageView(
                                fragment.requireContext(),
                                client,
                                notification.getIcon(),
                                holder.binding.icon,
                                R.drawable.ic_notification,
                                false
                            )
                    }
                } catch (e: Exception) {
                    Log_OC.e("RichDocumentsTemplateAdapter", "Exception setData: " + e)
                }
            }
        }

        viewThemeUtils.platform.run {
            colorImageView(holder.binding.icon, ColorRole.ON_SURFACE_VARIANT)
            colorImageView(holder.binding.dismiss, ColorRole.ON_SURFACE_VARIANT)
            colorTextView(holder.binding.subject, ColorRole.ON_SURFACE)
            colorTextView(holder.binding.message, ColorRole.ON_SURFACE_VARIANT)
            colorTextView(holder.binding.datetime, ColorRole.ON_SURFACE_VARIANT)
        }


        setButtons(holder, notification)

        holder.binding.dismiss.setOnClickListener {
            fragment.lifecycleScope.launch(Dispatchers.IO) {
                val result = DeleteNotificationRemoteOperation(notification.notificationId)
                    .execute(client!!)
                withContext(Dispatchers.Main) {
                    fragment.onRemovedNotification(result.isSuccess)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return notificationsList.size
    }

    fun setButtons(holder: NotificationViewHolder, notification: Notification) {
        // add action buttons
        holder.binding.buttons.removeAllViews()

        val resources: Resources = fragment.resources
        val params: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(
            resources.getDimensionPixelOffset(R.dimen.standard_quarter_margin),
            0,
            resources.getDimensionPixelOffset(R.dimen.standard_half_margin),
            0
        )

        val overflowActions = ArrayList<Action>()

        if (notification.getActions().isNotEmpty()) {
            holder.binding.buttons.visibility = View.VISIBLE
        } else {
            holder.binding.buttons.visibility = View.GONE
        }

        if (notification.getActions().size > 2) {
            for (action in notification.getActions()) {
                if (action.primary) {
                    val button: MaterialButton = MaterialButton(fragment.requireContext())
                    button.setAllCaps(false)

                    button.text = action.label
                    button.setCornerRadiusResource(R.dimen.button_corner_radius)

                    button.setLayoutParams(params)
                    button.setGravity(Gravity.CENTER)

                    button.setOnClickListener {
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
                            )
                                .execute(action)
                        }
                    }

                    viewThemeUtils.material.colorMaterialButtonPrimaryFilled(button)
                    holder.binding.buttons.addView(button)
                } else {
                    overflowActions.add(action)
                }
            }

            // further actions
            val moreButton = MaterialButton(fragment.requireContext())
            moreButton.setBackgroundColor(
                ResourcesCompat.getColor(
                    resources,
                    android.R.color.transparent,
                    null
                )
            )
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(moreButton)

            moreButton.setAllCaps(false)
            moreButton.setText(R.string.more)
            moreButton.setCornerRadiusResource(R.dimen.button_corner_radius)
            moreButton.setLayoutParams(params)
            moreButton.setGravity(Gravity.CENTER)
            moreButton.setOnClickListener {
                val popup = PopupMenu(fragment.requireContext(), moreButton)
                for (action in overflowActions) {
                    popup.menu.add(action.label)
                        .setOnMenuItemClickListener {
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
                                )
                                    .execute(action)
                            }
                            true
                        }
                }
                popup.show()
            }

            holder.binding.buttons.addView(moreButton)
        } else {
            for (action in notification.getActions()) {
                val button = MaterialButton(fragment.requireContext())

                if (action.primary) {
                    viewThemeUtils.material.colorMaterialButtonPrimaryFilled(button)
                } else {
                    button.setBackgroundColor(
                        ResourcesCompat.getColor(
                            resources,
                            android.R.color.transparent,
                            null
                        )
                    )
                    viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(button)
                }

                button.setAllCaps(false)

                button.text = action.label
                button.setCornerRadiusResource(R.dimen.button_corner_radius)

                button.setLayoutParams(params)

                button.setOnClickListener {
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
                        )
                            .execute(action)
                    }
                }

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
