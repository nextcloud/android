package com.owncloud.android.ui.adapter;

import android.app.Activity;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.owncloud.android.R;

public class MyExpandableListAdapter extends BaseExpandableListAdapter {

  private final SparseArray<Group> groups;
  public LayoutInflater inflater;
  public Activity activity;

  public MyExpandableListAdapter(Activity act, SparseArray<Group> groups) {
    activity = act;
    this.groups = groups;
    inflater = act.getLayoutInflater();
    
  }
  
  
  @Override
  public Object getChild(int groupPosition, int childPosition) {
    return groups.get(groupPosition).children.get(childPosition);
  }

  @Override
  public long getChildId(int groupPosition, int childPosition) {
    return 0;
  }

  @Override
  public View getChildView(int groupPosition, final int childPosition,
      boolean isLastChild, View convertView, ViewGroup parent) {
    final String children = (String) getChild(groupPosition, childPosition);
    TextView text = null;
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.listrow_details, null);
    }
    
    
    text = (TextView) convertView.findViewById(R.id.textView1);
    text.setText(children);
    convertView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        Toast.makeText(activity, children, Toast.LENGTH_SHORT).show();
      }
    });
    return convertView;
  }
  
  @Override
  public View getGroupView(int groupPosition, boolean isExpanded,
      View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.listrow_group, null);
    }
    
    final Group group = (Group) getGroup(groupPosition);
    if (group.children.size() == 0){
        convertView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(activity, group.string, Toast.LENGTH_SHORT).show();
            }
        });
    }
    ((TextView) convertView).setText(group.string);
    
    return convertView;
  }

  @Override
  public int getChildrenCount(int groupPosition) {
    return groups.get(groupPosition).children.size();
  }

  @Override
  public Object getGroup(int groupPosition) {
    return groups.get(groupPosition);
  }

  @Override
  public int getGroupCount() {
    return groups.size();
  }

  @Override
  public void onGroupCollapsed(int groupPosition) {
    super.onGroupCollapsed(groupPosition);
  }

  @Override
  public void onGroupExpanded(int groupPosition) {
    super.onGroupExpanded(groupPosition);
  }

  @Override
  public long getGroupId(int groupPosition) {
    return 0;
  }

  @Override
  public boolean hasStableIds() {
    return false;
  }

  @Override
  public boolean isChildSelectable(int groupPosition, int childPosition) {
    return false;
  }
} 