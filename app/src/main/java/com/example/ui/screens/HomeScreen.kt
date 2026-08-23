package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AccountCredentialsDialog
import com.example.ui.components.CreateAccountDialog
import com.example.ui.components.DeveloperContactDialog
import com.example.ui.components.EmailDetailDialog
import com.example.ui.components.EmailHeaderCard
import com.example.ui.components.LanguageSelectionDialog
import com.example.ui.components.LoginAccountDialog
import com.example.ui.components.MessageItemCard
import com.example.ui.components.SavedAccountsSheet
import com.example.ui.viewmodel.TempMailViewModel
import com.example.util.AppLanguage
import com.example.util.AppStrings
import androidx.compose.material.icons.filled.Language

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TempMailViewModel
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val strings by viewModel.appStrings.collectAsStateWithLifecycle()
    val activeAccount by viewModel.activeAccount.collectAsStateWithLifecycle()
    val savedAccounts by viewModel.savedAccounts.collectAsStateWithLifecycle()
    val savedCount by viewModel.accountsCount.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isLoadingMessages by viewModel.isLoadingMessages.collectAsStateWithLifecycle()
    val isGeneratingAccount by viewModel.isGeneratingAccount.collectAsStateWithLifecycle()
    val activeMessageDetail by viewModel.activeMessageDetail.collectAsStateWithLifecycle()
    val isLoadingDetail by viewModel.isLoadingDetail.collectAsStateWithLifecycle()
    val availableDomains by viewModel.availableDomains.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val notification by viewModel.notification.collectAsStateWithLifecycle()
    val autoRefreshSec by viewModel.autoRefreshSeconds.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val isExpired by viewModel.isMailboxExpired.collectAsStateWithLifecycle()

    // Dialog & Sheet States
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(notification) {
        notification?.let {
            snackbarHostState.showSnackbar(it.message)
            viewModel.clearNotification()
        }
    }

    val filteredMessages = remember(messages, searchQuery) {
        if (searchQuery.isBlank()) messages else {
            messages.filter {
                (it.subject ?: "").contains(searchQuery, ignoreCase = true) ||
                        (it.from.address).contains(searchQuery, ignoreCase = true) ||
                        (it.from.name ?: "").contains(searchQuery, ignoreCase = true) ||
                        (it.intro ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val unreadCount = remember(messages) {
        messages.count { !it.seen }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_scaffold"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_app_icon_1787512971860),
                            contentDescription = "Temp Mail Pro Logo",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Temp Mail Pro",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "API",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Button
                    Surface(
                        onClick = { showLanguageDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("language_switch_top_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = currentLanguage.flagEmoji,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Developer Support Action Button
                    IconButton(
                        onClick = { showDeveloperDialog = true },
                        modifier = Modifier.testTag("developer_support_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Developer Support",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.refreshInbox(silent = false) },
                        modifier = Modifier.testTag("refresh_inbox_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Inbox",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(strings.newMail) },
                modifier = Modifier.testTag("fab_new_address")
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hero Email Card
            item {
                EmailHeaderCard(
                    activeAccount = activeAccount,
                    savedAccountsCount = savedCount,
                    isLoading = isLoadingMessages,
                    isGenerating = isGeneratingAccount,
                    autoRefreshSeconds = autoRefreshSec,
                    remainingSeconds = remainingSeconds,
                    isExpired = isExpired,
                    strings = strings,
                    onCopyAddress = { address ->
                        viewModel.copyToClipboard(context, address, strings.addressCopied)
                    },
                    onManualRefresh = { viewModel.refreshInbox(silent = false) },
                    onOpenCreateDialog = { showCreateDialog = true },
                    onOpenVaultSheet = { showVaultSheet = true },
                    onOpenCredentialsDialog = { showCredentialsDialog = true },
                    onOpenLoginDialog = { showLoginDialog = true },
                    onExtendTime = { minutes -> viewModel.extendActiveMailboxTime(minutes) },
                    onResetTime = { viewModel.resetActiveMailboxTime(10) }
                )
            }

            // Live Mail Server Status & Gmail Delivery Guide
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live SMTP Server Connected",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Send any email from your personal Gmail/Yahoo to this address. It will appear here within 5-10 seconds automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Developer Banner / Quick Contact Strip
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.developerContact,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        FilledTonalButton(
                            onClick = { showDeveloperDialog = true },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("open_developer_strip_btn"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(strings.developerContact, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(strings.searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("inbox_search_input"),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )
            }

            // Inbox Header Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.receivedEmails,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${messages.size}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (unreadCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "$unreadCount ${strings.newMail}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // Messages List
            if (filteredMessages.isNotEmpty()) {
                items(filteredMessages, key = { it.id }) { message ->
                    MessageItemCard(
                        message = message,
                        onClick = { viewModel.openMessage(message.id) },
                        onDelete = { viewModel.deleteCurrentMessage(message.id) }
                    )
                }
            } else {
                item {
                    EmptyInboxCard(
                        activeAddress = activeAccount?.address,
                        strings = strings,
                        onRefresh = { viewModel.refreshInbox(silent = false) },
                        onCopyAddress = {
                            activeAccount?.let {
                                viewModel.copyToClipboard(context, it.address, strings.addressCopied)
                            }
                        }
                    )
                }
            }
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { selected ->
                viewModel.setLanguage(selected)
            }
        )
    }

    // Developer Contact Dialog (User Specified)
    if (showDeveloperDialog) {
        DeveloperContactDialog(
            onDismissRequest = { showDeveloperDialog = false }
        )
    }

    // Create Account Dialog
    if (showCreateDialog) {
        CreateAccountDialog(
            availableDomains = availableDomains,
            isCreating = isGeneratingAccount,
            onDismissRequest = { showCreateDialog = false },
            onCreateRandom = { domain, label ->
                viewModel.generateQuickRandomAccount(domain, label)
            },
            onCreateCustom = { username, domain, pass, label ->
                viewModel.createCustomAccount(username, domain, pass, label)
            }
        )
    }

    // Login / Bind Account Dialog
    if (showLoginDialog) {
        LoginAccountDialog(
            isLoggingIn = isGeneratingAccount,
            onDismissRequest = { showLoginDialog = false },
            onLogin = { email, pass, label ->
                viewModel.loginExistingAccount(email, pass, label)
            }
        )
    }

    // Account Credentials Dialog
    if (showCredentialsDialog && activeAccount != null) {
        AccountCredentialsDialog(
            account = activeAccount!!,
            onDismissRequest = { showCredentialsDialog = false },
            onCopyAddress = { viewModel.copyToClipboard(context, it, strings.addressCopied) },
            onCopyPassword = { viewModel.copyToClipboard(context, it, "Password copied!") },
            onCopyAll = { viewModel.copyToClipboard(context, it, "All account credentials copied!") }
        )
    }

    // Saved Accounts Vault Sheet
    if (showVaultSheet) {
        SavedAccountsSheet(
            accounts = savedAccounts,
            activeAccountAddress = activeAccount?.address,
            onDismissRequest = { showVaultSheet = false },
            onSwitchAccount = { viewModel.switchActiveAccount(it) },
            onDeleteAccount = { viewModel.deleteAccount(it) },
            onUpdateLabel = { addr, label -> viewModel.updateAccountLabel(addr, label) },
            onOpenCreateDialog = { showCreateDialog = true },
            onOpenLoginDialog = { showLoginDialog = true },
            onCopyAddress = { viewModel.copyToClipboard(context, it, strings.addressCopied) },
            onCopyPassword = { viewModel.copyToClipboard(context, it, "Password copied!") },
            onExportAll = { viewModel.copyToClipboard(context, it, "Vault credentials exported to clipboard!") }
        )
    }

    // Full Email Detail Dialog
    if (activeMessageDetail != null || isLoadingDetail) {
        EmailDetailDialog(
            message = activeMessageDetail,
            isLoading = isLoadingDetail,
            onDismissRequest = { viewModel.closeMessageDetail() },
            onDelete = { viewModel.deleteCurrentMessage(it) },
            onCopyContent = { viewModel.copyToClipboard(context, it, "Email content copied!") }
        )
    }
}

@Composable
private fun EmptyInboxCard(
    activeAddress: String?,
    strings: AppStrings,
    onRefresh: () -> Unit,
    onCopyAddress: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("empty_inbox_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inbox,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = strings.emptyInboxTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = strings.emptyInboxSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCopyAddress,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_copy_address_btn")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.copyAddress)
                }

                FilledTonalButton(
                    onClick = onRefresh,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("empty_refresh_btn")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.refresh)
                }
            }
        }
    }
}
