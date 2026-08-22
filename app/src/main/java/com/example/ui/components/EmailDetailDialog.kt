package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.MessageDetailResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun EmailDetailDialog(
    message: MessageDetailResponse?,
    isLoading: Boolean,
    onDismissRequest: () -> Unit,
    onDelete: (String) -> Unit,
    onCopyContent: (String) -> Unit
) {
    val context = LocalContext.current
    var viewMode by remember { mutableIntStateOf(0) } // 0: Formatted Text, 1: Raw Text, 2: HTML

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("email_detail_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            if (isLoading || message == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading complete email...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                ) {
                    // Top App Bar in Reader
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier.testTag("back_from_email_btn")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Email Reader",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Copy body button
                            IconButton(
                                onClick = {
                                    val contentToCopy = message.text ?: message.html?.firstOrNull() ?: message.intro ?: ""
                                    onCopyContent(contentToCopy)
                                },
                                modifier = Modifier.testTag("copy_email_body_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Content")
                            }

                            // Share button
                            IconButton(
                                onClick = {
                                    shareEmail(context, message.subject ?: "Email", message.text ?: message.intro ?: "")
                                },
                                modifier = Modifier.testTag("share_email_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Email")
                            }

                            // Delete button
                            IconButton(
                                onClick = {
                                    onDelete(message.id)
                                    onDismissRequest()
                                },
                                modifier = Modifier.testTag("delete_open_email_btn")
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Email",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Header Meta Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Subject
                            Text(
                                text = if (message.subject.isNullOrBlank()) "(No Subject)" else message.subject,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // From
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "From: ",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (!message.from.name.isNullOrBlank()) "${message.from.name} <${message.from.address}>" else message.from.address,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // To
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "To: ",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val toList = message.to.joinToString(", ") { it.address }
                                Text(
                                    text = toList.ifBlank { "You" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Date
                            val dateParsed = remember(message.createdAt) {
                                formatFullDate(message.createdAt)
                            }
                            Text(
                                text = "Received: $dateParsed",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // View Mode Switcher
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SegmentedButton(
                            selected = viewMode == 0,
                            onClick = { viewMode = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("Text View")
                        }
                        SegmentedButton(
                            selected = viewMode == 1,
                            onClick = { viewMode = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("Raw / HTML")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Scrollable Message Content
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val bodyText = message.text
                        val htmlContent = message.html?.firstOrNull()

                        if (viewMode == 0) {
                            if (!bodyText.isNullOrBlank()) {
                                Text(
                                    text = bodyText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            } else if (!htmlContent.isNullOrBlank()) {
                                // Render formatted HTML using AndroidView TextView
                                AndroidView(
                                    factory = { ctx ->
                                        TextView(ctx).apply {
                                            setTextColor(android.graphics.Color.WHITE)
                                            textSize = 15f
                                            text = Html.fromHtml(htmlContent, Html.FROM_HTML_MODE_COMPACT)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = message.intro ?: "Empty email body",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Raw / HTML view
                            Text(
                                text = htmlContent ?: bodyText ?: "(No raw body)",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Attachments if any
                    if (!message.attachments.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Attachments (${message.attachments.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        message.attachments.forEach { att ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = att.filename,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${att.size / 1024} KB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFullDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val cleanStr = if (dateStr.length >= 19) dateStr.substring(0, 19) else dateStr
        val date = parser.parse(cleanStr) ?: Date()
        val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        format.format(date)
    } catch (e: Exception) {
        dateStr
    }
}

private fun shareEmail(context: Context, subject: String, body: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Subject: $subject\n\n$body")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Share Email via"))
    } catch (e: Exception) {
        // Ignored
    }
}
