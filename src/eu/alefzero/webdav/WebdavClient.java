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

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.http.HttpStatus;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.net.Uri;
import android.util.Log;
import eu.alefzero.owncloud.AccountUtils;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.authenticator.EasySSLSocketFactory;
import eu.alefzero.owncloud.files.interfaces.OnDatatransferProgressListener;
import eu.alefzero.owncloud.utils.OwnCloudVersion;

public class WebdavClient extends HttpClient {
    private Uri mUri;
    private Credentials mCredentials;
    final private static String TAG = "WebdavClient";
    private static final String USER_AGENT = "Android-ownCloud";
    
    /** Default timeout for waiting data from the server: 10 seconds */
    public static final int DEFAULT_DATA_TIMEOUT = 10000;
    
    /** Default timeout for establishing a connection: infinite */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 0;
    
    private OnDatatransferProgressListener mDataTransferListener;
    static private MultiThreadedHttpConnectionManager mConnManager = null;
    
    static public MultiThreadedHttpConnectionManager getMultiThreadedConnManager() {
        if (mConnManager == null) {
            mConnManager = new MultiThreadedHttpConnectionManager();
            mConnManager.setMaxConnectionsPerHost(5);
            mConnManager.setMaxTotalConnections(5);
        }
        return mConnManager;
    }
    
    /**
     * Creates a WebdavClient setup for the current account
     * @param account The client accout
     * @param context The application context
     * @return
     */
    public WebdavClient (Account account, Context context) {
        setDefaultTimeouts();
        
        OwnCloudVersion ownCloudVersion = new OwnCloudVersion(AccountManager.get(context).getUserData(account,
                AccountAuthenticator.KEY_OC_VERSION));
        String baseUrl = AccountManager.get(context).getUserData(account, AccountAuthenticator.KEY_OC_BASE_URL);
        String webDavPath = AccountUtils.getWebdavPath(ownCloudVersion);        
        String username = account.name.substring(0, account.name.lastIndexOf('@'));
        String password = AccountManager.get(context).getPassword(account);
        
        mUri = Uri.parse(baseUrl + webDavPath);
        Log.e("ASD", ""+username);
        setCredentials(username, password);
    }
    
    public WebdavClient() {
        super(getMultiThreadedConnManager());
        
        setDefaultTimeouts();
        
        getParams().setParameter(HttpMethodParams.USER_AGENT, USER_AGENT);
        getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        allowSelfsignedCertificates();
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
    
    /**
     * Sets the connection and wait-for-data timeouts to be applied by default.
     */
    private void setDefaultTimeouts() {
        getParams().setSoTimeout(DEFAULT_DATA_TIMEOUT);
        getHttpConnectionManager().getParams().setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT);
    }

    public void allowSelfsignedCertificates() {
        // https
        Protocol.registerProtocol("https", new Protocol("https",
                new EasySSLSocketFactory(), 443));
    }

    /**
     * Downloads a file in remoteFilepath to the local targetPath.
     * 
     * @param remoteFilepath    Path to the file in the remote server, URL DECODED. 
     * @param targetFile        Local path to save the downloaded file.
     * @return                  'True' when the file is successfully downloaded.
     */
    public boolean downloadFile(String remoteFilepath, File targetFile) {
        boolean ret = false;
        boolean caughtException = false;
        GetMethod get = new GetMethod(mUri.toString() + WebdavUtils.encodePath(remoteFilepath));

        // get.setHeader("Host", mUri.getHost());
        // get.setHeader("User-Agent", "Android-ownCloud");

        int status = -1;
        try {
            status = executeMethod(get);
            if (status == HttpStatus.SC_OK) {
                targetFile.createNewFile();
                BufferedInputStream bis = new BufferedInputStream(
                        get.getResponseBodyAsStream());
                FileOutputStream fos = new FileOutputStream(targetFile);

                byte[] bytes = new byte[4096];
                int readResult;
                while ((readResult = bis.read(bytes)) != -1) {
                    if (mDataTransferListener != null)
                        mDataTransferListener.transferProgress(readResult);
                    fos.write(bytes, 0, readResult);
                }
                ret = true;
            }
            
        } catch (HttpException e) {
            Log.e(TAG, "HTTP exception downloading " + remoteFilepath, e);
            caughtException = true;

        } catch (IOException e) {
            Log.e(TAG, "I/O exception downloading " + remoteFilepath, e);
            caughtException = true;

        } catch (Exception e) {
            Log.e(TAG, "Unexpected exception downloading " + remoteFilepath, e);
            caughtException = true;
            
        } finally {
            if (!ret) {
                if (!caughtException) {
                    Log.e(TAG, "Download of " + remoteFilepath + " to " + targetFile + " failed with HTTP status " + status);
                }
                if (targetFile.exists()) {
                    targetFile.delete();
                }
            }
        }
        return ret;
    }
    
    /**
     * Deletes a remote file via webdav
     * @param remoteFilePath       Remote file path of the file to delete, in URL DECODED format.
     * @return
     */
    public boolean deleteFile(String remoteFilePath){
        DavMethod delete = new DeleteMethod(mUri.toString() + WebdavUtils.encodePath(remoteFilePath));
        try {
            executeMethod(delete);
        }  catch (Throwable e) {
            Log.e(TAG, "Deleting failed with error: " + e.getMessage(), e);
            return false;
        }
        return true;
    }

    public void setDataTransferProgressListener(OnDatatransferProgressListener listener) {
        mDataTransferListener = listener;
    }
    
    /**
     * Creates or update a file in the remote server with the contents of a local file.
     * 
     * 
     * @param localFile         Path to the local file to upload.
     * @param remoteTarget      Remote path to the file to create or update, URL DECODED
     * @param contentType       MIME type of the file.
     * @return                  'True' then the upload was successfully completed
     */
    public boolean putFile(String localFile, String remoteTarget,
            String contentType) {
        boolean result = false;

        try {
            Log.e("ASD", contentType + "");
            File f = new File(localFile);
            FileRequestEntity entity = new FileRequestEntity(f, contentType);
            entity.setOnDatatransferProgressListener(mDataTransferListener);
            Log.e("ASD", f.exists() + " " + entity.getContentLength());
            PutMethod put = new PutMethod(mUri.toString() + WebdavUtils.encodePath(remoteTarget));
            put.setRequestEntity(entity);
            Log.d(TAG, "" + put.getURI().toString());
            int status = executeMethod(put, 0);
            Log.d(TAG, "PUT method return with status " + status);

            if (status == HttpStatus.SC_OK || status == HttpStatus.SC_CREATED || status == HttpStatus.SC_NO_CONTENT) {
                result = true;
                Log.i(TAG, "Uploading, done");
            }
            
        } catch (final Exception e) {
            Log.i(TAG, "" + e.getMessage());
            result = false;
        }

        return result;
    }

    /**
     * Tries to log in to the given WedDavURI, with the given credentials
     * @param uri To test
     * @param username Username to check
     * @param password Password to verify
     * @return A {@link HttpStatus}-Code of the result. SC_OK is good.
     */
    public static int tryToLogin(Uri uri, String username, String password) {
        int returnCode = 0;
        WebdavClient client = new WebdavClient();
        client.setCredentials(username, password);
        HeadMethod head = new HeadMethod(uri.toString());
        try {
            returnCode = client.executeMethod(head);
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }
        return returnCode;
    }

    /**
     * Creates a remote directory with the received path.
     * 
     * @param path      Path of the directory to create, URL DECODED
     * @return          'True' when the directory is successfully created
     */
    public boolean createDirectory(String path) {
        try {
            MkColMethod mkcol = new MkColMethod(mUri.toString() + WebdavUtils.encodePath(path));
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
    
    
    /**
     * Check if a file exists in the OC server
     * 
     * @return      'Boolean.TRUE' if the file exists; 'Boolean.FALSE' it doesn't exist; NULL if couldn't be checked
     */
    public Boolean existsFile(String path) {
        try {
            HeadMethod head = new HeadMethod(mUri.toString() + WebdavUtils.encodePath(path));
            int status = executeMethod(head);
            return (status == HttpStatus.SC_OK);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Requests the received method with the received timeout (milliseconds).
     * 
     * Executes the method through the inherited HttpClient.executedMethod(method).
     * 
     * Sets the socket timeout for the HttpMethodBase method received.
     * 
     * @param method    HTTP method request.
     * @param timeout   Timeout to set, in milliseconds; <= 0 means infinite.
     */
    public int executeMethod(HttpMethodBase method, int readTimeout) throws HttpException, IOException {
        int oldSoTimeout = getParams().getSoTimeout();
        try {
            if (readTimeout < 0) { 
                readTimeout = 0;
            }
            HttpMethodParams params = method.getParams();
            params.setSoTimeout(readTimeout);       
            method.setParams(params);               // this should be enough...
            getParams().setSoTimeout(readTimeout);  // ... but this is necessary for HTTPS
            return executeMethod(method);
        } finally {
            getParams().setSoTimeout(oldSoTimeout);
        }
    }
}
