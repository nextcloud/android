/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2020 Tobias Kaminsky
 * Copyright (C) 2020 Nextcloud GmbH
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
package com.owncloud.android.ui.fragment

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.fragment.app.Fragment
import com.owncloud.android.R
import com.owncloud.android.ui.TextDrawable

internal class AvatarTestFragment : Fragment() {
    lateinit var list1: LinearLayout
    lateinit var list2: LinearLayout

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.avatar_fragment, null)

        list1 = view.findViewById(R.id.avatar_list1)
        list2 = view.findViewById(R.id.avatar_list2)

        return view
    }

    fun addAvatar(name: String, avatarRadius: Float, width: Int, targetContext: Context) {
        val margin = padding
        val imageView = ImageView(targetContext)
        imageView.setImageDrawable(TextDrawable.createNamedAvatar(name, avatarRadius))

        val layoutParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(width, width)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        layoutParams.setMargins(margin, margin, margin, margin)
        imageView.layoutParams = layoutParams

        list1.addView(imageView)
    }

    fun addBitmap(bitmap: Bitmap, width: Int, list: Int, targetContext: Context) {
        val margin = padding
        val imageView = ImageView(targetContext)
        imageView.setImageBitmap(bitmap)

        val layoutParams: RelativeLayout.LayoutParams = RelativeLayout.LayoutParams(width, width)
        layoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        layoutParams.setMargins(margin, margin, margin, margin)
        imageView.layoutParams = layoutParams

        if (list == 1) {
            list1.addView(imageView)
        } else {
            list2.addView(imageView)
        }
    }

    companion object {
        private const val padding = 10
    }
}
