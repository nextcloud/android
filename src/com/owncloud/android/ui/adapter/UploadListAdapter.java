package com.owncloud.android.ui.adapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import android.content.Context;
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
import com.owncloud.android.utils.DisplayUtils;

/**
 * This Adapter populates a ListView with following types of uploads: pending, active, completed.
 * Filtering possible.
 * 
 */
public class UploadListAdapter extends BaseAdapter implements ListAdapter {
    
    private Context mContext;
    private UploadDbObject[] mUploads = null;

    public UploadListAdapter(Context context) {
        mContext = context;
        loadUploadItemsFromDb();
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
            LayoutInflater inflator = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.upload_list_item, null);
        }
        if (mUploads != null && mUploads.length > position) {
            UploadDbObject file = mUploads[position];
            
            TextView fileName = (TextView) view.findViewById(R.id.Filename);
            String name = file.getLocalPath();
            fileName.setText(name);
            
//            ImageView fileIcon = (ImageView) view.findViewById(R.id.imageView1);
//            if (!file.isDirectory()) {
//                fileIcon.setImageResource(R.drawable.file);
//            } else {
//                fileIcon.setImageResource(R.drawable.ic_menu_archive);
//            }

            TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
            TextView lastModV = (TextView) view.findViewById(R.id.last_mod);
            ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
//            if (!file.isDirectory()) {
//                fileSizeV.setVisibility(View.VISIBLE);
//                fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.length()));
//                lastModV.setVisibility(View.VISIBLE);
//                lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.lastModified()));
//                ListView parentList = (ListView)parent;
//                if (parentList.getChoiceMode() == ListView.CHOICE_MODE_NONE) { 
//                    checkBoxV.setVisibility(View.GONE);
//                } else {
//                    if (parentList.isItemChecked(position)) {
//                        checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
//                    } else {
//                        checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
//                    }
//                    checkBoxV.setVisibility(View.VISIBLE);
//                }
//
//            } else {
                fileSizeV.setVisibility(View.GONE);
                lastModV.setVisibility(View.GONE);
                checkBoxV.setVisibility(View.GONE);
//            }
            
            view.findViewById(R.id.imageView2).setVisibility(View.INVISIBLE);   // not GONE; the alignment changes; ugly way to keep it
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
    public void loadUploadItemsFromDb() {
        UploadDbHandler mDb = new UploadDbHandler(mContext);
        List<UploadDbObject> list = mDb.getAllStoredUploads();
        mUploads = list.toArray(new UploadDbObject[list.size()]);
        if (mUploads != null) {
            Arrays.sort(mUploads, new Comparator<UploadDbObject>() {
                @Override
                public int compare(UploadDbObject lhs, UploadDbObject rhs) {
//                    if (lhs.isDirectory() && !rhs.isDirectory()) {
//                        return -1;
//                    } else if (!lhs.isDirectory() && rhs.isDirectory()) {
//                        return 1;
//                    }
                    return compareNames(lhs, rhs);
                }
            
                private int compareNames(UploadDbObject lhs, UploadDbObject rhs) {
                    return lhs.getLocalPath().toLowerCase().compareTo(rhs.getLocalPath().toLowerCase());                
                }
            
            });
        }
        notifyDataSetChanged();
    }
}
