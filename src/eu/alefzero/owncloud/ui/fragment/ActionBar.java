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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.SupportActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import eu.alefzero.owncloud.R;

/**
 * A custom ActionBar implementation used in the FileDisplayActivity
 * @author Bartek Przybylski
 *
 */
public class ActionBar extends Fragment {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
  }
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.action_bar, container, false);
    return v;
  }
  
  @Override
  public void onAttach(SupportActivity activity) {
    super.onAttach(activity);
  }
  
  public void setPath(String path) {
    if (getPathLayout() != null)
      getPathLayout().addPath(path);
  }
  
  public String getCurrentPath() {
    if (getPathLayout() != null)
       return getPathLayout().getFullPath();
    return "";
  }
  
  private PathLayout getPathLayout() {
    return (PathLayout) getActivity().findViewById(R.id.pathLayout1);
  }
}
