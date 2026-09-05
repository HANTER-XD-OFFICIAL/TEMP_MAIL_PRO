package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContactSupport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import com.example.ui.components.isBrokenDeliveryDomain
import com.example.ui.components.isSecMailDomain
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AccountCredentialsDialog
import com.example.ui.components.CreateAccountDialog
import com.example.ui.components.DeveloperContactDialog
import com.example.ui.components.DomainSelectorDialog
import com.example.ui.components.EmailDetailDialog
import com.example.ui.components.EmailHeaderCard
import com.example.ui.components.LanguageSelectionDialog
import com.example.ui.components.LoginAccountDialog
import com.example.ui.components.MessageItemCard
import com.example.ui.components.SavedAccountsSheet
import com.example.ui.components.ServerNetworkHubDialog
import com.example.ui.components.TelegramBotWelcomeDialog
import com.example.ui.components.UserPrivacySecurityDialog
import com.example.ui.components.isSecMailDomain
import com.example.ui.components.isRapidApiDomain
import com.example.ui.viewmodel.TempMailViewModel
import com.example.util.AppLanguage
import com.example.util.AppStrings

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
    var showTelegramWelcomeDialog by remember { mutableStateOf(true) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDeveloperDialog by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showCredentialsDialog by remember { mutableStateOf(false) }
    var showVaultSheet by remember { mutableStateOf(false) }
    var showServerHubDialog by remember { mutableStateOf(false) }
    var showDomainSelectorDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

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
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo_tempmailpro_1787514649787),
                            contentDescription = "Temp Mail Pro Logo",
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Temp Mail",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.2.sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "PRO",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Verified Gateways • Zero Logs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher Button (compact)
                    Surface(
                        onClick = { showLanguageDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(end = 2.dp)
                            .testTag("language_switch_top_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentLanguage.flagEmoji,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 100% User Privacy & Security Action Button
                    IconButton(
                        onClick = { showPrivacyDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("privacy_security_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = strings.privacyGuaranteeTitle,
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Telegram Bot Action Button (compact)
                    IconButton(
                        onClick = { showTelegramWelcomeDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("telegram_bot_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Telegram Bot (@TEMPMAILPRO34_bot)",
                            tint = Color(0xFF229ED9),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Developer Support Action Button (compact)
                    IconButton(
                        onClick = { showDeveloperDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("developer_support_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Developer Support",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Refresh Button (compact)
                    IconButton(
                        onClick = { viewModel.refreshInbox(silent = false) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("refresh_inbox_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Inbox",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
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
                    onOpenServerHub = { showServerHubDialog = true },
                    onOpenDomainSelector = { showDomainSelectorDialog = true },
                    onExtendTime = { minutes -> viewModel.extendActiveMailboxTime(minutes) },
                    onResetTime = { viewModel.resetActiveMailboxTime(10) }
                )
            }

            // Delivery Outage Warning Banner for maildrop.cc and 1secmail
            if (activeAccount != null && isBrokenDeliveryDomain(activeAccount!!.address.substringAfter("@", ""))) {
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFFFEBEE),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF5350)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("secmail_outage_banner")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFFCDD2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFC62828),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "মেইল রিসিভ সমস্যা (Server Delivery Outage)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB71C1C)
                                    )
                                    Text(
                                        text = "maildrop.cc এবং 1secmail সার্ভারে জিমেইল বা ওয়েবসাইট থেকে কোনো মেসেজ আসে না (Greylisting / 403 ব্লক)। তাৎক্ষণিক মেসেজ ও ভেরিফিকেশন কোড পাওয়ার জন্য নিচের সচল ইনবক্সে সুইচ করুন।",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF37474F),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        viewModel.generateQuickRandomAccount(domain = "sharklasers.com")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFE65100)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB74D)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Guerrilla (🛡️)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = {
                                        viewModel.generateQuickRandomAccount(domain = "emalupe.com")
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF00796B),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1.3f)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Mail.tm এ যান (সচল ⚡)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Live Mail Server Status & Multi-Engine Gateways Hub
            item {
                Surface(
                    onClick = { showServerHubDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_hub_gateway_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Verified Gateways",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "${availableDomains.size} Domains",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Mail.tm • Getnada • Guerrilla • 100% Active. Tap for latency.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // 100% User Privacy & Security Guarantee Banner
            item {
                Surface(
                    onClick = { showPrivacyDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.25f),
                    border = BorderStroke(
                        1.dp,
                        Color(0xFF10B981).copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("user_privacy_guarantee_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Privacy Shield",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = strings.privacyGuaranteeTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "100%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF34D399),
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.privacySubtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("view_privacy_guarantee_btn"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF10B981).copy(alpha = 0.15f),
                                contentColor = Color(0xFF34D399)
                            )
                        ) {
                            Text(
                                text = if (currentLanguage == AppLanguage.BENGALI) "নিশ্চয়তা" else "Policy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
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
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDeveloperDialog = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupportAgent,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = strings.developerContact,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(androidx.compose.ui.graphics.Color(0xFF10B981))
                                    )
                                }
                                Text(
                                    text = "MD RASEL • WhatsApp, Telegram & Bot",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showDeveloperDialog = true },
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("open_developer_strip_btn"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = strings.developerAction,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp)
                            )
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

    // Telegram Bot Welcome Dialog (Auto-pops on start & on tap)
    if (showTelegramWelcomeDialog) {
        TelegramBotWelcomeDialog(
            botUsername = "TEMPMAILPRO34_bot",
            onDismiss = { showTelegramWelcomeDialog = false }
        )
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
            onDismissRequest = { showDeveloperDialog = false },
            onOpenPrivacyGuarantee = {
                showDeveloperDialog = false
                showPrivacyDialog = true
            }
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
            onExportAll = { viewModel.copyToClipboard(context, it, "Vault credentials exported to clipboard!") },
            onPanicWipe = { viewModel.panicWipeAllData(strings.panicWipeSuccess) }
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

    // Server Network Hub Dialog
    if (showServerHubDialog) {
        ServerNetworkHubDialog(
            totalDomainsCount = availableDomains.size,
            onDismissRequest = { showServerHubDialog = false },
            onOpenDomainSelector = {
                showServerHubDialog = false
                showDomainSelectorDialog = true
            }
        )
    }

    // Direct Domain Selector Dialog
    if (showDomainSelectorDialog) {
        DomainSelectorDialog(
            availableDomains = availableDomains,
            currentSelectedDomain = activeAccount?.address?.substringAfter("@") ?: (availableDomains.firstOrNull()?.domain ?: ""),
            onDismissRequest = { showDomainSelectorDialog = false },
            onSelectDomain = { selectedDomain ->
                showDomainSelectorDialog = false
                viewModel.generateQuickRandomAccount(domain = selectedDomain, label = "")
            },
            onDirectGenerate = { selectedDomain ->
                showDomainSelectorDialog = false
                viewModel.generateQuickRandomAccount(domain = selectedDomain, label = "")
            }
        )
    }

    // 100% User Privacy & Security Guarantee Dialog
    if (showPrivacyDialog) {
        UserPrivacySecurityDialog(
            currentLanguage = currentLanguage,
            strings = strings,
            onDismiss = { showPrivacyDialog = false },
            onPanicWipeAllData = {
                viewModel.panicWipeAllData(strings.panicWipeSuccess)
            }
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
