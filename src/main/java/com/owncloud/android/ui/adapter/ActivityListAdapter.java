/*
 * Nextcloud Android client application
 *
 * @author Alejandro Bautista
 * @author Chris Narkiewicz
 * @author Andy Scherzinger
 *
 * Copyright (C) 2017 Alejandro Bautista
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * Copyright (C) 2020 Andy Scherzinger
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
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.GenericRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.model.StreamEncoder;
import com.bumptech.glide.load.resource.file.FileToStreamDecoder;
import com.caverock.androidsvg.SVG;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ActivityListItemBinding;
import com.owncloud.android.databinding.ActivityListItemHeaderBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.model.Activity;
import com.owncloud.android.lib.resources.activities.model.RichElement;
import com.owncloud.android.lib.resources.activities.model.RichObject;
import com.owncloud.android.lib.resources.activities.models.PreviewObject;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.glide.CustomGlideStreamLoader;
import com.owncloud.android.utils.svg.SvgBitmapTranscoder;
import com.owncloud.android.utils.svg.SvgDecoder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Adapter for the activity view.
 */
public class ActivityListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements StickyHeaderAdapter {

    static final int HEADER_TYPE = 100;
    static final int ACTIVITY_TYPE = 101;
    private final ActivityListInterface activityListInterface;
    private final int px;
    private static final String TAG = ActivityListAdapter.class.getSimpleName();
    protected NextcloudClient client;

    protected Context context;
    private CurrentAccountProvider currentAccountProvider;
    private ClientFactory clientFactory;
    protected List<Object> values;
    private boolean isDetailView;

    public ActivityListAdapter(
        Context context,
        CurrentAccountProvider currentAccountProvider,
        ActivityListInterface activityListInterface,
        ClientFactory clientFactory,
        boolean isDetailView) {
        this.values = new ArrayList<>();
        this.context = context;
        this.currentAccountProvider = currentAccountProvider;
        this.activityListInterface = activityListInterface;
        this.clientFactory = clientFactory;
        px = getThumbnailDimension();
        this.isDetailView = isDetailView;
    }

    public void setActivityItems(List<Object> activityItems, NextcloudClient client, boolean clear) {
        this.client = client;
        String sTime = "";

        if (clear) {
            values.clear();
        }

        for (Object o : activityItems) {
            Activity activity = (Activity) o;
            String time;
            if (activity.getDatetime() != null) {
                time = getHeaderDateString(context, activity.getDatetime().getTime()).toString();
            } else if (activity.getDate() != null) {
                time = getHeaderDateString(context, activity.getDate().getTime()).toString();
            } else {
                time = context.getString(R.string.date_unknown);
            }

            if (sTime.equalsIgnoreCase(time)) {
                values.add(activity);
            } else {
                sTime = time;
                values.add(sTime);
                values.add(activity);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ACTIVITY_TYPE) {
            return new ActivityViewHolder(
                ActivityListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
            );
        } else {
            return new ActivityViewHeaderHolder(
                ActivityListItemHeaderBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false)
            );
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ActivityViewHolder) {
            final ActivityViewHolder activityViewHolder = (ActivityViewHolder) holder;
            Activity activity = (Activity) values.get(position);
            if (activity.getDatetime() != null) {
                activityViewHolder.binding.datetime.setVisibility(View.VISIBLE);
                activityViewHolder.binding.datetime.setText(DateFormat.format("HH:mm", activity.getDatetime().getTime()));
            } else {
                activityViewHolder.binding.datetime.setVisibility(View.GONE);
            }

            if (activity.getRichSubjectElement() != null &&
                !TextUtils.isEmpty(activity.getRichSubjectElement().getRichSubject())) {
                activityViewHolder.binding.subject.setVisibility(View.VISIBLE);
                activityViewHolder.binding.subject.setMovementMethod(LinkMovementMethod.getInstance());
                activityViewHolder.binding.subject.setText(addClickablePart(activity.getRichSubjectElement()), TextView.BufferType.SPANNABLE);
                activityViewHolder.binding.subject.setVisibility(View.VISIBLE);
            } else if (!TextUtils.isEmpty(activity.getSubject())) {
                activityViewHolder.binding.subject.setVisibility(View.VISIBLE);
                activityViewHolder.binding.subject.setText(activity.getSubject());
            } else {
                activityViewHolder.binding.subject.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(activity.getMessage())) {
                activityViewHolder.binding.message.setText(activity.getMessage());
                activityViewHolder.binding.message.setVisibility(View.VISIBLE);
            } else {
                activityViewHolder.binding.message.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(activity.getIcon())) {
                downloadIcon(activity, activityViewHolder.binding.icon);
            }

            int nightModeFlag = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

            if (!"file_created".equalsIgnoreCase(activity.getType()) &&
                !"file_deleted".equalsIgnoreCase(activity.getType())) {
                if (Configuration.UI_MODE_NIGHT_YES == nightModeFlag) {
                    activityViewHolder.binding.icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
                } else {
                    activityViewHolder.binding.icon.setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
                }
            }


            if (activity.getRichSubjectElement() != null &&
                activity.getRichSubjectElement().getRichObjectList().size() > 0) {
                activityViewHolder.binding.list.setVisibility(View.VISIBLE);
                activityViewHolder.binding.list.removeAllViews();

                activityViewHolder.binding.list.post(() -> {
                    int w = activityViewHolder.binding.list.getMeasuredWidth();
                    int elPxSize = px + 20;
                    int totalColumnCount = w / elPxSize;

                    try {
                        activityViewHolder.binding.list.setColumnCount(totalColumnCount);
                    } catch (IllegalArgumentException e) {
                        Log_OC.e(TAG, "error setting column count to " + totalColumnCount);
                    }
                });

                for (PreviewObject previewObject : activity.getPreviews()) {
                    if (!isDetailView || MimeTypeUtil.isImageOrVideo(previewObject.getMimeType()) ||
                        MimeTypeUtil.isVideo(previewObject.getMimeType())) {
                        ImageView imageView = createThumbnailNew(previewObject,
                                                                 activity
                                                                     .getRichSubjectElement()
                                                                     .getRichObjectList());
                        activityViewHolder.binding.list.addView(imageView);
                    }
                }
            } else {
                activityViewHolder.binding.list.removeAllViews();
                activityViewHolder.binding.list.setVisibility(View.GONE);
            }
        } else {
            ActivityViewHeaderHolder activityViewHeaderHolder = (ActivityViewHeaderHolder) holder;
            activityViewHeaderHolder.binding.header.setText((String) values.get(position));
        }
    }

    private ImageView createThumbnailNew(PreviewObject previewObject, List<RichObject> richObjectList) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px, px);
        params.setMargins(10, 10, 10, 10);
        ImageView imageView = new ImageView(context);
        imageView.setLayoutParams(params);

        for (RichObject object : richObjectList) {
            int objectId = -1;
            try {
                objectId = Integer.parseInt(object.getId());
            } catch (NumberFormatException e) {
                // object.getId() can also be a string if RichObjects refers to an user
            }
            if (objectId == previewObject.getFileId()) {
                imageView.setOnClickListener(v -> activityListInterface.onActivityClicked(object));
                break;
            }
        }

        if (MimeTypeUtil.isImageOrVideo(previewObject.getMimeType())) {
            int placeholder;
            if (MimeTypeUtil.isImage(previewObject.getMimeType())) {
                placeholder = R.drawable.file_image;
            } else {
                placeholder = R.drawable.file_movie;
            }
            Glide.with(context).using(new CustomGlideStreamLoader(currentAccountProvider.getUser(), clientFactory))
                .load(previewObject.getSource())
                .placeholder(placeholder)
                .error(android.R.color.holo_red_light)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(imageView);
        } else {
            if (MimeTypeUtil.isFolder(previewObject.getMimeType())) {
                imageView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context));
            } else {
                imageView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(previewObject.getMimeType(), "", context));
            }
        }

        return imageView;
    }

    private void downloadIcon(Activity activity, ImageView itemViewType) {
        GenericRequestBuilder<Uri, InputStream, SVG, Bitmap> requestBuilder = Glide.with(context)
            .using(Glide.buildStreamModelLoader(Uri.class, context), InputStream.class)
            .from(Uri.class)
            .as(SVG.class)
            .transcode(new SvgBitmapTranscoder(128, 128), Bitmap.class)
            .sourceEncoder(new StreamEncoder())
            .cacheDecoder(new FileToStreamDecoder<>(new SvgDecoder()))
            .decoder(new SvgDecoder())
            .placeholder(R.drawable.ic_activity)
            .error(R.drawable.ic_activity)
            .animate(android.R.anim.fade_in);

        Uri uri = Uri.parse(activity.getIcon());
        requestBuilder
            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
            .load(uri)
            .into(itemViewType);
    }

    private SpannableStringBuilder addClickablePart(RichElement richElement) {
        String text = richElement.getRichSubject();
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);

        int idx1 = text.indexOf('{');
        int idx2;
        while (idx1 != -1) {
            idx2 = text.indexOf('}', idx1) + 1;
            final String clickString = text.substring(idx1 + 1, idx2 - 1);
            final RichObject richObject = searchObjectByName(richElement.getRichObjectList(), clickString);
            if (richObject != null) {
                String name = richObject.getName();
                ssb.replace(idx1, idx2, name);
                text = ssb.toString();
                idx2 = idx1 + name.length();
                ssb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        activityListInterface.onActivityClicked(richObject);
                    }

                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        ds.setUnderlineText(false);
                    }
                }, idx1, idx2, 0);
                ssb.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), idx1, idx2, 0);
                ssb.setSpan(
                    new ForegroundColorSpan(context.getResources().getColor(R.color.text_color)),
                    idx1,
                    idx2,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            idx1 = text.indexOf('{', idx2);
        }

        return ssb;
    }

    private RichObject searchObjectByName(List<RichObject> richObjectList, String name) {
        for (RichObject richObject : richObjectList) {
            if (richObject.getTag().equalsIgnoreCase(name)) {
                return richObject;
            }
        }
        return null;
    }


    @Override
    public int getItemViewType(int position) {
        if (values.get(position) instanceof Activity) {
            return ACTIVITY_TYPE;
        } else {
            return HEADER_TYPE;
        }
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
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

    CharSequence getHeaderDateString(Context context, long modificationTimestamp) {
        if ((System.currentTimeMillis() - modificationTimestamp) < DateUtils.WEEK_IN_MILLIS) {
            return DisplayUtils.getRelativeDateTimeString(context, modificationTimestamp, DateUtils.DAY_IN_MILLIS,
                                                          DateUtils.WEEK_IN_MILLIS, 0);
        } else {
            return DateFormat.format(DateFormat.getBestDateTimePattern(
                Locale.getDefault(), "EEEE, MMMM d"), modificationTimestamp);
        }
    }


    @Override
    public int getHeaderPositionForItem(int itemPosition) {
        int headerPosition = itemPosition;
        while (headerPosition >= 0) {
            if (this.isHeader(headerPosition)) {
                break;
            }
            headerPosition -= 1;
        }
        return headerPosition;
    }


    @Override
    public int getHeaderLayout(int headerPosition) {
        return R.layout.activity_list_item_header;
    }

    @Override
    public void bindHeaderData(View header, int headerPosition) {
        TextView textView = header.findViewById(R.id.header);
        String headline = (String) values.get(headerPosition);
        textView.setText(headline);
    }

    @Override
    public boolean isHeader(int itemPosition) {
        return this.getItemViewType(itemPosition) == HEADER_TYPE;
    }

    protected class ActivityViewHolder extends RecyclerView.ViewHolder {

        ActivityListItemBinding binding;

        ActivityViewHolder(ActivityListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    protected class ActivityViewHeaderHolder extends RecyclerView.ViewHolder {

        ActivityListItemHeaderBinding binding;

        ActivityViewHeaderHolder(ActivityListItemHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
