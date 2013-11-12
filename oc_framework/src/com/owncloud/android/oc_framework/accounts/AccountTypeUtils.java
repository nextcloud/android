/* ownCloud Android client application
 *   Copyright (C) 2012  Bartek Przybylski
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

package com.owncloud.android.oc_framework.accounts;

/**
 * @author masensio
 * @author David A. Velasco
 */
public class AccountTypeUtils {

    public static String getAuthTokenTypePass(String accountType) {
        return accountType + ".password";
    }
    
    public static String getAuthTokenTypeAccessToken(String accountType) {
        return accountType  + ".oauth2.access_token";
    }
    
    public static String getAuthTokenTypeRefreshToken(String accountType) {
        return accountType  + ".oauth2.refresh_token";
    }
    
    public static String getAuthTokenTypeSamlSessionCookie(String accountType) {
        return accountType  +  ".saml.web_sso.session_cookie";
    }
    
}
