/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2020 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.etm.pages

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.di.Injectable
import com.nextcloud.client.etm.EtmBaseFragment
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.JobInfo
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.R
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class EtmBackgroundJobsFragment : EtmBaseFragment(), Injectable {

    @Inject
    lateinit var preferences: AppPreferences

    class Adapter(private val inflater: LayoutInflater, private val preferences: AppPreferences) :
        RecyclerView.Adapter<Adapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val uuid = view.findViewById<TextView>(R.id.etm_background_job_uuid)
            val name = view.findViewById<TextView>(R.id.etm_background_job_name)
            val user = view.findViewById<TextView>(R.id.etm_background_job_user)
            val state = view.findViewById<TextView>(R.id.etm_background_job_state)
            val started = view.findViewById<TextView>(R.id.etm_background_job_started)
            val progress = view.findViewById<TextView>(R.id.etm_background_job_progress)
            private val progressRow = view.findViewById<View>(R.id.etm_background_job_progress_row)
            val executionCount = view.findViewById<TextView>(R.id.etm_background_execution_count)
            val executionLog = view.findViewById<TextView>(R.id.etm_background_execution_logs)
            private val executionLogRow = view.findViewById<View>(R.id.etm_background_execution_logs_row)
            val executionTimesRow = view.findViewById<View>(R.id.etm_background_execution_times_row)

            var progressEnabled: Boolean = progressRow.isVisible
                get() {
                    return progressRow.isVisible
                }
                set(value) {
                    field = value
                    progressRow.visibility = if (value) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }

            var logsEnabled: Boolean = executionLogRow.isVisible
                get() {
                    return executionLogRow.isVisible
                }
                set(value) {
                    field = value
                    executionLogRow.visibility = if (value) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
        }

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:MM:ssZ", Locale.getDefault())
        var backgroundJobs: List<JobInfo> = emptyList()
            @SuppressLint("NotifyDataSetChanged")
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = inflater.inflate(R.layout.etm_background_job_list_item, parent, false)
            val viewHolder = ViewHolder(view)
            viewHolder.logsEnabled = false
            viewHolder.executionTimesRow.visibility = View.GONE
            view.setOnClickListener {
                viewHolder.logsEnabled = !viewHolder.logsEnabled
            }
            return viewHolder
        }

        override fun getItemCount(): Int {
            return backgroundJobs.size
        }

        @SuppressLint("SetTextI18n")
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

            val logs = preferences.readLogEntry()
            val logsForThisWorker =
                logs.filter { BackgroundJobManagerImpl.parseTag(it.workerClass)?.second == info.workerClass }
            if (logsForThisWorker.isNotEmpty()) {
                vh.executionTimesRow.visibility = View.VISIBLE
                vh.executionCount.text =
                    "${logsForThisWorker.filter { it.started != null }.size} " +
                    "(${logsForThisWorker.filter { it.finished != null }.size})"
                var logText = "Worker Logs\n\n" +
                    "*** Does NOT differentiate between immediate or periodic kinds of Work! ***\n" +
                    "*** Times run in 48h: Times started (Times finished) ***\n"
                logsForThisWorker.forEach {
                    logText += "----------------------\n"
                    logText += "Worker ${BackgroundJobManagerImpl.parseTag(it.workerClass)?.second}\n"
                    logText += if (it.started == null) {
                        "ENDED at\n${it.finished}\nWith result: ${it.result}\n"
                    } else {
                        "STARTED at\n${it.started}\n"
                    }
                }
                vh.executionLog.text = logText
            } else {
                vh.executionLog.text = "Worker Logs\n\n" +
                    "No Entries -> Maybe logging is not implemented for Worker or it has not run yet."
                vh.executionCount.text = "0"
                vh.executionTimesRow.visibility = View.GONE
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
        adapter = Adapter(inflater, preferences)
        list = view.findViewById(R.id.etm_background_jobs_list)
        list.layoutManager = LinearLayoutManager(context)
        list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        list.adapter = adapter
        vm.backgroundJobs.observe(viewLifecycleOwner, Observer { onBackgroundJobsUpdated(it) })
        return view
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_etm_background_jobs, menu)
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.etm_background_jobs_cancel -> {
                vm.cancelAllJobs()
                true
            }

            R.id.etm_background_jobs_prune -> {
                vm.pruneJobs()
                true
            }

            R.id.etm_background_jobs_start_test -> {
                vm.startTestJob(periodic = false)
                true
            }

            R.id.etm_background_jobs_schedule_test -> {
                vm.startTestJob(periodic = true)
                true
            }

            R.id.etm_background_jobs_cancel_test -> {
                vm.cancelTestJob()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onBackgroundJobsUpdated(backgroundJobs: List<JobInfo>) {
        adapter.backgroundJobs = backgroundJobs
    }
}
