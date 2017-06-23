/**
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   Copyright (C) 2015 ownCloud Inc.
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

package com.owncloud.android.authentication;

/** 
 * Constant values for OAuth 2 protocol.
 * 
 * Includes required and optional parameter NAMES used in the 'authorization code' grant type.
 */

public class OAuth2Constants {
    
    /// Parameters to send to the Authorization Endpoint
    public static final String KEY_RESPONSE_TYPE = "response_type";
    public static final String KEY_REDIRECT_URI = "redirect_uri";
    public static final String KEY_CLIENT_ID = "client_id";
    public static final String KEY_SCOPE = "scope";
    public static final String KEY_STATE = "state"; 
    
    /// Additional parameters to send to the Token Endpoint
    public static final String KEY_GRANT_TYPE = "grant_type";
    public static final String KEY_CODE = "code";
    
    /// Parameters received in an OK response from the Token Endpoint 
    public static final String KEY_ACCESS_TOKEN = "access_token";
    public static final String KEY_TOKEN_TYPE = "token_type";
    public static final String KEY_EXPIRES_IN = "expires_in";
    public static final String KEY_REFRESH_TOKEN = "refresh_token";
    
    /// Parameters in an ERROR response
    public static final String KEY_ERROR = "error";
    public static final String KEY_ERROR_DESCRIPTION = "error_description";
    public static final String KEY_ERROR_URI = "error_uri";
    public static final String VALUE_ERROR_ACCESS_DENIED = "access_denied";
    
}
