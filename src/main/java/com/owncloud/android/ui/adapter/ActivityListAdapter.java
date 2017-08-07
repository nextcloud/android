/*
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
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.models.Activity;
import com.owncloud.android.lib.resources.activities.models.RichElement;
import com.owncloud.android.lib.resources.activities.models.RichObject;
import com.owncloud.android.lib.resources.files.FileUtils;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.ThemeUtils;
import com.owncloud.android.utils.glide.CustomGlideStreamLoader;
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

    public static final int HEADER_TYPE = 100;
    public static final int ACTIVITY_TYPE = 101;
    private final ActivityListInterface activityListInterface;
    private final int px;
    private static final String TAG = ActivityListAdapter.class.getSimpleName();
    private OwnCloudClient mClient;

    private Context context;
    private FileDataStorageManager storageManager;
    private List<Object> mValues;

    public ActivityListAdapter(Context context, ActivityListInterface activityListInterface,
                               FileDataStorageManager storageManager) {
        this.mValues = new ArrayList<>();
        this.context = context;
        this.activityListInterface = activityListInterface;
        this.storageManager = storageManager;
        px = getThumbnailDimension();
    }

    public void setActivityItems(List<Object> activityItems, OwnCloudClient client) {
        this.mClient = client;
        mValues.clear();
        String sTime = "";
        for (Object o : activityItems) {
            Activity activity = (Activity) o;
            String time = null;
            if (activity.getDatetime() != null) {
                time = DisplayUtils.getRelativeTimestamp(context,
                        activity.getDatetime().getTime()).toString();
            } else if (activity.getDate() != null) {
                time = DisplayUtils.getRelativeTimestamp(context,
                        activity.getDate().getTime()).toString();
            } else {
                time = "Unknown";
            }

            if (sTime.equalsIgnoreCase(time)) {
                mValues.add(activity);
            } else {

                sTime = time;
                mValues.add(sTime);
                mValues.add(activity);
            }
        }
        notifyDataSetChanged();

    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == ACTIVITY_TYPE) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item, parent, false);
            return new ActivityViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_list_item_header, parent, false);
            return new ActivityViewHeaderHolder(v);
        }

    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (holder instanceof ActivityViewHolder) {
            final ActivityViewHolder activityViewHolder = (ActivityViewHolder) holder;
            Activity activity = (Activity) mValues.get(position);
            if (activity.getDatetime() != null) {
                activityViewHolder.dateTime.setText(DisplayUtils.getRelativeTimestamp(context,
                        activity.getDatetime().getTime()));
            } else {
                activityViewHolder.dateTime.setText(DisplayUtils.getRelativeTimestamp(context,
                        new Date().getTime()));
            }

            if (activity.getRichSubjectElement() != null &&
                    !TextUtils.isEmpty(activity.getRichSubjectElement().getRichSubject())) {
                activityViewHolder.subject.setVisibility(View.VISIBLE);
                activityViewHolder.subject.setMovementMethod(LinkMovementMethod.getInstance());
                activityViewHolder.subject.setText(addClickablePart(activity.getRichSubjectElement()),
                        TextView.BufferType.SPANNABLE);
                activityViewHolder.subject.setVisibility(View.VISIBLE);
            } else if (!TextUtils.isEmpty(activity.getSubject())) {
                activityViewHolder.subject.setVisibility(View.VISIBLE);
                activityViewHolder.subject.setText(activity.getSubject());
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

            if (activity.getRichSubjectElement() != null &&
                    activity.getRichSubjectElement().getRichObjectList().size() > 0) {

                activityViewHolder.list.setVisibility(View.VISIBLE);
                activityViewHolder.list.removeAllViews();

                activityViewHolder.list.post(new Runnable() {
                    @Override
                    public void run() {
                        int w = activityViewHolder.list.getMeasuredWidth();
                        int elPxSize = px + 20;
                        int totalColumnCount = (int) Math.floor(w / elPxSize);

                        try {
                            activityViewHolder.list.setColumnCount(totalColumnCount);
                        } catch (IllegalArgumentException e) {
                            Log_OC.e(TAG, "error setting column count to " + totalColumnCount);
                        }
                    }
                });


                for (RichObject richObject : activity.getRichSubjectElement().getRichObjectList()) {
                    ImageView imageView = createThumbnail(richObject);
                    activityViewHolder.list.addView(imageView);
                }

            } else {
                activityViewHolder.list.removeAllViews();
                activityViewHolder.list.setVisibility(View.GONE);
            }
        } else {
            ActivityViewHeaderHolder activityViewHeaderHolder = (ActivityViewHeaderHolder) holder;
            activityViewHeaderHolder.title.setText((String) mValues.get(position));
        }
    }

    private ImageView createThumbnail(final RichObject richObject) {
        String path = FileUtils.PATH_SEPARATOR + richObject.getPath();
        OCFile file = storageManager.getFileByPath(path);

        if (file == null) {
            file = storageManager.getFileByPath(path + FileUtils.PATH_SEPARATOR);
        }
        if (file == null) {
            file = new OCFile(path);
            file.setRemoteId(richObject.getId());
        }

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px, px);
        params.setMargins(10, 10, 10, 10);
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(params);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activityListInterface.onActivityClicked(richObject);
            }
        });
        setBitmap(file, imageView);

        return imageView;
    }

    private void setBitmap(OCFile file, ImageView fileIcon) {
        // No Folder
        if (!file.isFolder()) {
            if ((MimeTypeUtil.isImage(file) || MimeTypeUtil.isVideo(file))) {
                String uri = mClient.getBaseUri() + "" +
                        "/index.php/apps/files/api/v1/thumbnail/" +
                        px + "/" + px + Uri.encode(file.getRemotePath(), "/");

                Glide.with(context).using(new CustomGlideStreamLoader()).load(uri).into(fileIcon); //Using custom fetcher

            } else {
                fileIcon.setImageDrawable(MimeTypeUtil.getFileTypeIcon(file.getMimetype(), file.getFileName(), null));
            }
        } else {
            // Folder
            fileIcon.setImageDrawable(
                    MimeTypeUtil.getFolderTypeIcon(
                            file.isSharedWithMe() || file.isSharedWithSharee(),
                            file.isSharedViaLink()
                    )
            );
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

    private SpannableStringBuilder addClickablePart(RichElement richElement) {
        String text = richElement.getRichSubject();
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);

        int idx1 = text.indexOf("{");
        int idx2;
        while (idx1 != -1) {
            idx2 = text.indexOf("}", idx1) + 1;
            final String clickString = text.substring(idx1 + 1, idx2 - 1);
            final RichObject richObject = searchObjectByName(richElement.getRichObjectList(), clickString);
            if (richObject != null) {
                String name = richObject.getName();
                ssb.replace(idx1, idx2, name);
                text = ssb.toString();
                idx2 = idx1 + name.length();
                ssb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        activityListInterface.onActivityClicked(richObject);
                    }
                }, idx1, idx2, 0);
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), idx1, idx2, 0);
                ssb.setSpan(new ForegroundColorSpan(ThemeUtils.primaryAccentColor()), idx1, idx2,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            idx1 = text.indexOf("{", idx2);
        }

        return ssb;
    }

    public RichObject searchObjectByName(ArrayList<RichObject> richObjectList, String name) {
        for (RichObject richObject : richObjectList) {
            if (richObject.getTag().equalsIgnoreCase(name))
                return richObject;
        }
        return null;
    }


    @Override
    public int getItemViewType(int position) {
        if (mValues.get(position) instanceof Activity)
            return ACTIVITY_TYPE;
        else
            return HEADER_TYPE;
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    /**
     * Converts size of file icon from dp to pixel
     *
     * @return int
     */
    private int getThumbnailDimension() {
        // Converts dp to pixel
        Resources r = MainApp.getAppContext().getResources();
        Double d = Math.pow(2, Math.floor(Math.log(r.getDimension(R.dimen.file_icon_size_grid)) / Math.log(2))) / 2;
        return d.intValue();
    }

    private class ActivityViewHolder extends RecyclerView.ViewHolder {

        private final ImageView activityIcon;
        private final TextView subject;
        private final TextView message;
        private final TextView dateTime;
        private final GridLayout list;

        private ActivityViewHolder(View itemView) {
            super(itemView);
            activityIcon = (ImageView) itemView.findViewById(R.id.activity_icon);
            subject = (TextView) itemView.findViewById(R.id.activity_subject);
            message = (TextView) itemView.findViewById(R.id.activity_message);
            dateTime = (TextView) itemView.findViewById(R.id.activity_datetime);
            list = (GridLayout) itemView.findViewById(R.id.list);
        }
    }

    private class ActivityViewHeaderHolder extends RecyclerView.ViewHolder {

        private final TextView title;

        private ActivityViewHeaderHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.title_header);

        }
    }

}
