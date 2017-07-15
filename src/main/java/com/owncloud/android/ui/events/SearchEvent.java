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

import com.owncloud.android.lib.resources.files.SearchOperation;

import org.parceler.Parcel;

/**
 * Search event
 */
@Parcel
public class SearchEvent {
    public String searchQuery;

    public SearchOperation.SearchType searchType;

    public UnsetType unsetType;

    public enum UnsetType {
        NO_UNSET,
        UNSET_DRAWER,
        UNSET_BOTTOM_NAV_BAR
    }

    public SearchEvent() {

    }

    public SearchEvent(String searchQuery, SearchOperation.SearchType searchType, UnsetType unsetType) {

        this.searchQuery = searchQuery;
        this.searchType = searchType;
        this.unsetType = unsetType;

    }

    public UnsetType getUnsetType() {
        return unsetType;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public SearchOperation.SearchType getSearchType() {
        return searchType;
    }

    @Override
    public int hashCode() {

        // Start with a non-zero constant. Prime is preferred
        int result = 17;
        result = 31 * result + (searchQuery == null ? 1 : searchQuery.hashCode());
        result = 31 * result + (searchType == null ? 0 : searchType.hashCode());
        result = 31 * result + (unsetType == null ? 0 : unsetType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null) return false;
        if (!(obj instanceof SearchEvent)) return false;

        final SearchEvent that = (SearchEvent) obj;

        if ((this.searchQuery == null) ? that.searchQuery != null : !this.searchQuery.equals(that.searchQuery)) {
            return false;
        }


        if ((this.searchType == null) ? that.searchType != null : !this.searchType.equals(that.searchType)) {
            return false;
        }

        if ((this.unsetType == null) ? that.unsetType != null : !this.unsetType.equals(that.unsetType)) {
            return false;
        }

        return true;
    }

}
