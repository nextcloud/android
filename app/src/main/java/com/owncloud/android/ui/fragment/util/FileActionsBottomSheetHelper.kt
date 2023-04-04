/* Nextcloud Android Library is available under MIT license
*
*   @author Joshua Soberg
*   Copyright (C) 2023 Joshua Soberg
*   Copyright (C) 2023 Nextcloud GmbH
*
*   Permission is hereby granted, free of charge, to any person obtaining a copy
*   of this software and associated documentation files (the "Software"), to deal
*   in the Software without restriction, including without limitation the rights
*   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
*   copies of the Software, and to permit persons to whom the Software is
*   furnished to do so, subject to the following conditions:
*
*   The above copyright notice and this permission notice shall be included in
*   all copies or substantial portions of the Software.
*
*   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
*   EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
*   MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
*   NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS
*   BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
*   ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
*   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
*   THE SOFTWARE.
*
*/

package com.owncloud.android.ui.fragment.util

import androidx.fragment.app.Fragment
import com.nextcloud.ui.fileactions.FileActionsBottomSheet

object FileActionsBottomSheetHelper {

    /** Checks [parentFragment]'s child Fragment manager for a [FileActionsBottomSheet] with the tag [childFragmentTag].
     * If a [FileActionsBottomSheet] is found, [listener] will be set on it with [parentFragment]'s lifecycle. */
    @JvmStatic
    fun trySetResultListener(
        parentFragment: Fragment,
        childFragmentTag: String,
        listener: FileActionsBottomSheet.ResultListener
    ) {
        with(parentFragment) {
            val fragmentManager = childFragmentManager
            val actionsFragment = fragmentManager.findFragmentByTag(childFragmentTag) as? FileActionsBottomSheet
            actionsFragment?.setResultListener(fragmentManager, this, listener)
        }
    }
}
