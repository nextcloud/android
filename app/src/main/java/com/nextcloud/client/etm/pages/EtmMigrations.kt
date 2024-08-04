/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.etm.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.nextcloud.client.etm.EtmBaseFragment
import com.owncloud.android.R
import com.owncloud.android.databinding.FragmentEtmMigrationsBinding
import java.util.Locale

class EtmMigrations : EtmBaseFragment() {
    private var _binding: FragmentEtmMigrationsBinding? = null
    val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentEtmMigrationsBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        showStatus()
    }

    fun showStatus() {
        val builder = StringBuilder()
        val status = vm.migrationsStatus.toString().toLowerCase(Locale.US)
        builder.append("Migration status: $status\n")
        val lastMigratedVersion = if (vm.lastMigratedVersion >= 0) {
            vm.lastMigratedVersion.toString()
        } else {
            "never"
        }
        builder.append("Last migrated version: $lastMigratedVersion\n")
        builder.append("Migrations:\n")
        vm.migrationsInfo.forEach {
            val migrationStatus = if (it.applied) {
                "applied"
            } else {
                "pending"
            }
            builder.append(" - ${it.id} ${it.description} - $migrationStatus\n")
        }
        binding.etmMigrationsText.text = builder.toString()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_etm_migrations, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.etm_migrations_delete -> {
                onDeleteMigrationsClicked()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onDeleteMigrationsClicked() {
        vm.clearMigrations()
        showStatus()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }
}
