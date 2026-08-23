package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
    const val PRIMARY_BASE_URL = "https://api.mail.tm/"
    const val SECONDARY_BASE_URL = "https://api.mail.gw/"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    val mailTmService: MailTmApi by lazy {
        createService(PRIMARY_BASE_URL)
    }

    val mailGwService: MailTmApi by lazy {
        createService(SECONDARY_BASE_URL)
    }

    const val SEC_MAIL_PRIMARY_URL = "https://www.1secmail.com/api/v1/"
    const val SEC_MAIL_NET_URL = "https://www.1secmail.net/api/v1/"
    const val SEC_MAIL_ORG_URL = "https://www.1secmail.org/api/v1/"

    val secMailService: SecMailApi by lazy {
        createSecMailService(SEC_MAIL_PRIMARY_URL)
    }

    val secMailNetService: SecMailApi by lazy {
        createSecMailService(SEC_MAIL_NET_URL)
    }

    val secMailOrgService: SecMailApi by lazy {
        createSecMailService(SEC_MAIL_ORG_URL)
    }

    fun createSecMailService(baseUrl: String): SecMailApi {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SecMailApi::class.java)
    }

    fun createService(baseUrl: String): MailTmApi {
        val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(url)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(MailTmApi::class.java)
    }
}
