/*
 *   ownCloud Android client application
 *
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.nextcloud.android.common.ui.theme.utils.ColorRole
import com.nextcloud.client.di.Injectable
import com.owncloud.android.databinding.LoadingDialogBinding
import com.owncloud.android.utils.theme.ViewThemeUtils
import javax.inject.Inject

class LoadingDialog : DialogFragment(), Injectable {

    @JvmField
    @Inject
    var viewThemeUtils: ViewThemeUtils? = null

    private var mMessage: String? = null
    private lateinit var binding: LoadingDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        retainInstance = true
        isCancelable = false
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = LoadingDialogBinding.inflate(inflater, container, false)
        binding.loadingText.text = mMessage

        val loadingDrawable = binding.loadingBar.indeterminateDrawable
        if (loadingDrawable != null) {
            viewThemeUtils?.platform?.tintDrawable(requireContext(), loadingDrawable)
        }

        viewThemeUtils?.platform?.colorViewBackground(binding.loadingLayout, ColorRole.SURFACE)

        return binding.root
    }

    override fun onDestroyView() {
        if (dialog != null && retainInstance) {
            dialog?.setDismissMessage(null)
        }

        super.onDestroyView()
    }

    companion object {

        @JvmStatic
        fun newInstance(message: String?): LoadingDialog {
            val loadingDialog = LoadingDialog()
            loadingDialog.mMessage = message
            return loadingDialog
        }
    }
}
