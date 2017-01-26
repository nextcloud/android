package com.owncloud.android.ui.activity;

import android.accounts.Account;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.owncloud.android.R;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.users.GetRemoteUserInfoOperation;
import com.owncloud.android.ui.adapter.UserInfoAdapter;

import org.parceler.Parcels;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Created by mdjanic on 25/01/2017.
 */

public class UserInfoActivity extends FileActivity {
    private static final String TAG = UserInfoActivity.class.getSimpleName();

    private static final String KEY_USER_DATA = "USER_DATA";
    private static final String KEY_ACCOUNT = "ACCOUNT";
    private static final String KEY_DISPLAY_NAME = "DISPLAY_NAME";

    @BindView(R.id.generic_rv)
    RecyclerView genericRecyclerView;

    @BindView(R.id.multi_view)
    RelativeLayout multiView;
    @BindView(R.id.empty_list_view)
    LinearLayout multiListContainer;
    @BindView(R.id.empty_list_view_text)
    TextView multiListMessage;
    @BindView(R.id.empty_list_view_headline)
    TextView multiListHeadline;
    @BindView(R.id.empty_list_icon)
    ImageView multiListIcon;
    @BindView(R.id.empty_list_progress)
    ProgressBar multiListProgressBar;

    @BindString(R.string.preview_sorry)
    String sorryMessage;

    RecyclerView.LayoutManager layoutManager;

    RecyclerView.Adapter adapter;

    private Unbinder unbinder;

    private UserInfo userInfo;
    private Account account;

    private String displayName;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();

        account = Parcels.unwrap(bundle.getParcelable(KEY_ACCOUNT));

        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_USER_DATA)) {
            userInfo = Parcels.unwrap(savedInstanceState.getParcelable(KEY_USER_DATA));
        }

        if (bundle.containsKey(KEY_DISPLAY_NAME)) {
            displayName = bundle.getString(KEY_DISPLAY_NAME);
        } else if (userInfo != null && !TextUtils.isEmpty(userInfo.getDisplayName())) {
            displayName = userInfo.getDisplayName();
        }

        setContentView(R.layout.user_info_layout);
        unbinder = ButterKnife.bind(this);

        setupToolbar();
        if (!TextUtils.isEmpty(displayName)) {
            updateActionBarTitleAndHomeButtonByString(displayName);
        } else {
            updateActionBarTitleAndHomeButtonByString(getResources().getString(R.string.user_information_description));
        }

        layoutManager = new LinearLayoutManager(this);
        genericRecyclerView.setLayoutManager(layoutManager);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(genericRecyclerView.getContext(),
                ((LinearLayoutManager)layoutManager).getOrientation());
        genericRecyclerView.addItemDecoration(dividerItemDecoration);

        if (userInfo != null) {
            adapter = new UserInfoAdapter(userInfo, UserInfoActivity.this);
            multiView.setVisibility(View.VISIBLE);
            genericRecyclerView.setVisibility(View.GONE);
        } else {
            setMultiListLoadingMessage();
            fetchAndSetData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_info_menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval = true;
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
            default:
                retval = super.onOptionsItemSelected(item);
        }
        return retval;
    }

    public void onDestroy() {
        super.onDestroy();
        unbinder.unbind();
    }


    private void setMultiListLoadingMessage() {
        if (multiView != null) {
            multiListHeadline.setText(R.string.file_list_loading);
            multiListMessage.setText("");

            multiListIcon.setVisibility(View.GONE);
            multiListProgressBar.setVisibility(View.VISIBLE);
        }
    }

    public void setMessageForMultiList(String headline, String message) {
        if (multiView != null && multiListMessage != null) {
            multiListHeadline.setText(headline);
            multiListMessage.setText(message);

            multiListProgressBar.setVisibility(View.GONE);
        }
    }


    private void fetchAndSetData() {
        Thread t = new Thread(new Runnable() {
            public void run() {

                RemoteOperation getRemoteUserInfoOperation = new GetRemoteUserInfoOperation();
                RemoteOperationResult result = getRemoteUserInfoOperation.execute(account, UserInfoActivity.this);

                if (result.isSuccess() && result.getData() != null) {
                    userInfo = (UserInfo) result.getData().get(0);
                    adapter = new UserInfoAdapter(userInfo, UserInfoActivity.this);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            genericRecyclerView.setAdapter(adapter);
                            multiView.setVisibility(View.GONE);
                            genericRecyclerView.setVisibility(View.VISIBLE);
                        }
                    });
                } else {
                    // show error
                    setMessageForMultiList(result.getLogMessage(), sorryMessage);
                    Log_OC.d(TAG, result.getLogMessage());
                }
            }
        });

        t.start();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (userInfo != null) {
            outState.putParcelable(KEY_USER_DATA, Parcels.wrap(userInfo));
        }
    }

}
