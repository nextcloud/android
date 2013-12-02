/* ownCloud Android client application
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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

package com.owncloud.android.oc_framework.operations;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.oc_framework.utils.FileUtils;

/**
 *  Contains the data of a Remote File from a WebDavEntry
 * 
 *  @author masensio
 */

public class RemoteFile implements Parcelable, Serializable {

	/** Generated - should be refreshed every time the class changes!! */
	private static final long serialVersionUID = 7256606476031992757L;
	
	private String mRemotePath;
	private String mMimeType;
	private long mLength;
	private long mCreationTimestamp;
	private long mModifiedTimestamp;
	private String mEtag;
	
	/** 
	 * Getters and Setters
	 */
	
    public String getRemotePath() {
		return mRemotePath;
	}

	public void setRemotePath(String remotePath) {
		this.mRemotePath = remotePath;
	}

	public String getMimeType() {
		return mMimeType;
	}

	public void setMimeType(String mimeType) {
		this.mMimeType = mimeType;
	}

	public long getLength() {
		return mLength;
	}

	public void setLength(long length) {
		this.mLength = length;
	}

	public long getCreationTimestamp() {
		return mCreationTimestamp;
	}

	public void setCreationTimestamp(long creationTimestamp) {
		this.mCreationTimestamp = creationTimestamp;
	}

	public long getModifiedTimestamp() {
		return mModifiedTimestamp;
	}

	public void setModifiedTimestamp(long modifiedTimestamp) {
		this.mModifiedTimestamp = modifiedTimestamp;
	}

	public String getEtag() {
		return mEtag;
	}

	public void setEtag(String etag) {
		this.mEtag = etag;
	}

	/**
     * Create new {@link RemoteFile} with given path.
     * 
     * The path received must be URL-decoded. Path separator must be OCFile.PATH_SEPARATOR, and it must be the first character in 'path'.
     * 
     * @param path The remote path of the file.
     */
	public RemoteFile(String path) {
		resetData();
        if (path == null || path.length() <= 0 || !path.startsWith(FileUtils.PATH_SEPARATOR)) {
            throw new IllegalArgumentException("Trying to create a OCFile with a non valid remote path: " + path);
        }
        mRemotePath = path;
	}

	/**
     * Used internally. Reset all file properties
     */
    private void resetData() {
        mRemotePath = null;
        mMimeType = null;
        mLength = 0;
        mCreationTimestamp = 0;
        mModifiedTimestamp = 0;
        mEtag = null;
    }

    /** 
     * Parcelable Methods
     */
    public static final Parcelable.Creator<RemoteFile> CREATOR = new Parcelable.Creator<RemoteFile>() {
        @Override
        public RemoteFile createFromParcel(Parcel source) {
            return new RemoteFile(source);
        }

        @Override
        public RemoteFile[] newArray(int size) {
            return new RemoteFile[size];
        }
    };
    
    
    /**
     * Reconstruct from parcel
     * 
     * @param source The source parcel
     */
    private RemoteFile(Parcel source) {
        mRemotePath = source.readString();
        mMimeType = source.readString();
        mLength = source.readLong();
        mCreationTimestamp = source.readLong();
        mModifiedTimestamp = source.readLong();
        mEtag = source.readString();
    }
    
	@Override
	public int describeContents() {
		return this.hashCode();
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mRemotePath);
		dest.writeString(mMimeType);    
		dest.writeLong(mLength);
		dest.writeLong(mCreationTimestamp);
		dest.writeLong(mModifiedTimestamp);
		dest.writeString(mEtag);		
	}
    
    
}
