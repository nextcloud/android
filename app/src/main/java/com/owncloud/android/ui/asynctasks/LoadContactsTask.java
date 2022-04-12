/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2021 Tobias Kaminsky
 * Copyright (C) 2021 Nextcloud GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment;
import com.owncloud.android.ui.fragment.contactsbackup.VCardComparator;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
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
                vCards.addAll(Ezvcard.parse(file).all());
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
