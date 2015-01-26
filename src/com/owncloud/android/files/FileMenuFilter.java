/* ownCloud Android client application
 *   Copyright (C) 2014 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.files;

import java.util.ArrayList;
import java.util.List;

import android.accounts.Account;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.files.services.FileUploader.FileUploaderBinder;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.activity.ComponentsGetter;

/**
 * Filters out the file actions available in a given {@link Menu} for a given {@link OCFile} 
 * according to the current state of the latest. 
 * 
 * @author David A. Velasco
 */
public class FileMenuFilter {

    private OCFile mFile;
    private ComponentsGetter mComponentsGetter;
    private Account mAccount;
    private Context mContext;
    
    /**
     * Constructor
     * 
     * @param targetFile        {@link OCFile} target of the action to filter in the {@link Menu}.
     * @param account           ownCloud {@link Account} holding targetFile.
     * @param cg                Accessor to app components, needed to access the
     *                          {@link FileUploader} and {@link FileDownloader} services
     * @param context           Android {@link Context}, needed to access build setup resources.
     */
    public FileMenuFilter(OCFile targetFile, Account account, ComponentsGetter cg, Context context) {
        mFile = targetFile;
        mAccount = account;
        mComponentsGetter = cg;
        mContext = context;
    }
    
    
    /**
     * Filters out the file actions available in the passed {@link Menu} taken into account
     * the state of the {@link OCFile} held by the filter.
     *  
     * @param menu              Options or context menu to filter.
     */
    public void filter(Menu menu) {
        List<Integer> toShow = new ArrayList<Integer>();  
        List<Integer> toHide = new ArrayList<Integer>();    
        
        filter(toShow, toHide);
        
        MenuItem item = null;
        for (int i : toShow) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(true);
                item.setEnabled(true);
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
     * Filters out the file actions available in the passed {@link Menu} taken into account
     * the state of the {@link OCFile} held by the filter.
     * 
     * Second method needed thanks to ActionBarSherlock.
     * 
     * TODO Get rid of it when ActionBarSherlock is replaced for newer Android Support Library.
     *  
     * @param menu              Options or context menu to filter.
     */
    public void filter(com.actionbarsherlock.view.Menu menu) {

        List<Integer> toShow = new ArrayList<Integer>();
        List<Integer> toHide = new ArrayList<Integer>();
        
        filter(toShow, toHide);

        com.actionbarsherlock.view.MenuItem item = null;
        for (int i : toShow) {
            item = menu.findItem(i);
            if (item != null) {
                item.setVisible(true);
                item.setEnabled(true);
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
     * Performs the real filtering, to be applied in the {@link Menu} by the caller methods.
     * 
     * Decides what actions must be shown and hidden.
     *  
     * @param toShow            List to save the options that must be shown in the menu. 
     * @param toHide            List to save the options that must be shown in the menu.
     */
    private void filter(List<Integer> toShow, List <Integer> toHide) {
        boolean downloading = false;
        boolean uploading = false;
        if (mComponentsGetter != null && mFile != null && mAccount != null) {
            FileDownloaderBinder downloaderBinder = mComponentsGetter.getFileDownloaderBinder();
            downloading = (downloaderBinder != null && downloaderBinder.isDownloading(mAccount, mFile));
            OperationsServiceBinder opsBinder = mComponentsGetter.getOperationsServiceBinder();
            downloading |= (opsBinder != null && opsBinder.isSynchronizing(mAccount, mFile.getRemotePath()));
            FileUploaderBinder uploaderBinder = mComponentsGetter.getFileUploaderBinder();
            uploading = (uploaderBinder != null && uploaderBinder.isUploading(mAccount, mFile));
        }
        
        /// decision is taken for each possible action on a file in the menu
        
        // DOWNLOAD 
        if (mFile == null || mFile.isDown() || downloading || uploading) {
            toHide.add(R.id.action_download_file);
            
        } else {
            toShow.add(R.id.action_download_file);
        }
        
        // RENAME
        if (mFile == null || downloading || uploading) {
            toHide.add(R.id.action_rename_file);
            
        } else {
            toShow.add(R.id.action_rename_file);
        }

        // MOVE
        if (mFile == null || downloading || uploading) {
            toHide.add(R.id.action_move);

        } else {
            toShow.add(R.id.action_move);
        }
        
        // REMOVE
        if (mFile == null || downloading || uploading) {
            toHide.add(R.id.action_remove_file);
            
        } else {
            toShow.add(R.id.action_remove_file);
        }
        
        // OPEN WITH (different to preview!)
        if (mFile == null || mFile.isFolder() || !mFile.isDown() || downloading || uploading) {
            toHide.add(R.id.action_open_file_with);
            
        } else {
            toShow.add(R.id.action_open_file_with);
        }
        
        
        // CANCEL DOWNLOAD
        if (mFile == null || !downloading) {
            toHide.add(R.id.action_cancel_download);
        } else {
            toShow.add(R.id.action_cancel_download);
        }
        
        // CANCEL UPLOAD
        if (mFile == null || !uploading || mFile.isFolder()) {
            toHide.add(R.id.action_cancel_upload);
        } else {
            toShow.add(R.id.action_cancel_upload);
        }
        
        // SYNC FILE CONTENTS
        if (mFile == null || mFile.isFolder() || !mFile.isDown() || downloading || uploading) {
            toHide.add(R.id.action_sync_file);
        } else {
            toShow.add(R.id.action_sync_file);
        }
        
        // SHARE FILE 
        // TODO add check on SHARE available on server side?
        if (mFile == null) {
            toHide.add(R.id.action_share_file);
        } else {
            toShow.add(R.id.action_share_file);
        }
        
        // UNSHARE FILE  
        // TODO add check on SHARE available on server side?
        if (mFile == null || !mFile.isShareByLink()) { 
            toHide.add(R.id.action_unshare_file);
        } else {
            toShow.add(R.id.action_unshare_file);
        }
        
        
        // SEE DETAILS
        if (mFile == null || mFile.isFolder()) {
            toHide.add(R.id.action_see_details);
        } else {
            toShow.add(R.id.action_see_details);
        }
        
        // SEND
        boolean sendAllowed = (mContext != null &&
                mContext.getString(R.string.send_files_to_other_apps).equalsIgnoreCase("on"));
        if (mFile == null || !sendAllowed || mFile.isFolder() || uploading || downloading) {
            toHide.add(R.id.action_send_file);
        } else {
            toShow.add(R.id.action_send_file);
        }

    }

}
