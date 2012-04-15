/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
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

package eu.alefzero.owncloud.datamodel;

import java.io.File;
import java.util.Vector;

import android.accounts.Account;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;

public class OCFile {
	private static String TAG = "OCFile";

	private long id;
	private long parentId;
	private long length;
	private long creationTimestamp;
	private long modifiedTimestamp;
	private String remotePath;
	private String localStoragePath;
	private String mimeType;

	private ContentResolver contentResolver;
	private ContentProviderClient providerClient;
	private Account account;
	
	private OCFile(ContentProviderClient providerClient, Account account) {
		this.account = account;
		this.providerClient = providerClient;
		resetData();
	}

	private OCFile(ContentResolver contentResolver, Account account) {
		this.account = account;
		this.contentResolver = contentResolver;
		resetData();
	}
	
	/**
	 * Query the database for a {@link OCFile} belonging to a given account 
	 * and id.
	 * 
	 * @param resolver The {@link ContentResolver} to use
	 * @param account The {@link Account} the {@link OCFile} belongs to
	 * @param id The ID the file has in the database
	 */
	public OCFile(ContentResolver resolver, Account account, long id) {
		this.contentResolver = resolver;
		this.account = account;
		Cursor c = this.contentResolver.query(ProviderTableMeta.CONTENT_URI_FILE,
				null, ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND "
						+ ProviderTableMeta._ID + "=?", new String[] {
						this.account.name, String.valueOf(id) }, null);
		if (c.moveToFirst())
			setFileData(c);
	}

	/**
	 * Query the database for a {@link OCFile} belonging to a given account
	 * and that matches remote path
	 * 
	 * @param contentResolver The {@link ContentResolver} to use
	 * @param account The {@link Account} the {@link OCFile} belongs to
	 * @param path The remote path of the file
	 */
	public OCFile(ContentResolver contentResolver, Account account, String path) {
		this.contentResolver = contentResolver;
		this.account = account;

		Cursor c = this.contentResolver.query(ProviderTableMeta.CONTENT_URI_FILE,
				null, ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND "
						+ ProviderTableMeta.FILE_PATH + "=?", new String[] {
						this.account.name, path }, null);
		if (c.moveToFirst()) {
			setFileData(c);
			if (remotePath != null)
				remotePath = path;
		}
	}
	
	public OCFile(ContentProviderClient cp, Account account, String path) {
		this.providerClient = cp;
		this.account = account;

		try {
			Cursor c = this.providerClient.query(ProviderTableMeta.CONTENT_URI_FILE, null,
					ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND "
							+ ProviderTableMeta.FILE_PATH + "=?", new String[] {
							this.account.name, path }, null);
			if (c.moveToFirst()) {
				setFileData(c);
				if (remotePath != null)
					remotePath = path;
			}
		} catch (RemoteException e) {
			Log.d(TAG, e.getMessage());
		}
	}

	/**
	 * Creates a new {@link OCFile}
	 *  
	 * @param providerClient The {@link ContentProviderClient} to use
	 * @param account The {@link Account} that this file belongs to
	 * @param path The remote path
	 * @param length The file size in bytes 
	 * @param creation_timestamp The UNIX timestamp of the creation date
	 * @param modified_timestamp The UNIX timestamp of the modification date
	 * @param mimetype The mimetype to set
	 * @param parent_id The parent folder of that file
	 * @return A new instance of {@link OCFile}
	 */
	public static OCFile createNewFile(ContentProviderClient providerClient,
			Account account, String path, long length, long creation_timestamp,
			long modified_timestamp, String mimetype, long parent_id) {
		OCFile new_file = new OCFile(providerClient, account);

		try {
			Cursor c = new_file.providerClient.query(ProviderTableMeta.CONTENT_URI_FILE,
					null, ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND "
							+ ProviderTableMeta.FILE_PATH + "=?", new String[] {
							new_file.account.name, path }, null);
			if (c.moveToFirst())
				new_file.setFileData(c);
			c.close();
		} catch (RemoteException e) {
			Log.e(TAG, e.getMessage());
		}

		new_file.remotePath = path;
		new_file.length = length;
		new_file.creationTimestamp = creation_timestamp;
		new_file.modifiedTimestamp = modified_timestamp;
		new_file.mimeType = mimetype;
		new_file.parentId = parent_id;

		return new_file;
	}

	/**
	 * Creates a new {@link OCFile}
	 * 
	 * @param contentResolver The {@link ContentResolver} to use
	 * @param account The {@link Account} that this file belongs to
	 * @param path The remote path
	 * @param length The file size in bytes
	 * @param creation_timestamp The UNIX timestamp of the creation date
	 * @param modified_timestamp The UNIX timestamp of the modification date 
	 * @param mimetype The mimetype to set
	 * @param parent_id The parent folder of that file
	 * @return A new instance of {@link OCFile}
	 */
	public static OCFile createNewFile(ContentResolver contentResolver,
			Account account, String path, int length, int creation_timestamp,
			int modified_timestamp, String mimetype, long parent_id) {
		OCFile new_file = new OCFile(contentResolver, account);
		Cursor c = new_file.contentResolver.query(
				ProviderTableMeta.CONTENT_URI_FILE, null,
				ProviderTableMeta.FILE_ACCOUNT_OWNER + "=? AND "
						+ ProviderTableMeta.FILE_PATH + "=?", new String[] {
						new_file.account.name, path }, null);
		if (c.moveToFirst())
			new_file.setFileData(c);
		c.close();

		new_file.remotePath = path;
		new_file.length = length;
		new_file.creationTimestamp = creation_timestamp;
		new_file.modifiedTimestamp = modified_timestamp;
		new_file.mimeType = mimetype;
		new_file.parentId = parent_id;

		return new_file;
	}


	/**
	 * Gets the ID of the file
	 * 
	 * @return the file ID
	 */
	public long getFileId() {
		return id;
	}

	/**
	 * Returns the path of the file
	 * 
	 * @return The path
	 */
	public String getPath() {
		return remotePath;
	}

	/**
	 * Can be used to check, whether or not this file exists in the database
	 * already
	 * 
	 * @return true, if the file exists in the database
	 */
	public boolean fileExists() {
		return id != -1;
	}

	/**
	 * Use this to find out if this file is a Directory
	 * 
	 * @return true if it is a directory
	 */
	public boolean isDirectory() {
		return mimeType != null && mimeType.equals("DIR");
	}

	/**
	 * Use this to check if this file is available locally
	 * 
	 * @return true if it is
	 */
	public boolean isDownloaded() {
		return localStoragePath != null;
	}

	/**
	 * The path, where the file is stored locally
	 * 
	 * @return The local path to the file
	 */
	public String getStoragePath() {
		return localStoragePath;
	}

	/**
	 * Can be used to set the path where the file is stored
	 * 
	 * @param storage_path
	 *            to set
	 */
	public void setStoragePath(String storage_path) {
		localStoragePath = storage_path;
	}

	/**
	 * Get a UNIX timestamp of the file creation time
	 * 
	 * @return A UNIX timestamp of the time that file was created
	 */
	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	/**
	 * Set a UNIX timestamp of the time the file was created
	 * 
	 * @param creation_timestamp
	 *            to set
	 */
	public void setCreationTimestamp(long creation_timestamp) {
		creationTimestamp = creation_timestamp;
	}

	/**
	 * Get a UNIX timestamp of the file modification time
	 * 
	 * @return A UNIX timestamp of the modification time
	 */
	public long getModificationTimestamp() {
		return modifiedTimestamp;
	}

	/**
	 * Set a UNIX timestamp of the time the time the file was modified.
	 * 
	 * @param modification_timestamp
	 *            to set
	 */
	public void setModificationTimestamp(long modification_timestamp) {
		modifiedTimestamp = modification_timestamp;
	}

	/**
	 * Returns the filename and "/" for the root directory
	 * 
	 * @return The name of the file
	 */
	public String getFileName() {
		if (remotePath != null) {
			File f = new File(remotePath);
			return f.getName().equals("") ? "/" : f.getName();
		}
		return null;
	}

	/**
	 * Can be used to get the Mimetype
	 * 
	 * @return the Mimetype as a String
	 */
	public String getMimetype() {
		return mimeType;
	}

	/**
	 * Instruct the file to save itself to the database
	 */
	public void save() {
		ContentValues cv = new ContentValues();
		cv.put(ProviderTableMeta.FILE_MODIFIED, modifiedTimestamp);
		cv.put(ProviderTableMeta.FILE_CREATION, creationTimestamp);
		cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, length);
		cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, mimeType);
		cv.put(ProviderTableMeta.FILE_NAME, getFileName());
		if (parentId != 0)
			cv.put(ProviderTableMeta.FILE_PARENT, parentId);
		cv.put(ProviderTableMeta.FILE_PATH, remotePath);
		cv.put(ProviderTableMeta.FILE_STORAGE_PATH, localStoragePath);
		cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, account.name);

		if (fileExists()) {
			if (providerClient != null) {
				try {
					providerClient.update(ProviderTableMeta.CONTENT_URI, cv,
							ProviderTableMeta._ID + "=?",
							new String[] { String.valueOf(id) });
				} catch (RemoteException e) {
					Log.e(TAG, e.getMessage());
					return;
				}
			} else {
				contentResolver.update(ProviderTableMeta.CONTENT_URI, cv,
						ProviderTableMeta._ID + "=?",
						new String[] { String.valueOf(id) });
			}
		} else {
			Uri new_entry = null;
			if (providerClient != null) {
				try {
					new_entry = providerClient.insert(ProviderTableMeta.CONTENT_URI_FILE,
							cv);
				} catch (RemoteException e) {
					Log.e(TAG, e.getMessage());
					id = -1;
					return;
				}
			} else {
				new_entry = contentResolver.insert(
						ProviderTableMeta.CONTENT_URI_FILE, cv);
			}
			try {
				String p = new_entry.getEncodedPath();
				id = Integer.parseInt(p.substring(p.lastIndexOf('/') + 1));
			} catch (NumberFormatException e) {
				Log.e(TAG,
						"Can't retrieve file id from uri: "
								+ new_entry.toString() + ", reason: "
								+ e.getMessage());
				id = -1;
			}
		}
	}

	/**
	 * List the directory content
	 * 
	 * @return The directory content or null, if the file is not a directory
	 */
	public Vector<OCFile> getDirectoryContent() {
		if (isDirectory() && id != -1) {
			Vector<OCFile> ret = new Vector<OCFile>();

			Uri req_uri = Uri.withAppendedPath(
					ProviderTableMeta.CONTENT_URI_DIR, String.valueOf(id));
			Cursor c = null;
			if (providerClient != null) {
				try {
					c = providerClient.query(req_uri, null, null, null, null);
				} catch (RemoteException e) {
					Log.e(TAG, e.getMessage());
					return ret;
				}
			} else {
				c = contentResolver.query(req_uri, null, null, null, null);
			}

			if (c.moveToFirst())
				do {
					OCFile child = new OCFile(providerClient, account);
					child.setFileData(c);
					ret.add(child);
				} while (c.moveToNext());

			c.close();
			return ret;
		}
		return null;
	}

	/**
	 * Adds a file to this directory. If this file is not a directory, an
	 * exception gets thrown.
	 * 
	 * @param file
	 *            to add
	 * @throws IllegalStateException
	 *             if you try to add a something and this is not a directory
	 */
	public void addFile(OCFile file) throws IllegalStateException {
		if (isDirectory()) {
			file.parentId = id;
			file.save();
			return;
		}
		throw new IllegalStateException(
				"This is not a directory where you can add stuff to!");
	}

	/**
	 * Used internally. Reset all file properties
	 */
	private void resetData() {
		id = -1;
		remotePath = null;
		parentId = 0;
		localStoragePath = null;
		mimeType = null;
		length = 0;
		creationTimestamp = 0;
		modifiedTimestamp = 0;
	}

	/**
	 * Used internally. Set properties based on the information in a {@link android.database.Cursor}
	 * @param c the Cursor containing the information
	 */
	private void setFileData(Cursor c) {
		resetData();
		if (c != null) {
			id = c.getLong(c.getColumnIndex(ProviderTableMeta._ID));
			remotePath = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_PATH));
			parentId = c.getLong(c
					.getColumnIndex(ProviderTableMeta.FILE_PARENT));
			localStoragePath = c.getString(c
					.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));
			mimeType = c.getString(c
					.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE));
			length = c.getLong(c
					.getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH));
			creationTimestamp = c.getLong(c
					.getColumnIndex(ProviderTableMeta.FILE_CREATION));
			modifiedTimestamp = c.getLong(c
					.getColumnIndex(ProviderTableMeta.FILE_MODIFIED));
		}
	}
}
