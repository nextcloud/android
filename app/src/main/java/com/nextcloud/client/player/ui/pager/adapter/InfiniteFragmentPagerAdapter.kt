/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Your Name <your@email.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.pager.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.nextcloud.client.player.ui.pager.PlayerPagerFragmentFactory

class InfiniteFragmentPagerAdapter<T>(
    fragmentManager: FragmentManager,
    private val fragmentFactory: PlayerPagerFragmentFactory<T>
) : AbstractFragmentPagerAdapter<T>(fragmentManager) {

    override fun getEntities(): List<T> = if (currentEntities.size >
        1
    ) {
        removeStubs(currentEntities)
    } else {
        currentEntities
    }

    override fun setEntities(entities: List<T>) {
        this.currentEntities = if (entities.size > 1) {
            addStubs(entities)
        } else {
            entities.toMutableList()
        }
        notifyDataSetChanged()
    }

    override fun getEntityIndex(entity: T): Int = if (currentEntities.size > 1) {
        val entities = removeStubs(currentEntities)
        val index = entities.indexOf(entity)
        if (index != -1) index + 1 else index
    } else {
        currentEntities.indexOf(entity)
    }

    override fun getLinkedEntity(position: Int): T {
        val entities = getEntities()
        return when (position) {
            0 -> entities[entities.size - 1]
            entities.size + 1 -> entities[0]
            else -> entities[position - 1]
        }
    }

    private fun addStubs(sources: List<T>): MutableList<T> {
        val result = sources.toMutableList()
        result.add(0, result[result.size - 1])
        result.add(result[1])
        return result
    }

    private fun removeStubs(sources: List<T>): MutableList<T> {
        val result = sources.toMutableList()
        result.removeAt(0)
        result.removeAt(result.size - 1)
        return result
    }

    override fun getItem(position: Int): Fragment = fragmentFactory.create(currentEntities[position])
}
