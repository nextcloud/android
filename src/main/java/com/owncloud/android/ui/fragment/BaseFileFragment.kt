// package com.owncloud.android.ui.fragment
//
// import android.os.Bundle
// import android.text.TextUtils
// import android.view.LayoutInflater
// import android.view.View
// import android.view.ViewGroup
// import androidx.fragment.app.Fragment
// import androidx.fragment.app.FragmentTransaction
// import com.owncloud.android.MainApp
// import com.owncloud.android.R
// import com.owncloud.android.datamodel.OCFile
// import com.owncloud.android.lib.common.operations.RemoteOperation
// import com.owncloud.android.operations.RefreshFolderOperation
// import com.owncloud.android.ui.activity.FileDisplayActivity
// import com.owncloud.android.ui.activity.OnEnforceableRefreshListener
// import com.owncloud.android.ui.events.SearchEvent
// import org.parceler.Parcels
//
// class BaseFileFragment : Fragment(), OnEnforceableRefreshListener {
//
//     override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
//         return inflater.inflate(R.layout.fragment_all_file, container, false)
//     }
//
//     override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//         super.onViewCreated(view, savedInstanceState)
//         val searchEvent = Parcels.unwrap<SearchEvent>(
//             savedInstanceState!!.getParcelable(
//                 OCFileListFragment
//                     .SEARCH_EVENT
//             )
//         )
//         val fileListFragment = OCFileListFragment()
//         val bundle = Bundle()
//         bundle.putParcelable(OCFileListFragment.SEARCH_EVENT, Parcels.wrap(searchEvent))
//         fileListFragment.arguments = bundle
//         val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
//         transaction.replace(R.id.left_fragment_container, fileListFragment, FileDisplayActivity.TAG_LIST_OF_FILES)
//         transaction.commit()
//     }
//
//     fun getListOfFilesFragment(): OCFileListFragment? {
//         val listOfFiles: Fragment? = childFragmentManager.findFragmentByTag(FileDisplayActivity.TAG_LIST_OF_FILES)
//         return listOfFiles as? OCFileListFragment
//     }
//
//     override fun onRefresh(enforced: Boolean) {
//
//     }
//
//     override fun onRefresh() {
//         syncAndUpdateFolder(true)
//     }
//
//     private fun syncAndUpdateFolder(ignoreETag: Boolean) {
//         val listOfFiles = getListOfFilesFragment()
//         if (listOfFiles != null && !listOfFiles.isSearchFragment) {
//             val folder = listOfFiles.currentFile
//             folder?.let { startSyncFolderOperation(it, ignoreETag) }
//         }
//     }
//
//     fun startSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean) {
//         startSyncFolderOperation(folder, ignoreETag, false)
//     }
//
//     fun startSyncFolderOperation(folder: OCFile?, ignoreETag: Boolean, ignoreFocus: Boolean) {
//         // the execution is slightly delayed to allow the activity get the window focus if it's being started
//         // or if the method is called from a dialog that is being dismissed
//         if (TextUtils.isEmpty(searchQuery)) {
//             getHandler().postDelayed(
//                 Runnable {
//                     if (ignoreFocus || hasWindowFocus()) {
//                         val currentSyncTime = System.currentTimeMillis()
//                         mSyncInProgress = true
//
//                         // perform folder synchronization
//                         val synchFolderOp: RemoteOperation = RefreshFolderOperation(
//                             folder,
//                             currentSyncTime,
//                             false,
//                             ignoreETag,
//                             getStorageManager(),
//                             getAccount(),
//                             getApplicationContext()
//                         )
//                         synchFolderOp.execute(
//                             getAccount(),
//                             MainApp.getAppContext(),
//                             this@FileDisplayActivity,
//                             null,
//                             null
//                         )
//                         val fragment = getListOfFilesFragment()
//                         if (fragment != null) {
//                             fragment.isLoading = true
//                         }
//                         setBackgroundText()
//                     } // else: NOTHING ; lets' not refresh when the user rotates the device but there is
//                     // another window floating over
//                 },
//                 FileDisplayActivity.DELAY_TO_REQUEST_REFRESH_OPERATION_LATER
//             )
//         }
//     }
// }
