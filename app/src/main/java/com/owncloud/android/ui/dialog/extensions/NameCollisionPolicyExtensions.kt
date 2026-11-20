/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.dialog.extensions

import com.owncloud.android.files.services.NameCollisionPolicy

/**
 * Converts a [NameCollisionPolicy] enum value into the corresponding
 * **UI dialog selection index** used in the dialog.
 *
 * ⚠️ **Important:**
 * These dialog indices are **not** the same as the integer values stored
 * in the database or defined in [NameCollisionPolicy].
 * This mapping is purely for UI selection purposes.
 *
 * | Policy              | Dialog Index |
 * |---------------------|--------------|
 * | ASK_USER (default)  | 0            |
 * | OVERWRITE           | 1            |
 * | RENAME              | 2            |
 * | SKIP                | 3            |
 *
 * @receiver The collision handling policy.
 * @return The index to preselect in the UI dialog for the given policy.
 */
@Suppress("MagicNumber")
fun NameCollisionPolicy.toDialogSelectionIndex(): Int = when (this) {
    NameCollisionPolicy.OVERWRITE -> 1
    NameCollisionPolicy.RENAME -> 2
    NameCollisionPolicy.SKIP -> 3
    NameCollisionPolicy.ASK_USER -> 0
}

/**
 * Converts a **UI dialog selection index** from the dialog
 * back into a [NameCollisionPolicy] enum value.
 *
 * ⚠️ **Important:**
 * These indices are defined only for the dialog and are **not** the same as
 * the values used in the database or internal enum ordinals.
 * Always use this function to translate the dialog result safely.
 *
 * | Dialog Index | Policy              |
 * |---------------|--------------------|
 * | 0             | ASK_USER (default) |
 * | 1             | OVERWRITE          |
 * | 2             | RENAME             |
 * | 3             | SKIP               |
 *
 * Any unexpected index value will default to [NameCollisionPolicy.ASK_USER].
 *
 * @param index The selected index from the dialog.
 * @return The corresponding [NameCollisionPolicy] value.
 */
@Suppress("MagicNumber")
fun nameCollisionPolicyForDialogSelectionIndex(index: Int): NameCollisionPolicy = when (index) {
    1 -> NameCollisionPolicy.OVERWRITE
    2 -> NameCollisionPolicy.RENAME
    3 -> NameCollisionPolicy.SKIP
    else -> NameCollisionPolicy.ASK_USER
}
