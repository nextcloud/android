/*
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
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.databinding.SortingOrderFragmentBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.FileSortOrder;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

/**
 * Dialog to show and choose the sorting order for the file listing.
 */
public class SortingOrderDialogFragment extends DialogFragment implements Injectable {

    private final static String TAG = SortingOrderDialogFragment.class.getSimpleName();

    public static final String SORTING_ORDER_FRAGMENT = "SORTING_ORDER_FRAGMENT";
    private static final String KEY_SORT_ORDER = "SORT_ORDER";

    private SortingOrderFragmentBinding binding;
    private View[] mTaggedViews;
    private String mCurrentSortOrderName;


    @Inject ViewThemeUtils viewThemeUtils;

    public static SortingOrderDialogFragment newInstance(FileSortOrder sortOrder) {
        SortingOrderDialogFragment dialogFragment = new SortingOrderDialogFragment();

        Bundle args = new Bundle();
        args.putString(KEY_SORT_ORDER, sortOrder.name);
        dialogFragment.setArguments(args);

        dialogFragment.setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog);

        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep the state of the fragment on configuration changes
        setRetainInstance(true);

        binding = null;
        mCurrentSortOrderName = getArguments().getString(KEY_SORT_ORDER, FileSortOrder.sort_a_to_z.name);
    }

    /**
     * find all relevant UI elements and set their values.
     *
     * @param binding the parent binding
     */
    private void setupDialogElements(SortingOrderFragmentBinding binding) {
        viewThemeUtils.platform.colorTextButtons(binding.cancel);

        mTaggedViews = new View[12];
        mTaggedViews[0] = binding.sortByNameAscending;
        mTaggedViews[0].setTag(FileSortOrder.sort_a_to_z);
        mTaggedViews[1] = binding.sortByNameAZText;
        mTaggedViews[1].setTag(FileSortOrder.sort_a_to_z);
        mTaggedViews[2] = binding.sortByNameDescending;
        mTaggedViews[2].setTag(FileSortOrder.sort_z_to_a);
        mTaggedViews[3] = binding.sortByNameZAText;
        mTaggedViews[3].setTag(FileSortOrder.sort_z_to_a);
        mTaggedViews[4] = binding.sortByModificationDateAscending;
        mTaggedViews[4].setTag(FileSortOrder.sort_old_to_new);
        mTaggedViews[5] = binding.sortByModificationDateOldestFirstText;
        mTaggedViews[5].setTag(FileSortOrder.sort_old_to_new);
        mTaggedViews[6] = binding.sortByModificationDateDescending;
        mTaggedViews[6].setTag(FileSortOrder.sort_new_to_old);
        mTaggedViews[7] = binding.sortByModificationDateNewestFirstText;
        mTaggedViews[7].setTag(FileSortOrder.sort_new_to_old);
        mTaggedViews[8] = binding.sortBySizeAscending;
        mTaggedViews[8].setTag(FileSortOrder.sort_small_to_big);
        mTaggedViews[9] = binding.sortBySizeSmallestFirstText;
        mTaggedViews[9].setTag(FileSortOrder.sort_small_to_big);
        mTaggedViews[10] = binding.sortBySizeDescending;
        mTaggedViews[10].setTag(FileSortOrder.sort_big_to_small);
        mTaggedViews[11] = binding.sortBySizeBiggestFirstText;
        mTaggedViews[11].setTag(FileSortOrder.sort_big_to_small);

        setupActiveOrderSelection();
    }

    /**
     * tints the icon reflecting the actual sorting choice in the apps primary color.
     */
    private void setupActiveOrderSelection() {
        for (View view : mTaggedViews) {
            if (!((FileSortOrder) view.getTag()).name.equals(mCurrentSortOrderName)) {
                continue;
            }
            if (view instanceof ImageButton) {
                viewThemeUtils.platform.themeImageButton((ImageButton) view);
                ((ImageButton) view).setSelected(true);
            }
            if (view instanceof TextView) {
                viewThemeUtils.platform.colorPrimaryTextViewElement((TextView) view);
                ((TextView) view).setTypeface(Typeface.DEFAULT_BOLD);
            }
        }
    }

    /**
     * setup all listeners.
     */
    private void setupListeners() {
        binding.cancel.setOnClickListener(view -> dismiss());

        OnSortOrderClickListener sortOrderClickListener = new OnSortOrderClickListener();

        for (View view : mTaggedViews) {
            view.setOnClickListener(sortOrderClickListener);
        }
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        binding = SortingOrderFragmentBinding.inflate(requireActivity().getLayoutInflater(), null, false);

        setupDialogElements(binding);
        setupListeners();

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(binding.getRoot().getContext());
        builder.setView(binding.getRoot());

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.getRoot().getContext(), builder);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        Log_OC.d(TAG, "destroy SortingOrderDialogFragment view");
        if (getDialog() != null && getRetainInstance()) {
            getDialog().setDismissMessage(null);
        }
        super.onDestroyView();
    }

    private class OnSortOrderClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            dismissAllowingStateLoss();
            ((SortingOrderDialogFragment.OnSortingOrderListener) getActivity())
                .onSortingOrderChosen((FileSortOrder) v.getTag());
        }
    }

    public interface OnSortingOrderListener {
        void onSortingOrderChosen(FileSortOrder selection);
    }
}
