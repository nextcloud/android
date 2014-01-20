/* ownCloud Android Library is available under MIT license
 *   Copyright (C) 2014 ownCloud (http://www.owncloud.org/)
 *   
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *   
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *   
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, 
 *   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 *   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
 *   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS 
 *   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN 
 *   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN 
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *   THE SOFTWARE.
 *
 */

package com.owncloud.android.lib.operations.common;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

import com.owncloud.android.lib.network.webdav.WebdavEntry;
import com.owncloud.android.lib.utils.FileUtils;

/**
 *  Contains the data of a Remote File from a WebDavEntry
 * 
 *  @author masensio
 */

public class RemoteFile implements Parcelable, Serializable {

	/** Generated - should be refreshed every time the class changes!! */
	private static final long serialVersionUID = 532139091191390616L;
	
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
	
	public RemoteFile(WebdavEntry we) {
        this(we.decodedPath());
        this.setCreationTimestamp(we.createTimestamp());
        this.setLength(we.contentLength());
        this.setMimeType(we.contentType());
        this.setModifiedTimestamp(we.modifiedTimestamp());
        this.setEtag(we.etag());
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
