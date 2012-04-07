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
package eu.alefzero.owncloud.ui.fragment;

import java.util.Stack;
import java.util.Vector;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.app.DownloadManager.Query;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.owncloud.ui.FragmentListView;
import eu.alefzero.owncloud.ui.activity.FileDetailActivity;
import eu.alefzero.owncloud.ui.activity.FileDisplayActivity;
import eu.alefzero.owncloud.ui.adapter.FileListListAdapter;

/**
 * A Fragment that lists all files and folders in a given path.
 * @author Bartek Przybylski
 *
 */
public class FileList extends FragmentListView {
  private Account mAccount;
  private AccountManager mAccountManager;
  private Stack<String> mDirNames;
  private Stack<String> mParentsIds;
  private Vector<OCFile> mFiles;

  public FileList() {
    mDirNames = new Stack<String>();
    mParentsIds = new Stack<String>();
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mAccountManager = (AccountManager)getActivity().getSystemService(Service.ACCOUNT_SERVICE);
    mAccount = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)[0];
    populateFileList();
  }
  
  @Override
  public void onItemClick(AdapterView<?> l, View v, int position, long id) {
    if (mFiles.size() <= position) {
      throw new IndexOutOfBoundsException("Incorrect item selected");
    }
    OCFile file = mFiles.get(position);
    String id_ = String.valueOf(file.getFileId());
    if (file.getMimetype().equals("DIR")) {
        String dirname = file.getFileName();

        mDirNames.push(dirname);
        mParentsIds.push(id_);
        ((FileDisplayActivity)getActivity()).pushPath(dirname);
        
        populateFileList();
        return;
    }
    Intent i = new Intent(getActivity(), FileDetailActivity.class);
    String filename = ((TextView)v.findViewById(R.id.Filename)).getText().toString();
    i.putExtra("FILE_NAME", filename);
    i.putExtra("FULL_PATH", "/" + filename);
    i.putExtra("FILE_ID", id_);
    i.putExtra("ACCOUNT", mAccount);
    FileDetail fd = (FileDetail) getSupportFragmentManager().findFragmentById(R.id.fileDetail);
    if (fd != null) {
      fd.setStuff(i);
    } else {
      startActivity(i);
    }
  }

  public void onBackPressed() {
    mParentsIds.pop();
    mDirNames.pop();
    populateFileList();
  }

  private void populateFileList() {
    String s = "/";
    for (String a : mDirNames)
      s+= a+"/";

    OCFile file = new OCFile(getActivity().getContentResolver(), mAccount, s);
    mFiles = file.getDirectoryContent();
    setListAdapter(new FileListListAdapter(file, getActivity()));
  }
}
