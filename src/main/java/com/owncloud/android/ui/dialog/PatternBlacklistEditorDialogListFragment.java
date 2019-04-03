package com.owncloud.android.ui.dialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.owncloud.android.R;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import lombok.Getter;

public class PatternBlacklistEditorDialogListFragment extends ArrayAdapter<String> {

    @Getter
    private List<String> patternList;

    public PatternBlacklistEditorDialogListFragment(Context context, List<String> patterns) {
        super(context, 0, patterns);
        patternList = patterns;
    }

    @Override
    public @NonNull
    View getView(int position, View view, @NonNull ViewGroup parent) {

        String pattern = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.pattern_blacklist_editor_list_item, parent, false);
        }

        TextView label = view.findViewById(R.id.pattern_name);
        ImageButton ib = view.findViewById(R.id.delete_pattern_button);

        ib.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removePattern(pattern);
                }
            });
        label.setText(pattern);
        return view;
    }

    private void removePattern(String pattern) {
        patternList.remove(pattern);
        notifyDataSetChanged();

    }

    public void addNewPattern(String patternToAdd) {

        //pattern cannot be empty
        if (TextUtils.isEmpty(patternToAdd)) {
            return;
        }

        boolean foundPattern = false;
        for (String pattern : patternList) {
            if (patternToAdd.equals(pattern)) {
                foundPattern = true;
            }
        }

        if (!foundPattern) {
            patternList.add(patternToAdd);
        }
        notifyDataSetChanged();
    }
}
