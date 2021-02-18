package com.nextcloud.client.etm.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.etm.EtmBaseFragment
import com.nextcloud.client.files.downloader.Direction
import com.nextcloud.client.files.downloader.Transfer
import com.nextcloud.client.files.downloader.TransferManager
import com.nextcloud.client.files.downloader.Request
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile

class EtmDownloaderFragment : EtmBaseFragment() {

    companion object {
        private const val TEST_DOWNLOAD_DUMMY_PATH = "/test/dummy_file.txt"
    }

    class Adapter(private val inflater: LayoutInflater) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val uuid = view.findViewById<TextView>(R.id.etm_download_uuid)
            val path = view.findViewById<TextView>(R.id.etm_download_path)
            val user = view.findViewById<TextView>(R.id.etm_download_user)
            val state = view.findViewById<TextView>(R.id.etm_download_state)
            val progress = view.findViewById<TextView>(R.id.etm_download_progress)
            private val progressRow = view.findViewById<View>(R.id.etm_download_progress_row)

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

        private var downloads = listOf<Transfer>()

        fun setStatus(status: TransferManager.Status) {
            downloads = listOf(status.pending, status.running, status.completed).flatten().reversed()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = inflater.inflate(R.layout.etm_download_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return downloads.size
        }

        override fun onBindViewHolder(vh: ViewHolder, position: Int) {
            val download = downloads[position]
            vh.uuid.text = download.uuid.toString()
            vh.path.text = download.request.file.remotePath
            vh.user.text = download.request.user.accountName
            vh.state.text = download.state.toString()
            if (download.progress >= 0) {
                vh.progressEnabled = true
                vh.progress.text = download.progress.toString()
            } else {
                vh.progressEnabled = false
            }
        }
    }

    private lateinit var adapter: Adapter
    private lateinit var list: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_etm_downloader, container, false)
        adapter = Adapter(inflater)
        list = view.findViewById(R.id.etm_download_list)
        list.layoutManager = LinearLayoutManager(context)
        list.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        list.adapter = adapter
        return view
    }

    override fun onResume() {
        super.onResume()
        vm.downloaderConnection.bind()
        vm.downloaderConnection.registerStatusListener(this::onDownloaderStatusChanged)
    }

    override fun onPause() {
        super.onPause()
        vm.downloaderConnection.unbind()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_etm_downloader, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.etm_test_download -> {
                scheduleTestDownload(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun scheduleTestDownload() {
        val request = Request(
            vm.currentUser,
            OCFile(TEST_DOWNLOAD_DUMMY_PATH),
            Direction.DOWNLOAD,
            true
        )
        vm.downloaderConnection.enqueue(request)
    }

    private fun onDownloaderStatusChanged(status: TransferManager.Status) {
        adapter.setStatus(status)
    }
}
