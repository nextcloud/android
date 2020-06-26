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
    fun deckApi(context: Context, packageManager: PackageManager): DeckApi {
        return DeckApiImpl(context, packageManager)
    }
}
