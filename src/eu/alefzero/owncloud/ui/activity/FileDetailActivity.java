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
package eu.alefzero.owncloud.ui.activity;


import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.R.id;
import eu.alefzero.owncloud.R.layout;
import eu.alefzero.owncloud.ui.fragment.ActionBar;
import eu.alefzero.owncloud.ui.fragment.FileDetail;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.Window;

/**
 * This activity displays the details of a file like
 * its name, its size and so on.
 * @author Bartek Przybylski
 *
 */
public class FileDetailActivity extends FragmentActivity {
  private FileDetail mFileDetail;
  
@Override
protected void onCreate(Bundle savedInstanceState) {
  // TODO Auto-generated method stub
  super.onCreate(savedInstanceState);
  getWindow().requestFeature(Window.FEATURE_NO_TITLE);
  setContentView(R.layout.file_activity_details);
  
  mFileDetail = new FileDetail();
  FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
  ft.add(R.id.fileDetail, mFileDetail);
  ft.commit();
  
}

}
