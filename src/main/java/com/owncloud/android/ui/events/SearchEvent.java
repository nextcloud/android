package com.owncloud.android.ui.events;

import com.owncloud.android.lib.resources.files.SearchOperation;

/**
 * Created by mdjanic on 10/03/2017.
 */

public class SearchEvent {
    public final String searchQuery;

    public final SearchOperation.SearchType searchType;


    public SearchEvent(String searchQuery, SearchOperation.SearchType searchType) {

        this.searchQuery = searchQuery;
        this.searchType = searchType;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public SearchOperation.SearchType getSearchType() {
        return searchType;
    }
}
