package com.example.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SecMailApi {

    @GET("?action=getDomainList")
    suspend fun getDomainList(): Response<List<String>>

    @GET("?action=getMessages")
    suspend fun getMessages(
        @Query("login") login: String,
        @Query("domain") domain: String
    ): Response<List<SecMailMessageHeader>>

    @GET("?action=readMessage")
    suspend fun readMessage(
        @Query("login") login: String,
        @Query("domain") domain: String,
        @Query("id") id: String
    ): Response<SecMailMessageDetail>
}
