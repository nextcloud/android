package com.owncloud.android.db;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.accounts.Account;
import android.content.Context;
import android.util.Base64;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.db.UploadDbHandler.UploadStatus;
import com.owncloud.android.files.services.FileUploadService.LocalBehaviour;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Stores all information in order to start upload operations. PersistentUploadObject can
 * be stored persistently by {@link UploadDbHandler}.
 * 
 * @author LukeOwncloud
 * 
 */
public class UploadDbObject implements Serializable {

    /** Generated - should be refreshed every time the class changes!! */
    ;
    private static final long serialVersionUID = -2306246191385279928L;

    private static final String TAG = "UploadDbObject";
    
    public UploadDbObject(OCFile ocFile) {
        this.ocFile = ocFile;
    }
//    /**
//     * Local path to file which is to be uploaded.
//     */
//    String localPath;
//    /**
//     * Remote path where file is to be uploaded to.
//     */
//    String remotePath;
//
//    /**
//     * Mime type of upload file.
//     */
//    String mimeType;
    OCFile ocFile;
    
    public OCFile getOCFile() {
        return ocFile;
    }
    
    
    /**
     * Local action for upload.
     */
    LocalBehaviour localAction;

    /**
     * Date and time when this upload was first requested.
     */
    Calendar uploadTime = new GregorianCalendar();

    public Calendar getUploadTime() {
        return uploadTime;
    }

    /**
     * @return the uploadStatus
     */
    public UploadStatus getUploadStatus() {
        return uploadStatus;
    }

    /**
     * Sets uploadStatus AND SETS lastResult = null;
     * @param uploadStatus the uploadStatus to set
     */
    public void setUploadStatus(UploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
        setLastResult(null);
    }

    /**
     * @return the lastResult
     */
    public RemoteOperationResult getLastResult() {
        return lastResult;
    }

    /**
     * @param lastResult the lastResult to set
     */
    public void setLastResult(RemoteOperationResult lastResult) {
        this.lastResult = lastResult;
    }

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
     * Upload only if phone being charged?
     */
    boolean isWhileChargingOnly;
    /**
     * Earliest time when upload may be started. Negative if not set.
     */
    long uploadTimestamp;

    /**
     * Name of Owncloud account to upload file to.
     */
    String accountName;

    /**
     * Status of upload (later, in_progress, ...).
     */
    UploadStatus uploadStatus;

    /**
     * Result from last upload operation. Can be null.
     */
    RemoteOperationResult lastResult;

    /**
     * @return the localPath
     */
    public String getLocalPath() {
        return ocFile.getStoragePath();
    }

    /**
     * @return the remotePath
     */
    public String getRemotePath() {
        return ocFile.getRemotePath();
    }

    /**
     * @return the mimeType
     */
    public String getMimeType() {
        return ocFile.getMimetype();
    }


    /**
     * @return the localAction
     */
    public LocalBehaviour getLocalAction() {
        // return null;
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

    /**
     * Returns a base64 encoded serialized string of this object.
     */
    @Override
    public String toString() {
        // serialize the object
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ObjectOutputStream so = new ObjectOutputStream(bo);
            so.writeObject(this);
            so.flush();
            String serializedObjectBase64 = Base64.encodeToString(bo.toByteArray(), Base64.DEFAULT);
            so.close();
            bo.close();
            return serializedObjectBase64;
        } catch (Exception e) {
            Log_OC.e(TAG, "Cannot serialize UploadDbObject with localPath:" + getLocalPath(), e);
        }
        return null;
    }

    /**
     * Accepts a base64 encoded serialized string of an {@link UploadDbObject}
     * and instantiates and returns an according object.
     * 
     * @param serializedObjectBase64
     * @return
     */
    static public UploadDbObject fromString(String serializedObjectBase64) {
        // deserialize the object
        try {
            byte[] b = Base64.decode(serializedObjectBase64, Base64.DEFAULT);
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            UploadDbObject obj = (UploadDbObject) si.readObject();
            return obj;
        } catch (Exception e) {
            Log_OC.e(TAG, "Cannot deserialize UploadDbObject " + serializedObjectBase64, e);
        }
        return null;
    }

    /**
     * Returns owncloud account as {@link Account} object.  
     */
    public Account getAccount(Context context) {
        return AccountUtils.getOwnCloudAccountByName(context, getAccountName());
    }

    public void setWhileChargingOnly(boolean isWhileChargingOnly) {
        this.isWhileChargingOnly = isWhileChargingOnly;        
    }
    
    public boolean isWhileChargingOnly() {
        return isWhileChargingOnly;
    }

    /**
     * Earliest time when upload may be started. Negative if not set.
     * @return the uploadTimestamp
     */
    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    /**
     * Earliest time when upload may be started. Set to negative value for immediate upload.
     * @param uploadTimestamp the uploadTimestamp to set
     */
    public void setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }
    
    /**
     * For debugging purposes only.
     */
    public String toFormattedString() {
        return getLocalPath() + " status:"+getUploadStatus() + " result:" + (getLastResult()==null?"null":getLastResult().getCode());
    }

    /**
     * Removes all uploads restrictions. After calling this function upload is performed immediately if requested.
     */
    public void removeAllUploadRestrictions() {
        setUseWifiOnly(false);
        setWhileChargingOnly(false);
        setUploadTimestamp(0);
    }
}
