/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.io

import java.io.IOException
import java.io.OutputStream

internal class SaveErrorOutputStream(private val source: OutputStream) : OutputStream() {
	val error: IOException? get() = _error
	private var _error: IOException? = null

	override fun write(b: Int) = try {
		source.write(b)
	} catch (e: IOException) {
		_error = e
		throw e
	}

	override fun write(b: ByteArray) = try {
		source.write(b)
	} catch (e: IOException) {
		_error = e
		throw e
	}

	override fun write(b: ByteArray, off: Int, len: Int) = try {
		source.write(b, off, len)
	} catch (e: IOException) {
		_error = e
		throw e
	}

	override fun flush() = try {
		source.flush()
	} catch (e: IOException) {
		_error = e
		throw e
	}

	override fun close() = try {
		source.close()
	} catch (e: IOException) {
		_error = e
		throw e
	}
}