/* ownCloud Android client application
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

package com.owncloud.android.operations;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.util.Log;

import com.owncloud.android.AccountUtils;
import com.owncloud.android.authenticator.AccountAuthenticator;
import com.owncloud.android.operations.RemoteOperationResult.ResultCode;
import com.owncloud.android.utils.OwnCloudVersion;

import eu.alefzero.webdav.WebdavClient;

/**
 * Remote operation that checks the version of an ownCloud server and stores it locally
 * 
 * @author David A. Velasco
 */
public class UpdateOCVersionOperation extends RemoteOperation {

    private static final String TAG = UpdateOCVersionOperation.class.getSimpleName();

    private Account mAccount;
    private Context mContext;
    
    
    public UpdateOCVersionOperation(Account account, Context context) {
        mAccount = account;
        mContext = context;
    }
    
    
    @Override
    protected RemoteOperationResult run(WebdavClient client) {
        AccountManager accountMngr = AccountManager.get(mContext); 
        String statUrl = accountMngr.getUserData(mAccount, AccountAuthenticator.KEY_OC_BASE_URL);
        statUrl += AccountUtils.STATUS_PATH;
        RemoteOperationResult result = null;
        GetMethod get = null;
        try {
            get = new GetMethod(statUrl);
            int status = client.executeMethod(get);
            if (status != HttpStatus.SC_OK) {
                client.exhaustResponse(get.getResponseBodyAsStream());
                result = new RemoteOperationResult(false, status);
                
            } else {
                String response = get.getResponseBodyAsString();
                if (response != null) {
                    JSONObject json = new JSONObject(response);
                    if (json != null && json.getString("version") != null) {
                        OwnCloudVersion ocver = new OwnCloudVersion(json.getString("version"));
                        if (ocver.isVersionValid()) {
                            accountMngr.setUserData(mAccount, AccountAuthenticator.KEY_OC_VERSION, ocver.toString());
                            Log.d(TAG, "Got new OC version " + ocver.toString());
                            result = new RemoteOperationResult(ResultCode.OK);
                            
                        } else {
                            Log.w(TAG, "Invalid version number received from server: " + json.getString("version"));
                            result = new RemoteOperationResult(RemoteOperationResult.ResultCode.BAD_OC_VERSION);
                        }
                    }
                }
                if (result == null) {
                    result = new RemoteOperationResult(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);
                }
            }
            Log.i(TAG, "Check for update of ownCloud server version at " + client.getBaseUri() + ": " + result.getLogMessage());
            
        } catch (JSONException e) {
            result = new RemoteOperationResult(RemoteOperationResult.ResultCode.INSTANCE_NOT_CONFIGURED);
            Log.e(TAG, "Check for update of ownCloud server version at " + client.getBaseUri() + ": " + result.getLogMessage(), e);
                
        } catch (Exception e) {
            result = new RemoteOperationResult(e);
            Log.e(TAG, "Check for update of ownCloud server version at " + client.getBaseUri() + ": " + result.getLogMessage(), e);
            
        } finally {
            if (get != null) 
                get.releaseConnection();
        }
        return result;
    }

}
