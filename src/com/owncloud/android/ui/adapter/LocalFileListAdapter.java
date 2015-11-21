/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.owncloud.android.ui.adapter;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.BitmapUtils;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

/**
 * This Adapter populates a ListView with all files and directories contained
 * in a local directory
 */
public class LocalFileListAdapter extends BaseAdapter implements ListAdapter {

    private static final String TAG = LocalFileListAdapter.class.getSimpleName();

    private Context mContext;
    private File mDirectory;
    private File[] mFiles = null;
    
    public LocalFileListAdapter(File directory, Context context) {
        mContext = context;
        swapDirectory(directory);
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
        return mFiles != null ? mFiles.length : 0;
    }

    @Override
    public Object getItem(int position) {
        if (mFiles == null || mFiles.length <= position)
            return null;
        return mFiles[position];
    }

    @Override
    public long getItemId(int position) {
        return mFiles != null && mFiles.length <= position ? position : -1;
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
            view = inflator.inflate(R.layout.list_item, null);
        }
        if (mFiles != null && mFiles.length > position) {
            File file = mFiles[position];
            
            TextView fileName = (TextView) view.findViewById(R.id.Filename);
            String name = file.getName();
            fileName.setText(name);
            
            ImageView fileIcon = (ImageView) view.findViewById(R.id.thumbnail);

            /** Cancellation needs do be checked and done before changing the drawable in fileIcon, or
             * {@link ThumbnailsCacheManager#cancelPotentialWork} will NEVER cancel any task.
             **/
            boolean allowedToCreateNewThumbnail = (ThumbnailsCacheManager.cancelPotentialWork(file, fileIcon));

            if (!file.isDirectory()) {
                fileIcon.setImageResource(R.drawable.file);
            } else {
                fileIcon.setImageResource(R.drawable.ic_menu_archive);
            }
            fileIcon.setTag(file.hashCode());

            TextView fileSizeV = (TextView) view.findViewById(R.id.file_size);
            TextView lastModV = (TextView) view.findViewById(R.id.last_mod);
            ImageView checkBoxV = (ImageView) view.findViewById(R.id.custom_checkbox);
            if (!file.isDirectory()) {
                fileSizeV.setVisibility(View.VISIBLE);
                fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.length()));

                lastModV.setVisibility(View.VISIBLE);
                lastModV.setText(DisplayUtils.unixTimeToHumanReadable(file.lastModified()));
                ListView parentList = (ListView) parent;
                if (parentList.getChoiceMode() == ListView.CHOICE_MODE_NONE) { 
                    checkBoxV.setVisibility(View.GONE);
                } else {
                    if (parentList.isItemChecked(position)) {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_on_background);
                    } else {
                        checkBoxV.setImageResource(android.R.drawable.checkbox_off_background);
                    }
                    checkBoxV.setVisibility(View.VISIBLE);
                }
                
             // get Thumbnail if file is image
                if (BitmapUtils.isImage(file)){
                // Thumbnail in Cache?
                    Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                            String.valueOf(file.hashCode())
                    );
                    if (thumbnail != null){
                        fileIcon.setImageBitmap(thumbnail);
                    } else {

                        // generate new Thumbnail
                        if (allowedToCreateNewThumbnail) {
                            final ThumbnailsCacheManager.ThumbnailGenerationTask task =
                                    new ThumbnailsCacheManager.ThumbnailGenerationTask(fileIcon);
                            if (thumbnail == null) {
                                thumbnail = ThumbnailsCacheManager.mDefaultImg;
                            }
                            final ThumbnailsCacheManager.AsyncDrawable asyncDrawable =
                        		new ThumbnailsCacheManager.AsyncDrawable(
                                    mContext.getResources(), 
                                    thumbnail, 
                                    task
                		        );
                            fileIcon.setImageDrawable(asyncDrawable);
                            task.execute(file);
                            Log_OC.v(TAG, "Executing task to generate a new thumbnail");

                        } // else, already being generated, don't restart it
                    }
                } else {
                    fileIcon.setImageResource(MimetypeIconUtil.getFileTypeIconId(null, file.getName()));
                }  

            } else {
                fileSizeV.setVisibility(View.GONE);
                lastModV.setVisibility(View.GONE);
                checkBoxV.setVisibility(View.GONE);
            }

            // not GONE; the alignment changes; ugly way to keep it
            view.findViewById(R.id.localFileIndicator).setVisibility(View.INVISIBLE);   
            view.findViewById(R.id.favoriteIcon).setVisibility(View.GONE);
            
            view.findViewById(R.id.sharedIcon).setVisibility(View.GONE);
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
        return (mFiles == null || mFiles.length == 0);
    }

    /**
     * Change the adapted directory for a new one
     * @param directory     New file to adapt. Can be NULL, meaning "no content to adapt".
     */
    public void swapDirectory(File directory) {
        mDirectory = directory;
        mFiles = (mDirectory != null ? mDirectory.listFiles() : null);
        if (mFiles != null) {
            Arrays.sort(mFiles, new Comparator<File>() {
                @Override
                public int compare(File lhs, File rhs) {
                    if (lhs.isDirectory() && !rhs.isDirectory()) {
                        return -1;
                    } else if (!lhs.isDirectory() && rhs.isDirectory()) {
                        return 1;
                    }
                    return compareNames(lhs, rhs);
                }
            
                private int compareNames(File lhs, File rhs) {
                    return lhs.getName().toLowerCase().compareTo(rhs.getName().toLowerCase());                
                }
            
            });
        }
        notifyDataSetChanged();
    }
}
