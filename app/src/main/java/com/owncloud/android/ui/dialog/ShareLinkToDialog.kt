/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2017-2018 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017-2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2015 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.utils.extensions.getParcelableArgument
import com.owncloud.android.R
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.ui.activity.CopyToClipboardActivity
import java.util.Collections

/**
 * Dialog showing a list activities able to resolve a given Intent,
 * filtering out the activities matching give package names.
 */
class ShareLinkToDialog : DialogFragment() {
    private var mAdapter: ActivityAdapter? = null
    private var mIntent: Intent? = null

    init {
        Log_OC.d(TAG, "constructor")
    }

    @Suppress("SpreadOperator")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        mIntent = arguments.getParcelableArgument(ARG_INTENT, Intent::class.java) ?: throw NullPointerException()
        val packagesToExclude = arguments?.getStringArray(ARG_PACKAGES_TO_EXCLUDE)
        val packagesToExcludeList = listOf(*packagesToExclude ?: arrayOfNulls(0))
        val pm = activity?.packageManager ?: throw NullPointerException()

        val activities = pm.queryIntentActivities(mIntent!!, PackageManager.MATCH_DEFAULT_ONLY)
        val it = activities.iterator()
        var resolveInfo: ResolveInfo
        while (it.hasNext()) {
            resolveInfo = it.next()
            if (packagesToExcludeList.contains(resolveInfo.activityInfo.packageName.lowercase())) {
                it.remove()
            }
        }

        val sendAction = mIntent?.getBooleanExtra(Intent.ACTION_SEND, false)

        if (sendAction == false) {
            // add activity for copy to clipboard
            val copyToClipboardIntent = Intent(requireActivity(), CopyToClipboardActivity::class.java)
            val copyToClipboard = pm.queryIntentActivities(copyToClipboardIntent, 0)
            if (copyToClipboard.isNotEmpty()) {
                activities.add(copyToClipboard[0])
            }
        }

        Collections.sort(activities, ResolveInfo.DisplayNameComparator(pm))
        mAdapter = ActivityAdapter(requireActivity(), pm, activities)

        return createSelector(sendAction ?: false)
    }

    private fun createSelector(sendAction: Boolean): AlertDialog {
        val titleId = if (sendAction) {
            R.string.activity_chooser_send_file_title
        } else {
            R.string.activity_chooser_title
        }

        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(titleId)
            .setAdapter(mAdapter) { _, which ->
                // Add the information of the chosen activity to the intent to send
                val chosen = mAdapter?.getItem(which)
                val actInfo = chosen?.activityInfo ?: return@setAdapter
                val name = ComponentName(
                    actInfo.applicationInfo.packageName,
                    actInfo.name
                )
                mIntent?.setComponent(name)
                activity?.startActivity(mIntent)
            }
            .create()
    }

    internal inner class ActivityAdapter(
        context: Context,
        private val mPackageManager: PackageManager,
        apps: List<ResolveInfo>
    ) : ArrayAdapter<ResolveInfo?>(context, R.layout.activity_row, apps) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: newView(parent)
            bindView(position, view)
            return view
        }

        private fun newView(parent: ViewGroup): View =
            (context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
                .inflate(R.layout.activity_row, parent, false)

        private fun bindView(position: Int, row: View) {
            row.findViewById<TextView>(R.id.title).run {
                text = getItem(position)?.loadLabel(mPackageManager)
            }

            row.findViewById<ImageView>(R.id.icon).run {
                setImageDrawable(getItem(position)?.loadIcon(mPackageManager))
            }
        }
    }

    companion object {
        private val TAG: String = ShareLinkToDialog::class.java.simpleName
        private val ARG_INTENT = ShareLinkToDialog::class.java.simpleName +
            ".ARG_INTENT"
        private val ARG_PACKAGES_TO_EXCLUDE = ShareLinkToDialog::class.java.simpleName +
            ".ARG_PACKAGES_TO_EXCLUDE"

        @JvmStatic
        fun newInstance(intent: Intent?, vararg packagesToExclude: String?): ShareLinkToDialog {
            val bundle = Bundle().apply {
                putParcelable(ARG_INTENT, intent)
                putStringArray(ARG_PACKAGES_TO_EXCLUDE, packagesToExclude)
            }

            return ShareLinkToDialog().apply {
                arguments = bundle
            }
        }
    }
}
