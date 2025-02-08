/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Tobias Kaminsky <tobias.kaminsky@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.app.ActivityCompat.finishAffinity
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.nextcloud.android.lib.resources.tos.GetTermsRemoteOperation
import com.nextcloud.android.lib.resources.tos.SignTermRemoteOperation
import com.nextcloud.android.lib.resources.tos.Term
import com.nextcloud.client.account.UserAccountManager
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.network.ClientFactory
import com.nextcloud.common.NextcloudClient
import com.nextcloud.utils.extensions.setHtmlContent
import com.owncloud.android.R
import com.owncloud.android.databinding.DialogShowTosBinding
import com.owncloud.android.lib.common.operations.RemoteOperationResult
import com.owncloud.android.lib.common.utils.Log_OC
import com.owncloud.android.utils.DisplayUtils
import com.owncloud.android.utils.theme.ViewThemeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TermsOfServiceDialog : DialogFragment(), Injectable {
    private lateinit var binding: DialogShowTosBinding

    @Inject
    lateinit var clientFactory: ClientFactory

    @Inject
    lateinit var accountManager: UserAccountManager

    @Inject
    lateinit var viewThemeUtils: ViewThemeUtils

    lateinit var client: NextcloudClient
    lateinit var terms: List<Term>
    lateinit var languages: Map<String, String>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogShowTosBinding.inflate(requireActivity().layoutInflater)
        return createDialogBuilder().create()
    }

    override fun onStart() {
        super.onStart()
        fetchTerms()
    }

    private fun updateDialog() {
        binding.message.setHtmlContent(terms[0].renderedBody)

        val arrayAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            binding.root.context,
            android.R.layout.simple_spinner_item
        ).apply {
            for ((_, _, languageCode) in terms) {
                add(languages[languageCode])
            }

            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        setupLanguageDropdown(arrayAdapter)
    }

    private fun setupLanguageDropdown(arrayAdapter: ArrayAdapter<String>) {
        binding.languageDropdown.run {
            adapter = arrayAdapter
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(adapterView: AdapterView<*>?, view: View, position: Int, l: Long) {
                    binding.message
                        .setHtmlContent(terms[position].renderedBody)
                }

                override fun onNothingSelected(adapterView: AdapterView<*>?) = Unit
            }

            if (terms.size == 1) {
                visibility = View.GONE
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun fetchTerms() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                client = clientFactory.createNextcloudClient(accountManager.user)
                val result = GetTermsRemoteOperation().execute(client)

                if (result.isSuccess &&
                    !result.resultData.hasSigned &&
                    result.resultData.terms.isNotEmpty()
                ) {
                    languages = result.resultData.languages
                    terms = result.resultData.terms

                    withContext(Dispatchers.Main) {
                        updateDialog()
                    }
                }
            } catch (exception: ClientFactory.CreationException) {
                Log_OC.e(TAG, "Error creating client!")
            }
        }
    }

    private fun createDialogBuilder(): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(binding.root.context)
            .setView(binding.root)
            .setTitle(R.string.terms_of_service_title)
            .setNegativeButton(R.string.dialog_close) { _, _ ->
                activity?.let { finishAffinity(it) }
            }
            .setPositiveButton(R.string.terms_of_services_agree) { dialog, _ ->
                dialog.dismiss()
                agreeToS()
            }
    }

    private fun agreeToS() {
        lifecycleScope.launch(Dispatchers.IO) {
            val id = binding.languageDropdown.selectedItemPosition
            val signResult: RemoteOperationResult<Void> =
                SignTermRemoteOperation(terms[id].id).execute(client)

            if (!signResult.isSuccess) {
                withContext(Dispatchers.Main) {
                    DisplayUtils.showSnackMessage(view, R.string.sign_tos_failed)
                }
            }
        }
    }

    companion object {
        private const val TAG = "TermsOfServiceDialog"
    }
}
