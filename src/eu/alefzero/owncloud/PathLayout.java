package eu.alefzero.owncloud;

import java.util.LinkedList;
import java.util.Stack;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class PathLayout extends LinearLayout {

  private LinkedList<String> paths;
  ScrollView internalScroll;
  LinearLayout view;

  public PathLayout(Context context) {
    super(context);
    initialize();
  }
  
  public PathLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public String pop() {
    if (paths.size() == 0) {
      return null;
    }
    int start = paths.size()*2-2;
    int count = 2;
    if (paths.size() == 1) {
      start++;
      count--;
    }
    view.removeViews(start, count);
    return paths.removeLast();
  }

  public void addPath(String path) {
    for (String s : path.split("/")) if (s.length() != 0) push(s);
  }
  
  public void push(String path) {
    // its weird that we cannot declare static imgView as path separator
    if (paths.size() != 0) {
      ImageView iv = new ImageView(getContext());
      iv.setImageDrawable(getResources().getDrawable(R.drawable.breadcrumb));
      iv.setPadding(2, 0, 2, 0);
      view.addView(iv);
    }
    TextView tv = new TextView(getContext());
    tv.setLayoutParams(getLayoutParams());
    tv.setText(path);
    view.addView(tv);
    HorizontalScrollView hsv = (HorizontalScrollView) internalScroll.getChildAt(0);
    hsv.smoothScrollTo(hsv.getMaxScrollAmount()*2, 0);
    paths.addLast(path);
  }
  
  public String peek() {
    return paths.peek();
  }

  public String getFullPath() {
    String ret = new String();
    for (int i = 0; i < paths.size(); i++) {
      ret += "/" + paths.get(i);
    }
    return ret;
  }
  
  private void initialize() {
    paths = new LinkedList<String>();
    internalScroll = new ScrollView(getContext());
    internalScroll.setFillViewport(true);
    HorizontalScrollView hsv = new HorizontalScrollView(getContext());
    hsv.setSmoothScrollingEnabled(true);
    internalScroll.addView(hsv);
    view = new LinearLayout(getContext());
    addView(internalScroll);
    hsv.addView(view);
    ImageView iv = new ImageView(getContext());
    iv.setImageDrawable(getResources().getDrawable(R.drawable.breadcrumb));
    view.addView(iv);
  }

}
