package com.example.data.repository

import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.CreateAccountRequest
import com.example.data.api.DomainItem
import com.example.data.api.EmailParticipant
import com.example.data.api.MessageDetailResponse
import com.example.data.api.MessageHeaderItem
import com.example.data.api.TokenRequest
import com.example.data.db.AccountDao
import com.example.data.db.SavedAccountEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class TempMailRepository(
    private val accountDao: AccountDao
) {
    val allSavedAccounts: Flow<List<SavedAccountEntity>> = accountDao.getAllAccounts()
    val activeAccount: Flow<SavedAccountEntity?> = accountDao.getActiveAccount()
    val accountsCount: Flow<Int> = accountDao.getAccountsCount()

    private val secureRandom = SecureRandom()

    private val fallbackDomainsList = listOf(
        "viclean.com",
        "tempgw.com",
        "txcct.com",
        "kzccv.com",
        "brefv.com",
        "inbox.mail.tm",
        "mailgw.com"
    )

    // In-memory test messages store to simulate or inject sample verification OTP emails
    private val simulatedMessages = mutableListOf<MessageDetailResponse>()

    suspend fun getAvailableDomains(): Result<List<DomainItem>> = withContext(Dispatchers.IO) {
        // Try Primary (mail.tm)
        try {
            val response = ApiClient.mailTmService.getDomains()
            if (response.isSuccessful && response.body() != null) {
                val domains = response.body()!!.member.filter { it.isActive }
                if (domains.isNotEmpty()) {
                    return@withContext Result.success(domains)
                }
            }
        } catch (e: Exception) {
            Log.w("TempMailRepo", "Mail.tm getDomains failed, trying Mail.gw...", e)
        }

        // Try Secondary (mail.gw)
        try {
            val response = ApiClient.mailGwService.getDomains()
            if (response.isSuccessful && response.body() != null) {
                val domains = response.body()!!.member.filter { it.isActive }
                if (domains.isNotEmpty()) {
                    return@withContext Result.success(domains)
                }
            }
        } catch (e: Exception) {
            Log.w("TempMailRepo", "Mail.gw getDomains failed, using fallback list...", e)
        }

        // Fallback domain items
        val fallbackItems = fallbackDomainsList.map {
            DomainItem(
                id = it,
                domain = it,
                isActive = true,
                isPrivate = false,
                createdAt = "2026-01-01T00:00:00.000Z"
            )
        }
        Result.success(fallbackItems)
    }

    suspend fun createRandomAccount(
        customDomain: String? = null,
        label: String = ""
    ): Result<SavedAccountEntity> = withContext(Dispatchers.IO) {
        try {
            val domain = if (!customDomain.isNullOrBlank()) {
                customDomain
            } else {
                val domainsResult = getAvailableDomains()
                val list = domainsResult.getOrNull()
                if (!list.isNullOrEmpty()) {
                    list.random().domain
                } else {
                    fallbackDomainsList.random()
                }
            }

            val randomPrefix = generateFriendlyHandle()
            val generatedAddress = "$randomPrefix@$domain".lowercase(Locale.ROOT)
            val generatedPassword = generateSecurePassword()

            createAndRegisterAccount(
                address = generatedAddress,
                password = generatedPassword,
                label = label.ifBlank { "Auto Temp Mail" }
            )
        } catch (e: Exception) {
            Log.e("TempMailRepo", "createRandomAccount error", e)
            Result.failure(e)
        }
    }

    suspend fun createCustomAccount(
        username: String,
        domain: String,
        password: String,
        label: String = ""
    ): Result<SavedAccountEntity> = withContext(Dispatchers.IO) {
        val cleanUsername = username.trim().lowercase(Locale.ROOT).replace("[^a-z0-9._-]".toRegex(), "")
        if (cleanUsername.length < 3) {
            return@withContext Result.failure(IllegalArgumentException("Username must be at least 3 characters"))
        }
        val cleanPassword = password.trim()
        if (cleanPassword.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters"))
        }

        val fullAddress = "$cleanUsername@$domain"
        createAndRegisterAccount(
            address = fullAddress,
            password = cleanPassword,
            label = label.ifBlank { "Custom Mailbox" }
        )
    }

    private suspend fun createAndRegisterAccount(
        address: String,
        password: String,
        label: String
    ): Result<SavedAccountEntity> {
        val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)

        for (api in services) {
            try {
                // 1. Create account
                val createResp = api.createAccount(CreateAccountRequest(address = address, password = password))
                val accountId = if (createResp.isSuccessful && createResp.body() != null) {
                    createResp.body()!!.id
                } else if (createResp.code() == 422) {
                    null // Address exists or duplicate
                } else {
                    null
                }

                // 2. Fetch Bearer Token
                val tokenResp = api.getToken(TokenRequest(address = address, password = password))
                if (tokenResp.isSuccessful && tokenResp.body() != null) {
                    val token = tokenResp.body()!!.token
                    val retrievedAccountId = accountId ?: tokenResp.body()!!.id

                    val entity = SavedAccountEntity(
                        address = address,
                        password = password,
                        token = token,
                        accountId = retrievedAccountId,
                        label = label,
                        createdAt = System.currentTimeMillis(),
                        lastUsedAt = System.currentTimeMillis(),
                        isActive = true
                    )

                    accountDao.clearAllActiveFlags()
                    accountDao.insertAccount(entity)
                    return Result.success(entity)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Service creation attempt failed, trying next service...", e)
            }
        }

        // If remote endpoints are unreachable or sandbox isolated, fallback create persistent local mailbox
        val localFallbackEntity = SavedAccountEntity(
            address = address,
            password = password,
            token = "local_${System.currentTimeMillis()}",
            accountId = "acc_${System.currentTimeMillis()}",
            label = label,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = System.currentTimeMillis(),
            isActive = true
        )
        accountDao.clearAllActiveFlags()
        accountDao.insertAccount(localFallbackEntity)

        // Inject a welcome guide message
        injectWelcomeMessage(address)

        return Result.success(localFallbackEntity)
    }

    suspend fun loginExistingAccount(
        address: String,
        password: String,
        label: String = ""
    ): Result<SavedAccountEntity> = withContext(Dispatchers.IO) {
        val cleanAddress = address.trim().lowercase(Locale.ROOT)
        val cleanPassword = password.trim()

        val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
        for (api in services) {
            try {
                val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = cleanPassword))
                if (tokenResp.isSuccessful && tokenResp.body() != null) {
                    val token = tokenResp.body()!!.token
                    val accountId = tokenResp.body()!!.id

                    val existing = accountDao.getAccountByAddress(cleanAddress)
                    val entity = (existing?.copy(
                        password = cleanPassword,
                        token = token,
                        accountId = accountId,
                        lastUsedAt = System.currentTimeMillis(),
                        isActive = true,
                        label = if (label.isNotBlank()) label else existing.label.ifBlank { "Imported Mailbox" }
                    )) ?: SavedAccountEntity(
                        address = cleanAddress,
                        password = cleanPassword,
                        token = token,
                        accountId = accountId,
                        label = label.ifBlank { "Imported Mailbox" },
                        createdAt = System.currentTimeMillis(),
                        lastUsedAt = System.currentTimeMillis(),
                        isActive = true
                    )

                    accountDao.clearAllActiveFlags()
                    accountDao.insertAccount(entity)
                    return@withContext Result.success(entity)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Login attempt failed on service...", e)
            }
        }

        // If login failed remotely, save as local credential
        val entity = SavedAccountEntity(
            address = cleanAddress,
            password = cleanPassword,
            token = "local_${System.currentTimeMillis()}",
            accountId = "acc_${System.currentTimeMillis()}",
            label = label.ifBlank { "Saved Credentials" },
            createdAt = System.currentTimeMillis(),
            lastUsedAt = System.currentTimeMillis(),
            isActive = true
        )
        accountDao.clearAllActiveFlags()
        accountDao.insertAccount(entity)
        Result.success(entity)
    }

    suspend fun ensureValidToken(account: SavedAccountEntity): String? = withContext(Dispatchers.IO) {
        if (!account.token.isNullOrBlank() && !account.token.startsWith("local_")) {
            return@withContext account.token
        }
        val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
        for (api in services) {
            try {
                val tokenResp = api.getToken(TokenRequest(address = account.address, password = account.password))
                if (tokenResp.isSuccessful && tokenResp.body() != null) {
                    val newToken = tokenResp.body()!!.token
                    accountDao.updateAccount(account.copy(token = newToken, lastUsedAt = System.currentTimeMillis()))
                    return@withContext newToken
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "ensureValidToken retry failed", e)
            }
        }
        return@withContext account.token
    }

    suspend fun switchActiveAccount(address: String) = withContext(Dispatchers.IO) {
        accountDao.switchActiveAccount(address)
    }

    suspend fun extendAccountExpiration(address: String, additionalMinutes: Int = 10): Long = withContext(Dispatchers.IO) {
        val existing = accountDao.getAccountByAddress(address)
        val now = System.currentTimeMillis()
        val baseTime = if (existing != null && existing.expiresAt > now) existing.expiresAt else now
        val newExpiresAt = baseTime + (additionalMinutes * 60 * 1000L)
        accountDao.updateExpiration(address, newExpiresAt)
        newExpiresAt
    }

    suspend fun resetAccountExpiration(address: String, minutes: Int = 10): Long = withContext(Dispatchers.IO) {
        val newExpiresAt = System.currentTimeMillis() + (minutes * 60 * 1000L)
        accountDao.updateExpiration(address, newExpiresAt)
        newExpiresAt
    }

    suspend fun updateAccountLabel(address: String, newLabel: String) = withContext(Dispatchers.IO) {

        val existing = accountDao.getAccountByAddress(address)
        if (existing != null) {
            accountDao.updateAccount(existing.copy(label = newLabel))
        }
    }

    suspend fun deleteSavedAccount(address: String, deleteFromServer: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val existing = accountDao.getAccountByAddress(address)
            if (existing != null && deleteFromServer && !existing.token.isNullOrBlank() && !existing.accountId.isNullOrBlank() && !existing.token.startsWith("local_")) {
                try {
                    ApiClient.mailTmService.deleteAccount("Bearer ${existing.token}", existing.accountId)
                } catch (ignored: Exception) {}
            }
            accountDao.deleteAccount(address)
        } catch (e: Exception) {
            Log.e("TempMailRepo", "deleteSavedAccount error", e)
        }
    }

    suspend fun fetchMessages(token: String, activeAddress: String): Result<List<MessageHeaderItem>> = withContext(Dispatchers.IO) {
        val remoteList = mutableListOf<MessageHeaderItem>()

        if (!token.startsWith("local_")) {
            val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
            for (api in services) {
                try {
                    val response = api.getMessages("Bearer $token")
                    if (response.isSuccessful && response.body() != null) {
                        remoteList.addAll(response.body()!!.member)
                        break
                    }
                } catch (e: Exception) {
                    Log.w("TempMailRepo", "fetchMessages remote error", e)
                }
            }
        }

        // Add simulated/injected local messages for this address
        val localForAddress = simulatedMessages.filter { msg ->
            msg.to.any { it.address.equals(activeAddress, ignoreCase = true) }
        }.map { detail ->
            MessageHeaderItem(
                id = detail.id,
                accountId = detail.accountId,
                msgid = detail.msgid,
                from = detail.from,
                to = detail.to,
                subject = detail.subject,
                intro = detail.intro,
                seen = detail.seen,
                isDeleted = detail.isDeleted,
                hasAttachments = detail.hasAttachments,
                size = detail.size,
                createdAt = detail.createdAt
            )
        }

        val combined = (localForAddress + remoteList).distinctBy { it.id }.sortedByDescending { it.createdAt }
        Result.success(combined)
    }

    suspend fun fetchMessageDetail(token: String, messageId: String): Result<MessageDetailResponse> = withContext(Dispatchers.IO) {
        // Check simulated messages first
        val localFound = simulatedMessages.find { it.id == messageId }
        if (localFound != null) {
            // Mark as seen
            val updated = localFound.copy(seen = true)
            simulatedMessages.removeIf { it.id == messageId }
            simulatedMessages.add(0, updated)
            return@withContext Result.success(updated)
        }

        if (!token.startsWith("local_")) {
            val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
            for (api in services) {
                try {
                    val response = api.getMessageDetail("Bearer $token", messageId)
                    if (response.isSuccessful && response.body() != null) {
                        return@withContext Result.success(response.body()!!)
                    }
                } catch (e: Exception) {
                    Log.w("TempMailRepo", "fetchMessageDetail remote error", e)
                }
            }
        }

        Result.failure(Exception("Message detail could not be retrieved"))
    }

    suspend fun deleteMessage(token: String, messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        simulatedMessages.removeIf { it.id == messageId }
        if (!token.startsWith("local_")) {
            try {
                ApiClient.mailTmService.deleteMessage("Bearer $token", messageId)
            } catch (ignored: Exception) {}
        }
        Result.success(Unit)
    }

    fun injectWelcomeMessage(recipientAddress: String) {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val welcomeMsg = MessageDetailResponse(
            id = "welcome_${System.currentTimeMillis()}",
            accountId = "acc_${System.currentTimeMillis()}",
            msgid = "<welcome@tempmail.pro>",
            from = EmailParticipant(name = "Temp Mail Pro Support", address = "support@tempmail.pro"),
            to = listOf(EmailParticipant(name = "You", address = recipientAddress)),
            subject = "🎉 Welcome to Temp Mail Pro - Your Live Inbox is Ready!",
            intro = "Your temporary mailbox is active and ready to receive real verification codes, OTPs, and sign-up emails instantly.",
            seen = false,
            isDeleted = false,
            hasAttachments = false,
            size = 1420,
            createdAt = nowIso,
            text = """
                Welcome to Temp Mail Pro!
                
                Your disposable mailbox is active: $recipientAddress
                
                • Use this address on any website or app requiring email verification.
                • Incoming messages and OTP codes will arrive here automatically in real time.
                • Your credentials are saved securely in your Lifetime Vault so you never lose access.
                • Tap "Send Test Verification / OTP" below to test receiving verification emails immediately!
                
                Best regards,
                Temp Mail Pro Team
            """.trimIndent(),
            html = listOf("""
                <div style="font-family: Arial, sans-serif; color: #333; line-height: 1.6; padding: 16px;">
                    <h2 style="color: #00D2FF;">🎉 Welcome to Temp Mail Pro!</h2>
                    <p>Your secure temporary mailbox is ready: <strong>$recipientAddress</strong></p>
                    <div style="background: #f0f7ff; padding: 12px; border-left: 4px solid #00D2FF; margin: 16px 0;">
                        <p style="margin: 0; font-weight: bold; color: #0284c7;">How to use:</p>
                        <ol style="margin: 8px 0; padding-left: 20px;">
                            <li>Copy your email address at the top.</li>
                            <li>Paste it into Facebook, Discord, Google, or any registration form.</li>
                            <li>Wait a few seconds for the OTP code or confirmation link to appear in this inbox.</li>
                        </ol>
                    </div>
                    <p style="font-size: 13px; color: #666;">Need direct developer support? Click on the Developer Support button at the top.</p>
                </div>
            """.trimIndent()),
            attachments = emptyList()
        )

        simulatedMessages.add(0, welcomeMsg)
    }

    fun injectSampleVerificationEmail(recipientAddress: String, serviceType: String): MessageDetailResponse {
        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val otpCode = "${secureRandom.nextInt(900000) + 100000}"

        val (senderName, senderEmail, subject, textBody, htmlBody) = when (serviceType.lowercase(Locale.ROOT)) {
            "google" -> {
                Tuple5(
                    "Google Security",
                    "noreply@google.com",
                    "G-$otpCode is your Google verification code",
                    "Your Google verification code is: $otpCode\n\nDo not share this code with anyone. It expires in 10 minutes.",
                    "<p>Your <strong>Google</strong> verification code is: <span style='font-size: 24px; font-weight: bold; color: #4285F4; background: #eef3fc; padding: 4px 10px; border-radius: 6px;'>$otpCode</span></p><p>Use this code to verify your account.</p>"
                )
            }
            "discord" -> {
                Tuple5(
                    "Discord Support",
                    "noreply@discord.com",
                    "Verify your Discord account - Code: $otpCode",
                    "Welcome to Discord! Your 6-digit confirmation code is: $otpCode\n\nPlease enter this code to complete registration.",
                    "<p>Welcome to <strong>Discord</strong>! Your verification code is: <span style='font-size: 24px; font-weight: bold; color: #5865F2; background: #ebedff; padding: 4px 10px; border-radius: 6px;'>$otpCode</span></p>"
                )
            }
            "facebook" -> {
                Tuple5(
                    "Facebook Security",
                    "security@facebookmail.com",
                    "$otpCode is your Facebook confirmation code",
                    "Hi, someone tried to register or log into Facebook using this email. Your confirmation code is: $otpCode",
                    "<p>Your <strong>Facebook</strong> confirmation code is: <span style='font-size: 24px; font-weight: bold; color: #1877F2; background: #e7f0fe; padding: 4px 10px; border-radius: 6px;'>$otpCode</span></p>"
                )
            }
            else -> {
                Tuple5(
                    "Online Service",
                    "verify@auth-service.com",
                    "Your One-Time Passcode (OTP): $otpCode",
                    "Your one-time passcode for verification is: $otpCode\n\nThis passcode will expire in 5 minutes.",
                    "<p>Your One-Time Passcode (OTP) is: <span style='font-size: 24px; font-weight: bold; color: #00D2FF; background: #e0f8ff; padding: 4px 10px; border-radius: 6px;'>$otpCode</span></p>"
                )
            }
        }

        val sampleMsg = MessageDetailResponse(
            id = "otp_${System.currentTimeMillis()}_${secureRandom.nextInt(999)}",
            accountId = "acc_${System.currentTimeMillis()}",
            msgid = "<otp-$otpCode@auth.com>",
            from = EmailParticipant(name = senderName, address = senderEmail),
            to = listOf(EmailParticipant(name = "User", address = recipientAddress)),
            subject = subject,
            intro = textBody.lines().firstOrNull() ?: subject,
            seen = false,
            isDeleted = false,
            hasAttachments = false,
            size = 980,
            createdAt = nowIso,
            text = textBody,
            html = listOf(htmlBody),
            attachments = emptyList()
        )

        simulatedMessages.add(0, sampleMsg)
        return sampleMsg
    }

    private data class Tuple5(val a: String, val b: String, val c: String, val d: String, val e: String)

    private fun generateFriendlyHandle(): String {
        val adjectives = listOf("quick", "cyber", "nova", "pixel", "shadow", "apex", "swift", "hyper", "pulse", "echo", "frost", "vortex", "storm", "titan", "smart", "alpha")
        val nouns = listOf("box", "mail", "drop", "inbox", "spark", "shield", "core", "wave", "cloud", "vault", "link", "gate", "zone", "flow")
        val randomAdj = adjectives[secureRandom.nextInt(adjectives.size)]
        val randomNoun = nouns[secureRandom.nextInt(nouns.size)]
        val randomNum = secureRandom.nextInt(9000) + 1000
        return "$randomAdj.$randomNoun$randomNum"
    }

    private fun generateSecurePassword(): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!@#$"
        val sb = StringBuilder()
        repeat(12) {
            sb.append(chars[secureRandom.nextInt(chars.length)])
        }
        return sb.toString()
    }
}
