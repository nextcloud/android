/*
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

import lombok.Getter;
import lombok.Setter;

/**
 * Synced folder entity containing all information per synced folder.
 */
public class SyncedFolder implements Serializable, Cloneable {
    public static final long UNPERSISTED_ID = Long.MIN_VALUE;
    public static final long EMPTY_ENABLED_TIMESTAMP_MS = -1;
    private static final long serialVersionUID = -793476118299906429L;

    @Getter @Setter private long id;
    @Getter @Setter private String localPath;
    @Getter @Setter private String remotePath;
    @Getter @Setter private boolean wifiOnly;
    @Getter @Setter private boolean chargingOnly;
    @Getter @Setter private boolean existing;
    @Getter @Setter private boolean subfolderByDate;
    @Getter @Setter private String account;
    @Getter @Setter private int uploadAction;
    @Getter private boolean enabled;
    @Getter private long enabledTimestampMs;
    @Getter @Setter private MediaFolderType type;
    @Getter @Setter private boolean hidden;

    /**
     * constructor for new, to be persisted entity.
     *
     * @param localPath       local path
     * @param remotePath      remote path
     * @param wifiOnly        upload on wifi only flag
     * @param chargingOnly    upload on charging only
     * @param existing        upload existing files
     * @param subfolderByDate create sub-folders by date (month)
     * @param account         the account owning the synced folder
     * @param uploadAction    the action to be done after the upload
     * @param enabled         flag if synced folder config is active
     * @param timestampMs     the current timestamp in milliseconds
     * @param type            the type of the folder
     * @param hidden          hide item flag
     */
    public SyncedFolder(String localPath,
                        String remotePath,
                        boolean wifiOnly,
                        boolean chargingOnly,
                        boolean existing,
                        boolean subfolderByDate,
                        String account,
                        int uploadAction,
                        boolean enabled,
                        long timestampMs,
                        MediaFolderType type,
                        boolean hidden) {
        this(UNPERSISTED_ID, localPath, remotePath, wifiOnly, chargingOnly, existing, subfolderByDate, account,
             uploadAction, enabled, timestampMs, type, hidden);
    }

    /**
     * constructor for wrapping existing folders.
     *
     * @param id id
     */
    protected SyncedFolder(long id,
                           String localPath,
                           String remotePath,
                           boolean wifiOnly,
                           boolean chargingOnly,
                           boolean existing,
                           boolean subfolderByDate,
                           String account,
                           int uploadAction,
                           boolean enabled,
                           long timestampMs,
                           MediaFolderType type,
                           boolean hidden) {
        this.id = id;
        this.localPath = localPath;
        this.remotePath = remotePath;
        this.wifiOnly = wifiOnly;
        this.chargingOnly = chargingOnly;
        this.existing = existing;
        this.subfolderByDate = subfolderByDate;
        this.account = account;
        this.uploadAction = uploadAction;
        this.setEnabled(enabled, timestampMs);
        this.type = type;
        this.hidden = hidden;
    }

    /**
     * @param timestampMs the current timestamp in milliseconds
     */
    public void setEnabled(boolean enabled, long timestampMs) {
        this.enabled = enabled;
        this.enabledTimestampMs = enabled ? timestampMs : EMPTY_ENABLED_TIMESTAMP_MS;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
