/*
 * Nextcloud Android client application
 *
 * @author Alper Ozturk
 * Copyright (C) 2024 Alper Ozturk
 * Copyright (C) 2024 Nextcloud GmbH
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.nextcloud.ui.composeFragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.nextcloud.client.assistant.AssistantScreen
import com.nextcloud.utils.extensions.getSerializableArgument
import com.owncloud.android.databinding.FragmentComposeViewBinding

class ComposeFragment : Fragment() {

    private var _binding: FragmentComposeViewBinding? = null

    private val binding get() = _binding!!
    private var destination: ComposeDestinations? = null

    companion object {
        const val destinationKey = "destinationKey"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentComposeViewBinding.inflate(inflater, container, false)
        destination = arguments.getSerializableArgument(destinationKey, ComposeDestinations::class.java)

        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                when(destination) {
                    ComposeDestinations.AssistantScreen -> {
                        AssistantScreen()
                    }
                    else -> {

                    }
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
