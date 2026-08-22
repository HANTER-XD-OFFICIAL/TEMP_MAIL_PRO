package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface MailTmApi {

    @GET("domains")
    suspend fun getDomains(@Query("page") page: Int = 1): Response<DomainListResponse>

    @POST("accounts")
    suspend fun createAccount(@Body request: CreateAccountRequest): Response<AccountResponse>

    @POST("token")
    suspend fun getToken(@Body request: TokenRequest): Response<TokenResponse>

    @GET("me")
    suspend fun getMe(@Header("Authorization") authHeader: String): Response<AccountResponse>

    @GET("messages")
    suspend fun getMessages(
        @Header("Authorization") authHeader: String,
        @Query("page") page: Int = 1
    ): Response<MessageListResponse>

    @GET("messages/{id}")
    suspend fun getMessageDetail(
        @Header("Authorization") authHeader: String,
        @Path("id") messageId: String
    ): Response<MessageDetailResponse>

    @DELETE("messages/{id}")
    suspend fun deleteMessage(
        @Header("Authorization") authHeader: String,
        @Path("id") messageId: String
    ): Response<Unit>

    @DELETE("accounts/{id}")
    suspend fun deleteAccount(
        @Header("Authorization") authHeader: String,
        @Path("id") accountId: String
    ): Response<Unit>
}
