package com.owncloud.android.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.owncloud.android.files.services.FileUploadService.LocalBehaviour;

/**
 * Stores all information in order to start upload. PersistentUploadObject can
 * be stored persistently by {@link UploadDbHandler}.
 * 
 * @author LukeOwncloud
 * 
 */
public class PersistentUploadObject {
    /**
     * Local path to file which is to be uploaded.
     */
    String localPath;
    /**
     * Remote path where file is to be uploaded to.
     */
    String remotePath;

    /**
     * Mime type of upload file.
     */
    String mimeType;
    /**
     * Local action for upload.
     */
    LocalBehaviour localAction;
    /**
     * Overwrite destination file?
     */
    boolean forceOverwrite;
    /**
     * Create destination folder?
     */
    boolean isCreateRemoteFolder;
    /**
     * Upload only via wifi?
     */
    boolean isUseWifiOnly;
    /**
     * Name of Owncloud account to upload file to.
     */
    String accountName;

    /**
     * @return the localPath
     */
    public String getLocalPath() {
        return localPath;
    }

    /**
     * @param localPath the localPath to set
     */
    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    /**
     * @return the remotePath
     */
    public String getRemotePath() {
        return remotePath;
    }

    /**
     * @param remotePath the remotePath to set
     */
    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param mimeType the mimeType to set
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the localAction
     */
    public LocalBehaviour getLocalAction() {
        return localAction;
    }

    /**
     * @param localAction the localAction to set
     */
    public void setLocalAction(LocalBehaviour localAction) {
        this.localAction = localAction;
    }

    /**
     * @return the forceOverwrite
     */
    public boolean isForceOverwrite() {
        return forceOverwrite;
    }

    /**
     * @param forceOverwrite the forceOverwrite to set
     */
    public void setForceOverwrite(boolean forceOverwrite) {
        this.forceOverwrite = forceOverwrite;
    }

    /**
     * @return the isCreateRemoteFolder
     */
    public boolean isCreateRemoteFolder() {
        return isCreateRemoteFolder;
    }

    /**
     * @param isCreateRemoteFolder the isCreateRemoteFolder to set
     */
    public void setCreateRemoteFolder(boolean isCreateRemoteFolder) {
        this.isCreateRemoteFolder = isCreateRemoteFolder;
    }

    /**
     * @return the isUseWifiOnly
     */
    public boolean isUseWifiOnly() {
        return isUseWifiOnly;
    }

    /**
     * @param isUseWifiOnly the isUseWifiOnly to set
     */
    public void setUseWifiOnly(boolean isUseWifiOnly) {
        this.isUseWifiOnly = isUseWifiOnly;
    }

    /**
     * @return the accountName
     */
    public String getAccountName() {
        return accountName;
    }

    /**
     * @param accountName the accountName to set
     */
    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    @Override
    public String toString() {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(this);
            so.flush();
            return bo.toString();
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

    public PersistentUploadObject fromString(String serializedObject) {
        try {
            byte b[] = serializedObject.getBytes();
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            return (PersistentUploadObject) si.readObject();
        } catch (Exception e) {
            System.out.println(e);
        }
        return null;
    }

}
