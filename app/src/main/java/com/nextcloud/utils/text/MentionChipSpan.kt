/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.text

import android.graphics.drawable.Drawable
import third_parties.fresco.BetterImageSpan
import java.util.Objects

class MentionChipSpan(drawable: Drawable, verticalAlignment: Int, var id: String, var label: CharSequence?) :
    BetterImageSpan(drawable, verticalAlignment) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MentionChipSpan) return false
        return id == other.id && label == other.label
    }

    override fun hashCode(): Int = Objects.hash(id, label)

    override fun toString(): String = "MentionChipSpan(id=$id, label=$label)"
}
