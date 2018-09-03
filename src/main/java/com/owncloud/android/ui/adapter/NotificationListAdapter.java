/*
 * ownCloud Android client application
 *
 * @author Andy Scherzinger
 * Copyright (C) 2016 ownCloud Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.adapter;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.PictureDrawable;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
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
import com.google.android.material.button.MaterialButton;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.resources.notifications.models.Action;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.lib.resources.notifications.models.RichObject;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.asynctasks.DeleteNotificationTask;
import com.owncloud.android.ui.asynctasks.NotificationExecuteActionTask;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;
import com.owncloud.android.utils.svg.SvgDecoder;
import com.owncloud.android.utils.svg.SvgDrawableTranscoder;
import com.owncloud.android.utils.svg.SvgSoftwareLayerSetter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * This Adapter populates a RecyclerView with all notifications for an account within the app.
 */
public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.NotificationViewHolder> {
    private static final String FILE = "file";
    private StyleSpan styleSpanBold = new StyleSpan(Typeface.BOLD);
    private ForegroundColorSpan foregroundColorSpanBlack;

    private List<Notification> notificationsList;
    private OwnCloudClient client;
    private NotificationsActivity notificationsActivity;

    public NotificationListAdapter(OwnCloudClient client, NotificationsActivity notificationsActivity) {
        this.notificationsList = new ArrayList<>();
        this.client = client;
        this.notificationsActivity = notificationsActivity;
        foregroundColorSpanBlack = new ForegroundColorSpan(notificationsActivity.getResources().getColor(R.color.textColor));
    }

    public void setNotificationItems(List<Notification> notificationItems) {
        notificationsList.clear();
        notificationsList.addAll(notificationItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(notificationsActivity).inflate(R.layout.notification_list_item, parent, false);
        return new NotificationViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationsList.get(position);
        holder.dateTime.setText(DisplayUtils.getRelativeTimestamp(notificationsActivity,
                notification.getDatetime().getTime()));

        RichObject file = notification.subjectRichParameters.get(FILE);
        String subject = notification.getSubject();
        if (file == null && !TextUtils.isEmpty(notification.getLink())) {
            subject = subject + " ↗";
            holder.subject.setTypeface(holder.subject.getTypeface(), Typeface.BOLD);
            holder.subject.setOnClickListener(v -> openLink(notification.getLink()));
            holder.subject.setText(subject);
        } else {
            if (!TextUtils.isEmpty(notification.subjectRich)) {
                holder.subject.setText(makeSpecialPartsBold(notification));
            } else {
                holder.subject.setText(subject);
            }

            if (file != null && !TextUtils.isEmpty(file.id)) {
                holder.subject.setOnClickListener(v -> {
                    Intent intent = new Intent(notificationsActivity, FileDisplayActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(FileDisplayActivity.KEY_FILE_ID, file.id);

                    notificationsActivity.startActivity(intent);
                });
            }
        }

        holder.message.setText(notification.getMessage());

        // Todo set proper action icon (to be clarified how to pick)
        if (!TextUtils.isEmpty(notification.getIcon())) {
            downloadIcon(notification.getIcon(), holder.icon);
        }

        // add action buttons
        holder.buttons.removeAllViews();
        MaterialButton button;

        Resources resources = notificationsActivity.getResources();
        NotificationExecuteActionTask task = new NotificationExecuteActionTask(client, holder, notificationsActivity);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(20, 0, 20, 0);

        for (Action action : notification.getActions()) {
            button = new MaterialButton(notificationsActivity);

            int primaryColor = ThemeUtils.primaryColor(notificationsActivity);

            if (action.primary) {
                button.getBackground().setColorFilter(primaryColor, PorterDuff.Mode.SRC_ATOP);
                button.setTextColor(ThemeUtils.fontColor(notificationsActivity));
                button.setTypeface(button.getTypeface(), Typeface.BOLD);
            } else {
                button.setStrokeColor(ColorStateList.valueOf(resources.getColor(R.color.grey_200)));
                button.setStrokeWidth(3);

                button.getBackground().setColorFilter(resources.getColor(R.color.transparent),
                                                      PorterDuff.Mode.SRC_ATOP);
                button.setTextColor(primaryColor);
                button.setTypeface(button.getTypeface(), Typeface.BOLD);
            }

            button.setText(action.label);
            button.setCornerRadiusResource(R.dimen.button_corner_radius);

            button.setLayoutParams(params);
            button.setGravity(Gravity.CENTER);

            button.setPadding(40, 40, 40, 40);

            button.setOnClickListener(v -> {
                setButtonEnabled(holder, false);
                task.execute(action);
            });

            holder.buttons.addView(button);
        }

        holder.dismiss.setOnClickListener(v -> new DeleteNotificationTask(client, notification, holder,
                                                                          notificationsActivity).execute());
    }

    private SpannableStringBuilder makeSpecialPartsBold(Notification notification) {
        String text = notification.getSubjectRich();
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);

        int openingBrace = text.indexOf('{');
        int closingBrace;
        String replaceablePart;
        while (openingBrace != -1) {
            closingBrace = text.indexOf('}', openingBrace) + 1;
            replaceablePart = text.substring(openingBrace + 1, closingBrace - 1);

            RichObject richObject = notification.subjectRichParameters.get(replaceablePart);
            if (richObject != null) {
                String name = richObject.getName();
                ssb.replace(openingBrace, closingBrace, name);
                text = ssb.toString();
                closingBrace = openingBrace + name.length();

                ssb.setSpan(styleSpanBold, openingBrace, closingBrace, 0);
                ssb.setSpan(foregroundColorSpanBlack, openingBrace, closingBrace,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
            openingBrace = text.indexOf('{', closingBrace);
        }

        return ssb;
    }

    public void removeNotification(NotificationViewHolder holder) {
        int position = holder.getAdapterPosition();

        if (position >= 0 && position < notificationsList.size()) {
            notificationsList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, notificationsList.size());
        }
    }

    public void removeAllNotifications() {
        notificationsList.clear();
        notifyDataSetChanged();
    }


    public void setButtonEnabled(NotificationViewHolder holder, boolean enabled) {
        for (int i = 0; i < holder.buttons.getChildCount(); i++) {
            holder.buttons.getChildAt(i).setEnabled(enabled);
        }
    }

    private void downloadIcon(String icon, ImageView itemViewType) {
        GenericRequestBuilder<Uri, InputStream, SVG, PictureDrawable> requestBuilder = Glide.with(notificationsActivity)
                .using(Glide.buildStreamModelLoader(Uri.class, notificationsActivity), InputStream.class)
                .from(Uri.class)
                .as(SVG.class)
                .transcode(new SvgDrawableTranscoder(), PictureDrawable.class)
                .sourceEncoder(new StreamEncoder())
                .cacheDecoder(new FileToStreamDecoder<>(new SvgDecoder()))
                .decoder(new SvgDecoder())
                .placeholder(R.drawable.ic_notification)
                .error(R.drawable.ic_notification)
                .animate(android.R.anim.fade_in)
                .listener(new SvgSoftwareLayerSetter<>());


        Uri uri = Uri.parse(icon);
        requestBuilder
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .load(uri)
                .into(itemViewType);
    }

    private void openLink(String link) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));

        DisplayUtils.startIntentIfAppAvailable(intent, notificationsActivity, R.string.no_browser_available);
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.notification_icon)
        public ImageView icon;
        @BindView(R.id.notification_subject)
        public TextView subject;
        @BindView(R.id.notification_message)
        public TextView message;
        @BindView(R.id.notification_datetime)
        public TextView dateTime;
        @BindView(R.id.notification_buttons)
        public LinearLayout buttons;
        @BindView(R.id.notification_dismiss)
        public ImageView dismiss;

        private NotificationViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
