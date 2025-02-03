/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.screens.camera.ui_components

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.ionos.scanbot.R

private const val DEFAULT_DISPLAY_TIME = 2500L

class UserGuidanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var iconImageView: ImageView
    private var textView: TextView
    private val handler = Handler(Looper.getMainLooper())

    init {
        LayoutInflater.from(context).inflate(R.layout.scanbot_user_guidance, this, true)
        iconImageView = findViewById(R.id.icon)
        textView = findViewById(R.id.text)

        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.UserGuidance,
            0, 0).apply {
            try {
                val text = getString(R.styleable.UserGuidance_text)
                val icon = getDrawable(R.styleable.UserGuidance_icon)
                setText(text)
                setIcon(icon)
            } finally {
                recycle()
            }
        }
        visibility = GONE
    }

    fun setText(text: String?) {
        textView.text = text
    }

    fun setIcon(icon: Drawable?) {
        iconImageView.setImageDrawable(icon)
    }

    fun setIcon(resourceId: Int) {
        iconImageView.setImageResource(resourceId)
    }


    fun show(duration: Long = DEFAULT_DISPLAY_TIME) {
        visibility = VISIBLE
        handler.postDelayed({ hide() }, duration)
    }

    fun hide() {
        visibility = GONE
    }
}