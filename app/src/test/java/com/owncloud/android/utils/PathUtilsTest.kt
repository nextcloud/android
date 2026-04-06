/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2022 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Suite

private val directParents: Array<Array<Any>> = arrayOf(
    arrayOf("/bar", "/bar/foo.tgz", true),
    arrayOf("/bar/", "/bar/foo.tgz", true),
    arrayOf("/bar/", "/bar/foo/", true),
    arrayOf("/bar/", "/bar/foo", true),
    arrayOf("/", "/bar/", true),
    arrayOf("/bar/", "/foo/bar", false)
)

private val nonAncestors: Array<Array<Any>> = arrayOf(
    arrayOf("/bar/", "/", false),
    arrayOf("/bar/", "", false),
    arrayOf("/", "", false),
    arrayOf("", "", false),
    arrayOf("", "/", false)
)

/**
 * These should return `false` for [PathUtils.isDirectParent] but `true` for [PathUtils.isAncestor]
 */
private val indirectAncestors: Array<Array<Any>> = arrayOf(
    arrayOf("/bar", "/bar/foo/baz.tgz", true),
    arrayOf("/bar/", "/bar/foo/baz.tgz", true),
    arrayOf("/bar/", "/bar/foo/baz/", true),
    arrayOf("/bar/", "/bar/foo/baz", true)
)

@RunWith(Suite::class)
@Suite.SuiteClasses(
    PathUtilsTest.IsDirectParent::class,
    PathUtilsTest.IsAncestor::class
)
class PathUtilsTest {

    @RunWith(Parameterized::class)
    internal class IsDirectParent(
        private val folderPath: String,
        private val filePath: String,
        private val isParent: Boolean
    ) {

        @Test
        fun testIsParent() {
            assertEquals("Wrong isParentPath result", isParent, PathUtils.isDirectParent(folderPath, filePath))
        }

        companion object {
            @Parameterized.Parameters(name = "{0}, {1} => {2}")
            @JvmStatic
            fun urls(): Array<Array<Any>> {
                indirectAncestors.forEach {
                    it[2] = false
                }
                return directParents + nonAncestors + indirectAncestors
            }
        }
    }

    @RunWith(Parameterized::class)
    internal class IsAncestor(
        private val folderPath: String,
        private val filePath: String,
        private val isAscendant: Boolean
    ) {

        @Test
        fun testIsAncestor() {
            assertEquals("Wrong isParentPath result", isAscendant, PathUtils.isAncestor(folderPath, filePath))
        }

        companion object {
            @Parameterized.Parameters(name = "{0}, {1} => {2}")
            @JvmStatic
            fun urls(): Array<Array<Any>> {
                indirectAncestors.forEach {
                    it[2] = true // setting explicitly again to true for better readability
                }
                return directParents + nonAncestors + indirectAncestors
            }
        }
    }
}
