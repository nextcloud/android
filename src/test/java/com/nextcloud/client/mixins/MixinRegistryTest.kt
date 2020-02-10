/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.mixins

import android.os.Bundle
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.same
import org.junit.Test
import org.mockito.Mockito

class MixinRegistryTest {

    @Test
    fun `callbacks are invoked in order of calls and mixin registration`() {
        // GIVEN
        //      registry has 2 mixins registered
        val registry = MixinRegistry()
        val firstMixin = mock<ActivityMixin>()
        val secondMixin = mock<ActivityMixin>()
        registry.add(firstMixin, secondMixin)

        // WHEN
        //      all lifecycle callbacks are invoked
        val bundle = mock<Bundle>()
        registry.onCreate(bundle)
        registry.onStart()
        registry.onResume()
        registry.onPause()
        registry.onStop()
        registry.onDestroy()

        // THEN
        //      callbacks are invoked in order of mixin registration
        //      callbacks are invoked in order of registry calls
        Mockito.inOrder(firstMixin, secondMixin).apply {
            verify(firstMixin).onCreate(same(bundle))
            verify(secondMixin).onCreate(same(bundle))
            verify(firstMixin).onStart()
            verify(secondMixin).onStart()
            verify(firstMixin).onResume()
            verify(secondMixin).onResume()
            verify(firstMixin).onPause()
            verify(secondMixin).onPause()
            verify(firstMixin).onStop()
            verify(secondMixin).onStop()
            verify(firstMixin).onDestroy()
            verify(secondMixin).onDestroy()
        }
    }
}
