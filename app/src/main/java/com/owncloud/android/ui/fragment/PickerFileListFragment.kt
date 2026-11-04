/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Daniele Verducci <daniele.verducci@nextcloud.com>
 * SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.fragment

import com.nextcloud.utils.extensions.getDepth
import com.owncloud.android.datamodel.OCFile
import com.owncloud.android.datamodel.OCFileDepth

/**
 * An OCFileListFragment that stores the fileDepth on its own instance,
 * differently from OCFileListFragment, that stores it in a static field.
 * To be used for file picker navigation, to avoid changing main list's one.
 */
class PickerFileListFragment : OCFileListFragment() {
    override var fileDepth: OCFileDepth? = OCFileDepth.Root

    override fun setFileDepth(file: OCFile?) {
        fileDepth = file.getDepth()
    }

    override fun resetFileDepth() {
        fileDepth = OCFileDepth.Root
    }
}
