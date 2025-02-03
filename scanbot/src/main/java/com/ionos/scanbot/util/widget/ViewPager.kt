/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.widget

import androidx.viewpager.widget.ViewPager

internal fun ViewPager.addOnPageSelectedListener(listener: (position: Int) -> Unit) {
	addOnPageChangeListener(object : ViewPager.SimpleOnPageChangeListener() {
		override fun onPageSelected(position: Int) {
			listener.invoke(position)
		}
	})
}
