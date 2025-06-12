/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;
import android.os.Build;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment;
import com.owncloud.android.ui.fragment.contactsbackup.VCardComparator;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ezvcard.Ezvcard;
import ezvcard.VCard;

public class LoadContactsTask extends AsyncTask<Void, Void, Boolean> {
    private final WeakReference<BackupListFragment> backupListFragmentWeakReference;
    private final OCFile ocFile;
    private final List<VCard> vCards = new ArrayList<>();

    public LoadContactsTask(BackupListFragment backupListFragment, OCFile ocFile) {
        this.backupListFragmentWeakReference = new WeakReference<>(backupListFragment);
        this.ocFile = ocFile;
    }

    @Override
    protected void onPreExecute() {
        if (backupListFragmentWeakReference.get() != null && !backupListFragmentWeakReference.get().hasCalendarEntry()) {
            backupListFragmentWeakReference.get().showLoadingMessage(true);
        }
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        if (!isCancelled()) {
            File file = new File(ocFile.getStoragePath());
            try {
                vCards.addAll(Ezvcard.parse(new BufferedInputStream(Files.newInputStream(file.toPath()))).all());
                Collections.sort(vCards, new VCardComparator());
            } catch (IOException e) {
                Log_OC.e(this, "IO Exception: " + file.getAbsolutePath());
                return Boolean.FALSE;
            }
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    protected void onPostExecute(Boolean bool) {
        if (!isCancelled() && bool && backupListFragmentWeakReference.get() != null) {
            backupListFragmentWeakReference.get().loadVCards(vCards);
        }
    }
}
