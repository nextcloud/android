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
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.text.format.DateUtils;
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
import java.util.Date;
import java.util.List;

/**
 * Adapter for the activity view
 */

public class ActivityListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int HEADER_TYPE=100;
    public static final int ACTIVITY_TYPE=101;

    private Context context;
    private List<Object> mValues;

    public ActivityListAdapter(Context context) {
        this.mValues = new ArrayList<>();
        this.context = context;
    }

    public void setActivityItems(List<Object> activityItems) {
        mValues.clear();
        String sTime="";
        for (Object o : activityItems) {
            Activity activity = (Activity) o;
            String time=DisplayUtils.getRelativeTimestamp(context,
                    activity.getDatetime().getTime()).toString();
            if(sTime.equalsIgnoreCase(time)){
                mValues.add(activity);
            }else{

                sTime=time;
                mValues.add(sTime);
                mValues.add(activity);
            }
        }
        notifyDataSetChanged();

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType==ACTIVITY_TYPE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);
            return new ActivityViewHolder(v);
        }else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item_header, parent, false);
            return new ActivityViewHeaderHolder(v);
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if(holder instanceof ActivityViewHolder) {
            ActivityViewHolder activityViewHolder=(ActivityViewHolder) holder;
            Activity activity = (Activity) mValues.get(position);
            if (activity.getDatetime() != null) {
                activityViewHolder.dateTime.setText(DisplayUtils.getRelativeTimestamp(context,
                        activity.getDatetime().getTime()));
            } else {
                activityViewHolder.dateTime.setText(DisplayUtils.getRelativeTimestamp(context,
                        new Date().getTime()));
            }

            if (!TextUtils.isEmpty(activity.getSubject())) {
                activityViewHolder.subject.setText(activity.getSubject());
                activityViewHolder.subject.setVisibility(View.VISIBLE);
            } else {
                activityViewHolder.subject.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(activity.getMessage())) {
                activityViewHolder.message.setText(activity.getMessage());
                activityViewHolder.message.setVisibility(View.VISIBLE);
            } else {
                activityViewHolder.message.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(activity.getIcon())) {
                downloadIcon(activity.getIcon(), activityViewHolder.activityIcon);
            }

            ArrayList<String> richObjects=new ArrayList<>();
            richObjects.add("http://static3.businessinsider.com/image/55b675ab2acae7c7018ba34e-1200/milky-way-galaxy.jpg");
            richObjects.add("http://static3.businessinsider.com/image/55b675ab2acae7c7018ba34e-1200/milky-way-galaxy.jpg");
            richObjects.add("http://static3.businessinsider.com/image/55b675ab2acae7c7018ba34e-1200/milky-way-galaxy.jpg");
            richObjects.add("http://static3.businessinsider.com/image/55b675ab2acae7c7018ba34e-1200/milky-way-galaxy.jpg");
            richObjects.add("http://static3.businessinsider.com/image/55b675ab2acae7c7018ba34e-1200/milky-way-galaxy.jpg");

            RichObjectAdapter richObjectAdapter=new RichObjectAdapter(richObjects);
            activityViewHolder.list.setLayoutManager(new GridLayoutManager(context,4));
            activityViewHolder.list.setAdapter(richObjectAdapter);

        }else{
            ActivityViewHeaderHolder activityViewHeaderHolder=(ActivityViewHeaderHolder)holder;
            activityViewHeaderHolder.title.setText((String)mValues.get(position));
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
    public int getItemViewType(int position) {
        if(mValues.get(position) instanceof Activity)
            return ACTIVITY_TYPE;
        else
            return HEADER_TYPE;
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
        private final RecyclerView list;

        private ActivityViewHolder(View itemView) {
            super(itemView);
            activityIcon = (ImageView) itemView.findViewById(R.id.activity_icon);
            subject = (TextView) itemView.findViewById(R.id.activity_subject);
            message = (TextView) itemView.findViewById(R.id.activity_message);
            dateTime = (TextView) itemView.findViewById(R.id.activity_datetime);
            list = (RecyclerView) itemView.findViewById(R.id.list);
        }
    }

    class ActivityViewHeaderHolder extends RecyclerView.ViewHolder {

        private final TextView title;

        private ActivityViewHeaderHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title_header);

        }
    }

}
