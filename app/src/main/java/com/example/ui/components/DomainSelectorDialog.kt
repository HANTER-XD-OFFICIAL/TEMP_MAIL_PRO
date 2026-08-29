package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.DomainItem
import java.util.Locale

enum class MailNetworkProvider(
    val title: String,
    val shortLabel: String,
    val badgeEmoji: String,
    val primaryColor: Color,
    val containerColor: Color,
    val speedRating: String
) {
    ALL("All Servers", "All", "🌐", Color(0xFF1976D2), Color(0xFFE3F2FD), "Global"),
    MAIL_TM("Mail.tm", "Mail.tm", "⚡", Color(0xFF00796B), Color(0xFFE0F2F1), "< 1.5s"),
    GETNADA("Getnada", "Getnada", "📬", Color(0xFF0288D1), Color(0xFFE1F5FE), "< 1.8s"),
    GUERRILLA("Guerrilla", "Guerrilla", "🛡️", Color(0xFFE65100), Color(0xFFFFF3E0), "< 2.0s")
}

fun isSecMailDomain(domain: String): Boolean {
    val d = domain.lowercase(Locale.ROOT)
    return d.contains("1secmail") || d == "esiix.com" || d == "wwjmp.com" || d == "icznn.com" || d == "ezztt.com" || d == "vmani.com"
}

fun isRapidApiDomain(domain: String): Boolean {
    val d = domain.lowercase(Locale.ROOT)
    return d.contains("rapid") || d == "cevipsa.com" || d == "freeml.net" || d == "txcct.com" || d == "vebby.com" || d == "disbox.net" || d == "dropmail.me"
}

fun resolveNetworkProvider(domain: String): MailNetworkProvider {
    val d = domain.lowercase(Locale.ROOT)
    return when {
        d.contains("nada") || d == "getairmail.com" || d == "inboxbear.com" || d == "dropjar.com" || d == "robot-mail.com" || d == "tafmail.com" || d == "vomoto.com" || d == "gimpmail.com" || d == "blondmail.com" || d == "chapsmail.com" || d == "clowmail.com" || d == "fivermail.com" || d == "getmule.com" || d == "givmail.com" || d == "guysmail.com" || d == "replyloop.com" || d == "temptami.com" || d == "tupmail.com" || d == "abyssmail.com" || d == "boximail.com" || d == "clrmail.com" -> MailNetworkProvider.GETNADA
        d.contains("guerrillamail") || d == "grr.la" || d == "sharklasers.com" || d == "pokemail.net" || d == "spam4.me" -> MailNetworkProvider.GUERRILLA
        else -> MailNetworkProvider.MAIL_TM
    }
}

@Composable
fun DomainSelectorDialog(
    availableDomains: List<DomainItem>,
    currentSelectedDomain: String,
    onDismissRequest: () -> Unit,
    onSelectDomain: (String) -> Unit,
    onDirectGenerate: ((String) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(MailNetworkProvider.ALL) }

    val filteredDomains = remember(availableDomains, searchQuery, selectedFilter) {
        availableDomains.filter { domainItem ->
            val provider = resolveNetworkProvider(domainItem.domain)
            val matchesFilter = selectedFilter == MailNetworkProvider.ALL || provider == selectedFilter
            val matchesSearch = searchQuery.isBlank() ||
                    domainItem.domain.contains(searchQuery.trim(), ignoreCase = true) ||
                    provider.title.contains(searchQuery.trim(), ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    // Compute counts per provider
    val providerCounts = remember(availableDomains) {
        val counts = mutableMapOf<MailNetworkProvider, Int>()
        counts[MailNetworkProvider.ALL] = availableDomains.size
        MailNetworkProvider.entries.filter { it != MailNetworkProvider.ALL }.forEach { provider ->
            counts[provider] = availableDomains.count { resolveNetworkProvider(it.domain) == provider }
        }
        counts
    }

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
                .fillMaxWidth(0.95f)
                .widthIn(max = 500.dp)
                .heightIn(max = 720.dp)
                .padding(vertical = 12.dp)
                .testTag("domain_selector_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // ==================== 1. TOP HEADER ====================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Select Mail Domain",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.2.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = "${availableDomains.size} Servers",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "5 verified high-speed disposable networks",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_domain_selector_btn")
                    ) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ==================== 2. SEARCH BAR ====================
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search server domain (e.g. shark, getnada, air)...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("domain_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // ==================== 3. PROVIDER FILTER TABS ====================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MailNetworkProvider.entries.forEach { provider ->
                        val isSelected = selectedFilter == provider
                        val count = providerCounts[provider] ?: 0

                        Surface(
                            onClick = { selectedFilter = provider },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = provider.badgeEmoji,
                                    fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = provider.shortLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f)
                                    } else {
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                    }
                                ) {
                                    Text(
                                        text = "$count",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing ${filteredDomains.size} of ${availableDomains.size} servers",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.size(6.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "All Nodes Operational",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF388E3C),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==================== 4. DOMAIN SERVER LIST ====================
                if (filteredDomains.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No mail servers match your query",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try searching with a different domain prefix or change the provider tab.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    searchQuery = ""
                                    selectedFilter = MailNetworkProvider.ALL
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset Filters", fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(filteredDomains, key = { it.domain }) { item ->
                            val provider = resolveNetworkProvider(item.domain)
                            val isSelected = item.domain.equals(currentSelectedDomain, ignoreCase = true)

                            DomainServerItemCard(
                                domain = item.domain,
                                provider = provider,
                                isSelected = isSelected,
                                onSelect = {
                                    onSelectDomain(item.domain)
                                    onDismissRequest()
                                },
                                onDirectGenerate = if (onDirectGenerate != null) {
                                    {
                                        onDirectGenerate(item.domain)
                                        onDismissRequest()
                                    }
                                } else null
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ==================== 5. FOOTER SYSTEM NOTE ====================
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Multi-Engine Failover: Emails auto-sync in real-time across all 5 provider gateways with zero logs.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp,
                            lineHeight = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DomainServerItemCard(
    domain: String,
    provider: MailNetworkProvider,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDirectGenerate: (() -> Unit)? = null
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        },
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("domain_item_${domain.replace(".", "_")}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Provider Icon & Domain Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Provider Avatar Badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(provider.containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = provider.badgeEmoji,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "@$domain",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = provider.containerColor
                        ) {
                            Text(
                                text = provider.title,
                                style = MaterialTheme.typography.labelSmall,
                                color = provider.primaryColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "● ${provider.speedRating}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "SSL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right: Selected Indicator or Direct Generate
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onDirectGenerate != null && !isSelected) {
                    Surface(
                        onClick = onDirectGenerate,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "Use Now",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Unselected",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
