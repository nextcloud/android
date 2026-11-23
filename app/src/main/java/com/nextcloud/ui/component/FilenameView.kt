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
import kotlin.math.ceil
import kotlin.math.floor
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

        // also removes the extension from view
        title.text = base

        if (ext.isNullOrEmpty()) {
            removeView(extension)
            return
        }

        extension.text = ext

        // removed extension view added back here
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

    private fun applyStyle(typedArray: TypedArray) {
        // Bottom sheet sets nothing, so it gets the defaults: big headline text, up to 3 lines, cut at the end.
        // Rows set their own size, color, 1 line and cut in the middle in their layout XML.
        val textAppearance = typedArray.getResourceId(
            R.styleable.FilenameView_filenameTextAppearance,
            MaterialR.style.TextAppearance_Material3_HeadlineSmall
        )
        val textColor = typedArray.getColorStateList(R.styleable.FilenameView_filenameTextColor)
        val textSize = typedArray.getDimension(R.styleable.FilenameView_filenameTextSize, UNSET_TEXT_SIZE)
        val titleMaxLines = typedArray.getInt(R.styleable.FilenameView_filenameMaxLines, TITLE_MAX_LINES)
        val titleEllipsize = typedArray.getInt(R.styleable.FilenameView_filenameEllipsize, ELLIPSIZE_END)

        // Name and extension must look the same, so both get the same style.
        listOf(title, extension).forEach { textView ->
            TextViewCompat.setTextAppearance(textView, textAppearance)
            textColor?.let(textView::setTextColor)
            if (textSize != UNSET_TEXT_SIZE) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
            }
            textView.gravity = Gravity.START
        }

        title.applyLines(truncateAtOf(titleEllipsize), titleMaxLines)

        // The extension is always one line and cut from the start, so its end stays visible.
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
