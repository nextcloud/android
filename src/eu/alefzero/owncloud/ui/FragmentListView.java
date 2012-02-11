package eu.alefzero.owncloud.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class FragmentListView extends Fragment implements OnItemClickListener {
  ListView mList;
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    mList = new ListView(getActivity());
    mList.setOnItemClickListener(this);
    super.onCreate(savedInstanceState);
  }
  
  public void setListAdapter(ListAdapter listAdapter) {
    mList.setAdapter(listAdapter);
    mList.invalidate();
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return mList;
    //return super.onCreateView(inflater, container, savedInstanceState);
  }
  
  public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {}
  
  
}
