package com.owncloud.android.datamodel;

import java.util.ArrayList;

/**
 * Created by tobi on 13.11.17.
 */

public class SearchResultOCFile extends OCFile {

    private ArrayList<String> excerpts;

    public SearchResultOCFile(String path) {
        super(path);
    }

    public void setExcerpts(ArrayList<String> excerpts) {
        this.excerpts = excerpts;
    }

    public ArrayList<String> getExcerpts() {
        return excerpts;
    }
}
