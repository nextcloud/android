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

package com.owncloud.android.oc_framework.operations;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.owncloud.android.oc_framework.network.webdav.WebdavEntry;
import com.owncloud.android.oc_framework.utils.FileUtils;

/**
 * Contains the data of a Share Remote File from the Share API
 * 
 * @author masensio
 *
 */
public class ShareRemoteFile extends RemoteFile {

	/**
	 * Generated - should be refreshed every time the class changes!!
	 */
	private static final long serialVersionUID = -5916376011588784325L;
	
    private static final String TAG = ShareRemoteFile.class.getSimpleName();
    
    private long mFileSource;
    private long mItemSource;
    private ShareType mShareType;
    private String mShareWith;
    private String mPath;
    private int mPermissions;
    private long mSharedDate;
    private long mExpirationDate;
    private String mToken;
    private String mSharedWithDisplayName;
    private boolean mIsDirectory;
    private long mUserId;
    private long mIdRemoteShared;
    
    
	public ShareRemoteFile(String path) {
		super(path);
		resetData();
        if (path == null || path.length() <= 0 || !path.startsWith(FileUtils.PATH_SEPARATOR)) {
            Log.e(TAG, "Trying to create a OCShare with a non valid path");
            throw new IllegalArgumentException("Trying to create a OCShare with a non valid path: " + path);
        }
        mPath = path;
	}

	public ShareRemoteFile(WebdavEntry we) {
		super(we);
		// TODO Auto-generated constructor stub
	}

	/**
     * Used internally. Reset all file properties
     */
    private void resetData() {
        mFileSource = 0;
        mItemSource = 0;
        mShareType = ShareType.NO_SHARED; 
        mShareWith = null;
        mPath = null;
        mPermissions = -1;
        mSharedDate = 0;
        mExpirationDate = 0;
        mToken = null;
        mSharedWithDisplayName = null;
        mIsDirectory = false;
        mUserId = -1;
        mIdRemoteShared = -1;        
    }	
    
    /// Getters and Setters
    public long getFileSource() {
        return mFileSource;
    }

    public void setFileSource(long fileSource) {
        this.mFileSource = fileSource;
    }

    public long getItemSource() {
        return mItemSource;
    }

    public void setItemSource(long itemSource) {
        this.mItemSource = itemSource;
    }

    public ShareType getShareType() {
        return mShareType;
    }

    public void setShareType(ShareType shareType) {
        this.mShareType = shareType;
    }

    public String getShareWith() {
        return mShareWith;
    }

    public void setShareWith(String shareWith) {
        this.mShareWith = shareWith;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        this.mPath = path;
    }

    public int getPermissions() {
        return mPermissions;
    }

    public void setPermissions(int permissions) {
        this.mPermissions = permissions;
    }

    public long getSharedDate() {
        return mSharedDate;
    }

    public void setSharedDate(long sharedDate) {
        this.mSharedDate = sharedDate;
    }

    public long getExpirationDate() {
        return mExpirationDate;
    }

    public void setExpirationDate(long expirationDate) {
        this.mExpirationDate = expirationDate;
    }

    public String getToken() {
        return mToken;
    }

    public void setToken(String token) {
        this.mToken = token;
    }

    public String getSharedWithDisplayName() {
        return mSharedWithDisplayName;
    }

    public void setSharedWithDisplayName(String sharedWithDisplayName) {
        this.mSharedWithDisplayName = sharedWithDisplayName;
    }

    public boolean isDirectory() {
        return mIsDirectory;
    }

    public void setIsDirectory(boolean isDirectory) {
        this.mIsDirectory = isDirectory;
    }

    public long getUserId() {
        return mUserId;
    }

    public void setUserId(long userId) {
        this.mUserId = userId;
    }

    public long getIdRemoteShared() {
        return mIdRemoteShared;
    }

    public void setIdRemoteShared(long idRemoteShared) {
        this.mIdRemoteShared = idRemoteShared;
    }

    /** 
     * Parcelable Methods
     */
    public static final Parcelable.Creator<ShareRemoteFile> CREATOR = new Parcelable.Creator<ShareRemoteFile>() {
        @Override
        public ShareRemoteFile createFromParcel(Parcel source) {
            return new ShareRemoteFile(source);
        }

        @Override
        public ShareRemoteFile[] newArray(int size) {
            return new ShareRemoteFile[size];
        }
    };
    
    /**
     * Reconstruct from parcel
     * 
     * @param source The source parcel
     */    
    protected ShareRemoteFile(Parcel source) {
    	super(source);
    }
    
    public void readFromParcel(Parcel source) {
    	super.readFromParcel(source);
    	
        mFileSource = source.readLong();
        mItemSource = source.readLong();
        try {
            mShareType = ShareType.valueOf(source.readString());
        } catch (IllegalArgumentException x) {
            mShareType = ShareType.NO_SHARED;
        }
        mShareWith = source.readString();
        mPath = source.readString();
        mPermissions = source.readInt();
        mSharedDate = source.readLong();
        mExpirationDate = source.readLong();
        mToken = source.readString();
        mSharedWithDisplayName = source.readString();
        mIsDirectory = source.readInt() == 0;
        mUserId = source.readLong();
        mIdRemoteShared = source.readLong();
    }


    @Override
    public void writeToParcel(Parcel dest, int flags) {
    	super.writeToParcel(dest, flags);
    	
        dest.writeLong(mFileSource);
        dest.writeLong(mItemSource);
        dest.writeString((mShareType == null) ? "" : mShareType.name());
        dest.writeString(mShareWith);
        dest.writeString(mPath);
        dest.writeInt(mPermissions);
        dest.writeLong(mSharedDate);
        dest.writeLong(mExpirationDate);
        dest.writeString(mToken);
        dest.writeString(mSharedWithDisplayName);
        dest.writeInt(mIsDirectory ? 1 : 0);
        dest.writeLong(mUserId);
        dest.writeLong(mIdRemoteShared);
    }
}
