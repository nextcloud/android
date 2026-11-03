/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.nextcloud.utils.view.LocaleDirection
import com.owncloud.android.utils.FileStorageUtils
import com.google.android.material.R as MaterialR

class FilenameView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {

    private val title = createTextView(TextUtils.TruncateAt.END, TITLE_MAX_LINES)

    private val extension = createTextView(TextUtils.TruncateAt.START, EXTENSION_MAX_LINES)

    var text: CharSequence
        get() = title.text
        set(value) {
            title.text = value
            removeView(extension)
        }

    init {
        orientation = HORIZONTAL
        addView(title, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, TITLE_WEIGHT))
    }

    fun setFilename(filename: String, isFolder: Boolean) {
        if (!FileStorageUtils.containsBidiControlCharacters(filename)) {
            text = filename
            return
        }

        val (base, ext) = FileStorageUtils.getFilenameAndExtension(filename, isFolder, LocaleDirection.isRtl)
        title.text = base

        if (ext.isNullOrEmpty()) {
            removeView(extension)
            return
        }

        extension.text = ext
        if (extension.parent == null) {
            addView(
                extension,
                LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.BOTTOM }
            )
        }
    }

    private fun createTextView(truncateAt: TextUtils.TruncateAt, lines: Int): TextView = TextView(context).apply {
        TextViewCompat.setTextAppearance(this, MaterialR.style.TextAppearance_Material3_HeadlineSmall)
        ellipsize = truncateAt
        maxLines = lines
        gravity = Gravity.START
    }

    companion object {
        private const val TITLE_WEIGHT = 1f
        private const val TITLE_MAX_LINES = 3
        private const val EXTENSION_MAX_LINES = 1
    }
}
