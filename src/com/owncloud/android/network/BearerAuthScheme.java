/* ownCloud Android client application
 *   Copyright (C) 2012  ownCloud Inc.
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

package com.owncloud.android.network;

import java.util.Map;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.auth.AuthChallengeParser;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.auth.AuthenticationException;
import org.apache.commons.httpclient.auth.InvalidCredentialsException;
import org.apache.commons.httpclient.auth.MalformedChallengeException;

import com.owncloud.android.Log_OC;

/**
 * Bearer authentication scheme as defined in RFC 6750.
 * 
 * @author David A. Velasco
 */

public class BearerAuthScheme implements AuthScheme /*extends RFC2617Scheme*/ {
    
    private static final String TAG = BearerAuthScheme.class.getSimpleName();

    public static final String AUTH_POLICY = "Bearer";
    
    /** Whether the bearer authentication process is complete */
    private boolean mComplete;
    
    /** Authentication parameter map */
    private Map mParams = null;
    
    
    /**
     * Default constructor for the bearer authentication scheme.
     */
    public BearerAuthScheme() {
        mComplete = false;
    }

    /**
     * Constructor for the basic authentication scheme.
     * 
     * @param   challenge                       Authentication challenge
     * 
     * @throws  MalformedChallengeException     Thrown if the authentication challenge is malformed
     * 
     * @deprecated Use parameterless constructor and {@link AuthScheme#processChallenge(String)} method
     */
    public BearerAuthScheme(final String challenge) throws MalformedChallengeException {
        processChallenge(challenge);
        mComplete = true;
    }

    /**
     * Returns textual designation of the bearer authentication scheme.
     * 
     * @return "Bearer"
     */
    public String getSchemeName() {
        return "bearer";
    }

    /**
     * Processes the Bearer challenge.
     *  
     * @param   challenge                   The challenge string
     * 
     * @throws MalformedChallengeException  Thrown if the authentication challenge is malformed
     */
    public void processChallenge(String challenge) throws MalformedChallengeException {
        String s = AuthChallengeParser.extractScheme(challenge);
        if (!s.equalsIgnoreCase(getSchemeName())) {
            throw new MalformedChallengeException(
              "Invalid " + getSchemeName() + " challenge: " + challenge); 
        }
        mParams = AuthChallengeParser.extractParams(challenge);
        mComplete = true;
    }

    /**
     * Tests if the Bearer authentication process has been completed.
     * 
     * @return 'true' if Bearer authorization has been processed, 'false' otherwise.
     */
    public boolean isComplete() {
        return this.mComplete;
    }

    /**
     * Produces bearer authorization string for the given set of 
     * {@link Credentials}.
     * 
     * @param   credentials                     The set of credentials to be used for authentication
     * @param   method                          Method name is ignored by the bearer authentication scheme
     * @param   uri                             URI is ignored by the bearer authentication scheme
     * @throws  InvalidCredentialsException     If authentication credentials are not valid or not applicable 
     *                                          for this authentication scheme
     * @throws  AuthenticationException         If authorization string cannot be generated due to an authentication failure
     * @return  A bearer authorization string
     * 
     * @deprecated Use {@link #authenticate(Credentials, HttpMethod)}
     */
    public String authenticate(Credentials credentials, String method, String uri) throws AuthenticationException {
        Log_OC.d(TAG, "enter BearerScheme.authenticate(Credentials, String, String)");

        BearerCredentials bearer = null;
        try {
            bearer = (BearerCredentials) credentials;
        } catch (ClassCastException e) {
            throw new InvalidCredentialsException(
             "Credentials cannot be used for bearer authentication: " 
              + credentials.getClass().getName());
        }
        return BearerAuthScheme.authenticate(bearer);
    }

    
    /**
     * Returns 'false'. Bearer authentication scheme is request based.
     * 
     * @return 'false'.
     */
    public boolean isConnectionBased() {
        return false;    
    }

    /**
     * Produces bearer authorization string for the given set of {@link Credentials}.
     * 
     * @param   credentials                     The set of credentials to be used for authentication
     * @param   method                          The method being authenticated
     * @throws  InvalidCredentialsException     If authentication credentials are not valid or not applicable for this authentication 
     *                                          scheme.
     * @throws AuthenticationException         If authorization string cannot be generated due to an authentication failure.
     * 
     * @return a basic authorization string
     */
    public String authenticate(Credentials credentials, HttpMethod method) throws AuthenticationException {
        Log_OC.d(TAG, "enter BearerScheme.authenticate(Credentials, HttpMethod)");

        if (method == null) {
            throw new IllegalArgumentException("Method may not be null");
        }
        BearerCredentials bearer = null;
        try {
            bearer = (BearerCredentials) credentials;
        } catch (ClassCastException e) {
            throw new InvalidCredentialsException(
                    "Credentials cannot be used for bearer authentication: " 
                    + credentials.getClass().getName());
        }
        return BearerAuthScheme.authenticate(
            bearer, 
            method.getParams().getCredentialCharset());
    }
    
    /**
     * @deprecated Use {@link #authenticate(BearerCredentials, String)}
     * 
     * Returns a bearer Authorization header value for the given 
     * {@link BearerCredentials}.
     * 
     * @param   credentials     The credentials to encode.
     * 
     * @return                  A bearer authorization string
     */
    public static String authenticate(BearerCredentials credentials) {
        return authenticate(credentials, "ISO-8859-1");
    }

    /**
     * Returns a bearer Authorization header value for the given 
     * {@link BearerCredentials} and charset.
     * 
     * @param   credentials         The credentials to encode.
     * @param   charset             The charset to use for encoding the credentials
     * 
     * @return                      A bearer authorization string
     * 
     * @since 3.0
     */
    public static String authenticate(BearerCredentials credentials, String charset) {
        Log_OC.d(TAG, "enter BearerAuthScheme.authenticate(BearerCredentials, String)");

        if (credentials == null) {
            throw new IllegalArgumentException("Credentials may not be null"); 
        }
        if (charset == null || charset.length() == 0) {
            throw new IllegalArgumentException("charset may not be null or empty");
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append(credentials.getAccessToken());
        
        //return "Bearer " + EncodingUtil.getAsciiString(EncodingUtil.getBytes(buffer.toString(), charset));
        return "Bearer " + buffer.toString();
    }

    /**
     * Returns a String identifying the authentication challenge.  This is
     * used, in combination with the host and port to determine if
     * authorization has already been attempted or not.  Schemes which
     * require multiple requests to complete the authentication should
     * return a different value for each stage in the request.
     * 
     * Additionally, the ID should take into account any changes to the
     * authentication challenge and return a different value when appropriate.
     * For example when the realm changes in basic authentication it should be
     * considered a different authentication attempt and a different value should
     * be returned.
     * 
     * This method simply returns the realm for the challenge.
     * 
     * @return String       a String identifying the authentication challenge.
     * 
     * @deprecated no longer used
     */
    @Override
    public String getID() {
        return getRealm();
    }

    /**
     * Returns authentication parameter with the given name, if available.
     * 
     * @param   name    The name of the parameter to be returned
     * 
     * @return          The parameter with the given name
     */
    @Override
    public String getParameter(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Parameter name may not be null"); 
        }
        if (mParams == null) {
            return null;
        }
        return (String) mParams.get(name.toLowerCase());
    }

    /**
     * Returns authentication realm. The realm may not be null.
     * 
     * @return  The authentication realm
     */
    @Override
    public String getRealm() {
        return getParameter("realm");
    }
    
}
