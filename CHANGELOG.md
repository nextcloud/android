## 3.7.0 (July, 09, 2019)

- Collabora enhancements
- Chromebook support
- delete push notifications if read on other device (NC 18 and newer)
- open file from notification
- open file from Talk app
- minimum supported server: NC12
- end of life warning: NC13 and older
- lots of bugfixes and refinements under the hood to provide an even more stable app

For a full list, please see https://github.com/nextcloud/android/milestone/32

## 3.6.2 (May, 23, 2019)
- fix bug when creating preview
- fix crash on opening app
- fix account switch
- fix jumping to top on sync

For a full list, please see https://github.com/nextcloud/android/milestone/34

## 3.6.1 (May, 12, 2019)
- show reshares correctly
- allow open files from Talk
- collabora: hide loading delay warning if document is loaded
- correctly show idn string in drawer
- show outdated warning on NC13
- enhance pass protection system
- bugfixes

For a full list, please see https://github.com/nextcloud/android/milestone/33

## 3.6.0 (April, 09, 2019)
- remove "expert mode"
- show warning if server is unavailable
- delete notification on server
- actions in notifications
- add storage path chooser for local file picker
- show shared user
- show notes on sharing
- min supported server is NC12
- warn on outdated server: <=NC14

For a full list, please see https://github.com/nextcloud/android/milestone/30

## 3.5.1 (March, 18, 2019)
- fixed SSO dialog
- abort sync on no connection
- fix chunked upload
- fix federated share
- fix button disabled state in folder sync preferences
- add storage picker to upload local chooser
- updated translations

For a full list, please see https://github.com/nextcloud/android/milestone/31

## 3.5.0 (February, 13, 2019)
- Chunked upload: 1MB on mobile data, 10MB on Wi-Fi
- Switch to Material Design
- Option to not show notifications for new media folders
- Add support for QR codes & deep links
- Direct camera upload
- Fully working Document provider
- Detail view: Show complete date upon click
- Show correct share error message
- Use default/device font
- Sync all downloaded
- Add battery optimization warning

For a full list, please see https://github.com/nextcloud/android/milestone/28

## 3.4.2 (January, 21, 2019)
- fix sharing to group
- show correct share error messages
- fix bug when searching for user/group if Talk is disabled

For a full list, please see https://github.com/nextcloud/android/milestone/29

## 3.4.1 (December, 23, 2018)
- fix wrong detection of direct editing capability for RichDocuments

## 3.4.0 (December, 17, 2018)
- hide download when creating share links
- direct editing files with Collabora (Collabora Server >=4.0)
- sort deleted files by deletion date by default
- set/edit notes on shares
- search inside of text files
- actions on notifications
- remember last path on upload
- share file to Talk room
- show local size in "on device" view
- SSO: add request header for deck app
- bug fixes

For a full list, please see https://github.com/nextcloud/android/milestone/25

## 3.3.2 (November, 02, 2018)
- fix fingerprint not working on certain devices

For a full list, please see https://github.com/nextcloud/android/milestone/27

## 3.3.1 (October, 29, 2018)
Bugfix release
- fix crash on shared folder/file via Talk
- fix crash on Notification activity
- fixed setup DAVdroid via settings
- hide edit option on shares, if not allowed

For a full list, please see https://github.com/nextcloud/android/milestone/26

## 3.3.0 (September, 19, 2018)
- Support for Trashbin (Nc14+)
- Media streaming (Nc14+)
- New media detection for AutoUpload
- Improved TalkBack screenreader support
- Show outdated server warning for server <NC12
- Add support for device credentials
- Show offline / maintenance info
- Improved activities
- Improved file detail / sharing with comments
- Improved Share link creation via bottom sheet
- Improved Notification, supporting actions
- Minor UI/UX improvements
- Many bug fixes

## 3.2.4 (September, 04, 2018)
- Fix push notification on gplay release

## 3.2.3 (August, 21, 2018)
- Fix crash on Android 4.x

## 3.2.2 (August, 20, 2018)
- New simple signup screen

## 3.2.1 (June, 11, 2018)
- Enhanced file detail/sharing screen for mail-shares
- Fix local sorting and file selection
- Fix local filtering
- Fix back navigation on privacy screen
- Fix bug on searching
- Fix crash on sorting
- Fix wrong menu highlighting
- various bug fixes

## 3.2.0 (May, 13, 2018)
- Revamped details screen & sharing
- minor UI/UX improvements
- many bug fixes
 
## 3.1.0 (April, 22, 2018)
- enhance support for 8.x
- speed improvements
- minor UI/UX improvements
- many bug fixes

## 3.0.3 (March, 05, 2018)
- Fix creating folders in auto upload

## 3.0.2 (February, 27, 2018)
- Fix crash on old android versions
- Fix E2E
- Fix crash on old server

## 3.0.1 (February, 14, 2018)
- Bugfix E2E
- Fix SSL via PlayStore updater
- Fix push notification
- New android lib, fixing wrong user agent

## 3.0.0 (February, 08, 2018)
- End to end encryption
- Screen adapted images instead of downloading
- direct access to operations for single files
- Android 8 support 
- folder based sort order
- right to left language support
- detect walled garden
- load more activities when reaching end of stream
- quicker access to share 
- automatically update avatars
- auto upload improvements
- fix push notifications
- UI enhancements
- bug fixes

## 2.0.0 (October, 17, 2017)
- Account-wide search (Nc 12+)
- Auto upload available on Android 4+
- Separation between Image & Video for Auto upload
- Ability to define custom folders for Auto upload
- Simple contacts backup & restore
- Server-side Theming support
- Shared files view
- Notifications view and Push notifications (on Google Play-powered devices)
- Favorites, Photos and Activities
- Fingerprint locking
- SVG preview
- Set edit permissions in federated shares of folders (Nc 10+)
- New sorting dialog
- User information view
- Custom external links support
- Detect server maintainance mode
- Nicer error views for images & video preview
- Included privacy policy
- Preserve modification time of uploaded files
- Various bug fixes & improvements

## 1.4.3 (May 22, 2017)
- Hotfix: ignore oauth header for now and use basic auth to allow new logins for Nc12

## 1.4.2 (March 14, 2017)
- Auto Upload for newly taken photos/images (Android 6+)
- Auto Upload improvements and fixes
- Filtering improvements
- Fix for Android permissions (removed read phone state permission)
- Fix re-upload of files
- Avoid toggling favourite for all selected files
- Link to providers list in the setup screen
- Further bug fixes and improvements

## 1.4.1 (January 27, 2017)
- Share URLs to Nextcloud
- Improve performance of Auto Upload view
- Fix for removing files
- Proper email sharee handling
- Navigation drawer: Fix lag on older devices
- Android 7: Pending jobs in upload view
- Android 7: Auto upload: ignore ".tmp" files and folders
- Bug fixes and design improvements

## 1.4.0 (December 8, 2016)
- External SD card support
- Auto Upload (Android 7+)
- What's new start screen
- Show/hide hidden folders & files
- Upload view: switch between grid/list view
- Descend into folder after creating it in uploader
- Provisioning links to launch and prefill app with login data
- Add open URL file feature
- Sort favorite files first in list
- Account switcher optimizations
- Bugfixes and design improvements
- Update library to 1.0.9

## 1.3.1 (September 20, 2016)
- Move action hard to discover - difference between single/multi selection
- Show move/copy context in toolbar title
- Share file with dictadroid to Nextcloud
- Don't show upload failure notification for already deleted files

## 1.3.0 (September 17, 2016)
- Files drop permission in share viewe
- Display quota if configured/available in navigation drawer
- Resume chunked uploads instead of complete restarts
- Filter remote and local file lists
- Simple integration with DAVdroid for calendar and contacts sync
- Mix folders and files on sort by date
- Upload when charging option
- Revamp upload options Move/Copy/Just-Upload
- Text can be selected and shared to Nextcloud
- Respect metered wifi and block instant uploads
- Proper handling of .djvu files
- Bugfixes and design improvements

## 1.2.0 (July 29, 2016)
- Multi select capabilities
- Confirmation dialog on account removal
- Offline available filter in main menu
- Sorting capability when choosing files to be uploaded
- Video thumbnails if activated on server
- Thumbnails during upload
- Fixed user agent for file firewalling
- Showing server side folder sizes
- Text in test preview can now be selected
- Search suggestions fixed for user/group search during sharing
- Minor bugfixes

## 1.1.0 (July 6, 2016)
- New main menu to switch accounts easily
- Ability to open Nextcloud hosted files (read-only) from other apps that support the standard file chooser (requires Android 4.4 / KitKat or higher)
- "Select all files" for upload within a folder
- Optional feature to auto-create monthly folders for your instant uploads
- Revamped login screen
- Minor bugfixes

## 1.0.1 (June 20, 2016)
- Fix thumbnail preview support for new files
- Add support for animated GIFs
- Optimized uploader layout

## 1.0.0 (June 12, 2016)
- Initial release of the Nextcloud Android app
