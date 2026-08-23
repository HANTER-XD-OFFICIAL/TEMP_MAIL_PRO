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

    // Backup domains in case network is completely blocked
    private val fallbackDomains = listOf(
        "emalupe.com",
        "txcct.com",
        "omdiya.com",
        "guerrillamail.com",
        "sharklasers.com",
        "1secmail.com",
        "1secmail.net",
        "1secmail.org"
    )

    suspend fun getAvailableDomains(): Result<List<DomainItem>> = withContext(Dispatchers.IO) {
        val resultDomains = mutableListOf<DomainItem>()

        // 1. Fetch live domains directly from Mail.tm API (Primary)
        val mailServices = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
        for (api in mailServices) {
            try {
                val resp = api.getDomains()
                if (resp.isSuccessful && resp.body() != null) {
                    val activeList = resp.body()!!.member.filter { it.isActive }
                    activeList.forEach { d ->
                        if (resultDomains.none { it.domain.equals(d.domain, ignoreCase = true) }) {
                            resultDomains.add(d)
                        }
                    }
                    if (resultDomains.isNotEmpty()) break
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Mail.tm getDomains error", e)
            }
        }

        // 2. Fetch from Guerrilla / 1SecMail as additional mirrors
        if (resultDomains.isEmpty()) {
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
                    Log.w("TempMailRepo", "SecMail getDomainList error", e)
                }
            }
        }

        // 3. Guarantee known fallback domains if offline
        fallbackDomains.forEach { d ->
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
            // Determine domain
            val domain = if (!customDomain.isNullOrBlank()) {
                customDomain.trim().lowercase(Locale.ROOT)
            } else {
                val domainsResult = getAvailableDomains()
                val list = domainsResult.getOrNull()
                if (!list.isNullOrEmpty()) {
                    list.random().domain
                } else {
                    "emalupe.com"
                }
            }

            val randomPrefix = generateFriendlyHandle()
            val generatedAddress = "$randomPrefix@$domain".lowercase(Locale.ROOT)
            val generatedPassword = generateSecurePassword()

            createAndRegisterAccount(
                address = generatedAddress,
                password = generatedPassword,
                label = label.ifBlank { "Mail.tm Live Mailbox" }
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

        val fullAddress = "$cleanUsername@${domain.trim().lowercase(Locale.ROOT)}"
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

        // 1. Primary: Register and authenticate on Mail.tm / Mail.gw API
        val mailTmServices = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
        for (api in mailTmServices) {
            try {
                // Register account
                val createResp = api.createAccount(CreateAccountRequest(address = cleanAddress, password = password))
                val accountId = if (createResp.isSuccessful && createResp.body() != null) {
                    createResp.body()!!.id
                } else null

                // Obtain JWT Auth Token
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
                    return Result.success(entity)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Mail.tm account registration attempt failed", e)
            }
        }

        // 2. Secondary: If it's a GuerrillaMail domain, register with Guerrilla
        if (isGuerrillaDomain(domain)) {
            try {
                val userPrefix = cleanAddress.substringBefore("@")
                val initResp = ApiClient.guerrillaMailService.getEmailAddress()
                val sidToken = if (initResp.isSuccessful && initResp.body() != null) initResp.body()!!.sidToken else null
                val setResp = ApiClient.guerrillaMailService.setEmailUser(emailUser = userPrefix, site = domain, sidToken = sidToken)
                val activeSid = if (setResp.isSuccessful && setResp.body() != null) setResp.body()!!.sidToken else (sidToken ?: "")

                val entity = SavedAccountEntity(
                    address = cleanAddress,
                    password = password,
                    token = "grr_$activeSid",
                    accountId = "grr_$userPrefix",
                    label = label,
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                    isActive = true,
                    serverUrl = ApiClient.GUERRILLA_MAIL_URL
                )
                accountDao.clearAllActiveFlags()
                accountDao.insertAccount(entity)
                return Result.success(entity)
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Guerrilla registration failed", e)
            }
        }

        // 3. Fallback: If it's a 1secmail-compatible domain
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

        // Try Mail.tm login
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
                        label = if (label.isNotBlank()) label else existing.label.ifBlank { "Mail.tm Account" }
                    )) ?: SavedAccountEntity(
                        address = cleanAddress,
                        password = cleanPassword,
                        token = token,
                        accountId = accountId,
                        label = label.ifBlank { "Mail.tm Account" },
                        createdAt = System.currentTimeMillis(),
                        lastUsedAt = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                        isActive = true,
                        serverUrl = ApiClient.PRIMARY_BASE_URL
                    )

                    accountDao.clearAllActiveFlags()
                    accountDao.insertAccount(entity)
                    return@withContext Result.success(entity)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Login attempt failed on Mail.tm service...", e)
            }
        }

        // Guerrilla check
        if (isGuerrillaDomain(domain)) {
            val userPrefix = cleanAddress.substringBefore("@")
            try {
                val initResp = ApiClient.guerrillaMailService.getEmailAddress()
                val sidToken = if (initResp.isSuccessful && initResp.body() != null) initResp.body()!!.sidToken else null
                val setResp = ApiClient.guerrillaMailService.setEmailUser(emailUser = userPrefix, site = domain, sidToken = sidToken)
                val activeSid = if (setResp.isSuccessful && setResp.body() != null) setResp.body()!!.sidToken else (sidToken ?: "")

                val entity = SavedAccountEntity(
                    address = cleanAddress,
                    password = cleanPassword,
                    token = "grr_$activeSid",
                    accountId = "grr_$userPrefix",
                    label = label.ifBlank { "Imported Mailbox" },
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                    isActive = true,
                    serverUrl = ApiClient.GUERRILLA_MAIL_URL
                )
                accountDao.clearAllActiveFlags()
                accountDao.insertAccount(entity)
                return@withContext Result.success(entity)
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Login guerrilla error", e)
            }
        }

        // Save fallback
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
        val cleanAddress = account.address.trim().lowercase(Locale.ROOT)
        val domain = cleanAddress.substringAfter("@", "")

        if (isGuerrillaDomain(domain) && account.token?.startsWith("grr_") == true) {
            return@withContext account.token
        }
        if (isSecMailDomain(domain) && account.token?.startsWith("secmail_") == true) {
            return@withContext account.token
        }

        // Always check / refresh token with Mail.tm for Mail.tm accounts (or any general account)
        val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
        for (api in services) {
            try {
                // Check if existing token still works
                if (!account.token.isNullOrBlank() && !account.token.startsWith("secmail_") && !account.token.startsWith("grr_")) {
                    val meResp = api.getMe("Bearer ${account.token}")
                    if (meResp.isSuccessful) {
                        return@withContext account.token
                    }
                }

                // If not, fetch a fresh JWT token with password
                val tokenResp = api.getToken(TokenRequest(address = account.address, password = account.password))
                if (tokenResp.isSuccessful && tokenResp.body() != null) {
                    val newToken = tokenResp.body()!!.token
                    val newId = tokenResp.body()!!.id
                    accountDao.updateAccount(
                        account.copy(
                            token = newToken,
                            accountId = newId,
                            lastUsedAt = System.currentTimeMillis()
                        )
                    )
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
            if (existing != null && deleteFromServer && !existing.token.isNullOrBlank() && !existing.accountId.isNullOrBlank() && !existing.token.startsWith("secmail_") && !existing.token.startsWith("grr_")) {
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

        // 1. PRIMARY: Fetch from Mail.tm (https://api.mail.tm/messages)
        val mailTmServices = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
        var authToken = token
        for (api in mailTmServices) {
            try {
                // If token is secmail or grr or invalid, try to get real token first
                if (authToken.startsWith("secmail_") || authToken.startsWith("grr_") || authToken.isBlank()) {
                    val existingAccount = accountDao.getAccountByAddress(cleanAddress)
                    if (existingAccount != null) {
                        val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = existingAccount.password))
                        if (tokenResp.isSuccessful && tokenResp.body() != null) {
                            authToken = tokenResp.body()!!.token
                            accountDao.updateAccount(existingAccount.copy(token = authToken))
                        }
                    }
                }

                if (!authToken.startsWith("secmail_") && !authToken.startsWith("grr_") && authToken.isNotBlank()) {
                    val response = api.getMessages("Bearer $authToken")
                    if (response.isSuccessful && response.body() != null) {
                        remoteList.addAll(response.body()!!.member)
                        break
                    } else if (response.code() == 401) {
                        // Token expired: Refresh token and retry
                        val existingAccount = accountDao.getAccountByAddress(cleanAddress)
                        if (existingAccount != null) {
                            val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = existingAccount.password))
                            if (tokenResp.isSuccessful && tokenResp.body() != null) {
                                authToken = tokenResp.body()!!.token
                                accountDao.updateAccount(existingAccount.copy(token = authToken))
                                val retryResp = api.getMessages("Bearer $authToken")
                                if (retryResp.isSuccessful && retryResp.body() != null) {
                                    remoteList.addAll(retryResp.body()!!.member)
                                    break
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Mail.tm fetchMessages error", e)
            }
        }

        // 2. If GuerrillaMail account
        if (remoteList.isEmpty() && (authToken.startsWith("grr_") || isGuerrillaDomain(domain))) {
            val sidToken = authToken.removePrefix("grr_").ifBlank { null }
            try {
                val checkResp = ApiClient.guerrillaMailService.checkEmail(seq = 0, sidToken = sidToken)
                if (checkResp.isSuccessful && checkResp.body() != null) {
                    val body = checkResp.body()!!
                    body.list.forEach { item ->
                        remoteList.add(
                            MessageHeaderItem(
                                id = "grr_${item.mailId}",
                                accountId = "grr_$login",
                                msgid = "<grr-${item.mailId}@$domain>",
                                from = EmailParticipant(
                                    address = item.mailFrom,
                                    name = item.mailFrom.substringBefore("@")
                                ),
                                to = listOf(EmailParticipant(address = cleanAddress, name = "You")),
                                subject = item.mailSubject.ifBlank { "(No Subject)" },
                                intro = item.mailExcerpt.ifBlank { item.mailSubject },
                                seen = item.mailRead == "1",
                                isDeleted = false,
                                hasAttachments = item.att != "0",
                                size = item.mailSize.toLongOrNull() ?: 1024L,
                                createdAt = item.mailDate.ifBlank { item.mailTimestamp }
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "GuerrillaMail checkEmail error", e)
            }
        }

        // 3. If 1secmail API mirrors
        if (remoteList.isEmpty() && (isSecMailDomain(domain) || authToken.startsWith("secmail_"))) {
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
                    Log.w("TempMailRepo", "SecMail fetchMessages error", e)
                }
            }
        }

        val combined = remoteList.distinctBy { it.id }.sortedByDescending { it.createdAt }
        Result.success(combined)
    }

    suspend fun fetchMessageDetail(
        token: String,
        messageId: String,
        activeAddress: String = ""
    ): Result<MessageDetailResponse> = withContext(Dispatchers.IO) {
        val cleanAddress = activeAddress.trim().lowercase(Locale.ROOT)
        val login = cleanAddress.substringBefore("@")
        val domain = cleanAddress.substringAfter("@")

        // 1. If GuerrillaMail message (id starts with grr_)
        if (messageId.startsWith("grr_")) {
            val rawId = messageId.removePrefix("grr_")
            val sidToken = token.removePrefix("grr_").ifBlank { null }
            try {
                val fetchResp = ApiClient.guerrillaMailService.fetchEmail(emailId = rawId, sidToken = sidToken)
                if (fetchResp.isSuccessful && fetchResp.body() != null) {
                    val b = fetchResp.body()!!
                    val rawBody = b.mailBody
                    val isHtml = rawBody.contains("<div") || rawBody.contains("<p") || rawBody.contains("<html") || rawBody.contains("<br") || rawBody.contains("<table")

                    val detail = MessageDetailResponse(
                        id = messageId,
                        accountId = "grr_$login",
                        msgid = "<grr-$rawId@$domain>",
                        from = EmailParticipant(
                            address = b.mailFrom,
                            name = b.mailFrom.substringBefore("@")
                        ),
                        to = listOf(EmailParticipant(address = b.mailRecipient.ifBlank { cleanAddress }, name = "You")),
                        subject = b.mailSubject.ifBlank { "(No Subject)" },
                        intro = b.mailExcerpt.ifBlank { b.mailSubject },
                        seen = true,
                        isDeleted = false,
                        hasAttachments = false,
                        size = rawBody.length.toLong(),
                        createdAt = b.mailDate.ifBlank { b.mailTimestamp },
                        text = if (isHtml) null else rawBody,
                        html = if (isHtml) listOf(rawBody) else null,
                        attachments = emptyList()
                    )
                    return@withContext Result.success(detail)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "GuerrillaMail fetchEmail error", e)
            }
        }

        // 2. If 1secmail message (id starts with sec_)
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

        // 3. Primary: Fetch from Mail.tm / Mail.gw API
        val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
        var authToken = token
        for (api in services) {
            try {
                if (!authToken.startsWith("secmail_") && !authToken.startsWith("grr_") && authToken.isNotBlank()) {
                    val response = api.getMessageDetail("Bearer $authToken", messageId)
                    if (response.isSuccessful && response.body() != null) {
                        return@withContext Result.success(response.body()!!)
                    } else if (response.code() == 401) {
                        // Refresh token
                        val existingAccount = accountDao.getAccountByAddress(cleanAddress)
                        if (existingAccount != null) {
                            val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = existingAccount.password))
                            if (tokenResp.isSuccessful && tokenResp.body() != null) {
                                authToken = tokenResp.body()!!.token
                                accountDao.updateAccount(existingAccount.copy(token = authToken))
                                val retryResp = api.getMessageDetail("Bearer $authToken", messageId)
                                if (retryResp.isSuccessful && retryResp.body() != null) {
                                    return@withContext Result.success(retryResp.body()!!)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Mail.tm fetchMessageDetail error", e)
            }
        }

        Result.failure(Exception("Message detail could not be retrieved. Please check your internet connection."))
    }

    suspend fun deleteMessage(token: String, messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!token.startsWith("secmail_") && !token.startsWith("grr_")) {
            try {
                ApiClient.mailTmService.deleteMessage("Bearer $token", messageId)
            } catch (ignored: Exception) {}
        }
        Result.success(Unit)
    }

    private fun isGuerrillaDomain(domain: String): Boolean {
        val d = domain.lowercase(Locale.ROOT)
        return d.contains("guerrillamail") || d == "grr.la" || d == "sharklasers.com" || d == "pokemail.net" || d == "spam4.me"
    }

    private fun isSecMailDomain(domain: String): Boolean {
        val d = domain.lowercase(Locale.ROOT)
        return d.contains("1secmail") ||
                d == "esiix.com" ||
                d == "wwjmp.com" ||
                d == "icznn.com" ||
                d == "ezztt.com" ||
                d == "vmani.com"
    }

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
