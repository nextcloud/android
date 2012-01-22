package eu.alefzero.owncloud.ui.fragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import eu.alefzero.owncloud.R;
import eu.alefzero.owncloud.authenticator.AccountAuthenticator;
import eu.alefzero.owncloud.ui.FileDisplayActivity;
import eu.alefzero.owncloud.ui.Preferences;

public class LandingPageFragment extends Fragment implements OnItemClickListener {

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View root = inflater.inflate(R.layout.landing_page_fragment, container);
		return root;
	}


	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		GridView grid = (GridView) getView().findViewById(R.id.gridview);
		grid.setAdapter(new LandingScreenAdapter(getActivity()));
		grid.setOnItemClickListener(this);
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Intent intent;
		
		/**
		 * If the user selects something and acounts are setup,
		 * we can use our LandingScreenAdapter to get the matching
		 * intent for the selected item.
		 * 
		 * Otherwise, the accounsAreSetuo() method will trigger the 
		 * creation of one.
		 */
		if(accountsAreSetup()){
			intent = (Intent) parent.getAdapter().getItem(position);
			if(intent != null ){
				startActivity(intent);
			} else {
				Toast toast = Toast.makeText(getActivity(), "Not yet implemented!", Toast.LENGTH_SHORT);
				toast.show();
			}
			
		} 
	}

	/**
	 * Checks, whether or not there are any ownCloud accounts 
	 * setup. If there is none, it will create one.
	 * 
	 * If there are more then one, it will trigger a selection
	 * unless the selection has not been made yet.
	 * 
	 * @return true, if there is at least one account.
	 */
	private boolean accountsAreSetup() {
		AccountManager accMan = AccountManager.get(getActivity());
		Account[] accounts = accMan
				.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE);

		if (accounts.length == 0) {
			Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
			intent.putExtra("authorities",
					new String[] { AccountAuthenticator.AUTH_TOKEN_TYPE });
			startActivity(intent);
			return false;
		} else if (accounts.length > 1) {
			// TODO: Figure out what to do.
		} 

		return true;
	}

	/**
	 * Used to populate the landing page grid.
	 * Defined this one right in here as private class
	 * as it is unlikely that this Adapter can be useful
	 * anywhere else.
	 *  
	 * @author Lennart Rosam
	 *
	 */
	private class LandingScreenAdapter extends BaseAdapter {

		private Context mContext;

		private final Integer[] mLandingScreenIcons = { R.drawable.home,
				R.drawable.music, R.drawable.contacts,
				android.R.drawable.ic_menu_today,
				android.R.drawable.ic_menu_agenda,
				android.R.drawable.ic_menu_preferences };

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
}
