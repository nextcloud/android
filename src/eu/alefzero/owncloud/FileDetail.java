package eu.alefzero.owncloud;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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
