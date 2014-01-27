/* ownCloud Android client application
 *   Copyright (C) 2012-2014 ownCloud Inc.
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

package com.owncloud.android.datamodel;

import com.owncloud.android.lib.operations.common.ShareRemoteFile;
import com.owncloud.android.lib.operations.common.ShareType;
import com.owncloud.android.utils.Log_OC;

import android.os.Parcel;
import android.os.Parcelable;

public class OCShare implements Parcelable{

    private static final String TAG = OCShare.class.getSimpleName();
    
    private long mId;
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
    
    
    /**
     * Create new {@link OCShare} with given path.
     * 
     * The path received must be URL-decoded. Path separator must be OCFile.PATH_SEPARATOR, and it must be the first character in 'path'.
     * 
     * @param path The remote path of the file.
     */
    public OCShare(String path) {
        resetData();
        if (path == null || path.length() <= 0 || !path.startsWith(OCFile.PATH_SEPARATOR)) {
            Log_OC.e(TAG, "Trying to create a OCShare with a non valid path");
            throw new IllegalArgumentException("Trying to create a OCShare with a non valid path: " + path);
        }
        mPath = path;
    }

    public OCShare(ShareRemoteFile remoteFile) {
        mId = -1;

        String path = remoteFile.getPath();
        if (path == null || path.length() <= 0 || !path.startsWith(OCFile.PATH_SEPARATOR)) {
            Log_OC.e(TAG, "Trying to create a OCShare with a non valid path");
            throw new IllegalArgumentException("Trying to create a OCShare with a non valid path: " + path);
        }
        mPath = path;
        
        mFileSource = remoteFile.getFileSource();
        mItemSource = remoteFile.getItemSource();
        mShareType = remoteFile.getShareType();
        mShareWith = remoteFile.getShareWith();
        mPermissions = remoteFile.getPermissions();
        mSharedDate = remoteFile.getSharedDate();
        mExpirationDate = remoteFile.getExpirationDate();
        mToken = remoteFile.getToken();
        mSharedWithDisplayName = remoteFile.getSharedWithDisplayName();
        mIsDirectory = remoteFile.isDirectory();
        mUserId = remoteFile.getUserId();
        mIdRemoteShared = remoteFile.getIdRemoteShared();
    }
    
    /**
     * Used internally. Reset all file properties
     */
    private void resetData() {
        mId = -1;
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

    public long getId() {
        return mId;
    }
    
    public void setId(long id){
        mId = id;
    }

    /** 
     * Parcelable Methods
     */
    public static final Parcelable.Creator<OCShare> CREATOR = new Parcelable.Creator<OCShare>() {
        @Override
        public OCShare createFromParcel(Parcel source) {
            return new OCShare(source);
        }

        @Override
        public OCShare[] newArray(int size) {
            return new OCShare[size];
        }
    };
    
    /**
     * Reconstruct from parcel
     * 
     * @param source The source parcel
     */
    private OCShare(Parcel source) {
        mId = source.readLong();
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
    public int describeContents() {
        return this.hashCode();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mId);
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
