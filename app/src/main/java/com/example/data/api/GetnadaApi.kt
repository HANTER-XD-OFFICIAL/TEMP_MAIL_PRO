package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.Path

interface GetnadaApi {

    @GET("domain")
    suspend fun getDomains(): Response<GetnadaDomainsResponse>

    @POST("inbox")
    suspend fun createInbox(): Response<GetnadaInboxCreateResponse>

    @GET("inbox/{inbox}")
    suspend fun getInbox(
        @Path("inbox") inbox: String
    ): Response<GetnadaInboxResponse>

    @GET("message/{id}")
    suspend fun getMessage(
        @Path("id") id: String
    ): Response<GetnadaMessageDetailResponse>

    @DELETE("inbox/{inbox}")
    suspend fun deleteInbox(
        @Path("inbox") inbox: String
    ): Response<Unit>

    @HTTP(method = "DELETE", path = "message/", hasBody = true)
    suspend fun deleteMessages(
        @Body request: GetnadaDeleteRequest
    ): Response<Unit>
}
