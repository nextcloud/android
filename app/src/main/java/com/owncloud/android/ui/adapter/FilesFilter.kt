/*
 *
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2022 Tobias Kaminsky
 * Copyright (C) 2022 Nextcloud GmbH
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
package com.owncloud.android.ui.adapter

import android.text.TextUtils
import android.widget.Filter
import com.owncloud.android.datamodel.OCFile
import java.util.Locale
import java.util.Vector

internal class FilesFilter(private val ocFileListAdapter: OCFileListAdapter) : Filter() {
    override fun performFiltering(constraint: CharSequence): FilterResults {
        val results = FilterResults()
        val filteredFiles = Vector<OCFile>()
        if (!TextUtils.isEmpty(constraint)) {
            for (file in ocFileListAdapter.allFiles) {
                if (file.parentRemotePath == ocFileListAdapter.currentDirectory.remotePath &&
                    file.fileName.lowercase(Locale.getDefault()).contains(
                            constraint.toString().lowercase(Locale.getDefault())
                        ) &&
                    !filteredFiles.contains(file)
                ) {
                    filteredFiles.add(file)
                }
            }
        }
        results.values = filteredFiles
        results.count = filteredFiles.size
        return results
    }

    override fun publishResults(constraint: CharSequence, results: FilterResults) {
        val ocFiles = results.values as Vector<OCFile>
        ocFileListAdapter.updateFilteredResults(ocFiles)
    }
}
