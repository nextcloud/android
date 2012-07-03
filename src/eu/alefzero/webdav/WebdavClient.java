/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package eu.alefzero.webdav;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.authenticator.EasySSLSocketFactory;
import eu.alefzero.owncloud.files.interfaces.OnDatatransferProgressListener;
import eu.alefzero.owncloud.utils.OwnCloudVersion;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

public class WebdavClient extends HttpClient {
    private Uri mUri;
    private Credentials mCredentials;
    final private static String TAG = "WebdavClient";
    private static final String USER_AGENT = "Android-ownCloud";
    private OnDatatransferProgressListener mDataTransferListener;
    private static HashMap<String, WebdavClient> clients = new HashMap<String, WebdavClient>();
    
    /**
     * Gets a WebdavClient setup for the current account
     * @param account The client accout
     * @param context The application context
     * @return
     */
    public static synchronized WebdavClient getInstance(Account account, Context context){
        WebdavClient instance = clients.get(account.name);
        if(instance == null ){
            OwnCloudVersion ownCloudVersion = new OwnCloudVersion(AccountManager.get(context).getUserData(account,
                    AccountAuthenticator.KEY_OC_VERSION));
            String baseUrl = AccountManager.get(context).getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL);
            String webDavPath = AccountUtils.getWebdavPath(ownCloudVersion);
            WebdavClient client = new WebdavClient();
            
            String username = account.name.substring(0, account.name.indexOf('@'));
            String password = AccountManager.get(context).getPassword(account);
            
            client.mUri = Uri.parse(baseUrl + webDavPath);
            client.getParams().setParameter(HttpMethodParams.USER_AGENT, USER_AGENT);
            client.setCredentials(username, password);
            client.allowSelfsignedCertificates();
            clients.put(account.name, client);
        }
        return instance;
    }

    public void setCredentials(String username, String password) {
        getParams().setAuthenticationPreemptive(true);
        getState().setCredentials(AuthScope.ANY,
                getCredentials(username, password));
    }

    private Credentials getCredentials(String username, String password) {
        if (mCredentials == null)
            mCredentials = new UsernamePasswordCredentials(username, password);
        return mCredentials;
    }

    public void allowSelfsignedCertificates() {
        // https
        Protocol.registerProtocol("https", new Protocol("https",
                new EasySSLSocketFactory(), 443));
    }

    public boolean downloadFile(String remoteFilepath, File targetPath) {
        // HttpGet get = new HttpGet(mUri.toString() + filepath.replace(" ",
        // "%20"));
        /* dvelasco - this is not necessary anymore; OCFile.mRemotePath (the origin of remoteFielPath) keeps valid URL strings
        String[] splitted_filepath = remoteFilepath.split("/");
        remoteFilepath = "";
        for (String s : splitted_filepath) {
            if (s.equals("")) continue;
            remoteFilepath += "/" + URLEncoder.encode(s);
        }

        Log.e("ASD", mUri.toString() + remoteFilepath.replace(" ", "%20") + "");
        GetMethod get = new GetMethod(mUri.toString()
                + remoteFilepath.replace(" ", "%20"));
        */
        GetMethod get = new GetMethod(mUri.toString() + remoteFilepath);

        // get.setHeader("Host", mUri.getHost());
        // get.setHeader("User-Agent", "Android-ownCloud");

        try {
            int status = executeMethod(get);
            Log.e(TAG, "status return: " + status);
            if (status != HttpStatus.SC_OK) {
                return false;
            }
            BufferedInputStream bis = new BufferedInputStream(
                    get.getResponseBodyAsStream());
            FileOutputStream fos = new FileOutputStream(targetPath);

            byte[] bytes = new byte[4096];
            int readResult;
            while ((readResult = bis.read(bytes)) != -1) {
                if (mDataTransferListener != null)
                    mDataTransferListener.transferProgress(readResult);
                fos.write(bytes, 0, readResult);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    /**
     * Deletes a remote file via webdav
     * @param remoteFilePath
     * @return
     */
    public boolean deleteFile(String remoteFilePath){
        DavMethod delete = new DeleteMethod(mUri.toString() + remoteFilePath);
        try {
            executeMethod(delete);
        }  catch (IOException e) {
            Log.e(TAG, "Logging failed with error: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    public void setDataTransferProgressListener(OnDatatransferProgressListener listener) {
        mDataTransferListener = listener;
    }
    
    public boolean putFile(String localFile, String remoteTarget,
            String contentType) {
        boolean result = true;

        try {
            Log.e("ASD", contentType + "");
            File f = new File(localFile);
            FileRequestEntity entity = new FileRequestEntity(f, contentType);
            entity.setOnDatatransferProgressListener(mDataTransferListener);
            Log.e("ASD", f.exists() + " " + entity.getContentLength());
            PutMethod put = new PutMethod(mUri.toString() + remoteTarget);
            put.setRequestEntity(entity);
            Log.d(TAG, "" + put.getURI().toString());
            int status = executeMethod(put);
            Log.d(TAG, "PUT method return with status " + status);

            Log.i(TAG, "Uploading, done");
        } catch (final Exception e) {
            Log.i(TAG, "" + e.getMessage());
            result = false;
        }

        return result;
    }

    public int tryToLogin() {
        int r = 0;
        HeadMethod head = new HeadMethod(mUri.toString());
        try {
            r = executeMethod(head);
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
        return r;
    }

    public boolean createDirectory(String path) {
        try {
            MkColMethod mkcol = new MkColMethod(mUri.toString() + path);
            int status = executeMethod(mkcol);
            Log.d(TAG, "Status returned " + status);
            Log.d(TAG, "uri: " + mkcol.getURI().toString());
            Log.i(TAG, "Creating dir completed");
        } catch (final Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
