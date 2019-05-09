/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz <hello@ezaquarii.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.di;

import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.authentication.DeepLinkLoginActivity;
import com.owncloud.android.files.BootupBroadcastReceiver;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.providers.DiskLruImageCacheFileProvider;
import com.owncloud.android.providers.DocumentsStorageProvider;
import com.owncloud.android.providers.UsersAndGroupsSearchProvider;
import com.owncloud.android.ui.activities.ActivitiesActivity;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.activity.CopyToClipboardActivity;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.ErrorsWhileCopyingHandlerActivity;
import com.owncloud.android.ui.activity.ExternalSiteWebView;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FilePickerActivity;
import com.owncloud.android.ui.activity.FirstRunActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.LogHistoryActivity;
import com.owncloud.android.ui.activity.ManageAccountsActivity;
import com.owncloud.android.ui.activity.ManageSpaceActivity;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.activity.ParticipateActivity;
import com.owncloud.android.ui.activity.PassCodeActivity;
import com.owncloud.android.ui.activity.ReceiveExternalFilesActivity;
import com.owncloud.android.ui.activity.RequestCredentialsActivity;
import com.owncloud.android.ui.activity.RichDocumentsWebView;
import com.owncloud.android.ui.activity.SettingsActivity;
import com.owncloud.android.ui.activity.ShareActivity;
import com.owncloud.android.ui.activity.SsoGrantPermissionActivity;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.activity.UploadListActivity;
import com.owncloud.android.ui.activity.UploadPathActivity;
import com.owncloud.android.ui.activity.UserInfoActivity;
import com.nextcloud.client.whatsnew.WhatsNewActivity;
import com.owncloud.android.ui.dialog.ChooseTemplateDialogFragment;
import com.owncloud.android.ui.errorhandling.ErrorShowActivity;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.FileDetailActivitiesFragment;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.LocalFileListFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewVideoActivity;
import com.owncloud.android.ui.trashbin.TrashbinActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Register classes that require dependency injection. This class is used by Dagger compiler
 * only.
 */
@Module
abstract class ComponentsModule {
    @ContributesAndroidInjector abstract ActivitiesActivity activitiesActivity();
    @ContributesAndroidInjector abstract AuthenticatorActivity authenticatorActivity();
    @ContributesAndroidInjector abstract BaseActivity baseActivity();
    @ContributesAndroidInjector abstract ConflictsResolveActivity conflictsResolveActivity();
    @ContributesAndroidInjector abstract ContactsPreferenceActivity contactsPreferenceActivity();
    @ContributesAndroidInjector abstract CopyToClipboardActivity copyToClipboardActivity();
    @ContributesAndroidInjector abstract DeepLinkLoginActivity deepLinkLoginActivity();
    @ContributesAndroidInjector abstract DrawerActivity drawerActivity();
    @ContributesAndroidInjector abstract ErrorShowActivity errorShowActivity();
    @ContributesAndroidInjector abstract ErrorsWhileCopyingHandlerActivity errorsWhileCopyingHandlerActivity();
    @ContributesAndroidInjector abstract ExternalSiteWebView externalSiteWebView();
    @ContributesAndroidInjector abstract FileDisplayActivity fileDisplayActivity();
    @ContributesAndroidInjector abstract FilePickerActivity filePickerActivity();
    @ContributesAndroidInjector abstract FirstRunActivity firstRunActivity();
    @ContributesAndroidInjector abstract FolderPickerActivity folderPickerActivity();
    @ContributesAndroidInjector abstract LogHistoryActivity logHistoryActivity();
    @ContributesAndroidInjector abstract ManageAccountsActivity manageAccountsActivity();
    @ContributesAndroidInjector abstract ManageSpaceActivity manageSpaceActivity();
    @ContributesAndroidInjector abstract NotificationsActivity notificationsActivity();
    @ContributesAndroidInjector abstract ParticipateActivity participateActivity();
    @ContributesAndroidInjector abstract PassCodeActivity passCodeActivity();
    @ContributesAndroidInjector abstract PreviewImageActivity previewImageActivity();
    @ContributesAndroidInjector abstract PreviewVideoActivity previewVideoActivity();
    @ContributesAndroidInjector abstract ReceiveExternalFilesActivity receiveExternalFilesActivity();
    @ContributesAndroidInjector abstract RequestCredentialsActivity requestCredentialsActivity();
    @ContributesAndroidInjector abstract RichDocumentsWebView richDocumentsWebView();
    @ContributesAndroidInjector abstract SettingsActivity settingsActivity();
    @ContributesAndroidInjector abstract ShareActivity shareActivity();
    @ContributesAndroidInjector abstract SsoGrantPermissionActivity ssoGrantPermissionActivity();
    @ContributesAndroidInjector abstract SyncedFoldersActivity syncedFoldersActivity();
    @ContributesAndroidInjector abstract TrashbinActivity trashbinActivity();
    @ContributesAndroidInjector abstract UploadFilesActivity uploadFilesActivity();
    @ContributesAndroidInjector abstract UploadListActivity uploadListActivity();
    @ContributesAndroidInjector abstract UploadPathActivity uploadPathActivity();
    @ContributesAndroidInjector abstract UserInfoActivity userInfoActivity();
    @ContributesAndroidInjector abstract WhatsNewActivity whatsNewActivity();

    @ContributesAndroidInjector abstract ExtendedListFragment extendedListFragment();
    @ContributesAndroidInjector abstract FileDetailFragment fileDetailFragment();
    @ContributesAndroidInjector abstract LocalFileListFragment localFileListFragment();
    @ContributesAndroidInjector abstract OCFileListFragment ocFileListFragment();
    @ContributesAndroidInjector abstract FileDetailActivitiesFragment fileDetailActivitiesFragment();
    @ContributesAndroidInjector abstract ChooseTemplateDialogFragment chooseTemplateDialogFragment();

    @ContributesAndroidInjector abstract FileUploader fileUploader();

    @ContributesAndroidInjector abstract BootupBroadcastReceiver bootupBroadcastReceiver();

    @ContributesAndroidInjector abstract DocumentsStorageProvider documentsStorageProvider();
    @ContributesAndroidInjector abstract UsersAndGroupsSearchProvider usersAndGroupsSearchProvider();
    @ContributesAndroidInjector abstract DiskLruImageCacheFileProvider diskLruImageCacheFileProvider();
}
