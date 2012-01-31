package eu.alefzero.owncloud.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.ui.FileDisplayActivity;
import eu.alefzero.owncloud.ui.Preferences;

/**
 * Populates the landing screen icons.
 * @author Benutzer
 *
 */
public class LandingScreenAdapter extends BaseAdapter {
	
	private Context mContext;

	private final Integer[] mLandingScreenIcons = { R.drawable.home,
			R.drawable.music, R.drawable.contacts,
			R.drawable.calendar,
			android.R.drawable.ic_menu_agenda,
			R.drawable.settings };

	private final Integer[] mLandingScreenTexts = { R.string.main_files,
			R.string.main_music, R.string.main_contacts,
			R.string.main_calendar, R.string.main_bookmarks,
			R.string.main_settings };

	public LandingScreenAdapter(Context context) {
		mContext = context;
	}

	@Override
	public int getCount() {
		return mLandingScreenIcons.length;
	}

	@Override
	/**
	 * Returns the Intent associated with this object
	 * or null if the functionality is not yet implemented
	 */
	public Object getItem(int position) {
		Intent intent = new Intent();
		switch (position) {
		case 0:
			intent.setClass(mContext, FileDisplayActivity.class);
			break;
		case 5:
			intent.setClass(mContext, Preferences.class);
			break;
		default:
			intent = null;
		}
		return intent;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflator = LayoutInflater.from(mContext);
			convertView = inflator
					.inflate(R.layout.landing_page_item, null);

			ImageView icon = (ImageView) convertView
					.findViewById(R.id.gridImage);
			TextView iconText = (TextView) convertView
					.findViewById(R.id.gridText);

			icon.setImageResource(mLandingScreenIcons[position]);
			iconText.setText(mLandingScreenTexts[position]);
		}
		return convertView;
	}

}
