package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.api.DomainItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateAccountDialog(
    availableDomains: List<DomainItem>,
    isCreating: Boolean,
    onDismissRequest: () -> Unit,
    onCreateRandom: (domain: String?, label: String) -> Unit,
    onCreateCustom: (username: String, domain: String, pass: String, label: String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Fast Random, 1: Custom Handle
    var selectedDomain by remember(availableDomains) {
        mutableStateOf(availableDomains.firstOrNull()?.domain ?: "emalupe.com")
    }
    var domainExpanded by remember { mutableStateOf(false) }
    var customUsername by remember { mutableStateOf("") }
    var customPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var accountLabel by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("create_account_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "New Temp Mailbox",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_create_dialog_btn")
                    ) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Mode Tabs
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Auto Instant")
                    }

                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Mail,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    ) {
                        Text("Custom Name")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Domain Selection
                if (availableDomains.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                        expanded = domainExpanded,
                        onExpandedChange = { domainExpanded = !domainExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = "@$selectedDomain",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Active Domain") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = domainExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("domain_selector_field"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = domainExpanded,
                            onDismissRequest = { domainExpanded = false }
                        ) {
                            availableDomains.forEach { domainItem ->
                                DropdownMenuItem(
                                    text = { Text("@${domainItem.domain}") },
                                    onClick = {
                                        selectedDomain = domainItem.domain
                                        domainExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 1) {
                    // Custom Username
                    OutlinedTextField(
                        value = customUsername,
                        onValueChange = { customUsername = it.trim().lowercase().replace(" ", "") },
                        label = { Text("Custom Username") },
                        placeholder = { Text("e.g. rasel.work") },
                        leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_username_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Custom Password
                    OutlinedTextField(
                        value = customPassword,
                        onValueChange = { customPassword = it },
                        label = { Text("Password (Min 6 chars)") },
                        placeholder = { Text("Secure password") },
                        leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = "Toggle password"
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_password_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Account Label / Tag
                OutlinedTextField(
                    value = accountLabel,
                    onValueChange = { accountLabel = it },
                    label = { Text("Label / Tag (Optional)") },
                    placeholder = { Text("e.g. Discord, Gaming, Lifetime") },
                    leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("account_label_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                if (selectedTab == 1 && customUsername.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Preview: ${customUsername.trim().lowercase()}@$selectedDomain",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                onCreateRandom(selectedDomain, accountLabel)
                            } else {
                                onCreateCustom(
                                    customUsername.trim().lowercase().replace(" ", ""),
                                    selectedDomain,
                                    customPassword.ifBlank { "Pass${(1000..9999).random()}!" },
                                    accountLabel.trim()
                                )
                            }
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("confirm_create_account_btn"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isCreating && (selectedTab == 0 || customUsername.length >= 3)
                    ) {
                        if (isCreating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (selectedTab == 0) "Generate" else "Create")
                        }
                    }
                }
            }
        }
    }
}
