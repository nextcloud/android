package com.owncloud.android.services.observer;

import java.io.File;

import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.files.InstantUploadBroadcastReceiver;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.operations.UploadFileOperation;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.RecursiveFileObserver;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.FileObserver;
import android.webkit.MimeTypeMap;

public class InstantUploadFolderObserver extends RecursiveFileObserver {

    File dirToWatch;
    String remoteFolder;
    
    public InstantUploadFolderObserver(String path, String remoteFolder) {
        super(path, FileObserver.CREATE + FileObserver.MOVED_TO);
        
        dirToWatch = new File(path);
        this.remoteFolder = remoteFolder;
        Log_OC.d("InstantUploadFolderObserver", "Started watching: "+ path);
    }
    
    

    @Override
    public void onEvent(int event, String path) {
        File file = new File(path);
        Context context = MainApp.getAppContext();
        Account account = AccountUtils.getCurrentOwnCloudAccount(context);
        
        
        Uri selectedUri = Uri.fromFile(file);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

        // int behaviour = getUploadBehaviour(context);
        FileUploader.UploadRequester requester = new FileUploader.UploadRequester();
        requester.uploadNewFile(
                context,
                account,
                file.getAbsolutePath(),
                remoteFolder,
                FileUploader.LOCAL_BEHAVIOUR_COPY,
                null,
                true,
                UploadFileOperation.CREATED_AS_INSTANT_PICTURE
        );

//        i.putExtra(FileUploader.KEY_ACCOUNT, account);
//        i.putExtra(FileUploader.KEY_LOCAL_FILE, path);
//        i.putExtra(FileUploader.KEY_REMOTE_FILE, OCFile.PATH_SEPARATOR + remoteFolder + OCFile.PATH_SEPARATOR +  getRelativePath(file, dirToWatch));
//        i.putExtra(FileUploader.KEY_UPLOAD_TYPE, FileUploader.UPLOAD_SINGLE_FILE);
//        i.putExtra(FileUploader.KEY_MIME_TYPE, mimeType);
//        i.putExtra(FileUploader.KEY_INSTANT_UPLOAD, true);
//        MainApp.getAppContext().startService(i);
    }

    private String getRelativePath(File file, File folder) {
        String filePath = file.getAbsolutePath();
        String folderPath = folder.getAbsolutePath();
        if (filePath.startsWith(folderPath)) {
            return filePath.substring(folderPath.length() + 1);
        } else {
            return null;
        }
    }

}
