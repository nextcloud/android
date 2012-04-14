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
import java.util.Date;
import java.util.LinkedList;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
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
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.webdav.HttpPropFind;
import eu.alefzero.webdav.TreeNode;
import eu.alefzero.webdav.TreeNode.NodeProperty;
import eu.alefzero.webdav.WebdavClient;
import eu.alefzero.webdav.WebdavUtils;

/**
 * Base SyncAdapter for OwnCloud Designed to be subclassed for the concrete
 * SyncAdapter, like ConcatsSync, CalendarSync, FileSync etc..
 * 
 * @author sassman
 * 
 */
public abstract class AbstractOwnCloudSyncAdapter extends
		AbstractThreadedSyncAdapter {

	private AccountManager accountManager;
	private Account account;
	private ContentProviderClient contentProvider;
	private Date lastUpdated;

	private HttpHost mHost;
	private WebdavClient mClient = null;
	private static String TAG = "AbstractOwnCloudSyncAdapter";

	public AbstractOwnCloudSyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		this.setAccountManager(AccountManager.get(context));
	}

	public AccountManager getAccountManager() {
		return accountManager;
	}

	public void setAccountManager(AccountManager accountManager) {
		this.accountManager = accountManager;
	}

	public Account getAccount() {
		return account;
	}

	public void setAccount(Account account) {
		this.account = account;
	}

	public ContentProviderClient getContentProvider() {
		return contentProvider;
	}

	public void setContentProvider(ContentProviderClient contentProvider) {
		this.contentProvider = contentProvider;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	protected ConnectionKeepAliveStrategy getKeepAliveStrategy() {
		return new ConnectionKeepAliveStrategy() {
			public long getKeepAliveDuration(HttpResponse response,
					HttpContext context) {
				// Change keep alive straategy basing on response: ie
				// forbidden/not found/etc
				// should have keep alive 0
				// default return: 5s
				int statusCode = response.getStatusLine().getStatusCode();

				// HTTP 400, 500 Errors as well as HTTP 118 - Connection timed
				// out
				if ((statusCode >= 400 && statusCode <= 418)
						|| (statusCode >= 421 && statusCode <= 426)
						|| (statusCode >= 500 && statusCode <= 510)
						|| statusCode == 118) {
					return 0;
				}

				return 5 * 1000;
			}
		};
	}

	protected HttpPropFind getPropFindQuery()
			throws OperationCanceledException, AuthenticatorException,
			IOException {
		HttpPropFind query = new HttpPropFind(getUri().toString());
		query.setHeader("Content-type", "text/xml");
		query.setHeader("User-Agent", "Android-ownCloud");
		return query;
	}

	protected HttpResponse fireRawRequest(HttpRequest query)
			throws ClientProtocolException, OperationCanceledException,
			AuthenticatorException, IOException {
		BasicHttpContext httpContext = new BasicHttpContext();
		BasicScheme basicAuth = new BasicScheme();
		httpContext.setAttribute("preemptive-auth", basicAuth);

		HttpResponse response = getClient().execute(mHost, query, httpContext);
		return response;
	}

	protected TreeNode fireRequest(HttpRequest query)
			throws ClientProtocolException, OperationCanceledException,
			AuthenticatorException, IOException {
		HttpResponse response = fireRawRequest(query);

		TreeNode root = new TreeNode();
		root.setProperty(TreeNode.NodeProperty.NAME, "");
		this.parseResponse(response, getUri(), getClient(), mHost,
				root.getChildList(), false, 0);
		return root;
	}

	protected Uri getUri() {
		return Uri.parse(this.getAccountManager().getUserData(getAccount(),
				AccountAuthenticator.KEY_OC_URL));
	}

	private DefaultHttpClient getClient() throws OperationCanceledException,
			AuthenticatorException, IOException {
		if (mClient == null) {
			String username = getAccount().name.split("@")[0];
			String password = this.getAccountManager().blockingGetAuthToken(
					getAccount(), AccountAuthenticator.AUTH_TOKEN_TYPE, true);
			if (this.getAccountManager().getUserData(getAccount(),
					AccountAuthenticator.KEY_OC_URL) == null) {
				throw new UnknownHostException();
			}
			Uri uri = getUri();

			mClient = new WebdavClient(uri);
			mClient.setCredentials(username, password);
			mClient.allowUnsignedCertificates();
			mHost = mClient.getTargetHost();
		}

		return mClient.getHttpClient();
	}

	private void parseResponse(HttpResponse resp, Uri uri,
			DefaultHttpClient client, HttpHost targetHost,
			LinkedList<TreeNode> insertList, boolean sf, long parent_id)
			throws IOException, OperationCanceledException,
			AuthenticatorException {
		boolean skipFirst = sf, override_parent = !sf;
		for (TreeNode n : WebdavUtils.parseResponseToNodes(resp.getEntity()
				.getContent())) {
			if (skipFirst) {
				skipFirst = false;
				continue;
			}
			String path = n.stripPathFromFilename(uri.getPath());

			long mod = n.getProperty(NodeProperty.LAST_MODIFIED_DATE) == null ? 0
					: Long.parseLong(n
							.getProperty(NodeProperty.LAST_MODIFIED_DATE));
			OCFile file = new OCFile(getContentProvider(), getAccount(),
					n.getProperty(NodeProperty.PATH));
			if (file.fileExtist() && file.getModificationTimestamp() >= mod) {
				Log.d(TAG, "No update for file/dir " + file.getFileName()
						+ " is needed");
			} else {
				Log.d(TAG, "File " + n.getProperty(NodeProperty.PATH)
						+ " will be "
						+ (file.fileExtist() ? "updated" : "created"));
				long len = n.getProperty(NodeProperty.CONTENT_LENGTH) == null ? 0
						: Long.parseLong(n
								.getProperty(NodeProperty.CONTENT_LENGTH));
				long create = n.getProperty(NodeProperty.CREATE_DATE) == null ? 0
						: Long.parseLong(n
								.getProperty(NodeProperty.CREATE_DATE));
				file = OCFile.createNewFile(getContentProvider(), getAccount(),
						n.getProperty(NodeProperty.PATH), len, create, mod,
						n.getProperty(NodeProperty.RESOURCE_TYPE), parent_id);
				file.save();
				if (override_parent) {
					parent_id = file.getFileId();
					override_parent = false;
				}
			}

			if (!TextUtils.isEmpty(n.getProperty(NodeProperty.NAME))
					&& n.getProperty(NodeProperty.RESOURCE_TYPE).equals("DIR")) {

				HttpPropFind method = new HttpPropFind(uri.getPath() + path
						+ n.getProperty(NodeProperty.NAME).replace(" ", "%20")
						+ "/");
				HttpResponse response = fireRawRequest(method);
				parseResponse(response, uri, client, targetHost,
						n.getChildList(), true, file.getFileId());
			}
		}
	}
}