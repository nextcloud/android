package com.owncloud.android.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;

import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.datamodel.ThumbnailsCacheManager;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;

public class ShortcutUtil {



    /**
     * Adds a pinned shortcut to the home screen that points to the passed file/folder.
     *
     * @param file The file/folder to which a pinned shortcut should be added to the home screen.
     */
    public static void addShortcutToHomescreen(Context context, OCFile file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final ShortcutManager shortcutManager = context.getSystemService(ShortcutManager.class);

            if (shortcutManager.isRequestPinShortcutSupported()) {
                final Intent intent = new Intent(context, FileDisplayActivity.class);
                intent.setAction(FileDisplayActivity.OPEN_FILE);
                intent.putExtra(FileActivity.EXTRA_FILE, file.getRemotePath());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                final String shortcutId = "nextcloud_shortcut_" + file.getRemoteId();

                Icon icon;

                Bitmap thumbnail = ThumbnailsCacheManager.getBitmapFromDiskCache(
                    ThumbnailsCacheManager.PREFIX_THUMBNAIL + file.getRemoteId());
                if (thumbnail != null) {
                    thumbnail = bitmapToAdaptiveBitmap(thumbnail, context);
                    icon = Icon.createWithAdaptiveBitmap(thumbnail);
                } else if (file.isFolder()) {
                    icon = Icon.createWithResource(context,
                                                   MimeTypeUtil.getFolderTypeIconId(file.isSharedWithMe() ||
                                                                                        file.isSharedWithSharee(), file.isSharedViaLink(), file.isEncrypted(), file.getMountType()));
                } else {
                    icon = Icon.createWithResource(context,
                                                   MimeTypeUtil.getFileTypeIconId(file.getMimeType(), file.getFileName()));
                }

                final ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(context, shortcutId)
                    .setShortLabel(file.getFileName())
                    .setLongLabel("Open " + file.getFileName())
                    .setIcon(icon)
                    .setIntent(intent)
                    .build();

                final Intent pinnedShortcutCallbackIntent =
                    shortcutManager.createShortcutResultIntent(pinShortcutInfo);

                final PendingIntent successCallback = PendingIntent.getBroadcast(context, 0,
                                                                                 pinnedShortcutCallbackIntent, 0);

                shortcutManager.requestPinShortcut(pinShortcutInfo,
                                                   successCallback.getIntentSender());
            }
        }
    }

    private static Bitmap bitmapToAdaptiveBitmap(final Bitmap orig, final Context context) {
        final float screenDensity = context.getResources().getDisplayMetrics().density;
        final int adaptiveIconSize = Math.round(108 * screenDensity);
        final int adaptiveIconOuterSides = Math.round(18 * screenDensity);

        Drawable drawable = new BitmapDrawable(context.getResources(), orig);

        final Bitmap bitmap = Bitmap.createBitmap(adaptiveIconSize, adaptiveIconSize, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(adaptiveIconOuterSides, adaptiveIconOuterSides, adaptiveIconSize - adaptiveIconOuterSides,
                           adaptiveIconSize - adaptiveIconOuterSides);
        drawable.draw(canvas);
        return bitmap;
    }

}
