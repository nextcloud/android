/**
 * Nextcloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2017 Andy Scherzinger
 * Copyright (C) 2017 Nextcloud
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.dialog;

import android.app.Dialog;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

/**
 * Dialog to show and choose the sorting order for the file listing.
 */
public class SortingOrderDialogFragment extends DialogFragment {

    private final static String TAG = SortingOrderDialogFragment.class.getSimpleName();

    private static final String KEY_SORT_ORDER = "SORT_ORDER";
    private static final String KEY_ASCENDING = "ASCENDING";

    public static final int BY_NAME_ASC = 0;
    public static final int BY_NAME_DESC = 1;
    public static final int BY_MODIFICATION_DATE_ASC = 2;
    public static final int BY_MODIFICATION_DATE_DESC = 3;
    public static final int BY_SIZE_ASC = 4;
    public static final int BY_SIZE_DESC = 5;

    private View mView = null;
    private ImageButton mSortByNameAscendingButton = null;
    private ImageButton mSortByNameDescendingButton = null;
    private ImageButton mSortBySizeAscendingButton = null;
    private ImageButton mSortBySizeDescendingButton = null;
    private ImageButton mSortByModificationDateAscendingButton = null;
    private ImageButton mSortByModificationDateDescendingButton = null;

    private int mSortOrder;
    private boolean mSortAscending;

    public static SortingOrderDialogFragment newInstance(int sortOrder, boolean ascending) {
        SortingOrderDialogFragment dialogFragment = new SortingOrderDialogFragment();

        Bundle args = new Bundle();
        args.putInt(KEY_SORT_ORDER, sortOrder);
        args.putBoolean(KEY_ASCENDING, ascending);
        dialogFragment.setArguments(args);

        dialogFragment.setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep the state of the fragment on configuration changes
        setRetainInstance(true);

        mView = null;

        mSortOrder = getArguments().getInt(KEY_SORT_ORDER, BY_NAME_ASC);
        mSortAscending = getArguments().getBoolean(KEY_ASCENDING, true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView, savedInstanceState is " + savedInstanceState);

        mView = inflater.inflate(R.layout.sorting_order_fragment, container, false);

        setupDialogElements(mView);
        setupListeners(mView);

        return mView;
    }

    /**
     * find all relevant UI elements and set their values.
     *
     * @param view the parent view
     */
    private void setupDialogElements(View view) {
        // find/saves UI elements
        mSortByNameAscendingButton = (ImageButton) view.findViewById(R.id.sortByNameAscending);
        mSortByNameDescendingButton = (ImageButton) view.findViewById(R.id.sortByNameDescending);
        mSortByModificationDateAscendingButton = (ImageButton) view.findViewById(R.id.sortByModificationDateAscending);
        mSortByModificationDateDescendingButton = (ImageButton) view.findViewById(R.id.sortByModificationDateDescending);
        mSortBySizeAscendingButton = (ImageButton) view.findViewById(R.id.sortBySizeAscending);
        mSortBySizeDescendingButton = (ImageButton) view.findViewById(R.id.sortBySizeDescending);

        mSortByNameAscendingButton.setTag(BY_NAME_ASC);
        mSortByNameDescendingButton.setTag(BY_NAME_DESC);
        mSortByModificationDateAscendingButton.setTag(BY_MODIFICATION_DATE_ASC);
        mSortByModificationDateDescendingButton.setTag(BY_MODIFICATION_DATE_DESC);
        mSortBySizeAscendingButton.setTag(BY_SIZE_ASC);
        mSortBySizeDescendingButton.setTag(BY_SIZE_DESC);

        setupActiveOrderSelection();
    }

    /**
     * tints the icon reflecting the actual sorting choice in the apps primary color.
     */
    private void setupActiveOrderSelection() {
        if (mSortAscending) {
            switch (mSortOrder) {
                case 0:
                    setActiveState(mSortByNameAscendingButton);
                    break;
                case 1:
                    setActiveState(mSortByModificationDateAscendingButton);
                    break;
                case 2:
                    setActiveState(mSortBySizeAscendingButton);
                    break;
                default: //do nothing
                    Log_OC.w(TAG, "Unknown sort criteria!");
                    break;
            }
        } else {
            switch (mSortOrder) {
                case 0:
                    setActiveState(mSortByNameDescendingButton);
                    break;
                case 1:
                    setActiveState(mSortByModificationDateDescendingButton);
                    break;
                case 2:
                    setActiveState(mSortBySizeDescendingButton);
                    break;
                default: //do nothing
                    Log_OC.w(TAG, "Unknown sort criteria!");
                    break;
            }
        }
    }

    /**
     * tints a given ImageButton's drawable in the app's primary color.
     *
     * @param imageButton the image button to tint
     */
    private void setActiveState(ImageButton imageButton) {
        imageButton.setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.SRC_ATOP);
    }

    /**
     * setup all listeners.
     *
     * @param view the parent view
     */
    private void setupListeners(View view) {
        view.findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        OnSortingOrderClickListener sortingClickListener = new OnSortingOrderClickListener();
        mSortByNameAscendingButton.setOnClickListener(sortingClickListener);
        mSortByNameDescendingButton.setOnClickListener(sortingClickListener);
        mSortByModificationDateAscendingButton.setOnClickListener(sortingClickListener);
        mSortByModificationDateDescendingButton.setOnClickListener(sortingClickListener);
        mSortBySizeAscendingButton.setOnClickListener(sortingClickListener);
        mSortBySizeDescendingButton.setOnClickListener(sortingClickListener);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        Log_OC.d(TAG, "destroy SortingOrderDialogFragment view");
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    private class OnSortingOrderClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            dismissAllowingStateLoss();
            ((SortingOrderDialogFragment.OnSortingOrderListener) getActivity()).onSortingOrderChosen((int) v.getTag());
        }
    }

    public interface OnSortingOrderListener {
        void onSortingOrderChosen(int selection);
    }
}
