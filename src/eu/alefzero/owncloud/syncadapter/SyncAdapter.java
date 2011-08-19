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

package eu.alefzero.owncloud.syncadapter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.webdav.HttpPropFind;
import eu.alefzero.webdav.TreeNode;
import eu.alefzero.webdav.WebdavUtils;
import eu.alefzero.webdav.TreeNode.NodeProperty;

/**
 * SyncAdapter implementation for syncing sample SyncAdapter contacts to the
 * platform ContactOperations provider.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "SyncAdapter";

    private final AccountManager mAccountManager;
    private Account mAccount;
    private ContentProviderClient mContentProvider;
    private final Context mContext;

    private Date mLastUpdated;

    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContext = context;
        mAccountManager = AccountManager.get(context);
    }
    
    @Override
    public synchronized void onPerformSync(Account account, Bundle extras, String authority,
        ContentProviderClient provider, SyncResult syncResult) {
      mAccount = account;
      mContentProvider = provider;
      try {
        String username = account.name.split("@")[0];
        String password = mAccountManager.blockingGetAuthToken(account, AccountAuthenticator.AUTH_TOKEN_TYPE, true);
        if (mAccountManager.getUserData(account, AccountAuthenticator.KEY_OC_URL) == null) {
          throw new UnknownHostException();
        }
        Uri uri = Uri.parse(mAccountManager.getUserData(account, AccountAuthenticator.KEY_OC_URL));
        Log.i(TAG, "Syncing owncloud account: " + account.name + " on url: " + uri.toString());
        
        DefaultHttpClient client = new DefaultHttpClient();
        client.getCredentialsProvider().setCredentials(
            new AuthScope(uri.getHost(), (uri.getPort() == -1)?80:uri.getPort()),
            new UsernamePasswordCredentials(username, password));
        client.setKeepAliveStrategy(new ConnectionKeepAliveStrategy() {
          public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            // TODO: change keep alive straategy basing on response: ie forbidden/not found/etc
            // should have keep alive 0
            // default return: 5s
            return 5 * 1000;
          }
        });
        
        BasicHttpContext httpContext = new BasicHttpContext();
        BasicScheme basicAuth = new BasicScheme();
        httpContext.setAttribute("preemptive-auth", basicAuth);
        HttpHost targetHost = new HttpHost(uri.getHost(), (uri.getPort() == -1)
            ? 80
            : uri.getPort(), (uri.getScheme() == "https") ? "https" : "http");
        
        HttpPropFind query = new HttpPropFind(uri.toString());
        query.setHeader("Content-type", "text/xml");
        query.setHeader("User-Agent", "Android-ownCloud");
        HttpEntity entity = new StringEntity(WebdavUtils.prepareXmlForPropFind());
        query.setEntity(entity);
        HttpResponse response = client.execute(targetHost, query, httpContext);
        /*try {
          mContentProvider.delete(ProviderTableMeta.CONTENT_URI,
                                  ProviderTableMeta.FILE_NAME + " LIKE '%' AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER +"=?"
                                  , new String[]{account.name});
        } catch (RemoteException e) {
          e.printStackTrace();
          return;
        }*/
        TreeNode root = new TreeNode();
        root.setProperty(TreeNode.NodeProperty.NAME, "/");
        parseResponse(response, uri, client, targetHost, httpContext, root.getChildList());
        
        commitToDatabase(root, null);
        
      } catch (OperationCanceledException e) {
        e.printStackTrace();
      } catch (AuthenticatorException e) {
        syncResult.stats.numAuthExceptions++;
        e.printStackTrace();
      } catch (IOException e) {
        syncResult.stats.numIoExceptions++;
        e.printStackTrace();
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    }
    
    private void commitToDatabase(TreeNode root, String parentId) throws RemoteException {
      for (TreeNode n : root.getChildList()) {
        Log.d(TAG, n.toString());
        ContentValues cv = new ContentValues();
        cv.put(ProviderTableMeta.FILE_CONTENT_LENGTH, n.getProperty(NodeProperty.CONTENT_LENGTH));
        cv.put(ProviderTableMeta.FILE_MODIFIED, n.getProperty(NodeProperty.LAST_MODIFIED_DATE));
        cv.put(ProviderTableMeta.FILE_CONTENT_TYPE, n.getProperty(NodeProperty.RESOURCE_TYPE));
        cv.put(ProviderTableMeta.FILE_PARENT, parentId);
        
        String name = n.getProperty(NodeProperty.NAME),
               path = n.getProperty(NodeProperty.PATH);
        Cursor c = mContentProvider.query(ProviderTableMeta.CONTENT_URI_FILE,
                                          null,
                                          ProviderTableMeta.FILE_NAME+"=? AND " + ProviderTableMeta.FILE_PATH + "=? AND " + ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                                          new String[]{name, path, mAccount.name},
                                          null);
        if (c.moveToFirst()) {
          mContentProvider.update(ProviderTableMeta.CONTENT_URI,
                                  cv,
                                  ProviderTableMeta._ID+"=?",
                                  new String[]{c.getString(c.getColumnIndex(ProviderTableMeta._ID))});
          Log.d(TAG, "ID of: "+name+":"+c.getString(c.getColumnIndex(ProviderTableMeta._ID)));
        } else {
          cv.put(ProviderTableMeta.FILE_NAME, n.getProperty(NodeProperty.NAME));
          cv.put(ProviderTableMeta.FILE_PATH, n.getProperty(NodeProperty.PATH));
          cv.put(ProviderTableMeta.FILE_ACCOUNT_OWNER, mAccount.name);
          Uri entry = mContentProvider.insert(ProviderTableMeta.CONTENT_URI_FILE, cv);
          Log.d(TAG, "Inserting new entry " + path + name);
          c = mContentProvider.query(entry, null, null, null, null);
          c.moveToFirst();
        }
        if (n.getProperty(NodeProperty.RESOURCE_TYPE).equals("DIR")) {
          commitToDatabase(n, c.getString(c.getColumnIndex(ProviderTableMeta._ID)));
        }
      }
      // clean removed files
      String[] selection = new String[root.getChildList().size()+2];
      selection[0] = mAccount.name;
      selection[1] = parentId;
      String qm = "";
      for (int i = 2; i < selection.length-1; ++i) {
        qm += "?,";
        selection[i] = root.getChildList().get(i-2).getProperty(NodeProperty.NAME);
      }
      if (selection.length >= 3) {
        selection[selection.length-1] = root.getChildrenNames()[selection.length-3];
        qm += "?";
      }
      for (int i = 0; i < selection.length; ++i) {
        Log.d(TAG,selection[i]+"");
      }
      Log.d(TAG,"Removing files "+ parentId);
      mContentProvider.delete(ProviderTableMeta.CONTENT_URI,
                              ProviderTableMeta.FILE_ACCOUNT_OWNER+"=? AND " + ProviderTableMeta.FILE_PARENT + (parentId==null?" IS ":"=")+"? AND " + ProviderTableMeta.FILE_NAME + " NOT IN ("+qm+")",
                              selection);
    }

    private void parseResponse(HttpResponse resp, Uri uri, DefaultHttpClient client, HttpHost targetHost, BasicHttpContext httpContext, LinkedList<TreeNode> insertList) throws IOException {
      boolean skipFirst = true;
      for (TreeNode n :WebdavUtils.parseResponseToNodes(resp.getEntity().getContent())) {
        String path = n.stripPathFromFilename(uri.getPath());
        if (skipFirst) {
          skipFirst = false;
          continue;
        }
        insertList.add(n);
        
        if (!TextUtils.isEmpty(n.getProperty(NodeProperty.NAME)) &&
            n.getProperty(NodeProperty.RESOURCE_TYPE).equals("DIR")) {
          HttpPropFind method = new HttpPropFind(uri.getPath() + path + n.getProperty(NodeProperty.NAME).replace(" ", "%20") + "/");
          Log.i(TAG, uri.getPath() + path + n.getProperty(NodeProperty.NAME).replace(" ", "%20") + "/");
          Log.i(TAG, method.getRequestLine().toString());
          HttpResponse response = client.execute(targetHost, method, httpContext);
          parseResponse(response, uri, client, targetHost, httpContext, n.getChildList());
        }
      }
    }
}
