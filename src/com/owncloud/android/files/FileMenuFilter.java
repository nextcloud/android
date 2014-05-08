package com.owncloud.android.files;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.actionbarsherlock.app.SherlockFragment;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;

public class FileMenuFilter {

    private OCFile mFile;
    private ComponentsGetter mComponentsGetter;
    private Account mAccount;
    private Context mContext;
    private SherlockFragment mFragment;

    public void setFile(OCFile targetFile) {
        mFile = targetFile;
    }

    public void setAccount(Account account) {
        mAccount = account;
    }

    public void setComponentGetter(ComponentsGetter cg) {
        mComponentsGetter = cg;
    }

    public void setContext(Context context) {
        mContext = context;
    }
    
    public void setFragment(SherlockFragment fragment) {
        mFragment = fragment;  
    }

    public void filter(Menu menu) {
        List<Integer> toShow = new ArrayList<Integer>();  
        List<Integer> toDisable = new ArrayList<Integer>();  
        List<Integer> toHide = new ArrayList<Integer>();    
        
        filter(toShow, toDisable, toHide);
        
        MenuItem item = null;
        for (int i : toShow) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(true);
                item.setEnabled(true);
            }
        }
        
        for (int i : toDisable) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(true);
                item.setEnabled(false);
            }
        }
        
        for (int i : toHide) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
    }

    /**
     * ActionBarSherlock...
     * 
     */
    public void filter(com.actionbarsherlock.view.Menu menu) {

        List<Integer> toShow = new ArrayList<Integer>();
        List<Integer> toDisable = new ArrayList<Integer>(); 
        List<Integer> toHide = new ArrayList<Integer>();
        
        filter(toShow, toDisable, toHide);

        com.actionbarsherlock.view.MenuItem item = null;
        for (int i : toShow) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(true);
                item.setEnabled(true);
            }
        }
        for (int i : toDisable) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(true);
                item.setEnabled(false);
            }
        }
        for (int i : toHide) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(false);
                item.setEnabled(false);
            }
        }
    }

    private void filter(List<Integer> toShow, List<Integer> toDisable, List <Integer> toHide) {
        boolean downloading = false;
        boolean uploading = false;
        if (mComponentsGetter != null && mFile != null && mAccount != null) {
            FileDownloaderBinder downloaderBinder = mComponentsGetter.getFileDownloaderBinder();
            downloading = downloaderBinder != null && downloaderBinder.isDownloading(mAccount, mFile);
            FileUploaderBinder uploaderBinder = mComponentsGetter.getFileUploaderBinder();
            uploading = uploaderBinder != null && uploaderBinder.isUploading(mAccount, mFile);
        }
        
        // R.id.action_download_file
        if (mFile == null || mFile.isFolder() || mFile.isDown() || downloading || uploading ||
                (mFragment != null && (
                        mFragment instanceof PreviewImageFragment ||
                        mFragment instanceof PreviewMediaFragment
                        )
                )
            ) {
            toHide.add(R.id.action_download_file);
            
        } else {
            toShow.add(R.id.action_download_file);
        }
        
        // R.id.action_rename_file
        if ((downloading || uploading) && 
                (mFragment != null && mFragment instanceof OCFileListFragment) 
            ) {
            toDisable.add(R.id.action_rename_file);
            
        } else if (mFile == null || downloading || uploading ||
                (mFragment != null && (
                        mFragment instanceof PreviewImageFragment ||
                        mFragment instanceof PreviewMediaFragment
                        )
                ) 
            ) {
            toHide.add(R.id.action_rename_file);
            
        } else {
            toShow.add(R.id.action_rename_file);
        }
        
        // R.id.action_remove_file
        if ((downloading || uploading) && 
                (mFragment != null && mFragment instanceof OCFileListFragment) 
            ) {
            toDisable.add(R.id.action_remove_file);
            
        } else if (mFile == null || downloading || uploading) {
            toHide.add(R.id.action_remove_file);
            
        } else {
            toShow.add(R.id.action_remove_file);
        }
        
        // R.id.action_open_file_with
        if (mFile == null || mFile.isFolder() || !mFile.isDown() || downloading || uploading || 
                (mFragment != null && mFragment instanceof OCFileListFragment)) {
            toHide.add(R.id.action_open_file_with);
            
        } else {
            toShow.add(R.id.action_open_file_with);
        }
        
        
        // R.id.action_cancel_download
        if (mFile == null || !downloading || mFile.isFolder() || 
                (mFragment != null && (
                        (mFragment instanceof PreviewImageFragment) ||
                        (mFragment instanceof PreviewMediaFragment)
                        )
                ) 
            ) {
            toHide.add(R.id.action_cancel_download);
        } else {
            toShow.add(R.id.action_cancel_download);
        }
        
        // R.id.action_cancel_upload
        if (mFile == null || !uploading || mFile.isFolder() ||
                (mFragment != null && (
                        (mFragment instanceof PreviewImageFragment) ||
                        (mFragment instanceof PreviewMediaFragment)
                        )
                ) 
            ) {
            toHide.add(R.id.action_cancel_upload);
        } else {
            toShow.add(R.id.action_cancel_upload);
        }
        
        // R.id.action_sync_file
        if (mFile == null || mFile.isFolder() || !mFile.isDown() || downloading || uploading ||
                (mFragment != null && mFragment instanceof PreviewMediaFragment)
            ) {
            toHide.add(R.id.action_sync_file);
        } else {
            toShow.add(R.id.action_sync_file);
        }
        
        // R.id.action_share_file  // TODO add check on SHARE available on server side?
        if (mFile == null) {
            toHide.add(R.id.action_share_file);
        } else {
            toShow.add(R.id.action_share_file);
        }
        
        // R.id.action_unshare_file  // TODO add check on SHARE available on server side?
        if (mFile == null || !mFile.isShareByLink()) { 
            toHide.add(R.id.action_unshare_file);
        } else {
            toShow.add(R.id.action_unshare_file);
        }
        
        
        // R.id.action_see_details
        if (mFile == null || mFile.isFolder() || (mFragment != null && mFragment instanceof FileDetailFragment)) {
            // TODO check dual pane when FileDetailFragment is shown
            toHide.add(R.id.action_see_details);
        } else {
            toShow.add(R.id.action_see_details);
        }
        
        // R.id.action_send_file
        boolean sendEnabled = (mContext != null &&
                mContext.getString(R.string.send_files_to_other_apps).equalsIgnoreCase("on"));
        if (mFile != null && sendEnabled && !mFile.isFolder()) {
            toShow.add(R.id.action_send_file);
        } else {
            toHide.add(R.id.action_send_file);
        }

    }

}
