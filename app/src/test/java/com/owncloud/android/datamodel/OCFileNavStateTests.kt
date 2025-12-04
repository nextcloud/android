/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2025 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.datamodel

import org.junit.Test

class OCFileNavStateTests {

    @Test
    fun testGetNavStateWhenGivenNullPathShouldReturnNull() {
        val navState = OCFileNavState.getNavState(null)
        assert(navState == null)
    }

    @Test
    fun testGetNavStateWhenGivenEmptyPathShouldReturnNull() {
        val navState = OCFileNavState.getNavState("")
        assert(navState == null)
    }

    @Test
    fun testGetNavStateWhenGivenInvalidPathShouldReturnNull() {
        val navState = OCFileNavState.getNavState("null")
        assert(navState == null)
    }

    @Test
    fun testGetNavStateWhenGivenRootPathShouldReturnRootState() {
        val navState = OCFileNavState.getNavState("/")
        assert(navState == OCFileNavState.Root)
    }

    @Test
    fun testGetNavStateWhenGivenRootSubDirOfRootPathShouldReturnSubDirOfRootState() {
        val navState = OCFileNavState.getNavState("/Abc/")
        assert(navState == OCFileNavState.SubDirOfRoot)
    }

    @Test
    fun testGetNavStateWhenGivenRootSubDirOfDirPathShouldReturnSubDirOfDirState() {
        val navState = OCFileNavState.getNavState("/Abc/Ba/")
        assert(navState == OCFileNavState.SubDirOfDir)
    }
}
