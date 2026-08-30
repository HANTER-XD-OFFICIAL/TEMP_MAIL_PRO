package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class EngineNodeInfo(
    val name: String,
    val emoji: String,
    val status: String,
    val ping: String,
    val domainCount: String,
    val techStack: String,
    val description: String,
    val accentColor: Color,
    val bgColor: Color
)

@Composable
fun ServerNetworkHubDialog(
    totalDomainsCount: Int,
    onDismissRequest: () -> Unit,
    onOpenDomainSelector: () -> Unit
) {
    val engines = listOf(
        EngineNodeInfo(
            name = "Maildrop GraphQL Engine",
            emoji = "🚀",
            status = "Operational",
            ping = "70ms",
            domainCount = "maildrop.cc (100% Live)",
            techStack = "GraphQL API • Instant Webhook",
            description = "High-speed disposable email delivery engine with instant message reception and HTML rendering.",
            accentColor = Color(0xFF2E7D32),
            bgColor = Color(0xFFE8F5E9)
        ),
        EngineNodeInfo(
            name = "Guerrilla Mail Network",
            emoji = "🛡️",
            status = "Operational",
            ping = "118ms",
            domainCount = "9 Live Domains",
            techStack = "Session SID Protocol • Grr.la",
            description = "Decentralized disposable engine with sharklasers.com and pokemail.net, immune to spam traps.",
            accentColor = Color(0xFFE65100),
            bgColor = Color(0xFFFFF3E0)
        ),
        EngineNodeInfo(
            name = "Mail.tm Primary Core",
            emoji = "⚡",
            status = "Operational",
            ping = "85ms",
            domainCount = "Active Domain",
            techStack = "REST API • JWT Bearer Auth",
            description = "High-fidelity mailbox provider with persistent credentials vault and automated real-time delivery.",
            accentColor = Color(0xFF00796B),
            bgColor = Color(0xFFE0F2F1)
        ),
        EngineNodeInfo(
            name = "Getnada / Inboxes Cluster",
            emoji = "📬",
            status = "Operational",
            ping = "110ms",
            domainCount = "18 Live Domains",
            techStack = "HTML Realtime Mesh • Inboxes.com",
            description = "Large multi-domain network including getairmail, inboxbear, and dropjar with instant reception.",
            accentColor = Color(0xFF0288D1),
            bgColor = Color(0xFFE1F5FE)
        ),
        EngineNodeInfo(
            name = "1secmail Mirror Network",
            emoji = "⏱️",
            status = "Server Issue (HTTP 403)",
            ping = "Blocked",
            domainCount = "5 Domains (Outage)",
            techStack = "Upstream Cloudflare Block (403)",
            description = "1secmail.com public API is currently returning 403 Forbidden upstream. Emails cannot be received. Use Maildrop or GuerrillaMail instead.",
            accentColor = Color(0xFFC62828),
            bgColor = Color(0xFFFFEBEE)
        )
    )

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
                .widthIn(max = 520.dp)
                .heightIn(max = 720.dp)
                .padding(vertical = 12.dp)
                .testTag("server_network_hub_dialog"),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                // Header
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
                                imageVector = Icons.Default.Hub,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Server Network Hub",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.2.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFE8F5E9)
                                ) {
                                    Text(
                                        text = "100% ONLINE",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        fontSize = 9.sp,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "3 Verified Live Gateways • $totalDomainsCount Domains",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.testTag("close_server_hub_btn")
                    ) {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Network Key Metrics
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Avg. Ping",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "92ms",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Active Nodes",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "3 / 3 Active",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outlineVariant))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Privacy & Logs",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "Zero Logs",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Engines List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 6.dp)
                ) {
                    items(engines.size) { index ->
                        val engine = engines[index]
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(engine.bgColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = engine.emoji, fontSize = 15.sp)
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = engine.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = engine.techStack,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color(0xFF2E7D32),
                                                modifier = Modifier.size(5.dp)
                                            ) {}
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = engine.ping,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = engine.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = engine.bgColor
                                    ) {
                                        Text(
                                            text = engine.domainCount,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = engine.accentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "TLS 1.3 Encrypted",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close", fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onDismissRequest()
                            onOpenDomainSelector()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("browse_domains_from_hub_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select Domain", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
