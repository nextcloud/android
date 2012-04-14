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

	private long id_;
	private long parent_id_;
	private long length_;
	private long creation_timestamp_;
	private long modified_timestamp_;
	private String path_;
	private String storage_path_;
	private String mimetype_;
	private boolean update_while_saving_;

	/**
	 * Create new {@link OCFile} with given path
	 * 
	 * @param path The remote path of the file
	 */
	public OCFile(String path) {
	  update_while_saving_ = false;
		path_ = path;
		resetData();
	}

	/**
	 * Gets the ID of the file
	 * 
	 * @return the file ID
	 */
	public long getFileId() {
		return id_;
	}

	/**
	 * Returns the path of the file
	 * 
	 * @return The path
	 */
	public String getPath() {
		return path_;
	}

	/**
	 * Can be used to check, whether or not this file exists in the database
	 * already
	 * 
	 * @return true, if the file exists in the database
	 */
	public boolean fileExists() {
		return id_ != -1;
	}

	/**
	 * Use this to find out if this file is a Directory
	 * 
	 * @return true if it is a directory
	 */
	public boolean isDirectory() {
		return mimetype_ != null && mimetype_.equals("DIR");
	}

	/**
	 * Use this to check if this file is available locally
	 * 
	 * @return true if it is
	 */
	public boolean isDownloaded() {
		return storage_path_ != null;
	}

	/**
	 * The path, where the file is stored locally
	 * 
	 * @return The local path to the file
	 */
	public String getStoragePath() {
		return storage_path_;
	}

	/**
	 * Can be used to set the path where the file is stored
	 * 
	 * @param storage_path
	 *            to set
	 */
	public void setStoragePath(String storage_path) {
		storage_path_ = storage_path;
	}

	/**
	 * Get a UNIX timestamp of the file creation time
	 * 
	 * @return A UNIX timestamp of the time that file was created
	 */
	public long getCreationTimestamp() {
		return creation_timestamp_;
	}

	/**
	 * Set a UNIX timestamp of the time the file was created
	 * 
	 * @param creation_timestamp
	 *            to set
	 */
	public void setCreationTimestamp(long creation_timestamp) {
		creation_timestamp_ = creation_timestamp;
	}

	/**
	 * Get a UNIX timestamp of the file modification time
	 * 
	 * @return A UNIX timestamp of the modification time
	 */
	public long getModificationTimestamp() {
		return modified_timestamp_;
	}

	/**
	 * Set a UNIX timestamp of the time the time the file was modified.
	 * 
	 * @param modification_timestamp
	 *            to set
	 */
	public void setModificationTimestamp(long modification_timestamp) {
		modified_timestamp_ = modification_timestamp;
	}

	/**
	 * Returns the filename and "/" for the root directory
	 * 
	 * @return The name of the file
	 */
	public String getFileName() {
		if (path_ != null) {
			File f = new File(path_);
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
		return mimetype_;
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
			file.parent_id_ = id_;
			update_while_saving_ = true;
			return;
		}
		throw new IllegalStateException("This is not a directory where you can add stuff to!");
	}

	/**
	 * Used internally. Reset all file properties
	 */
	private void resetData() {
		id_ = -1;
		path_ = null;
		parent_id_ = 0;
		storage_path_ = null;
		mimetype_ = null;
		length_ = 0;
		creation_timestamp_ = 0;
		modified_timestamp_ = 0;
	}

	public void setFileId(long file_id) {
	  id_ = file_id;
	}
	
	public void setMimetype(String mimetype) {
	  mimetype_ = mimetype;
	}
	
	public void setParentId(long parent_id) {
	  parent_id_ = parent_id;
	}
	
	public void setFileLength(long file_len) {
	  length_ = file_len;
	}
	
  public long getFileLength() {
    return length_;
  }
  
  public long getParentId() {
    return parent_id_;
  }
  
  public boolean needsUpdatingWhileSaving() {
    return update_while_saving_;
  }
}
