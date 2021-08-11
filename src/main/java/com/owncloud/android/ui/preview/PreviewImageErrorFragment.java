package com.owncloud.android.ui.preview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.owncloud.android.R;
import com.owncloud.android.ui.fragment.FileFragment;

import androidx.annotation.Nullable;

import static com.owncloud.android.ui.activity.FileActivity.EXTRA_FILE;

/**
 * A fragment showing an error message
 */

public class PreviewImageErrorFragment extends FileFragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.preview_image_error_fragment, container, false);
    }

    public static FileFragment newInstance() {
        FileFragment fileFragment = new PreviewImageErrorFragment();
        Bundle bundle = new Bundle();

        bundle.putParcelable(EXTRA_FILE, null);
        fileFragment.setArguments(bundle);

        return fileFragment;
    }
}
