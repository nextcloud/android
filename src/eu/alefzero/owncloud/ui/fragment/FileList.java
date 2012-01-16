package eu.alefzero.owncloud.ui.fragment;

import eu.alefzero.owncloud.FileDetail;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.R.id;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import eu.alefzero.owncloud.ui.FileDetailActivity;
import eu.alefzero.owncloud.ui.adapter.FileListListAdapter;
import eu.alefzero.owncloud.ui.fragment.ActionBar;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class FileList extends ListFragment {
  private Cursor mCursor;
  private Account mAccount;
  private AccountManager mAccountManager;
  private View mheaderView;

  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
    
    mAccountManager = (AccountManager)getActivity().getSystemService(Service.ACCOUNT_SERVICE);
    mAccount = mAccountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE)[0];
    populateFileList();
  }
  
  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onActivityCreated(savedInstanceState);
  }
  
  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    FileDetail fd = (FileDetail) getFragmentManager().findFragmentById(R.id.fileDetail);
    ActionBar ab = (ActionBar) getFragmentManager().findFragmentById(R.id.actionBar);
    
    if (!mCursor.moveToPosition(position)) {
      throw new IndexOutOfBoundsException("Incorrect item selected");
    }
    if (mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)).equals("DIR")) {
        String id_ = mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta._ID));
        String dirname = mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_NAME));
        //ab..push(DisplayUtils.HtmlDecode(dirname));
        //mPath.addLast(DisplayUtils.HtmlDecode(dirname));
        //mParents.push(id_);
        mCursor = getActivity().managedQuery(Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_DIR, id_),
                               null,
                               ProviderTableMeta.FILE_ACCOUNT_OWNER + "=?",
                               new String[]{mAccount.name}, null);
        setListAdapter(new FileListListAdapter(mCursor, getActivity()));
        setListShown(false);
        setListShown(true);
        super.onListItemClick(l, v, position, id);
        return;
    }
    Intent i = new Intent(getActivity(), FileDetailActivity.class);
    i.putExtra("FILE_PATH", ab.getCurrentPath());
    i.putExtra("FILE_NAME", ((TextView)v.findViewById(R.id.Filename)).getText());
    if (fd != null) {
      fd.setStuff(i);
      //fd.use(((TextView)v.findViewById(R.id.Filename)).getText());
    } else {
      i.putExtra("FILE_PATH", ab.getCurrentPath());
      i.putExtra("FILE_NAME", ((TextView)v.findViewById(R.id.Filename)).getText());
      startActivity(i);
    }
    super.onListItemClick(l, v, position, id);

  }
  
  @Override
  public void onDestroyView() {
    setListAdapter(null);
    super.onDestroyView();
  }
  
  private void populateFileList() {
    mCursor = getActivity().getContentResolver().query(ProviderTableMeta.CONTENT_URI,
        null,
        ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
        new String[]{mAccount.name},
        null);
    
    setListAdapter(new FileListListAdapter(mCursor, getActivity()));
  }
}
