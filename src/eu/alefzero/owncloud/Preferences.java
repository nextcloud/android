package eu.alefzero.owncloud;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class Preferences extends PreferenceActivity {
  private String TAG = "OwnCloudPreferences";
  private final int mNewSession = 47;
  private final int mEditSession = 48;
  private DbHandler mDbHandler;
  private Vector<OwnCloudSession> mSessions;
  private int mSelectedMenuItem;
  
  @Override
  public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    mDbHandler = new DbHandler(getBaseContext());
    mSessions = new Vector<OwnCloudSession>();
    addPreferencesFromResource(R.xml.preferences);
    registerForContextMenu(getListView());
    //populateSessionList();
  }
  
  private void populateSessionList() {
    mSessions.clear();
    mSessions = mDbHandler.getSessionList();
    PreferenceScreen ps = getPreferenceScreen();
    ps.removeAll();
    addPreferencesFromResource(R.xml.preferences);
    for (int i = 0; i < mSessions.size(); i++) {
      Preference preference = new Preference(getBaseContext());
      preference.setTitle(mSessions.get(i).getName());
      URI uri;
      try {
        uri = new URI(mSessions.get(i).getUrl());
      } catch (URISyntaxException e) {
        e.printStackTrace(); // should never happend
        continue;
      }
      preference.setSummary(uri.getScheme() + "://" + uri.getHost()+uri.getPath());
      ps.addPreference(preference);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.prefs_menu, menu);
    return true;
  }
  
  @Override
  public boolean onMenuItemSelected(int featureId, MenuItem item) {
    super.onMenuItemSelected(featureId, item);
    Intent intent;
    
    switch (item.getItemId()) {
      case R.id.addSessionItem:
        intent = new Intent(this, PreferencesNewSession.class);
        startActivityForResult(intent, mNewSession);
        break;
      case R.id.SessionContextEdit:
        intent = new Intent(this, PreferencesNewSession.class);
        intent.putExtra("sessionId", mSessions.get(mSelectedMenuItem).getEntryId());
        intent.putExtra("sessionName", mSessions.get(mSelectedMenuItem).getName());
        intent.putExtra("sessionURL", mSessions.get(mSelectedMenuItem).getUrl());
        startActivityForResult(intent, mEditSession);
        break;
      case R.id.SessionContextRemove:
        OwnCloudSession ocs = mSessions.get(mSelectedMenuItem);
        mDbHandler.removeSessionWithId(ocs.getEntryId());
        mSessions.remove(ocs);
        getPreferenceScreen().removePreference(getPreferenceScreen().getPreference(mSelectedMenuItem+1));
        break;
      default:
        Log.w(TAG, "Unknown menu item triggered");
        return false;
    }
    return true;
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode == Activity.RESULT_OK) {
      switch (requestCode) {
        case mNewSession:
          mDbHandler.addSession(data.getStringExtra("sessionName"), 
                                data.getStringExtra("sessionURL"));
          getPreferenceScreen().removeAll();
          addPreferencesFromResource(R.xml.preferences);
          populateSessionList();
          break;
        case mEditSession:
          mDbHandler.changeSessionFields(data.getIntExtra("sessionId", -1),
                                         data.getStringExtra("sessionName"),
                                         data.getStringExtra("sessionURL"));
          populateSessionList();
          break;
      }
    }
  }

  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
    mSelectedMenuItem = info.position-1;
    menu.setHeaderTitle(mSessions.get(mSelectedMenuItem).getName());
    
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.session_context_menu, menu);
    
  }
  
  @Override
  protected void onDestroy() {
    mDbHandler.close();
    super.onDestroy();
  }
  
}
