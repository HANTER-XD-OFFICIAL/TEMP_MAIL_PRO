package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.SavedAccountEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedAccountsSheet(
    accounts: List<SavedAccountEntity>,
    activeAccountAddress: String?,
    onDismissRequest: () -> Unit,
    onSwitchAccount: (SavedAccountEntity) -> Unit,
    onDeleteAccount: (SavedAccountEntity) -> Unit,
    onUpdateLabel: (address: String, label: String) -> Unit,
    onOpenCreateDialog: () -> Unit,
    onOpenLoginDialog: () -> Unit,
    onCopyAddress: (String) -> Unit,
    onCopyPassword: (String) -> Unit,
    onExportAll: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var editingAccountForTag by remember { mutableStateOf<SavedAccountEntity?>(null) }
    var tagInput by remember { mutableStateOf("") }

    val filteredAccounts = remember(accounts, searchQuery) {
        if (searchQuery.isBlank()) accounts else {
            accounts.filter {
                it.address.contains(searchQuery, ignoreCase = true) ||
                        it.label.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("saved_accounts_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Lifetime Accounts Vault",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${accounts.size} saved permanent inboxes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row {
                    // Export backup button
                    IconButton(
                        onClick = {
                            val backupText = accounts.joinToString("\n\n") {
                                "Email: ${it.address}\nPassword: ${it.password}\nTag: ${it.label}"
                            }
                            onExportAll(backupText)
                        },
                        modifier = Modifier.testTag("export_vault_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Backup Vault")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Action Buttons (New & Login)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        onDismissRequest()
                        onOpenCreateDialog()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("sheet_new_account_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Email", fontSize = 13.sp)
                }

                FilledTonalButton(
                    onClick = {
                        onDismissRequest()
                        onOpenLoginDialog()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("sheet_login_account_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Login / Bind", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by address or tag...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_vault_input"),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Accounts List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAccounts, key = { it.address }) { account ->
                    val isActive = account.address == activeAccountAddress
                    AccountVaultItemCard(
                        account = account,
                        isActive = isActive,
                        onSwitch = {
                            onSwitchAccount(account)
                            onDismissRequest()
                        },
                        onCopyAddress = { onCopyAddress(account.address) },
                        onCopyPassword = { onCopyPassword(account.password) },
                        onEditTag = {
                            editingAccountForTag = account
                            tagInput = account.label
                        },
                        onDelete = { onDeleteAccount(account) }
                    )
                }

                if (filteredAccounts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No matching mailboxes in vault",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Edit Tag Dialog
    if (editingAccountForTag != null) {
        val target = editingAccountForTag!!
        Dialog(onDismissRequest = { editingAccountForTag = null }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Edit Mailbox Tag",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Tag / Nickname") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { editingAccountForTag = null }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            onUpdateLabel(target.address, tagInput.trim())
                            editingAccountForTag = null
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountVaultItemCard(
    account: SavedAccountEntity,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onCopyAddress: () -> Unit,
    onCopyPassword: () -> Unit,
    onEditTag: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("vault_account_${account.address}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isActive) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Active",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    Column {
                        Text(
                            text = account.address,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (account.label.isNotBlank()) {
                            Text(
                                text = "🏷️ ${account.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val remSec = (account.expiresAt - System.currentTimeMillis()) / 1000L
                        Text(
                            text = if (remSec > 0) "⏳ ${remSec / 60}m ${remSec % 60}s remaining" else "⚠️ 10-min expired (can extend)",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (remSec > 0) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f) else MaterialTheme.colorScheme.error,
                            fontSize = 10.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCopyAddress, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Email", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onCopyPassword, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Key, contentDescription = "Copy Password", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onEditTag, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Tag", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (!isActive) {
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = onSwitch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Switch to this Inbox", fontSize = 12.sp)
                }
            }
        }
    }
}
