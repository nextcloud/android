/**
 *   ownCloud Android client application
 *
 *   Copyright (C) 2012 Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import android.os.FileObserver;

public class RecursiveFileObserver extends FileObserver {

    private final List<SingleFileObserver> mObservers =  new ArrayList<>();
    private boolean watching = false;
    private String mPath;
    private int mMask;
    
    public RecursiveFileObserver(String path) {
        this(path, ALL_EVENTS);
    }
    
    RecursiveFileObserver(String path, int mask) {
        super(path, mask);
        mPath = path;
        mMask = mask;
    }

    @Override
    public void startWatching() {
        if (watching) {
            return;
        }
        watching = true;
        final Stack<String> stack = new Stack<String>();
        stack.push(mPath);
        
        while (!stack.empty()) {
            String parent = stack.pop();
            mObservers.add(new SingleFileObserver(parent, mMask));
            File path = new File(parent);
            File[] files = path.listFiles();
            if (files == null) {
                continue;
            }
            for (final File file : files) {
                if (file.isDirectory() && !file.getName().equals(".")
                        && !file.getName().equals("..")) {
                    stack.push(file.getPath());
                }
            }
        }
        for (int i = 0; i < mObservers.size(); i++) {
            mObservers.get(i).startWatching();
        }
    }
    
    @Override
    public void stopWatching() {
        if (!watching) {
            return;
        }

        for (int i = 0; i < mObservers.size(); ++i) {
            mObservers.get(i).stopWatching();
        }
        mObservers.clear();
        watching = false;
    }
    
    @Override
    public void onEvent(int event, String path) {
        
    }
    
    private class SingleFileObserver extends FileObserver {
        private String mPath;

        SingleFileObserver(String path, int mask) {
            super(path, mask);
            mPath = path;
        }
        
        @Override
        public void onEvent(int event, String path) {
            String newPath = mPath + "/" + path;
            RecursiveFileObserver.this.onEvent(event, newPath);
        } 
        
    }
}
