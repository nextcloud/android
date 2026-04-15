/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.nextcloud.client.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Suppress("MagicNumber", "LongMethod")
val MIGRATION_99_100 = object : Migration(99, 100) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DROP TABLE IF EXISTS capabilities")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS capabilities (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                account TEXT,
                version_mayor INTEGER,
                version_minor INTEGER,
                version_micro INTEGER,
                version_string TEXT,
                version_edition TEXT,
                extended_support INTEGER,
                core_pollinterval INTEGER,
                sharing_api_enabled INTEGER,
                sharing_public_enabled INTEGER,
                sharing_public_password_enforced INTEGER,
                sharing_public_ask_for_optional_password INTEGER,
                sharing_public_expire_date_enabled INTEGER,
                sharing_public_expire_date_days INTEGER,
                sharing_public_expire_date_enforced INTEGER,
                sharing_public_send_mail INTEGER,
                sharing_public_upload INTEGER,
                sharing_user_send_mail INTEGER,
                sharing_resharing INTEGER,
                sharing_federation_outgoing INTEGER,
                sharing_federation_incoming INTEGER,
                files_bigfilechunking INTEGER,
                files_undelete INTEGER,
                files_versioning INTEGER,
                files_locking_version TEXT,
                external_links INTEGER,
                server_name TEXT,
                server_color TEXT,
                server_text_color TEXT,
                server_element_color TEXT,
                background_url TEXT,
                server_slogan TEXT,
                server_logo TEXT,
                background_default INTEGER,
                background_plain INTEGER,
                end_to_end_encryption INTEGER,
                end_to_end_encryption_keys_exist INTEGER,
                end_to_end_encryption_api_version TEXT,
                activity INTEGER,
                richdocument INTEGER,
                recommendation INTEGER,
                richdocument_mimetype_list TEXT,
                richdocument_optional_mimetype_list TEXT,
                richdocument_direct_editing INTEGER,
                richdocument_direct_templates INTEGER,
                richdocument_product_name TEXT,
                direct_editing_etag TEXT,
                etag TEXT,
                user_status INTEGER,
                user_status_supports_emoji INTEGER,
                user_status_supports_busy INTEGER,
                assistant INTEGER,
                groupfolders INTEGER,
                drop_account INTEGER,
                security_guard INTEGER,
                forbidden_filename_characters TEXT,
                forbidden_filenames TEXT,
                forbidden_filename_extensions TEXT,
                forbidden_filename_basenames TEXT,
                windows_compatible_filenames INTEGER,
                files_download_limit INTEGER,
                files_download_limit_default INTEGER,
                notes_folder_path TEXT,
                default_permissions INTEGER,
                has_valid_subscription INTEGER,
                client_integration_json TEXT
            )
            """.trimIndent()
        )
    }
}
