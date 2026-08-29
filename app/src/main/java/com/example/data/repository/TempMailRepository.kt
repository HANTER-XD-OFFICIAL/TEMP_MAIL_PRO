package com.example.data.repository

import android.util.Log
import com.example.data.api.ApiClient
import com.example.data.api.AttachmentItem
import com.example.data.api.CreateAccountRequest
import com.example.data.api.DomainItem
import com.example.data.api.EmailParticipant
import com.example.data.api.MailTmApi
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

    // Map each domain to its hosting server (Mail.tm vs Mail.gw)
    private val domainServerMap = java.util.concurrent.ConcurrentHashMap<String, String>().apply {
        put("emalupe.com", ApiClient.PRIMARY_BASE_URL)
        put("westcast-systems.com", ApiClient.SECONDARY_BASE_URL)
    }

    private fun getServicesForDomain(domain: String): List<Pair<MailTmApi, String>> {
        val cleanDomain = domain.trim().lowercase(Locale.ROOT)
        val preferredServer = domainServerMap[cleanDomain]
        return if (preferredServer == ApiClient.SECONDARY_BASE_URL || cleanDomain == "westcast-systems.com") {
            listOf(
                Pair(ApiClient.mailGwService, ApiClient.SECONDARY_BASE_URL),
                Pair(ApiClient.mailTmService, ApiClient.PRIMARY_BASE_URL)
            )
        } else {
            listOf(
                Pair(ApiClient.mailTmService, ApiClient.PRIMARY_BASE_URL),
                Pair(ApiClient.mailGwService, ApiClient.SECONDARY_BASE_URL)
            )
        }
    }

    // Available domains from Mail.tm, Mail.gw and GuerrillaMail APIs
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
        val serviceEndpoints = listOf(
            Pair(ApiClient.mailTmService, ApiClient.PRIMARY_BASE_URL),
            Pair(ApiClient.mailGwService, ApiClient.SECONDARY_BASE_URL)
        )
        for ((api, serverUrl) in serviceEndpoints) {
            try {
                val resp = api.getDomains()
                if (resp.isSuccessful && resp.body() != null) {
                    val activeList = resp.body()!!.member.filter { it.isActive }
                    activeList.forEach { d ->
                        val cleanD = d.domain.lowercase(Locale.ROOT)
                        domainServerMap[cleanD] = serverUrl
                        if (resultDomains.none { it.domain.equals(d.domain, ignoreCase = true) }) {
                            resultDomains.add(d)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Fetch domains error from $serverUrl", e)
            }
        }

        // Guarantee emalupe.com (Mail.tm) and westcast-systems.com (Mail.gw)
        domainServerMap["emalupe.com"] = ApiClient.PRIMARY_BASE_URL
        domainServerMap["westcast-systems.com"] = ApiClient.SECONDARY_BASE_URL

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

        // Add Getnada / Inboxes.com domains
        try {
            val nadaResp = ApiClient.getnadaService.getDomains()
            if (nadaResp.isSuccessful && nadaResp.body() != null) {
                nadaResp.body()!!.domains.forEach { item ->
                    val domainName = item.qdn?.trim()?.lowercase(Locale.ROOT) ?: item.name?.trim()?.lowercase(Locale.ROOT)
                    if (!domainName.isNullOrBlank() && resultDomains.none { it.domain.equals(domainName, ignoreCase = true) }) {
                        resultDomains.add(
                            DomainItem(
                                id = domainName,
                                domain = domainName,
                                isActive = true,
                                isPrivate = false,
                                createdAt = "2026-01-01T00:00:00.000Z"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TempMailRepo", "Getnada domains fetch error", e)
        }

        val getnadaDomains = listOf(
            "getnada.com",
            "getairmail.com",
            "inboxbear.com",
            "dropjar.com",
            "robot-mail.com",
            "tafmail.com",
            "vomoto.com",
            "gimpmail.com",
            "blondmail.com",
            "chapsmail.com",
            "clowmail.com",
            "fivermail.com",
            "getmule.com",
            "givmail.com",
            "guysmail.com",
            "replyloop.com",
            "temptami.com",
            "tupmail.com"
        )
        getnadaDomains.forEach { d ->
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

        // Add 1secmail domains
        try {
            val secResp = ApiClient.secMailService.getDomainList()
            if (secResp.isSuccessful && secResp.body() != null) {
                secResp.body()!!.forEach { d ->
                    val cleanD = d.trim().lowercase(Locale.ROOT)
                    if (cleanD.isNotBlank() && resultDomains.none { it.domain.equals(cleanD, ignoreCase = true) }) {
                        resultDomains.add(
                            DomainItem(
                                id = cleanD,
                                domain = cleanD,
                                isActive = true,
                                isPrivate = false,
                                createdAt = "2026-01-01T00:00:00.000Z"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("TempMailRepo", "1secmail domains fetch error", e)
        }

        val secMailDomains = listOf(
            "1secmail.com",
            "1secmail.net",
            "1secmail.org",
            "esiix.com",
            "wwjmp.com",
            "icznn.com",
            "ezztt.com",
            "vmani.com"
        )
        secMailDomains.forEach { d ->
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

        // Add RapidAPI Temp-Mail domains if configured
        if (ApiClient.rapidApiKey.isNotBlank()) {
            try {
                val rapidResp = ApiClient.rapidApiTempMailService.getDomains(ApiClient.rapidApiKey)
                if (rapidResp.isSuccessful && rapidResp.body() != null) {
                    rapidResp.body()!!.forEach { d ->
                        val cleanD = d.trim().removePrefix("@").lowercase(Locale.ROOT)
                        if (cleanD.isNotBlank() && resultDomains.none { it.domain.equals(cleanD, ignoreCase = true) }) {
                            resultDomains.add(
                                DomainItem(
                                    id = cleanD,
                                    domain = cleanD,
                                    isActive = true,
                                    isPrivate = false,
                                    createdAt = "2026-01-01T00:00:00.000Z"
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "RapidAPI domains fetch error", e)
            }
        }

        val rapidApiDomains = listOf(
            "cevipsa.com",
            "freeml.net",
            "txcct.com",
            "vebby.com"
        )
        rapidApiDomains.forEach { d ->
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

            // Direct Getnada / Inboxes.com API generator
            if (isGetnadaDomain(domain)) {
                val userPrefix = generateFriendlyHandle()
                val finalAddress = "$userPrefix@$domain"
                try {
                    ApiClient.getnadaService.createInbox()
                } catch (ignored: Exception) {}

                val entity = SavedAccountEntity(
                    address = finalAddress,
                    password = generateSecurePassword(),
                    token = "nada_$finalAddress",
                    accountId = "nada_$userPrefix",
                    label = label.ifBlank { "Getnada Instant Mailbox" },
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                    isActive = true,
                    serverUrl = ApiClient.GETNADA_URL
                )
                accountDao.clearAllActiveFlags()
                accountDao.insertAccount(entity)
                return@withContext Result.success(entity)
            }

            // Direct 1secmail API generator
            if (isSecMailDomain(domain)) {
                val userPrefix = generateFriendlyHandle()
                val finalAddress = "$userPrefix@$domain"
                val entity = SavedAccountEntity(
                    address = finalAddress,
                    password = generateSecurePassword(),
                    token = "secmail_$finalAddress",
                    accountId = "secmail_$userPrefix",
                    label = label.ifBlank { "1secmail Instant Mailbox" },
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

            // Direct RapidAPI Temp-Mail generator
            if (isRapidApiDomain(domain)) {
                val userPrefix = generateFriendlyHandle()
                val finalAddress = "$userPrefix@$domain"
                val entity = SavedAccountEntity(
                    address = finalAddress,
                    password = generateSecurePassword(),
                    token = "rapid_$finalAddress",
                    accountId = "rapid_$userPrefix",
                    label = label.ifBlank { "RapidAPI Temp-Mail" },
                    createdAt = System.currentTimeMillis(),
                    lastUsedAt = System.currentTimeMillis(),
                    expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                    isActive = true,
                    serverUrl = ApiClient.RAPID_API_URL
                )
                accountDao.clearAllActiveFlags()
                accountDao.insertAccount(entity)
                return@withContext Result.success(entity)
            }

            var lastError: Throwable? = null
            // Retry up to 3 times with a fresh random handle in case of any rare handle collision
            for (attempt in 1..3) {
                val randomPrefix = generateFriendlyHandle()
                val generatedAddress = "$randomPrefix@$domain".lowercase(Locale.ROOT)
                val generatedPassword = generateSecurePassword()

                val result = createAndRegisterAccount(
                    address = generatedAddress,
                    password = generatedPassword,
                    label = label.ifBlank { "Live Mailbox" }
                )
                if (result.isSuccess) {
                    return@withContext result
                } else {
                    lastError = result.exceptionOrNull()
                }
            }
            Result.failure(lastError ?: Exception("Could not generate mailbox on @$domain. Please try again."))
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

        // 2. If Getnada domain:
        if (isGetnadaDomain(cleanDomain)) {
            try {
                ApiClient.getnadaService.createInbox()
            } catch (ignored: Exception) {}
            val entity = SavedAccountEntity(
                address = fullAddress,
                password = cleanPassword,
                token = "nada_$fullAddress",
                accountId = "nada_$cleanUsername",
                label = label.ifBlank { "Getnada Custom Mailbox" },
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                isActive = true,
                serverUrl = ApiClient.GETNADA_URL
            )
            accountDao.clearAllActiveFlags()
            accountDao.insertAccount(entity)
            return@withContext Result.success(entity)
        }

        // 3. If 1secmail domain:
        if (isSecMailDomain(cleanDomain)) {
            val entity = SavedAccountEntity(
                address = fullAddress,
                password = cleanPassword,
                token = "secmail_$fullAddress",
                accountId = "secmail_$cleanUsername",
                label = label.ifBlank { "1secmail Custom Mailbox" },
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

        // 4. If RapidAPI Temp-Mail domain:
        if (isRapidApiDomain(cleanDomain)) {
            val entity = SavedAccountEntity(
                address = fullAddress,
                password = cleanPassword,
                token = "rapid_$fullAddress",
                accountId = "rapid_$cleanUsername",
                label = label.ifBlank { "RapidAPI Custom Mailbox" },
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                isActive = true,
                serverUrl = ApiClient.RAPID_API_URL
            )
            accountDao.clearAllActiveFlags()
            accountDao.insertAccount(entity)
            return@withContext Result.success(entity)
        }

        // 5. Mail.tm / Mail.gw domain:
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

        val mailTmServices = getServicesForDomain(domain)

        var lastErrorMessage = ""

        for ((api, serverUrl) in mailTmServices) {
            try {
                // Register account on Mail.tm / Mail.gw
                val createResp = api.createAccount(CreateAccountRequest(address = cleanAddress, password = password))
                val accountId = if (createResp.isSuccessful && createResp.body() != null) {
                    createResp.body()!!.id
                } else null

                if (createResp.isSuccessful) {
                    // Record and cache serverUrl for domain
                    domainServerMap[domain] = serverUrl
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
                } else {
                    val rawErrorBody = try { createResp.errorBody()?.string().orEmpty() } catch (_: Exception) { "" }
                    Log.w("TempMailRepo", "Create account failed on $serverUrl ($cleanAddress): code=${createResp.code()}, body=$rawErrorBody")

                    if (createResp.code() == 422) {
                        val isInvalidDomain = rawErrorBody.contains("not valid", ignoreCase = true) ||
                                              rawErrorBody.contains("domain", ignoreCase = true)
                        val isAlreadyUsed = rawErrorBody.contains("already used", ignoreCase = true) ||
                                            rawErrorBody.contains("already taken", ignoreCase = true)

                        if (isInvalidDomain) {
                            // Domain is not on this server (e.g. mail.tm vs mail.gw), continue to next server in loop
                            lastErrorMessage = "Domain @$domain is not supported on $serverUrl."
                            continue
                        }

                        if (isAlreadyUsed) {
                            // Address already exists on this server. Check if user already owns it.
                            val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = password))
                            if (tokenResp.isSuccessful && tokenResp.body() != null) {
                                domainServerMap[domain] = serverUrl
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
                                return Result.failure(
                                    Exception("Username '$cleanUsername' is already taken on @$domain. Please choose a different username.")
                                )
                            }
                        }

                        lastErrorMessage = "Account request error: $rawErrorBody"
                    } else if (createResp.code() == 429) {
                        return Result.failure(Exception("Mail service rate limit reached. Please wait 10 seconds and try again."))
                    } else {
                        lastErrorMessage = "Server error (HTTP ${createResp.code()})"
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Mail account registration attempt failed on $serverUrl", e)
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

        // 2. Getnada domain
        if (isGetnadaDomain(domain)) {
            val entity = SavedAccountEntity(
                address = cleanAddress,
                password = cleanPassword,
                token = "nada_$cleanAddress",
                accountId = "nada_$userPrefix",
                label = label.ifBlank { "Getnada Mailbox" },
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                isActive = true,
                serverUrl = ApiClient.GETNADA_URL
            )
            accountDao.clearAllActiveFlags()
            accountDao.insertAccount(entity)
            return@withContext Result.success(entity)
        }

        // 3. 1secmail domain
        if (isSecMailDomain(domain)) {
            val entity = SavedAccountEntity(
                address = cleanAddress,
                password = cleanPassword,
                token = "secmail_$cleanAddress",
                accountId = "secmail_$userPrefix",
                label = label.ifBlank { "1secmail Mailbox" },
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

        // 4. RapidAPI domain
        if (isRapidApiDomain(domain)) {
            val entity = SavedAccountEntity(
                address = cleanAddress,
                password = cleanPassword,
                token = "rapid_$cleanAddress",
                accountId = "rapid_$userPrefix",
                label = label.ifBlank { "RapidAPI Mailbox" },
                createdAt = System.currentTimeMillis(),
                lastUsedAt = System.currentTimeMillis(),
                expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                isActive = true,
                serverUrl = ApiClient.RAPID_API_URL
            )
            accountDao.clearAllActiveFlags()
            accountDao.insertAccount(entity)
            return@withContext Result.success(entity)
        }

        // 5. Mail.tm / Mail.gw
        val services = getServicesForDomain(domain)
        for ((api, serverUrl) in services) {
            try {
                val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = cleanPassword))
                if (tokenResp.isSuccessful && tokenResp.body() != null) {
                    domainServerMap[domain] = serverUrl
                    val token = tokenResp.body()!!.token
                    val accountId = tokenResp.body()!!.id

                    val existing = accountDao.getAccountByAddress(cleanAddress)
                    val entity = (existing?.copy(
                        password = cleanPassword,
                        token = token,
                        accountId = accountId,
                        lastUsedAt = System.currentTimeMillis(),
                        isActive = true,
                        serverUrl = serverUrl,
                        label = if (label.isNotBlank()) label else existing.label.ifBlank { "Live Account" }
                    )) ?: SavedAccountEntity(
                        address = cleanAddress,
                        password = cleanPassword,
                        token = token,
                        accountId = accountId,
                        label = label.ifBlank { "Live Account" },
                        createdAt = System.currentTimeMillis(),
                        lastUsedAt = System.currentTimeMillis(),
                        expiresAt = System.currentTimeMillis() + (10 * 60 * 1000L),
                        isActive = true,
                        serverUrl = serverUrl
                    )

                    accountDao.clearAllActiveFlags()
                    accountDao.insertAccount(entity)
                    return@withContext Result.success(entity)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Login attempt failed on $serverUrl", e)
            }
        }

        Result.failure(Exception("Invalid email address or password. Please verify credentials and try again."))
    }

    suspend fun ensureValidToken(account: SavedAccountEntity): String? = withContext(Dispatchers.IO) {
        val cleanAddress = account.address.trim().lowercase(Locale.ROOT)
        val domain = cleanAddress.substringAfter("@", "")
        val userPrefix = cleanAddress.substringBefore("@")

        if (account.token?.startsWith("nada_") == true) {
            return@withContext account.token
        }
        if (account.token?.startsWith("secmail_") == true) {
            return@withContext account.token
        }
        if (account.token?.startsWith("rapid_") == true) {
            return@withContext account.token
        }

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
        val services = getServicesForDomain(domain)
        for ((api, serverUrl) in services) {
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
                                lastUsedAt = System.currentTimeMillis(),
                                serverUrl = serverUrl
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
                                        lastUsedAt = System.currentTimeMillis(),
                                        serverUrl = serverUrl
                                    )
                                )
                                return@withContext newToken
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "ensureValidToken retry failed on $serverUrl", e)
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
            if (existing != null && deleteFromServer && !existing.token.isNullOrBlank() && !existing.accountId.isNullOrBlank() &&
                !existing.token.startsWith("secmail_") &&
                !existing.token.startsWith("grr_") &&
                !existing.token.startsWith("nada_") &&
                !existing.token.startsWith("rapid_")
            ) {
                try {
                    ApiClient.mailTmService.deleteAccount("Bearer ${existing.token}", existing.accountId)
                } catch (ignored: Exception) {}
            }
            if (existing != null && existing.token?.startsWith("nada_") == true && deleteFromServer) {
                try {
                    ApiClient.getnadaService.deleteInbox(address)
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
        } else if (isGetnadaDomain(domain) || token.startsWith("nada_")) {
            // 2. Getnada / Inboxes.com account
            try {
                val resp = ApiClient.getnadaService.getInbox(cleanAddress)
                if (resp.isSuccessful && resp.body() != null) {
                    resp.body()!!.msgs.forEach { item ->
                        val uid = item.uid.ifBlank { "${System.currentTimeMillis()}" }
                        remoteList.add(
                            MessageHeaderItem(
                                id = "nada_$uid",
                                accountId = "nada_$login",
                                msgid = "<nada-$uid@$domain>",
                                from = EmailParticipant(
                                    address = item.fe ?: item.f ?: "sender@$domain",
                                    name = item.f ?: item.fe ?: "Sender"
                                ),
                                to = listOf(EmailParticipant(address = cleanAddress, name = "You")),
                                subject = item.s?.ifBlank { "(No Subject)" } ?: "(No Subject)",
                                intro = item.text?.take(160) ?: item.s ?: "",
                                seen = false,
                                isDeleted = false,
                                hasAttachments = false,
                                size = (item.text?.length ?: 1024).toLong(),
                                createdAt = formatTimestamp(item.r)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Getnada fetchMessages error", e)
            }
        } else if (isSecMailDomain(domain) || token.startsWith("secmail_")) {
            // 3. 1secmail account
            try {
                val secResp = ApiClient.secMailService.getMessages(login = login, domain = domain)
                val list = if (secResp.isSuccessful && secResp.body() != null) {
                    secResp.body()!!
                } else {
                    val netResp = ApiClient.secMailNetService.getMessages(login = login, domain = domain)
                    if (netResp.isSuccessful && netResp.body() != null) netResp.body()!! else emptyList()
                }
                list.forEach { item ->
                    remoteList.add(
                        MessageHeaderItem(
                            id = "secmail_${item.id}",
                            accountId = "secmail_$login",
                            msgid = "<secmail-${item.id}@$domain>",
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
                            size = 1024L,
                            createdAt = item.date
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "1secmail fetchMessages error", e)
            }
        } else if (isRapidApiDomain(domain) || token.startsWith("rapid_")) {
            // 4. RapidAPI Temp-Mail account
            try {
                val md5 = md5Hex(cleanAddress)
                val rapidResp = ApiClient.rapidApiTempMailService.getMail(md5 = md5, apiKey = ApiClient.rapidApiKey)
                if (rapidResp.isSuccessful && rapidResp.body() != null) {
                    rapidResp.body()!!.forEach { item ->
                        remoteList.add(
                            MessageHeaderItem(
                                id = "rapid_${item.mailId}",
                                accountId = "rapid_$login",
                                msgid = "<rapid-${item.mailId}@$domain>",
                                from = EmailParticipant(
                                    address = item.mailFrom,
                                    name = item.mailFrom.substringBefore("@")
                                ),
                                to = listOf(EmailParticipant(address = cleanAddress, name = "You")),
                                subject = item.mailSubject.ifBlank { "(No Subject)" },
                                intro = item.mailPreview.ifBlank { item.mailSubject },
                                seen = false,
                                isDeleted = false,
                                hasAttachments = !item.attachments.isNullOrEmpty(),
                                size = 1024L,
                                createdAt = formatTimestamp(item.mailTimestamp?.toLong())
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "RapidAPI fetchMessages error", e)
            }
        } else {
            // 5. Mail.tm / Mail.gw account
            val mailTmServices = getServicesForDomain(domain)
            var authToken = token
            for ((api, serverUrl) in mailTmServices) {
                try {
                    // If token is invalid or missing, heal and fetch JWT token
                    if (authToken.startsWith("secmail_") || authToken.startsWith("grr_") || authToken.isBlank()) {
                        val existingAccount = accountDao.getAccountByAddress(cleanAddress)
                        if (existingAccount != null && existingAccount.password.isNotBlank()) {
                            val tokenResp = api.getToken(TokenRequest(address = cleanAddress, password = existingAccount.password))
                            if (tokenResp.isSuccessful && tokenResp.body() != null) {
                                authToken = tokenResp.body()!!.token
                                accountDao.updateAccount(existingAccount.copy(token = authToken, serverUrl = serverUrl))
                            } else {
                                val createResp = api.createAccount(CreateAccountRequest(address = cleanAddress, password = existingAccount.password))
                                if (createResp.isSuccessful || createResp.code() == 422) {
                                    val retryToken = api.getToken(TokenRequest(address = cleanAddress, password = existingAccount.password))
                                    if (retryToken.isSuccessful && retryToken.body() != null) {
                                        authToken = retryToken.body()!!.token
                                        val newId = createResp.body()?.id ?: retryToken.body()!!.id
                                        accountDao.updateAccount(existingAccount.copy(token = authToken, accountId = newId, serverUrl = serverUrl))
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
                                    accountDao.updateAccount(existingAccount.copy(token = authToken, serverUrl = serverUrl))
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
                    Log.w("TempMailRepo", "Mail.tm fetchMessages error on $serverUrl", e)
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
        } else if (messageId.startsWith("nada_")) {
            // 2. Getnada message
            val rawId = messageId.removePrefix("nada_")
            try {
                val resp = ApiClient.getnadaService.getMessage(rawId)
                if (resp.isSuccessful && resp.body()?.msg != null) {
                    val m = resp.body()!!.msg!!
                    val rawBody = m.html ?: m.text ?: ""
                    val isHtml = m.html != null || rawBody.contains("<div") || rawBody.contains("<p") || rawBody.contains("<html") || rawBody.contains("<br")
                    val detail = MessageDetailResponse(
                        id = messageId,
                        accountId = "nada_$login",
                        msgid = "<nada-$rawId@$domain>",
                        from = EmailParticipant(address = m.fe ?: m.f ?: "sender@$domain", name = m.f ?: m.fe ?: "Sender"),
                        to = listOf(EmailParticipant(address = cleanAddress, name = "You")),
                        subject = m.s?.ifBlank { "(No Subject)" } ?: "(No Subject)",
                        intro = m.text?.take(160) ?: m.s ?: "",
                        seen = true,
                        isDeleted = false,
                        hasAttachments = false,
                        size = rawBody.length.toLong(),
                        createdAt = formatTimestamp(m.r),
                        text = if (isHtml && m.html != null) null else (m.text ?: rawBody),
                        html = if (m.html != null) listOf(m.html) else if (isHtml) listOf(rawBody) else null,
                        attachments = emptyList()
                    )
                    return@withContext Result.success(detail)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Getnada getMessage error", e)
            }
        } else if (messageId.startsWith("secmail_")) {
            // 3. 1secmail message
            val rawId = messageId.removePrefix("secmail_")
            try {
                val resp = ApiClient.secMailService.readMessage(login = login, domain = domain, id = rawId)
                if (resp.isSuccessful && resp.body() != null) {
                    val b = resp.body()!!
                    val rawHtml = b.htmlBody
                    val rawText = b.textBody ?: b.body ?: ""
                    val isHtml = !rawHtml.isNullOrBlank() || rawText.contains("<div") || rawText.contains("<p") || rawText.contains("<html")
                    val attachments = b.attachments.map { at ->
                        AttachmentItem(
                            id = at.filename,
                            filename = at.filename,
                            contentType = at.contentType,
                            size = at.size,
                            downloadUrl = "https://www.1secmail.com/api/v1/?action=download&login=$login&domain=$domain&id=$rawId&file=${at.filename}"
                        )
                    }
                    val detail = MessageDetailResponse(
                        id = messageId,
                        accountId = "secmail_$login",
                        msgid = "<secmail-$rawId@$domain>",
                        from = EmailParticipant(address = b.from, name = b.from.substringBefore("@")),
                        to = listOf(EmailParticipant(address = cleanAddress, name = "You")),
                        subject = b.subject.ifBlank { "(No Subject)" },
                        intro = b.subject,
                        seen = true,
                        isDeleted = false,
                        hasAttachments = attachments.isNotEmpty(),
                        size = (rawHtml?.length ?: rawText.length).toLong(),
                        createdAt = b.date,
                        text = if (isHtml && !rawHtml.isNullOrBlank()) null else rawText,
                        html = if (!rawHtml.isNullOrBlank()) listOf(rawHtml) else if (isHtml) listOf(rawText) else null,
                        attachments = attachments
                    )
                    return@withContext Result.success(detail)
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "1secmail readMessage error", e)
            }
        } else if (messageId.startsWith("rapid_")) {
            // 4. RapidAPI message
            val rawId = messageId.removePrefix("rapid_")
            try {
                val md5 = md5Hex(cleanAddress)
                val resp = ApiClient.rapidApiTempMailService.getMail(md5 = md5, apiKey = ApiClient.rapidApiKey)
                if (resp.isSuccessful && resp.body() != null) {
                    val match = resp.body()!!.firstOrNull { it.mailId == rawId }
                    if (match != null) {
                        val rawHtml = match.mailHtml
                        val rawText = match.mailText ?: match.mailTextOnly ?: match.mailPreview
                        val isHtml = !rawHtml.isNullOrBlank()
                        val attachments = match.attachments?.map { at ->
                            AttachmentItem(
                                id = at.filename,
                                filename = at.filename,
                                contentType = "application/octet-stream",
                                size = at.size
                            )
                        } ?: emptyList()
                        val detail = MessageDetailResponse(
                            id = messageId,
                            accountId = "rapid_$login",
                            msgid = "<rapid-$rawId@$domain>",
                            from = EmailParticipant(address = match.mailFrom, name = match.mailFrom.substringBefore("@")),
                            to = listOf(EmailParticipant(address = cleanAddress, name = "You")),
                            subject = match.mailSubject.ifBlank { "(No Subject)" },
                            intro = match.mailPreview,
                            seen = true,
                            isDeleted = false,
                            hasAttachments = attachments.isNotEmpty(),
                            size = (rawHtml?.length ?: rawText.length).toLong(),
                            createdAt = formatTimestamp(match.mailTimestamp?.toLong()),
                            text = if (isHtml && !rawHtml.isNullOrBlank()) null else rawText,
                            html = if (!rawHtml.isNullOrBlank()) listOf(rawHtml) else null,
                            attachments = attachments
                        )
                        return@withContext Result.success(detail)
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "RapidAPI getMail error", e)
            }
        }

        // 5. Primary: Fetch from Mail.tm / Mail.gw API
        val services = getServicesForDomain(domain)
        var authToken = token
        for ((api, serverUrl) in services) {
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
                                accountDao.updateAccount(existingAccount.copy(token = authToken, serverUrl = serverUrl))
                                val retryResp = api.getMessageDetail("Bearer $authToken", messageId)
                                if (retryResp.isSuccessful && retryResp.body() != null) {
                                    return@withContext Result.success(retryResp.body()!!)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("TempMailRepo", "Mail.tm fetchMessageDetail error on $serverUrl", e)
            }
        }

        Result.failure(Exception("Message detail could not be retrieved. Please check your internet connection."))
    }

    suspend fun deleteMessage(token: String, messageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (messageId.startsWith("nada_")) {
            val rawId = messageId.removePrefix("nada_")
            try {
                ApiClient.getnadaService.deleteMessages(com.example.data.api.GetnadaDeleteRequest(ids = listOf(rawId)))
            } catch (ignored: Exception) {}
        } else if (messageId.startsWith("rapid_")) {
            val rawId = messageId.removePrefix("rapid_")
            try {
                ApiClient.rapidApiTempMailService.deleteMail(mailId = rawId, apiKey = ApiClient.rapidApiKey)
            } catch (ignored: Exception) {}
        } else if (!token.startsWith("secmail_") && !token.startsWith("grr_")) {
            try {
                ApiClient.mailTmService.deleteMessage("Bearer $token", messageId)
            } catch (ignored: Exception) {}
            try {
                ApiClient.mailGwService.deleteMessage("Bearer $token", messageId)
            } catch (ignored: Exception) {}
        }
        Result.success(Unit)
    }

    fun getProviderNameForDomain(domain: String): String {
        val d = domain.trim().lowercase(Locale.ROOT)
        return when {
            isGetnadaDomain(d) -> "Getnada"
            isSecMailDomain(d) -> "1secmail"
            isGuerrillaDomain(d) -> "Guerrilla Mail"
            isRapidApiDomain(d) -> "Temp-Mail"
            else -> "Mail.tm"
        }
    }

    fun isGetnadaDomain(domain: String): Boolean {
        val d = domain.trim().lowercase(Locale.ROOT)
        return d.contains("nada") ||
                d == "getairmail.com" ||
                d == "inboxbear.com" ||
                d == "dropjar.com" ||
                d == "robot-mail.com" ||
                d == "tafmail.com" ||
                d == "vomoto.com" ||
                d == "gimpmail.com" ||
                d == "blondmail.com" ||
                d == "chapsmail.com" ||
                d == "clowmail.com" ||
                d == "fivermail.com" ||
                d == "getmule.com" ||
                d == "givmail.com" ||
                d == "guysmail.com" ||
                d == "replyloop.com" ||
                d == "temptami.com" ||
                d == "tupmail.com" ||
                d == "abyssmail.com" ||
                d == "boximail.com" ||
                d == "clrmail.com"
    }

    fun isRapidApiDomain(domain: String): Boolean {
        val d = domain.trim().lowercase(Locale.ROOT)
        return d.contains("rapid") ||
                d == "cevipsa.com" ||
                d == "freeml.net" ||
                d == "txcct.com" ||
                d == "vebby.com" ||
                d == "disbox.net" ||
                d == "dropmail.me"
    }

    fun isSecMailDomain(domain: String): Boolean {
        val d = domain.lowercase(Locale.ROOT)
        return d.contains("1secmail") ||
                d == "esiix.com" ||
                d == "wwjmp.com" ||
                d == "icznn.com" ||
                d == "ezztt.com" ||
                d == "vmani.com"
    }

    fun isGuerrillaDomain(domain: String): Boolean {
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

    private fun md5Hex(input: String): String {
        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val digest = md.digest(input.trim().lowercase(Locale.ROOT).toByteArray(Charsets.UTF_8))
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            input.hashCode().toString()
        }
    }

    private fun formatTimestamp(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0) return ""
        return try {
            val millis = if (timestamp < 10000000000L) timestamp * 1000L else timestamp
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            sdf.format(Date(millis))
        } catch (_: Exception) {
            ""
        }
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
