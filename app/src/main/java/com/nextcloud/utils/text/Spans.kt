/*
 * Nextcloud Talk - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017-2018 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package com.nextcloud.utils.text

import android.graphics.drawable.Drawable
import thirdparties.fresco.BetterImageSpan

class Spans {
    class MentionChipSpan(drawable: Drawable, verticalAlignment: Int, var id: String, var label: CharSequence?) :
        BetterImageSpan(drawable, verticalAlignment) {
        override fun equals(o: Any?): Boolean {
            if (o === this) {
                return true
            }
            if (o !is MentionChipSpan) {
                return false
            }
            val other = o
            if (!other.canEqual(this as Any)) {
                return false
            }
            val `this$id`: Any? = this.id
            val `other$id`: Any? = other.id
            if (if (`this$id` == null) `other$id` != null else (`this$id` != `other$id`)) {
                return false
            }
            val `this$label`: Any? = this.label
            val `other$label`: Any? = other.label

            return if (`this$label` == null) `other$label` == null else (`this$label` == `other$label`)
        }

        protected fun canEqual(other: Any?): Boolean {
            return other is MentionChipSpan
        }

        override fun hashCode(): Int {
            val PRIME = 59
            var result = 1
            val `$id`: Any? = this.id
            result = result * PRIME + (if (`$id` == null) 43 else `$id`.hashCode())
            val `$label`: Any? = this.label
            return result * PRIME + (if (`$label` == null) 43 else `$label`.hashCode())
        }

        override fun toString(): String {
            return "Spans.MentionChipSpan(id=" + this.id + ", label=" + this.label + ")"
        }
    }
}
