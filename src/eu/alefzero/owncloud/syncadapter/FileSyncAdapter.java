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
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.webdav.HttpPropFind;
import eu.alefzero.webdav.TreeNode;
import eu.alefzero.webdav.TreeNode.NodeProperty;
import eu.alefzero.webdav.WebdavUtils;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
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

		try {
			this.setAccount(account);
			this.setContentProvider(provider);

			HttpPropFind query = this.getBasicQuery();
			query.setEntity(new StringEntity(WebdavUtils.prepareXmlForPropFind()));
			TreeNode root = this.fireQuery(query);

			commitToDatabase(root, null);
		} catch (OperationCanceledException e) {
			e.printStackTrace();
		} catch (AuthenticatorException e) {
			syncResult.stats.numAuthExceptions++;
			e.printStackTrace();
		} catch (IOException e) {
			syncResult.stats.numIoExceptions++;
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	private void commitToDatabase(TreeNode root, String parentId) throws RemoteException {
		for (TreeNode n : root.getChildList()) {
			Log.d(TAG, n.toString());
			ContentValues cv = new ContentValues();
			cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, n.getProperty(NodeProperty.CONTENT_LENGTH));
			cv.put(ProviderTableMeta.FILE_MODIFIED, n.getProperty(NodeProperty.LAST_MODIFIED_DATE));
			cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, n.getProperty(NodeProperty.RESOURCE_TYPE));
			cv.put(ProviderTableMeta.FILE_PARENT, parentId);

			String name = n.getProperty(NodeProperty.NAME),
					path = n.getProperty(NodeProperty.PATH);
			Cursor c = this.getContentProvider().query(ProviderTableMeta.CONTENT_URI_FILE,
					null,
					ProviderTableMeta.FILE_NAME+"=? AND " + ProviderTableMeta.FILE_PATH + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
					new String[]{name, path, this.getAccount().name},
					null);
			if (c.moveToFirst()) {
				this.getContentProvider().update(ProviderTableMeta.CONTENT_URI,
						cv,
						ProviderTableMeta._ID+"=?",
								new String[]{c.getString(c.getColumnIndex(ProviderTableMeta._ID))});
				Log.d(TAG, "ID of: "+name+":"+c.getString(c.getColumnIndex(ProviderTableMeta._ID)));
			} else {
				cv.put(ProviderTableMeta.FILE_NAME, n.getProperty(NodeProperty.NAME));
				cv.put(ProviderTableMeta.FILE_PATH, n.getProperty(NodeProperty.PATH));
				cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, this.getAccount().name);
				Uri entry = this.getContentProvider().insert(ProviderTableMeta.CONTENT_URI_FILE, cv);
				Log.d(TAG, "Inserting new entry " + path + name);
				c = this.getContentProvider().query(entry, null, null, null, null);
				c.moveToFirst();
			}
			if (n.getProperty(NodeProperty.RESOURCE_TYPE).equals("DIR")) {
				commitToDatabase(n, c.getString(c.getColumnIndex(ProviderTableMeta._ID)));
			}
		}
		// clean removed files
		String[] selection = new String[root.getChildList().size()+2];
		selection[0] = this.getAccount().name;
		selection[1] = parentId;
		String qm = "";
		for (int i = 2; i < selection.length-1; ++i) {
			qm += "?,";
			selection[i] = root.getChildList().get(i-2).getProperty(NodeProperty.NAME);
		}
		if (selection.length >= 3) {
			selection[selection.length-1] = root.getChildrenNames()[selection.length-3];
			qm += "?";
		}
		for (int i = 0; i < selection.length; ++i) {
			Log.d(TAG,selection[i]+"");
		}
		Log.d(TAG,"Removing files "+ parentId);
		this.getContentProvider().delete(ProviderTableMeta.CONTENT_URI,
				ProviderTableMeta.FILE_ACCOUNT_OWNER+"=? AND " + ProviderTableMeta.FILE_PARENT + (parentId==null?" IS ":"=")+"? AND " + ProviderTableMeta.FILE_NAME + " NOT IN ("+qm+")",
				selection);
	}
}
