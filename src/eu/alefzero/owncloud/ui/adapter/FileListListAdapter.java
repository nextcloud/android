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

import java.util.Vector;

import eu.alefzero.owncloud.DisplayUtils;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.datamodel.DataStorageManager;
import eu.alefzero.owncloud.datamodel.OCFile;

import android.content.Context;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

/**
 * This Adapter populates a ListView with all files and folders in an ownCloud
 * instance.
 * 
 * @author Bartek Przybylski
 * 
 */
public class FileListListAdapter implements ListAdapter {
    private Context mContext;
    private OCFile mFile;
    private Vector<OCFile> mFiles;
    private DataStorageManager mStorageManager;

    public FileListListAdapter(OCFile file, DataStorageManager storage_man,
            Context context) {
        mFile = file;
        mStorageManager = storage_man;
        mFiles = mStorageManager.getDirectoryContent(mFile);
        mContext = context;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public boolean isEnabled(int position) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public int getCount() {
        return mFiles != null ? mFiles.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        if (mFiles.size() <= position)
            return null;
        return mFiles.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mFiles != null ? mFiles.get(position).getFileId() : 0;
    }

    @Override
    public int getItemViewType(int position) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflator = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflator.inflate(R.layout.list_layout, null);
        }
        if (mFiles.size() > position) {
            OCFile file = mFiles.get(position);
            TextView fileName = (TextView) view.findViewById(R.id.Filename);
            String name = file.getFileName();

            fileName.setText(DisplayUtils.HtmlDecode(name));
            ImageView fileIcon = (ImageView) view.findViewById(R.id.imageView1);
            if (!file.getMimetype().equals("DIR")) {
                fileIcon.setImageResource(R.drawable.file);
            } else {
                fileIcon.setImageResource(R.drawable.ic_menu_archive);
            }
            ImageView down = (ImageView) view.findViewById(R.id.imageView2);
            if (file.getStoragePath() != null)
                down.setVisibility(View.VISIBLE);
            else
                down.setVisibility(View.INVISIBLE);

            if (!file.isDirectory()) {
                view.findViewById(R.id.file_size).setVisibility(View.VISIBLE);
                view.findViewById(R.id.last_mod).setVisibility(View.VISIBLE);
                ((TextView)view.findViewById(R.id.file_size)).setText(DisplayUtils.bytesToHumanReadable(file.getFileLength()));
                ((TextView)view.findViewById(R.id.last_mod)).setText(DisplayUtils.unixTimeToHumanReadable(file.getModificationTimestamp()));
            } else {
               view.findViewById(R.id.file_size).setVisibility(View.GONE);
               view.findViewById(R.id.last_mod).setVisibility(View.GONE);
            }
        }

        return view;
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isEmpty() {
        return mFiles != null ? mFiles.isEmpty() : false;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
        // TODO Auto-generated method stub

    }
}
