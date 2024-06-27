/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.dialog

import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.utils.IntentUtil.createSendIntent
import com.owncloud.android.R
import com.owncloud.android.databinding.SendFilesFragmentBinding
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.ui.adapter.SendButtonAdapter
import com.owncloud.android.ui.components.SendButtonData
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class SendFilesDialog : BottomSheetDialogFragment(R.layout.send_files_fragment), Injectable {

    private var files: Array<OCFile>? = null
    private lateinit var binding: SendFilesFragmentBinding

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // keep the state of the fragment on configuration changes
        retainInstance = true

        files = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelableArray(KEY_OCFILES, OCFile::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelableArray(KEY_OCFILES) as Array<OCFile>?
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = SendFilesFragmentBinding.inflate(inflater, container, false)

        setupSendButtonRecyclerView()
        viewThemeUtils?.platform?.colorViewBackground(binding.bottomSheet, ColorRole.SURFACE)

        return binding.root
    }

    private fun setupSendButtonRecyclerView() {
        val sendIntent = createSendIntent(requireContext(), files!!)
        val matches = requireActivity().packageManager.queryIntentActivities(sendIntent, 0)

        if (matches.isEmpty()) {
            Toast.makeText(context, R.string.no_send_app, Toast.LENGTH_SHORT).show()
            dismiss()
            return
        }

        val sendButtonDataList = setupSendButtonData(matches)
        val clickListener = setupSendButtonClickListener(sendIntent)

        @Suppress("MagicNumber")
        binding.sendButtonRecyclerView.layoutManager = GridLayoutManager(requireActivity(), 4)
        binding.sendButtonRecyclerView.adapter = SendButtonAdapter(sendButtonDataList, clickListener)
    }

    private fun setupSendButtonClickListener(sendIntent: Intent): SendButtonAdapter.ClickListener {
        return SendButtonAdapter.ClickListener { sendButtonDataData: SendButtonData ->
            val packageName = sendButtonDataData.packageName
            val activityName = sendButtonDataData.activityName
            sendIntent.component = ComponentName(packageName, activityName)
            requireActivity().startActivity(Intent.createChooser(sendIntent, getString(R.string.send)))
            dismiss()
        }
    }

    private fun setupSendButtonData(matches: List<ResolveInfo>): List<SendButtonData> {
        var icon: Drawable
        var sendButtonData: SendButtonData
        var label: CharSequence
        val sendButtonDataList: MutableList<SendButtonData> = ArrayList(matches.size)
        for (match in matches) {
            icon = match.loadIcon(requireActivity().packageManager)
            label = match.loadLabel(requireActivity().packageManager)
            sendButtonData = SendButtonData(
                icon,
                label,
                match.activityInfo.packageName,
                match.activityInfo.name
            )
            sendButtonDataList.add(sendButtonData)
        }
        return sendButtonDataList
    }

    companion object {
        private const val KEY_OCFILES = "KEY_OCFILES"

        fun newInstance(files: Set<OCFile>): SendFilesDialog {
            val dialogFragment = SendFilesDialog()
            val args = Bundle()
            args.putParcelableArray(KEY_OCFILES, files.toTypedArray())
            dialogFragment.arguments = args
            return dialogFragment
        }
    }
}
