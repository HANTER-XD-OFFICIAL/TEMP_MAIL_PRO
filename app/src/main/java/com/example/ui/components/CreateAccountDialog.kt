package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
    val activeAvailableDomains = remember(availableDomains) {
        availableDomains.filterNot { isBrokenDeliveryDomain(it.domain) }
    }
    var selectedDomain by remember(activeAvailableDomains) {
        mutableStateOf(activeAvailableDomains.firstOrNull()?.domain ?: "emalupe.com")
    }
    var showDomainSelectorDialog by remember { mutableStateOf(false) }
    var customUsername by remember { mutableStateOf("") }
    var customPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var accountLabel by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 440.dp)
                .heightIn(max = 660.dp)
                .padding(vertical = 16.dp)
                .testTag("create_account_dialog"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Pinned)
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

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Form Fields
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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

                    Spacer(modifier = Modifier.height(14.dp))

                    // Domain Selection Card
                    if (availableDomains.isNotEmpty()) {
                        val activeProvider = resolveNetworkProvider(selectedDomain)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MAIL SERVER DOMAIN",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp
                                )

                                Text(
                                    text = "${availableDomains.size} Servers Available",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Surface(
                                onClick = { showDomainSelectorDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("domain_selector_field")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Provider Avatar Icon + Domain Name & Status
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(activeProvider.containerColor)
                                                .border(
                                                    1.dp,
                                                    activeProvider.primaryColor.copy(alpha = 0.3f),
                                                    RoundedCornerShape(10.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = activeProvider.badgeEmoji, fontSize = 18.sp)
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "@$selectedDomain",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(3.dp))

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = activeProvider.containerColor
                                                ) {
                                                    Text(
                                                        text = activeProvider.shortLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = activeProvider.primaryColor,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.5.dp)
                                                    )
                                                }

                                                Surface(
                                                    shape = CircleShape,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.size(5.dp)
                                                ) {}

                                                Text(
                                                    text = "Live & Verified",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF10B981),
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Right: Change Button
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Change",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontSize = 11.sp
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTab == 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generates a randomized secure mailbox on @$selectedDomain",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
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
                            placeholder = { Text("e.g. user.work") },
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
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Preview: ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${customUsername.trim().lowercase()}@$selectedDomain",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons (Pinned at bottom)
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

    if (showDomainSelectorDialog) {
        DomainSelectorDialog(
            availableDomains = activeAvailableDomains,
            currentSelectedDomain = selectedDomain,
            onDismissRequest = { showDomainSelectorDialog = false },
            onSelectDomain = {
                selectedDomain = it
                showDomainSelectorDialog = false
            }
        )
    }
}
