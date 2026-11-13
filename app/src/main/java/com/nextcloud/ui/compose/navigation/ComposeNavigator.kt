/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.ui.compose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList

class ComposeNavigator(private val entries: SnapshotStateList<ComposeDestination>, private val onExit: () -> Unit) {

    val backStack: List<ComposeDestination>
        get() = entries

    val current: ComposeDestination
        get() = entries.last()

    fun navigate(destination: ComposeDestination) {
        entries.add(destination)
    }

    fun goBack() {
        if (entries.size > 1) {
            entries.removeLastOrNull()
        } else {
            onExit()
        }
    }
}

private val backStackSaver = listSaver<SnapshotStateList<ComposeDestination>, ComposeDestination>(
    save = { it.toList() },
    restore = { it.toMutableStateList() }
)

@Composable
fun rememberComposeNavigator(startDestination: ComposeDestination, onExit: () -> Unit): ComposeNavigator {
    val entries = rememberSaveable(saver = backStackSaver) { mutableStateListOf(startDestination) }
    val currentOnExit by rememberUpdatedState(onExit)
    return remember(entries) { ComposeNavigator(entries) { currentOnExit() } }
}
