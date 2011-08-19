package eu.alefzero.owncloud;

import android.graphics.drawable.Drawable;
import android.view.View.OnClickListener;

public class ActionItem {
  private Drawable mIcon;
  private String mTitle;
  private OnClickListener mClickListener;
  
  public ActionItem() { }
  
  public void setTitle(String title) {
    mTitle = title;
  }
  
  public String getTitle() {
    return mTitle;
  }
  
  public void setIcon(Drawable icon) {
    mIcon = icon;
  }
  
  public Drawable getIcon() {
    return mIcon;
  }
  
  public void setOnClickListener(OnClickListener listener) {
    mClickListener = listener;
  }
  
  public OnClickListener getOnClickListerner() {
    return mClickListener;
  }
  
}
