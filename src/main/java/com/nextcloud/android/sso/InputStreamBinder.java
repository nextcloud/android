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
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import com.nextcloud.android.sso.aidl.IInputStreamService;
import com.nextcloud.android.sso.aidl.NextcloudRequest;
import com.nextcloud.android.sso.aidl.ParcelFileDescriptorUtil;
import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManager;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.EncryptionUtils;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static com.nextcloud.android.sso.Constants.EXCEPTION_ACCOUNT_NOT_FOUND;
import static com.nextcloud.android.sso.Constants.EXCEPTION_HTTP_REQUEST_FAILED;
import static com.nextcloud.android.sso.Constants.EXCEPTION_INVALID_REQUEST_URL;
import static com.nextcloud.android.sso.Constants.EXCEPTION_INVALID_TOKEN;
import static com.nextcloud.android.sso.Constants.EXCEPTION_UNSUPPORTED_METHOD;
import static com.nextcloud.android.sso.Constants.SSO_SHARED_PREFERENCE;


/**
 * Stream binder to pass usable InputStreams across the process boundary in Android.
 */
public class InputStreamBinder extends IInputStreamService.Stub {

    private final static String TAG = "InputStreamBinder";
    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";
    private static final String CHARSET_UTF8 = "UTF-8";

    private static final int HTTP_STATUS_CODE_OK = 200;
    private static final int HTTP_STATUS_CODE_MULTIPLE_CHOICES = 300;

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
            Log_OC.e(TAG, "Error during Nextcloud request", e);
            exception = e;
        }

        try {
            // Write exception to the stream followed by the actual network stream
            InputStream exceptionStream = serializeObjectToInputStream(exception);
            InputStream resultStream = new java.io.SequenceInputStream(exceptionStream, httpStream);
            return ParcelFileDescriptorUtil.pipeFrom(resultStream, thread -> Log.d(TAG, "Done sending result"));
        } catch (IOException e) {
            Log_OC.e(TAG, "Error while sending response back to client app", e);
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
        Account account = AccountUtils.getOwnCloudAccountByName(context, request.getAccountName()); // TODO handle case that account is not found!
        if(account == null) {
            throw new IllegalStateException(EXCEPTION_ACCOUNT_NOT_FOUND);
        }

        // Validate token
        if (!isValid(request)) {
            throw new IllegalStateException(EXCEPTION_INVALID_TOKEN);
        }

        // Validate URL
        if(request.getUrl().length() == 0 || request.getUrl().charAt(0) != PATH_SEPARATOR) {
            throw new IllegalStateException(EXCEPTION_INVALID_REQUEST_URL, new IllegalStateException("URL need to start with a /"));
        }

        OwnCloudClientManager ownCloudClientManager = OwnCloudClientManagerFactory.getDefaultSingleton();
        OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
        OwnCloudClient client = ownCloudClientManager.getClientFor(ocAccount, context);

        String requestUrl = client.getBaseUri() + request.getUrl();
        HttpMethodBase method;

        switch (request.getMethod()) {
            case "GET":
                method = new GetMethod(requestUrl);
                break;

            case "POST":
                method = new PostMethod(requestUrl);
                if (request.getRequestBody() != null) {
                    StringRequestEntity requestEntity = new StringRequestEntity(
                            request.getRequestBody(),
                            CONTENT_TYPE_APPLICATION_JSON,
                            CHARSET_UTF8);
                    ((PostMethod) method).setRequestEntity(requestEntity);
                }
                break;

            case "PUT":
                method = new PutMethod(requestUrl);
                if (request.getRequestBody() != null) {
                    StringRequestEntity requestEntity = new StringRequestEntity(
                            request.getRequestBody(),
                            CONTENT_TYPE_APPLICATION_JSON,
                            CHARSET_UTF8);
                    ((PutMethod) method).setRequestEntity(requestEntity);
                }
                break;

            case "DELETE":
                method = new DeleteMethod(requestUrl);
                break;

            default:
                throw new UnsupportedOperationException(EXCEPTION_UNSUPPORTED_METHOD);

        }

        method.setQueryString(convertMapToNVP(request.getParameter()));
        method.addRequestHeader("OCS-APIREQUEST", "true");

        for(Map.Entry<String, List<String>> header : request.getHeader().entrySet()) {
            // https://stackoverflow.com/a/3097052
            method.addRequestHeader(header.getKey(), TextUtils.join(",", header.getValue()));
        }

        client.setFollowRedirects(request.isFollowRedirects());
        int status = client.executeMethod(method);

        // Check if status code is 2xx --> https://en.wikipedia.org/wiki/List_of_HTTP_status_codes#2xx_Success
        if (status >= HTTP_STATUS_CODE_OK && status < HTTP_STATUS_CODE_MULTIPLE_CHOICES) {
            return method.getResponseBodyAsStream();
        } else {
            StringBuilder total = new StringBuilder();
            InputStream inputStream = method.getResponseBodyAsStream();
            // If response body is available
            if (inputStream != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String line = reader.readLine();
                while (line != null) {
                    total.append(line).append('\n');
                    line = reader.readLine();
                }
                Log_OC.e(TAG, total.toString());
            }
            throw new IllegalStateException(EXCEPTION_HTTP_REQUEST_FAILED,
                new IllegalStateException(String.valueOf(status), new Throwable(total.toString())));
        }
    }

    private boolean isValid(NextcloudRequest request) {
        String callingPackageName = context.getPackageManager().getNameForUid(Binder.getCallingUid());

        SharedPreferences sharedPreferences = context.getSharedPreferences(SSO_SHARED_PREFERENCE,
                Context.MODE_PRIVATE);
        String hash = sharedPreferences.getString(callingPackageName, "");
        return validateToken(hash, request.getToken());
    }

    private boolean validateToken(String hash, String token) {
        if (hash.isEmpty() || !hash.contains("$")) {
            throw new IllegalStateException(EXCEPTION_INVALID_TOKEN);
        }

        String salt = hash.split("\\$")[1]; // TODO extract "$"

        String newHash = EncryptionUtils.generateSHA512(token, salt);

        // As discussed with Lukas R. at the Nextcloud Conf 2018, always compare whole strings
        // and don't exit prematurely if the string does not match anymore to prevent timing-attacks
        return isEqual(hash.getBytes(), newHash.getBytes());
    }

    // Taken from http://codahale.com/a-lesson-in-timing-attacks/
    private static boolean isEqual(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}
