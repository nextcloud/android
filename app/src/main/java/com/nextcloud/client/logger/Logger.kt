/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger

interface Logger {

    fun v(tag: String, message: String)
    fun d(tag: String, message: String)
    fun d(tag: String, message: String, t: Throwable)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String)
    fun e(tag: String, message: String, t: Throwable)
}
