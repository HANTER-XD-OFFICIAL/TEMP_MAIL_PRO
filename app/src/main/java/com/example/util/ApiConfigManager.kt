package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.ApiClient

data class MailProviderInfo(
    val id: String,
    val name: String,
    val shortName: String,
    val endpointUrl: String,
    val description: String,
    val requiresApiKey: Boolean,
    val isAvailable: Boolean = true,
    val referenceUrl: String
)

object ApiConfigManager {
    private const val PREFS_NAME = "temp_mail_api_prefs"
    private const val KEY_RAPID_API_KEY = "rapid_api_key"

    fun init(context: Context) {
        val prefs = getPrefs(context)
        val savedKey = prefs.getString(KEY_RAPID_API_KEY, "") ?: ""
        ApiClient.rapidApiKey = savedKey
    }

    fun getRapidApiKey(context: Context): String {
        return getPrefs(context).getString(KEY_RAPID_API_KEY, "") ?: ""
    }

    fun setRapidApiKey(context: Context, key: String) {
        val cleanKey = key.trim()
        getPrefs(context).edit().putString(KEY_RAPID_API_KEY, cleanKey).apply()
        ApiClient.rapidApiKey = cleanKey
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val ALL_PROVIDERS = listOf(
        MailProviderInfo(
            id = "secmail",
            name = "1secmail API",
            shortName = "1secmail",
            endpointUrl = "https://www.1secmail.com/api/",
            description = "Lightweight disposable email API. No API key required.",
            requiresApiKey = false,
            referenceUrl = "https://www.1secmail.com/api/"
        ),
        MailProviderInfo(
            id = "guerrilla",
            name = "Guerrilla Mail API",
            shortName = "Guerrilla",
            endpointUrl = "https://api.guerrillamail.com/",
            description = "Open API for temporary session-based email creation and checking.",
            requiresApiKey = false,
            referenceUrl = "https://www.guerrillamail.com/GuerrillaMailAPI.html"
        ),
        MailProviderInfo(
            id = "mailtm",
            name = "Mail.tm / Mail.gw API",
            shortName = "Mail.tm",
            endpointUrl = "https://api.mail.tm/",
            description = "Modern RESTful API for disposable inbox creation with JWT authentication.",
            requiresApiKey = false,
            referenceUrl = "https://docs.mail.tm/"
        ),
        MailProviderInfo(
            id = "getnada",
            name = "Getnada / Nada API",
            shortName = "Getnada",
            endpointUrl = "https://inboxes.com/api/v2/",
            description = "Fast temporary email inbox API for automated testing environments.",
            requiresApiKey = false,
            referenceUrl = "https://getnada.com/"
        ),
        MailProviderInfo(
            id = "rapidapi",
            name = "Temp-Mail (RapidAPI)",
            shortName = "RapidAPI",
            endpointUrl = "https://privatix-temp-mail-v1.p.rapidapi.com/",
            description = "RapidAPI-hosted disposable email service with free tier limits.",
            requiresApiKey = true,
            referenceUrl = "https://rapidapi.com/Privatix/api/temp-mail"
        )
    )
}
