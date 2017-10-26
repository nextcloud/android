package de.luhmer.owncloud.accountimporter.helper;

import android.accounts.Account;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.ui.asynctasks.AsyncTaskHelper;

import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Map;
import java.util.concurrent.Callable;

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

        try {
            final NextcloudRequest request = deserializeObjectAndCloseStream(is);

            InputStream resultStream = processRequest(request);
            try {
                return ParcelFileDescriptorUtil.pipeFrom(resultStream, new IThreadListener() {
                    @Override
                    public void onThreadFinished(Thread thread) {
                        Log.d(TAG, "Done sending result");
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            Log.d(TAG, "Test #1 failed");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }


    private <T> T deserializeObjectAndCloseStream(InputStream is) throws IOException, ClassNotFoundException {
        ObjectInputStream ois = new ObjectInputStream(is);
        T result = (T) ois.readObject();
        is.close();
        ois.close();
        return result;
    }

    private InputStream processRequest(final NextcloudRequest request) {
        try {
            return AsyncTaskHelper.executeBlockingRequest(new Callable<InputStream>() {
                @Override
                public InputStream call() throws Exception {
                    Account account = AccountUtils.getOwnCloudAccountByName(context, request.accountName); // TODO handle case that account is not found!
                    OwnCloudAccount ocAccount = new OwnCloudAccount(account, context);
                    OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, context);

                    //OwnCloudVersion version = AccountUtils.getServerVersion(account);
                    //client.setOwnCloudVersion(version);

                    // TODO do some checks if url is correct!! (prevent ../ in url etc..
                    request.url = client.getBaseUri() + request.url;

                    //AccountManagerService.INetworkInterface network = (stream) ? new AccountManagerService.StreamingRequest(port) : new AccountManagerService.PlainRequest();

                    HttpMethodBase method = null;

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

                    //throw new Exception("Test!!!");

                    int status = client.executeMethod(method);
                    if (status == 200) {
                        return method.getResponseBodyAsStream();
                    } else {
                        throw new Exception("Request returned code: " + status);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            //TODO return exception to calling client app!
            //return exceptionToInputStream(null);
        }
        return null;
    }

}
