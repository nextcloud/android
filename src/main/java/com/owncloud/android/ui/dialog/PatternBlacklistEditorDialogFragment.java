package com.owncloud.android.ui.dialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ListView;

import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.preferences.AppPreferences;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;

import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import static com.owncloud.android.ui.activity.ContactsPreferenceActivity.TAG;

public class PatternBlacklistEditorDialogFragment extends DialogFragment implements Injectable {

    private PatternBlacklistEditorDialogListFragment adapter;

    @Inject AppPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // keep the state of the fragment on configuration changes
        setRetainInstance(true);


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log_OC.d(TAG, "onCreateView, savedInstanceState is " + savedInstanceState);

        View mView = inflater.inflate(R.layout.pattern_blacklist_editor_layout, container, true);


        ListView scrollViewContainer = mView.findViewById(R.id.details_scroll_container);

        List<String> list = preferences.getAutoUploadPatternBlackList();

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
        preferences.setAutoUploadPatternBlackList(adapter.getPatternList());
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
