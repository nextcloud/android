package com.owncloud.android.ui.activity;

import com.owncloud.android.files.services.FileDownloader.FileDownloaderBinder;

public interface TransferServiceGetter {

    /**
     * Callback method invoked when the parent activity is fully created to get a reference to the FileDownloader service API.
     * 
     * @return  Directory to list firstly. Can be NULL.
     */
    public FileDownloaderBinder getFileDownloaderBinder();


}
