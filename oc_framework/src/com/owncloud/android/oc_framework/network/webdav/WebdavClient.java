/* ownCloud Android client application
 *   Copyright (C) 2011  Bartek Przybylski
 *   Copyright (C) 2012-2013 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
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

package com.owncloud.android.oc_framework.network.webdav;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthPolicy;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.HeadMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.http.HttpStatus;
import org.apache.http.params.CoreProtocolPNames;

import com.owncloud.android.oc_framework.network.BearerAuthScheme;
import com.owncloud.android.oc_framework.network.BearerCredentials;

import android.net.Uri;
import android.util.Log;

public class WebdavClient extends HttpClient {
    private static final int MAX_REDIRECTIONS_COUNT = 3;
    
    private Uri mUri;
    private Credentials mCredentials;
    private boolean mFollowRedirects;
    private String mSsoSessionCookie;
    final private static String TAG = WebdavClient.class.getSimpleName();
    public static final String USER_AGENT = "Android-ownCloud";
    
    static private byte[] sExhaustBuffer = new byte[1024];
    
    /**
     * Constructor
     */
    public WebdavClient(HttpConnectionManager connectionMgr) {
        super(connectionMgr);
        Log.d(TAG, "Creating WebdavClient");
        getParams().setParameter(HttpMethodParams.USER_AGENT, USER_AGENT);
        getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
        mFollowRedirects = true;
        mSsoSessionCookie = null;
    }

    public void setBearerCredentials(String accessToken) {
        AuthPolicy.registerAuthScheme(BearerAuthScheme.AUTH_POLICY, BearerAuthScheme.class);
        
        List<String> authPrefs = new ArrayList<String>(1);
        authPrefs.add(BearerAuthScheme.AUTH_POLICY);
        getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);        
        
        mCredentials = new BearerCredentials(accessToken);
        getState().setCredentials(AuthScope.ANY, mCredentials);
        mSsoSessionCookie = null;
    }

    public void setBasicCredentials(String username, String password) {
        List<String> authPrefs = new ArrayList<String>(1);
        authPrefs.add(AuthPolicy.BASIC);
        getParams().setParameter(AuthPolicy.AUTH_SCHEME_PRIORITY, authPrefs);        
        
        getParams().setAuthenticationPreemptive(true);
        mCredentials = new UsernamePasswordCredentials(username, password);
        getState().setCredentials(AuthScope.ANY, mCredentials);
        mSsoSessionCookie = null;
    }
    
    public void setSsoSessionCookie(String accessToken) {
        getParams().setAuthenticationPreemptive(false);
        getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES);
        mSsoSessionCookie = accessToken;
        mCredentials = null;
    }
    
    
    /**
     * Check if a file exists in the OC server
     * 
     * TODO replace with ExistenceOperation
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
    
    
    @Override
    public int executeMethod(HttpMethod method) throws IOException, HttpException {
        boolean customRedirectionNeeded = false;
        try {
            method.setFollowRedirects(mFollowRedirects);
        } catch (Exception e) {
            //if (mFollowRedirects) Log_OC.d(TAG, "setFollowRedirects failed for " + method.getName() + " method, custom redirection will be used if needed");
            customRedirectionNeeded = mFollowRedirects;
        }
        if (mSsoSessionCookie != null && mSsoSessionCookie.length() > 0) {
            method.setRequestHeader("Cookie", mSsoSessionCookie);
        }
        int status = super.executeMethod(method);
        int redirectionsCount = 0;
        while (customRedirectionNeeded &&
                redirectionsCount < MAX_REDIRECTIONS_COUNT &&
                (   status == HttpStatus.SC_MOVED_PERMANENTLY || 
                    status == HttpStatus.SC_MOVED_TEMPORARILY ||
                    status == HttpStatus.SC_TEMPORARY_REDIRECT)
                ) {
            
            Header location = method.getResponseHeader("Location");
            if (location != null) {
                Log.d(TAG,  "Location to redirect: " + location.getValue());
                method.setURI(new URI(location.getValue(), true));
                status = super.executeMethod(method);
                redirectionsCount++;
                
            } else {
                Log.d(TAG,  "No location to redirect!");
                status = HttpStatus.SC_NOT_FOUND;
            }
        }
        
        return status;
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

    public final Credentials getCredentials() {
        return mCredentials;
    }
    
    public final String getSsoSessionCookie() {
        return mSsoSessionCookie;
    }

    public void setFollowRedirects(boolean followRedirects) {
        mFollowRedirects = followRedirects;
    }

}
