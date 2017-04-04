/**
 *   Nextcloud Android client application
 *
 *   @author Tobias Kaminsky
 *   Copyright (C) 2016 Tobias Kaminsky
 *   Copyright (C) 2016 Nextcloud
 *
 *   This program is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 *   License as published by the Free Software Foundation; either
 *   version 3 of the License, or any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 *   You should have received a copy of the GNU Affero General Public
 *   License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import java.io.Serializable;

/**
 * Synced folder entity containing all information per synced folder.
 */
public class SyncedFolder implements Serializable {
    public static final long UNPERSISTED_ID = Long.MIN_VALUE;
    private static final long serialVersionUID = -793476118299906429L;
    private long id = UNPERSISTED_ID;
    private String localPath;
    private String remotePath;
    private Boolean wifiOnly;
    private Boolean chargingOnly;
    private Boolean subfolderByDate;
    private String account;
    private Integer uploadAction;
    private boolean enabled;
    private Integer type;

    /**
     * constructor for already persisted entity.
     *
     * @param id              the primary key
     * @param localPath       local path
     * @param remotePath      remote path
     * @param wifiOnly        upload on wifi only flag
     * @param chargingOnly    upload on charging only
     * @param subfolderByDate create sub-folders by date (month)
     * @param account         the account owning the synced folder
     * @param uploadAction    the action to be done after the upload
     * @param enabled         flag if synced folder config is active
     * @param type            the type of the folder
     */
    public SyncedFolder(long id, String localPath, String remotePath, Boolean wifiOnly, Boolean chargingOnly,
                        Boolean subfolderByDate, String account, Integer uploadAction, Boolean enabled, Integer type) {
        this.id = id;
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.wifiOnly = wifiOnly;
        this.chargingOnly = chargingOnly;
        this.subfolderByDate = subfolderByDate;
        this.account = account;
        this.uploadAction = uploadAction;
        this.enabled = enabled;
        this.type = type;
    }

    /**
     * constructor for new, to be persisted entity.
     *
     * @param localPath       local path
     * @param remotePath      remote path
     * @param wifiOnly        upload on wifi only flag
     * @param chargingOnly    upload on charging only
     * @param subfolderByDate create sub-folders by date (month)
     * @param account         the account owning the synced folder
     * @param uploadAction    the action to be done after the upload
     * @param enabled         flag if synced folder config is active
     * @param type            the type of the folder
     */
    public SyncedFolder(String localPath, String remotePath, Boolean wifiOnly, Boolean chargingOnly,
                        Boolean subfolderByDate, String account, Integer uploadAction, Boolean enabled, Integer type) {
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.wifiOnly = wifiOnly;
        this.chargingOnly = chargingOnly;
        this.subfolderByDate = subfolderByDate;
        this.account = account;
        this.uploadAction = uploadAction;
        this.enabled = enabled;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public Boolean getWifiOnly() {
        return wifiOnly;
    }

    public void setWifiOnly(Boolean wifiOnly) {
        this.wifiOnly = wifiOnly;
    }

    public Boolean getChargingOnly() {
        return chargingOnly;
    }

    public void setChargingOnly(Boolean chargingOnly) {
        this.chargingOnly = chargingOnly;
    }

    public Boolean getSubfolderByDate() {
        return subfolderByDate;
    }

    public void setSubfolderByDate(Boolean subfolderByDate) {
        this.subfolderByDate = subfolderByDate;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public Integer getUploadAction() {
        return uploadAction;
    }

    public void setUploadAction(Integer uploadAction) {
        this.uploadAction = uploadAction;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}