/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.component

import android.content.Context
import android.content.res.TypedArray
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.core.widget.TextViewCompat
import com.nextcloud.utils.view.LocaleDirection
import com.owncloud.android.R
import com.owncloud.android.utils.FileStorageUtils
import com.google.android.material.R as MaterialR

class FilenameView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {

    private val title = TextView(context)

    private val extension = TextView(context)

    var text: CharSequence
        get() = title.text
        set(value) {
            title.text = value
            removeView(extension)
        }

    init {
        orientation = HORIZONTAL
        context.withStyledAttributes(attrs, R.styleable.FilenameView, defStyleAttr) { applyStyle(this) }
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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        val width = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), heightMeasureSpec)

        setMeasuredDimension(width, measuredHeight)
    }

    private fun applyStyle(typedArray: TypedArray) {
        val textAppearance = typedArray.getResourceId(
            R.styleable.FilenameView_filenameTextAppearance,
            MaterialR.style.TextAppearance_Material3_HeadlineSmall
        )
        val textColor = typedArray.getColorStateList(R.styleable.FilenameView_filenameTextColor)
        val textSize = typedArray.getDimension(R.styleable.FilenameView_filenameTextSize, UNSET_TEXT_SIZE)
        val titleMaxLines = typedArray.getInt(R.styleable.FilenameView_filenameMaxLines, TITLE_MAX_LINES)
        val titleEllipsize = typedArray.getInt(R.styleable.FilenameView_filenameEllipsize, ELLIPSIZE_END)

        listOf(title, extension).forEach { textView ->
            TextViewCompat.setTextAppearance(textView, textAppearance)
            textColor?.let(textView::setTextColor)
            if (textSize != UNSET_TEXT_SIZE) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            }
            textView.gravity = Gravity.START
        }

        title.applyLines(truncateAtOf(titleEllipsize), titleMaxLines)
        extension.applyLines(TextUtils.TruncateAt.START, EXTENSION_MAX_LINES)
    }

    private fun TextView.applyLines(truncateAt: TextUtils.TruncateAt, lines: Int) {
        ellipsize = truncateAt
        if (lines == SINGLE_LINE) {
            isSingleLine = true
        } else {
            maxLines = lines
        }
    }

    private fun truncateAtOf(ellipsize: Int): TextUtils.TruncateAt = when (ellipsize) {
        ELLIPSIZE_START -> TextUtils.TruncateAt.START
        ELLIPSIZE_MIDDLE -> TextUtils.TruncateAt.MIDDLE
        else -> TextUtils.TruncateAt.END
    }

    companion object {
        private const val TITLE_WEIGHT = 1f
        private const val TITLE_MAX_LINES = 3
        private const val EXTENSION_MAX_LINES = 1
        private const val SINGLE_LINE = 1
        private const val UNSET_TEXT_SIZE = -1f

        private const val ELLIPSIZE_START = 1
        private const val ELLIPSIZE_MIDDLE = 2
        private const val ELLIPSIZE_END = 3
    }
}
