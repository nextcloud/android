<!--
  ~ Nextcloud - Android Client
  ~
  ~ SPDX-FileCopyrightText: 2026 Nextcloud GmbH and Nextcloud contributors
  ~ SPDX-License-Identifier: AGPL-3.0-or-later
-->

# E2EE vault analysis

This document describes the current end-to-end encryption media flow in the Android client and proposes a
target architecture for turning E2EE folders into locally protected media vaults.

The analysis is based on the code currently present in this repository. It intentionally does not assume a
new server protocol.

## Current E2EE model

### Important classes

| Area | Classes and files | Current responsibility |
| --- | --- | --- |
| File model | `com.owncloud.android.datamodel.OCFile` | Stores encrypted and decrypted paths, local storage path, preview flags, E2EE state and E2EE counter. |
| File database | `com.nextcloud.client.database.entity.FileEntity`, `com.nextcloud.client.database.dao.FileDao` | Persists file metadata, including `path`, `path_decrypted`, `storage_path`, `is_encrypted` and `e2e_counter`. |
| Storage facade | `com.owncloud.android.datamodel.FileDataStorageManager` | Resolves files by encrypted/decrypted paths, maps decrypted paths to encrypted paths, reads E2EE capability version, creates temporary encrypted upload folders. |
| Storage paths | `com.owncloud.android.utils.FileStorageUtils` | Builds local save paths and temporary download/upload paths. |
| E2EE v1 crypto | `com.owncloud.android.utils.EncryptionUtils` | Downloads/decrypts v1 metadata, encrypts/decrypts files, handles AES-GCM/RSA helpers, locks/unlocks folders. |
| E2EE v2 crypto | `com.owncloud.android.utils.EncryptionUtilsV2` | Parses, verifies, decrypts, migrates, updates, signs and uploads v2 metadata. |
| E2EE setup UI | `com.owncloud.android.ui.dialog.setupEncryption.SetupEncryptionDialogFragment` | Creates or imports E2EE keys and stores local E2EE setup data. |
| Local secret storage | `com.owncloud.android.datamodel.ArbitraryDataProvider`, `ArbitraryDataProviderImpl` | Stores arbitrary account key/value data in the app database. This currently includes E2EE private key and mnemonic. |
| Folder navigation | `com.owncloud.android.ui.fragment.OCFileListFragment` | Detects encrypted folder clicks and either opens the folder or launches E2EE setup. |
| Download/decrypt | `com.owncloud.android.operations.DownloadFileOperation` | Downloads remote files, then decrypts encrypted files to the normal local save path. |

### How an E2EE folder is detected

The central flag is `OCFile.isEncrypted()`. It is backed by the database column exposed through
`FileEntity.isEncrypted`.

`OCFile` keeps two path concepts:

- `remotePath`: the encrypted server path/name for E2EE entries.
- `decryptedRemotePath`: the user-visible path/name.

`OCFile.getFileName()` returns the decrypted file name for encrypted files. `OCFile.getEncryptedFileName()`
extracts the encrypted basename from `remotePath`.

`FileDataStorageManager` offers both encrypted and decrypted lookup paths:

- `getFileByEncryptedRemotePath(path)`
- `getFileByDecryptedRemotePath(path)`
- `getEncryptedRemotePath(decryptedRemotePath)`

This means the UI can display decrypted names while operations against the server still use encrypted names.

### Current encrypted folder entry flow

`OCFileListFragment.folderOnItemClick()` is the current folder entry point.

For encrypted folders it:

1. checks that E2EE is available in server capabilities;
2. checks that E2EE setup exists through `FileOperationsHelper.isEndToEndEncryptionSetup()`;
3. if setup exists, calls `browseToFolder(file, position)` directly;
4. if setup is missing, opens `SetupEncryptionDialogFragment`.

There is currently no biometric gate before entering an encrypted folder. Once E2EE setup data exists locally,
the folder can be opened like any other folder.

### How metadata is fetched and decrypted

`EncryptionUtils.downloadFolderMetadata(folder, client, context, user)` performs the metadata download using the
folder local ID and detects whether the metadata is v1 or v2.

For E2EE v1:

- the folder metadata key is decrypted with the locally stored private key;
- encrypted file metadata is decrypted with the metadata key;
- file names, MIME types, file keys, IVs/nonces and authentication tags are loaded into decrypted metadata models;
- the mnemonic is used for checksum verification.

For E2EE v2:

- `EncryptionUtilsV2.parseAnyMetadata()` retrieves the private key from `ArbitraryDataProvider`;
- `EncryptionUtilsV2.decryptFolderMetadataFile()` decrypts the metadata key for top-level folders;
- subfolders reuse the top-most metadata key;
- `EncryptionUtilsV2.verifyMetadata()` checks counters, CMS signatures and metadata key checksums;
- encrypted metadata is decrypted with AES-GCM and decompressed from gzip.

The v2 metadata stores per-file values needed by the client:

- encrypted file name;
- decrypted file name;
- MIME type;
- file key;
- nonce;
- authentication tag.

### How keys and mnemonic are currently stored

`SetupEncryptionDialogFragment` stores these values through `ArbitraryDataProvider.storeOrUpdateKeyValue()`:

- `EncryptionUtils.PRIVATE_KEY`
- `EncryptionUtils.PUBLIC_KEY`
- `EncryptionUtils.MNEMONIC`

`ArbitraryDataProviderImpl` persists them in the app database through `ArbitraryDataDao`. There is no current
use of AndroidX BiometricPrompt, `AndroidKeyStore`, `MasterKey` or `EncryptedSharedPreferences` for these E2EE
secrets.

The current storage is private to the app, but the private key and mnemonic are not cryptographically bound to
fresh local user authentication. This is a main security gap for the vault requirement.

### How the mnemonic is used

The mnemonic is used during setup/import to decrypt the server-stored encrypted private key with
`CryptoHelper.decryptPrivateKey()`. For new setup it is generated and used to encrypt the private key before
uploading it to the server.

After setup, the current client also stores the mnemonic locally through `ArbitraryDataProvider`. E2EE v1
metadata verification reads it from local storage and uses it for checksum verification.

For the target vault behavior, the mnemonic must not remain readable as plaintext from the ordinary arbitrary
data table.

## Current E2EE file download and plaintext storage

### Download flow

`DownloadFileOperation` downloads the encrypted server object first:

1. temporary ciphertext path is built from `FileStorageUtils.getTemporalPath(accountName) + file.remotePath`;
2. `DownloadFileRemoteOperation(file.remotePath, tmpFolder, file.fileLength)` downloads the remote encrypted file;
3. if `file.isEncrypted`, `handleDecryption()` loads the parent folder metadata;
4. the file key, nonce/IV and authentication tag are read from metadata by encrypted file name;
5. `EncryptionUtils.decryptFile()` decrypts the temporary ciphertext into `File(savePath)`.

`savePath` is based on `FileStorageUtils.getDefaultSavePathFor(accountName, file)`, which uses
`file.getDecryptedRemotePath()`. Therefore a downloaded E2EE file is currently written as a decrypted file under
the normal local Nextcloud account storage path.

The temporary download directory is built by `FileStorageUtils.getTemporalPath()`. The encrypted upload temporary
directory is separate and uses `FileStorageUtils.getTemporalEncryptedFolderPath()`, which is in the app files
directory.

### Plaintext on disk

Plaintext E2EE downloads are currently written to the normal local file path and then treated as downloaded
files. Image and video viewers later read from `OCFile.storagePath` or `OCFile.storageUri`.

This behavior matches the existing offline-file model, but it does not match the vault requirement because the
plaintext can persist after preview.

### Authentication tag handling

The current file format uses AES-GCM with a single IV/nonce and a single authentication tag per file. The tag is
stored in metadata.

`EncryptionUtils.decryptFile()` reads the encrypted file and writes decrypted bytes to the output file, then
calls `cipher.doFinal()` and compares the computed authentication tag with the metadata tag. The method catches
exceptions internally and logs a generic error. This path must be audited before it is reused for vault media,
because callers need an explicit hard failure when authentication fails.

## Current thumbnails and gallery behavior

### Important classes

| Area | Classes and files | Current responsibility |
| --- | --- | --- |
| Thumbnail cache | `com.owncloud.android.datamodel.ThumbnailsCacheManager` | Stores thumbnails/resized previews in `cacheDir/thumbnailCache` through `DiskLruImageCache`. |
| List binding | `com.owncloud.android.ui.adapter.OCFileListDelegate` | Binds rows and gallery rows, reads cached resized images, starts generation jobs. |
| Display helpers | `com.owncloud.android.utils.DisplayUtils` | Loads thumbnails from cache or starts `ThumbnailGenerationTask`. |
| Gallery job | `com.nextcloud.client.jobs.gallery.GalleryImageGenerationJob` | Generates gallery thumbnails from local files or server previews. |
| Disk cache implementation | `com.owncloud.android.ui.adapter.DiskLruImageCache` | JPEG disk LRU cache used by `ThumbnailsCacheManager`. |

### Normal thumbnail flow

For normal files, thumbnail generation uses these steps:

1. try `ThumbnailsCacheManager.getBitmapFromDiskCache()`;
2. if the file is downloaded, decode the local image or video thumbnail from `OCFile.storagePath`;
3. if the file is not downloaded and a client is available, call the server preview endpoint;
4. store the bitmap in the disk cache with keys such as `PREFIX_THUMBNAIL + remoteId` or
   `PREFIX_RESIZED_IMAGE + remoteId`.

The server preview endpoint is built around `/index.php/core/preview` for normal files.

### Current E2EE thumbnail behavior

There is no dedicated E2EE thumbnail provider. The same code path is used for encrypted and non-encrypted media.

Consequences:

- if an E2EE media file is not downloaded, the normal preview path can try the server preview endpoint, but the
  server only has ciphertext and must not receive plaintext;
- if an E2EE media file is downloaded, the local thumbnail path can decode from the decrypted `storagePath`;
- generated E2EE thumbnails can be written unencrypted to the shared thumbnail disk cache under app cache.

The current behavior avoids sending decrypted bytes to the server, but the cache is not a vault-safe thumbnail
cache because it stores clear thumbnails persistently.

## Current image viewer behavior

### Important classes

| Class | Current responsibility |
| --- | --- |
| `PreviewImageActivity` | Hosts media paging with `ViewPager2`; observes encrypted download completion. |
| `PreviewMediaPagerAdapter` | Builds the media list with `FileDataStorageManager.getFolderImagesAndVideos()` and selects image/video/download fragments. |
| `PreviewImageFragment` | Displays downloaded or resized images. Full local preview reads from `OCFile.storagePath`. |
| `FileDownloadFragment` | Download/progress screen used by encrypted files that cannot be previewed before download. |

### Current encrypted image opening

When the user opens an encrypted image that is not downloaded:

1. `OCFileListFragment.handlePendingDownloadFile()` avoids the normal direct preview paths because the file is
   encrypted;
2. `FileDisplayActivity.startDownloadForPreview()` starts a download;
3. `DownloadFileOperation` decrypts the file into the normal local save path;
4. `PreviewImageActivity` observes completion;
5. `PreviewMediaPagerAdapter` returns `PreviewImageFragment` once `file.isDown` is true;
6. `PreviewImageFragment.LoadBitmapTask` decodes from `OCFile.storagePath`.

The integrated viewer is reusable, but its current source abstraction is file-path based. It assumes decrypted
local files are safe to read from disk.

## Current video player behavior

### Important classes

| Class | Current responsibility |
| --- | --- |
| `PreviewMediaActivity` | Full-screen audio/video player activity. |
| `PreviewMediaFragment` | Audio/video fragment used by the media pager. |
| `NextcloudExoPlayer` | Creates a Media3 ExoPlayer with a `DefaultDataSource.Factory` and an OkHttp data source backed by the Nextcloud client. |
| `StreamMediaFileOperation` | Requests a direct server streaming URL using `/ocs/v2.php/apps/dav/api/v1/direct`. |

### Normal video flow

For normal videos:

- if the file is downloaded, the player uses `OCFile.storageUri`;
- if the file is not downloaded, the client requests a direct stream URL with `StreamMediaFileOperation` and
  gives that URL to ExoPlayer.

### Current encrypted video flow

Encrypted media is excluded from direct stream preview paths in `OCFileListFragment`. The encrypted file is
downloaded first and decrypted to the normal local save path. After that, `PreviewMediaActivity` or
`PreviewMediaFragment` plays from `OCFile.storageUri`.

The existing ExoPlayer setup is reusable, but the encrypted video source must not be a persistent plaintext file.

## Current local lock and secure display behavior

The app has an existing global lock mechanism:

- `PassCodeManager`
- `PassCodeActivity`
- `RequestCredentialsActivity`
- `DeviceCredentialUtils`

`PassCodeManager` can set `FLAG_SECURE` and can request either an app passcode or Android device credentials.
The timeout is currently `PASS_CODE_TIMEOUT = 5000`.

This mechanism is useful as a reference, but it is not a vault session:

- it is global to the app, not scoped to E2EE folders;
- it does not gate E2EE folder entry;
- it does not protect E2EE private key or mnemonic through Android Keystore;
- it does not clear decrypted media references when an E2EE vault locks.

## Existing tests found

### E2EE and encrypted files

- `app/src/androidTest/java/com/owncloud/android/EncryptionIT.kt`
- `app/src/androidTest/java/com/owncloud/android/util/EncryptionTestIT.java`
- `app/src/androidTest/java/com/owncloud/android/utils/EncryptionUtilsIT.kt`
- `app/src/androidTest/java/com/owncloud/android/utils/EncryptionUtilsV2IT.kt`
- `app/src/androidTest/java/com/owncloud/android/utils/EncryptionUtilsMetadataVerificationTests.kt`
- `app/src/androidTest/java/com/owncloud/android/operations/DeleteE2ERemoteOperationIT.kt`
- `app/src/androidTest/java/com/owncloud/android/ui/dialog/SetupEncryptionDialogFragmentIT.kt`
- `app/src/test/java/com/owncloud/android/utils/E2ECertificateRenewalMetadataVerificationTest.kt`
- `app/src/test/java/com/owncloud/android/utils/E2EVersionHelperTest.kt`

These tests cover metadata encryption/decryption, migration, signing, verification, key utilities and E2EE
operation behavior. They do not cover biometric vault entry, vault session expiry, secure E2EE thumbnail cache
or no-plaintext media preview.

### Download/cache/gallery/preview/media

- `app/src/androidTest/java/com/owncloud/android/DownloadIT.kt`
- `app/src/androidTest/java/com/nextcloud/client/files/download/DownloaderServiceTest.kt`
- `app/src/androidTest/java/com/nextcloud/client/files/download/RegistryTest.kt`
- `app/src/androidTest/java/com/nextcloud/client/files/download/TransferManagerConnectionTest.kt`
- `app/src/androidTest/java/com/nextcloud/client/files/download/TransferManagerTest.kt`
- `app/src/androidTest/java/com/owncloud/android/ui/fragment/GalleryFragmentIT.kt`
- `app/src/test/java/com/owncloud/android/ui/adapter/GalleryAdapterTest.kt`
- `app/src/androidTest/java/com/owncloud/android/ui/preview/PreviewImageActivityIT.kt`
- `app/src/androidTest/java/com/owncloud/android/ui/preview/PreviewBitmapScreenshotIT.kt`
- `app/src/androidTest/java/com/owncloud/android/ui/preview/PreviewTextFileFragmentTest.kt`
- `app/src/test/java/com/nextcloud/client/media/AudioFocusManagerTest.kt`
- `app/src/test/java/com/nextcloud/client/media/AudioFocusTest.kt`
- `app/src/test/java/com/nextcloud/client/media/PlayerStateMachineTest.kt`
- `app/src/test/java/com/owncloud/android/datamodel/FileDataStorageManagerTriggerMediaScanTest.kt`

These tests cover normal media/gallery behavior, but not vault-safe E2EE media sources.

### Local security

- `app/src/test/java/com/owncloud/android/authentication/PassCodeManagerTest.kt`
- `app/src/androidTest/java/com/owncloud/android/authentication/PassCodeManagerIT.kt`
- `app/src/androidTest/java/com/owncloud/android/ui/activity/PassCodeActivityIT.kt`

These tests cover the existing app lock, not a per-vault biometric session or Keystore-bound E2EE secrets.

## Security risks in the current behavior

1. `PRIVATE_KEY` and `MNEMONIC` are stored through `ArbitraryDataProviderImpl` in the app database, not through
   a Keystore-backed store requiring recent user authentication.
2. Entering an encrypted folder only checks that E2EE setup exists; it does not require biometric authentication.
3. E2EE media downloaded for preview is decrypted to the normal local save path and can persist there.
4. E2EE thumbnails generated from downloaded plaintext can be stored unencrypted in the normal thumbnail disk
   cache.
5. Current preview code can read decrypted E2EE media from `storagePath` and expose "open with" style flows that
   hand a local URI to another app.
6. Current image/video preview activities do not appear to enable strict `FLAG_SECURE` based on E2EE context.
7. The current E2EE file decrypt method catches errors internally; vault code should expose authentication
   failures explicitly and must never show bytes from a file with an invalid tag.
8. The normal server preview code path is not explicitly blocked for E2EE files in the thumbnail layer.

## Protocol constraints for video

The current E2EE file format is not chunked:

- algorithm: AES-GCM;
- one file key per file;
- one IV/nonce per file;
- one authentication tag per file;
- authentication data is stored in folder metadata;
- there is no per-chunk nonce/tag table in the Android metadata model.

With this format, authentic random-access plaintext streaming is not available. A player cannot safely request an
arbitrary plaintext byte range and have the client decrypt only that range while preserving the current
cryptographic guarantees.

The safe behavior is:

1. download the complete ciphertext to app-private storage;
2. authenticate and decrypt locally;
3. only expose plaintext to the in-app player after authentication succeeds;
4. avoid any persistent plaintext video file.

Seeking can be limited or expensive with the current protocol because a random seek either needs a seekable
plaintext representation or re-decryption from the beginning. A future chunked E2EE format could support
authenticated range access, but this should not be emulated by bypassing the full-file authentication tag.

## Proposed target architecture

The target design should be added behind E2EE checks so normal folders, previews, downloads, uploads and streams
continue to use the existing paths.

The Keystore migration plan for the private key and mnemonic is documented in `docs/e2ee-vault/KEYSTORE_MIGRATION.md`.

### Proposed components

| Component | Suggested package | Responsibility |
| --- | --- | --- |
| `E2eeVaultSession` | `com.nextcloud.client.e2ee.vault` | Tracks unlocked vault sessions per account/top-level encrypted folder, expiry time and lock state. |
| `E2eeVaultSessionConfig` | `com.nextcloud.client.e2ee.vault` | Centralizes the initial session duration and later settings hooks. |
| `VaultBiometricManager` | `com.nextcloud.client.e2ee.vault` | Wraps AndroidX BiometricPrompt and exposes success/cancel/failure results to callers. |
| `E2eeSecretStore` | `com.nextcloud.client.e2ee.vault` | Stores and retrieves E2EE private key/mnemonic through Android Keystore-protected encrypted data. |
| `E2eeMetadataRepository` | `com.nextcloud.client.e2ee.vault` | Loads metadata through existing E2EE utilities, but obtains secrets only through the vault secret store. |
| `E2eeMediaRepository` | `com.nextcloud.client.e2ee.vault.media` | Downloads ciphertext to private cache, validates/decrypts media for viewer/player use, and owns cleanup. |
| `E2eeThumbnailProvider` | `com.nextcloud.client.e2ee.vault.media` | Generates thumbnails locally after unlock and never calls the server preview endpoint for E2EE media. |
| `E2eeThumbnailMemoryCache` | `com.nextcloud.client.e2ee.vault.media` | First vault-safe thumbnail cache. Stores plaintext thumbnails only in memory during an unlocked session. |
| `E2eeMediaDataSource` | `com.nextcloud.client.e2ee.vault.media` | Media3 data source or equivalent pipe-based source for decrypted plaintext streams without persistent plaintext files. |

### Secret protection proposal

The vault should not only hide the UI. It should bind access to local E2EE secrets to recent user authentication.

The preferred Android design is:

1. store the encrypted private key/mnemonic blob in app-private storage or database;
2. protect the blob key with `AndroidKeyStore`;
3. create the Keystore key with user authentication required;
4. use a short centralized validity duration matching the vault session;
5. after biometric success, allow the session to retrieve/decrypt E2EE secrets;
6. clear in-memory secret references when the vault locks.

The existing `ArbitraryDataProvider` storage should not remain the long-term plaintext source for `PRIVATE_KEY`
or `MNEMONIC`. Migration must be careful because existing users already have these values in the database.

### Thumbnail cache decision

For the first secure version, use an in-memory thumbnail cache scoped to the unlocked vault session.

Reasons:

- it avoids persistent plaintext thumbnails;
- it avoids introducing a second encrypted disk cache before the media flow is correct;
- it can still prevent repeated decrypt/decode work during one folder browsing session;
- cache invalidation is simple when the vault locks.

Cache keys should include stable file identity and modification data, for example:

- account name or user ID;
- encrypted folder local ID;
- file remote ID or encrypted remote path;
- E2EE counter;
- modification timestamp or ETag where available;
- file length.

If persistent thumbnail caching is later required for performance, add a separate encrypted disk cache in
app-private cache storage, with `.nomedia`, a Keystore-protected cache key and cache deletion on logout/E2EE
reset/session invalidation.

### Image viewer proposal

Reuse the existing `PreviewImageActivity`, `PreviewMediaPagerAdapter` and `PreviewImageFragment` paging model,
but add a source abstraction for encrypted files.

Target flow:

1. user enters encrypted folder after vault unlock;
2. thumbnails are generated by `E2eeThumbnailProvider`;
3. tapping an E2EE image opens the existing image preview activity;
4. the adapter creates an E2EE-aware image fragment/source for encrypted files;
5. plaintext is decoded from a vault-controlled stream or memory buffer;
6. no decrypted JPEG/PNG is written to the normal local save path for preview;
7. closing the viewer releases bitmaps and vault media handles.

Large images should be decoded with bounds/downsampling like the existing `BitmapUtils.retrieveBitmapFromFile()`
path, but from a vault-controlled source rather than a persistent plaintext file path.

### Video player proposal

Reuse Media3 ExoPlayer and `NextcloudExoPlayer` where possible, but inject an E2EE-safe local source for
encrypted media.

Target flow:

1. download full ciphertext to app-private vault cache;
2. authenticate/decrypt locally using the metadata file key, nonce and tag;
3. provide plaintext to ExoPlayer through a non-exported internal `ContentProvider`, `ParcelFileDescriptor`
   pipe or custom Media3 `DataSource`;
4. stop the player and close streams on vault lock;
5. do not create a persistent plaintext video file.

Because the current AES-GCM file format is whole-file authenticated, initial video playback may need to disable
or constrain seeking for large E2EE videos until a robust no-plaintext random-access strategy is implemented.

### Integration points to modify

| File/class | Planned change |
| --- | --- |
| `OCFileListFragment.folderOnItemClick()` | Gate encrypted folder entry through `E2eeVaultSession` and `VaultBiometricManager`. |
| `FileOperationsHelper.isEndToEndEncryptionSetup()` | Keep setup check, but do not treat setup as vault unlock. |
| `SetupEncryptionDialogFragment` | Store/migrate E2EE secrets through `E2eeSecretStore` instead of plaintext arbitrary data. |
| `EncryptionUtilsV2.parseAnyMetadata()` and v1 metadata helpers | Introduce a secret-provider seam so metadata decryption can use vault-unlocked secrets. |
| `DownloadFileOperation` | Keep existing normal download behavior, but add a separate vault preview path that does not write plaintext to `savePath`. |
| `ThumbnailsCacheManager`, `DisplayUtils`, `GalleryImageGenerationJob`, `OCFileListDelegate` | Route encrypted thumbnails to `E2eeThumbnailProvider`; explicitly avoid server preview for E2EE files. |
| `PreviewMediaPagerAdapter` | Replace encrypted `FileDownloadFragment` preview with E2EE-aware image/video preview when vault is unlocked. |
| `PreviewImageFragment` | Add an E2EE source path that decodes without persistent plaintext. |
| `PreviewMediaActivity`, `PreviewMediaFragment`, `NextcloudExoPlayer` | Add an E2EE Media3 source/data source for vault videos. |
| Preview/list activities displaying vault content | Enable strict `FLAG_SECURE` while E2EE vault content is visible. |

## What is immediately feasible

- Add a biometric gate before entering encrypted folders.
- Add a vault session object with centralized timeout and app-background/device-lock invalidation.
- Enable `FLAG_SECURE` while E2EE vault content is visible.
- Explicitly prevent server preview calls for E2EE thumbnails.
- Generate E2EE image thumbnails locally after unlock.
- Use an in-memory thumbnail cache scoped to the vault session.
- Add tests for session state and E2EE folder navigation gating.

## What needs careful staged work

- Migrating existing plaintext `PRIVATE_KEY` and `MNEMONIC` from `ArbitraryDataProvider` to a Keystore-protected
  store without locking out existing users.
- Refactoring metadata utilities so private-key access is explicit and testable.
- Refactoring image preview to decode from vault-owned sources instead of only `storagePath`.
- Implementing video playback without persistent plaintext while respecting Media3 expectations.
- Defining exact cleanup behavior for interrupted downloads, corrupt ciphertext, invalid authentication tags and
  activity backgrounding.

## What is limited by the current E2EE protocol

- True progressive E2EE video streaming with authenticated random access is not supported by the current
  whole-file AES-GCM format.
- HTTP Range over ciphertext cannot be mapped safely to arbitrary plaintext ranges without a chunked encryption
  format and per-chunk authentication metadata.
- The first safe vault implementation must therefore prefer full ciphertext download and local authentication
  before media playback.
