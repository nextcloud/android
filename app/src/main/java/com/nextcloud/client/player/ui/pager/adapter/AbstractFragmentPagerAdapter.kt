/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.nextcloud.client.player.ui.pager.adapter

import android.view.ViewGroup
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

abstract class AbstractFragmentPagerAdapter<T>(fragmentManager: FragmentManager) :
    FragmentStatePagerAdapter(fragmentManager) {
    protected var currentEntities = mutableListOf<T>()
    private var currentEntity: T? = null
    private val cachedItems = mutableListOf<Pair<T, Fragment>>()

    abstract fun getEntities(): List<T>

    abstract fun setEntities(entities: List<T>)

    abstract fun getEntityIndex(entity: T): Int

    protected abstract fun getLinkedEntity(position: Int): T

    fun getEntityForPosition(position: Int): T = currentEntities[position]

    fun setCurrentEntity(entity: T?) {
        currentEntity = entity
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        val linkedEntity = getLinkedEntity(position)
        if (!findAndReplace(fragment, linkedEntity)) {
            cachedItems.add(Pair(linkedEntity, fragment))
        }
        return fragment
    }

    private fun findAndReplace(fragment: Fragment, linkedEntity: T): Boolean {
        for (pair in cachedItems) {
            if (pair.first == linkedEntity) {
                val newPair = Pair(pair.first, fragment)
                cachedItems.add(newPair)
                cachedItems.remove(pair)
                return true
            }
        }
        return false
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        for (pair in cachedItems) {
            if (pair.second == `object`) {
                cachedItems.remove(pair)
                break
            }
        }
        super.destroyItem(container, position, `object`)
    }

    override fun getItemPosition(`object`: Any): Int {
        for (pair in cachedItems) {
            if (pair.second == `object`) {
                return if (currentEntity != null && currentEntity == pair.first) {
                    super.getItemPosition(`object`)
                } else {
                    POSITION_NONE
                }
            }
        }
        return POSITION_NONE
    }

    override fun getCount(): Int = currentEntities.size
}
