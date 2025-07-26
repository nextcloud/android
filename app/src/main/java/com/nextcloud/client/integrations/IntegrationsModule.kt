/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.integrations

import android.content.Context
import android.content.pm.PackageManager
import com.nextcloud.client.integrations.deck.DeckApi
import com.nextcloud.client.integrations.deck.DeckApiImpl
import dagger.Module
import dagger.Provides

@Module
class IntegrationsModule {
    @Provides
    fun deckApi(context: Context, packageManager: PackageManager): DeckApi = DeckApiImpl(context, packageManager)
}
