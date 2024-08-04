/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.mixins

import android.os.Bundle
import org.junit.Test
import org.mockito.Mockito
import org.mockito.kotlin.mock
import org.mockito.kotlin.same

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
