package eu.alefzero.owncloud;

import java.security.Provider;

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
