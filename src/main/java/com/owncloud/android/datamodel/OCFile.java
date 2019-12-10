/*
 *   ownCloud Android client application
 *
 *   @author Bartek Przybylski
 *   @author David A. Velasco
 *   Copyright (C) 2012  Bartek Przybylski
 *   Copyright (C) 2016 ownCloud Inc.
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


import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.network.WebdavEntry;
import com.owncloud.android.lib.common.network.WebdavUtils;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.ServerFileInterface;
import com.owncloud.android.lib.resources.shares.ShareeUser;
import com.owncloud.android.utils.MimeType;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import lombok.Getter;
import lombok.Setter;
import third_parties.daveKoeller.AlphanumComparator;

public class OCFile implements Parcelable, Comparable<OCFile>, ServerFileInterface {
    private final static String PERMISSION_SHARED_WITH_ME = "S";
    private final static String PERMISSION_CAN_RESHARE = "R";
    private final static String PERMISSION_CAN_WRITE = "CK";

    public static final String PATH_SEPARATOR = "/";
    public static final String ROOT_PATH = PATH_SEPARATOR;

    private static final String TAG = OCFile.class.getSimpleName();

    @Getter @Setter private long fileId; // android internal ID of the file
    @Getter @Setter private long parentId;
    @Getter @Setter private long fileLength;
    @Getter @Setter private long creationTimestamp; // UNIX timestamp of the time the file was created
    @Getter @Setter private long modificationTimestamp; // UNIX timestamp of the file modification time
    /** UNIX timestamp of the modification time, corresponding to the value returned by the server
     * in the last synchronization of THE CONTENTS of this file.
     */
    @Getter @Setter private long modificationTimestampAtLastSyncForData;
    @Setter private String remotePath;
    private String localPath;
    @Getter @Setter private String mimeType;
    @Getter private boolean needsUpdatingWhileSaving;
    @Getter @Setter private long lastSyncDateForProperties;
    @Getter @Setter private long lastSyncDateForData;
    @Getter @Setter private boolean previewAvailable;
    @Getter private String etag;
    @Getter private String etagOnServer;
    @Getter @Setter private boolean sharedViaLink;
    @Getter @Setter private String publicLink;
    @Getter @Setter private String permissions;
    @Getter @Setter private String remoteId; // The fileid namespaced by the instance fileId, globally unique
    @Getter @Setter private boolean updateThumbnailNeeded;
    @Getter @Setter private boolean downloading;
    @Getter @Setter private String etagInConflict; // Only saves file etag in the server, when there is a conflict
    @Getter @Setter private boolean sharedWithSharee;
    @Getter @Setter private boolean favorite;
    @Getter @Setter private boolean encrypted;
    @Getter @Setter private WebdavEntry.MountType mountType;
    @Getter @Setter private int unreadCommentsCount;
    @Getter @Setter private String ownerId;
    @Getter @Setter private String ownerDisplayName;
    @Getter @Setter String note;
    @Getter @Setter private List<ShareeUser> sharees;

    /**
     * URI to the local path of the file contents, if stored in the device; cached after first call
     * to {@link #getStorageUri()}
     */
    private Uri localUri;


    /**
     * Exportable URI to the local path of the file contents, if stored in the device.
     * <p>
     * Cached after first call, until changed.
     */
    private Uri exposedFileUri;
    @Getter @Setter private String encryptedFileName;


    /**
     * Create new {@link OCFile} with given path.
     * <p>
     * The path received must be URL-decoded. Path separator must be OCFile.PATH_SEPARATOR, and it must be the first character in 'path'.
     *
     * @param path The remote path of the file.
     */
    public OCFile(String path) {
        resetData();
        needsUpdatingWhileSaving = false;
        if (TextUtils.isEmpty(path) || !path.startsWith(PATH_SEPARATOR)) {
            throw new IllegalArgumentException("Trying to create a OCFile with a non valid remote path: " + path);
        }
        remotePath = path;
    }

    /**
     * Reconstruct from parcel
     *
     * @param source The source parcel
     */
    private OCFile(Parcel source) {
        fileId = source.readLong();
        parentId = source.readLong();
        fileLength = source.readLong();
        creationTimestamp = source.readLong();
        modificationTimestamp = source.readLong();
        modificationTimestampAtLastSyncForData = source.readLong();
        remotePath = source.readString();
        localPath = source.readString();
        mimeType = source.readString();
        needsUpdatingWhileSaving = source.readInt() == 0;
        lastSyncDateForProperties = source.readLong();
        lastSyncDateForData = source.readLong();
        etag = source.readString();
        etagOnServer = source.readString();
        sharedViaLink = source.readInt() == 1;
        publicLink = source.readString();
        permissions = source.readString();
        remoteId = source.readString();
        updateThumbnailNeeded = source.readInt() == 1;
        downloading = source.readInt() == 1;
        etagInConflict = source.readString();
        sharedWithSharee = source.readInt() == 1;
        favorite = source.readInt() == 1;
        encrypted = source.readInt() == 1;
        encryptedFileName = source.readString();
        ownerId = source.readString();
        ownerDisplayName = source.readString();
        mountType = (WebdavEntry.MountType) source.readSerializable();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(fileId);
        dest.writeLong(parentId);
        dest.writeLong(fileLength);
        dest.writeLong(creationTimestamp);
        dest.writeLong(modificationTimestamp);
        dest.writeLong(modificationTimestampAtLastSyncForData);
        dest.writeString(remotePath);
        dest.writeString(localPath);
        dest.writeString(mimeType);
        dest.writeInt(needsUpdatingWhileSaving ? 1 : 0);
        dest.writeLong(lastSyncDateForProperties);
        dest.writeLong(lastSyncDateForData);
        dest.writeString(etag);
        dest.writeString(etagOnServer);
        dest.writeInt(sharedViaLink ? 1 : 0);
        dest.writeString(publicLink);
        dest.writeString(permissions);
        dest.writeString(remoteId);
        dest.writeInt(updateThumbnailNeeded ? 1 : 0);
        dest.writeInt(downloading ? 1 : 0);
        dest.writeString(etagInConflict);
        dest.writeInt(sharedWithSharee ? 1 : 0);
        dest.writeInt(favorite ? 1 : 0);
        dest.writeInt(encrypted ? 1 : 0);
        dest.writeString(encryptedFileName);
        dest.writeString(ownerId);
        dest.writeString(ownerDisplayName);
        dest.writeSerializable(mountType);
    }

    public String getDecryptedRemotePath() {
        return remotePath;
    }

    /**
     * Returns the remote path of the file on ownCloud
     *
     * @return The remote path to the file
     */
    public String getRemotePath() {
        if (isEncrypted() && !isFolder()) {
            String parentPath = new File(remotePath).getParent();

            if (parentPath.endsWith(PATH_SEPARATOR)) {
                return parentPath + getEncryptedFileName();
            } else {
                return parentPath + PATH_SEPARATOR + getEncryptedFileName();
            }
        } else {
            if (isFolder()) {
                if (remotePath.endsWith(PATH_SEPARATOR)) {
                    return remotePath;
                } else {
                    return remotePath + PATH_SEPARATOR;
                }
            } else {
                return remotePath;
            }
        }
    }

    /**
     * Can be used to check, whether or not this file exists in the database
     * already
     *
     * @return true, if the file exists in the database
     */
    public boolean fileExists() {
        return fileId != -1;
    }

    /**
     * Use this to find out if this file is a folder.
     *
     * @return true if it is a folder
     */
    public boolean isFolder() {
        return MimeType.DIRECTORY.equals(mimeType);
    }


    /**
     * Sets mimetype to folder and returns this file
     * Only for testing
     *
     * @return OCFile this file
     */
    public OCFile setFolder() {
        setMimeType(MimeType.DIRECTORY);
        return this;
    }

    /**
     * Use this to check if this file is available locally
     *
     * @return true if it is
     */
    public boolean isDown() {
        return !isFolder() && existsOnDevice();
    }

    /**
     * Use this to check if this file or folder is available locally
     *
     * @return true if it is
     */
    public boolean existsOnDevice() {
        if (!TextUtils.isEmpty(localPath)) {
            return new File(localPath).exists();
        }
        return false;
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
     * The URI to the file contents, if stored locally
     *
     * @return A URI to the local copy of the file, or NULL if not stored in the device
     */
    public Uri getStorageUri() {
        if (TextUtils.isEmpty(localPath)) {
            return null;
        }
        if (localUri == null) {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(ContentResolver.SCHEME_FILE);
            builder.path(localPath);
            localUri = builder.build();
        }
        return localUri;
    }


    public Uri getLegacyExposedFileUri() {
        if (TextUtils.isEmpty(localPath)) {
            return null;
        }

        if (exposedFileUri == null) {
            return Uri.parse(ContentResolver.SCHEME_FILE + "://" + WebdavUtils.encodePath(localPath));
        }

        return exposedFileUri;

    }
    /*
        Partly disabled because not all apps understand paths that we get via this method for now
     */
    public Uri getExposedFileUri(Context context) {
        if (TextUtils.isEmpty(localPath)) {
            return null;
        }
        if (exposedFileUri == null) {
            try {
                exposedFileUri = FileProvider.getUriForFile(
                        context,
                        context.getString(R.string.file_provider_authority),
                        new File(localPath));
            } catch (IllegalArgumentException ex) {
                // Could not share file using FileProvider URI scheme.
                // Fall back to legacy URI parsing.
                getLegacyExposedFileUri();
            }
        }

        return exposedFileUri;
    }

    /**
     * Can be used to set the path where the file is stored
     *
     * @param storage_path to set
     */
    public void setStoragePath(String storage_path) {
        localPath = storage_path;
        localUri = null;
        exposedFileUri = null;
    }

    /**
     * Returns the filename and "/" for the root directory
     *
     * @return The name of the file
     */
    public String getFileName() {
        File f = new File(remotePath);
        return f.getName().length() == 0 ? ROOT_PATH : f.getName();
    }

    /**
     * Sets the name of the file
     * <p/>
     * Does nothing if the new name is null, empty or includes "/" ; or if the file is the root
     * directory
     */
    public void setFileName(String name) {
        Log_OC.d(TAG, "OCFile name changing from " + remotePath);
        if (!TextUtils.isEmpty(name) && !name.contains(PATH_SEPARATOR) && !ROOT_PATH.equals(remotePath)) {
            String parent = new File(this.getRemotePath()).getParent();
            parent = parent.endsWith(PATH_SEPARATOR) ? parent : parent + PATH_SEPARATOR;
            remotePath = parent + name;
            if (isFolder()) {
                remotePath += PATH_SEPARATOR;
            }
            Log_OC.d(TAG, "OCFile name changed to " + remotePath);
        }
    }

    /**
     * Used internally. Reset all file properties
     */
    private void resetData() {
        fileId = -1;
        remotePath = null;
        parentId = 0;
        localPath = null;
        mimeType = null;
        fileLength = 0;
        creationTimestamp = 0;
        modificationTimestamp = 0;
        modificationTimestampAtLastSyncForData = 0;
        lastSyncDateForProperties = 0;
        lastSyncDateForData = 0;
        needsUpdatingWhileSaving = false;
        etag = null;
        etagOnServer = null;
        sharedViaLink = false;
        publicLink = null;
        permissions = null;
        remoteId = null;
        updateThumbnailNeeded = false;
        downloading = false;
        etagInConflict = null;
        sharedWithSharee = false;
        favorite = false;
        encrypted = false;
        encryptedFileName = null;
        mountType = WebdavEntry.MountType.INTERNAL;
    }

    /**
     * get remote path of parent file
     *
     * @return remote path
     */
    public String getParentRemotePath() {
        String parentPath = new File(this.getRemotePath()).getParent();
        return parentPath.endsWith(PATH_SEPARATOR) ? parentPath : parentPath + PATH_SEPARATOR;
    }

    @Override
    public int describeContents() {
        return super.hashCode();
    }

    @Override
    public int compareTo(@NonNull OCFile another) {
        if (isFolder() && another.isFolder()) {
            return new AlphanumComparator().compare(this, another);
        } else if (isFolder()) {
            return -1;
        } else if (another.isFolder()) {
            return 1;
        }
        return new AlphanumComparator().compare(this, another);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OCFile ocFile = (OCFile) o;

        return fileId == ocFile.fileId && parentId == ocFile.parentId;
    }

    @Override
    public int hashCode() {
        return 31 * (int) (fileId ^ (fileId >>> 32)) + (int) (parentId ^ (parentId >>> 32));
    }

    @NonNull
    @Override
    public String toString() {
        String asString = "[id=%s, name=%s, mime=%s, downloaded=%s, local=%s, remote=%s, " +
                "parentId=%s, etag=%s, favourite=%s]";
        return String.format(asString, fileId, getFileName(), mimeType, isDown(), localPath, remotePath, parentId,
            etag, favorite);
    }

    public void setEtag(String etag) {
        this.etag = etag != null ? etag : "";
    }

    public void setEtagOnServer(String etag) {
        this.etagOnServer = etag != null ? etag : "";
    }

    public long getLocalModificationTimestamp() {
        if (!TextUtils.isEmpty(localPath)) {
            File f = new File(localPath);
            return f.lastModified();
        }
        return 0;
    }

    /**
     * @return 'True' if the file is hidden
     */
    public boolean isHidden() {
        return !TextUtils.isEmpty(getFileName()) && getFileName().charAt(0) == '.';
    }

    /**
     * The unique fileId for the file within the instance
     *
     * @return file fileId, unique within the instance
     */
    public String getLocalId() {
        return getRemoteId().substring(0, 8).replaceAll("^0*", "");
    }

    public boolean isInConflict() {
        return !TextUtils.isEmpty(etagInConflict);
    }

    public boolean isSharedWithMe() {
        String permissions = getPermissions();
        return permissions != null && permissions.contains(PERMISSION_SHARED_WITH_ME);
    }

    public boolean canReshare() {
        String permissions = getPermissions();
        return permissions != null && permissions.contains(PERMISSION_CAN_RESHARE);
    }

    public boolean canWrite() {
        String permissions = getPermissions();
        return permissions != null && permissions.contains(PERMISSION_CAN_WRITE);
    }

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
}
