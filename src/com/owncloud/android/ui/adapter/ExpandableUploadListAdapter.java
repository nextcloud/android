package com.owncloud.android.ui.adapter;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Observable;
import java.util.Observer;

import android.app.Activity;
import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.db.UploadDbHandler;
import com.owncloud.android.db.UploadDbObject;
import com.owncloud.android.lib.common.network.OnDatatransferProgressListener;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DisplayUtils;

/**
 * This Adapter populates a ListView with following types of uploads: pending,
 * active, completed. Filtering possible.
 * 
 */
public class ExpandableUploadListAdapter extends BaseExpandableListAdapter implements Observer, OnDatatransferProgressListener {

    private static final String TAG = "ExpandableUploadListAdapter";
    private Activity mActivity;
    
    interface Refresh {
        public void refresh();
    }
    abstract class UploadGroup implements Refresh {
        UploadDbObject[] items;
        String name;
        public UploadGroup(String groupName) {
            this.name = groupName;            
            items = new UploadDbObject[0];
        }        
        public String getGroupName() {
            return name;
        }
        public Comparator<UploadDbObject> comparator = new Comparator<UploadDbObject>() {
            @Override
            public int compare(UploadDbObject lhs, UploadDbObject rhs) {
                return compareUploadTime(lhs, rhs);
            }
            private int compareUploadTime(UploadDbObject lhs, UploadDbObject rhs) {
                return rhs.getUploadTime().compareTo(lhs.getUploadTime());
            }
        };
        abstract public int getGroupIcon();
    }
    private UploadGroup[] mUploadGroups = null;
    UploadDbHandler mDb;

    public ExpandableUploadListAdapter(Activity context) {
        Log_OC.d(TAG, "UploadListAdapter");
        mActivity = context;
        mDb = UploadDbHandler.getInstance(mActivity);
        mUploadGroups = new UploadGroup[3];
        mUploadGroups[0] = new UploadGroup("Current Uploads") {
            @Override
            public void refresh() {
                items = mDb.getCurrentAndPendingUploads();
                Arrays.sort(items, comparator);
            }
            @Override
            public int getGroupIcon() {
                return R.drawable.upload_in_progress;
            }
        };
        mUploadGroups[1] = new UploadGroup("Failed Uploads"){
            @Override
            public void refresh() {
                items = mDb.getFailedUploads();
                Arrays.sort(items, comparator);
            }
            @Override
            public int getGroupIcon() {
                return R.drawable.upload_failed;
            }

        };
        mUploadGroups[2] = new UploadGroup("Finished Uploads"){
            @Override
            public void refresh() {
                items = mDb.getFinishedUploads();
                Arrays.sort(items, comparator);
            }
            @Override
            public int getGroupIcon() {
                return R.drawable.upload_finished;
            }

        };
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

    private View getView(UploadDbObject[] uploadsItems, int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflator = (LayoutInflater) mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.upload_list_item, null);
        }
        if (uploadsItems != null && uploadsItems.length > position) {
            UploadDbObject uploadObject = uploadsItems[position];

            TextView fileName = (TextView) view.findViewById(R.id.upload_name);
            fileName.setText(uploadObject.getLocalPath());

            TextView statusView = (TextView) view.findViewById(R.id.upload_status);
            String status = uploadObject.getUploadStatus().toString();
            if (uploadObject.getLastResult() != null && !uploadObject.getLastResult().isSuccess()) {
                status += ": " + uploadObject.getLastResult().getLogMessage();
            }
            statusView.setText(status);

            ImageView fileIcon = (ImageView) view.findViewById(R.id.imageView1);
            fileIcon.setImageResource(R.drawable.file);
            try {
                //?? TODO RemoteID is not set yet. How to get thumbnail?
                Bitmap b = ThumbnailsCacheManager.getBitmapFromDiskCache(uploadObject.getOCFile().getRemoteId());
                if (b != null) {
                    fileIcon.setImageBitmap(b);
                }
            } catch (NullPointerException e) {
            }            

            TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
            CharSequence dateString = DisplayUtils.getRelativeDateTimeString(mActivity, uploadObject.getUploadTime()
                    .getTimeInMillis(), DateUtils.SECOND_IN_MILLIS, DateUtils.WEEK_IN_MILLIS, 0);
            fileSizeV.setText(dateString);
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
    public boolean hasStableIds() {
        return false;
    }

    

    /**
     * Load upload items from {@link UploadDbHandler}.
     */
    private void loadUploadItemsFromDb() {
        Log_OC.d(TAG, "loadUploadItemsFromDb");
        
        for (UploadGroup group : mUploadGroups) {
            group.refresh();
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

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return mUploadGroups[groupPosition].items[childPosition];
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
            ViewGroup parent) {
        return getView(mUploadGroups[groupPosition].items, childPosition, convertView, parent);
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return mUploadGroups[groupPosition].items.length;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return mUploadGroups[groupPosition];
    }

    @Override
    public int getGroupCount() {
        return mUploadGroups.length;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        //force group to stay unfolded
        ExpandableListView listView = (ExpandableListView) parent;
        listView.expandGroup(groupPosition);
        
        listView.setGroupIndicator(null);
        
        UploadGroup group = (UploadGroup) getGroup(groupPosition);
        if (convertView == null) {
            LayoutInflater infalInflater = (LayoutInflater) mActivity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = infalInflater.inflate(R.layout.upload_list_group, null);
        }
        TextView tv = (TextView) convertView.findViewById(R.id.uploadListGroupName);
        tv.setText(group.getGroupName());
        ImageView icon = (ImageView) convertView.findViewById(R.id.uploadListGroupIcon);
        icon.setImageResource(group.getGroupIcon());
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public void onTransferProgress(long progressRate, long totalTransferredSoFar, long totalToTransfer,
            String fileAbsoluteName) {
        // TODO Auto-generated method stub
        
    }
}
