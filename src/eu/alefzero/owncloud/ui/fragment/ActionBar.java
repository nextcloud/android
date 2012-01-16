package eu.alefzero.owncloud.ui.fragment;

import eu.alefzero.owncloud.PathLayout;
import eu.alefzero.owncloud.R;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

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
  public void onAttach(Activity activity) {
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
