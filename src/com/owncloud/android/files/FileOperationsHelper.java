/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

import org.apache.http.protocol.HTTP;

import android.accounts.AccountManager;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.DialogFragment;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.accounts.OwnCloudAccount;
import com.owncloud.android.lib.network.webdav.WebdavUtils;
import com.owncloud.android.lib.operations.common.ShareType;
import com.owncloud.android.operations.CreateShareOperation;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.dialog.ActivityChooserDialog;
import com.owncloud.android.utils.Log_OC;

/**
 * 
 * @author masensio
 * @author David A. Velasco
 */
public class FileOperationsHelper {

    private static final String TAG = FileOperationsHelper.class.getName();
    
    private static final String FTAG_CHOOSER_DIALOG = "CHOOSER_DIALOG"; 

    
    public void openFile(OCFile file, FileActivity callerActivity) {
        if (file != null) {
            String storagePath = file.getStoragePath();
            String encodedStoragePath = WebdavUtils.encodePath(storagePath);
            
            Intent intentForSavedMimeType = new Intent(Intent.ACTION_VIEW);
            intentForSavedMimeType.setDataAndType(Uri.parse("file://"+ encodedStoragePath), file.getMimetype());
            intentForSavedMimeType.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            
            Intent intentForGuessedMimeType = null;
            if (storagePath.lastIndexOf('.') >= 0) {
                String guessedMimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(storagePath.substring(storagePath.lastIndexOf('.') + 1));
                if (guessedMimeType != null && !guessedMimeType.equals(file.getMimetype())) {
                    intentForGuessedMimeType = new Intent(Intent.ACTION_VIEW);
                    intentForGuessedMimeType.setDataAndType(Uri.parse("file://"+ encodedStoragePath), guessedMimeType);
                    intentForGuessedMimeType.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }
            }
            
            Intent chooserIntent = null;
            if (intentForGuessedMimeType != null) {
                chooserIntent = Intent.createChooser(intentForGuessedMimeType, callerActivity.getString(R.string.actionbar_open_with));
            } else {
                chooserIntent = Intent.createChooser(intentForSavedMimeType, callerActivity.getString(R.string.actionbar_open_with));
            }
            
            callerActivity.startActivity(chooserIntent);
            
        } else {
            Log_OC.wtf(TAG, "Trying to open a NULL OCFile");
        }
    }
    

    public void shareFileWithLink(OCFile file, FileActivity callerActivity) {
        
        if (isSharedSupported(callerActivity)) {
            if (file != null) {
                String link = "https://fake.url";
                Intent intent = createShareWithLinkIntent(link);
                String[] packagesToExclude = new String[] { callerActivity.getPackageName() };
                DialogFragment chooserDialog = ActivityChooserDialog.newInstance(intent, packagesToExclude, file);
                chooserDialog.show(callerActivity.getSupportFragmentManager(), FTAG_CHOOSER_DIALOG);
                
            } else {
                Log_OC.wtf(TAG, "Trying to share a NULL OCFile");
            }
            
        } else {
            // Show a Message
            Toast t = Toast.makeText(callerActivity, callerActivity.getString(R.string.share_link_no_support_share_api), Toast.LENGTH_LONG);
            t.show();
        }
    }
    
    
    public void shareFileWithLinkToApp(OCFile file, Intent sendIntent, FileActivity callerActivity) {
        
        if (file != null) {
            callerActivity.showLoadingDialog();
            CreateShareOperation createShare = new CreateShareOperation(file.getRemotePath(), ShareType.PUBLIC_LINK, "", false, "", 1, sendIntent);
            createShare.execute(callerActivity.getStorageManager(), 
                                callerActivity, 
                                callerActivity.getRemoteOperationListener(), 
                                callerActivity.getHandler(), 
                                callerActivity);
            
        } else {
            Log_OC.wtf(TAG, "Trying to open a NULL OCFile");
        }
    }
    
    
    private Intent createShareWithLinkIntent(String link) {
        Intent intentToShareLink = new Intent(Intent.ACTION_SEND);
        intentToShareLink.putExtra(Intent.EXTRA_TEXT, link);
        intentToShareLink.setType(HTTP.PLAIN_TEXT_TYPE);
        return intentToShareLink; 
    }
    
    
    /**
     *  @return 'True' if the server supports the Share API
     */
    public boolean isSharedSupported(FileActivity callerActivity) {
        if (callerActivity.getAccount() != null) {
            AccountManager accountManager = AccountManager.get(callerActivity);
            return Boolean.parseBoolean(accountManager.getUserData(callerActivity.getAccount(), OwnCloudAccount.Constants.KEY_SUPPORTS_SHARE_API));
        }
        return false;
    }

}
