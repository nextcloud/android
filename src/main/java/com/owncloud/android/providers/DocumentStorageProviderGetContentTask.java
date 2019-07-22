package com.owncloud.android.providers;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.DocumentsContract;

import com.evernote.android.job.JobRequest;
import com.evernote.android.job.util.Device;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.R;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.operations.RefreshFolderOperation;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DocumentStorageProviderGetContentTask extends AsyncTask<Void, Void, Boolean> {
    private final OwnCloudClient client;
    private final String parentDocumentId;
    private DocumentsStorageProvider documentsStorageProvider;
    private Context context;
    private OCFile browsedDir;
    private FileDataStorageManager fileDataStorageManager;
    private UserAccountManager accountManager;

    public DocumentStorageProviderGetContentTask(DocumentsStorageProvider documentsStorageProvider,
                                                 Context context,
                                                 OCFile browsedDir,
                                                 FileDataStorageManager fileDataStorageManager,
                                                 UserAccountManager accountManager,
                                                 OwnCloudClient client, String parentDocumentId) {
        this.documentsStorageProvider = documentsStorageProvider;
        this.context = context;
        this.browsedDir = browsedDir;
        this.fileDataStorageManager = fileDataStorageManager;
        this.accountManager = accountManager;
        this.client = client;
        this.parentDocumentId = parentDocumentId;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        documentsStorageProvider.setSyncRunning(true);
    }


    @Override
    protected Boolean doInBackground(Void... voids) {
        if (Device.getNetworkType(context).equals(JobRequest.NetworkType.UNMETERED)) {
            RemoteOperationResult result = new RefreshFolderOperation(browsedDir,
                                                                      System.currentTimeMillis(),
                                                                      false,
                                                                      false,
                                                                      true,
                                                                      fileDataStorageManager,
                                                                      accountManager.getCurrentAccount(),
                                                                      context)
                .execute(client);

            return result.isSuccess();
        } else {
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean bool) {
        super.onPostExecute(bool);

        documentsStorageProvider.setSyncRunning(false);
        String documentsAuthority = context.getResources().getString(R.string.document_provider_authority);
        Uri updatedUri = DocumentsContract.buildChildDocumentsUri(documentsAuthority, parentDocumentId);
        context.getContentResolver().notifyChange(updatedUri, null);
    }
}


