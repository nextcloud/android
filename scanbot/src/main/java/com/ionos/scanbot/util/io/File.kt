/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.io

import java.io.File

internal fun File.removeRecursive(): Boolean {
	if (exists() && isDirectory) {
		val children = list() ?: return delete()
		for (child in children) {
			val success = File(this, child).removeRecursive()
			if (!success) {
				return false
			}
		}
	}
	return delete()
}
