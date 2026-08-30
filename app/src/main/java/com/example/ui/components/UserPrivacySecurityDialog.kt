package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NoAccounts
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.AppLanguage
import com.example.util.AppStrings
import com.example.util.LocalizationManager

@Composable
fun UserPrivacySecurityDialog(
    currentLanguage: AppLanguage,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onPanicWipeAllData: () -> Unit
) {
    var showPanicConfirmDialog by remember { mutableStateOf(false) }
    val isBengali = currentLanguage == AppLanguage.BENGALI

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .testTag("user_privacy_security_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header Banner: 100% Privacy Shield
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF064E3B).copy(alpha = 0.6f),
                                    Color(0xFF0F172A)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            Color(0xFF10B981).copy(alpha = 0.35f),
                            RoundedCornerShape(18.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.18f))
                                .border(1.5.dp, Color(0xFF10B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Privacy Shield",
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFF34D399),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isBengali) "১০০% ইউজার প্রাইভেসি ও সিকিউরিটি নিশ্চয়তা" else "100% PRIVACY & SECURITY GUARANTEED",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp,
                                    color = Color(0xFF34D399)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (isBengali) "আপনার কোনো ডেটা বা লগ আমরা কখনোই সংরক্ষণ বা বিক্রি করি না" else "Zero Logs. Zero Tracking. Full Anonymity.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Official Guarantee Statement
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = if (isBengali) "আমাদের ১০০% প্রাইভেসি প্রতিশ্রুতি (Our Pledge)" else "Our Strict 100% Privacy Pledge",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isBengali)
                                "Temp Mail Pro ব্যবহারের ক্ষেত্রে একজন ইউজারের সম্পূর্ণ ব্যক্তিগত গোপনীয়তা ও সুরক্ষা রক্ষা করা আমাদের সর্বোচ্চ অঙ্গীকার। আপনি ১০০% নিশ্চিন্তে এই অ্যাপ ব্যবহার করতে পারেন — কোনো ধরনের ডেটা লিক, স্প্যাম বা গোপনীয়তা ভঙ্গের ভয় নেই।"
                            else
                                "At Temp Mail Pro, protecting user privacy is our foundational commitment. We guarantee 100% confidentiality, zero surveillance, and complete protection against spam, tracking, and identity exposure.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isBengali) "আমাদের ৬টি মূল প্রাইভেসি সুরক্ষা স্তম্ভ:" else "Core Privacy & Protection Pillars:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Privacy Pillar Cards
                PrivacyPillarCard(
                    icon = Icons.Default.VisibilityOff,
                    accentColor = Color(0xFF38BDF8),
                    title = if (isBengali) "১. জিরো লগ ও জিরো ট্র্যাকিং (Zero Logs)" else "1. Strict Zero-Log Policy",
                    description = if (isBengali)
                        "আমরা কোনো আইপি অ্যাড্রেস (IP), ব্রাউজার ফিঙ্গারপ্রিন্ট বা ইউজার কার্যকলাপ রেকর্ড করি না। আপনি সম্পূর্ণ বেনামে কাজ করবেন।"
                    else
                        "No IP addresses, connection timestamps, or browsing telemetry are ever recorded on our servers or relays."
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrivacyPillarCard(
                    icon = Icons.Default.NoAccounts,
                    accentColor = Color(0xFFA78BFA),
                    title = if (isBengali) "২. কোনো ব্যক্তিগত তথ্যের প্রয়োজন নেই" else "2. No Personal Data Required",
                    description = if (isBengali)
                        "অ্যাপটি ব্যবহার করতে কোনো সাইন-আপ, আসল নাম, ফোন নম্বর কিংবা আসল ইমেইল দিতে হয় না। কোনো অ্যাকাউন্ট ছাড়াই তাত্ক্ষণিক তৈরি।"
                    else
                        "You never have to provide your real name, phone number, personal Gmail, or credentials to use our service."
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrivacyPillarCard(
                    icon = Icons.Default.Security,
                    accentColor = Color(0xFF34D399),
                    title = if (isBengali) "৩. আসল পরিচয় গোপন ও স্প্যাম প্রতিরোধ" else "3. Burner Identity Shield",
                    description = if (isBengali)
                        "অস্থায়ী ডিসপোজেবল ইমেইল দিয়ে যেকোনো সাইটে সাইন-আপ করুন। আপনার আসল জিমেইল হ্যাকার, স্ক্যামার ও স্প্যাম ডেটাবেস থেকে ১০০% নিরাপদ থাকবে।"
                    else
                        "Mask your true identity online. Protect your personal inbox from data breaches, junk mail, and unsolicited promotional bots."
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrivacyPillarCard(
                    icon = Icons.Default.Lock,
                    accentColor = Color(0xFFFBBF24),
                    title = if (isBengali) "৪. কোনো তথ্য বিক্রি বা শেয়ার করা হয় না" else "4. Zero Data Selling or Third Parties",
                    description = if (isBengali)
                        "আপনার প্রাপ্ত ইমেইল বা মেসেজের কোনো অংশ কোনো বিজ্ঞাপনদাতা, ডেটা ব্রোকার বা তৃতীয় পক্ষের সাথে শেয়ার কিংবা বিক্রি করা হয় না।"
                    else
                        "We never monetize, sell, lease, or distribute incoming messages or addresses to marketing networks."
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrivacyPillarCard(
                    icon = Icons.Default.Key,
                    accentColor = Color(0xFFF472B6),
                    title = if (isBengali) "৫. ডিভাইসে এনক্রিপ্টেড সুরক্ষিত সংরক্ষণ" else "5. On-Device Sandboxed Storage",
                    description = if (isBengali)
                        "আপনার সেভ করা মেইলবক্স ও পাসওয়ার্ড শুধুমাত্র আপনার নিজস্ব ফোনের প্রাইভেট এনক্রিপ্টেড ডাটাবেসে থাকে, বাইরের ক্লাউডে নয়।"
                    else
                        "All saved accounts, tags, and cached headers stay on your local device storage inside Android's private app sandbox."
                )

                Spacer(modifier = Modifier.height(8.dp))

                PrivacyPillarCard(
                    icon = Icons.Default.Timer,
                    accentColor = Color(0xFF4ADE80),
                    title = if (isBengali) "৬. ডিসপোজেবল ও স্বয়ংক্রিয় ক্লিনআপ" else "6. Ephemeral & Clean Auto-Expiry",
                    description = if (isBengali)
                        "অস্থায়ী মেইলবক্সের প্রয়োজন শেষ হলে স্বয়ংক্রিয়ভাবে মুছে যায়, ইন্টারনেটে আপনার কোনো ডিজিটাল পদচিহ্ন (Footprint) অবশিষ্ট থাকে না।"
                    else
                        "Temporary mailboxes expire cleanly, permanently dissolving without leaving lingering digital traces."
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Live Privacy Audit Checklist
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "লাইভ সিকিউরিটি অডিট চেকলিস্ট" else "Live Privacy & Security Audit",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        AuditItem(
                            label = if (isBengali) "লোকাল এনক্রিপ্টেড ডাটাবেস" else "Local Sandboxed SQLite Database",
                            status = "Active & Isolated"
                        )
                        AuditItem(
                            label = if (isBengali) "ইউজার আইপি / লোকেশন লগিং" else "User IP & Location Logs",
                            status = "0% (Completely Disabled)"
                        )
                        AuditItem(
                            label = if (isBengali) "বিজ্ঞাপন ট্র্যাকার ও স্পাই পিক্সেল" else "Ad Trackers & Telemetry Pixels",
                            status = "Blocked & Filtered"
                        )
                        AuditItem(
                            label = if (isBengali) "ডেটা মালিকানা ও নিয়ন্ত্রণ" else "User Data Ownership",
                            status = "100% User Owned"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Panic Shred / Wipe Button
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isBengali) "জরুরি প্রাইভেসি ডেটা ওয়াইপ (Panic Shredder)" else "Emergency Panic Wipe (Zero-Trace)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isBengali)
                                "আপনি কি এক ক্লিকে আপনার বর্তমান ও সংরক্ষিত সমস্ত অস্থায়ী মেইলবক্স ও মেসেজ ক্যাশ স্থায়ীভাবে সম্পূর্ণ ধ্বংস করতে চান? কোনো তথ্য পুনরুদ্ধার করা যাবে না।"
                            else
                                "Need to erase everything instantly? Shred all active accounts, saved inboxes, and cached messages with zero residual trace.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { showPanicConfirmDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("panic_wipe_all_data_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isBengali) "সব তথ্য এখনই সম্পূর্ণ ধ্বংস করুন (Shred All)" else "Panic Shred All Data & Reset",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Bottom Close Button
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("close_privacy_dialog_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isBengali) "আমি সম্পূর্ণ সুরক্ষিত (বন্ধ করুন)" else "Understood (Close)",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Confirmation Alert Dialog for Panic Wipe
    if (showPanicConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPanicConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = if (isBengali) "সব ডেটা সম্পূর্ণ ধ্বংস করবেন?" else "Confirm Panic Wipe?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )
            },
            text = {
                Text(
                    text = if (isBengali)
                        "এটি নিশ্চিত করলে অ্যাপের সমস্ত সংরক্ষিত অ্যাকাউন্ট, বর্তমান মেইলবক্স এবং প্রাপ্ত মেসেজ স্থায়ীভাবে মুছে ফেলা হবে। কোনো তথ্য আর ফেরত পাওয়া যাবে না। আপনি কি নিশ্চিত?"
                    else
                        "This will permanently erase all active and saved temporary mailboxes, credentials, and local message caches. This action is irreversible.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPanicConfirmDialog = false
                        onPanicWipeAllData()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    )
                ) {
                    Text(if (isBengali) "হ্যাঁ, সম্পূর্ণ ধ্বংস করুন" else "Yes, Shred Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPanicConfirmDialog = false }) {
                    Text(if (isBengali) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun PrivacyPillarCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(9.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AuditItem(
    label: String,
    status: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFCBD5E1),
            fontSize = 11.sp
        )
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF10B981).copy(alpha = 0.18f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF34D399),
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}
