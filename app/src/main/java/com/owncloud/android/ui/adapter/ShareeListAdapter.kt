/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2017-2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-FileCopyrightText: 2015-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.account.User
import com.nextcloud.utils.mdm.MDMConfig.shareViaLink
import com.owncloud.android.R
import com.owncloud.android.databinding.FileDetailsShareInternalShareLinkBinding
import com.owncloud.android.databinding.FileDetailsShareLinkShareItemBinding
import com.owncloud.android.databinding.FileDetailsSharePublicLinkAddNewItemBinding
import com.owncloud.android.databinding.FileDetailsShareSecureFileDropAddNewItemBinding
import com.owncloud.android.databinding.FileDetailsShareShareItemBinding
import com.owncloud.android.datamodel.SharesType
import com.owncloud.android.lib.resources.shares.OCShare
import com.owncloud.android.lib.resources.shares.ShareType
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.utils.DisplayUtils.AvatarGenerationListener
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlin.math.min

/**
 * Adapter to show a user/group/email/remote in Sharing list in file details view.
 */
@Suppress("LongParameterList")
class ShareeListAdapter(
    private val fileActivity: FileActivity,
    @JvmField var shares: ArrayList<OCShare>,
    private val listener: ShareeListAdapterListener,
    private val userId: String?,
    private val user: User?,
    private val viewThemeUtils: ViewThemeUtils,
    private val encrypted: Boolean,
    private val sharesType: SharesType?
) : RecyclerView.Adapter<RecyclerView.ViewHolder?>(),
    AvatarGenerationListener {
    private val avatarRadiusDimension: Float = fileActivity.getResources().getDimension(R.dimen.user_icon_radius)
    var isShowAll: Boolean = false
        private set

    init {
        sortShares()
    }

    override fun getItemViewType(position: Int): Int = shares.getOrNull(position)?.shareType?.value ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(fileActivity)
        val shareViaLink = shareViaLink(fileActivity)
        val shareType = ShareType.fromValue(viewType)

        return when {
            !shareViaLink || shareType == ShareType.INTERNAL -> {
                val binding = FileDetailsShareInternalShareLinkBinding.inflate(inflater, parent, false)
                InternalShareViewHolder(binding, fileActivity)
            }

            shareType == ShareType.PUBLIC_LINK || shareType == ShareType.EMAIL -> {
                val binding = FileDetailsShareLinkShareItemBinding.inflate(inflater, parent, false)
                LinkShareViewHolder(binding, fileActivity, viewThemeUtils, encrypted)
            }

            shareType == ShareType.NEW_PUBLIC_LINK -> {
                if (encrypted) {
                    val binding = FileDetailsShareSecureFileDropAddNewItemBinding.inflate(inflater, parent, false)
                    NewSecureFileDropViewHolder(binding)
                } else {
                    val binding = FileDetailsSharePublicLinkAddNewItemBinding.inflate(inflater, parent, false)
                    NewLinkShareViewHolder(binding)
                }
            }

            else -> {
                val binding = FileDetailsShareShareItemBinding.inflate(inflater, parent, false)
                ShareViewHolder(binding, user, fileActivity, viewThemeUtils, encrypted)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (shares.size <= position) {
            return
        }

        val share = shares[position]
        val shareViaLink = shareViaLink(fileActivity)

        if (!shareViaLink) {
            if (holder is InternalShareViewHolder) {
                holder.bind(share, listener)
            }

            return
        }

        when (holder) {
            is LinkShareViewHolder -> holder.bind(share, listener, position)
            is InternalShareViewHolder -> holder.bind(share, listener)
            is NewLinkShareViewHolder -> holder.bind(listener)
            is NewSecureFileDropViewHolder -> holder.bind(listener)
            is ShareViewHolder -> holder.bind(share, listener, this, userId, avatarRadiusDimension)
        }
    }

    override fun getItemId(position: Int): Long = shares[position].id

    @Suppress("MagicNumber")
    override fun getItemCount(): Int = if (shareViaLink(fileActivity)) {
        if (isShowAll) shares.size else min(shares.size, 3)
    } else {
        1
    }

    @SuppressLint("NotifyDataSetChanged")
    fun toggleShowAll() {
        isShowAll = !isShowAll
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addShares(sharesToAdd: List<OCShare>) {
        shares.addAll(sharesToAdd)
        sortShares()
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeAll() {
        shares.clear()
        notifyDataSetChanged()
    }

    override fun avatarGenerated(avatarDrawable: Drawable?, callContext: Any?) {
        if (callContext is ImageView) {
            callContext.setImageDrawable(avatarDrawable)
        }
    }

    override fun shouldCallGeneratedCallback(tag: String, callContext: Any?): Boolean {
        if (callContext is ImageView) {
            // needs to be changed once federated users have avatars
            return callContext.tag.toString() == tag.split("@".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()[0]
        }
        return false
    }

    fun remove(share: OCShare?) {
        share ?: return
        val position = shares.indexOf(share)
        if (position != -1) {
            shares.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun sortShares() {
        shares.sortWith(
            compareBy<OCShare> {
                when (it.shareType) {
                    ShareType.PUBLIC_LINK -> 0
                    ShareType.EMAIL -> 1
                    else -> 2
                }
            }.thenByDescending { it.sharedDate }
        )

        // add internal share link at end
        if (!encrypted && sharesType == SharesType.INTERNAL) {
            shares.add(OCShare().apply { shareType = ShareType.INTERNAL })
        }
    }
}
