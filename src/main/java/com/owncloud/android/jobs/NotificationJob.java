/*
 * Nextcloud application
 *
 * @author Mario Danic
 * @author Chris Narkiewicz
 * Copyright (C) 2017-2018 Mario Danic <mario@lovelyhq.com>
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.owncloud.android.jobs;

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.gson.Gson;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.DecryptedPushMessage;
import com.owncloud.android.datamodel.SignatureVerification;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.DeleteNotificationRemoteOperation;
import com.owncloud.android.lib.resources.notifications.GetNotificationRemoteOperation;
import com.owncloud.android.lib.resources.notifications.models.Action;
import com.owncloud.android.lib.resources.notifications.models.Notification;
import com.owncloud.android.lib.resources.notifications.models.RichObject;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.ThemeUtils;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.Utf8PostMethod;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import dagger.android.AndroidInjection;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class NotificationJob extends Job {
    public static final String TAG = "NotificationJob";

    public static final String KEY_NOTIFICATION_ACCOUNT = "KEY_NOTIFICATION_ACCOUNT";
    public static final String KEY_NOTIFICATION_SUBJECT = "subject";
    public static final String KEY_NOTIFICATION_SIGNATURE = "signature";
    private static final String KEY_NOTIFICATION_ACTION_LINK = "KEY_NOTIFICATION_ACTION_LINK";
    private static final String KEY_NOTIFICATION_ACTION_TYPE = "KEY_NOTIFICATION_ACTION_TYPE";
    private static final String PUSH_NOTIFICATION_ID = "PUSH_NOTIFICATION_ID";
    private static final String NUMERIC_NOTIFICATION_ID = "NUMERIC_NOTIFICATION_ID";

    private Context context;
    private UserAccountManager accountManager;

    NotificationJob(final Context context, final UserAccountManager accountManager) {
        this.context = context;
        this.accountManager = accountManager;
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        context = getContext();
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        PersistableBundleCompat persistableBundleCompat = getParams().getExtras();
        String subject = persistableBundleCompat.getString(KEY_NOTIFICATION_SUBJECT, "");
        String signature = persistableBundleCompat.getString(KEY_NOTIFICATION_SIGNATURE, "");

        if (!TextUtils.isEmpty(subject) && !TextUtils.isEmpty(signature)) {
            try {
                byte[] base64DecodedSubject = Base64.decode(subject, Base64.DEFAULT);
                byte[] base64DecodedSignature = Base64.decode(signature, Base64.DEFAULT);
                PrivateKey privateKey = (PrivateKey) PushUtils.readKeyFromFile(false);

                try {
                    SignatureVerification signatureVerification = PushUtils.verifySignature(context,
                                                                                            accountManager,
                                                                                            base64DecodedSignature,
                                                                                            base64DecodedSubject);

                    if (signatureVerification != null && signatureVerification.isSignatureValid()) {
                        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
                        cipher.init(Cipher.DECRYPT_MODE, privateKey);
                        byte[] decryptedSubject = cipher.doFinal(base64DecodedSubject);

                        Gson gson = new Gson();
                        DecryptedPushMessage decryptedPushMessage = gson.fromJson(new String(decryptedSubject),
                                                                                  DecryptedPushMessage.class);

                        if (decryptedPushMessage.delete) {
                            notificationManager.cancel(decryptedPushMessage.nid);
                        } else if (decryptedPushMessage.deleteAll) {
                            notificationManager.cancelAll();
                        } else {
                            final User user = accountManager.getUser(signatureVerification.getAccount().name)
                                    .orElseThrow(RuntimeException::new);
                            fetchCompleteNotification(user, decryptedPushMessage);
                        }
                    }
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e1) {
                    Log.d(TAG, "Error decrypting message " + e1.getClass().getName()
                        + " " + e1.getLocalizedMessage());
                }
            } catch (Exception exception) {
                Log.d(TAG, "Something went very wrong" + exception.getLocalizedMessage());
            }
        }
        return Result.SUCCESS;
    }

    private void sendNotification(Notification notification, User user) {
        SecureRandom randomId = new SecureRandom();
        RichObject file = notification.subjectRichParameters.get("file");

        Intent intent;
        if (file == null) {
            intent = new Intent(context, NotificationsActivity.class);
        } else {
            intent = new Intent(context, FileDisplayActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.putExtra(FileDisplayActivity.KEY_FILE_ID, file.id);
        }
        intent.putExtra(KEY_NOTIFICATION_ACCOUNT, user.getAccountName());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        int pushNotificationId = randomId.nextInt();

        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_PUSH)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_icon))
                .setColor(ThemeUtils.primaryColor(user.toPlatformAccount(), false, context))
                .setShowWhen(true)
                .setSubText(user.getAccountName())
                .setContentTitle(notification.getSubject())
                .setContentText(notification.getMessage())
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setContentIntent(pendingIntent);

        // Remove
        if (notification.getActions().isEmpty()) {
            Intent disableDetection = new Intent(context, NotificationJob.NotificationReceiver.class);
            disableDetection.putExtra(NUMERIC_NOTIFICATION_ID, notification.getNotificationId());
            disableDetection.putExtra(PUSH_NOTIFICATION_ID, pushNotificationId);
            disableDetection.putExtra(KEY_NOTIFICATION_ACCOUNT, user.getAccountName());

            PendingIntent disableIntent = PendingIntent.getBroadcast(context, pushNotificationId, disableDetection,
                                                                     PendingIntent.FLAG_CANCEL_CURRENT);

            notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_close,
                                                                        context.getString(R.string.remove_push_notification), disableIntent));
        } else {
            // Actions
            for (Action action : notification.getActions()) {
                Intent actionIntent = new Intent(context, NotificationJob.NotificationReceiver.class);
                actionIntent.putExtra(NUMERIC_NOTIFICATION_ID, notification.getNotificationId());
                actionIntent.putExtra(PUSH_NOTIFICATION_ID, pushNotificationId);
                actionIntent.putExtra(KEY_NOTIFICATION_ACCOUNT, user.getAccountName());
                actionIntent.putExtra(KEY_NOTIFICATION_ACTION_LINK, action.link);
                actionIntent.putExtra(KEY_NOTIFICATION_ACTION_TYPE, action.type);


                PendingIntent actionPendingIntent = PendingIntent.getBroadcast(context, randomId.nextInt(),
                                                                               actionIntent,
                                                                               PendingIntent.FLAG_CANCEL_CURRENT);

                int icon;
                if (action.primary) {
                    icon = R.drawable.ic_check_circle;
                } else {
                    icon = R.drawable.ic_check_circle_outline;
                }

                notificationBuilder.addAction(new NotificationCompat.Action(icon, action.label, actionPendingIntent));
            }
        }

        notificationBuilder.setPublicVersion(
            new NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_PUSH)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_icon))
                .setColor(ThemeUtils.primaryColor(user.toPlatformAccount(), false, context))
                .setShowWhen(true)
                .setSubText(user.getAccountName())
                .setContentTitle(context.getString(R.string.new_notification))
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent).build());

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(notification.getNotificationId(), notificationBuilder.build());
    }

    private void fetchCompleteNotification(User account, DecryptedPushMessage decryptedPushMessage) {
        Optional<User> optionalUser = accountManager.getUser(account.getAccountName());

        if (!optionalUser.isPresent()) {
            Log_OC.e(this, "Account may not be null");
            return;
        }
        User user = optionalUser.get();

        try {
            OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton()
                .getClientFor(user.toOwnCloudAccount(), context);

            RemoteOperationResult result = new GetNotificationRemoteOperation(decryptedPushMessage.nid)
                .execute(client);

            if (result.isSuccess()) {
                Notification notification = result.getNotificationData().get(0);
                sendNotification(notification, account);
            }

        } catch (Exception e) {
            Log_OC.e(this, "Error creating account", e);
        }
    }

    public static class NotificationReceiver extends BroadcastReceiver {

        @Inject UserAccountManager accountManager;

        @Override
        public void onReceive(Context context, Intent intent) {
            AndroidInjection.inject(this, context);
            int numericNotificationId = intent.getIntExtra(NUMERIC_NOTIFICATION_ID, 0);
            String accountName = intent.getStringExtra(NotificationJob.KEY_NOTIFICATION_ACCOUNT);

            if (numericNotificationId != 0) {
                new Thread(() -> {
                    NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                        Activity.NOTIFICATION_SERVICE);

                    android.app.Notification oldNotification = null;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
                        for (StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
                            if (numericNotificationId == statusBarNotification.getId()) {
                                oldNotification = statusBarNotification.getNotification();
                                break;
                            }
                        }

                        cancel(context, numericNotificationId);
                    }

                    try {
                        Optional<User> optionalUser = accountManager.getUser(accountName);
                        if (!optionalUser.isPresent()) {
                            Log_OC.e(this, "Account may not be null");
                            return;
                        }
                        User user = optionalUser.get();
                        OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton()
                            .getClientFor(user.toOwnCloudAccount(), context);

                        String actionType = intent.getStringExtra(KEY_NOTIFICATION_ACTION_TYPE);
                        String actionLink = intent.getStringExtra(KEY_NOTIFICATION_ACTION_LINK);

                        boolean success;
                        if (!TextUtils.isEmpty(actionType) && !TextUtils.isEmpty(actionLink)) {
                            int resultCode = executeAction(actionType, actionLink, client);
                            success = resultCode == HttpStatus.SC_OK || resultCode == HttpStatus.SC_ACCEPTED;
                        } else {
                            success = new DeleteNotificationRemoteOperation(numericNotificationId)
                                .execute(client).isSuccess();
                        }

                        if (success) {
                            if (oldNotification == null) {
                                cancel(context, numericNotificationId);
                            }
                        } else if (notificationManager != null) {
                            notificationManager.notify(numericNotificationId, oldNotification);
                        }
                    } catch (IOException | OperationCanceledException | AuthenticatorException e) {
                        Log_OC.e(TAG, "Error initializing client", e);
                    }
                }).start();
            }
        }

        @SuppressFBWarnings(value = "HTTP_PARAMETER_POLLUTION",
            justification = "link and type are from server and expected to be safe")
        private int executeAction(String actionType, String actionLink, OwnCloudClient client) {
            HttpMethod method;

            switch (actionType) {
                case "GET":
                    method = new GetMethod(actionLink);
                    break;

                case "POST":
                    method = new Utf8PostMethod(actionLink);
                    break;

                case "DELETE":
                    method = new DeleteMethod(actionLink);
                    break;

                case "PUT":
                    method = new PutMethod(actionLink);
                    break;

                default:
                    // do nothing
                    return 0;
            }

            method.setRequestHeader(RemoteOperation.OCS_API_HEADER, RemoteOperation.OCS_API_HEADER_VALUE);

            try {
                return client.executeMethod(method);
            } catch (IOException e) {
                Log_OC.e(TAG, "Execution of notification action failed: " + e);
            }
            return 0;
        }

        private void cancel(Context context, int notificationId) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(
                Activity.NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.cancel(notificationId);
            }
        }
    }
}
