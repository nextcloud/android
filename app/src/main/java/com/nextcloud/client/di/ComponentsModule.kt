/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-FileCopyrightText: 2024-2026 TSI-mc <surinder.kumar@t-systems.com>
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.di

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.nextcloud.client.documentscan.DocumentScanActivity
import com.nextcloud.client.editimage.EditImageActivity
import com.nextcloud.client.etm.EtmActivity
import com.nextcloud.client.etm.pages.EtmBackgroundJobsFragment
import com.nextcloud.client.jobs.BackgroundJobManagerImpl
import com.nextcloud.client.jobs.NotificationWork
import com.nextcloud.client.jobs.TestJob
import com.nextcloud.client.jobs.transfer.FileTransferService
import com.nextcloud.client.jobs.upload.FileUploadHelper
import com.nextcloud.client.logger.ui.LogsActivity
import com.nextcloud.client.logger.ui.LogsViewModel
import com.nextcloud.client.migrations.Migrations
import com.nextcloud.client.onboarding.FirstRunActivity
import com.nextcloud.client.onboarding.WhatsNewActivity
import com.nextcloud.client.widget.DashboardWidgetConfigurationActivity
import com.nextcloud.client.widget.DashboardWidgetProvider
import com.nextcloud.client.widget.DashboardWidgetService
import com.nextcloud.ui.ChooseAccountDialogFragment
import com.nextcloud.ui.ChooseStorageLocationDialogFragment
import com.nextcloud.ui.SetOnlineStatusBottomSheet
import com.nextcloud.ui.SetStatusMessageBottomSheet
import com.nextcloud.ui.albumItemActions.AlbumItemActionsBottomSheet
import com.nextcloud.ui.composeActivity.ComposeActivity
import com.nextcloud.ui.fileInfo.FileInfoFragment
import com.nextcloud.ui.fileactions.FileActionsBottomSheet
import com.nextcloud.ui.tags.TagManagementBottomSheet
import com.nextcloud.ui.trashbinFileActions.TrashbinFileActionsBottomSheet
import com.nmc.android.ui.LauncherActivity
import com.owncloud.android.MainApp
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.authentication.DeepLinkLoginActivity
import com.owncloud.android.files.BootupBroadcastReceiver
import com.owncloud.android.providers.DiskLruImageCacheFileProvider
import com.owncloud.android.providers.DocumentsStorageProvider
import com.owncloud.android.providers.FileContentProvider
import com.owncloud.android.providers.UsersAndGroupsSearchProvider
import com.owncloud.android.services.AccountManagerService
import com.owncloud.android.services.OperationsService
import com.owncloud.android.syncadapter.FileSyncService
import com.owncloud.android.ui.activity.AlbumsPickerActivity
import com.owncloud.android.ui.activity.BaseActivity
import com.owncloud.android.ui.activity.ConflictsResolveActivity
import com.owncloud.android.ui.activity.ContactsPreferenceActivity
import com.owncloud.android.ui.activity.CopyToClipboardActivity
import com.owncloud.android.ui.activity.DrawerActivity
import com.owncloud.android.ui.activity.ErrorsWhileCopyingHandlerActivity
import com.owncloud.android.ui.activity.ExternalSiteWebView
import com.owncloud.android.ui.activity.FileActivity
import com.owncloud.android.ui.activity.FileDisplayActivity
import com.owncloud.android.ui.activity.FilePickerActivity
import com.owncloud.android.ui.activity.FolderPickerActivity
import com.owncloud.android.ui.activity.InternalTwoWaySyncActivity
import com.owncloud.android.ui.activity.ManageAccountsActivity
import com.owncloud.android.ui.activity.ManageSpaceActivity
import com.owncloud.android.ui.activity.PassCodeActivity
import com.owncloud.android.ui.activity.ReceiveExternalFilesActivity
import com.owncloud.android.ui.activity.ReceiveExternalFilesActivity.DialogInputUploadFilename
import com.owncloud.android.ui.activity.RequestCredentialsActivity
import com.owncloud.android.ui.activity.RichDocumentsEditorWebView
import com.owncloud.android.ui.activity.SettingsActivity
import com.owncloud.android.ui.activity.ShareActivity
import com.owncloud.android.ui.activity.SsoGrantPermissionActivity
import com.owncloud.android.ui.activity.SyncedFoldersActivity
import com.owncloud.android.ui.activity.TextEditorWebView
import com.owncloud.android.ui.activity.ToolbarActivity
import com.owncloud.android.ui.activity.UploadFilesActivity
import com.owncloud.android.ui.activity.UserInfoActivity
import com.owncloud.android.ui.dialog.AccountRemovalDialog
import com.owncloud.android.ui.dialog.AppPassCodeDialog
import com.owncloud.android.ui.dialog.ChooseRichDocumentsTemplateDialogFragment
import com.owncloud.android.ui.dialog.ChooseTemplateDialogFragment
import com.owncloud.android.ui.dialog.ConfirmationDialogFragment
import com.owncloud.android.ui.dialog.CreateAlbumDialogFragment
import com.owncloud.android.ui.dialog.CreateFolderDialogFragment
import com.owncloud.android.ui.dialog.ExpirationDatePickerDialogFragment
import com.owncloud.android.ui.dialog.IndeterminateProgressDialog
import com.owncloud.android.ui.dialog.LoadingDialog
import com.owncloud.android.ui.dialog.LocalStoragePathPickerDialogFragment
import com.owncloud.android.ui.dialog.MultipleAccountsDialog
import com.owncloud.android.ui.dialog.RemoveFilesDialogFragment
import com.owncloud.android.ui.dialog.RenameFileDialogFragment
import com.owncloud.android.ui.dialog.SendFilesDialog
import com.owncloud.android.ui.dialog.SendShareDialog
import com.owncloud.android.ui.dialog.SharePasswordDialogFragment
import com.owncloud.android.ui.dialog.SortingOrderDialogFragment
import com.owncloud.android.ui.dialog.SslUntrustedCertDialog
import com.owncloud.android.ui.dialog.StoragePermissionDialogFragment
import com.owncloud.android.ui.dialog.SyncFileNotEnoughSpaceDialogFragment
import com.owncloud.android.ui.dialog.SyncedFolderPreferencesDialogFragment
import com.owncloud.android.ui.dialog.TermsOfServiceDialog
import com.owncloud.android.ui.dialog.ThemeSelectionDialog
import com.owncloud.android.ui.dialog.conflict.ConflictsResolveDialog
import com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment
import com.owncloud.android.ui.fragment.ActivitiesFragment
import com.owncloud.android.ui.fragment.ExtendedListFragment
import com.owncloud.android.ui.fragment.FeatureFragment
import com.owncloud.android.ui.fragment.FileDetailActivitiesFragment
import com.owncloud.android.ui.fragment.FileDetailFragment
import com.owncloud.android.ui.fragment.FileDetailSharingFragment
import com.owncloud.android.ui.fragment.FileDetailsSharingProcessFragment
import com.owncloud.android.ui.fragment.GalleryFragment
import com.owncloud.android.ui.fragment.GalleryFragmentBottomSheetDialog
import com.owncloud.android.ui.fragment.GroupfolderListFragment
import com.owncloud.android.ui.fragment.OCFileListBottomSheetDialog
import com.owncloud.android.ui.fragment.OCFileListFragment
import com.owncloud.android.ui.fragment.SharedListFragment
import com.owncloud.android.ui.fragment.UnifiedSearchFragment
import com.owncloud.android.ui.fragment.albums.AlbumItemsFragment
import com.owncloud.android.ui.fragment.albums.AlbumsFragment
import com.owncloud.android.ui.fragment.albums.bottomsheet.AlbumSharingBottomSheet
import com.owncloud.android.ui.fragment.community.CommunityFragment
import com.owncloud.android.ui.fragment.contactsbackup.BackupFragment
import com.owncloud.android.ui.fragment.contactsbackup.BackupListFragment
import com.owncloud.android.ui.fragment.localfilelist.LocalFileListFragment
import com.owncloud.android.ui.fragment.notifications.NotificationsFragment
import com.owncloud.android.ui.fragment.uploadList.UploadListFragment
import com.owncloud.android.ui.navigation.NavigatorActivity
import com.owncloud.android.ui.preview.FileDownloadFragment
import com.owncloud.android.ui.preview.PreviewBitmapActivity
import com.owncloud.android.ui.preview.PreviewImageActivity
import com.owncloud.android.ui.preview.PreviewImageFragment
import com.owncloud.android.ui.preview.PreviewPlaybackFragment
import com.owncloud.android.ui.preview.PreviewTextFileFragment
import com.owncloud.android.ui.preview.PreviewTextFragment
import com.owncloud.android.ui.preview.PreviewTextStringFragment
import com.owncloud.android.ui.preview.pdf.PreviewPdfFragment
import com.owncloud.android.ui.trashbin.TrashbinFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Register classes that require dependency injection. This class is used by Dagger compiler only.
 */
@Module
@Suppress("TooManyFunctions")
internal interface ComponentsModule {
    @ContributesAndroidInjector
    fun trashbinFragment(): TrashbinFragment

    @ContributesAndroidInjector
    fun uploadListFragment(): UploadListFragment

    @ContributesAndroidInjector
    fun activitiesFragment(): ActivitiesFragment

    @ContributesAndroidInjector
    fun notificationFragment(): NotificationsFragment

    @ContributesAndroidInjector
    fun authenticatorActivity(): AuthenticatorActivity

    @ContributesAndroidInjector
    fun baseActivity(): BaseActivity

    @ContributesAndroidInjector
    fun conflictsResolveActivity(): ConflictsResolveActivity

    @ContributesAndroidInjector
    fun contactsPreferenceActivity(): ContactsPreferenceActivity

    @ContributesAndroidInjector
    fun copyToClipboardActivity(): CopyToClipboardActivity

    @ContributesAndroidInjector
    fun deepLinkLoginActivity(): DeepLinkLoginActivity

    @ContributesAndroidInjector
    fun drawerActivity(): DrawerActivity

    @ContributesAndroidInjector
    fun errorsWhileCopyingHandlerActivity(): ErrorsWhileCopyingHandlerActivity

    @ContributesAndroidInjector
    fun externalSiteWebView(): ExternalSiteWebView

    @ContributesAndroidInjector
    fun fileDisplayActivity(): FileDisplayActivity

    @ContributesAndroidInjector
    fun filePickerActivity(): FilePickerActivity

    @ContributesAndroidInjector
    fun firstRunActivity(): FirstRunActivity

    @ContributesAndroidInjector
    fun folderPickerActivity(): FolderPickerActivity

    @ContributesAndroidInjector
    fun logsActivity(): LogsActivity

    @ContributesAndroidInjector
    fun manageAccountsActivity(): ManageAccountsActivity

    @ContributesAndroidInjector
    fun manageSpaceActivity(): ManageSpaceActivity

    @ContributesAndroidInjector
    fun composeActivity(): ComposeActivity

    @ContributesAndroidInjector
    fun passCodeActivity(): PassCodeActivity

    @ContributesAndroidInjector
    fun previewImageActivity(): PreviewImageActivity

    @ContributesAndroidInjector
    fun receiveExternalFilesActivity(): ReceiveExternalFilesActivity

    @ContributesAndroidInjector
    fun requestCredentialsActivity(): RequestCredentialsActivity

    @ContributesAndroidInjector
    fun settingsActivity(): SettingsActivity

    @ContributesAndroidInjector
    fun shareActivity(): ShareActivity

    @ContributesAndroidInjector
    fun ssoGrantPermissionActivity(): SsoGrantPermissionActivity

    @ContributesAndroidInjector
    fun syncedFoldersActivity(): SyncedFoldersActivity

    @ContributesAndroidInjector
    fun trashbinFileActionsBottomSheet(): TrashbinFileActionsBottomSheet

    @ContributesAndroidInjector
    fun uploadFilesActivity(): UploadFilesActivity

    @ContributesAndroidInjector
    fun userInfoActivity(): UserInfoActivity

    @ContributesAndroidInjector
    fun whatsNewActivity(): WhatsNewActivity

    @ContributesAndroidInjector
    fun etmActivity(): EtmActivity

    @ContributesAndroidInjector
    fun richDocumentsWebView(): RichDocumentsEditorWebView

    @ContributesAndroidInjector
    fun textEditorWebView(): TextEditorWebView

    @ContributesAndroidInjector
    fun extendedListFragment(): ExtendedListFragment

    @ContributesAndroidInjector
    fun fileDetailFragment(): FileDetailFragment

    @ContributesAndroidInjector
    fun localFileListFragment(): LocalFileListFragment

    @ContributesAndroidInjector
    fun ocFileListFragment(): OCFileListFragment

    @ContributesAndroidInjector
    fun fileDetailActivitiesFragment(): FileDetailActivitiesFragment

    @ContributesAndroidInjector
    fun fileDetailsSharingProcessFragment(): FileDetailsSharingProcessFragment

    @ContributesAndroidInjector
    fun fileDetailSharingFragment(): FileDetailSharingFragment

    @ContributesAndroidInjector
    fun chooseTemplateDialogFragment(): ChooseTemplateDialogFragment

    @ContributesAndroidInjector
    fun accountRemovalDialog(): AccountRemovalDialog

    @ContributesAndroidInjector
    fun chooseRichDocumentsTemplateDialogFragment(): ChooseRichDocumentsTemplateDialogFragment

    @ContributesAndroidInjector
    fun contactsBackupFragment(): BackupFragment

    @ContributesAndroidInjector
    fun previewImageFragment(): PreviewImageFragment

    @ContributesAndroidInjector
    fun chooseContactListFragment(): BackupListFragment

    @ContributesAndroidInjector
    fun previewTextFragment(): PreviewTextFragment

    @ContributesAndroidInjector
    fun chooseAccountDialogFragment(): ChooseAccountDialogFragment

    @ContributesAndroidInjector
    fun setOnlineStatusBottomSheet(): SetOnlineStatusBottomSheet

    @ContributesAndroidInjector
    fun previewPlaybackFragment(): PreviewPlaybackFragment

    @ContributesAndroidInjector
    fun previewTextFileFragment(): PreviewTextFileFragment

    @ContributesAndroidInjector
    fun previewTextStringFragment(): PreviewTextStringFragment

    @ContributesAndroidInjector
    fun searchFragment(): UnifiedSearchFragment

    @ContributesAndroidInjector
    fun photoFragment(): GalleryFragment

    @ContributesAndroidInjector
    fun multipleAccountsDialog(): MultipleAccountsDialog

    @ContributesAndroidInjector
    fun dialogInputUploadFilename(): DialogInputUploadFilename

    @ContributesAndroidInjector
    fun bootupBroadcastReceiver(): BootupBroadcastReceiver

    @ContributesAndroidInjector
    fun notificationWorkBroadcastReceiver(): NotificationWork.NotificationReceiver

    @ContributesAndroidInjector
    fun fileContentProvider(): FileContentProvider

    @ContributesAndroidInjector
    fun usersAndGroupsSearchProvider(): UsersAndGroupsSearchProvider

    @ContributesAndroidInjector
    fun diskLruImageCacheFileProvider(): DiskLruImageCacheFileProvider

    @ContributesAndroidInjector
    fun documentsStorageProvider(): DocumentsStorageProvider

    @ContributesAndroidInjector
    fun accountManagerService(): AccountManagerService

    @ContributesAndroidInjector
    fun operationsService(): OperationsService

    @ContributesAndroidInjector
    fun fileDownloaderService(): FileTransferService

    @ContributesAndroidInjector
    fun fileSyncService(): FileSyncService

    @ContributesAndroidInjector
    fun dashboardWidgetService(): DashboardWidgetService

    @ContributesAndroidInjector
    fun previewPDFFragment(): PreviewPdfFragment

    @ContributesAndroidInjector
    fun sharedFragment(): SharedListFragment

    @ContributesAndroidInjector
    fun featureFragment(): FeatureFragment

    @ContributesAndroidInjector
    fun indeterminateProgressDialog(): IndeterminateProgressDialog

    @ContributesAndroidInjector
    fun sortingOrderDialogFragment(): SortingOrderDialogFragment

    @ContributesAndroidInjector
    fun confirmationDialogFragment(): ConfirmationDialogFragment

    @ContributesAndroidInjector
    fun conflictsResolveDialog(): ConflictsResolveDialog

    @ContributesAndroidInjector
    fun createFolderDialogFragment(): CreateFolderDialogFragment

    @ContributesAndroidInjector
    fun expirationDatePickerDialogFragment(): ExpirationDatePickerDialogFragment

    @ContributesAndroidInjector
    fun fileActivity(): FileActivity

    @ContributesAndroidInjector
    fun fileDownloadFragment(): FileDownloadFragment

    @ContributesAndroidInjector
    fun loadingDialog(): LoadingDialog

    @ContributesAndroidInjector
    fun localStoragePathPickerDialogFragment(): LocalStoragePathPickerDialogFragment

    @ContributesAndroidInjector
    fun logsViewModel(): LogsViewModel

    @ContributesAndroidInjector
    fun mainApp(): MainApp

    @ContributesAndroidInjector
    fun migrations(): Migrations

    @ContributesAndroidInjector
    fun notificationWork(): NotificationWork

    @ContributesAndroidInjector
    fun removeFilesDialogFragment(): RemoveFilesDialogFragment

    @ContributesAndroidInjector
    fun sendShareDialog(): SendShareDialog

    @ContributesAndroidInjector
    fun setupEncryptionDialogFragment(): SetupEncryptionDialogFragment

    @ContributesAndroidInjector
    fun chooseStorageLocationDialogFragment(): ChooseStorageLocationDialogFragment

    @ContributesAndroidInjector
    fun themeSelectionDialog(): ThemeSelectionDialog

    @ContributesAndroidInjector
    fun appPassCodeDialog(): AppPassCodeDialog

    @ContributesAndroidInjector
    fun sharePasswordDialogFragment(): SharePasswordDialogFragment

    @ContributesAndroidInjector
    fun syncedFolderPreferencesDialogFragment(): SyncedFolderPreferencesDialogFragment

    @ContributesAndroidInjector
    fun toolbarActivity(): ToolbarActivity

    @ContributesAndroidInjector
    fun storagePermissionDialogFragment(): StoragePermissionDialogFragment

    @ContributesAndroidInjector
    fun ocfileListBottomSheetDialog(): OCFileListBottomSheetDialog

    @ContributesAndroidInjector
    fun renameFileDialogFragment(): RenameFileDialogFragment

    @ContributesAndroidInjector
    fun syncFileNotEnoughSpaceDialogFragment(): SyncFileNotEnoughSpaceDialogFragment

    @ContributesAndroidInjector
    fun dashboardWidgetConfigurationActivity(): DashboardWidgetConfigurationActivity

    @ContributesAndroidInjector
    fun dashboardWidgetProvider(): DashboardWidgetProvider

    @ContributesAndroidInjector
    fun galleryFragmentBottomSheetDialog(): GalleryFragmentBottomSheetDialog

    @ContributesAndroidInjector
    fun previewBitmapActivity(): PreviewBitmapActivity

    @ContributesAndroidInjector
    fun fileUploadHelper(): FileUploadHelper

    @ContributesAndroidInjector
    fun sslUntrustedCertDialog(): SslUntrustedCertDialog

    @ContributesAndroidInjector
    fun fileActionsBottomSheet(): FileActionsBottomSheet

    @ContributesAndroidInjector
    fun sendFilesDialog(): SendFilesDialog

    @ContributesAndroidInjector
    fun documentScanActivity(): DocumentScanActivity

    @ContributesAndroidInjector
    fun groupfolderListFragment(): GroupfolderListFragment

    @ContributesAndroidInjector
    fun launcherActivity(): LauncherActivity

    @ContributesAndroidInjector
    fun editImageActivity(): EditImageActivity

    @ContributesAndroidInjector
    fun fileInfoFragment(): FileInfoFragment

    @ContributesAndroidInjector
    fun etmBackgroundJobsFragment(): EtmBackgroundJobsFragment

    @ContributesAndroidInjector
    fun backgroundJobManagerImpl(): BackgroundJobManagerImpl

    @ContributesAndroidInjector
    fun testJob(): TestJob

    @ContributesAndroidInjector
    fun internalTwoWaySyncActivity(): InternalTwoWaySyncActivity

    @OptIn(UnstableApi::class)
    @ContributesAndroidInjector
    fun termsOfServiceDialog(): TermsOfServiceDialog

    @ContributesAndroidInjector
    fun setStatusMessageBottomSheet(): SetStatusMessageBottomSheet

    @ContributesAndroidInjector
    fun tagManagementBottomSheet(): TagManagementBottomSheet

    @ContributesAndroidInjector
    fun navigatorActivity(): NavigatorActivity

    @ContributesAndroidInjector
    fun communityFragment(): CommunityFragment

    @ContributesAndroidInjector
    fun albumsPickerActivity(): AlbumsPickerActivity

    @ContributesAndroidInjector
    fun createAlbumDialogFragment(): CreateAlbumDialogFragment

    @ContributesAndroidInjector
    fun albumsFragment(): AlbumsFragment

    @ContributesAndroidInjector
    fun albumItemsFragment(): AlbumItemsFragment

    @ContributesAndroidInjector
    fun albumItemActionsBottomSheet(): AlbumItemActionsBottomSheet

    @ContributesAndroidInjector
    fun albumSharingBottomSheet(): AlbumSharingBottomSheet
}
