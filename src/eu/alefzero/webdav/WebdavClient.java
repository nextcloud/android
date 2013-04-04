/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 2 of the License, or
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
import java.io.InputStream;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpStatus;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.jackrabbit.webdav.client.methods.DavMethod;
import org.apache.jackrabbit.webdav.client.methods.DeleteMethod;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;

import android.net.Uri;
import android.util.Log;

public class WebdavClient extends HttpClient {
    private Uri mUri;
    private Credentials mCredentials;
    final private static String TAG = "WebdavClient";
    private static final String USER_AGENT = "Android-ownCloud";
    
    private OnDatatransferProgressListener mDataTransferListener;
    static private byte[] sExhaustBuffer = new byte[1024];
    
    /**
     * Constructor
     */
    public WebdavClient(HttpConnectionManager connectionMgr) {
        super(connectionMgr);
        Log.d(TAG, "Creating WebdavClient");
        getParams().setParameter(HttpMethodParams.USER_AGENT, USER_AGENT);
        getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
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
     * Downloads a file in remoteFilepath to the local targetPath.
     * 
     * @param remoteFilepath    Path to the file in the remote server, URL DECODED. 
     * @param targetFile        Local path to save the downloaded file.
     * @return                  'True' when the file is successfully downloaded.
     */
    public boolean downloadFile(String remoteFilePath, File targetFile) {
        boolean ret = false;
        GetMethod get = new GetMethod(mUri.toString() + WebdavUtils.encodePath(remoteFilePath));

        try {
            int status = executeMethod(get);
            if (status == HttpStatus.SC_OK) {
                targetFile.createNewFile();
                BufferedInputStream bis = new BufferedInputStream(
                        get.getResponseBodyAsStream());
                FileOutputStream fos = new FileOutputStream(targetFile);

                byte[] bytes = new byte[4096];
                int readResult;
                while ((readResult = bis.read(bytes)) != -1) {
                    if (mDataTransferListener != null)
                        mDataTransferListener.onTransferProgress(readResult);
                    fos.write(bytes, 0, readResult);
                }
                fos.close();
                ret = true;
            } else {
                exhaustResponse(get.getResponseBodyAsStream());
            }
            Log.e(TAG, "Download of " + remoteFilePath + " to " + targetFile + " finished with HTTP status " + status + (!ret?"(FAIL)":""));
        } catch (Exception e) {
            logException(e, "dowloading " + remoteFilePath);
            
        } finally {
            if (!ret && targetFile.exists()) {
                targetFile.delete();
            }
            get.releaseConnection();    // let the connection available for other methods
        }
        return ret;
    }
    
    /**
     * Deletes a remote file via webdav
     * @param remoteFilePath       Remote file path of the file to delete, in URL DECODED format.
     * @return
     */
    public boolean deleteFile(String remoteFilePath) {
        boolean ret = false;
        DavMethod delete = new DeleteMethod(mUri.toString() + WebdavUtils.encodePath(remoteFilePath));
        try {
            int status = executeMethod(delete);
            ret = (status == HttpStatus.SC_OK || status == HttpStatus.SC_ACCEPTED || status == HttpStatus.SC_NO_CONTENT);
            exhaustResponse(delete.getResponseBodyAsStream());
            
            Log.e(TAG, "DELETE of " + remoteFilePath + " finished with HTTP status " + status +  (!ret?"(FAIL)":""));
            
        } catch (Exception e) {
            logException(e, "deleting " + remoteFilePath);
            
        } finally {
            delete.releaseConnection();    // let the connection available for other methods
        }
        return ret;
    }

    
    public void setDataTransferProgressListener(OnDatatransferProgressListener listener) {
        mDataTransferListener = listener;
    }
    
    /**
     * Creates or update a file in the remote server with the contents of a local file.
     * 
     * @param localFile         Path to the local file to upload.
     * @param remoteTarget      Remote path to the file to create or update, URL DECODED
     * @param contentType       MIME type of the file.
     * @return                  Status HTTP code returned by the server.
     * @throws IOException      When a transport error that could not be recovered occurred while uploading the file to the server.
     * @throws HttpException    When a violation of the HTTP protocol occurred. 
     */
    public int putFile(String localFile, String remoteTarget, String contentType) throws HttpException, IOException {
        int status = -1;
        PutMethod put = new PutMethod(mUri.toString() + WebdavUtils.encodePath(remoteTarget));
        
        try {
            File f = new File(localFile);
            FileRequestEntity entity = new FileRequestEntity(f, contentType);
            entity.addDatatransferProgressListener(mDataTransferListener);
            put.setRequestEntity(entity);
            status = executeMethod(put);
            
            exhaustResponse(put.getResponseBodyAsStream());
            
        } finally {
            put.releaseConnection();    // let the connection available for other methods
        }
        return status;
    }
    
    /**
     * Tries to log in to the current URI, with the current credentials
     * 
     * @return A {@link HttpStatus}-Code of the result. SC_OK is good.
     */
    public int tryToLogin() {
        int status = 0;
        HeadMethod head = new HeadMethod(mUri.toString());
        try {
            status = executeMethod(head);
            boolean result = status == HttpStatus.SC_OK;
            Log.d(TAG, "HEAD for " + mUri + " finished with HTTP status " + status + (!result?"(FAIL)":""));
            exhaustResponse(head.getResponseBodyAsStream());
            
        } catch (Exception e) {
            logException(e, "trying to login at " + mUri.toString());
            
        } finally {
            head.releaseConnection();
        }
        return status;
    }

    /**
     * Creates a remote directory with the received path.
     * 
     * @param path      Path of the directory to create, URL DECODED
     * @return          'True' when the directory is successfully created
     */
    public boolean createDirectory(String path) {
        boolean result = false;
        int status = -1;
        MkColMethod mkcol = new MkColMethod(mUri.toString() + WebdavUtils.encodePath(path));
        try {
            Log.d(TAG, "Creating directory " + path);
            status = executeMethod(mkcol);
            Log.d(TAG, "Status returned: " + status);
            result = mkcol.succeeded();
            
            Log.d(TAG, "MKCOL to " + path + " finished with HTTP status " + status + (!result?"(FAIL)":""));
            exhaustResponse(mkcol.getResponseBodyAsStream());
            
        } catch (Exception e) {
            logException(e, "creating directory " + path);
            
        } finally {
            mkcol.releaseConnection();    // let the connection available for other methods
        }
        return result;
    }
    
    
    /**
     * Check if a file exists in the OC server
     * 
     * @return              'true' if the file exists; 'false' it doesn't exist
     * @throws  Exception   When the existence could not be determined
     */
    public boolean existsFile(String path) throws IOException, HttpException {
        HeadMethod head = new HeadMethod(mUri.toString() + WebdavUtils.encodePath(path));
        try {
            int status = executeMethod(head);
            Log.d(TAG, "HEAD to " + path + " finished with HTTP status " + status + ((status != HttpStatus.SC_OK)?"(FAIL)":""));
            exhaustResponse(head.getResponseBodyAsStream());
            return (status == HttpStatus.SC_OK);
            
        } finally {
            head.releaseConnection();    // let the connection available for other methods
        }
    }


    /**
     * Requests the received method with the received timeout (milliseconds).
     * 
     * Executes the method through the inherited HttpClient.executedMethod(method).
     * 
     * Sets the socket and connection timeouts only for the method received.
     * 
     * The timeouts are both in milliseconds; 0 means 'infinite'; < 0 means 'do not change the default'
     * 
     * @param method            HTTP method request.
     * @param readTimeout       Timeout to set for data reception
     * @param conntionTimout    Timeout to set for connection establishment
     */
    public int executeMethod(HttpMethodBase method, int readTimeout, int connectionTimeout) throws HttpException, IOException {
        int oldSoTimeout = getParams().getSoTimeout();
        int oldConnectionTimeout = getHttpConnectionManager().getParams().getConnectionTimeout();
        try {
            if (readTimeout >= 0) { 
                method.getParams().setSoTimeout(readTimeout);   // this should be enough...
                getParams().setSoTimeout(readTimeout);          // ... but this looks like necessary for HTTPS
            }
            if (connectionTimeout >= 0) {
                getHttpConnectionManager().getParams().setConnectionTimeout(connectionTimeout);
            }
            return executeMethod(method);
        } finally {
            getParams().setSoTimeout(oldSoTimeout);
            getHttpConnectionManager().getParams().setConnectionTimeout(oldConnectionTimeout);
        }
    }

    /**
     * Exhausts a not interesting HTTP response. Encouraged by HttpClient documentation.
     * 
     * @param responseBodyAsStream      InputStream with the HTTP response to exhaust.
     */
    public void exhaustResponse(InputStream responseBodyAsStream) {
        if (responseBodyAsStream != null) {
            try {
                while (responseBodyAsStream.read(sExhaustBuffer) >= 0);
                responseBodyAsStream.close();
            
            } catch (IOException io) {
                Log.e(TAG, "Unexpected exception while exhausting not interesting HTTP response; will be IGNORED", io);
            }
        }
    }


    /**
     * Logs an exception triggered in a HTTP request. 
     * 
     * @param e         Caught exception.
     * @param doing     Suffix to add at the end of the logged message.
     */
    private void logException(Exception e, String doing) {
        if (e instanceof HttpException) {
            Log.e(TAG, "HTTP violation while " + doing, e);

        } else if (e instanceof IOException) {
            Log.e(TAG, "Unrecovered transport exception while " + doing, e);

        } else {
            Log.e(TAG, "Unexpected exception while " + doing, e);
        }
    }

    
    /**
     * Sets the connection and wait-for-data timeouts to be applied by default to the methods performed by this client.
     */
    public void setDefaultTimeouts(int defaultDataTimeout, int defaultConnectionTimeout) {
            getParams().setSoTimeout(defaultDataTimeout);
            getHttpConnectionManager().getParams().setConnectionTimeout(defaultConnectionTimeout);
    }

    /**
     * Sets the base URI for the helper methods that receive paths as parameters, instead of full URLs
     * @param uri
     */
    public void setBaseUri(Uri uri) {
        mUri = uri;
    }

    public Uri getBaseUri() {
        return mUri;
    }

}
