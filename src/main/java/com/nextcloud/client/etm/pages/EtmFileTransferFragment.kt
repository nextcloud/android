package com.nextcloud.client.etm.pages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.nextcloud.client.etm.EtmBaseFragment
import com.nextcloud.client.files.downloader.DownloadRequest
import com.nextcloud.client.files.downloader.Transfer
import com.nextcloud.client.files.downloader.TransferManager
import com.nextcloud.client.files.downloader.UploadRequest
import com.owncloud.android.R
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.db.OCUpload

class EtmFileTransferFragment : EtmBaseFragment() {

    companion object {
        private const val TEST_DOWNLOAD_DUMMY_PATH = "/test/dummy_file.txt"
    }

    class Adapter(private val inflater: LayoutInflater) : RecyclerView.Adapter<Adapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val type = view.findViewById<TextView>(R.id.etm_transfer_type)
            val typeIcon = view.findViewById<ImageView>(R.id.etm_transfer_type_icon)
            val uuid = view.findViewById<TextView>(R.id.etm_transfer_uuid)
            val path = view.findViewById<TextView>(R.id.etm_transfer_remote_path)
            val user = view.findViewById<TextView>(R.id.etm_transfer_user)
            val state = view.findViewById<TextView>(R.id.etm_transfer_state)
            val progress = view.findViewById<TextView>(R.id.etm_transfer_progress)
            private val progressRow = view.findViewById<View>(R.id.etm_transfer_progress_row)

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

        private var transfers = listOf<Transfer>()

        fun setStatus(status: TransferManager.Status) {
            transfers = listOf(status.pending, status.running, status.completed).flatten().reversed()
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = inflater.inflate(R.layout.etm_transfer_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount(): Int {
            return transfers.size
        }

        override fun onBindViewHolder(vh: ViewHolder, position: Int) {
            val transfer = transfers[position]

            val transferTypeStrId = when (transfer.request) {
                is DownloadRequest -> R.string.etm_transfer_type_download
                is UploadRequest -> R.string.etm_transfer_type_upload
            }

            val transferTypeIconId = when (transfer.request) {
                is DownloadRequest -> R.drawable.ic_cloud_download
                is UploadRequest -> R.drawable.ic_cloud_upload
            }

            vh.type.setText(transferTypeStrId)
            vh.typeIcon.setImageResource(transferTypeIconId)
            vh.uuid.text = transfer.uuid.toString()
            vh.path.text = transfer.request.file.remotePath
            vh.user.text = transfer.request.user.accountName
            vh.state.text = transfer.state.toString()
            if (transfer.progress >= 0) {
                vh.progressEnabled = true
                vh.progress.text = transfer.progress.toString()
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
        vm.transferManagerConnection.bind()
        vm.transferManagerConnection.registerStatusListener(this::onDownloaderStatusChanged)
    }

    override fun onPause() {
        super.onPause()
        vm.transferManagerConnection.unbind()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_etm_file_transfer, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.etm_test_download -> {
                scheduleTestDownload(); true
            }
            R.id.etm_test_upload -> {
                scheduleTestUpload(); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun scheduleTestDownload() {
        val request = DownloadRequest(
            vm.currentUser,
            OCFile(TEST_DOWNLOAD_DUMMY_PATH),
            true
        )
        vm.transferManagerConnection.enqueue(request)
    }

    private fun scheduleTestUpload() {
        val request = UploadRequest(
            vm.currentUser,
            OCUpload(TEST_DOWNLOAD_DUMMY_PATH, TEST_DOWNLOAD_DUMMY_PATH, vm.currentUser.accountName),
            true
        )
        vm.transferManagerConnection.enqueue(request)
    }

    private fun onDownloaderStatusChanged(status: TransferManager.Status) {
        adapter.setStatus(status)
    }
}
