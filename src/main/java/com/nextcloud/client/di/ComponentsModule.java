/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2020 Chris Narkiewicz <hello@ezaquarii.com>
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

import com.nextcloud.client.etm.EtmActivity;
import com.nextcloud.client.files.downloader.FileTransferService;
import com.nextcloud.client.jobs.NotificationWork;
import com.nextcloud.client.logger.ui.LogsActivity;
import com.nextcloud.client.media.PlayerService;
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.nextcloud.client.onboarding.WhatsNewActivity;
import com.nextcloud.ui.ChooseAccountDialogFragment;
import com.nextcloud.ui.SetStatusDialogFragment;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.authentication.DeepLinkLoginActivity;
import com.owncloud.android.files.BootupBroadcastReceiver;
import com.owncloud.android.files.services.FileDownloader;
import com.owncloud.android.files.services.FileUploader;
import com.owncloud.android.providers.DiskLruImageCacheFileProvider;
import com.owncloud.android.providers.FileContentProvider;
import com.owncloud.android.providers.UsersAndGroupsSearchProvider;
import com.owncloud.android.services.AccountManagerService;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.ui.activities.ActivitiesActivity;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.CommunityActivity;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.activity.CopyToClipboardActivity;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.ErrorsWhileCopyingHandlerActivity;
import com.owncloud.android.ui.activity.ExternalSiteWebView;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FilePickerActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.ManageAccountsActivity;
import com.owncloud.android.ui.activity.ManageSpaceActivity;
import com.owncloud.android.ui.activity.NotificationsActivity;
import com.owncloud.android.ui.activity.PassCodeActivity;
import com.owncloud.android.ui.activity.ReceiveExternalFilesActivity;
import com.owncloud.android.ui.activity.RequestCredentialsActivity;
import com.owncloud.android.ui.activity.RichDocumentsEditorWebView;
import com.owncloud.android.ui.activity.SettingsActivity;
import com.owncloud.android.ui.activity.ShareActivity;
import com.owncloud.android.ui.activity.SsoGrantPermissionActivity;
import com.owncloud.android.ui.activity.SyncedFoldersActivity;
import com.owncloud.android.ui.activity.TextEditorWebView;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.activity.UploadListActivity;
import com.owncloud.android.ui.activity.UserInfoActivity;
import com.owncloud.android.ui.dialog.AccountRemovalConfirmationDialog;
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment;
import com.owncloud.android.ui.dialog.ChooseTemplateDialogFragment;
import com.owncloud.android.ui.dialog.MultipleAccountsDialog;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.FileDetailActivitiesFragment;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileDetailSharingFragment;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.ui.fragment.LocalFileListFragment;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.fragment.contactsbackup.ContactListFragment;
import com.owncloud.android.ui.fragment.contactsbackup.ContactsBackupFragment;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewTextFileFragment;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.ui.preview.PreviewTextStringFragment;
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
    @ContributesAndroidInjector abstract ErrorsWhileCopyingHandlerActivity errorsWhileCopyingHandlerActivity();
    @ContributesAndroidInjector abstract ExternalSiteWebView externalSiteWebView();
    @ContributesAndroidInjector abstract FileDisplayActivity fileDisplayActivity();
    @ContributesAndroidInjector abstract FilePickerActivity filePickerActivity();
    @ContributesAndroidInjector abstract FirstRunActivity firstRunActivity();
    @ContributesAndroidInjector abstract FolderPickerActivity folderPickerActivity();
    @ContributesAndroidInjector abstract LogsActivity logsActivity();
    @ContributesAndroidInjector abstract ManageAccountsActivity manageAccountsActivity();
    @ContributesAndroidInjector abstract ManageSpaceActivity manageSpaceActivity();
    @ContributesAndroidInjector abstract NotificationsActivity notificationsActivity();
    @ContributesAndroidInjector abstract CommunityActivity participateActivity();
    @ContributesAndroidInjector abstract PassCodeActivity passCodeActivity();
    @ContributesAndroidInjector abstract PreviewImageActivity previewImageActivity();
    @ContributesAndroidInjector abstract PreviewVideoActivity previewVideoActivity();
    @ContributesAndroidInjector abstract ReceiveExternalFilesActivity receiveExternalFilesActivity();
    @ContributesAndroidInjector abstract RequestCredentialsActivity requestCredentialsActivity();
    @ContributesAndroidInjector abstract SettingsActivity settingsActivity();
    @ContributesAndroidInjector abstract ShareActivity shareActivity();
    @ContributesAndroidInjector abstract SsoGrantPermissionActivity ssoGrantPermissionActivity();
    @ContributesAndroidInjector abstract SyncedFoldersActivity syncedFoldersActivity();
    @ContributesAndroidInjector abstract TrashbinActivity trashbinActivity();
    @ContributesAndroidInjector abstract UploadFilesActivity uploadFilesActivity();
    @ContributesAndroidInjector abstract UploadListActivity uploadListActivity();
    @ContributesAndroidInjector abstract UserInfoActivity userInfoActivity();
    @ContributesAndroidInjector abstract WhatsNewActivity whatsNewActivity();
    @ContributesAndroidInjector abstract EtmActivity etmActivity();

    @ContributesAndroidInjector abstract RichDocumentsEditorWebView richDocumentsWebView();
    @ContributesAndroidInjector abstract TextEditorWebView textEditorWebView();

    @ContributesAndroidInjector abstract ExtendedListFragment extendedListFragment();
    @ContributesAndroidInjector abstract FileDetailFragment fileDetailFragment();
    @ContributesAndroidInjector abstract LocalFileListFragment localFileListFragment();
    @ContributesAndroidInjector abstract OCFileListFragment ocFileListFragment();
    @ContributesAndroidInjector abstract FileDetailActivitiesFragment fileDetailActivitiesFragment();

    @ContributesAndroidInjector
    abstract FileDetailSharingFragment fileDetailSharingFragment();

    @ContributesAndroidInjector
    abstract ChooseTemplateDialogFragment chooseTemplateDialogFragment();

    @ContributesAndroidInjector
    abstract AccountRemovalConfirmationDialog accountRemovalConfirmationDialog();

    @ContributesAndroidInjector
    abstract ChooseRichDocumentsTemplateDialogFragment chooseRichDocumentsTemplateDialogFragment();

    @ContributesAndroidInjector
    abstract ContactsBackupFragment contactsBackupFragment();

    @ContributesAndroidInjector
    abstract PreviewImageFragment previewImageFragment();

    @ContributesAndroidInjector
    abstract ContactListFragment chooseContactListFragment();

    @ContributesAndroidInjector
    abstract PreviewMediaFragment previewMediaFragment();

    @ContributesAndroidInjector
    abstract PreviewTextFragment previewTextFragment();

    @ContributesAndroidInjector
    abstract ChooseAccountDialogFragment chooseAccountDialogFragment();

    @ContributesAndroidInjector
    abstract SetStatusDialogFragment setStatusDialogFragment();

    @ContributesAndroidInjector
    abstract PreviewTextFileFragment previewTextFileFragment();

    @ContributesAndroidInjector
    abstract PreviewTextStringFragment previewTextStringFragment();

    @ContributesAndroidInjector
    abstract GalleryFragment photoFragment();

    @ContributesAndroidInjector
    abstract MultipleAccountsDialog multipleAccountsDialog();

    @ContributesAndroidInjector
    abstract ReceiveExternalFilesActivity.DialogInputUploadFilename dialogInputUploadFilename();

    @ContributesAndroidInjector
    abstract FileUploader fileUploader();

    @ContributesAndroidInjector
    abstract FileDownloader fileDownloader();

    @ContributesAndroidInjector abstract BootupBroadcastReceiver bootupBroadcastReceiver();
    @ContributesAndroidInjector abstract NotificationWork.NotificationReceiver notificationWorkBroadcastReceiver();

    @ContributesAndroidInjector abstract FileContentProvider fileContentProvider();
    @ContributesAndroidInjector abstract UsersAndGroupsSearchProvider usersAndGroupsSearchProvider();
    @ContributesAndroidInjector abstract DiskLruImageCacheFileProvider diskLruImageCacheFileProvider();

    @ContributesAndroidInjector abstract AccountManagerService accountManagerService();
    @ContributesAndroidInjector abstract OperationsService operationsService();
    @ContributesAndroidInjector abstract PlayerService playerService();

    @ContributesAndroidInjector
    abstract FileTransferService fileDownloaderService();
}
