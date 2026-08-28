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

    // Available domains from Mail.tm and GuerrillaMail APIs
    private val allSupportedDomains = listOf(
        "emalupe.com",
        "westcast-systems.com",
        "sharklasers.com",
        "guerrillamail.com",
        "grr.la",
        "pokemail.net",
        "spam4.me"
    )

    suspend fun getAvailableDomains(): Result<List<DomainItem>> = withContext(Dispatchers.IO) {
        val resultDomains = mutableListOf<DomainItem>()

        // 1. Fetch live domains directly from Mail.tm / Mail.gw APIs
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
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Mail.tm getDomains error", e)
            }
        }

        // Guarantee emalupe.com and westcast-systems.com
        val guaranteedTmDomains = listOf("emalupe.com", "westcast-systems.com")
        guaranteedTmDomains.forEach { tmDomain ->
            if (resultDomains.none { it.domain.equals(tmDomain, ignoreCase = true) }) {
                resultDomains.add(
                    DomainItem(
                        id = tmDomain,
                        domain = tmDomain,
                        isActive = true,
                        isPrivate = false,
                        createdAt = "2026-01-01T00:00:00.000Z"
                    )
                )
            }
        }

        // Add GuerrillaMail domains with high reliability
        val guerrillaDomains = listOf(
            "sharklasers.com",
            "guerrillamail.com",
            "grr.la",
            "pokemail.net",
            "spam4.me"
        )
        guerrillaDomains.forEach { gDomain ->
            if (resultDomains.none { it.domain.equals(gDomain, ignoreCase = true) }) {
                resultDomains.add(
                    DomainItem(
                        id = gDomain,
                        domain = gDomain,
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
            val domain = customDomain?.lowercase(Locale.ROOT)?.trim() ?: "emalupe.com"

            // Direct GuerrillaMail API generator if Guerrilla domain selected
            if (isGuerrillaDomain(domain)) {
                try {
                    val initResp = ApiClient.guerrillaMailService.getEmailAddress()
                    if (initResp.isSuccessful && initResp.body() != null) {
                        val addrResp = initResp.body()!!
                        val sidToken = addrResp.sidToken
                        val userPrefix = generateFriendlyHandle()
                        val site = guerrillaSite(domain)

                        val setResp = ApiClient.guerrillaMailService.setEmailUser(
                            emailUser = userPrefix,
                            site = site,
                            sidToken = sidToken
                        )
                        val activeSid = if (setResp.isSuccessful && setResp.body() != null) {
                            setResp.body()!!.sidToken.ifBlank { sidToken }
                        } else sidToken

                        val finalAddress = "$userPrefix@$domain"

                        val entity = SavedAccountEntity(
                            address = finalAddress,
                            password = generateSecurePassword(),
                            token = "grr_$activeSid",
                            accountId = "grr_$userPrefix",
                            label = label.ifBlank { "Guerrilla Instant Mailbox" },
                            createdAt = System.currentTimeMillis(),
                            lastUsedAt = System.currentTimeMillis(),
                            expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                            isActive = true,
                            serverUrl = ApiClient.GUERRILLA_MAIL_URL
                        )
                        accountDao.clearAllActiveFlags()
                        accountDao.insertAccount(entity)
                        return@withContext Result.success(entity)
                    }
                } catch (e: Exception) {
                    Log.w("TempMailRepo", "Guerrilla random generation attempt failed", e)
                }
            }

            val randomPrefix = generateFriendlyHandle()
            val generatedAddress = "$randomPrefix@$domain".lowercase(Locale.ROOT)
            val generatedPassword = generateSecurePassword()

            createAndRegisterAccount(
                address = generatedAddress,
                password = generatedPassword,
                label = label.ifBlank { "Live Mailbox" }
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
        val cleanDomain = domain.trim().lowercase(Locale.ROOT)
        val cleanUsername = username.trim().lowercase(Locale.ROOT)
            .replace("[^a-z0-9._-]".toRegex(), "")
            .trim('.', '-', '_')

        if (cleanUsername.length < 3) {
            return@withContext Result.failure(
                IllegalArgumentException("Username must have at least 3 characters (letters, numbers, dot, dash, underscore)")
            )
        }
        var cleanPassword = password.trim()
        if (cleanPassword.length < 6) {
            cleanPassword += generateSecurePassword().take(8)
        }

        val fullAddress = "$cleanUsername@$cleanDomain"

        // 1. If GuerrillaMail domain:
        if (isGuerrillaDomain(cleanDomain)) {
            try {
                val initResp = ApiClient.guerrillaMailService.getEmailAddress()
                val sidToken = if (initResp.isSuccessful && initResp.body() != null) initResp.body()!!.sidToken else null
                val site = guerrillaSite(cleanDomain)
                val setResp = ApiClient.guerrillaMailService.setEmailUser(
                    emailUser = cleanUsername,
                    site = site,
                    sidToken = sidToken
                )
                val activeSid = if (setResp.isSuccessful && setResp.body() != null) {
                    setResp.body()!!.sidToken.ifBlank { sidToken ?: "" }
                } else (sidToken ?: "")

                val entity = SavedAccountEntity(
                    address = fullAddress,
                    password = cleanPassword,
                    token = "grr_$activeSid",
                    accountId = "grr_$cleanUsername",
                    label = label.ifBlank { "Custom Mailbox" },
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
                Log.w("TempMailRepo", "Guerrilla custom registration failed", e)
                return@withContext Result.failure(Exception("Failed to initialize mailbox on Guerrilla Mail: ${e.message}"))
            }
        }

        // 2. Mail.tm / Mail.gw domain:
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
        val cleanUsername = cleanAddress.substringBefore("@")

        val mailTmServices = listOf(
            Pair(ApiClient.mailTmService, ApiClient.PRIMARY_BASE_URL),
            Pair(ApiClient.mailGwService, ApiClient.SECONDARY_BASE_URL)
        )

        var lastErrorMessage = ""

        for ((api, serverUrl) in mailTmServices) {
            try {
                // Register account on Mail.tm / Mail.gw
                val createResp = api.createAccount(CreateAccountRequest(address = cleanAddress, password = password))
                val accountId = if (createResp.isSuccessful && createResp.body() != null) {
                    createResp.body()!!.id
                } else null

                // If registration failed because address is already used (422), try logging in with password
                if (!createResp.isSuccessful && createResp.code() == 422) {
                    val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = password))
                    if (tokenResp.isSuccessful && tokenResp.body() != null) {
                        val token = tokenResp.body()!!.token
                        val retrievedAccountId = tokenResp.body()!!.id
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
                            serverUrl = serverUrl
                        )
                        accountDao.clearAllActiveFlags()
                        accountDao.insertAccount(entity)
                        return Result.success(entity)
                    } else {
                        // Username taken by someone else
                        return Result.failure(
                            Exception("Username '$cleanUsername' is already taken on @$domain. Please choose a different username.")
                        )
                    }
                }

                if (createResp.code() == 429) {
                    return Result.failure(Exception("Mail service rate limit reached. Please wait 10 seconds and try again."))
                }

                // If account created, get JWT token
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
                        serverUrl = serverUrl
                    )

                    accountDao.clearAllActiveFlags()
                    accountDao.insertAccount(entity)
                    return Result.success(entity)
                } else {
                    lastErrorMessage = "Failed to authenticate mailbox: HTTP ${tokenResp.code()}"
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Mail.tm account registration attempt failed", e)
                lastErrorMessage = e.localizedMessage ?: "Network error"
            }
        }

        return Result.failure(
            Exception(lastErrorMessage.ifBlank { "Could not create mailbox on @$domain. Please try another domain or username." })
        )
    }

    suspend fun loginExistingAccount(
        address: String,
        password: String,
        label: String = ""
    ): Result<SavedAccountEntity> = withContext(Dispatchers.IO) {
        val cleanAddress = address.trim().lowercase(Locale.ROOT)
        val cleanPassword = password.trim()
        val domain = cleanAddress.substringAfter("@", "")
        val userPrefix = cleanAddress.substringBefore("@")

        // 1. Guerrilla domain
        if (isGuerrillaDomain(domain)) {
            try {
                val initResp = ApiClient.guerrillaMailService.getEmailAddress()
                val sidToken = if (initResp.isSuccessful && initResp.body() != null) initResp.body()!!.sidToken else null
                val setResp = ApiClient.guerrillaMailService.setEmailUser(
                    emailUser = userPrefix,
                    site = guerrillaSite(domain),
                    sidToken = sidToken
                )
                val activeSid = if (setResp.isSuccessful && setResp.body() != null) {
                    setResp.body()!!.sidToken.ifBlank { sidToken ?: "" }
                } else (sidToken ?: "")

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
                return@withContext Result.failure(Exception("Failed to access Guerrilla mailbox: ${e.message}"))
            }
        }

        // 2. Mail.tm / Mail.gw
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

        Result.failure(Exception("Invalid email address or password. Please verify credentials and try again."))
    }

    suspend fun ensureValidToken(account: SavedAccountEntity): String? = withContext(Dispatchers.IO) {
        val cleanAddress = account.address.trim().lowercase(Locale.ROOT)
        val domain = cleanAddress.substringAfter("@", "")
        val userPrefix = cleanAddress.substringBefore("@")

        // 1. Guerrilla domain: ensure session is initialized and bound to user handle
        if (isGuerrillaDomain(domain) || account.token?.startsWith("grr_") == true) {
            var sidToken = account.token?.removePrefix("grr_")?.trim()?.ifBlank { null }
            try {
                if (sidToken.isNullOrBlank()) {
                    val initResp = ApiClient.guerrillaMailService.getEmailAddress()
                    if (initResp.isSuccessful && initResp.body() != null) {
                        sidToken = initResp.body()!!.sidToken.ifBlank { null }
                    }
                }
                val setResp = ApiClient.guerrillaMailService.setEmailUser(
                    emailUser = userPrefix,
                    site = guerrillaSite(domain),
                    sidToken = sidToken
                )
                val activeSid = if (setResp.isSuccessful && setResp.body() != null) {
                    setResp.body()!!.sidToken.ifBlank { sidToken }
                } else sidToken

                if (!activeSid.isNullOrBlank()) {
                    val fullToken = "grr_$activeSid"
                    if (fullToken != account.token) {
                        accountDao.updateAccount(account.copy(token = fullToken))
                    }
                    return@withContext fullToken
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "ensureValidToken guerrilla error", e)
            }
            return@withContext account.token ?: "grr_"
        }

        // 2. Mail.tm / Mail.gw: check token health or re-authenticate / auto-register
        val services = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
        for (api in services) {
            try {
                if (!account.token.isNullOrBlank() && !account.token.startsWith("secmail_") && !account.token.startsWith("grr_")) {
                    val meResp = api.getMe("Bearer ${account.token}")
                    if (meResp.isSuccessful) {
                        return@withContext account.token
                    }
                }

                if (account.password.isNotBlank()) {
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
                    } else {
                        // Auto-register and get token if legacy un-registered account
                        val createResp = api.createAccount(CreateAccountRequest(address = account.address, password = account.password))
                        if (createResp.isSuccessful || createResp.code() == 422) {
                            val retryTokenResp = api.getToken(TokenRequest(address = account.address, password = account.password))
                            if (retryTokenResp.isSuccessful && retryTokenResp.body() != null) {
                                val newToken = retryTokenResp.body()!!.token
                                val newId = createResp.body()?.id ?: retryTokenResp.body()!!.id
                                accountDao.updateAccount(
                                    account.copy(
                                        token = newToken,
                                        accountId = newId,
                                        lastUsedAt = System.currentTimeMillis()
                                    )
                                )
                                return@withContext newToken
                            }
                        }
                    }
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

        // 1. GuerrillaMail account: always bind session with setEmailUser to get actual messages
        if (isGuerrillaDomain(domain) || token.startsWith("grr_")) {
            var sidToken = token.removePrefix("grr_").trim().ifBlank { null }
            try {
                if (sidToken.isNullOrBlank()) {
                    val initResp = ApiClient.guerrillaMailService.getEmailAddress()
                    if (initResp.isSuccessful && initResp.body() != null) {
                        sidToken = initResp.body()!!.sidToken.ifBlank { null }
                    }
                }

                // CRITICAL: Bind session to user & site
                val setResp = ApiClient.guerrillaMailService.setEmailUser(
                    emailUser = login,
                    site = guerrillaSite(domain),
                    sidToken = sidToken
                )
                val activeSid = if (setResp.isSuccessful && setResp.body() != null) {
                    setResp.body()!!.sidToken.ifBlank { sidToken }
                } else sidToken

                if (!activeSid.isNullOrBlank() && "grr_$activeSid" != token) {
                    val existingAccount = accountDao.getAccountByAddress(cleanAddress)
                    if (existingAccount != null) {
                        accountDao.updateAccount(existingAccount.copy(token = "grr_$activeSid"))
                    }
                }

                // Fetch email list
                val listResp = ApiClient.guerrillaMailService.getEmailList(offset = 0, sidToken = activeSid)
                val mailList = if (listResp.isSuccessful && listResp.body() != null && listResp.body()!!.list.isNotEmpty()) {
                    listResp.body()!!.list
                } else {
                    val checkResp = ApiClient.guerrillaMailService.checkEmail(seq = 0, sidToken = activeSid)
                    if (checkResp.isSuccessful && checkResp.body() != null) checkResp.body()!!.list else emptyList()
                }

                mailList.forEach { item ->
                    val itemId = item.mailId.toString()
                    if (itemId.isNotBlank() && itemId != "0") {
                        remoteList.add(
                            MessageHeaderItem(
                                id = "grr_$itemId",
                                accountId = "grr_$login",
                                msgid = "<grr-$itemId@$domain>",
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
                Log.w("TempMailRepo", "GuerrillaMail fetchMessages error", e)
            }
        } else {
            // 2. Mail.tm / Mail.gw account
            val mailTmServices = listOf(ApiClient.mailTmService, ApiClient.mailGwService)
            var authToken = token
            for (api in mailTmServices) {
                try {
                    // If token is invalid or missing, heal and fetch JWT token
                    if (authToken.startsWith("secmail_") || authToken.startsWith("grr_") || authToken.isBlank()) {
                        val existingAccount = accountDao.getAccountByAddress(cleanAddress)
                        if (existingAccount != null && existingAccount.password.isNotBlank()) {
                            val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = existingAccount.password))
                            if (tokenResp.isSuccessful && tokenResp.body() != null) {
                                authToken = tokenResp.body()!!.token
                                accountDao.updateAccount(existingAccount.copy(token = authToken))
                            } else {
                                val createResp = api.createAccount(CreateAccountRequest(address = cleanAddress, password = existingAccount.password))
                                if (createResp.isSuccessful || createResp.code() == 422) {
                                    val retryToken = api.getToken(TokenRequest(address = cleanAddress, password = existingAccount.password))
                                    if (retryToken.isSuccessful && retryToken.body() != null) {
                                        authToken = retryToken.body()!!.token
                                        val newId = createResp.body()?.id ?: retryToken.body()!!.id
                                        accountDao.updateAccount(existingAccount.copy(token = authToken, accountId = newId))
                                    }
                                }
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
                            if (existingAccount != null && existingAccount.password.isNotBlank()) {
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
            var sidToken = token.removePrefix("grr_").trim().ifBlank { null }
            try {
                if (sidToken.isNullOrBlank()) {
                    val initResp = ApiClient.guerrillaMailService.getEmailAddress()
                    if (initResp.isSuccessful && initResp.body() != null) {
                        sidToken = initResp.body()!!.sidToken.ifBlank { null }
                    }
                }
                val setResp = ApiClient.guerrillaMailService.setEmailUser(
                    emailUser = login,
                    site = guerrillaSite(domain),
                    sidToken = sidToken
                )
                val activeSid = if (setResp.isSuccessful && setResp.body() != null) {
                    setResp.body()!!.sidToken.ifBlank { sidToken }
                } else sidToken

                val fetchResp = ApiClient.guerrillaMailService.fetchEmail(emailId = rawId, sidToken = activeSid)
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

        // 2. Primary: Fetch from Mail.tm / Mail.gw API
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
                        if (existingAccount != null && existingAccount.password.isNotBlank()) {
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

    private fun guerrillaSite(domain: String): String {
        val d = domain.lowercase(Locale.ROOT)
        return when (d) {
            "sharklasers.com" -> "sharklasers.com"
            "grr.la" -> "grr.la"
            "pokemail.net" -> "pokemail.net"
            "spam4.me" -> "spam4.me"
            else -> "guerrillamail.com"
        }
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
        val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder()
        repeat(8) {
            sb.append(chars[secureRandom.nextInt(chars.length)])
        }
        return sb.toString()
    }

    private fun generateSecurePassword(): String {
        val upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lower = "abcdefghijklmnopqrstuvwxyz"
        val digits = "0123456789"
        val all = upper + lower + digits
        val sb = StringBuilder()
        // Ensure at least one upper, one lower, one digit
        sb.append(upper[secureRandom.nextInt(upper.length)])
        sb.append(lower[secureRandom.nextInt(lower.length)])
        sb.append(digits[secureRandom.nextInt(digits.length)])
        repeat(7) {
            sb.append(all[secureRandom.nextInt(all.length)])
        }
        return sb.toString()
    }
}
