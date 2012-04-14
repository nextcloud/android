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
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AuthUtils;
import eu.alefzero.owncloud.datamodel.DataStorageManager;
import eu.alefzero.owncloud.datamodel.FileDataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;
import eu.alefzero.owncloud.ui.FragmentListView;
import eu.alefzero.owncloud.ui.activity.FileDetailActivity;
import eu.alefzero.owncloud.ui.activity.FileDisplayActivity;
import eu.alefzero.owncloud.ui.adapter.FileListListAdapter;

/**
 * A Fragment that lists all files and folders in a given path.
 * @author Bartek Przybylski
 *
 */
public class FileListFragment extends FragmentListView {
  private Account mAccount;
  private Stack<String> mDirNames;
  private Vector<OCFile> mFiles;
  private DataStorageManager mStorageManager;

  public FileListFragment() {
    mDirNames = new Stack<String>();
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    mAccount = AuthUtils.getCurrentOwnCloudAccount(getActivity());
    populateFileList();
    // TODO: Remove this testing stuff
    //addContact(mAccount, "Bartek Przybylski", "czlowiek");
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
        ((FileDisplayActivity)getActivity()).pushPath(dirname);
        
        populateFileList();
        return;
    }
    Intent i = new Intent(getActivity(), FileDetailActivity.class);
    i.putExtra("FILE_NAME", file.getFileName());
    i.putExtra("FULL_PATH", file.getPath());
    i.putExtra("FILE_ID", id_);
    i.putExtra("ACCOUNT", mAccount);
    FileDetailFragment fd = (FileDetailFragment) getFragmentManager().findFragmentById(R.id.fileDetail);
    if (fd != null) {
      fd.setStuff(i);
    } else {
      startActivity(i);
    }
  }

  /**
   * Call this, when the user presses the up button
   */
  public void onNavigateUp() {
    mDirNames.pop();
    populateFileList();
  }

  /**
   * Lists the directory
   */
  private void populateFileList() {
    String s = "/";
    for (String a : mDirNames)
      s+= a+"/";

    mStorageManager = new FileDataStorageManager(mAccount, getActivity().getContentResolver());
    OCFile file = new OCFile(s);
    mFiles = mStorageManager.getDirectoryContent(file);
    setListAdapter(new FileListListAdapter(file, mStorageManager, getActivity()));
  }
  
  //TODO: Delete this testing stuff.
  /*private  void addContact(Account account, String name, String username) {
    Log.i("ASD", "Adding contact: " + name);
    ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
    
    //Create our RawContact
    ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
    builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
    builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
    builder.withValue(RawContacts.SYNC1, username);
    operationList.add(builder.build());
    
    //Create a Data record of common type 'StructuredName' for our RawContact
    builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
    builder.withValueBackReference(ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
    builder.withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
    builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
    operationList.add(builder.build());
    
    //Create a Data record of custom type "vnd.android.cursor.item/vnd.fm.last.android.profile" to display a link to the Last.fm profile
    builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
    builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
    builder.withValue(ContactsContract.Data.MIMETYPE, "vnd.android.cursor.item/vnd.owncloud.contact.profile");
    builder.withValue(ContactsContract.Data.DATA1, username);
    builder.withValue(ContactsContract.Data.DATA2, "Last.fm Profile");
    builder.withValue(ContactsContract.Data.DATA3, "View profile");
    operationList.add(builder.build());
    
    try {
     getActivity().getContentResolver().applyBatch(ContactsContract.AUTHORITY, operationList);
    } catch (Exception e) {
     Log.e("ASD", "Something went wrong during creation! " + e);
     e.printStackTrace();
    }
   }*/
  
}
