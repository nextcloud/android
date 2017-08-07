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
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.ThemeUtils;

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

    private TextView mSortByNameAscendingText = null;
    private TextView mSortByNameDescendingText = null;
    private TextView mSortBySizeAscendingText = null;
    private TextView mSortBySizeDescendingText = null;
    private TextView mSortByModificationDateAscendingText = null;
    private TextView mSortByModificationDateDescendingText = null;

    private int mSortOrder;
    private boolean mSortAscending;
    private AppCompatButton mCancel;

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
        setupListeners();

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
        mCancel = (AppCompatButton) view.findViewById(R.id.cancel);
        mCancel.setTextColor(ThemeUtils.primaryAccentColor());

        mSortByNameAscendingText = (TextView) view.findViewById(R.id.sortByNameAZText);
        mSortByNameDescendingText = (TextView) view.findViewById(R.id.sortByNameZAText);
        mSortByModificationDateAscendingText = (TextView) view.findViewById(R.id.sortByModificationDateOldestFirstText);
        mSortByModificationDateDescendingText = (TextView) view.findViewById(R.id.sortByModificationDateNewestFirstText);
        mSortBySizeAscendingText = (TextView) view.findViewById(R.id.sortBySizeSmallestFirstText);
        mSortBySizeDescendingText = (TextView) view.findViewById(R.id.sortBySizeBiggestFirstText);

        mSortByNameAscendingButton.setTag(BY_NAME_ASC);
        mSortByNameDescendingButton.setTag(BY_NAME_DESC);
        mSortByModificationDateAscendingButton.setTag(BY_MODIFICATION_DATE_ASC);
        mSortByModificationDateDescendingButton.setTag(BY_MODIFICATION_DATE_DESC);
        mSortBySizeAscendingButton.setTag(BY_SIZE_ASC);
        mSortBySizeDescendingButton.setTag(BY_SIZE_DESC);

        mSortByNameAscendingText.setTag(BY_NAME_ASC);
        mSortByNameDescendingText.setTag(BY_NAME_DESC);
        mSortByModificationDateAscendingText.setTag(BY_MODIFICATION_DATE_ASC);
        mSortByModificationDateDescendingText.setTag(BY_MODIFICATION_DATE_DESC);
        mSortBySizeAscendingText.setTag(BY_SIZE_ASC);
        mSortBySizeDescendingText.setTag(BY_SIZE_DESC);

        setupActiveOrderSelection();
    }

    /**
     * tints the icon reflecting the actual sorting choice in the apps primary color.
     */
    private void setupActiveOrderSelection() {
        if (mSortAscending) {
            switch (mSortOrder) {
                case 0:
                    colorActiveSortingIconAndText(mSortByNameAscendingButton,
                            mSortByNameAscendingText);
                    break;
                case 1:
                    colorActiveSortingIconAndText(mSortByModificationDateAscendingButton,
                            mSortByModificationDateAscendingText);
                    break;
                case 2:
                    colorActiveSortingIconAndText(mSortBySizeAscendingButton,
                            mSortBySizeAscendingText);
                    break;
                default: //do nothing
                    Log_OC.w(TAG, "Unknown sort order " + mSortOrder);
                    break;
            }
        } else {
            switch (mSortOrder) {
                case 0:
                    colorActiveSortingIconAndText(mSortByNameDescendingButton,
                            mSortByNameDescendingText);
                    break;
                case 1:
                    colorActiveSortingIconAndText(mSortByModificationDateDescendingButton,
                            mSortByModificationDateDescendingText);
                    break;
                case 2:
                    colorActiveSortingIconAndText(mSortBySizeDescendingButton,
                            mSortBySizeDescendingText);
                    break;
                default: //do nothing
                    Log_OC.w(TAG, "Unknown sort order " + mSortOrder);
                    break;
            }
        }
    }

    /**
     * Sets the text color and tint the icon of given text view and image button.
     *
     * @param imageButton the image button, the icon to be tinted
     * @param textView    the text view, the text color to be set
     */
    private void colorActiveSortingIconAndText(ImageButton imageButton, TextView textView) {
        int color = ThemeUtils.primaryAccentColor();
        ThemeUtils.colorImageButton(imageButton, color);
        textView.setTextColor(color);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
    }

    /**
     * setup all listeners.
     */
    private void setupListeners() {
        mCancel.setOnClickListener(new View.OnClickListener() {
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

        mSortByNameAscendingText.setOnClickListener(sortingClickListener);
        mSortByNameDescendingText.setOnClickListener(sortingClickListener);
        mSortByModificationDateAscendingText.setOnClickListener(sortingClickListener);
        mSortByModificationDateDescendingText.setOnClickListener(sortingClickListener);
        mSortBySizeAscendingText.setOnClickListener(sortingClickListener);
        mSortBySizeDescendingText.setOnClickListener(sortingClickListener);
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
