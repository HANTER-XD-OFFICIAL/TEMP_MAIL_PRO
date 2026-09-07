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
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.MessageHeaderItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

// Vibrant Gmail-like Avatar Palette
private val GmailAvatarColors = listOf(
    Pair(Color(0xFFEA4335), Color(0xFFC5221F)), // Google Red
    Pair(Color(0xFF4285F4), Color(0xFF1A73E8)), // Google Blue
    Pair(Color(0xFF34A853), Color(0xFF1E8E3E)), // Google Green
    Pair(Color(0xFFFBBC04), Color(0xFFF9AB00)), // Google Yellow
    Pair(Color(0xFFA142F4), Color(0xFF8430CE)), // Google Purple
    Pair(Color(0xFFFA7B17), Color(0xFFE37400)), // Google Orange
    Pair(Color(0xFF00ACC1), Color(0xFF00838F)), // Google Teal
    Pair(Color(0xFFE91E63), Color(0xFFC2185B))  // Google Pink
)

@Composable
fun MessageItemCard(
    message: MessageHeaderItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isStarred by remember { mutableStateOf(false) }

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
        senderDisplay.firstOrNull()?.uppercaseChar()?.toString() ?: "G"
    }

    // Gmail-style avatar gradient based on sender
    val avatarGradient = remember(senderDisplay) {
        val index = abs(senderDisplay.hashCode()) % GmailAvatarColors.size
        GmailAvatarColors[index]
    }

    // Extract quick OTP if present in subject or intro
    val detectedOtp = remember(message.subject, message.intro) {
        extractQuickOtp((message.subject ?: "") + " " + (message.intro ?: ""))
    }

    // Clean snippet preview (stripping forwarding headers if any)
    val cleanIntro = remember(message.intro) {
        cleanSnippetText(message.intro ?: "")
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("message_card_${message.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (!message.seen) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (!message.seen) 3.dp else 1.dp,
        border = if (!message.seen) {
            androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            )
        } else {
            androidx.compose.foundation.BorderStroke(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Authentic Gmail Round Avatar with letter initial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(avatarGradient.first, avatarGradient.second)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initialChar,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Main Email Column (Sender, Subject, Snippet, OTP)
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Sender Name and Timestamp Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = senderDisplay,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!message.seen) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!message.seen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (!message.seen) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Subject Line (Bold if unread)
                Text(
                    text = if (message.subject.isNullOrBlank()) "(No Subject)" else message.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!message.seen) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Snippet Preview (Clean muted text)
                Text(
                    text = cleanIntro.ifBlank { "Tap to read full email content" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                // Quick Detection Chips (OTP / Attachment)
                if (detectedOtp != null || message.hasAttachments) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick OTP Pill (1-Click Copy)
                        if (detectedOtp != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary,
                                tonalElevation = 2.dp,
                                modifier = Modifier
                                    .clickable {
                                        clipboardManager.setText(AnnotatedString(detectedOtp))
                                        Toast.makeText(context, "Code Copied: $detectedOtp ✅", Toast.LENGTH_SHORT).show()
                                    }
                                    .testTag("otp_chip_${message.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = "OTP Code",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Code: $detectedOtp (Tap to Copy)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Attachment Pill
                        if (message.hasAttachments) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
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
                                    Spacer(modifier = Modifier.width(3.dp))
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
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Right Action Column (Gmail Star & Delete)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.height(72.dp)
            ) {
                // Gmail Star toggle
                IconButton(
                    onClick = { isStarred = !isStarred },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isStarred) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Star Email",
                        tint = if (isStarred) Color(0xFFF9AB00) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete Icon
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_message_btn_${message.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

// Extract OTP helper
private fun extractQuickOtp(rawText: String): String? {
    if (rawText.isBlank()) return null
    // Strip HTML and styles first to avoid capturing hex colors like #141823
    val text = rawText
        .replace(Regex("""<style[^>]*>[\s\S]*?</style>""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""<script[^>]*>[\s\S]*?</script>""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""<[^>]*>"""), " ")
        .replace(Regex("""&nbsp;""", RegexOption.IGNORE_CASE), " ")
        .replace(Regex("""[\r\n\t]+"""), " ")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()

    // 1. Meta / Facebook specific: "Confirmation code 446457" or "code 446457"
    val metaMatch = Regex("""(?:confirmation\s*code|security\s*code|verification\s*code|login\s*code|access\s*code)\s*[:=-]?\s*(\b\d{4,8}\b)""", RegexOption.IGNORE_CASE).find(text)
    if (metaMatch != null && metaMatch.groupValues.size > 1) {
        return metaMatch.groupValues[1]
    }

    val combinedMatch = Regex("""(?:code|otp|pin)(\d{5,8})""", RegexOption.IGNORE_CASE).find(text)
    if (combinedMatch != null && combinedMatch.groupValues.size > 1) {
        return combinedMatch.groupValues[1]
    }

    val otpRegex = Regex("""(?:code|otp|verification|pin|passcode|confirm|security)[\s\w:]{0,25}?(\b\d{4,8}\b)""", RegexOption.IGNORE_CASE)
    val match = otpRegex.find(text)
    if (match != null && match.groupValues.size > 1) {
        return match.groupValues[1]
    }
    val standalone = Regex("""\b\d{6}\b""").find(text) ?: Regex("""\b\d{5}\b""").find(text)
    return standalone?.value
}

// Clean snippet text
private fun cleanSnippetText(text: String): String {
    if (text.isBlank()) return ""
    return text
        .replace(Regex("""<[^>]*>"""), " ")
        .replace(Regex("""-{3,}\s*(Forwarded message|Original Message)[\s\S]*?(Subject:[^\n]*\n|To:[^\n]*\n|\n\n)""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""(?:From|Date|Subject|To):[^\n]*\n""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""[\r\n\t]+"""), " ")
        .replace(Regex("""\s{2,}"""), " ")
        .trim()
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
