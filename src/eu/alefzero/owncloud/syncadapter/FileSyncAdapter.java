/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
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

package eu.alefzero.owncloud.syncadapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.webdav.WebdavEntry;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 * 
 * @author Bartek Przybylski
 */
public class FileSyncAdapter extends AbstractOwnCloudSyncAdapter {

    private final static String TAG = "FileSyncAdapter"; 
    
    private long mCurrentSyncTime;
    
    public FileSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public synchronized void onPerformSync(Account account, Bundle extras,
            String authority, ContentProviderClient provider,
            SyncResult syncResult) {

        this.setAccount(account);
        this.setContentProvider(provider);
        this.setStorageManager(new FileDataStorageManager(account,
                getContentProvider()));
        
        Log.d(TAG, "syncing owncloud account " + account.name);

        sendStickyBroadcast(true, -1);  // message to signal the start to the UI

        PropFindMethod query;
        try {
            mCurrentSyncTime = System.currentTimeMillis();
            query = new PropFindMethod(getUri().toString() + "/");
            getClient().executeMethod(query);
            MultiStatus resp = null;
            resp = query.getResponseBodyAsMultiStatus();

            if (resp.getResponses().length > 0) {
                WebdavEntry we = new WebdavEntry(resp.getResponses()[0], getUri().getPath());
                OCFile file = fillOCFile(we);
                file.setParentId(0);
                getStorageManager().saveFile(file);
                fetchData(getUri().toString(), syncResult, file.getFileId());
            }
        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            syncResult.stats.numAuthExceptions++;
            e.printStackTrace();
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
        } catch (DavException e) {
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
        }
        sendStickyBroadcast(false, -1);        
    }

    private void fetchData(String uri, SyncResult syncResult, long parentId) {
        try {
            PropFindMethod query = new PropFindMethod(uri);
            getClient().executeMethod(query);
            MultiStatus resp = null;
            resp = query.getResponseBodyAsMultiStatus();
            Queue<String> paths = new LinkedList<String>();
            Queue<Long> fileIds = new LinkedList<Long>(); 
            for (int i = 1; i < resp.getResponses().length; ++i) {
                WebdavEntry we = new WebdavEntry(resp.getResponses()[i], getUri().getPath());
                OCFile file = fillOCFile(we);
                file.setParentId(parentId);
                getStorageManager().saveFile(file);
                if (parentId == 0)
                    parentId = file.getFileId();
                if (we.contentType().equals("DIR")) {
                    // for recursive fetch later
                    paths.add(we.path());
                    fileIds.add(file.getFileId());
                }
            }
            
            Vector<OCFile> files = getStorageManager().getDirectoryContent(
                    getStorageManager().getFileById(parentId));
            for (OCFile file : files) {
                if (file.getLastSyncDate() != mCurrentSyncTime && file.getLastSyncDate() != 0)
                    getStorageManager().removeFile(file);
            }
            
            // synched folder -> notice to IU
            sendStickyBroadcast(true, parentId);

            // recursive fetch
            while(!paths.isEmpty()) {
                fetchData(getUri().toString() + paths.remove(), syncResult, fileIds.remove());
            }
            paths = null;
            fileIds = null;


        } catch (OperationCanceledException e) {
            e.printStackTrace();
        } catch (AuthenticatorException e) {
            syncResult.stats.numAuthExceptions++;
            e.printStackTrace();
        } catch (IOException e) {
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
        } catch (DavException e) {
            syncResult.stats.numIoExceptions++;
            e.printStackTrace();
        }
    }

    private OCFile fillOCFile(WebdavEntry we) {
        OCFile file = new OCFile(we.path());
        file.setCreationTimestamp(we.createTimestamp());
        file.setFileLength(we.contentLength());
        file.setMimetype(we.contentType());
        file.setModificationTimestamp(we.modifiedTimesamp());
        file.setLastSyncDate(mCurrentSyncTime);
        return file;
    }
    
    
    private void sendStickyBroadcast(boolean inProgress, long OCDirId) {
        Intent i = new Intent(FileSyncService.SYNC_MESSAGE);
        i.putExtra(FileSyncService.IN_PROGRESS, inProgress);
        i.putExtra(FileSyncService.ACCOUNT_NAME, getAccount().name);
        if (OCDirId > 0) {
            i.putExtra(FileSyncService.SYNC_FOLDER, OCDirId);
        }
        getContext().sendStickyBroadcast(i);
    }

}
