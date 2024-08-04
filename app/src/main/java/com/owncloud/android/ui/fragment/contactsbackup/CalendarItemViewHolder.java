/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2021 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2021 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.fragment.contactsbackup;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.afollestad.sectionedrecyclerview.SectionedViewHolder;
import com.owncloud.android.R;
import com.owncloud.android.databinding.CalendarlistListItemBinding;

import java.util.ArrayList;

import third_parties.sufficientlysecure.AndroidCalendar;

class CalendarItemViewHolder extends SectionedViewHolder {
    public CalendarlistListItemBinding binding;
    private final ArrayAdapter<AndroidCalendar> adapter;
    private final Context context;

    CalendarItemViewHolder(CalendarlistListItemBinding binding, Context context) {
        super(binding.getRoot());

        this.binding = binding;
        this.context = context;

        adapter = new ArrayAdapter<>(context,
                                     android.R.layout.simple_spinner_dropdown_item,
                                     new ArrayList<>());

        binding.spinner.setAdapter(adapter);
    }

    public void setCalendars(ArrayList<AndroidCalendar> calendars) {
        adapter.clear();
        adapter.addAll(calendars);
    }

    public void setListener(View.OnClickListener onClickListener) {
        itemView.setOnClickListener(onClickListener);
    }

    public void showCalendars(boolean show) {
        if (show) {
            if (adapter.isEmpty()) {
                Toast.makeText(context,
                               context.getResources().getString(R.string.no_calendar_exists),
                               Toast.LENGTH_LONG)
                    .show();
            } else {
                binding.spinner.setVisibility(View.VISIBLE);
            }
        } else {
            binding.spinner.setVisibility(View.GONE);
        }
    }
}
