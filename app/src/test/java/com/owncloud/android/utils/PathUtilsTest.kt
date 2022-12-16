/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
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
private val indirectAncestors: List<Pair<String, String>> = listOf(
    Pair("/bar", "/bar/foo/baz.tgz"),
    Pair("/bar/", "/bar/foo/baz.tgz"),
    Pair("/bar/", "/bar/foo/baz/"),
    Pair("/bar/", "/bar/foo/baz")
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
                val otherAncestors: Array<Array<Any>> = indirectAncestors.map {
                    @Suppress("UNCHECKED_CAST")
                    arrayOf(it.first, it.second, false) as Array<Any>
                }.toTypedArray()
                return directParents + nonAncestors + otherAncestors
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
                val otherAncestors: Array<Array<Any>> = indirectAncestors.map {
                    @Suppress("UNCHECKED_CAST")
                    arrayOf(it.first, it.second, true) as Array<Any>
                }.toTypedArray()
                return directParents + nonAncestors + otherAncestors
            }
        }
    }
}
