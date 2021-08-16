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
