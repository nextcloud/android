/**
 * Nextcloud Android client application
 *
 * @author Mario Danic
 * Copyright (C) 2017 Mario Danic
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.events;

/**
 * Event for making favoriting work
 */
public class FavoriteEvent {
    public final String remotePath;
    public final boolean shouldFavorite;
    public final String remoteId;

    public FavoriteEvent(String remotePath, boolean shouldFavorite, String remoteId) {
        this.remotePath = remotePath;
        this.shouldFavorite = shouldFavorite;
        this.remoteId = remoteId;
    }
}
