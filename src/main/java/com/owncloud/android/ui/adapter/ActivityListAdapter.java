package com.owncloud.android.ui.adapter;

import android.content.Context;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
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
import com.owncloud.android.utils.SvgDecoder;
import com.owncloud.android.utils.SvgDrawableTranscoder;
import com.owncloud.android.utils.SvgSoftwareLayerSetter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alejandro on 28/03/17.
 */

public class ActivityListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

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
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);
        return new ActivityViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Activity activity =(Activity) mValues.get(position);
        ((ActivityViewHolder)holder).dateTime.setText(DisplayUtils.getRelativeTimestamp(context, activity.getDatetime().getTime()));
        ((ActivityViewHolder)holder).subject.setText(activity.getSubject());
        ((ActivityViewHolder)holder).message.setText(activity.getMessage());

        // Todo set proper action icon (to be clarified how to pick)

        downloadIcon(activity.getIcon(),((ActivityViewHolder)holder).activityIcon);
        //((ActivityViewHolder)holder).activityIcon.setImageResource(R.drawable.ic_action_share);
    }

    private void downloadIcon(String icon, ImageView itemViewType) {
        GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder = Glide.with(context)
                .using(Glide.buildStreamModelLoader(Uri.class, context), InputStream.class)
                .from(Uri.class)
                .as(SVG.class)
                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                .sourceEncoder(new StreamEncoder())
                .cacheDecoder(new FileToStreamDecoder<SVG>(new SvgDecoder()))
                .decoder(new SvgDecoder())
                .placeholder(R.drawable.ic_menu_archive)
                .error(R.drawable.ic_web)
                .animate(android.R.anim.fade_in)
                .listener(new SvgSoftwareLayerSetter<Uri>());


        Uri uri = Uri.parse(icon);
        requestBuilder
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                // SVG cannot be serialized so it's not worth to cache it
                .load(uri)
                .into(itemViewType);
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }


    class HeaderViewHolder extends RecyclerView.ViewHolder {

        public HeaderViewHolder(View v) {
            super(v);
        }

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
