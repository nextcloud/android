package com.owncloud.android.utils;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import com.owncloud.android.MainApp;
import com.owncloud.android.ui.activity.ErrorReportActivity;

import java.io.PrintWriter;
import java.io.StringWriter;

// from https://stackoverflow.com/questions/23486627/catching-error-and-user-information#answer-23486834
public class ExceptionHandler implements Thread.UncaughtExceptionHandler {

    Context context;
    private final String LINE_SEPARATOR = "\n";

    public ExceptionHandler() {
        // TODO Auto-generated constructor stub
        context = MainApp.getAppContext();
    }

    @Override
    public void uncaughtException(Thread arg0, Throwable arg1) {
        // TODO Auto-generated method stub

        StringWriter stackTrace = new StringWriter();
        arg1.printStackTrace(new PrintWriter(stackTrace));
        final StringBuilder errorReport = new StringBuilder();
        errorReport.append("************ CAUSE OF ERROR ************\n\n");
        errorReport.append(stackTrace.toString());

        errorReport.append("\n************ DEVICE INFORMATION ***********\n");
        errorReport.append("Brand: ");
        errorReport.append(Build.BRAND);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Device: ");
        errorReport.append(Build.DEVICE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Model: ");
        errorReport.append(Build.MODEL);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Id: ");
        errorReport.append(Build.ID);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Product: ");
        errorReport.append(Build.PRODUCT);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("\n************ FIRMWARE ************\n");
        errorReport.append("SDK: ");
        errorReport.append(Build.VERSION.SDK);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Release: ");
        errorReport.append(Build.VERSION.RELEASE);
        errorReport.append(LINE_SEPARATOR);
        errorReport.append("Incremental: ");
        errorReport.append(Build.VERSION.INCREMENTAL);
        errorReport.append(LINE_SEPARATOR);


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

        Intent sendIntent = new Intent(context, ErrorReportActivity.class);
        sendIntent.putExtra(Intent.EXTRA_TEXT, errorReport.toString());
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        sendIntent.setType("text/plain");
        context.startActivity(sendIntent);


        System.exit(1);
//        android.os.Process.killProcess(android.os.Process.myPid());

    }

    private boolean isUIThread(){
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }
}