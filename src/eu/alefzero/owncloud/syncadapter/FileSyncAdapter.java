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

import org.apache.http.entity.StringEntity;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyName;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.webdav.HttpPropFind;
import eu.alefzero.webdav.TreeNode;
import eu.alefzero.webdav.TreeNode.NodeProperty;
import eu.alefzero.webdav.WebdavEntry;
import eu.alefzero.webdav.WebdavUtils;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 * 
 * @author Bartek Przybylski
 */
public class FileSyncAdapter extends AbstractOwnCloudSyncAdapter {
	private static final String TAG = "FileSyncAdapter";

	public FileSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
	}
	
	@Override
	public synchronized void onPerformSync(
			Account account, 
			Bundle extras, 
			String authority, 
			ContentProviderClient provider, 
			SyncResult syncResult) {

			this.setAccount(account);
			this.setContentProvider(provider);
			this.setStorageManager(new FileDataStorageManager(account, getContentProvider()));

			fetchData(getUri().toString(), syncResult, 0);
	}

  private void fetchData(String uri, SyncResult syncResult, long parentId) {
    try {
      PropFindMethod query = new PropFindMethod(uri);
      getClient().executeMethod(query);
      MultiStatus resp = null;
      resp = query.getResponseBodyAsMultiStatus();
      for (int i = (parentId==0?0:1); i < resp.getResponses().length; ++i) {
        WebdavEntry we = new WebdavEntry(resp.getResponses()[i]);
        OCFile file = new OCFile(we.path());
        file.setCreationTimestamp(we.createTimestamp());
        file.setFileLength(we.contentLength());
        file.setMimetype(we.contentType());
        file.setModificationTimestamp(we.modifiedTimesamp());
        file.setParentId(parentId);
        if (we.contentType().equals("DIR"))
          fetchData(getUri().toString() + we.path(), syncResult, file.getFileId());
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
  }
	
}
