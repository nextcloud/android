/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.di;

import com.nextcloud.client.documentscan.DocumentScanActivity;
import com.nextcloud.client.editimage.EditImageActivity;
import com.nextcloud.client.etm.EtmActivity;
import com.nextcloud.client.etm.pages.EtmBackgroundJobsFragment;
import com.nextcloud.client.jobs.BackgroundJobManagerImpl;
import com.nextcloud.client.jobs.NotificationWork;
import com.nextcloud.client.jobs.TestJob;
import com.nextcloud.client.jobs.transfer.FileTransferService;
import com.nextcloud.client.jobs.upload.FileUploadHelper;
import com.nextcloud.client.logger.ui.LogsActivity;
import com.nextcloud.client.logger.ui.LogsViewModel;
import com.nextcloud.client.media.PlayerService;
import com.nextcloud.client.migrations.Migrations;
import com.nextcloud.client.onboarding.FirstRunActivity;
import com.nextcloud.client.onboarding.WhatsNewActivity;
import com.nextcloud.client.widget.DashboardWidgetConfigurationActivity;
import com.nextcloud.client.widget.DashboardWidgetProvider;
import com.nextcloud.client.widget.DashboardWidgetService;
import com.nextcloud.receiver.NetworkChangeReceiver;
import com.nextcloud.ui.ChooseAccountDialogFragment;
import com.nextcloud.ui.ImageDetailFragment;
import com.nextcloud.ui.SetStatusDialogFragment;
import com.nextcloud.ui.composeActivity.ComposeActivity;
import com.nextcloud.ui.fileactions.FileActionsBottomSheet;
import com.nmc.android.ui.LauncherActivity;
import com.owncloud.android.MainApp;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.authentication.DeepLinkLoginActivity;
import com.owncloud.android.files.BootupBroadcastReceiver;
import com.owncloud.android.providers.DiskLruImageCacheFileProvider;
import com.owncloud.android.providers.DocumentsStorageProvider;
import com.owncloud.android.providers.FileContentProvider;
import com.owncloud.android.providers.UsersAndGroupsSearchProvider;
import com.owncloud.android.services.AccountManagerService;
import com.owncloud.android.services.OperationsService;
import com.owncloud.android.syncadapter.FileSyncService;
import com.owncloud.android.ui.activities.ActivitiesActivity;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.CommunityActivity;
import com.owncloud.android.ui.activity.ConflictsResolveActivity;
import com.owncloud.android.ui.activity.ContactsPreferenceActivity;
import com.owncloud.android.ui.activity.CopyToClipboardActivity;
import com.owncloud.android.ui.activity.DrawerActivity;
import com.owncloud.android.ui.activity.ErrorsWhileCopyingHandlerActivity;
import com.owncloud.android.ui.activity.ExternalSiteWebView;
import com.owncloud.android.ui.activity.FileActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;
import com.owncloud.android.ui.activity.FilePickerActivity;
import com.owncloud.android.ui.activity.FolderPickerActivity;
import com.owncloud.android.ui.activity.InternalTwoWaySyncActivity;
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
import com.owncloud.android.ui.activity.ToolbarActivity;
import com.owncloud.android.ui.activity.UploadFilesActivity;
import com.owncloud.android.ui.activity.UploadListActivity;
import com.owncloud.android.ui.activity.UserInfoActivity;
import com.owncloud.android.ui.dialog.AccountRemovalDialog;
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment;
import com.owncloud.android.ui.dialog.ChooseTemplateDialogFragment;
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment;
import com.owncloud.android.ui.dialog.ConflictsResolveDialog;
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment;
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment;
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog;
import com.owncloud.android.ui.dialog.LoadingDialog;
import com.owncloud.android.ui.dialog.LocalStoragePathPickerDialogFragment;
import com.owncloud.android.ui.dialog.MultipleAccountsDialog;
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment;
import com.owncloud.android.ui.dialog.RenameFileDialogFragment;
import com.owncloud.android.ui.dialog.RenamePublicShareDialogFragment;
import com.owncloud.android.ui.dialog.SendFilesDialog;
import com.owncloud.android.ui.dialog.SendShareDialog;
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment;
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment;
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment;
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog;
import com.owncloud.android.ui.dialog.StoragePermissionDialogFragment;
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment;
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment;
import com.owncloud.android.ui.fragment.ExtendedListFragment;
import com.owncloud.android.ui.fragment.FeatureFragment;
import com.owncloud.android.ui.fragment.FileDetailActivitiesFragment;
import com.owncloud.android.ui.fragment.FileDetailFragment;
import com.owncloud.android.ui.fragment.FileDetailSharingFragment;
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment;
import com.owncloud.android.ui.fragment.GalleryFragment;
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog;
import com.owncloud.android.ui.fragment.GroupfolderListFragment;
import com.owncloud.android.ui.fragment.LocalFileListFragment;
import com.owncloud.android.ui.fragment.OCFileListBottomSheetDialog;
import com.owncloud.android.ui.fragment.OCFileListFragment;
import com.owncloud.android.ui.fragment.SharedListFragment;
import com.owncloud.android.ui.fragment.UnifiedSearchFragment;
import com.owncloud.android.ui.fragment.contactsbackup.BackupFragment;
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment;
import com.owncloud.android.ui.preview.FileDownloadFragment;
import com.owncloud.android.ui.preview.PreviewBitmapActivity;
import com.owncloud.android.ui.preview.PreviewImageActivity;
import com.owncloud.android.ui.preview.PreviewImageFragment;
import com.owncloud.android.ui.preview.PreviewMediaActivity;
import com.owncloud.android.ui.preview.PreviewMediaFragment;
import com.owncloud.android.ui.preview.PreviewTextFileFragment;
import com.owncloud.android.ui.preview.PreviewTextFragment;
import com.owncloud.android.ui.preview.PreviewTextStringFragment;
import com.owncloud.android.ui.preview.pdf.PreviewPdfFragment;
import com.owncloud.android.ui.trashbin.TrashbinActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

/**
 * Register classes that require dependency injection. This class is used by Dagger compiler only.
 */
@Module
abstract class ComponentsModule {
    @ContributesAndroidInjector
    abstract ActivitiesActivity activitiesActivity();

    @ContributesAndroidInjector
    abstract AuthenticatorActivity authenticatorActivity();

    @ContributesAndroidInjector
    abstract BaseActivity baseActivity();

    @ContributesAndroidInjector
    abstract ConflictsResolveActivity conflictsResolveActivity();

    @ContributesAndroidInjector
    abstract ContactsPreferenceActivity contactsPreferenceActivity();

    @ContributesAndroidInjector
    abstract CopyToClipboardActivity copyToClipboardActivity();

    @ContributesAndroidInjector
    abstract DeepLinkLoginActivity deepLinkLoginActivity();

    @ContributesAndroidInjector
    abstract DrawerActivity drawerActivity();

    @ContributesAndroidInjector
    abstract ErrorsWhileCopyingHandlerActivity errorsWhileCopyingHandlerActivity();

    @ContributesAndroidInjector
    abstract ExternalSiteWebView externalSiteWebView();

    @ContributesAndroidInjector
    abstract FileDisplayActivity fileDisplayActivity();

    @ContributesAndroidInjector
    abstract FilePickerActivity filePickerActivity();

    @ContributesAndroidInjector
    abstract FirstRunActivity firstRunActivity();

    @ContributesAndroidInjector
    abstract FolderPickerActivity folderPickerActivity();

    @ContributesAndroidInjector
    abstract LogsActivity logsActivity();

    @ContributesAndroidInjector
    abstract ManageAccountsActivity manageAccountsActivity();

    @ContributesAndroidInjector
    abstract ManageSpaceActivity manageSpaceActivity();

    @ContributesAndroidInjector
    abstract NotificationsActivity notificationsActivity();

    @ContributesAndroidInjector
    abstract CommunityActivity participateActivity();

    @ContributesAndroidInjector
    abstract ComposeActivity composeActivity();

    @ContributesAndroidInjector
    abstract PassCodeActivity passCodeActivity();

    @ContributesAndroidInjector
    abstract PreviewImageActivity previewImageActivity();

    @ContributesAndroidInjector
    abstract PreviewMediaActivity previewMediaActivity();

    @ContributesAndroidInjector
    abstract ReceiveExternalFilesActivity receiveExternalFilesActivity();

    @ContributesAndroidInjector
    abstract RequestCredentialsActivity requestCredentialsActivity();

    @ContributesAndroidInjector
    abstract SettingsActivity settingsActivity();

    @ContributesAndroidInjector
    abstract ShareActivity shareActivity();

    @ContributesAndroidInjector
    abstract SsoGrantPermissionActivity ssoGrantPermissionActivity();

    @ContributesAndroidInjector
    abstract SyncedFoldersActivity syncedFoldersActivity();

    @ContributesAndroidInjector
    abstract TrashbinActivity trashbinActivity();

    @ContributesAndroidInjector
    abstract UploadFilesActivity uploadFilesActivity();

    @ContributesAndroidInjector
    abstract UploadListActivity uploadListActivity();

    @ContributesAndroidInjector
    abstract UserInfoActivity userInfoActivity();

    @ContributesAndroidInjector
    abstract WhatsNewActivity whatsNewActivity();

    @ContributesAndroidInjector
    abstract EtmActivity etmActivity();

    @ContributesAndroidInjector
    abstract RichDocumentsEditorWebView richDocumentsWebView();

    @ContributesAndroidInjector
    abstract TextEditorWebView textEditorWebView();

    @ContributesAndroidInjector
    abstract ExtendedListFragment extendedListFragment();

    @ContributesAndroidInjector
    abstract FileDetailFragment fileDetailFragment();

    @ContributesAndroidInjector
    abstract LocalFileListFragment localFileListFragment();

    @ContributesAndroidInjector
    abstract OCFileListFragment ocFileListFragment();

    @ContributesAndroidInjector
    abstract FileDetailActivitiesFragment fileDetailActivitiesFragment();

    @ContributesAndroidInjector
    abstract FileDetailsSharingProcessFragment fileDetailsSharingProcessFragment();

    @ContributesAndroidInjector
    abstract FileDetailSharingFragment fileDetailSharingFragment();

    @ContributesAndroidInjector
    abstract ChooseTemplateDialogFragment chooseTemplateDialogFragment();

    @ContributesAndroidInjector
    abstract AccountRemovalDialog accountRemovalDialog();

    @ContributesAndroidInjector
    abstract ChooseRichDocumentsTemplateDialogFragment chooseRichDocumentsTemplateDialogFragment();

    @ContributesAndroidInjector
    abstract BackupFragment contactsBackupFragment();

    @ContributesAndroidInjector
    abstract PreviewImageFragment previewImageFragment();

    @ContributesAndroidInjector
    abstract BackupListFragment chooseContactListFragment();

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
    abstract UnifiedSearchFragment searchFragment();

    @ContributesAndroidInjector
    abstract GalleryFragment photoFragment();

    @ContributesAndroidInjector
    abstract MultipleAccountsDialog multipleAccountsDialog();

    @ContributesAndroidInjector
    abstract ReceiveExternalFilesActivity.DialogInputUploadFilename dialogInputUploadFilename();

    @ContributesAndroidInjector
    abstract BootupBroadcastReceiver bootupBroadcastReceiver();

    @ContributesAndroidInjector
    abstract NetworkChangeReceiver networkChangeReceiver();

    @ContributesAndroidInjector
    abstract NotificationWork.NotificationReceiver notificationWorkBroadcastReceiver();

    @ContributesAndroidInjector
    abstract FileContentProvider fileContentProvider();

    @ContributesAndroidInjector
    abstract UsersAndGroupsSearchProvider usersAndGroupsSearchProvider();

    @ContributesAndroidInjector
    abstract DiskLruImageCacheFileProvider diskLruImageCacheFileProvider();

    @ContributesAndroidInjector
    abstract DocumentsStorageProvider documentsStorageProvider();

    @ContributesAndroidInjector
    abstract AccountManagerService accountManagerService();

    @ContributesAndroidInjector
    abstract OperationsService operationsService();

    @ContributesAndroidInjector
    abstract PlayerService playerService();

    @ContributesAndroidInjector
    abstract FileTransferService fileDownloaderService();

    @ContributesAndroidInjector
    abstract FileSyncService fileSyncService();

    @ContributesAndroidInjector
    abstract DashboardWidgetService dashboardWidgetService();

    @ContributesAndroidInjector
    abstract PreviewPdfFragment previewPDFFragment();

    @ContributesAndroidInjector
    abstract SharedListFragment sharedFragment();

    @ContributesAndroidInjector
    abstract FeatureFragment featureFragment();

    @ContributesAndroidInjector
    abstract IndeterminateProgressDialog indeterminateProgressDialog();

    @ContributesAndroidInjector
    abstract SortingOrderDialogFragment sortingOrderDialogFragment();

    @ContributesAndroidInjector
    abstract ConfirmationDialogFragment confirmationDialogFragment();

    @ContributesAndroidInjector
    abstract ConflictsResolveDialog conflictsResolveDialog();

    @ContributesAndroidInjector
    abstract CreateFolderDialogFragment createFolderDialogFragment();

    @ContributesAndroidInjector
    abstract ExpirationDatePickerDialogFragment expirationDatePickerDialogFragment();

    @ContributesAndroidInjector
    abstract FileActivity fileActivity();

    @ContributesAndroidInjector
    abstract FileDownloadFragment fileDownloadFragment();

    @ContributesAndroidInjector
    abstract LoadingDialog loadingDialog();

    @ContributesAndroidInjector
    abstract LocalStoragePathPickerDialogFragment localStoragePathPickerDialogFragment();

    @ContributesAndroidInjector
    abstract LogsViewModel logsViewModel();

    @ContributesAndroidInjector
    abstract MainApp mainApp();

    @ContributesAndroidInjector
    abstract Migrations migrations();

    @ContributesAndroidInjector
    abstract NotificationWork notificationWork();

    @ContributesAndroidInjector
    abstract RemoveFilesDialogFragment removeFilesDialogFragment();

    @ContributesAndroidInjector
    abstract RenamePublicShareDialogFragment renamePublicShareDialogFragment();

    @ContributesAndroidInjector
    abstract SendShareDialog sendShareDialog();

    @ContributesAndroidInjector
    abstract SetupEncryptionDialogFragment setupEncryptionDialogFragment();

    @ContributesAndroidInjector
    abstract SharePasswordDialogFragment sharePasswordDialogFragment();

    @ContributesAndroidInjector
    abstract SyncedFolderPreferencesDialogFragment syncedFolderPreferencesDialogFragment();

    @ContributesAndroidInjector
    abstract ToolbarActivity toolbarActivity();

    @ContributesAndroidInjector
    abstract StoragePermissionDialogFragment storagePermissionDialogFragment();

    @ContributesAndroidInjector
    abstract OCFileListBottomSheetDialog ocfileListBottomSheetDialog();

    @ContributesAndroidInjector
    abstract RenameFileDialogFragment renameFileDialogFragment();

    @ContributesAndroidInjector
    abstract SyncFileNotEnoughSpaceDialogFragment syncFileNotEnoughSpaceDialogFragment();

    @ContributesAndroidInjector
    abstract DashboardWidgetConfigurationActivity dashboardWidgetConfigurationActivity();

    @ContributesAndroidInjector
    abstract DashboardWidgetProvider dashboardWidgetProvider();

    @ContributesAndroidInjector
    abstract GalleryFragmentBottomSheetDialog galleryFragmentBottomSheetDialog();

    @ContributesAndroidInjector
    abstract PreviewBitmapActivity previewBitmapActivity();

    @ContributesAndroidInjector
    abstract FileUploadHelper fileUploadHelper();

    @ContributesAndroidInjector
    abstract SslUntrustedCertDialog sslUntrustedCertDialog();

    @ContributesAndroidInjector
    abstract FileActionsBottomSheet fileActionsBottomSheet();

    @ContributesAndroidInjector
    abstract SendFilesDialog sendFilesDialog();

    @ContributesAndroidInjector
    abstract DocumentScanActivity documentScanActivity();

    @ContributesAndroidInjector
    abstract GroupfolderListFragment groupfolderListFragment();

    @ContributesAndroidInjector
    abstract LauncherActivity launcherActivity();

    @ContributesAndroidInjector
    abstract EditImageActivity editImageActivity();

    @ContributesAndroidInjector
    abstract ImageDetailFragment imageDetailFragment();

    @ContributesAndroidInjector
    abstract EtmBackgroundJobsFragment etmBackgroundJobsFragment();

    @ContributesAndroidInjector
    abstract BackgroundJobManagerImpl backgroundJobManagerImpl();

    @ContributesAndroidInjector
    abstract TestJob testJob();
    
    @ContributesAndroidInjector
    abstract InternalTwoWaySyncActivity internalTwoWaySyncActivity();
}
