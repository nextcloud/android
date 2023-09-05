/*
 * Nextcloud Android client application
 *
 *  @author Álvaro Brey
 *  Copyright (C) 2022 Álvaro Brey
 *  Copyright (C) 2022 Nextcloud GmbH
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.nextcloud.client.database.migrations;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import com.nextcloud.client.core.Clock;
import com.owncloud.android.datamodel.SyncedFolder;
import com.owncloud.android.db.ProviderMeta;
import com.owncloud.android.files.services.NameCollisionPolicy;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.providers.FileContentProvider;

import java.util.Locale;

import androidx.sqlite.db.SupportSQLiteDatabase;

public class LegacyMigrationHelper {

    private static final String TAG = LegacyMigrationHelper.class.getSimpleName();

    public static final int ARBITRARY_DATA_TABLE_INTRODUCTION_VERSION = 20;


    private static final String ALTER_TABLE = "ALTER TABLE ";
    private static final String ADD_COLUMN = " ADD COLUMN ";
    private static final String INTEGER = " INTEGER, ";
    private static final String TEXT = " TEXT, ";

    private static final String UPGRADE_VERSION_MSG = "OUT of the ADD in onUpgrade; oldVersion == %d, newVersion == %d";

    private final Clock clock;

    public LegacyMigrationHelper(Clock clock) {
        this.clock = clock;
    }

    public void onUpgrade(SupportSQLiteDatabase db, int oldVersion, int newVersion) {
        Log_OC.i(TAG, "Entering in onUpgrade");
        boolean upgraded = false;

        if (oldVersion < 25 && newVersion >= 25) {
            Log_OC.i(TAG, "Entering in the #25 Adding encryption flag to file");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_IS_ENCRYPTED + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_ENCRYPTED_NAME + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION + " INTEGER ");
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 26 && newVersion >= 26) {
            Log_OC.i(TAG, "Entering in the #26 Adding text and element color to capabilities");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR + " TEXT ");

                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 27 && newVersion >= 27) {
            Log_OC.i(TAG, "Entering in the #27 Adding token to ocUpload");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN + " TEXT ");
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 28 && newVersion >= 28) {
            Log_OC.i(TAG, "Entering in the #28 Adding CRC32 column to filesystem table");
            db.beginTransaction();
            try {
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32 + " TEXT ");
                }
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 29 && newVersion >= 29) {
            Log_OC.i(TAG, "Entering in the #29 Adding background default/plain to capabilities");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_DEFAULT + " INTEGER ");

                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_BACKGROUND_PLAIN + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 30 && newVersion >= 30) {
            Log_OC.i(TAG, "Entering in the #30 Re-add 25, 26 if needed");
            db.beginTransaction();
            try {
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.FILE_IS_ENCRYPTED)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_IS_ENCRYPTED + " INTEGER ");
                }
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.FILE_ENCRYPTED_NAME)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_ENCRYPTED_NAME + " TEXT ");
                }
                if (oldVersion > ARBITRARY_DATA_TABLE_INTRODUCTION_VERSION) {
                    if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderMeta.ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION)) {
                        db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_END_TO_END_ENCRYPTION + " INTEGER ");
                    }
                    if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR)) {
                        db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_TEXT_COLOR + " TEXT ");
                    }
                    if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME,
                                             ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR)) {
                        db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                                       ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_ELEMENT_COLOR + " TEXT ");
                    }
                    if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME,
                                             ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32)) {
                        try {
                            db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILESYSTEM_TABLE_NAME +
                                           ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILESYSTEM_CRC32 + " TEXT ");
                        } catch (SQLiteException e) {
                            Log_OC.d(TAG, "Known problem on adding same column twice when upgrading from 24->30");
                        }
                    }
                }

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 31 && newVersion >= 31) {
            Log_OC.i(TAG, "Entering in the #31 add mount type");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_MOUNT_TYPE + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 32 && newVersion >= 32) {
            Log_OC.i(TAG, "Entering in the #32 add ocshares.is_password_protected");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_IS_PASSWORD_PROTECTED + " INTEGER "); // boolean

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 33 && newVersion >= 33) {
            Log_OC.i(TAG, "Entering in the #3 Adding activity to capability");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_ACTIVITY + " INTEGER ");
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 34 && newVersion >= 34) {
            Log_OC.i(TAG, "Entering in the #34 add redirect to external links");
            db.beginTransaction();
            try {
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.EXTERNAL_LINKS_REDIRECT + " INTEGER "); // boolean
                }
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 35 && newVersion >= 35) {
            Log_OC.i(TAG, "Entering in the #35 add note to share table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_NOTE + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 36 && newVersion >= 36) {
            Log_OC.i(TAG, "Entering in the #36 add has-preview to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_HAS_PREVIEW + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 37 && newVersion >= 37) {
            Log_OC.i(TAG, "Entering in the #37 add hide-download to share table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_HIDE_DOWNLOAD + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 38 && newVersion >= 38) {
            Log_OC.i(TAG, "Entering in the #38 add richdocuments");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT + " INTEGER "); // boolean
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_MIMETYPE_LIST + " TEXT "); // string

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 39 && newVersion >= 39) {
            Log_OC.i(TAG, "Entering in the #39 add richdocuments direct editing");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_DIRECT_EDITING + " INTEGER "); // bool

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 40 && newVersion >= 40) {
            Log_OC.i(TAG, "Entering in the #40 add unreadCommentsCount to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_UNREAD_COMMENTS_COUNT + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 41 && newVersion >= 41) {
            Log_OC.i(TAG, "Entering in the #41 add eTagOnServer");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_ETAG_ON_SERVER + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 42 && newVersion >= 42) {
            Log_OC.i(TAG, "Entering in the #42 add richDocuments templates");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_TEMPLATES + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 43 && newVersion >= 43) {
            Log_OC.i(TAG, "Entering in the #43 add ownerId and owner display name to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_OWNER_ID + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_OWNER_DISPLAY_NAME + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 44 && newVersion >= 44) {
            Log_OC.i(TAG, "Entering in the #44 add note to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_NOTE + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 45 && newVersion >= 45) {
            Log_OC.i(TAG, "Entering in the #45 add sharees to file table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_SHAREES + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 46 && newVersion >= 46) {
            Log_OC.i(TAG, "Entering in the #46 add optional mimetypes to capabilities table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_OPTIONAL_MIMETYPE_LIST
                               + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 47 && newVersion >= 47) {
            Log_OC.i(TAG, "Entering in the #47 add askForPassword to capability table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SHARING_PUBLIC_ASK_FOR_OPTIONAL_PASSWORD +
                               " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 48 && newVersion >= 48) {
            Log_OC.i(TAG, "Entering in the #48 add product name to capabilities table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_RICHDOCUMENT_PRODUCT_NAME + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 49 && newVersion >= 49) {
            Log_OC.i(TAG, "Entering in the #49 add extended support to capabilities table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_EXTENDED_SUPPORT + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 50 && newVersion >= 50) {
            Log_OC.i(TAG, "Entering in the #50 add persistent enable date to synced_folders table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS + " INTEGER ");

                db.execSQL("UPDATE " + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME + " SET " +
                               ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_ENABLED_TIMESTAMP_MS + " = CASE " +
                               " WHEN enabled = 0 THEN " + SyncedFolder.EMPTY_ENABLED_TIMESTAMP_MS + " " +
                               " ELSE " + clock.getCurrentTime() +
                               " END ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 51 && newVersion >= 51) {
            Log_OC.i(TAG, "Entering in the #51 add show/hide to folderSync table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_HIDDEN + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 52 && newVersion >= 52) {
            Log_OC.i(TAG, "Entering in the #52 add etag for directEditing to capability");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_DIRECT_EDITING_ETAG + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 53 && newVersion >= 53) {
            Log_OC.i(TAG, "Entering in the #53 add rich workspace to file table");
            db.beginTransaction();
            try {
                if (!checkIfColumnExists(db, ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME,
                                         ProviderMeta.ProviderTableMeta.FILE_RICH_WORKSPACE)) {
                    db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                                   ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_RICH_WORKSPACE + " TEXT ");
                }
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 54 && newVersion >= 54) {
            Log_OC.i(TAG, "Entering in the #54 add synced.existing," +
                " rename uploads.force_overwrite to uploads.name_collision_policy");
            db.beginTransaction();
            try {
                // Add synced.existing
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_EXISTING + " INTEGER "); // boolean


                // Rename uploads.force_overwrite to uploads.name_collision_policy
                String tmpTableName = ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME + "_old";
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME + " RENAME TO " + tmpTableName);
                createUploadsTable(db);
                db.execSQL("INSERT INTO " + ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME + " (" +
                               ProviderMeta.ProviderTableMeta._ID + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_PATH + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_REMOTE_PATH + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_ACCOUNT_NAME + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_FILE_SIZE + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_STATUS + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_TIME + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LAST_RESULT + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_WIFI_ONLY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_CREATED_BY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN +
                               ") " +
                               " SELECT " +
                               ProviderMeta.ProviderTableMeta._ID + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_PATH + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_REMOTE_PATH + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_ACCOUNT_NAME + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_FILE_SIZE + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_STATUS + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_TIME + ", " +
                               "force_overwrite" + ", " + // See FileUploader.NameCollisionPolicy
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_LAST_RESULT + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_IS_WIFI_ONLY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_CREATED_BY + ", " +
                               ProviderMeta.ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN +
                               " FROM " + tmpTableName);
                db.execSQL("DROP TABLE " + tmpTableName);

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 55 && newVersion >= 55) {
            Log_OC.i(TAG, "Entering in the #55 add synced.name_collision_policy.");
            db.beginTransaction();
            try {
                // Add synced.name_collision_policy
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY + " INTEGER "); // integer

                // make sure all existing folders set to FileUploader.NameCollisionPolicy.ASK_USER.
                db.execSQL("UPDATE " + ProviderMeta.ProviderTableMeta.SYNCED_FOLDERS_TABLE_NAME + " SET " +
                               ProviderMeta.ProviderTableMeta.SYNCED_FOLDER_NAME_COLLISION_POLICY + " = " +
                               NameCollisionPolicy.ASK_USER.serialize());
                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 56 && newVersion >= 56) {
            Log_OC.i(TAG, "Entering in the #56 add decrypted remote path");
            db.beginTransaction();
            try {
                // Add synced.name_collision_policy
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_PATH_DECRYPTED + " TEXT "); // strin

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 57 && newVersion >= 57) {
            Log_OC.i(TAG, "Entering in the #57 add etag for capabilities");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_ETAG + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 58 && newVersion >= 58) {
            Log_OC.i(TAG, "Entering in the #58 add public link to share table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_LINK + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 59 && newVersion >= 59) {
            Log_OC.i(TAG, "Entering in the #59 add public label to share table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.OCSHARES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.OCSHARES_SHARE_LABEL + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 60 && newVersion >= 60) {
            Log_OC.i(TAG, "Entering in the #60 add user status to capability table");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_USER_STATUS + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_USER_STATUS_SUPPORTS_EMOJI + " INTEGER ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 61 && newVersion >= 61) {
            Log_OC.i(TAG, "Entering in the #61 reset eTag to force capability refresh");
            db.beginTransaction();
            try {
                db.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 62 && newVersion >= 62) {
            Log_OC.i(TAG, "Entering in the #62 add logo to capability");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_SERVER_LOGO + " TEXT ");

                // force refresh
                db.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (oldVersion < 63 && newVersion >= 63) {
            Log_OC.i(TAG, "Adding file locking columns");
            db.beginTransaction();
            try {
                // locking capabilities
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.CAPABILITIES_TABLE_NAME + ADD_COLUMN + ProviderMeta.ProviderTableMeta.CAPABILITIES_FILES_LOCKING_VERSION + " TEXT ");
                // force refresh
                db.execSQL("UPDATE capabilities SET etag = '' WHERE 1=1");
                // locking properties
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCKED + " INTEGER "); // boolean
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_TYPE + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_OWNER + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_OWNER_DISPLAY_NAME + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_OWNER_EDITOR + " TEXT ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_TIMESTAMP + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_TIMEOUT + " INTEGER ");
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_LOCK_TOKEN + " TEXT ");
                db.execSQL("UPDATE " + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME + " SET " + ProviderMeta.ProviderTableMeta.FILE_ETAG + " = '' WHERE 1=1");

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }

        if (oldVersion < 64 && newVersion >= 64) {
            Log_OC.i(TAG, "Entering in the #64 add metadata size to files");
            db.beginTransaction();
            try {
                db.execSQL(ALTER_TABLE + ProviderMeta.ProviderTableMeta.FILE_TABLE_NAME +
                               ADD_COLUMN + ProviderMeta.ProviderTableMeta.FILE_METADATA_SIZE + " TEXT ");

                upgraded = true;
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (!upgraded) {
            Log_OC.i(TAG, String.format(Locale.ENGLISH, UPGRADE_VERSION_MSG, oldVersion, newVersion));
        }
    }

    private void createUploadsTable(SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ProviderMeta.ProviderTableMeta.UPLOADS_TABLE_NAME + "("
                       + ProviderMeta.ProviderTableMeta._ID + " INTEGER PRIMARY KEY, "
                       + ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_PATH + TEXT
                       + ProviderMeta.ProviderTableMeta.UPLOADS_REMOTE_PATH + TEXT
                       + ProviderMeta.ProviderTableMeta.UPLOADS_ACCOUNT_NAME + TEXT
                       + ProviderMeta.ProviderTableMeta.UPLOADS_FILE_SIZE + " LONG, "
                       + ProviderMeta.ProviderTableMeta.UPLOADS_STATUS + INTEGER               // UploadStatus
                       + ProviderMeta.ProviderTableMeta.UPLOADS_LOCAL_BEHAVIOUR + INTEGER      // Upload LocalBehaviour
                       + ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_TIME + INTEGER
                       + ProviderMeta.ProviderTableMeta.UPLOADS_NAME_COLLISION_POLICY + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.UPLOADS_IS_CREATE_REMOTE_FOLDER + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.UPLOADS_UPLOAD_END_TIMESTAMP + INTEGER
                       + ProviderMeta.ProviderTableMeta.UPLOADS_LAST_RESULT + INTEGER     // Upload LastResult
                       + ProviderMeta.ProviderTableMeta.UPLOADS_IS_WHILE_CHARGING_ONLY + INTEGER  // boolean
                       + ProviderMeta.ProviderTableMeta.UPLOADS_IS_WIFI_ONLY + INTEGER // boolean
                       + ProviderMeta.ProviderTableMeta.UPLOADS_CREATED_BY + INTEGER    // Upload createdBy
                       + ProviderMeta.ProviderTableMeta.UPLOADS_FOLDER_UNLOCK_TOKEN + " TEXT );");

    /* before:
    // PRIMARY KEY should always imply NOT NULL. Unfortunately, due to a
    // bug in some early versions, this is not the case in SQLite.
    //db.execSQL("CREATE TABLE " + TABLE_UPLOAD + " (" + " path TEXT PRIMARY KEY NOT NULL UNIQUE,"
    //        + " uploadStatus INTEGER NOT NULL, uploadObject TEXT NOT NULL);");
    // uploadStatus is used to easy filtering, it has precedence over
    // uploadObject.getUploadStatus()
    */
    }

    private boolean checkIfColumnExists(SupportSQLiteDatabase database, String table, String column) {
        Cursor cursor = database.query("SELECT * FROM " + table + " LIMIT 0");
        boolean exists = cursor.getColumnIndex(column) != -1;
        cursor.close();

        return exists;
    }


}
