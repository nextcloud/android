/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.jobs;

import android.content.ContentResolver;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.FilesystemDataProvider;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.datamodel.SyncedFolderProvider;

import org.lukhnos.nnio.file.FileVisitResult;
import org.lukhnos.nnio.file.Files;
import org.lukhnos.nnio.file.Path;
import org.lukhnos.nnio.file.Paths;
import org.lukhnos.nnio.file.SimpleFileVisitor;
import org.lukhnos.nnio.file.attribute.BasicFileAttributes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NewAutoUploadJob extends Job {
    public static final String TAG = "NewAutoUploadJob";

    private static final String GLOBAL = "global";
    private static final String LAST_AUTOUPLOAD_JOB_RUN = "last_autoupload_job_run";


    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        final Context context = MainApp.getAppContext();
        final ContentResolver contentResolver = context.getContentResolver();

        // Create all the providers we'll need
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProvider(contentResolver);
        final FilesystemDataProvider filesystemDataProvider = new FilesystemDataProvider(contentResolver);
        SyncedFolderProvider syncedFolderProvider = new SyncedFolderProvider(contentResolver);

        // Store when we started doing this
        arbitraryDataProvider.storeOrUpdateKeyValue(GLOBAL, LAST_AUTOUPLOAD_JOB_RUN,
                Long.toString(System.currentTimeMillis()));

        List<SyncedFolder> syncedFolders = syncedFolderProvider.getSyncedFolders();
        List<SyncedFolder> syncedFoldersToDelete = new ArrayList<>();

        for (SyncedFolder syncedFolder : syncedFolders) {
            for (SyncedFolder secondarySyncedFolder : syncedFolders) {
                if (!syncedFolder.equals(secondarySyncedFolder) &&
                        secondarySyncedFolder.getLocalPath().startsWith(syncedFolder.getLocalPath()) &&
                        !syncedFoldersToDelete.contains(secondarySyncedFolder)) {
                    syncedFoldersToDelete.add(secondarySyncedFolder);
                }
            }
        }

        // delete all the folders from the list that we won't traverse
        syncedFolders.removeAll(syncedFoldersToDelete);

        for (int i = 0; i < syncedFolders.size(); i++) {
            Path path = Paths.get(syncedFolders.get(i).getLocalPath());

            try {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {

                        filesystemDataProvider.storeOrUpdateFileValue(path.toAbsolutePath().toString(),
                                attrs.lastModifiedTime().toMillis(), path.toFile().isDirectory(), false);

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path file, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }

                });
            } catch (IOException e) {
                Log.d(TAG, "Something went wrong while indexing files for auto upload");
            }
        }
        return Result.SUCCESS;
    }
}
