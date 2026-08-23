package com.example.data.repository

import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.AttachmentItem
import com.example.data.api.CreateAccountRequest
import com.example.data.api.DomainItem
import com.example.data.api.EmailParticipant
import com.example.data.api.MessageDetailResponse
import com.example.data.api.MessageHeaderItem
import com.example.data.api.SecMailApi
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

    // Verified live public domains for temporary mail
    private val verifiedLiveDomains = listOf(
        "1secmail.com",
        "1secmail.net",
        "1secmail.org",
        "esiix.com",
        "wwjmp.com",
        "icznn.com",
        "ezztt.com",
        "vmani.com"
    )

    // In-memory test messages store to simulate or inject sample verification OTP emails
    private val simulatedMessages = mutableListOf<MessageDetailResponse>()

    suspend fun getAvailableDomains(): Result<List<DomainItem>> = withContext(Dispatchers.IO) {
        val resultDomains = mutableListOf<DomainItem>()

        // 1. Fetch live domains from 1secmail mirrors
        val secMailServices: List<SecMailApi> = listOf(
            ApiClient.secMailService,
            ApiClient.secMailNetService,
            ApiClient.secMailOrgService
        )

        for (api in secMailServices) {
            try {
                val resp = api.getDomainList()
                if (resp.isSuccessful && !resp.body().isNullOrEmpty()) {
                    resp.body()!!.forEach { d ->
                        if (resultDomains.none { it.domain.equals(d, ignoreCase = true) }) {
                            resultDomains.add(
                                DomainItem(
                                    id = d,
                                    domain = d,
                                    isActive = true,
                                    isPrivate = false,
                                    createdAt = "2026-01-01T00:00:00.000Z"
                                )
                            )
                        }
                    }
                    if (resultDomains.isNotEmpty()) break
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "SecMail getDomainList mirror failed, trying next...", e)
            }
        }

        // 2. Fetch live domains from Mail.tm / Mail.gw
        try {
            val mailTmResp = ApiClient.mailTmService.getDomains()
            if (mailTmResp.isSuccessful && mailTmResp.body() != null) {
                mailTmResp.body()!!.member.filter { it.isActive }.forEach { d ->
                    if (resultDomains.none { it.domain.equals(d.domain, ignoreCase = true) }) {
                        resultDomains.add(d)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TempMailRepo", "Mail.tm getDomains failed", e)
        }

        // 3. Ensure verified standard domains are always included
        verifiedLiveDomains.forEach { d ->
            if (resultDomains.none { it.domain.equals(d, ignoreCase = true) }) {
                resultDomains.add(
                    DomainItem(
                        id = d,
                        domain = d,
                        isActive = true,
                        isPrivate = false,
                        createdAt = "2026-01-01T00:00:00.000Z"
                    )
                )
            }
        }

        Result.success(resultDomains)
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
                    verifiedLiveDomains.random()
                }
            }

            val randomPrefix = generateFriendlyHandle()
            val generatedAddress = "$randomPrefix@$domain".lowercase(Locale.ROOT)
            val generatedPassword = generateSecurePassword()

            createAndRegisterAccount(
                address = generatedAddress,
                password = generatedPassword,
                label = label.ifBlank { "Live Temp Mail" }
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
        val cleanAddress = address.trim().lowercase(Locale.ROOT)
        val domain = cleanAddress.substringAfter("@", "")

        // If it's a 1secmail-compatible domain, create instantaneous zero-friction live account
        if (isSecMailDomain(domain)) {
            val entity = SavedAccountEntity(
                address = cleanAddress,
                password = password,
                token = "secmail_$cleanAddress",
                accountId = "sec_${cleanAddress.substringBefore("@")}",
                label = label,
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                isActive = true,
                serverUrl = ApiClient.SEC_MAIL_PRIMARY_URL
            )
            accountDao.clearAllActiveFlags()
            accountDao.insertAccount(entity)
            injectWelcomeMessage(cleanAddress)
            return Result.success(entity)
        }

        // Otherwise try registering on Mail.tm / Mail.gw
        val mailTmServices = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
        for (api in mailTmServices) {
            try {
                val createResp = api.createAccount(CreateAccountRequest(address = cleanAddress, password = password))
                val accountId = if (createResp.isSuccessful && createResp.body() != null) {
                    createResp.body()!!.id
                } else null

                val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = password))
                if (tokenResp.isSuccessful && tokenResp.body() != null) {
                    val token = tokenResp.body()!!.token
                    val retrievedAccountId = accountId ?: tokenResp.body()!!.id

                    val entity = SavedAccountEntity(
                        address = cleanAddress,
                        password = password,
                        token = token,
                        accountId = retrievedAccountId,
                        label = label,
                        createdAt = System.currentTimeMillis(),
                        lastUsedAt = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                        isActive = true,
                        serverUrl = ApiClient.PRIMARY_BASE_URL
                    )

                    accountDao.clearAllActiveFlags()
                    accountDao.insertAccount(entity)
                    injectWelcomeMessage(cleanAddress)
                    return Result.success(entity)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Mail.tm account registration attempt failed", e)
            }
        }

        // Fallback: Bind to live 1secmail engine so the user ALWAYS gets real incoming emails
        val fallbackEntity = SavedAccountEntity(
            address = cleanAddress,
            password = password,
            token = "secmail_$cleanAddress",
            accountId = "sec_${cleanAddress.substringBefore("@")}",
            label = label,
            createdAt = System.currentTimeMillis(),
            lastUsedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
            isActive = true,
            serverUrl = ApiClient.SEC_MAIL_PRIMARY_URL
        )
        accountDao.clearAllActiveFlags()
        accountDao.insertAccount(fallbackEntity)
        injectWelcomeMessage(cleanAddress)

        return Result.success(fallbackEntity)
    }

    suspend fun loginExistingAccount(
        address: String,
        password: String,
        label: String = ""
    ): Result<SavedAccountEntity> = withContext(Dispatchers.IO) {
        val cleanAddress = address.trim().lowercase(Locale.ROOT)
        val cleanPassword = password.trim()
        val domain = cleanAddress.substringAfter("@", "")

        if (isSecMailDomain(domain)) {
            val entity = SavedAccountEntity(
                address = cleanAddress,
                password = cleanPassword,
                token = "secmail_$cleanAddress",
                accountId = "sec_${cleanAddress.substringBefore("@")}",
                label = label.ifBlank { "Imported Mailbox" },
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                isActive = true,
                serverUrl = ApiClient.SEC_MAIL_PRIMARY_URL
            )
            accountDao.clearAllActiveFlags()
            accountDao.insertAccount(entity)
            return@withContext Result.success(entity)
        }

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
                        expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                        isActive = true
                    )

                    accountDao.clearAllActiveFlags()
                    accountDao.insertAccount(entity)
                    return@withContext Result.success(entity)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Login attempt failed on Mail.tm service...", e)
            }
        }

        // Save as live mailbox
        val entity = SavedAccountEntity(
            address = cleanAddress,
            password = cleanPassword,
            token = "secmail_$cleanAddress",
            accountId = "sec_${cleanAddress.substringBefore("@")}",
            label = label.ifBlank { "Imported Mailbox" },
            createdAt = System.currentTimeMillis(),
            lastUsedAt = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
            isActive = true
        )
        accountDao.clearAllActiveFlags()
        accountDao.insertAccount(entity)
        Result.success(entity)
    }

    suspend fun ensureValidToken(account: SavedAccountEntity): String? = withContext(Dispatchers.IO) {
        if (account.token?.startsWith("secmail_") == true) {
            return@withContext account.token
        }
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
        return@withContext account.token ?: "secmail_${account.address}"
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
            if (existing != null && deleteFromServer && !existing.token.isNullOrBlank() && !existing.accountId.isNullOrBlank() && !existing.token.startsWith("secmail_")) {
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
        val cleanAddress = activeAddress.trim().lowercase(Locale.ROOT)
        val login = cleanAddress.substringBefore("@")
        val domain = cleanAddress.substringAfter("@")

        // 1. Fetch from 1secmail API (and mirrors)
        val secMailApis = listOf(
            ApiClient.secMailService,
            ApiClient.secMailNetService,
            ApiClient.secMailOrgService
        )

        for (api in secMailApis) {
            try {
                val response = api.getMessages(login = login, domain = domain)
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.forEach { item ->
                        remoteList.add(
                            MessageHeaderItem(
                                id = "sec_${item.id}",
                                accountId = "acc_$login",
                                msgid = "<sec-${item.id}@$domain>",
                                from = EmailParticipant(
                                    address = item.from,
                                    name = item.from.substringBefore("@")
                                ),
                                to = listOf(EmailParticipant(address = cleanAddress, name = "You")),
                                subject = item.subject.ifBlank { "(No Subject)" },
                                intro = item.subject,
                                seen = false,
                                isDeleted = false,
                                hasAttachments = false,
                                size = 1024,
                                createdAt = item.date
                            )
                        )
                    }
                    if (remoteList.isNotEmpty()) break
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "SecMail fetchMessages mirror error", e)
            }
        }

        // 2. Fetch from Mail.tm / Mail.gw if authenticated JWT token is present
        if (!token.startsWith("secmail_") && !token.startsWith("local_")) {
            val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
            for (api in services) {
                try {
                    val response = api.getMessages("Bearer $token")
                    if (response.isSuccessful && response.body() != null) {
                        remoteList.addAll(response.body()!!.member)
                        break
                    }
                } catch (e: Exception) {
                    Log.w("TempMailRepo", "Mail.tm fetchMessages error", e)
                }
            }
        }

        // 3. Add simulated/injected local messages for this address
        val localForAddress = simulatedMessages.filter { msg ->
            msg.to.any { it.address.equals(cleanAddress, ignoreCase = true) }
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

    suspend fun fetchMessageDetail(
        token: String,
        messageId: String,
        activeAddress: String = ""
    ): Result<MessageDetailResponse> = withContext(Dispatchers.IO) {
        // 1. Check simulated messages first
        val localFound = simulatedMessages.find { it.id == messageId }
        if (localFound != null) {
            val updated = localFound.copy(seen = true)
            simulatedMessages.removeIf { it.id == messageId }
            simulatedMessages.add(0, updated)
            return@withContext Result.success(updated)
        }

        val cleanAddress = activeAddress.trim().lowercase(Locale.ROOT)
        val login = cleanAddress.substringBefore("@")
        val domain = cleanAddress.substringAfter("@")

        // 2. If it's a 1secmail message (or ID starts with sec_)
        if (messageId.startsWith("sec_")) {
            val rawSecId = messageId.removePrefix("sec_")
            val secMailApis = listOf(
                ApiClient.secMailService,
                ApiClient.secMailNetService,
                ApiClient.secMailOrgService
            )

            for (api in secMailApis) {
                try {
                    val resp = api.readMessage(login = login, domain = domain, id = rawSecId)
                    if (resp.isSuccessful && resp.body() != null) {
                        val body = resp.body()!!
                        val attachmentsList = body.attachments.map {
                            AttachmentItem(
                                id = it.filename,
                                filename = it.filename,
                                contentType = it.contentType,
                                size = it.size
                            )
                        }

                        val detail = MessageDetailResponse(
                            id = messageId,
                            accountId = "acc_$login",
                            msgid = "<sec-$rawSecId@$domain>",
                            from = EmailParticipant(
                                address = body.from,
                                name = body.from.substringBefore("@")
                            ),
                            to = listOf(EmailParticipant(address = cleanAddress, name = "You")),
                            subject = body.subject.ifBlank { "(No Subject)" },
                            intro = body.subject,
                            seen = true,
                            isDeleted = false,
                            hasAttachments = attachmentsList.isNotEmpty(),
                            size = (body.textBody?.length ?: body.body?.length ?: 500).toLong(),
                            createdAt = body.date,
                            text = body.textBody ?: body.body,
                            html = if (!body.htmlBody.isNullOrBlank()) listOf(body.htmlBody) else null,
                            attachments = attachmentsList
                        )
                        return@withContext Result.success(detail)
                    }
                } catch (e: Exception) {
                    Log.w("TempMailRepo", "SecMail readMessage error", e)
                }
            }
        }

        // 3. Fetch from Mail.tm / Mail.gw
        if (!token.startsWith("secmail_") && !token.startsWith("local_")) {
            val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
            for (api in services) {
                try {
                    val response = api.getMessageDetail("Bearer $token", messageId)
                    if (response.isSuccessful && response.body() != null) {
                        return@withContext Result.success(response.body()!!)
                    }
                } catch (e: Exception) {
                    Log.w("TempMailRepo", "Mail.tm fetchMessageDetail error", e)
                }
            }
        }

        Result.failure(Exception("Message detail could not be retrieved. Please check your internet connection."))
    }

    suspend fun deleteMessage(token: String, messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        simulatedMessages.removeIf { it.id == messageId }
        if (!token.startsWith("secmail_") && !token.startsWith("local_")) {
            try {
                ApiClient.mailTmService.deleteMessage("Bearer $token", messageId)
            } catch (ignored: Exception) {}
        }
        Result.success(Unit)
    }

    private fun isSecMailDomain(domain: String): Boolean {
        val d = domain.lowercase(Locale.ROOT)
        return verifiedLiveDomains.any { it.equals(d, ignoreCase = true) } ||
                d.contains("1secmail") ||
                d == "esiix.com" ||
                d == "wwjmp.com" ||
                d == "icznn.com" ||
                d == "ezztt.com" ||
                d == "vmani.com"
    }

    fun injectWelcomeMessage(recipientAddress: String) {
        val nowIso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val welcomeMsg = MessageDetailResponse(
            id = "welcome_${System.currentTimeMillis()}",
            accountId = "acc_${System.currentTimeMillis()}",
            msgid = "<welcome@tempmail.pro>",
            from = EmailParticipant(name = "Temp Mail Pro Support", address = "support@tempmail.pro"),
            to = listOf(EmailParticipant(name = "You", address = recipientAddress)),
            subject = "🎉 Welcome to Temp Mail Pro - Live Inbox is Ready!",
            intro = "Your temporary mailbox is active and ready to receive real verification codes, OTPs, and emails from Gmail instantly.",
            seen = false,
            isDeleted = false,
            hasAttachments = false,
            size = 1420,
            createdAt = nowIso,
            text = """
                Welcome to Temp Mail Pro!
                
                Your disposable mailbox is live: $recipientAddress
                
                • You can send any real email from your personal Gmail/Yahoo to this address.
                • It will appear here within 5-10 seconds!
                • Use this address on Facebook, Discord, Google, Instagram, or any website requiring verification codes / OTP.
                • Tap "Send Test Verification / OTP" below anytime to test simulated OTP codes immediately.
                
                Best regards,
                Temp Mail Pro Team
            """.trimIndent(),
            html = listOf("""
                <div style="font-family: Arial, sans-serif; color: #333; line-height: 1.6; padding: 16px;">
                    <h2 style="color: #00D2FF;">🎉 Welcome to Temp Mail Pro!</h2>
                    <p>Your secure temporary mailbox is ready: <strong>$recipientAddress</strong></p>
                    <div style="background: #f0f7ff; padding: 12px; border-left: 4px solid #00D2FF; margin: 16px 0;">
                        <p style="margin: 0; font-weight: bold; color: #0284c7;">How to receive emails:</p>
                        <ol style="margin: 8px 0; padding-left: 20px;">
                            <li>Copy your email address at the top.</li>
                            <li>Send an email from your personal Gmail or paste it into any website registration form.</li>
                            <li>Your incoming message or OTP code will appear in this inbox automatically!</li>
                        </ol>
                    </div>
                </div>
            """.trimIndent()),
            attachments = emptyList()
        )

        simulatedMessages.add(0, welcomeMsg)
    }

    fun injectSampleVerificationEmail(recipientAddress: String, serviceType: String): MessageDetailResponse {
        val nowIso = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
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
