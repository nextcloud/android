/*
 * Nextcloud Android client application
 *
 * @author Piotr Bator
 * Copyright (C) 2022 Piotr Bator
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

package com.owncloud.android.ui.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.nextcloud.client.di.Injectable;
import com.owncloud.android.R;
import com.owncloud.android.databinding.DurationPickerBinding;
import com.owncloud.android.utils.TimeUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import static com.owncloud.android.utils.TimeUtils.getDurationParts;

public class DurationPickerDialogFragment extends DialogFragment implements Injectable {
    private static final int MAX_DAYS_VALUE = 30;
    private static final int MAX_HOURS_VALUE = 24;
    private static final int MAX_MINUTES_VALUE = 59;

    private static final String DURATION = "DURATION";
    private static final String DIALOG_TITLE = "TITLE";
    private static final String HINT_MESSAGE = "HINT";

    @Inject ViewThemeUtils viewThemeUtils;

    private DurationPickerBinding binding;

    public Listener resultListener;

    public static DurationPickerDialogFragment newInstance(long duration, String title, String hintMessage) {
        Bundle args = new Bundle();
        args.putLong(DURATION, duration);
        args.putString(HINT_MESSAGE, hintMessage);
        args.putString(DIALOG_TITLE, title);

        DurationPickerDialogFragment dialogFragment = new DurationPickerDialogFragment();
        dialogFragment.setArguments(args);
        dialogFragment.setStyle(STYLE_NORMAL, R.style.Theme_ownCloud_Dialog);

        return dialogFragment;
    }

    public void setListener(Listener listener) {
        resultListener = listener;
    }

    @Override
    public void onStart() {
        super.onStart();

        AlertDialog alertDialog = (AlertDialog) getDialog();

        viewThemeUtils.platform.colorTextButtons(alertDialog.getButton(AlertDialog.BUTTON_POSITIVE),
                                                 alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(DURATION, getDuration());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedState) {
        binding = DurationPickerBinding.inflate(requireActivity().getLayoutInflater(), null, false);

        setupLimits();

        long duration;
        if (savedState != null) {
            duration = savedState.getLong(DURATION);
        } else {
            duration = requireArguments().getLong(DURATION);
        }
        setDuration(duration);

        String hintMessage = requireArguments().getString(HINT_MESSAGE);
        setHintMessage(hintMessage);

        binding.clear.setOnClickListener(view -> {
            binding.daysPicker.setValue(0);
            binding.hoursPicker.setValue(0);
            binding.minutesPicker.setValue(0);
        });

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(binding.getRoot().getContext());
        String dialogTitle = requireArguments().getString(DIALOG_TITLE);
        builder.setTitle(dialogTitle);
        builder.setView(binding.getRoot());
        builder.setPositiveButton(R.string.common_save, (dialog, whichButton) -> {
            if (resultListener != null) {
                resultListener.onDurationPickerResult(Activity.RESULT_OK, getDuration());
            }
        });
        builder.setNegativeButton(R.string.common_cancel, (dialog, whichButton) -> {
            if (resultListener != null) {
                resultListener.onDurationPickerResult(Activity.RESULT_CANCELED, 0);
            }
        });

        viewThemeUtils.dialog.colorMaterialAlertDialogBackground(binding.getRoot().getContext(), builder);

        return builder.create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void setupLimits() {
        binding.daysPicker.setMaxValue(MAX_DAYS_VALUE);
        binding.hoursPicker.setMaxValue(MAX_HOURS_VALUE);
        binding.minutesPicker.setMaxValue(MAX_MINUTES_VALUE);
    }

    private long getDuration() {
        return TimeUnit.DAYS.toMillis(binding.daysPicker.getValue()) +
            TimeUnit.HOURS.toMillis(binding.hoursPicker.getValue()) +
            TimeUnit.MINUTES.toMillis(binding.minutesPicker.getValue());
    }

    private void setDuration(long duration) {
        TimeUtils.DurationParts durationParts = getDurationParts(duration);
        binding.daysPicker.setValue(durationParts.getDays());
        binding.hoursPicker.setValue(durationParts.getHours());
        binding.minutesPicker.setValue(durationParts.getMinutes());
    }

    private void setHintMessage(String hintMessage) {
        binding.pickerHint.setVisibility(hintMessage != null ? View.VISIBLE : View.GONE);
        binding.pickerHint.setText(hintMessage);
    }

    interface Listener {
        void onDurationPickerResult(int resultCode, long duration);
    }
}
