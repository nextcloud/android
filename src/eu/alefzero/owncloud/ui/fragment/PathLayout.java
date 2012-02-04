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

import java.util.LinkedList;
import java.util.Stack;

import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.R.drawable;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Part of the ActionBar Layout
 * @author Bartek Przybylski
 *
 */
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
