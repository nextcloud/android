package de.luhmer.owncloud.accountimporter.helper;

import android.accounts.Account;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;

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

/**
 * Created by david on 29.06.17.
 *
 * More information here: https://github.com/abeluck/android-streams-ipc
 */

public class InputStreamBinder extends IInputStreamService.Stub {
    private final static String TAG = "InputStreamBinder";

    private Context context;
    public InputStreamBinder(Context ctxt) {
        this.context = ctxt;
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
            public int read() throws IOException {
                return 0;
            }
        };
        try {
            // Start request and catch exceptions
            NextcloudRequest request = deserializeObjectAndCloseStream(is);
            httpStream = processRequest(request);
        } catch (Exception e) {
            e.printStackTrace();
            exception = e;
        }

        try {
            // Write exception to the stream followed by the actual network stream
            InputStream exceptionStream = serializeObjectToInputStream(exception);
            InputStream resultStream = new java.io.SequenceInputStream(exceptionStream, httpStream);
            return ParcelFileDescriptorUtil.pipeFrom(resultStream, new IThreadListener() {
                @Override
                public void onThreadFinished(Thread thread) {
                    Log.d(TAG, "Done sending result");
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
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

    private InputStream processRequest(final NextcloudRequest request) throws Exception {
        Account account = AccountUtils.getOwnCloudAccountByName(context, request.accountName); // TODO handle case that account is not found!
        OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
        OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context);


        // Validate Auth-Token
        if(!client.getCredentials().getAuthToken().equals(request.token)) {
            throw new IllegalStateException("Provided authentication token does not match!");
        }

        // Validate URL
        if(!request.url.startsWith("/")) {
            throw new IllegalStateException("URL need to start with a /");
        }

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
                            "application/json",
                            "UTF-8");
                    ((PostMethod) method).setRequestEntity(requestEntity);
                }
                break;

            case "PUT":
                method = new PutMethod(request.url);
                if (request.requestBody != null) {
                    StringRequestEntity requestEntity = new StringRequestEntity(
                            request.requestBody,
                            "application/json",
                            "UTF-8");
                    ((PutMethod) method).setRequestEntity(requestEntity);
                }
                break;

            case "DELETE":
                method = new DeleteMethod(request.url);
                break;

            default:
                throw new Exception("Unexpected type!!");

        }

        method.setQueryString(convertMapToNVP(request.parameter));
        method.addRequestHeader("OCS-APIREQUEST", "true");

        int status = client.executeMethod(method);
        if (status == 200) {
            return method.getResponseBodyAsStream();
        } else {
            throw new Exception("Request returned code: " + status);
        }
    }

}
