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

public class SyncedFolder {
    private long id;
    private String localPath;
    private String remotePath;
    private Boolean wifiOnly;
    private Boolean chargingOnly;
    private Boolean subfolderByDate;
    private String account;
    private Integer uploadAction;
    private boolean enabled;

    public SyncedFolder(long id, String localPath, String remotePath, Boolean wifiOnly, Boolean chargingOnly,
                        Boolean subfolderByDate, String account, Integer uploadAction, Boolean enabled) {
        this.id = id;
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.wifiOnly = wifiOnly;
        this.chargingOnly = chargingOnly;
        this.subfolderByDate = subfolderByDate;
        this.account = account;
        this.uploadAction = uploadAction;
        this.enabled = enabled;
    }

    public long getId() {
        return id;
    }

    public String getLocalPath() {
        return localPath;
    }

    public String getRemotePath() {
        return remotePath;
    }

    public Boolean getWifiOnly() {
        return wifiOnly;
    }

    public Boolean getChargingOnly() {
        return chargingOnly;
    }

    public Boolean getSubfolderByDate() {
        return subfolderByDate;
    }

    public String getAccount() {
        return account;
    }

    public Integer getUploadAction() {
        return uploadAction;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}