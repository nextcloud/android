package com.owncloud.android.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.owncloud.android.ui.activity.ErrorReportActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

// from https://stackoverflow.com/questions/23486627/catching-error-and-user-information#answer-23486834
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {
    private final Activity mContext;

    private static final String TAG = com.owncloud.android.ui.errorhandling.ExceptionHandler.class.getSimpleName();

    public ExceptionHandler(Activity context) {
        mContext = context;
    }

    @Override
    public void uncaughtException(Thread arg0, Throwable exception) {
        Log.e(TAG, "ExceptionHandler caught UncaughtException", exception);
        StringWriter stackTrace = new StringWriter();
        exception.printStackTrace(new PrintWriter(stackTrace));
        final StringBuilder errorReport = new StringBuilder(192);
        final String LINE_SEPARATOR = "\n";
        errorReport.append("************ CAUSE OF ERROR ************\n\n")
                .append(stackTrace.toString())
                .append("\n************ DEVICE INFORMATION ***********\nBrand: ")
                .append(Build.BRAND)
                .append(LINE_SEPARATOR)
                .append("Device: ")
                .append(Build.DEVICE)
                .append(LINE_SEPARATOR)
                .append("Model: ")
                .append(Build.MODEL)
                .append(LINE_SEPARATOR)
                .append("Id: ")
                .append(Build.ID)
                .append(LINE_SEPARATOR)
                .append("Product: ")
                .append(Build.PRODUCT)
                .append(LINE_SEPARATOR)
                .append("\n************ FIRMWARE ************\nSDK: ")
                .append(Build.VERSION.SDK_INT)
                .append(LINE_SEPARATOR)
                .append("Release: ")
                .append(Build.VERSION.RELEASE)
                .append(LINE_SEPARATOR)
                .append("Incremental: ")
                .append(Build.VERSION.INCREMENTAL)
                .append(LINE_SEPARATOR);


        //after this you can do whatever you want , like i start an activity and show error log there

        if (isUIThread()) {
            invokeLogActivity(errorReport);
        } else {  //handle non UI thread throw uncaught exception
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    invokeLogActivity(errorReport);
                }
            });
        }
    }
    private void invokeLogActivity(StringBuilder errorReport){
//        Intent sendIntent = new Intent();
//        sendIntent.setAction(Intent.ACTION_SEND);
//        sendIntent.putExtra(Intent.EXTRA_TEXT, errorReport.toString());
//        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//        sendIntent.setType("text/plain");
//        context.startActivity(sendIntent);

        Intent sendIntent = new Intent(mContext, ErrorReportActivity.class);
        sendIntent.putExtra(Intent.EXTRA_TEXT, errorReport.toString());
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendIntent.setType("text/plain");
        mContext.startActivity(sendIntent);

        android.os.Process.killProcess(android.os.Process.myPid());
        System.exit(1);
    }

    private boolean isUIThread() {
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}