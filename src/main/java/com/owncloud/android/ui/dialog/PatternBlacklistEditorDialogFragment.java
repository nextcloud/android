package com.owncloud.android.ui.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.lib.common.utils.Log_OC;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.owncloud.android.R;
import com.owncloud.android.ui.activity.Preferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static com.owncloud.android.ui.activity.ContactsPreferenceActivity.TAG;

public class PatternBlacklistEditorDialogFragment extends DialogFragment {

    private View mView;
    private ListView scrollViewContainer;
    private PatternBlacklistEditorDialogListFragment adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep the state of the fragment on configuration changes
        setRetainInstance(true);


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView, savedInstanceState is " + savedInstanceState);

        mView = inflater.inflate(R.layout.pattern_blacklist_editor_layout, container, true);


        scrollViewContainer = mView.findViewById(R.id.details_scroll_container);

        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        ArrayList<String> list = new ArrayList<String>(sharedpreferences.getStringSet(Preferences.PreferenceKeys.EXCLUDED_AUTOUPLOAD_PATTEN_KEY, Preferences.PreferenceKeys.EXCLUDED_AUTOUPLOAD_PATTEN_DEFAULT_VALUES));


        adapter = new PatternBlacklistEditorDialogListFragment(this.getContext(), list);
        scrollViewContainer.setAdapter(adapter);



        EditText et = mView.findViewById(R.id.add_new_edit_text);
        mView.findViewById(R.id.add_pattern_button).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    adapter.addNewPattern(et.getText().toString());
                }
            });

        mView.findViewById(R.id.cancel).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                   close();
                }
            });

        mView.findViewById(R.id.save).setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    save();
                }
            });

        return mView;
    }

    private void save() {
        SharedPreferences sharedpreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putStringSet(Preferences.PreferenceKeys.EXCLUDED_AUTOUPLOAD_PATTEN_KEY, new HashSet<String>(adapter.getList()));
        editor.apply();
        close();
    }

    private void close() {
        this.dismiss();
    }

    public static PatternBlacklistEditorDialogFragment newInstance(int title) {
        PatternBlacklistEditorDialogFragment frag = new PatternBlacklistEditorDialogFragment();
        Bundle args = new Bundle();
        args.putInt("title", title);
        frag.setArguments(args);
        return frag;
    }


}
