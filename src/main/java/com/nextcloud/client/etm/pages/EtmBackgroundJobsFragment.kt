/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Chris Narkiewicz
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nextcloud.client.etm.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.etm.EtmBaseFragment
import com.nextcloud.client.jobs.JobInfo
import com.owncloud.android.R
import java.text.SimpleDateFormat
import java.util.Locale

class EtmBackgroundJobsFragment : EtmBaseFragment() {

    class Adapter(private val inflater: LayoutInflater) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val uuid = view.findViewById<TextView>(R.id.etm_background_job_uuid)
            val name = view.findViewById<TextView>(R.id.etm_background_job_name)
            val user = view.findViewById<TextView>(R.id.etm_background_job_user)
            val state = view.findViewById<TextView>(R.id.etm_background_job_state)
            val started = view.findViewById<TextView>(R.id.etm_background_job_started)
            val progress = view.findViewById<TextView>(R.id.etm_background_job_progress)
            private val progressRow = view.findViewById<View>(R.id.etm_background_job_progress_row)

            var progressEnabled: Boolean = progressRow.visibility == View.VISIBLE
                get() {
                    return progressRow.visibility == View.VISIBLE
                }
                set(value) {
                    field = value
                    progressRow.visibility = if (value) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
        }

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:MM:ssZ", Locale.getDefault())
        var backgroundJobs: List<JobInfo> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = inflater.inflate(R.layout.etm_background_job_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return backgroundJobs.size
        }

        override fun onBindViewHolder(vh: ViewHolder, position: Int) {
            val info = backgroundJobs[position]
            vh.uuid.text = info.id.toString()
            vh.name.text = info.name
            vh.user.text = info.user
            vh.state.text = info.state
            vh.started.text = dateFormat.format(info.started)
            if (info.progress >= 0) {
                vh.progressEnabled = true
                vh.progress.text = info.progress.toString()
            } else {
                vh.progressEnabled = false
            }
        }
    }

    private lateinit var list: RecyclerView
    private lateinit var adapter: Adapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_etm_background_jobs, container, false)
        adapter = Adapter(inflater)
        list = view.findViewById(R.id.etm_background_jobs_list)
        list.layoutManager = LinearLayoutManager(context)
        list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        list.adapter = adapter
        vm.backgroundJobs.observe(viewLifecycleOwner, Observer { onBackgroundJobsUpdated(it) })
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_etm_background_jobs, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.etm_background_jobs_cancel -> {
                vm.cancelAllJobs(); true
            }
            R.id.etm_background_jobs_prune -> {
                vm.pruneJobs(); true
            }
            R.id.etm_background_jobs_start_test -> {
                vm.startTestJob(periodic = false); true
            }
            R.id.etm_background_jobs_schedule_test -> {
                vm.startTestJob(periodic = true); true
            }
            R.id.etm_background_jobs_cancel_test -> {
                vm.cancelTestJob(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onBackgroundJobsUpdated(backgroundJobs: List<JobInfo>) {
        adapter.backgroundJobs = backgroundJobs
    }
}
