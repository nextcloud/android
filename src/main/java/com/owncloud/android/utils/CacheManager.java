package com.owncloud.android.utils;

import android.content.Context;

import java.io.File;

public class CacheManager {

    /**
     * deleteCache figures out if we have caches
     * and deletes the cache that we have for the app
     */
    public void deleteCache(Context context){
        File cache = context.getCacheDir();
        File appDir = new File(cache.getParent());
        if (appDir.exists() && appDir.list() != null) {
            String[] children = appDir.list();
            for (String s : children) {
                if (!"lib".equals(s)) {
                    deleteDir(new File(appDir, s));
                }
            }
        }
    }

    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory() && dir.list() != null) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
            return dir.delete();
        } else if(dir!= null && dir.isFile()) {
            return dir.delete();
        } else {
            return false;
        }
    }

}
