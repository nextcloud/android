/*
 * Nextcloud application
 *
 * @author Mario Danic
 * Copyright (C) 2017-2018 Mario Danic <mario@lovelyhq.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.datamodel;

import com.google.gson.annotations.SerializedName;

import org.parceler.Parcel;

/*
 * Push data from server, https://github.com/nextcloud/notifications/blob/master/docs/push-v2.md#encrypted-subject-data
 */
@Parcel
public class DecryptedPushMessage {
    public String app;
    public String type;
    public String subject;
    public String id;
    public int nid;
    public boolean delete;
    @SerializedName("delete-all")
    public boolean deleteAll;

    public DecryptedPushMessage(String app, String type, String subject, String id, int nid, boolean delete, boolean deleteAll) {
        this.app = app;
        this.type = type;
        this.subject = subject;
        this.id = id;
        this.nid = nid;
        this.delete = delete;
        this.deleteAll = deleteAll;
    }

    public DecryptedPushMessage() {
        // empty constructor
    }

    public String getApp() {
        return this.app;
    }

    public String getType() {
        return this.type;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getId() {
        return this.id;
    }

    public int getNid() {
        return this.nid;
    }

    public boolean isDelete() {
        return this.delete;
    }

    public boolean isDeleteAll() {
        return this.deleteAll;
    }

    public void setApp(String app) {
        this.app = app;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setNid(int nid) {
        this.nid = nid;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public void setDeleteAll(boolean deleteAll) {
        this.deleteAll = deleteAll;
    }
}
