/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.utils.extensions

import androidx.constraintlayout.widget.ConstraintSet

val PARENT_TOP: Pair<Int, Int> = ConstraintSet.PARENT_ID to ConstraintSet.TOP
val PARENT_BOTTOM: Pair<Int, Int> = ConstraintSet.PARENT_ID to ConstraintSet.BOTTOM
val PARENT_START: Pair<Int, Int> = ConstraintSet.PARENT_ID to ConstraintSet.START
val PARENT_END: Pair<Int, Int> = ConstraintSet.PARENT_ID to ConstraintSet.END

fun topOf(viewId: Int): Pair<Int, Int> = viewId to ConstraintSet.TOP

fun bottomOf(viewId: Int): Pair<Int, Int> = viewId to ConstraintSet.BOTTOM

fun startOf(viewId: Int): Pair<Int, Int> = viewId to ConstraintSet.START

fun endOf(viewId: Int): Pair<Int, Int> = viewId to ConstraintSet.END

/**
 * Connects the given sides of [viewId] to the supplied anchors, skipping the ones left null.
 * An anchor is the target view id paired with the side of that view to attach to, so
 * `anchor(id, top = bottomOf(other))` reads as "put the top of id below other".
 */
fun ConstraintSet.anchor(
    viewId: Int,
    top: Pair<Int, Int>? = null,
    bottom: Pair<Int, Int>? = null,
    start: Pair<Int, Int>? = null,
    end: Pair<Int, Int>? = null
) {
    top?.let { connect(viewId, ConstraintSet.TOP, it.first, it.second) }
    bottom?.let { connect(viewId, ConstraintSet.BOTTOM, it.first, it.second) }
    start?.let { connect(viewId, ConstraintSet.START, it.first, it.second) }
    end?.let { connect(viewId, ConstraintSet.END, it.first, it.second) }
}

fun ConstraintSet.fillParent(viewId: Int) =
    anchor(viewId, top = PARENT_TOP, bottom = PARENT_BOTTOM, start = PARENT_START, end = PARENT_END)
