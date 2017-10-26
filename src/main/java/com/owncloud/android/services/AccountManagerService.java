package com.owncloud.android.services;

import android.accounts.Account;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import com.owncloud.android.authentication.AccountUtils;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.asynctasks.AsyncTaskHelper;

import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Map;

import de.luhmer.owncloud.accountimporter.helper.InputStreamBinder;
import de.luhmer.owncloud.accountimporter.helper.NextcloudRequest;

public class AccountManagerService extends Service {

    private static final String TAG = AccountManagerService.class.getCanonicalName();

    /** Command to the service to display a message */
    private static final int MSG_CREATE_NEW_ACCOUNT = 3;

    private static final int MSG_REQUEST_NETWORK_REQUEST = 4;
    private static final int MSG_RESPONSE_NETWORK_REQUEST = 5;

    @Override
    public IBinder onBind(Intent intent) {
        return (new InputStreamBinder(this));
    }

    /**
     * Handler of incoming messages from clients.
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case MSG_CREATE_NEW_ACCOUNT:
                    addNewAccount();
                    break;
                case MSG_REQUEST_NETWORK_REQUEST:
                    // TODO token check here!
                    // Validate a Token here! - Make sure, that the other app has the permission to use this functionality
                    String expectedToken = "test"; // Use the android account manager to get/verify token
                    final String accountName = msg.getData().getString("account");  // e.g. david@nextcloud.test.de
                    String token             = msg.getData().getString("token");    // token that the other app received by calling the AccountManager
                    final boolean stream     = msg.getData().getBoolean("stream");  // Do you want to stream the result?
                    final Integer port       = msg.getData().getInt("port");
                    final NextcloudRequest request = (NextcloudRequest) msg.getData().getSerializable("request");

                    if(token.equals(expectedToken)) {

                        Object result = null;
                        Exception exception = null;
                        try {
                            result = AsyncTaskHelper.executeBlockingRequest(() -> {
                                Account account = AccountUtils.getOwnCloudAccountByName(AccountManagerService.this, accountName); // TODO handle case that account is not found!
                                OwnCloudAccount ocAccount = new OwnCloudAccount(account, AccountManagerService.this);
                                OwnCloudClient client = OwnCloudClientManagerFactory.getDefaultSingleton().getClientFor(ocAccount, AccountManagerService.this);

                                //OwnCloudVersion version = AccountUtils.getServerVersion(account);
                                //client.setOwnCloudVersion(version);

                                // TODO do some checks if url is correct!! (prevent ../ in url etc..
                                request.url = client.getBaseUri() + request.url;

                                INetworkInterface network = (stream) ? new StreamingRequest(port) : new PlainRequest();

                                switch(request.method) {
                                    case "GET":
                                        GetMethod get = new GetMethod(request.url);
                                        get.setQueryString(convertMapToNVP(request.parameter));
                                        get.addRequestHeader("OCS-APIREQUEST", "true");
                                        int status = client.executeMethod(get);
                                        if(status == 200) {
                                            return network.handleGetRequest(get);
                                        } else {
                                            throw new Exception("Network error!!");
                                        }
                                    case "POST":
                                        PostMethod post = new PostMethod(request.url);
                                        post.setQueryString(convertMapToNVP(request.parameter));
                                        post.addRequestHeader("OCS-APIREQUEST", "true");

                                        if(request.requestBody != null) {
                                            StringRequestEntity requestEntity = new StringRequestEntity(
                                                    request.requestBody,
                                                    "application/json",
                                                    "UTF-8");
                                            post.setRequestEntity(requestEntity);
                                        }
                                        int status2 = client.executeMethod(post);
                                        if(status2 == 200) {
                                            return network.handlePostRequest(post);
                                        } else {
                                            throw new Exception("Network error!!");
                                        }
                                    case "PUT":
                                        PutMethod put = new PutMethod(request.url);
                                        put.setQueryString(convertMapToNVP(request.parameter));
                                        put.addRequestHeader("OCS-APIREQUEST", "true");

                                        if(request.requestBody != null) {
                                            StringRequestEntity requestEntity = new StringRequestEntity(
                                                    request.requestBody,
                                                    "application/json",
                                                    "UTF-8");
                                            put.setRequestEntity(requestEntity);
                                        }
                                        int status4 = client.executeMethod(put);
                                        if(status4 == 200) {
                                            return network.handlePutRequest(put);
                                        } else {
                                            throw new Exception("Network error!!");
                                        }

                                    case "DELETE":
                                        DeleteMethod delete = new DeleteMethod(request.url);
                                        delete.setQueryString(convertMapToNVP(request.parameter));
                                        delete.addRequestHeader("OCS-APIREQUEST", "true");
                                        int status3 = client.executeMethod(delete);
                                        if(status3 == 200) {
                                            return true;
                                        } else {
                                            throw new Exception("Network error!!");
                                        }
                                    default:
                                        throw new Exception("Unexpected type!!");
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                            exception = e;
                        }

                        try {
                            Message resp = Message.obtain(null, MSG_RESPONSE_NETWORK_REQUEST);
                            Bundle bResp = new Bundle();
                            if(!stream) {
                                bResp.putByteArray("result", (byte[]) result);
                            } else {
                                // TODO return streaming server port
                            }
                            bResp.putSerializable("exception", exception);
                            resp.setData(bResp);
                            msg.replyTo.send(resp);
                        } catch (RemoteException e) {
                            Toast.makeText(AccountManagerService.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    interface INetworkInterface {

        Object handleGetRequest(GetMethod get) throws IOException;
        Object handlePostRequest(PostMethod post) throws IOException;
        Object handlePutRequest(PutMethod put) throws IOException;
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

    public class PlainRequest implements INetworkInterface {

        @Override
        public Object handleGetRequest(GetMethod get) throws IOException {
            return get.getResponseBody();
        }

        @Override
        public Object handlePostRequest(PostMethod post) throws IOException {
            return true;
        }

        @Override
        public Object handlePutRequest(PutMethod put) throws IOException {
            return true;
        }

    }

    public class StreamingRequest implements INetworkInterface {

        private int port;

        public StreamingRequest(int port) {
            this.port = port;
        }

        @Override
        public Object handleGetRequest(GetMethod get) throws IOException {
            Log.d(TAG, "handleGetRequest() called with: get = [" + get + "]");


            Socket m_activity_socket = new Socket();
            SocketAddress sockaddr = new InetSocketAddress("127.0.0.1", port);
            m_activity_socket.connect(sockaddr, 5000);


            if (m_activity_socket.isConnected()) {
                InputStream in = get.getResponseBodyAsStream();
                OutputStream out = m_activity_socket.getOutputStream();
                // the header describes the frame data (type, width, height, length)
                // frame width and height have been previously decoded
                //byte[] header = new byte[16];
                //build_header( 1, width, height, frameData.length, header, 1 );
                //m_nos.write( header );  // message header

                IOUtils.copy(in, out);
                in.close();
                out.close();
            }
            return null;
        }

        @Override
        public Object handlePostRequest(PostMethod post) throws IOException {
            return true;
        }

        @Override
        public Object handlePutRequest(PutMethod put) throws IOException {
            return true;
        }
    }


    private void addNewAccount() {
        Intent dialogIntent = new Intent(AccountManagerService.this, FileDisplayActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // TODO make the activity start the "add new account" dialog automatically...
        startActivity(dialogIntent);
    }
}