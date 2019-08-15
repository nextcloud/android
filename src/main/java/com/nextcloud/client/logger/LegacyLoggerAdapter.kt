/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
