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
package eu.alefzero.owncloud.ui.adapter;

import java.security.Provider;

import eu.alefzero.owncloud.DisplayUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.R.drawable;
import eu.alefzero.owncloud.R.id;
import eu.alefzero.owncloud.R.layout;
import eu.alefzero.owncloud.db.ProviderMeta;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This Adapter populates a ListView with all files and 
 * folders in an ownCloud instance.
 * @author Bartek Przybylski
 *
 */
public class FileListListAdapter implements ListAdapter {

  private Cursor mCursor;
  private Context mContext;
  
  public FileListListAdapter(Cursor c, Context context) {
    mCursor = c;
    mContext = context;
  }
  
  public boolean areAllItemsEnabled() {
    return true;
  }

  public boolean isEnabled(int position) {
    // TODO Auto-generated method stub
    return true;
  }

  public int getCount() {
    // TODO Auto-generated method stub
    return mCursor.getCount();
  }

  public Object getItem(int position) {
    // TODO Auto-generated method stub
    return null;
  }

  public long getItemId(int position) {
    // TODO Auto-generated method stub
    return 0;
  }

  public int getItemViewType(int position) {
    // TODO Auto-generated method stub
    return 0;
  }

  public View getView(int position, View convertView, ViewGroup parent) {
    View v = convertView;
    if (v == null) {
      LayoutInflater vi = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      v = vi.inflate(R.layout.list_layout, null);
    }
    if (mCursor.moveToPosition(position)) {
      TextView tv = (TextView) v.findViewById(R.id.Filename);
      tv.setText(DisplayUtils.HtmlDecode(mCursor.getString(mCursor.getColumnIndex(ProviderMeta.ProviderTableMeta.FILE_NAME))));
      if (!mCursor.getString(mCursor.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE)).equals("DIR")) {
        ImageView iv = (ImageView) v.findViewById(R.id.imageView1);
        iv.setImageResource(R.drawable.file);
      }
    }
    
    return v;
  }

  public int getViewTypeCount() {
    // TODO Auto-generated method stub
    return 4;
  }

  public boolean hasStableIds() {
    // TODO Auto-generated method stub
    return true;
  }

  public boolean isEmpty() {
    // TODO Auto-generated method stub
    return false;
  }

  public void registerDataSetObserver(DataSetObserver observer) {
    // TODO Auto-generated method stub
    
  }

  public void unregisterDataSetObserver(DataSetObserver observer) {
    // TODO Auto-generated method stub
    
  }
}
