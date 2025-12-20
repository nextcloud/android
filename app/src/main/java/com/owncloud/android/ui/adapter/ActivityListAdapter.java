/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019 Alex Plutta <alex.plutta@googlemail.com>
 * SPDX-FileCopyrightText: 2018 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Alejandro Morales <aleister09@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Handler;
import android.os.Looper;
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

import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.client.network.ClientFactory;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.utils.GlideHelper;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.databinding.ActivityListItemBinding;
import com.owncloud.android.databinding.ActivityListItemHeaderBinding;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.activities.model.Activity;
import com.owncloud.android.lib.resources.activities.model.RichElement;
import com.owncloud.android.lib.resources.activities.model.RichObject;
import com.owncloud.android.lib.resources.activities.models.PreviewObject;
import com.owncloud.android.ui.interfaces.ActivityListInterface;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.MimeTypeUtil;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    private ViewThemeUtils viewThemeUtils;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ActivityListAdapter(
        Context context,
        CurrentAccountProvider currentAccountProvider,
        ActivityListInterface activityListInterface,
        ClientFactory clientFactory,
        boolean isDetailView,
        ViewThemeUtils viewThemeUtils) {
        this.values = new ArrayList<>();
        this.context = context;
        this.currentAccountProvider = currentAccountProvider;
        this.activityListInterface = activityListInterface;
        this.clientFactory = clientFactory;
        px = getThumbnailDimension();
        this.isDetailView = isDetailView;
        this.viewThemeUtils = viewThemeUtils;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setActivityItems(List<Object> activityItems, NextcloudClient client, boolean clear) {
        this.client = client;
        String sTime = "";

        if (clear) {
            values.clear();
        }

        for (Object o : activityItems) {
            Activity activity = (Activity) o;
            String time;
            time = getHeaderDateString(context, activity.getDatetime().getTime()).toString();

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
        if (holder instanceof ActivityViewHolder activityViewHolder) {
            Activity activity = (Activity) values.get(position);
            activityViewHolder.binding.datetime.setVisibility(View.VISIBLE);
            activityViewHolder.binding.datetime.setText(DateFormat.format("HH:mm", activity.getDatetime().getTime()));

            if (!TextUtils.isEmpty(activity.getRichSubjectElement().getRichSubject())) {
                activityViewHolder.binding.subject.setVisibility(View.VISIBLE);
                activityViewHolder.binding.subject.setMovementMethod(LinkMovementMethod.getInstance());
                activityViewHolder.binding.subject.setText(addClickablePart(activity.getRichSubjectElement()),
                                                           TextView.BufferType.SPANNABLE);
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
                new Thread(() -> {{
                    try {
                        final var client = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(Optional.of(currentAccountProvider.getUser().toOwnCloudAccount()).get(), context);
                        handler.post(() -> GlideHelper.INSTANCE.loadIntoImageView(context, client, activity.getIcon(), activityViewHolder.binding.icon, R.drawable.ic_activity, false));
                    } catch (Exception e) {
                        Log_OC.e("TemplateAdapter", "Exception setData: " + e);
                    }
                }}).start();
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


            if (!activity.getRichSubjectElement().getRichObjectList().isEmpty()) {
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

            if (previewObject.getSource() != null) {
                new Thread(() -> {{
                    try {
                        final var client = OwnCloudClientManagerFactory.getDefaultSingleton().getNextcloudClientFor(Optional.of(currentAccountProvider.getUser().toOwnCloudAccount()).get(), context);
                        handler.post(() -> GlideHelper.INSTANCE.loadIntoImageView(context, client, previewObject.getSource(), imageView, placeholder, false));
                    } catch (Exception e) {
                        Log_OC.e("TemplateAdapter", "Exception setData: " + e);
                    }
                }}).start();
            }
        } else {
            if (MimeTypeUtil.isFolder(previewObject.getMimeType())) {
                imageView.setImageDrawable(MimeTypeUtil.getDefaultFolderIcon(context, viewThemeUtils));
            } else {
                imageView.setImageDrawable(MimeTypeUtil.getFileTypeIcon(previewObject.getMimeType(),
                                                                        "",
                                                                        context,
                                                                        viewThemeUtils));
            }
        }

        return imageView;
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
        double d = Math.pow(2, Math.floor(Math.log(r.getDimension(R.dimen.file_icon_size_grid)) / Math.log(2))) / 2;
        return (int) d;
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
