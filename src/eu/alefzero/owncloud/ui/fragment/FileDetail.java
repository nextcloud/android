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

import android.accounts.Account;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import eu.alefzero.owncloud.DisplayUtils;
import eu.alefzero.owncloud.FileDownloader;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.db.ProviderMeta.ProviderTableMeta;

/**
 * This Fragment is used to display the details about a file.
 * @author Bartek Przybylski
 *
 */
public class FileDetail extends Fragment implements OnClickListener {
  
  private Intent mIntent;
  private View mView;
  
  public void setStuff(Intent intent) {
    mIntent = intent;
    setStuff(getView());
  }
  
  private void setStuff(View view) {
    mView = view;
    String id = mIntent.getStringExtra("FILE_ID");
    Account account = mIntent.getParcelableExtra("ACCOUNT");
    String account_name = account.name;
    Cursor c = getActivity().managedQuery(
        Uri.withAppendedPath(ProviderTableMeta.CONTENT_URI_FILE, id),
        null,
        ProviderTableMeta.FILE_ACCOUNT_OWNER+"=?",
        new String[]{account_name},
        null);
    c.moveToFirst();

    // Retrieve details from DB
    String filename = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_NAME));
    String mimetype = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_TYPE));
    String path = c.getString(c.getColumnIndex(ProviderTableMeta.FILE_STORAGE_PATH));
    long filesize = c.getLong(c.getColumnIndex(ProviderTableMeta.FILE_CONTENT_LENGTH));

    // set file details
    setFilename(filename);
    setFiletype(DisplayUtils.convertMIMEtoPrettyPrint(mimetype));
    setFilesize(filesize);
    
    // set file preview if available and possible
    View w = view.findViewById(R.id.videoView1);
    w.setVisibility(View.INVISIBLE);
    if (path == null) {
      ImageView v = (ImageView) getView().findViewById(R.id.imageView2);
      v.setImageResource(R.drawable.download);
      v.setOnClickListener(this);
    } else {
      if (mimetype.startsWith("image/")) {
        ImageView v = (ImageView) view.findViewById(R.id.imageView2);
        Bitmap bmp = BitmapFactory.decodeFile(path);
        v.setImageBitmap(bmp);
      } else if (mimetype.startsWith("video/")) {
        VideoView v = (VideoView) view.findViewById(R.id.videoView1);
        v.setVisibility(View.VISIBLE);
        v.setVideoPath(path);
        v.start();
      }
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = null;
    
    if (getActivity().getIntent() != null && getActivity().getIntent().getStringExtra("FILE_ID") != null) {
    	v = inflater.inflate(R.layout.file_details, container, false);
    	mIntent = getActivity().getIntent();
    	setStuff(v);
    } else {
    	v = inflater.inflate(R.layout.file_details_empty, container, false);
    }
    return v;
  }

  @Override
  public View getView() {
    return mView == null ? super.getView() : mView;
  };
  
  public void setFilename(String filename) {
    TextView tv = (TextView) getView().findViewById(R.id.textView1);
    if (tv != null) tv.setText(filename);
  }
  
  public void setFiletype(String mimetype) {
    TextView tv = (TextView) getView().findViewById(R.id.textView2);
    if (tv != null) tv.setText(mimetype);
  }
  
  public void setFilesize(long filesize) {
    TextView tv = (TextView) getView().findViewById(R.id.textView3);
    if (tv != null) tv.setText(DisplayUtils.bitsToHumanReadable(filesize));
  }

  @Override
  public void onClick(View v) {
    Toast.makeText(getActivity(), "Downloading", Toast.LENGTH_LONG).show();
    Intent i = new Intent(getActivity(), FileDownloader.class);
    i.putExtra(FileDownloader.EXTRA_ACCOUNT, mIntent.getParcelableExtra("ACCOUNT"));
    i.putExtra(FileDownloader.EXTRA_FILE_PATH, mIntent.getStringExtra("FULL_PATH"));
    getActivity().startService(i);
  }
  
}
