/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2026 Alper Ozturk <alper.ozturk@nextcloud.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.owncloud.android.ui.fragment.share

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.owncloud.android.utils.theme.ViewThemeUtils

enum class UnifiedShareCategory {
    Invited, Anyone
}

enum class UnifiedShareType {
    InternalUser, InternalGroup, InternalLink, ExternalLink, ExternalFederated, ExternalMail;

    fun icon(): String {
        return when (this) {
            InternalUser -> "👤"
            InternalGroup -> "👥"
            InternalLink -> "🔗"
            ExternalLink -> "🌍"
            ExternalFederated -> "☁️"
            ExternalMail -> "📧"
        }
    }
}

data class UnifiedShareDownloadLimit(
    val limit: Int,
    val downloadCount: Int
)

sealed class UnifiedSharePermission {
    // file drop only for folder
    data object FileDrop : UnifiedSharePermission()

    data object CanView : UnifiedSharePermission()
    data object CanEdit : UnifiedSharePermission()

    // create only for folder
    data class Custom(val read: Boolean, val edit: Boolean, val delete: Boolean, val create: Boolean) :
        UnifiedSharePermission()

    fun getText(): String {
        return when(this) {
            FileDrop -> "File drop"
            CanView -> "Can view"
            CanEdit -> "Can edit"
            is Custom -> "Custom permissions"
        }
    }
}

data class UnifiedShares(
    val id: Int,
    val password: String,
    val note: String,
    val limit: UnifiedShareDownloadLimit,
    val expirationDate: Int,
    val permission: UnifiedSharePermission,
    val label: String,
    val sharedTo: String,
    val type: UnifiedShareType,
    val category: UnifiedShareCategory,
)

// TODO: MOVE TO THE ANDROID: COMMON
// TODO: MAKE LAZY COLUMN
// TODO: EXPOSE ACTIONS, IMPLEMENT VIEWMODEL, REPOSITORY TO FETCH ACTUAL SHARE, INJECT NECESSARY PARAMETERS

@Composable
fun UnifiedShareView() {
    var showAddShare by remember { mutableStateOf(false) }

    val mockUnifiedShares = listOf(
        UnifiedShares(
            id = 1,
            password = "",
            note = "Design review – please check latest changes",
            limit = UnifiedShareDownloadLimit(
                limit = 100,
                downloadCount = 12
            ),
            expirationDate = 0,
            permission = UnifiedSharePermission.CanView,
            label = "Alice Johnson",
            sharedTo = "alice@company.com",
            type = UnifiedShareType.InternalUser,
            category = UnifiedShareCategory.Invited
        ),

        UnifiedShares(
            id = 2,
            password = "",
            note = "",
            limit = UnifiedShareDownloadLimit(
                limit = 0,
                downloadCount = 0
            ),
            expirationDate = 0,
            permission = UnifiedSharePermission.CanEdit,
            label = "Marketing Team",
            sharedTo = "marketing",
            type = UnifiedShareType.InternalGroup,
            category = UnifiedShareCategory.Invited
        ),

        UnifiedShares(
            id = 3,
            password = "1234",
            note = "Public link for client review",
            limit = UnifiedShareDownloadLimit(
                limit = 50,
                downloadCount = 5
            ),
            expirationDate = 1710000000,
            permission = UnifiedSharePermission.Custom(
                read = true,
                edit = false,
                delete = false,
                create = false
            ),
            label = "Public Link",
            sharedTo = "https://nextcloud.com/s/abc123",
            type = UnifiedShareType.InternalLink,
            category = UnifiedShareCategory.Anyone
        ),

        UnifiedShares(
            id = 4,
            password = "",
            note = "External partner access",
            limit = UnifiedShareDownloadLimit(
                limit = 20,
                downloadCount = 2
            ),
            expirationDate = 0,
            permission = UnifiedSharePermission.CanView,
            label = "John External",
            sharedTo = "john@external.com",
            type = UnifiedShareType.ExternalMail,
            category = UnifiedShareCategory.Anyone
        ),

        UnifiedShares(
            id = 5,
            password = "",
            note = "Federated sharing with partner instance",
            limit = UnifiedShareDownloadLimit(
                limit = 0,
                downloadCount = 0
            ),
            expirationDate = 0,
            permission = UnifiedSharePermission.FileDrop,
            label = "Partner Cloud",
            sharedTo = "partner@nextcloud.org",
            type = UnifiedShareType.ExternalFederated,
            category = UnifiedShareCategory.Anyone
        )
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        mockUnifiedShares.forEachIndexed { index, share ->
            val type = when (index) {
                0 -> {
                    UnifiedSharesListItemType.Top
                }

                mockUnifiedShares.lastIndex -> {
                    UnifiedSharesListItemType.Bottom
                }

                else -> {
                    UnifiedSharesListItemType.Mid
                }
            }

            UnifiedSharesListItem(share, type)
        }

        FloatingActionButton(
            onClick = { showAddShare = true },
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 16.dp)
        ) {
            Icon(Icons.Default.Person, contentDescription = "Add")
        }

        if (showAddShare) {
            AddShareBottomSheet("Abc.txt",onDismiss = { showAddShare = false })
        }
    }
}

// TODO: Use like inner tags whenever user add a new people to the search and it should look like User 1, Group 1 etc.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShareBottomSheet(filename: String, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()

    var category by remember { mutableStateOf(UnifiedShareCategory.Invited) }
    var permission by remember { mutableStateOf<UnifiedSharePermission>(UnifiedSharePermission.CanView) }
    var searchQuery by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    // Toggle states for collapse/expand
    var showInvitedSettings by remember { mutableStateOf(false) }
    var showAnyoneSettings by remember { mutableStateOf(false) }

    var viewFiles by remember { mutableStateOf(false) }
    var editFiles by remember { mutableStateOf(false) }
    var createFiles by remember { mutableStateOf(false) }
    var deleteFiles by remember { mutableStateOf(false) }

    val availablePermissions = remember {
        listOf(
            UnifiedSharePermission.CanView,
            UnifiedSharePermission.CanEdit,
            UnifiedSharePermission.FileDrop,
            UnifiedSharePermission.Custom(false, false, false, false)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ShareBottomSheetHeader(filename)

            ShareCategoryButtonGroup(
                selectedCategory = category,
                onCategoryChange = { category = it }
            )

            if (category == UnifiedShareCategory.Invited) {
                InvitedShareContent(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    permission = permission,
                    availablePermissions = availablePermissions,
                    onPermissionChange = { permission = it },
                )

                CollapsibleSettingsSection(
                    isExpanded = showInvitedSettings,
                    onToggle = { showInvitedSettings = !showInvitedSettings }
                ) {
                    InvitedInlineSettings()
                }
            } else {
                AnyoneShareContent(
                    permission = permission,
                    availablePermissions = availablePermissions,
                    onPermissionChange = { permission = it },
                )

                if (permission is UnifiedSharePermission.Custom) {
                    SettingsSwitchRow("View files", viewFiles) { viewFiles = it }
                    SettingsSwitchRow("Edit files", editFiles) { editFiles = it }
                    SettingsSwitchRow("Create files", createFiles) { createFiles = it }
                    SettingsSwitchRow("Delete files", deleteFiles) { deleteFiles = it }
                }

                CollapsibleSettingsSection(
                    isExpanded = showAnyoneSettings,
                    onToggle = { showAnyoneSettings = !showAnyoneSettings }
                ) {
                    AnyoneInlineSettings()
                }
            }

            NoteToRecipients(note = note, onNoteChange = { note = it })


            ShareActionButtons(
                category = category,
                isSendEnabled = searchQuery.isNotBlank(),
                onCopyClick = { /* TODO */ },
                onSendClick = { /* TODO */ }
            )
        }
    }
}

@Composable
private fun CollapsibleSettingsSection(
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column {
                content()
            }
        }
    }
}

@Composable
private fun ShareBottomSheetHeader(filename: String) {
    Text(
        text = "Share $filename",
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareCategoryButtonGroup(
    selectedCategory: UnifiedShareCategory,
    onCategoryChange: (UnifiedShareCategory) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth()
    ) {
        UnifiedShareCategory.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selectedCategory == option,
                onClick = { onCategoryChange(option) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = UnifiedShareCategory.entries.size
                )
            ) {
                Text(option.name)
            }
        }
    }
}

@Composable
private fun InvitedShareContent(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    permission: UnifiedSharePermission,
    availablePermissions: List<UnifiedSharePermission>,
    onPermissionChange: (UnifiedSharePermission) -> Unit,

) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Add people") },
            placeholder = { Text("Name, team, email or federated ID") },
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )

        PermissionDropdown(
            label = "Participants",
            selectedPermission = permission,
            availablePermissions = availablePermissions,
            onPermissionChange = onPermissionChange
        )
    }
}

@Composable
private fun NoteToRecipients(
    note: String,
    onNoteChange: (String) -> Unit
) {
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("Note to recipients") },
        shape = RoundedCornerShape(8.dp)
    )
}

@Composable
private fun AnyoneShareContent(
    permission: UnifiedSharePermission,
    availablePermissions: List<UnifiedSharePermission>,
    onPermissionChange: (UnifiedSharePermission) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PermissionDropdown(
            label = "Anyone with the link",
            selectedPermission = permission,
            availablePermissions = availablePermissions,
            onPermissionChange = onPermissionChange
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionDropdown(
    label: String,
    selectedPermission: UnifiedSharePermission,
    availablePermissions: List<UnifiedSharePermission>,
    onPermissionChange: (UnifiedSharePermission) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedPermission.getText(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availablePermissions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.getText()) },
                    onClick = {
                        onPermissionChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InvitedInlineSettings() {
    var shareWithOthers by remember { mutableStateOf(false) }
    var editFile by remember { mutableStateOf(false) }
    var hasExpiration by remember { mutableStateOf(false) }
    var hideDownload by remember { mutableStateOf(false) }

    Column {
        SettingsSwitchRow("Share with others", shareWithOthers) { shareWithOthers = it }
        SettingsSwitchRow("Edit file", editFile) { editFile = it }
        SettingsSwitchRow("Expiration date", hasExpiration) { hasExpiration = it }
        SettingsSwitchRow("Hide download and sync options", hideDownload) { hideDownload = it }
    }
}

@Composable
private fun AnyoneInlineSettings() {
    var hasPassword by remember { mutableStateOf(false) }
    var hasExpiration by remember { mutableStateOf(false) }
    var limitDownloads by remember { mutableStateOf(false) }

    var hideDownloads by remember { mutableStateOf(false) }
    var videoVerification by remember { mutableStateOf(false) }
    var showFilesInGridView by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            label = { Text("Label") },
            placeholder = { Text("Optional name for this link") },
            singleLine = true
        )

        SettingsSwitchRow("Expiration date", hasExpiration) { hasExpiration = it }
        SettingsSwitchRow("Password", hasPassword) { hasPassword = it }
        SettingsSwitchRow("Limit downloads", limitDownloads) { limitDownloads = it }

        SettingsSwitchRow("Hide downloads", hideDownloads) { hideDownloads = it }
        SettingsSwitchRow("Video verification", videoVerification) { videoVerification = it }
        SettingsSwitchRow("Show files in grid view", showFilesInGridView) { showFilesInGridView = it }

    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ShareActionButtons(
    category: UnifiedShareCategory,
    isSendEnabled: Boolean,
    onCopyClick: () -> Unit,
    onSendClick: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(top = 16.dp)) {
        if (category == UnifiedShareCategory.Invited) {
            FilledTonalButton(
                onClick = onCopyClick,
                modifier = Modifier.weight(1f)
            ) {
                Text("Copy link")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onSendClick,
                modifier = Modifier.weight(1f),
                enabled = isSendEnabled // Disabled if search query is empty
            ) {
                Text("Send")
            }
        } else {
            // For "Anyone" (Public link), usually just one big action to create/copy
            Button(
                onClick = onCopyClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Create public link")
            }
        }
    }
}

enum class UnifiedSharesListItemType {
    Top, Mid, Bottom;

    @Composable
    fun getShape(): RoundedCornerShape {
        return when (this) {
            Top -> RoundedCornerShape(12.dp, 12.dp, 4.dp, 4.dp)
            Mid -> RoundedCornerShape(4.dp, 4.dp, 4.dp, 4.dp)
            Bottom -> RoundedCornerShape(4.dp, 4.dp, 12.dp, 12.dp)
        }
    }
}

// NOTE: To just create a public link anyone tab + just send DOES SAME THING
@Composable
private fun UnifiedSharesListItem(share: UnifiedShares, type: UnifiedSharesListItemType) {
    var showContextMenu by remember { mutableStateOf(false) }
    var showDetailSheet by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(type.getShape())
            .combinedClickable(
                onClick = { showDetailSheet = true },
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    showContextMenu = true
                },
            )
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(text = share.type.icon())
            }
        },
        headlineContent = {
            Text(
                text = share.label,
                style = MaterialTheme.typography.titleSmall
            )
        },
        supportingContent = {
            Text(
                text = share.permission.getText(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showContextMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }

                DropdownMenu(
                    expanded = showContextMenu,
                    onDismissRequest = { showContextMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showContextMenu = false
                            showDetailSheet = true
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Send email") },
                        onClick = { showContextMenu = false }
                    )

                    HorizontalDivider()

                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { showContextMenu = false }
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent
        )
    )

    // TODO: USE EXISTING SHARE DETAILS
    if (showDetailSheet) {
        AddShareBottomSheet(
            filename = share.label,
            onDismiss = { showDetailSheet = false }
        )
    }
}

fun ComposeView.setupUnifiedShare(viewThemeUtils: ViewThemeUtils, context: Context) {
    setContent {
        MaterialTheme(
            colorScheme = viewThemeUtils.getColorScheme(context),
            content = {
                UnifiedShareView()
            }
        )
    }
}
