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
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.MessageHeaderItem
import com.example.ui.theme.AccentRose
import com.example.ui.theme.GlowCyan
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun MessageItemCard(
    message: MessageHeaderItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedTime = remember(message.createdAt) {
        formatMessageTimestamp(message.createdAt)
    }

    val senderDisplay = remember(message.from) {
        if (!message.from.name.isNullOrBlank()) {
            message.from.name
        } else if (message.from.address.isNotBlank()) {
            message.from.address
        } else {
            "Unknown Sender"
        }
    }

    val initialChar = remember(senderDisplay) {
        senderDisplay.firstOrNull()?.uppercaseChar()?.toString() ?: "M"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("message_card_${message.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (message.seen) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (message.seen) 1.dp else 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (!message.seen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialChar,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (!message.seen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Message Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = senderDisplay,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (!message.seen) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!message.seen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (!message.seen) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Subject
                Text(
                    text = if (message.subject.isNullOrBlank()) "(No Subject)" else message.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!message.seen) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                // Snippet intro
                Text(
                    text = if (message.intro.isNullOrBlank()) "Click to open full message body" else message.intro,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (message.hasAttachments) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = "Attachment",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Attachment",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Delete action
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(32.dp)
                    .testTag("delete_message_btn_${message.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatMessageTimestamp(dateStr: String): String {
    return try {
        val cleanStr = if (dateStr.length >= 19) dateStr.substring(0, 19) else dateStr
        val parser = if (cleanStr.contains("T")) {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        val date = parser.parse(cleanStr) ?: Date()
        val now = System.currentTimeMillis()
        val diff = maxOf(0L, now - date.time)

        when {
            diff < 60_000 -> "Just now"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            diff < 86400_000 -> {
                val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                timeFormat.format(date)
            }
            else -> {
                val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                dayFormat.format(date)
            }
        }
    } catch (e: Exception) {
        "Recent"
    }
}
