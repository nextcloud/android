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
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes
import androidx.core.widget.TextViewCompat
import com.owncloud.android.R
import kotlin.math.ceil
import kotlin.math.floor

class FilenameView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    LinearLayout(context, attrs, defStyleAttr) {

    private val title = TextView(context)

    private val extension = TextView(context)

    var text: CharSequence
        get() = title.text
        set(value) = render(FilenameState.Single(value.toString()))

    init {
        orientation = HORIZONTAL
        context.withStyledAttributes(attrs, R.styleable.FilenameView, defStyleAttr) {
            applyStyle(FilenameStyle.from(this))
        }
        addView(title, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, TITLE_WEIGHT))
    }

    fun setFilename(filename: String, isFolder: Boolean) {
        render(FilenameState.of(filename, isFolder))
    }

    private fun render(state: FilenameState) {
        when (state) {
            is FilenameState.Single -> showSingle(state)
            is FilenameState.Split -> showSplit(state)
        }
    }

    private fun showSingle(state: FilenameState.Single) {
        title.text = state.name

        removeView(extension)
    }

    private fun showSplit(state: FilenameState.Split) {
        title.text = state.name
        extension.text = state.extension

        // removed extension added back here.
        if (extension.parent == null) {
            addView(
                extension,
                LayoutParams(
                    LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.BOTTOM }
            )
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Bottom sheet (wrap_content): the view is already as wide as its text, nothing extra needed.
        if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }

        // List row (match_parent): keep the name as wide as its text so the extension stays next to it.
        // Grid and recommended rows (0dp): same, so the name can be centered inside the cell.
        val width = MeasureSpec.getSize(widthMeasureSpec)
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), heightMeasureSpec)

        setMeasuredDimension(width, measuredHeight)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        // First place the name and extension side by side as usual, then fix the extension position
        // when the name wraps onto more than one line
        super.onLayout(changed, l, t, r, b)

        placeExtensionAfterLastTitleLine()
    }

    private fun placeExtensionAfterLastTitleLine() {
        // Single-line name (all rows) or no extension
        val titleLayout = title.layout ?: return
        if (extension.parent == null || titleLayout.lineCount <= 1) {
            return
        }

        // Multi-line name (bottom sheet): the name fills the full width, which would leave a gap before
        // the extension, so move the extension to right after the last word.
        val lastLine = titleLayout.lineCount - 1
        val lineStart = title.left + title.totalPaddingLeft

        // Right-to-left languages: the last line ends on the left, so the extension goes to its left.
        // Left-to-right languages: the last line ends on the right, so the extension goes to its right.
        val extensionLeft = if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            lineStart + floor(titleLayout.getLineLeft(lastLine)).toInt() - extension.measuredWidth
        } else {
            lineStart + ceil(titleLayout.getLineRight(lastLine)).toInt()
        }

        extension.layout(extensionLeft, extension.top, extensionLeft + extension.measuredWidth, extension.bottom)
    }

    private fun applyStyle(style: FilenameStyle) {
        // Name and extension must look the same, so both get the same style.
        listOf(title, extension).forEach { textView ->
            TextViewCompat.setTextAppearance(textView, style.textAppearance)
            style.textColor?.let(textView::setTextColor)
            style.textSize?.let { textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, it) }
            textView.gravity = Gravity.START
        }

        title.applyLines(style.titleEllipsize, style.titleMaxLines)

        extension.applyLines(style.extensionEllipsize, style.extensionMaxLines)
    }

    private fun TextView.applyLines(truncateAt: TextUtils.TruncateAt, lines: Int) {
        ellipsize = truncateAt
        if (lines == FilenameStyle.SINGLE_LINE) {
            isSingleLine = true
        } else {
            maxLines = lines
        }
    }

    companion object {
        private const val TITLE_WEIGHT = 1f
    }
}
