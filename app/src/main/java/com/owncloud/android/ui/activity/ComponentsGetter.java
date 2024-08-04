/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2024 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2016 ownCloud Inc.
 * SPDX-FileCopyrightText: 2015 María Asensio Valverde <masensio@solidgear.es>
 * SPDX-FileCopyrightText: 2014 David A. Velasco <dvelasco@solidgear.es>
 * SPDX-FileCopyrightText: 2012 Bartosz Przybylski <bart.p.pl@gmail.com>
 * SPDX-License-Identifier: GPL-2.0-only AND (AGPL-3.0-or-later OR GPL-2.0-only)
 */
package com.owncloud.android.ui.activity;

import com.nextcloud.client.jobs.download.FileDownloadWorker;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.services.OperationsService.OperationsServiceBinder;
import com.owncloud.android.ui.helpers.FileOperationsHelper;

public interface ComponentsGetter {

    /**
     * To be invoked when the parent activity is fully created to get a reference
     * to the FileDownloadWorker.
     */
    public FileDownloadWorker.FileDownloadProgressListener getFileDownloadProgressListener();

    /**
     * To be invoked when the parent activity is fully created to get a reference
     * to the FileUploader service API.
     */
    public FileUploadHelper getFileUploaderHelper();

    /**
     * To be invoked when the parent activity is fully created to get a reference
     * to the OperationsService service API.
     */
    public OperationsServiceBinder getOperationsServiceBinder();

    public FileDataStorageManager getStorageManager();

    public FileOperationsHelper getFileOperationsHelper();
}
