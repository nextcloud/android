package com.owncloud.android.ui.viewModel;

import android.accounts.Account;

import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.datamodel.UserInfo;
import com.owncloud.android.repository.UserInfoRepository;
import com.owncloud.android.ui.dialog.AccountRemovalConfirmationDialog;
import com.owncloud.android.ui.events.TokenPushEvent;
import com.owncloud.android.utils.PushUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;


public class UserInfoViewModel extends ViewModel {


    private Account account;
    private LiveData<UserInfo> userInfo;
    private UserAccountManager userAccountManager;

    public void init(Account account, UserInfoRepository userInfoRepository, UserAccountManager userAccountManager) {
        if (this.account != null) {
            return;
        }

        userInfo = userInfoRepository.getUserInfo(account);
        this.userAccountManager = userAccountManager;

        // TODO add here setHeader, registerPush, etc.
    }

    public LiveData<UserInfo> getUserInfo() {
        return userInfo;
    }

    public static void openAccountRemovalConfirmationDialog(Account account, FragmentManager fragmentManager,
                                                            boolean removeDirectly) {
        AccountRemovalConfirmationDialog.newInstance(account, removeDirectly).show(fragmentManager, "dialog");
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    public void onMessageEvent(TokenPushEvent event) {
        PushUtils.pushRegistrationToServer(userAccountManager, ""); // todo token?
    }
}
