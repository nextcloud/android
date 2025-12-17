/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2023 TSI-mc
 * SPDX-FileCopyrightText: 2022 Álvaro Brey <alvaro@alvarobrey.com>
 * SPDX-FileCopyrightText: 2018-2022 Tobias Kaminsky <tobias@kaminsky.me>
 * SPDX-FileCopyrightText: 2017 Andy Scherzinger <info@andy-scherzinger.de>
 * SPDX-FileCopyrightText: 2017 Mario Danic <mario@lovelyhq.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.owncloud.android.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
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
import android.widget.LinearLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipDrawable;
import com.nextcloud.android.common.ui.theme.utils.ColorRole;
import com.nextcloud.client.account.CurrentAccountProvider;
import com.nextcloud.common.NextcloudClient;
import com.nextcloud.utils.GlideHelper;
import com.nextcloud.utils.text.Spans;
import com.owncloud.android.R;
import com.owncloud.android.databinding.NotificationListItemBinding;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.models.Action;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.lib.resources.notifications.models.RichObject;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.asynctasks.DeleteNotificationTask;
import com.owncloud.android.ui.asynctasks.NotificationExecuteActionTask;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.theme.ViewThemeUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import thirdparties.fresco.BetterImageSpan;

/**
 * This Adapter populates a RecyclerView with all notifications for an account within the app.
 */
public class NotificationListAdapter extends RecyclerView.Adapter<NotificationListAdapter.NotificationViewHolder> 
    implements DisplayUtils.AvatarGenerationListener {
    private static final String FILE = "file";
    private static final String ACTION_TYPE_WEB = "WEB";
    private final StyleSpan styleSpanBold = new StyleSpan(Typeface.BOLD);
    private final ForegroundColorSpan foregroundColorSpanBlack;

    private final List<Notification> notificationsList;
    private final NextcloudClient client;
    private final NotificationsActivity notificationsActivity;
    private final ViewThemeUtils viewThemeUtils;
    private final CurrentAccountProvider currentAccountProvider;

    public NotificationListAdapter(NextcloudClient client,
                                   NotificationsActivity notificationsActivity,
                                   ViewThemeUtils viewThemeUtils, 
                                   CurrentAccountProvider currentAccountProvider) {
        this.notificationsList = new ArrayList<>();
        this.client = client;
        this.notificationsActivity = notificationsActivity;
        this.viewThemeUtils = viewThemeUtils;
        this.currentAccountProvider = currentAccountProvider;
        foregroundColorSpanBlack = new ForegroundColorSpan(
            notificationsActivity.getResources().getColor(R.color.text_color));
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setNotificationItems(List<Notification> notificationItems) {
        notificationsList.clear();
        notificationsList.addAll(notificationItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NotificationViewHolder(
            NotificationListItemBinding.inflate(LayoutInflater.from(notificationsActivity))
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationsList.get(position);
        holder.binding.datetime.setText(DisplayUtils.getRelativeTimestamp(notificationsActivity,
                                                                          notification.getDatetime().getTime()));

        RichObject file = notification.subjectRichParameters.get(FILE);
        String subject = notification.getSubject();
        if (file == null && !TextUtils.isEmpty(notification.getLink())) {
            subject = subject + " ↗";
            holder.binding.subject.setTypeface(holder.binding.subject.getTypeface(),
                                               Typeface.BOLD);
            holder.binding.subject.setOnClickListener(v -> DisplayUtils.startLinkIntent(notificationsActivity,
                                                                                        notification.getLink()));
            holder.binding.subject.setText(subject);
        } else {
            if (file != null && !TextUtils.isEmpty(file.id)) {
                holder.binding.subject.setOnClickListener(v -> {
                    Intent intent = new Intent(notificationsActivity, FileDisplayActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.putExtra(FileDisplayActivity.KEY_FILE_ID, file.id);

                    notificationsActivity.startActivity(intent);
                });
            }
        }

        if (TextUtils.isEmpty(notification.subjectRich)) {
            holder.binding.subject.setText(subject);
        } else {
            holder.binding.subject.setText(makeSpecialPartsBold(notification));
        }

        if (notification.getMessage() != null && !notification.getMessage().isEmpty()) {
            holder.binding.message.setText(notification.getMessage());
            holder.binding.message.setVisibility(View.VISIBLE);
        } else {
            holder.binding.message.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(notification.getIcon())) {
            new Thread(() -> {
                {
                    try {
                        notificationsActivity.runOnUiThread(() -> GlideHelper.INSTANCE
                            .loadIntoImageView(notificationsActivity,
                                               client,
                                               notification.getIcon(),
                                               holder.binding.icon,
                                               R.drawable.ic_notification,
                                               false));
                    } catch (Exception e) {
                        Log_OC.e("RichDocumentsTemplateAdapter", "Exception setData: " + e);
                    }
                }
            }).start();
        }

        viewThemeUtils.platform.colorImageView(holder.binding.icon, ColorRole.ON_SURFACE_VARIANT);
        viewThemeUtils.platform.colorImageView(holder.binding.dismiss, ColorRole.ON_SURFACE_VARIANT);
        viewThemeUtils.platform.colorTextView(holder.binding.subject, ColorRole.ON_SURFACE);
        viewThemeUtils.platform.colorTextView(holder.binding.message, ColorRole.ON_SURFACE_VARIANT);
        viewThemeUtils.platform.colorTextView(holder.binding.datetime, ColorRole.ON_SURFACE_VARIANT);

        setButtons(holder, notification);

        holder.binding.dismiss.setOnClickListener(v -> new DeleteNotificationTask(client,
                                                                                  notification,
                                                                                  holder,
                                                                                  notificationsActivity).execute());
    }

    public void setButtons(NotificationViewHolder holder, Notification notification) {
        // add action buttons
        holder.binding.buttons.removeAllViews();

        Resources resources = notificationsActivity.getResources();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                                                         LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(
            resources.getDimensionPixelOffset(R.dimen.standard_quarter_margin),
            0,
            resources.getDimensionPixelOffset(R.dimen.standard_half_margin),
            0);

        List<Action> overflowActions = new ArrayList<>();

        if (notification.getActions().size() > 0) {
            holder.binding.buttons.setVisibility(View.VISIBLE);
        } else {
            holder.binding.buttons.setVisibility(View.GONE);
        }

        if (notification.getActions().size() > 2) {
            for (Action action : notification.getActions()) {
                if (action.primary) {
                    final MaterialButton button = new MaterialButton(notificationsActivity);
                    button.setAllCaps(false);

                    button.setText(action.label);
                    button.setCornerRadiusResource(R.dimen.button_corner_radius);

                    button.setLayoutParams(params);
                    button.setGravity(Gravity.CENTER);

                    button.setOnClickListener(v -> {
                        setButtonEnabled(holder, false);

                        if (ACTION_TYPE_WEB.equals(action.type)) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(action.link));

                            notificationsActivity.startActivity(intent);
                        } else {
                            new NotificationExecuteActionTask(client,
                                                              holder,
                                                              notification,
                                                              notificationsActivity)
                                .execute(action);
                        }
                    });

                    viewThemeUtils.material.colorMaterialButtonPrimaryFilled(button);
                    holder.binding.buttons.addView(button);
                } else {
                    overflowActions.add(action);
                }
            }

            // further actions
            final MaterialButton moreButton = new MaterialButton(notificationsActivity);
            moreButton.setBackgroundColor(ResourcesCompat.getColor(resources,
                                                               android.R.color.transparent,
                                                               null));
            viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(moreButton);

            moreButton.setAllCaps(false);

            moreButton.setText(R.string.more);
            moreButton.setCornerRadiusResource(R.dimen.button_corner_radius);

            moreButton.setLayoutParams(params);
            moreButton.setGravity(Gravity.CENTER);

            moreButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(notificationsActivity, moreButton);

                for (Action action : overflowActions) {
                    popup.getMenu().add(action.label).setOnMenuItemClickListener(item -> {
                        setButtonEnabled(holder, false);

                        if (ACTION_TYPE_WEB.equals(action.type)) {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            intent.setData(Uri.parse(action.link));

                            notificationsActivity.startActivity(intent);
                        } else {
                            new NotificationExecuteActionTask(client,
                                                              holder,
                                                              notification,
                                                              notificationsActivity)
                                .execute(action);
                        }

                        return true;
                    });
                }

                popup.show();
            });

            holder.binding.buttons.addView(moreButton);
        } else {
            for (Action action : notification.getActions()) {
                final MaterialButton button = new MaterialButton(notificationsActivity);

                if (action.primary) {
                    viewThemeUtils.material.colorMaterialButtonPrimaryFilled(button);
                } else {
                    button.setBackgroundColor(ResourcesCompat.getColor(resources,
                                                                       android.R.color.transparent,
                                                                       null));
                    viewThemeUtils.material.colorMaterialButtonPrimaryBorderless(button);
                }

                button.setAllCaps(false);

                button.setText(action.label);
                button.setCornerRadiusResource(R.dimen.button_corner_radius);

                button.setLayoutParams(params);

                button.setOnClickListener(v -> {
                    setButtonEnabled(holder, false);

                    if (ACTION_TYPE_WEB.equals(action.type)) {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(action.link));

                        notificationsActivity.startActivity(intent);
                    } else {
                        new NotificationExecuteActionTask(client,
                                                          holder,
                                                          notification,
                                                          notificationsActivity)
                            .execute(action);
                    }
                });

                holder.binding.buttons.addView(button);
            }
        }
    }

    private SpannableStringBuilder makeSpecialPartsBold(Notification notification) {
        String text = notification.getSubjectRich();
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        Context context = notificationsActivity;

        int openingBrace = text.indexOf('{');
        int closingBrace;
        String replaceablePart;
        while (openingBrace != -1) {
            closingBrace = text.indexOf('}', openingBrace) + 1;
            replaceablePart = text.substring(openingBrace + 1, closingBrace - 1);

            RichObject richObject = notification.subjectRichParameters.get(replaceablePart);
            if (richObject != null) {
                if ("user".equals(richObject.getType())) {
                    String name = richObject.getName();

                    ChipDrawable drawableForChip = getDrawableForMentionChipSpan(R.xml.chip_others, name);

                    Spans.MentionChipSpan mentionChipSpan = new Spans.MentionChipSpan(drawableForChip,
                                                                                      BetterImageSpan.ALIGN_CENTER,
                                                                                      richObject.id,
                                                                                      name
                    );

                    DisplayUtils.setAvatar(
                        currentAccountProvider.getUser(),
                        richObject.id,
                        name,
                        this,
                        context.getResources().getDimension(R.dimen.standard_padding),
                        context.getResources(),
                        drawableForChip,
                        context
                                          );

                    ssb.setSpan(mentionChipSpan, openingBrace, closingBrace, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                } else {
                    String name = richObject.getName();
                    ssb.replace(openingBrace, closingBrace, name);
                    text = ssb.toString();
                    closingBrace = openingBrace + name.length();

                    ssb.setSpan(styleSpanBold, openingBrace, closingBrace, 0);
                    ssb.setSpan(foregroundColorSpanBlack, openingBrace, closingBrace, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            openingBrace = text.indexOf('{', closingBrace);
        }

        return ssb;
    }

    private ChipDrawable getDrawableForMentionChipSpan(int chipResource, String text) {
        ChipDrawable chip = ChipDrawable.createFromResource(notificationsActivity, chipResource);
        chip.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        chip.setLayoutDirection(notificationsActivity.getResources().getConfiguration().getLayoutDirection());
        chip.setText(text);
        chip.setChipIconResource(R.drawable.accent_circle);
        chip.setBounds(0, 0, chip.getIntrinsicWidth(), chip.getIntrinsicHeight());

        return chip;
    }

    public void removeNotification(NotificationViewHolder holder) {
        int position = holder.getAdapterPosition();

        if (position >= 0 && position < notificationsList.size()) {
            notificationsList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, notificationsList.size());
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void removeAllNotifications() {
        notificationsList.clear();
        notifyDataSetChanged();
    }


    public void setButtonEnabled(NotificationViewHolder holder, boolean enabled) {
        for (int i = 0; i < holder.binding.buttons.getChildCount(); i++) {
            holder.binding.buttons.getChildAt(i).setEnabled(enabled);
        }
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    @Override
    public void avatarGenerated(Drawable avatarDrawable, Object callContext) {
        ((ChipDrawable) callContext).setChipIcon(avatarDrawable);
    }

    @Override
    public boolean shouldCallGeneratedCallback(String tag, Object callContext) {
        return true;
    }

    public static class NotificationViewHolder extends RecyclerView.ViewHolder {
        NotificationListItemBinding binding;

        private NotificationViewHolder(NotificationListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
