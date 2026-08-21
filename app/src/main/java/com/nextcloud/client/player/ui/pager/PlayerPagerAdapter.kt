/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.player.ui.pager

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.nextcloud.client.player.model.file.PlaybackFile

/**
 * Pads the item list with a copy of the last item at the front and a copy of the first item at the end, so that
 * [PlayerPager] can wrap around and give the impression of an endless pager.
 */
@Suppress("DEPRECATION")
class PlayerPagerAdapter(fragmentManager: FragmentManager, private val createFragment: (PlaybackFile) -> Fragment) :
    FragmentStatePagerAdapter(fragmentManager) {

    private var paddedEntities = mutableListOf<PlaybackFile>()

    fun getEntities(): List<PlaybackFile> = if (isPadded()) removeStubs(paddedEntities) else paddedEntities

    fun setEntities(entities: List<PlaybackFile>) {
        paddedEntities = if (entities.size > 1) addStubs(entities) else entities.toMutableList()
        notifyDataSetChanged()
    }

    fun getEntityIndex(entity: PlaybackFile): Int = if (isPadded()) {
        val index = removeStubs(paddedEntities).indexOf(entity)
        if (index != -1) index + 1 else index
    } else {
        paddedEntities.indexOf(entity)
    }

    fun getEntityForPosition(position: Int): PlaybackFile = paddedEntities[position]

    override fun getItem(position: Int): Fragment = createFragment(paddedEntities[position])

    override fun getCount(): Int = paddedEntities.size

    override fun getItemPosition(item: Any): Int = POSITION_NONE

    private fun isPadded(): Boolean = paddedEntities.size > 1

    private fun addStubs(sources: List<PlaybackFile>): MutableList<PlaybackFile> {
        val result = sources.toMutableList()
        result.add(0, result[result.size - 1])
        result.add(result[1])
        return result
    }

    private fun removeStubs(sources: List<PlaybackFile>): MutableList<PlaybackFile> {
        val result = sources.toMutableList()
        result.removeAt(0)
        result.removeAt(result.size - 1)
        return result
    }
}
