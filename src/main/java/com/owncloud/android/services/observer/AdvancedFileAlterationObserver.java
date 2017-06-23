/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Original source code:
 * https://github.com/apache/commons-io/blob/master/src/main/java/org/apache/commons/io/monitor/FileAlterationObserver.java
 *
 * Modified by Mario Danic
 * Changes are Copyright (C) 2017 Mario Danic
 * Copyright (C) 2017 Nextcloud GmbH
 *
 * All changes are under the same licence as the original.
 *
 */
package com.owncloud.android.services.observer;

import android.os.SystemClock;

import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.services.AdvancedFileAlterationListener;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileEntry;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AdvancedFileAlterationObserver extends FileAlterationObserver implements Serializable {

    private static final long serialVersionUID = 1185122225658782848L;
    private static final int DELAY_INVOCATION_MS = 2500;
    private final List<AdvancedFileAlterationListener> listeners = new CopyOnWriteArrayList<>();
    private FileEntry rootEntry;
    private FileFilter fileFilter;
    private Comparator<File> comparator;
    private SyncedFolder syncedFolder;

    private static final FileEntry[] EMPTY_ENTRIES = new FileEntry[0];
    
    public AdvancedFileAlterationObserver(SyncedFolder syncedFolder, FileFilter fileFilter) {
        super(syncedFolder.getLocalPath(), fileFilter);

        this.rootEntry = new FileEntry(new File(syncedFolder.getLocalPath()));
        this.fileFilter = fileFilter;
        this.syncedFolder = syncedFolder;
        comparator = NameFileComparator.NAME_SYSTEM_COMPARATOR;
    }

    public long getSyncedFolderID() {
        return syncedFolder.getId();
    }

    public SyncedFolder getSyncedFolder() {
        return syncedFolder;
    }

    /**
     * Return the directory being observed.
     *
     * @return the directory being observed
     */
    public File getDirectory() {
        return rootEntry.getFile();
    }

    /**
     * Return the fileFilter.
     *
     * @return the fileFilter
     * @since 2.1
     */
    public FileFilter getFileFilter() {
        return fileFilter;
    }

    public FileEntry getRootEntry() {
        return rootEntry;
    }

    public void setRootEntry(FileEntry rootEntry) {
        this.rootEntry = rootEntry;
    }

    /**
     * Add a file system listener.
     *
     * @param listener The file system listener
     */
    public void addListener(final AdvancedFileAlterationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Remove a file system listener.
     *
     * @param listener The file system listener
     */
    public void removeListener(final AdvancedFileAlterationListener listener) {
        if (listener != null) {
            while (listeners.remove(listener)) {
            }
        }
    }

    /**
     * Returns the set of registered file system listeners.
     *
     * @return The file system listeners
     */
    public Iterable<AdvancedFileAlterationListener> getMagicListeners() {
        return listeners;
    }

    /**
     * Does nothing - hack for the monitor
     *
     *
     */
    public void initialize() {
        // does nothing - hack the monitor
    }


    /**
     * Initializes everything
     *
     * @throws Exception if an error occurs
     */
    public void init() throws Exception {
        rootEntry.refresh(rootEntry.getFile());
        final FileEntry[] children = doListFiles(rootEntry.getFile(), rootEntry);
        rootEntry.setChildren(children);
    }


    /**
     * Final processing.
     *
     * @throws Exception if an error occurs
     */
    public void destroy() throws Exception {
        Iterator iterator = getMagicListeners().iterator();
        while (iterator.hasNext()) {
            AdvancedFileAlterationListener AdvancedFileAlterationListener = (AdvancedFileAlterationListener) iterator.next();
            while (AdvancedFileAlterationListener.getActiveTasksCount() > 0) {
                SystemClock.sleep(250);
            }
        }
    }

    public void checkAndNotifyNow() {
                /* fire onStart() */
        for (final AdvancedFileAlterationListener listener : listeners) {
            listener.onStart(this);
        }

        /* fire directory/file events */
        final File rootFile = rootEntry.getFile();
        if (rootFile.exists()) {
            checkAndNotify(rootEntry, rootEntry.getChildren(), listFiles(rootFile), 0);
        } else if (rootEntry.isExists()) {
            try {
                // try to init once more
                init();
                if (rootEntry.getFile().exists()) {
                    checkAndNotify(rootEntry, rootEntry.getChildren(), listFiles(rootEntry.getFile()), 0);
                } else {
                    checkAndNotify(rootEntry, rootEntry.getChildren(), FileUtils.EMPTY_FILE_ARRAY, 0);
                }
            } catch (Exception e) {
                Log_OC.d("AdvancedFileAlterationObserver", "Failed getting an observer to intialize " + e);
                checkAndNotify(rootEntry, rootEntry.getChildren(), FileUtils.EMPTY_FILE_ARRAY, 0);
            }
        } // else didn't exist and still doesn't

        /* fire onStop() */
        for (final AdvancedFileAlterationListener listener : listeners) {
            listener.onStop(this);
        }
    }
    
    /**
     * Check whether the file and its children have been created, modified or deleted.
     */
    public void checkAndNotify() {

        /* fire onStart() */
        for (final AdvancedFileAlterationListener listener : listeners) {
            listener.onStart(this);
        }

        /* fire directory/file events */
        final File rootFile = rootEntry.getFile();
        if (rootFile.exists()) {
            checkAndNotify(rootEntry, rootEntry.getChildren(), listFiles(rootFile), DELAY_INVOCATION_MS);
        } else if (rootEntry.isExists()) {
            try {
                // try to init once more
                init();
                if (rootEntry.getFile().exists()) {
                    checkAndNotify(rootEntry, rootEntry.getChildren(), listFiles(rootEntry.getFile()),
                            DELAY_INVOCATION_MS);
                } else {
                    checkAndNotify(rootEntry, rootEntry.getChildren(), FileUtils.EMPTY_FILE_ARRAY, DELAY_INVOCATION_MS);
                }
            } catch (Exception e) {
                Log_OC.d("AdvancedFileAlterationObserver", "Failed getting an observer to intialize " + e);
                checkAndNotify(rootEntry, rootEntry.getChildren(), FileUtils.EMPTY_FILE_ARRAY, DELAY_INVOCATION_MS);
            }
        } // else didn't exist and still doesn't

        /* fire onStop() */
        for (final AdvancedFileAlterationListener listener : listeners) {
            listener.onStop(this);
        }
    }

    /**
     * Compare two file lists for files which have been created, modified or deleted.
     *
     * @param parent   The parent entry
     * @param previous The original list of files
     * @param files    The current list of files
     */
    private void checkAndNotify(final FileEntry parent, final FileEntry[] previous, final File[] files, int delay) {
        if (files != null && files.length > 0) {
            int c = 0;
            final FileEntry[] current = files.length > 0 ? new FileEntry[files.length] : EMPTY_ENTRIES;
            for (final FileEntry entry : previous) {
                while (c < files.length && comparator.compare(entry.getFile(), files[c]) > 0) {
                    current[c] = createFileEntry(parent, files[c]);
                    doCreate(current[c], delay);
                    c++;
                }
                if (c < files.length && comparator.compare(entry.getFile(), files[c]) == 0) {
                    doMatch(entry, files[c], delay);
                    checkAndNotify(entry, entry.getChildren(), listFiles(files[c]), delay);
                    current[c] = entry;
                    c++;
                } else {
                    checkAndNotify(entry, entry.getChildren(), FileUtils.EMPTY_FILE_ARRAY, delay);
                    doDelete(entry);
                }
            }
            for (; c < files.length; c++) {
                current[c] = createFileEntry(parent, files[c]);
                doCreate(current[c], delay);
            }
            parent.setChildren(current);
        }
    }

    /**
     * Create a new file entry for the specified file.
     *
     * @param parent The parent file entry
     * @param file   The file to create an entry for
     * @return A new file entry
     */
    private FileEntry createFileEntry(final FileEntry parent, final File file) {
        final FileEntry entry = parent.newChildInstance(file);
        entry.refresh(file);
        final FileEntry[] children = doListFiles(file, entry);
        entry.setChildren(children);
        return entry;
    }

    /**
     * List the files
     *
     * @param file  The file to list files for
     * @param entry the parent entry
     * @return The child files
     */
    private FileEntry[] doListFiles(File file, FileEntry entry) {
        final File[] files = listFiles(file);
        final FileEntry[] children = files.length > 0 ? new FileEntry[files.length] : EMPTY_ENTRIES;
        for (int i = 0; i < files.length; i++) {
            children[i] = createFileEntry(entry, files[i]);
        }
        return children;
    }

    /**
     * Fire directory/file created events to the registered listeners.
     *
     * @param entry The file entry
     */
    private void doCreate(final FileEntry entry, int delay) {
        for (final AdvancedFileAlterationListener listener : listeners) {
            if (entry.isDirectory()) {
                listener.onDirectoryCreate(entry.getFile());
            } else {
                listener.onFileCreate(entry.getFile(), delay);
            }
        }
        final FileEntry[] children = entry.getChildren();
        for (final FileEntry aChildren : children) {
            doCreate(aChildren, delay);
        }
    }

    /**
     * Fire directory/file change events to the registered listeners.
     *
     * @param entry The previous file system entry
     * @param file  The current file
     */
    private void doMatch(final FileEntry entry, final File file, int delay) {
        if (entry.refresh(file)) {
            for (final AdvancedFileAlterationListener listener : listeners) {
                if (entry.isDirectory()) {
                    listener.onDirectoryChange(file);
                } else {
                    listener.onFileChange(file, delay);
                }
            }
        }
    }

    /**
     * Fire directory/file delete events to the registered listeners.
     *
     * @param entry The file entry
     */
    private void doDelete(final FileEntry entry) {
        for (final AdvancedFileAlterationListener listener : listeners) {
            if (entry.isDirectory()) {
                listener.onDirectoryDelete(entry.getFile());
            } else {
                listener.onFileDelete(entry.getFile());
            }
        }
    }

    /**
     * List the contents of a directory
     *
     * @param file The file to list the contents of
     * @return the directory contents or a zero length array if
     * the empty or the file is not a directory
     */
    private File[] listFiles(final File file) {
        File[] children = null;
        if (file.isDirectory()) {
            children = fileFilter == null ? file.listFiles() : file.listFiles(fileFilter);
        }
        if (children == null) {
            children = FileUtils.EMPTY_FILE_ARRAY;
        }
        if (comparator != null && children.length > 1) {
            Arrays.sort(children, comparator);
        }
        return children;
    }

    /**
     * Provide a String representation of this observer.
     *
     * @return a String representation of this observer
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append("[file='");
        builder.append(getDirectory().getPath());
        builder.append('\'');
        if (fileFilter != null) {
            builder.append(", ");
            builder.append(fileFilter.toString());
        }
        builder.append(", listeners=");
        builder.append(listeners.size());
        builder.append("]");
        return builder.toString();
    }

}