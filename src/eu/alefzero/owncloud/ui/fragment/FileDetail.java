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

import eu.alefzero.owncloud.DisplayUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.cp;
import eu.alefzero.owncloud.R.id;
import eu.alefzero.owncloud.R.layout;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This Fragment is used to display the details about a file.
 * @author Bartek Przybylski
 *
 */
public class FileDetail extends Fragment {
  
  public Intent mIntent;
  
  public void setStuff(Intent intent) {
    mIntent = intent;
    setStuff(getView());
  }
  
  private void setStuff(View view) {
    String id = mIntent.getStringExtra("FILE_ID");
    String account_name = mIntent.getStringExtra("ACCOUNT_NAME");
    Cursor c = getActivity().managedQuery(
        Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE, id),
        null,
        ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
        new String[]{account_name},
        null);
    c.moveToFirst();
    
    String filename = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_NAME));
    setFilename(filename, view);
    String mimetype = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE));
    setFiletype(DisplayUtils.convertMIMEtoPrettyPrint(mimetype), view);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.file_details, container, false);
    
    if (getActivity().getIntent() != null) {
      mIntent = getActivity().getIntent();
      setStuff(v);
    }
    return v;
  }

  private void setFilename(String filename, View target_view) {
    TextView tv = (TextView) target_view.findViewById(R.id.textView1);
    if (tv != null) tv.setText(filename);
  }
  
  private void setFiletype(String mimetype, View target_view) {
    TextView tv = (TextView) target_view.findViewById(R.id.textView2);
    if (tv != null) tv.setText(mimetype);
  }
  
  public void setFilename(String filename) {
    setFilename(filename, getView());
  }
  
  public void setFiletype(String filename) {
    setFiletype(filename, getView());
  }
}
