/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.gallery

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import com.ionos.scanbot.R

class GalleryBottomNavigationItem @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attributeSet, defStyleAttr) {

    private val imageView: ImageView

    init {
        inflate(context, R.layout.scanbot_gallery_bottom_navigation_item, this)

        context.theme.obtainStyledAttributes(
            attributeSet,
            R.styleable.BottomNavigationItem,
            defStyleAttr,
            0
        ).apply {
            try {
                imageView = findViewById(R.id.ivIcon)
                imageView.setImageDrawable(
                    getDrawable(R.styleable.BottomNavigationItem_itemIcon)
                )
                findViewById<TextView>(R.id.tvLabel)
                    .text = getString(R.styleable.BottomNavigationItem_label)
            } finally {
                recycle()
            }
        }
    }

    fun setImageResource(@DrawableRes resId: Int) {
        imageView.setImageResource(resId)
    }

    override fun setBackgroundResource(resid: Int) {
        imageView.setBackgroundResource(resid)
    }
}