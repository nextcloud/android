package com.owncloud.android.ui.dialog;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.owncloud.android.R;

import java.util.ArrayList;

public class PatternBlacklistEditorDialogListFragment extends ArrayAdapter<String> {

    ArrayList<String> patternList = new ArrayList<>();

    public PatternBlacklistEditorDialogListFragment(Context context, ArrayList<String> patterns) {
        super(context, 0, patterns);
        patternList=patterns;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        String pattern = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view

        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.pattern_blacklist_editor_list_item, parent, false);
        }

        TextView label = (TextView) view.findViewById(R.id.pattern_name);
        ImageButton ib = (ImageButton) view.findViewById(R.id.delete_pattern_button);

        ib.setTag(position);
        ib.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removePattern(pattern, (Integer) v.getTag());
                }
            });


        label.setText(pattern);

        return view;
    }

    public ArrayList<String> getList() {
        return patternList;
    }


    private void removePattern(String pattern, int pos){
        patternList.remove(pattern);
        remove(getItem(pos));
        notifyDataSetChanged();

    }
}
