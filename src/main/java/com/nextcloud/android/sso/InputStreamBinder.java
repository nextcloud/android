/*
 * Nextcloud SingleSignOn
 *
 * @author David Luhmer
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
 *
 * More information here: https://github.com/abeluck/android-streams-ipc
 */

package com.nextcloud.android.sso;

import android.accounts.Account;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.nextcloud.android.sso.aidl.IInputStreamService;
import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.nextcloud.android.sso.aidl.ParcelFileDescriptorUtil;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.db.PreferenceManager;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManager;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map;

import static com.nextcloud.android.sso.Constants.EXCEPTION_ACCOUNT_NOT_FOUND;
import static com.nextcloud.android.sso.Constants.EXCEPTION_INVALID_TOKEN;
import static com.nextcloud.android.sso.Constants.EXCEPTION_UNSUPPORTED_METHOD;


/**
 * Stream binder to pass usable InputStreams across the process boundary in Android.
 */
public class InputStreamBinder extends IInputStreamService.Stub {

    private final static String TAG = "InputStreamBinder";
    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    private static final String CHARSET_UTF8 = "UTF-8";
    private static final int HTTP_STATUS_CODE_OK = 200;
    private static final char PATH_SEPARATOR = '/';
    private Context context;

    public InputStreamBinder(Context context) {
        this.context = context;
    }

    private NameValuePair[] convertMapToNVP(Map<String, String> map) {
        NameValuePair[] nvp = new NameValuePair[map.size()];
        int i = 0;
        for (String key : map.keySet()) {
            nvp[i] = new NameValuePair(key, map.get(key));
            i++;
        }
        return nvp;
    }

    public ParcelFileDescriptor performNextcloudRequest(ParcelFileDescriptor input) {
        // read the input
        final InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(input);

        Exception exception = null;
        InputStream httpStream = new InputStream() {
            @Override
            public int read() {
                return 0;
            }
        };
        try {
            // Start request and catch exceptions
            NextcloudRequest request = deserializeObjectAndCloseStream(is);
            httpStream = processRequest(request);
        } catch (Exception e) {
            Log_OC.e(TAG, e.getMessage());
            exception = e;
        }

        try {
            // Write exception to the stream followed by the actual network stream
            InputStream exceptionStream = serializeObjectToInputStream(exception);
            InputStream resultStream = new java.io.SequenceInputStream(exceptionStream, httpStream);
            return ParcelFileDescriptorUtil.pipeFrom(resultStream, thread -> Log.d(TAG, "Done sending result"));
        } catch (IOException e) {
            Log_OC.e(TAG, e.getMessage());
        }
        return null;
    }

    private <T extends Serializable> ByteArrayInputStream serializeObjectToInputStream(T obj) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.flush();
        oos.close();
        return new ByteArrayInputStream(baos.toByteArray());
    }

    private <T extends Serializable> T deserializeObjectAndCloseStream(InputStream is) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(is);
        T result = (T) ois.readObject();
        is.close();
        ois.close();
        return result;
    }


    private InputStream processRequest(final NextcloudRequest request) throws UnsupportedOperationException, com.owncloud.android.lib.common.accounts.AccountUtils.AccountNotFoundException, OperationCanceledException, AuthenticatorException, IOException {
        Account account = AccountUtils.getOwnCloudAccountByName(context, request.accountName); // TODO handle case that account is not found!
        if(account == null) {
            throw new IllegalStateException(EXCEPTION_ACCOUNT_NOT_FOUND);
        }

        // Validate token
        if (!isValid(request)) {
            throw new IllegalStateException(EXCEPTION_INVALID_TOKEN);
        }

        // Validate URL
        if(request.url.charAt(0) != PATH_SEPARATOR) {
            throw new IllegalStateException("URL need to start with a /");
        }

        OwnCloudClientManager ownCloudClientManager = OwnCloudClientManagerFactory.getDefaultSingleton();
        OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
        OwnCloudClient client = ownCloudClientManager.getClientFor(ocAccount, context);

        request.url = client.getBaseUri() + request.url;
        HttpMethodBase method;

        switch (request.method) {
            case "GET":
                method = new GetMethod(request.url);
                break;

            case "POST":
                method = new PostMethod(request.url);
                if (request.requestBody != null) {
                    StringRequestEntity requestEntity = new StringRequestEntity(
                            request.requestBody,
                            CONTENT_TYPE_APPLICATION_JSON,
                            CHARSET_UTF8);
                    ((PostMethod) method).setRequestEntity(requestEntity);
                }
                break;

            case "PUT":
                method = new PutMethod(request.url);
                if (request.requestBody != null) {
                    StringRequestEntity requestEntity = new StringRequestEntity(
                            request.requestBody,
                            CONTENT_TYPE_APPLICATION_JSON,
                            CHARSET_UTF8);
                    ((PutMethod) method).setRequestEntity(requestEntity);
                }
                break;

            case "DELETE":
                method = new DeleteMethod(request.url);
                break;

            default:
                throw new UnsupportedOperationException(EXCEPTION_UNSUPPORTED_METHOD);

        }

        method.setQueryString(convertMapToNVP(request.parameter));
        method.addRequestHeader("OCS-APIREQUEST", "true");

        int status = client.executeMethod(method);
        if (status == HTTP_STATUS_CODE_OK) {
            return method.getResponseBodyAsStream();
        } else {
            throw new IllegalStateException("Request returned code: " + status);
        }
    }

    private boolean isValid(NextcloudRequest request) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String storedToken = sharedPreferences.getString(request.packageName, "");
        return validPackages.contains(request.packageName) && request.token.equals(storedToken);
    }
}
