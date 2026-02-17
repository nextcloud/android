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
            val thisId: Any? = this.id
            val otherId: Any? = other.id
            if (if (thisId == null) otherId != null else (thisId != otherId)) {
                return false
            }
            val thisLabel: Any? = this.label
            val otherLabel: Any? = other.label

            return if (thisLabel == null) otherLabel == null else (thisLabel == otherLabel)
        }

        protected fun canEqual(other: Any?): Boolean = other is MentionChipSpan

        override fun hashCode(): Int {
            val prime = 59
            var result = 1
            val thisId: Any? = this.id
            result = result * prime + (if (thisId == null) 43 else thisId.hashCode())
            val label: Any? = this.label
            return result * prime + (if (label == null) 43 else label.hashCode())
        }

        override fun toString(): String = "Spans.MentionChipSpan(id=" + this.id + ", label=" + this.label + ")"
    }
}
