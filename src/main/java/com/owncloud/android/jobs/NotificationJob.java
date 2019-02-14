/*
 * Nextcloud application
 *
 * @author Mario Danic
 * Copyright (C) 2017-2018 Mario Danic <mario@lovelyhq.com>
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

import android.accounts.Account;
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
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.gson.Gson;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.datamodel.DecryptedPushMessage;
import com.owncloud.android.datamodel.SignatureVerification;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.notifications.DeleteNotificationRemoteOperation;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationJob extends Job {
    public static final String TAG = "NotificationJob";

    public static final String KEY_NOTIFICATION_SUBJECT = "subject";
    public static final String KEY_NOTIFICATION_SIGNATURE = "signature";
    public static final String KEY_NOTIFICATION_ACCOUNT = "KEY_NOTIFICATION_ACCOUNT";
    private static final String PUSH_NOTIFICATION_ID = "PUSH_NOTIFICATION_ID";
    private static final String NUMERIC_NOTIFICATION_ID = "NUMERIC_NOTIFICATION_ID";

    private SecureRandom randomId = new SecureRandom();

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {

        Context context = getContext();
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
                                                                                            base64DecodedSignature,
                                                                                            base64DecodedSubject);

                    if (signatureVerification.isSignatureValid()) {
                        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
                        cipher.init(Cipher.DECRYPT_MODE, privateKey);
                        byte[] decryptedSubject = cipher.doFinal(base64DecodedSubject);

                        Gson gson = new Gson();
                        DecryptedPushMessage decryptedPushMessage = gson.fromJson(new String(decryptedSubject),
                                                                                  DecryptedPushMessage.class);

                        // We ignore Spreed messages for now
                        if (!"spreed".equals(decryptedPushMessage.getApp())) {
                            sendNotification(decryptedPushMessage, signatureVerification.getAccount());
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

    private void sendNotification(DecryptedPushMessage pushMessage, Account account) {
        Context context = getContext();
        Intent intent = new Intent(getContext(), NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(KEY_NOTIFICATION_ACCOUNT, account.name);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        int pushNotificationId = randomId.nextInt();

        NotificationCompat.Builder notificationBuilder =
            new NotificationCompat.Builder(context, NotificationUtils.NOTIFICATION_CHANNEL_PUSH)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_icon))
                .setColor(ThemeUtils.primaryColor(account, false, context))
                .setShowWhen(true)
                .setSubText(account.name)
                .setContentTitle(pushMessage.getSubject())
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        Intent disableDetection = new Intent(context, NotificationJob.NotificationReceiver.class);
        disableDetection.putExtra(NUMERIC_NOTIFICATION_ID, pushMessage.getNid());
        disableDetection.putExtra(PUSH_NOTIFICATION_ID, pushNotificationId);
        disableDetection.putExtra(KEY_NOTIFICATION_ACCOUNT, account.name);

        PendingIntent disableIntent = PendingIntent.getBroadcast(context, pushNotificationId, disableDetection,
                                                                 PendingIntent.FLAG_CANCEL_CURRENT);

        notificationBuilder.addAction(new NotificationCompat.Action(R.drawable.ic_close,
                                                                    context.getString(R.string.remove_push_notification), disableIntent));

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(pushNotificationId, notificationBuilder.build());
    }

    public static class NotificationReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int numericNotificationId = intent.getIntExtra(NUMERIC_NOTIFICATION_ID, 0);
            int pushNotificationId = intent.getIntExtra(PUSH_NOTIFICATION_ID, 0);
            String accountName = intent.getStringExtra(NotificationJob.KEY_NOTIFICATION_ACCOUNT);

            if (numericNotificationId != 0) {
                new Thread(() -> {
                    try {
                        Account currentAccount = AccountUtils.getOwnCloudAccountByName(context, accountName);

                        if (currentAccount == null) {
                            Log_OC.e(this, "Account may not be null");
                            return;
                        }

                        OwnCloudAccount ocAccount = new OwnCloudAccount(currentAccount, context);
                        OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton()
                            .getClientFor(ocAccount, context);
                        client.setOwnCloudVersion(AccountUtils.getServerVersion(currentAccount));

                        new DeleteNotificationRemoteOperation(numericNotificationId).execute(client);

                        cancel(context, pushNotificationId);

                    } catch (com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException |
                        IOException | OperationCanceledException | AuthenticatorException e) {
                        Log_OC.e(TAG, "Error initializing client", e);
                    }
                }).start();
            }
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
