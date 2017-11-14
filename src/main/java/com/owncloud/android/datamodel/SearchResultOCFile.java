package com.owncloud.android.datamodel;

import java.util.ArrayList;

/**
 * Created by tobi on 13.11.17.
 */

public class SearchResultOCFile extends OCFile {

    private ArrayList<String> excerpts;
    private String searchTerm;

    public SearchResultOCFile(String path, ArrayList<String> excerpts) {
        super(path);
        this.excerpts = excerpts;
        this.searchTerm = searchTerm;
    }

    public void setExcerpts(ArrayList<String> excerpts) {
        this.excerpts = excerpts;
    }

    public ArrayList<String> getExcerpts() {
        return excerpts;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getSearchTerm() {
        return searchTerm;
    }
}
