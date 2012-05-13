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

public class OCFile {

	private long id;
	private long parentId;
	private long length;
	private long creationTimestamp;
	private long modifiedTimestamp;
	private String remotePath;
	private String localPath;
	private String mimeType;
	private boolean needsUpdating;

	/**
	 * Create new {@link OCFile} with given path
	 * 
	 * @param path The remote path of the file
	 */
	public OCFile(String path) {
	  resetData();
	  needsUpdating = false;
		remotePath = path;
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
		return localPath != null || localPath.equals("");
	}

	/**
	 * The path, where the file is stored locally
	 * 
	 * @return The local path to the file
	 */
	public String getStoragePath() {
		return localPath;
	}

	/**
	 * Can be used to set the path where the file is stored
	 * 
	 * @param storage_path
	 *            to set
	 */
	public void setStoragePath(String storage_path) {
		localPath = storage_path;
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
	 * Adds a file to this directory. If this file is not a directory, an
	 * exception gets thrown.
	 * 
	 * @param file to add
	 * @throws IllegalStateException if you try to add a something and this is not a directory
	 */
	public void addFile(OCFile file) throws IllegalStateException {
		if (isDirectory()) {
			file.parentId = id;
			needsUpdating = true;
			return;
		}
		throw new IllegalStateException("This is not a directory where you can add stuff to!");
	}

	/**
	 * Used internally. Reset all file properties
	 */
	private void resetData() {
		id = -1;
		remotePath = null;
		parentId = 0;
		localPath = null;
		mimeType = null;
		length = 0;
		creationTimestamp = 0;
		modifiedTimestamp = 0;
	}

	/**
	 * Sets the ID of the file
	 * @param file_id to set
	 */
	public void setFileId(long file_id) {
	  id = file_id;
	}
	
	/**
	 * Sets the Mime-Type of the 
	 * @param mimetype to set
	 */
	public void setMimetype(String mimetype) {
	  mimeType = mimetype;
	}
	
	/**
	 * Sets the ID of the parent folder
	 * @param parent_id to set
	 */
	public void setParentId(long parent_id) {
	  parentId = parent_id;
	}
	
	/**
	 * Sets the file size in bytes
	 * @param file_len to set
	 */
	public void setFileLength(long file_len) {
	  length = file_len;
	}
	
	/**
	 * Returns the size of the file in bytes
	 * @return The filesize in bytes
	 */
  public long getFileLength() {
    return length;
  }
  
  /**
   * Returns the ID of the parent Folder
   * @return The ID
   */
  public long getParentId() {
    return parentId;
  }
  
  /**
   * Check, if this file needs updating
   * @return
   */
  public boolean needsUpdatingWhileSaving() {
    return needsUpdating;
  }
}
