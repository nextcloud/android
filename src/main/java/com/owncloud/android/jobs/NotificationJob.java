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
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.util.support.PersistableBundleCompat;
import com.google.gson.Gson;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.DecryptedPushMessage;
import com.owncloud.android.datamodel.SignatureVerification;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.notifications.NotificationUtils;
import com.owncloud.android.utils.PushUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;

public class NotificationJob extends Job {
    public static final String TAG = "NotificationJob";

    public static final String KEY_NOTIFICATION_SUBJECT = "subject";
    public static final String KEY_NOTIFICATION_SIGNATURE = "signature";
    public static final String KEY_NOTIFICATION_ACCOUNT = "KEY_NOTIFICATION_ACCOUNT";

    @NonNull
    @Override
    protected Result onRunJob(Params params) {

        Context context = getContext();
        PersistableBundleCompat persistableBundleCompat = getParams().getExtras();
        String subject = persistableBundleCompat.getString(KEY_NOTIFICATION_SUBJECT, "");
        String signature = persistableBundleCompat.getString(KEY_NOTIFICATION_SIGNATURE, "");

        if (!TextUtils.isEmpty(subject) && !TextUtils.isEmpty(signature)) {

            try {
                byte[] base64DecodedSubject = Base64.decode(subject, Base64.DEFAULT);
                byte[] base64DecodedSignature = Base64.decode(signature, Base64.DEFAULT);
                PushUtils pushUtils = new PushUtils();
                PrivateKey privateKey = (PrivateKey) PushUtils.readKeyFromFile(false);

                try {
                    SignatureVerification signatureVerification = pushUtils.verifySignature(context,
                            base64DecodedSignature, base64DecodedSubject);

                    if (signatureVerification.isSignatureValid()) {
                        Cipher cipher = Cipher.getInstance("RSA/None/PKCS1Padding");
                        cipher.init(Cipher.DECRYPT_MODE, privateKey);
                        byte[] decryptedSubject = cipher.doFinal(base64DecodedSubject);

                        Gson gson = new Gson();
                        DecryptedPushMessage decryptedPushMessage = gson.fromJson(new String(decryptedSubject),
                                DecryptedPushMessage.class);

                        // We ignore Spreed messages for now
                        if (!decryptedPushMessage.getApp().equals("spreed")) {
                            sendNotification(decryptedPushMessage.getSubject(), signatureVerification.getAccount());
                        }

                    }
                } catch (NoSuchAlgorithmException e1) {
                    Log.d(TAG, "No proper algorithm to decrypt the message " + e1.getLocalizedMessage());
                } catch (NoSuchPaddingException e1) {
                    Log.d(TAG, "No proper padding to decrypt the message " + e1.getLocalizedMessage());
                } catch (InvalidKeyException e1) {
                    Log.d(TAG, "Invalid private key " + e1.getLocalizedMessage());
                }
            } catch (Exception exception) {
                Log.d(TAG, "Something went very wrong" + exception.getLocalizedMessage());
            }
        }
        return Result.SUCCESS;
    }

    private void sendNotification(String contentTitle, Account account) {
        Context context = getContext();
        Intent intent = new Intent(getContext(), NotificationsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(KEY_NOTIFICATION_ACCOUNT, account.name);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_icon))
                .setColor(ThemeUtils.primaryColor(context))
                .setShowWhen(true)
                .setSubText(account.name)
                .setContentTitle(contentTitle)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if ((android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)) {
            notificationBuilder.setChannelId(NotificationUtils.NOTIFICATION_CHANNEL_PUSH);
        }

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }

}
