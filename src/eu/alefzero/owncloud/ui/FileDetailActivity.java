package eu.alefzero.owncloud.ui;


import eu.alefzero.owncloud.FileDetail;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.R.id;
import eu.alefzero.owncloud.R.layout;
import eu.alefzero.owncloud.ui.fragment.ActionBar;
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
