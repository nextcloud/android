package com.owncloud.android.ui.asynctasks;

import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.concurrent.Callable;

/**
 *  Nextcloud SingleSignOn
 *
 *  @author David Luhmer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
