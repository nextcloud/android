/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.license

/**
 * User: Dima Muravyov
 * Date: 06.02.2020
 */
internal class LicenseResponseTransformer {

	companion object {
		private const val SEPARATOR = "\""
		private const val ESCAPED_NEW_LINE = "\\n"
		private const val NEW_LINE = "\n"
	}

    fun transform(bytes: ByteArray): String? {
		val inlinedResponse = String(bytes)

		var scanbotLicenseKey = ""
		var startIndex = inlinedResponse.indexOf(SEPARATOR)
		while (startIndex != -1) {
			val endIndex = inlinedResponse.indexOf(SEPARATOR, startIndex + 1)
			if (endIndex == -1) {
				scanbotLicenseKey = ""
				break
			}

			scanbotLicenseKey += inlinedResponse.substring(startIndex + 1, endIndex)

			startIndex = inlinedResponse.indexOf(SEPARATOR, endIndex + 1)
		}

		scanbotLicenseKey = scanbotLicenseKey.replace(ESCAPED_NEW_LINE, NEW_LINE)

		return if (scanbotLicenseKey.isNotBlank()) scanbotLicenseKey else null
	}
}