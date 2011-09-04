package eu.alefzero.owncloud;

import java.util.Stack;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class PathLayout extends LinearLayout {

  private Stack<String> paths;
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
    if (paths.empty()) {
      return null;
    }
    int start = paths.size()*2-2;
    int count = 2;
    if (paths.size() == 1) {
      start++;
      count--;
    }
    view.removeViews(start, count);
    return paths.pop();
  }

  public void push(String path) {
    // its weird that we cannot declare static imgView as path separator
    if (!paths.empty()) {
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
    paths.push(path);
  }
  
  public String peek() {
    return paths.peek();
  }

  private void initialize() {
    paths = new Stack<String>();
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
