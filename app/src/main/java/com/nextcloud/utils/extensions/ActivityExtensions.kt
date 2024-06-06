/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.utils.extensions

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

fun AppCompatActivity.isDialogFragmentReady(fragment: Fragment): Boolean = isActive() && !fragment.isStateSaved()

fun AppCompatActivity.isActive(): Boolean = !isFinishing && !isDestroyed
