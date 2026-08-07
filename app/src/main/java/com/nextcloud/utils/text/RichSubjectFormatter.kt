/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.text

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.chip.ChipDrawable
import com.nextcloud.client.account.CurrentAccountProvider
import com.owncloud.android.R
import com.owncloud.android.utils.DisplayUtils
import thirdparties.fresco.BetterImageSpan

class RichSubjectFormatter(private val context: Context, private val currentAccountProvider: CurrentAccountProvider) :
    DisplayUtils.AvatarGenerationListener {

    fun format(richSubject: String, paramForTag: (String) -> RichSubjectParam?): SpannableStringBuilder {
        var text = richSubject
        val ssb = SpannableStringBuilder(text)

        var start = text.indexOf(PLACEHOLDER_START)
        while (start != -1) {
            val end = text.indexOf(PLACEHOLDER_END, start) + 1
            if (end <= start) break

            val param = paramForTag(text.substring(start + 1, end - 1))

            val nextSearchStart = when {
                param == null -> end

                param.isMention -> {
                    ssb.applyMentionSpan(param, start, end)
                    end
                }

                else -> {
                    val nameEnd = ssb.applyClickableNameSpan(param, start, end)
                    text = ssb.toString()
                    nameEnd
                }
            }

            start = text.indexOf(PLACEHOLDER_START, nextSearchStart)
        }

        return ssb
    }

    override fun avatarGenerated(avatarDrawable: Drawable, callContext: Any) {
        (callContext as? ChipDrawable)?.chipIcon = avatarDrawable
    }

    override fun shouldCallGeneratedCallback(tag: String, callContext: Any): Boolean = true

    private fun SpannableStringBuilder.applyMentionSpan(param: RichSubjectParam, start: Int, end: Int) {
        val name = param.name
        val chip = mentionChipDrawable(name.orEmpty())
        val span = MentionChipSpan(chip, BetterImageSpan.ALIGN_CENTER, param.id.orEmpty(), name)

        param.id?.let { id ->
            DisplayUtils.setAvatar(
                currentAccountProvider.user,
                id,
                name,
                this@RichSubjectFormatter,
                context.resources.getDimension(R.dimen.avatar_icon_radius),
                context.resources,
                chip,
                context
            )
        }

        setSpan(span, start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
    }

    private fun SpannableStringBuilder.applyClickableNameSpan(param: RichSubjectParam, start: Int, end: Int): Int {
        val name = param.name.orEmpty()
        replace(start, end, name)
        val nameEnd = start + name.length

        param.onClick?.let { onClick ->
            setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) = onClick()
                    override fun updateDrawState(ds: TextPaint) {
                        ds.isUnderlineText = false
                    }
                },
                start,
                nameEnd,
                0
            )
        }

        setSpan(StyleSpan(Typeface.BOLD), start, nameEnd, 0)
        setSpan(
            ForegroundColorSpan(ContextCompat.getColor(context, R.color.text_color)),
            start,
            nameEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return nameEnd
    }

    private fun mentionChipDrawable(text: String): ChipDrawable =
        ChipDrawable.createFromResource(context, R.xml.chip_others).apply {
            setEllipsize(TextUtils.TruncateAt.MIDDLE)
            layoutDirection = context.resources.configuration.layoutDirection
            setText(text)
            setChipIconResource(R.drawable.accent_circle)
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }

    companion object {
        private const val PLACEHOLDER_START = '{'
        private const val PLACEHOLDER_END = '}'
    }
}
