/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.logger

import com.owncloud.android.lib.common.utils.Log_OC

/**
 * This adapter is used by legacy [Log_OC] logger to redirect logs to custom logger implementation.
 */
class LegacyLoggerAdapter(private val logger: Logger) : Log_OC.Adapter {

    override fun i(tag: String, message: String) {
        logger.i(tag, message)
    }

    override fun d(tag: String, message: String) {
        logger.d(tag, message)
    }

    override fun d(tag: String, message: String, e: Exception) {
        logger.d(tag, message, e)
    }

    override fun e(tag: String, message: String) {
        logger.e(tag, message)
    }

    override fun e(tag: String, message: String, t: Throwable) {
        logger.e(tag, message, t)
    }

    override fun v(tag: String, message: String) {
        logger.v(tag, message)
    }

    override fun w(tag: String, message: String) {
        logger.w(tag, message)
    }

    override fun wtf(tag: String, message: String) {
        logger.e(tag, message)
    }
}
