/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-FileCopyrightText: 2017 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.events

/**
 * Event for making favoriting work
 */
class FavoriteEvent(val remotePath: String, val shouldFavorite: Boolean)
