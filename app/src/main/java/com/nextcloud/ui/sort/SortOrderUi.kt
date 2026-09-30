/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.ui.sort

import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.owncloud.android.R
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment
import com.owncloud.android.utils.FileSortOrder

object SortOrderUi {

    @JvmStatic
    fun showDialog(fragmentManager: FragmentManager, sortOrder: FileSortOrder) {
        val transaction = fragmentManager.beginTransaction().addToBackStack(null)
        SortingOrderDialogFragment.newInstance(sortOrder)
            .show(transaction, SortingOrderDialogFragment.SORTING_ORDER_FRAGMENT)
    }

    @JvmStatic
    @StringRes
    fun labelRes(sortOrder: FileSortOrder): Int = when (sortOrder.name) {
        FileSortOrder.SORT_Z_TO_A_ID -> R.string.menu_item_sort_by_name_z_a
        FileSortOrder.SORT_NEW_TO_OLD_ID -> R.string.menu_item_sort_by_date_newest_first
        FileSortOrder.SORT_OLD_TO_NEW_ID -> R.string.menu_item_sort_by_date_oldest_first
        FileSortOrder.SORT_BIG_TO_SMALL_ID -> R.string.menu_item_sort_by_size_biggest_first
        FileSortOrder.SORT_SMALL_TO_BIG_ID -> R.string.menu_item_sort_by_size_smallest_first
        else -> R.string.menu_item_sort_by_name_a_z
    }
}
