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

import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.R.id;
import eu.alefzero.owncloud.R.layout;
import android.content.Intent;
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
    setStuff(intent, getView());
  }
  
  private void setStuff(Intent intent, View view) {
    String filename = intent.getStringExtra("FILE_NAME");
    String filepath = intent.getStringExtra("FILE_PATH");
    setFilename(filename, view);
  }
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onCreate(savedInstanceState);
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    // TODO Auto-generated method stub
    super.onActivityCreated(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.file_details, container, false);
    
    if (getActivity().getIntent() != null) setStuff(getActivity().getIntent(), v);
    return v;
  }

  private void setFilename(String filename, View target_view) {
    TextView tv = (TextView) target_view.findViewById(R.id.textView1);
    if (tv != null) tv.setText(filename);
  }
  
  public void setFilename(String filename) {
    setFilename(filename, getView());
  }
}
