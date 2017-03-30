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
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;
import com.owncloud.android.R;
import com.owncloud.android.lib.resources.activities.models.Activity;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.svg.SvgDecoder;
import com.owncloud.android.utils.svg.SvgDrawableTranscoder;
import com.owncloud.android.utils.svg.SvgSoftwareLayerSetter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the activity view
 */

public class ActivityListAdapter extends RecyclerView.Adapter<ActivityListAdapter.ActivityViewHolder> {

    private Context context;
    private List<Object> mValues;

    public ActivityListAdapter(Context context) {
        this.mValues = new ArrayList<>();
        this.context = context;
    }

    public void setActivityItems(List<Object> activityItems) {
        mValues.clear();
        mValues.addAll(activityItems);
        notifyDataSetChanged();
    }

    @Override
    public ActivityViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);
        return new ActivityViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ActivityViewHolder holder, int position) {
        Activity activity = (Activity) mValues.get(position);
        if (activity.getDatetime() != null) {
            holder.dateTime.setText(DisplayUtils.getRelativeTimestamp(context,
                    activity.getDatetime().getTime()));
        } else {
            holder.dateTime.setText(DisplayUtils.getRelativeTimestamp(context,
                    activity.getDate().getTime()));
        }

        if (!TextUtils.isEmpty(activity.getSubject())) {
            holder.subject.setText(activity.getSubject());
            holder.subject.setVisibility(View.VISIBLE);
        } else {
            holder.subject.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(activity.getMessage())) {
            holder.message.setText(activity.getMessage());
            holder.message.setVisibility(View.VISIBLE);
        } else {
            holder.message.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(activity.getIcon())) {
            downloadIcon(activity.getIcon(), holder.activityIcon);
        }
    }

    private void downloadIcon(String icon, ImageView itemViewType) {
        GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder = Glide.with(context)
                .using(Glide.buildStreamModelLoader(Uri.class, context), InputStream.class)
                .from(Uri.class)
                .as(SVG.class)
                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                .sourceEncoder(new StreamEncoder())
                .cacheDecoder(new FileToStreamDecoder<>(new SvgDecoder()))
                .decoder(new SvgDecoder())
                .placeholder(R.drawable.ic_activity)
                .error(R.drawable.ic_activity)
                .animate(android.R.anim.fade_in)
                .listener(new SvgSoftwareLayerSetter<Uri>());


        Uri uri = Uri.parse(icon);
        requestBuilder
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .load(uri)
                .into(itemViewType);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    class ActivityViewHolder extends RecyclerView.ViewHolder {

        private final ImageView activityIcon;
        private final TextView subject;
        private final TextView message;
        private final TextView dateTime;

        private ActivityViewHolder(View itemView) {
            super(itemView);
            activityIcon = (ImageView) itemView.findViewById(R.id.activity_icon);
            subject = (TextView) itemView.findViewById(R.id.activity_subject);
            message = (TextView) itemView.findViewById(R.id.activity_message);
            dateTime = (TextView) itemView.findViewById(R.id.activity_datetime);
        }
    }
}
