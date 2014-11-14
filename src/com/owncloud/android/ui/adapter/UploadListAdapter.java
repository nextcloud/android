package com.owncloud.android.ui.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.db.UploadDbHandler;
import com.owncloud.android.db.UploadDbObject;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.ui.activity.UploadListActivity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.FileStorageUtils;

/**
 * This Adapter populates a ListView with following types of uploads: pending,
 * active, completed. Filtering possible.
 * 
 */
public class UploadListAdapter extends BaseAdapter implements ListAdapter, Observer {

    private static final String TAG = "UploadListAdapter";
    private Activity mActivity;
    private UploadDbObject[] mUploads = null;
    UploadDbHandler mDb;

    public UploadListAdapter(Activity context) {
        Log_OC.d(TAG, "UploadListAdapter");
        mActivity = context;
        mDb = UploadDbHandler.getInstance(mActivity);
        loadUploadItemsFromDb();
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        super.registerDataSetObserver(observer);
        mDb.addObserver(this);
        Log_OC.d(TAG, "registerDataSetObserver");
    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        super.unregisterDataSetObserver(observer);
        mDb.deleteObserver(this);
        Log_OC.d(TAG, "unregisterDataSetObserver");
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        return true;
    }

    @Override
    public int getCount() {
        return mUploads != null ? mUploads.length : 0;
    }

    @Override
    public Object getItem(int position) {
        if (mUploads == null || mUploads.length <= position)
            return null;
        return mUploads[position];
    }

    @Override
    public long getItemId(int position) {
        return mUploads != null && mUploads.length <= position ? position : -1;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflator = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.upload_list_item, null);
        }
        if (mUploads != null && mUploads.length > position) {
            UploadDbObject uploadObject = mUploads[position];

            TextView fileName = (TextView) view.findViewById(R.id.upload_name);
            String name = FileStorageUtils.removeDataFolderPath(uploadObject.getLocalPath());
            fileName.setText(name);

            TextView statusView = (TextView) view.findViewById(R.id.upload_status);
            String status = uploadObject.getUploadStatus().toString();
            if (uploadObject.getLastResult() != null && !uploadObject.getLastResult().isSuccess()) {
                status += ": " + uploadObject.getLastResult().getLogMessage();
            }
            statusView.setText(status);

            // ImageView fileIcon = (ImageView)
            // view.findViewById(R.id.imageView1);
            // if (!file.isDirectory()) {
            // fileIcon.setImageResource(R.drawable.file);
            // } else {
            // fileIcon.setImageResource(R.drawable.ic_menu_archive);
            // }

            TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
            ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
            // if (!file.isDirectory()) {
            // fileSizeV.setVisibility(View.VISIBLE);
            // fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.length()));
            // lastModV.setVisibility(View.VISIBLE);
            // lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.lastModified()));
            // ListView parentList = (ListView)parent;
            // if (parentList.getChoiceMode() == ListView.CHOICE_MODE_NONE) {
            // checkBoxV.setVisibility(View.GONE);
            // } else {
            // if (parentList.isItemChecked(position)) {
            // checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
            // } else {
            // checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
            // }
            // checkBoxV.setVisibility(View.VISIBLE);
            // }
            //
            // } else {
            fileSizeV.setVisibility(View.GONE);
            checkBoxV.setVisibility(View.GONE);
            // }

            view.findViewById(R.id.imageView2).setVisibility(View.INVISIBLE); // not
                                                                              // GONE;
                                                                              // the
                                                                              // alignment
                                                                              // changes;
                                                                              // ugly
                                                                              // way
                                                                              // to
                                                                              // keep
                                                                              // it
            view.findViewById(R.id.imageView3).setVisibility(View.GONE);

            view.findViewById(R.id.sharedIcon).setVisibility(View.GONE);
            view.findViewById(R.id.sharedWithMeIcon).setVisibility(View.GONE);
        }

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isEmpty() {
        return (mUploads == null || mUploads.length == 0);
    }

    /**
     * Load upload items from {@link UploadDbHandler}.
     */
    private void loadUploadItemsFromDb() {
        Log_OC.d(TAG, "loadUploadItemsFromDb");
        List<UploadDbObject> list = mDb.getAllStoredUploads();
        mUploads = list.toArray(new UploadDbObject[list.size()]);
        if (mUploads != null) {
            Arrays.sort(mUploads, new Comparator<UploadDbObject>() {
                @Override
                public int compare(UploadDbObject lhs, UploadDbObject rhs) {
                    // if (lhs.isDirectory() && !rhs.isDirectory()) {
                    // return -1;
                    // } else if (!lhs.isDirectory() && rhs.isDirectory()) {
                    // return 1;
                    // }
                    return compareNames(lhs, rhs);
                }

                private int compareNames(UploadDbObject lhs, UploadDbObject rhs) {
                    return lhs.getLocalPath().toLowerCase().compareTo(rhs.getLocalPath().toLowerCase());
                }

            });
        }
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                notifyDataSetChanged();
            }
        });

    }

    @Override
    public void update(Observable arg0, Object arg1) {
        Log_OC.d(TAG, "update");
        loadUploadItemsFromDb();
    }
}
