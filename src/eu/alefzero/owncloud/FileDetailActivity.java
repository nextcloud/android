package eu.alefzero.owncloud;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.Window;

public class FileDetailActivity extends FragmentActivity {
@Override
protected void onCreate(Bundle savedInstanceState) {
  // TODO Auto-generated method stub
  super.onCreate(savedInstanceState);
  getWindow().requestFeature(Window.FEATURE_NO_TITLE);
  setContentView(R.layout.file_activity_details);
  
  FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
  ft.add(R.id.fileDetail, new FileDetail());
  ft.commit();
  
}
}
