/*
 * IONOS HiDrive Next - Android Client
 *
 * SPDX-FileCopyrightText: 2025 STRATO GmbH.
 * SPDX-License-Identifier: GPL-2.0
 */

package com.ionos.scanbot.util.collections

import java.util.*

internal class OrderedHashMap<K, V> : MutableMap<K, V> {

    private val orderedValues = ArrayList<V>()
    private val keyToIndexMap = HashMap<K, Int>()

    fun swapOrder(firstKey: K, secondKey: K) {
        val firstIndex = keyToIndexMap[firstKey]
        val secondIndex = keyToIndexMap[secondKey]

        if (firstIndex != null && secondIndex != null) {
            Collections.swap(orderedValues, firstIndex, secondIndex)
            keyToIndexMap[firstKey] = secondIndex
            keyToIndexMap[secondKey] = firstIndex
        }
    }

    override fun put(key: K, value: V): V? {
        if (keyToIndexMap.containsKey(key)) {
            keyToIndexMap[key]?.let { orderedValues[it] = value }
        } else {
            orderedValues.add(value)
            keyToIndexMap[key] = orderedValues.size - 1
        }

        return value
    }

    override val size: Int
        get() = orderedValues.size

    override fun containsKey(key: K) = keyToIndexMap.containsKey(key)

    override fun containsValue(value: V) = orderedValues.contains(value)

    override fun get(key: K): V? {
        return keyToIndexMap[key]
            ?.let { orderedValues[it] }
    }

    override fun isEmpty() = orderedValues.isEmpty()

    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = keyToIndexMap.entries
            .toMutableList()
            .also { it.sortBy { entry -> entry.value } }
            .map { Entry(it.key, orderedValues[it.value]) }
            .toMutableSet()

    override val keys: MutableSet<K>
        get() = keyToIndexMap.keys

    override val values: MutableCollection<V>
        get() = orderedValues.toMutableList()

    override fun clear() {
        orderedValues.clear()
        keyToIndexMap.clear()
    }

    override fun putAll(from: Map<out K, V>) {
        from.entries.forEach {
            put(it.key, it.value)
        }
    }

    override fun remove(key: K): V? {
        val respond: V? =
            keyToIndexMap[key]
                ?.let { orderedValues.removeAt(it) }

        val index = keyToIndexMap[key] ?: Int.MAX_VALUE
        keyToIndexMap.remove(key)
        keyToIndexMap.keys.forEach {
            val otherIndex = keyToIndexMap[it]
            if (otherIndex != null){
                if (otherIndex > index) keyToIndexMap[it] = otherIndex - 1
            }
        }

        return respond
    }

    inner class Entry(
        override val key: K,
        value: V,
    ) : MutableMap.MutableEntry<K, V> {
        private var innerValue = value
        override val value: V = innerValue

        override fun setValue(newValue: V): V {
            innerValue = newValue
            return innerValue
        }

    }

}