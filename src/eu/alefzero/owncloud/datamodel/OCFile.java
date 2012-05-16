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

import android.os.Parcel;
import android.os.Parcelable;

public class OCFile implements Parcelable {

    public static final Parcelable.Creator<OCFile> CREATOR = new Parcelable.Creator<OCFile>() {
        @Override
        public OCFile createFromParcel(Parcel source) {
            return new OCFile(source);
        }

        @Override
        public OCFile[] newArray(int size) {
            return new OCFile[size];
        }
    };

    private long mId;
    private long mParentId;
    private long mLength;
    private long mCreationTimestamp;
    private long mModifiedTimestamp;
    private String mRemotePath;
    private String mLocalPath;
    private String mMimeType;
    private boolean mNeedsUpdating;

    /**
     * Create new {@link OCFile} with given path
     * 
     * @param path
     *            The remote path of the file
     */
    public OCFile(String path) {
        resetData();
        mNeedsUpdating = false;
        mRemotePath = path;
    }

    /**
     * Reconstruct from parcel
     * 
     * @param source
     *            The source parcel
     */
    private OCFile(Parcel source) {
        mId = source.readLong();
        mParentId = source.readLong();
        mLength = source.readLong();
        mCreationTimestamp = source.readLong();
        mModifiedTimestamp = source.readLong();
        mRemotePath = source.readString();
        mLocalPath = source.readString();
        mMimeType = source.readString();
        mNeedsUpdating = source.readInt() == 0;
    }

    /**
     * Gets the ID of the file
     * 
     * @return the file ID
     */
    public long getFileId() {
        return mId;
    }

    /**
     * Returns the path of the file
     * 
     * @return The path
     */
    public String getPath() {
        return mRemotePath;
    }

    /**
     * Can be used to check, whether or not this file exists in the database
     * already
     * 
     * @return true, if the file exists in the database
     */
    public boolean fileExists() {
        return mId != -1;
    }

    /**
     * Use this to find out if this file is a Directory
     * 
     * @return true if it is a directory
     */
    public boolean isDirectory() {
        return mMimeType != null && mMimeType.equals("DIR");
    }

    /**
     * Use this to check if this file is available locally
     * 
     * @return true if it is
     */
    public boolean isDownloaded() {
        return mLocalPath != null || mLocalPath.equals("");
    }

    /**
     * The path, where the file is stored locally
     * 
     * @return The local path to the file
     */
    public String getStoragePath() {
        return mLocalPath;
    }

    /**
     * Can be used to set the path where the file is stored
     * 
     * @param storage_path
     *            to set
     */
    public void setStoragePath(String storage_path) {
        mLocalPath = storage_path;
    }

    /**
     * Get a UNIX timestamp of the file creation time
     * 
     * @return A UNIX timestamp of the time that file was created
     */
    public long getCreationTimestamp() {
        return mCreationTimestamp;
    }

    /**
     * Set a UNIX timestamp of the time the file was created
     * 
     * @param creation_timestamp
     *            to set
     */
    public void setCreationTimestamp(long creation_timestamp) {
        mCreationTimestamp = creation_timestamp;
    }

    /**
     * Get a UNIX timestamp of the file modification time
     * 
     * @return A UNIX timestamp of the modification time
     */
    public long getModificationTimestamp() {
        return mModifiedTimestamp;
    }

    /**
     * Set a UNIX timestamp of the time the time the file was modified.
     * 
     * @param modification_timestamp
     *            to set
     */
    public void setModificationTimestamp(long modification_timestamp) {
        mModifiedTimestamp = modification_timestamp;
    }

    /**
     * Returns the filename and "/" for the root directory
     * 
     * @return The name of the file
     */
    public String getFileName() {
        if (mRemotePath != null) {
            File f = new File(mRemotePath);
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
        return mMimeType;
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
            file.mParentId = mId;
            mNeedsUpdating = true;
            return;
        }
        throw new IllegalStateException(
                "This is not a directory where you can add stuff to!");
    }

    /**
     * Used internally. Reset all file properties
     */
    private void resetData() {
        mId = -1;
        mRemotePath = null;
        mParentId = 0;
        mLocalPath = null;
        mMimeType = null;
        mLength = 0;
        mCreationTimestamp = 0;
        mModifiedTimestamp = 0;
    }

    /**
     * Sets the ID of the file
     * 
     * @param file_id
     *            to set
     */
    public void setFileId(long file_id) {
        mId = file_id;
    }

    /**
     * Sets the Mime-Type of the
     * 
     * @param mimetype
     *            to set
     */
    public void setMimetype(String mimetype) {
        mMimeType = mimetype;
    }

    /**
     * Sets the ID of the parent folder
     * 
     * @param parent_id
     *            to set
     */
    public void setParentId(long parent_id) {
        mParentId = parent_id;
    }

    /**
     * Sets the file size in bytes
     * 
     * @param file_len
     *            to set
     */
    public void setFileLength(long file_len) {
        mLength = file_len;
    }

    /**
     * Returns the size of the file in bytes
     * 
     * @return The filesize in bytes
     */
    public long getFileLength() {
        return mLength;
    }

    /**
     * Returns the ID of the parent Folder
     * 
     * @return The ID
     */
    public long getParentId() {
        return mParentId;
    }

    /**
     * Check, if this file needs updating
     * 
     * @return
     */
    public boolean needsUpdatingWhileSaving() {
        return mNeedsUpdating;
    }

    @Override
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
        dest.writeLong(mParentId);
        dest.writeLong(mLength);
        dest.writeLong(mCreationTimestamp);
        dest.writeLong(mModifiedTimestamp);
        dest.writeString(mRemotePath);
        dest.writeString(mLocalPath);
        dest.writeString(mMimeType);
        dest.writeInt(mNeedsUpdating ? 0 : 1); // No writeBoolean method exists
                                               // - yay :D
    }

}
