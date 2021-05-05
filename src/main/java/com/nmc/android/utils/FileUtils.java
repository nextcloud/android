package com.nmc.android.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.text.TextUtils;

import com.owncloud.android.lib.common.utils.Log_OC;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FileUtils {
    private static final String TAG = FileUtils.class.getSimpleName();

    private static final String SCANNED_DIRECTORY_NAME = "Scanned";

    public static File saveImage(Context context, Bitmap bitmap, String imageName) {
        try {
            File file = saveImageFile(context, imageName);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            return file;
        } catch (Exception e) {
            Log_OC.e(TAG, " Failed to save image : " + e.getLocalizedMessage());
            return null;
        }
    }

    public static File saveImageFile(Context context, String imageName) {
        File imageFile = getOutputMediaFile(context);
        if (!TextUtils.isEmpty(imageName)) {
            return new File(imageFile.getPath() + File.separator + "IMG_" + imageName + ".jpg");
        } else {
            return new File(imageFile.getPath() + File.separator + "IMG_" + new SimpleDateFormat("yyyyMMdd_HHmmss",
                                                                                                 Locale.getDefault()).format(new Date()) + ".jpg");
        }
    }

    public static File getOutputMediaFile(Context context) {
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), SCANNED_DIRECTORY_NAME);
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    public static Bitmap convertFileToBitmap(File file) {
        String filePath = file.getPath();
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);
        return bitmap;
    }
}
