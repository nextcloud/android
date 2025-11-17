/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.pager.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.nextcloud.client.player.ui.pager.PlayerPagerFragmentFactory

class DefaultFragmentPagerAdapter<T>(
    fragmentManager: FragmentManager,
    private val fragmentFactory: PlayerPagerFragmentFactory<T>
) : AbstractFragmentPagerAdapter<T>(fragmentManager) {

    override fun getEntities(): List<T> = currentEntities

    override fun setEntities(entities: List<T>) {
        this.currentEntities = entities.toMutableList()
        notifyDataSetChanged()
    }

    override fun getEntityIndex(entity: T): Int = currentEntities.indexOf(entity)

    override fun getLinkedEntity(position: Int): T = currentEntities[position]

    override fun getItem(position: Int): Fragment = fragmentFactory.create(currentEntities[position])
}
