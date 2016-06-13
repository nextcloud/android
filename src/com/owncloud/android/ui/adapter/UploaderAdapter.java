/**
 *   ownCloud Android client application
 *
 *   @author Tobias Kaminsky
 *   Copyright (C) 2016 ownCloud Inc.
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

import android.accounts.Account;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.datamodel.ThumbnailsCacheManager.AsyncDrawable;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimetypeIconUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploaderAdapter extends SimpleAdapter {
    
    private Context mContext;
    private Account mAccount;
    private FileDataStorageManager mStorageManager;
    private LayoutInflater inflater;

    public UploaderAdapter(Context context,
                           List<? extends Map<String, ?>> data, int resource, String[] from,
                           int[] to, FileDataStorageManager storageManager, Account account) {
        super(context, data, resource, from, to);
        mAccount = account;
        mStorageManager = storageManager;
        mContext = context;
        inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View vi = convertView;
        if (convertView == null)
            vi = inflater.inflate(R.layout.uploader_list_item_layout, null);

        HashMap<String, OCFile> data = (HashMap<String, OCFile>) getItem(position);
        OCFile file = data.get("dirname");

        TextView filename = (TextView) vi.findViewById(R.id.filename);
        filename.setText(file.getFileName());
        
        ImageView fileIcon = (ImageView) vi.findViewById(R.id.thumbnail);
        fileIcon.setTag(file.getFileId());

        TextView lastModV = (TextView) vi.findViewById(R.id.last_mod);
        lastModV.setText(DisplayUtils.getRelativeTimestamp(mContext, file.getModificationTimestamp()));

        TextView fileSizeV = (TextView) vi.findViewById(R.id.file_size);
        TextView fileSizeSeparatorV = (TextView) vi.findViewById(R.id.file_separator);

        if(!file.isFolder()) {
            fileSizeV.setVisibility(View.VISIBLE);
            fileSizeSeparatorV.setVisibility(View.VISIBLE);
            fileSizeV.setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
        } else {
            fileSizeV.setVisibility(View.GONE);
            fileSizeSeparatorV.setVisibility(View.GONE);
        }
        
        // get Thumbnail if file is image
        if (file.isImage() && file.getRemoteId() != null){
             // Thumbnail in Cache?
            Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    String.valueOf(file.getRemoteId())
            );
            if (thumbnail != null && !file.needsUpdateThumbnail()){
                fileIcon.setImageBitmap(thumbnail);
            } else {
                // generate new Thumbnail
                if (ThumbnailsCacheManager.cancelPotentialWork(file, fileIcon)) {
                    final ThumbnailsCacheManager.ThumbnailGenerationTask task = 
                            new ThumbnailsCacheManager.ThumbnailGenerationTask(fileIcon, mStorageManager, 
                                    mAccount);
                    if (thumbnail == null) {
                        thumbnail = ThumbnailsCacheManager.mDefaultImg;
                    }
                    final AsyncDrawable asyncDrawable = new AsyncDrawable(
                            mContext.getResources(), 
                            thumbnail, 
                            task
                    );
                    fileIcon.setImageDrawable(asyncDrawable);
                    task.execute(file);
                }
            }
        } else {
            fileIcon.setImageResource(
                    MimetypeIconUtil.getFileTypeIconId(file.getMimetype(), file.getFileName())
            );
        }
        return vi;
    }


}
