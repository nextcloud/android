/**
 * Nextcloud Android client application
 *
 * @author Alejandro Bautista
 * Copyright (C) 2017 Alejandro Bautista
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.resources.activities.models.RichObject;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.glide.CustomGlideStreamLoader;

import java.util.ArrayList;

/**
 * Rich Object Adapter
 */

public class RichObjectAdapter extends RecyclerView.Adapter<RichObjectAdapter.RichObjectViewHolder> {

    private static final String TAG = RichObjectAdapter.class.getName();
    private final Context mContext;
    private final int px;
    private OwnCloudClient mClient;
    private ArrayList<RichObject> mValues;
    private final ActivityListInterface mActivityListInterface;
    private Uri mBaseUri;


    public RichObjectAdapter(Context context, ActivityListInterface activityListInterface) {
        this.mContext = context;
        this.mValues = new ArrayList<>();
        this.mActivityListInterface = activityListInterface;
        px=getThumbnailDimension();


    }


    public void setValues(ArrayList<RichObject> values,Uri baseUri) {
        this.mBaseUri=baseUri;
        this.mValues = values;
        notifyDataSetChanged();
    }

    @Override
    public RichObjectViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.rich_object_item, parent, false);
        return new RichObjectViewHolder(v);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    @Override
    public void onBindViewHolder(RichObjectViewHolder holder, int position) {
        final RichObject richObject = mValues.get(position);


        OCFile file = new OCFile("/" + richObject.getPath());
        file.setRemoteId(richObject.getId());
        holder.image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityListInterface.onActivityClicked(richObject);
            }
        });
        setBitmap(file, holder.image);
    }


    public void setBitmap(OCFile file, ImageView fileIcon) {
        // No Folder
        if (!file.isFolder()) {
            if ((MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file)) ) {
                String uri = mBaseUri+ "" +
                        "/index.php/apps/files/api/v1/thumbnail/" +
                        px + "/" + px + Uri.encode(file.getRemotePath(), "/");

                Glide.with(mContext).using(new CustomGlideStreamLoader()).load(uri).into(fileIcon); //Using custom fetcher

            } else {
                fileIcon.setImageResource(MimeTypeUtil.getFileTypeIconId(file.getMimetype(),
                        file.getFileName()));
            }
        } else {
            // Folder
            fileIcon.setImageResource(
                    MimeTypeUtil.getFolderTypeIconId(
                            file.isSharedWithMe() || file.isSharedWithSharee(),
                            file.isSharedViaLink()
                    )
            );
        }
    }


    static class RichObjectViewHolder extends RecyclerView.ViewHolder {

        ImageView image;

        RichObjectViewHolder(View v) {
            super(v);
            image = (ImageView) v.findViewById(R.id.image);
        }

    }

    /**
     * Converts size of file icon from dp to pixel
     * @return int
     */
    private int getThumbnailDimension(){
        // Converts dp to pixel
        Resources r = MainApp.getAppContext().getResources();
        Double d = Math.pow(2,Math.floor(Math.log(r.getDimension(R.dimen.file_icon_size_grid))/Math.log(2)));
        return d.intValue();
    }

}
