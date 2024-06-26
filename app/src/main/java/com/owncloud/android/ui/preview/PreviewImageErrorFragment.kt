/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.preview

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.owncloud.android.R
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.fragment.FileFragment

/**
 * A fragment showing an error message.
 */
class PreviewImageErrorFragment : FileFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.preview_image_error_fragment, container, false)
    }

    companion object {
        fun newInstance(): FileFragment {
            val fileFragment: FileFragment = PreviewImageErrorFragment()
            val bundle = Bundle()

            bundle.putParcelable(FileActivity.EXTRA_FILE, null)
            fileFragment.arguments = bundle

            return fileFragment
        }
    }
}
