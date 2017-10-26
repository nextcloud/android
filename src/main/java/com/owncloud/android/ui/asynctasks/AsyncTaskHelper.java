package com.owncloud.android.ui.asynctasks;

/**
 * Created by david on 27.06.17.
 */

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.concurrent.Callable;

/**
 * Created by david on 27.06.17.
 */

public class AsyncTaskHelper {

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public static <T> T executeBlockingRequest(Callable<T> callable) throws Exception {
        GenericAsyncTaskWithCallable<T> at = new GenericAsyncTaskWithCallable<>(callable);

        T result = at.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR).get();

        if(at.exception != null) {
            throw at.exception;
        }

        return result;
    }

    private static class GenericAsyncTaskWithCallable<T> extends AsyncTask<Void, Void, T> {

        private Callable<T> callable;
        private Exception exception;

        GenericAsyncTaskWithCallable(Callable<T> callable) {
            this.callable = callable;
        }

        @Override
        protected T doInBackground(Void... params) {
            try {
                return callable.call();
            } catch (Exception e) {
                exception = e;
                return null;
            }
        }
    }

}
