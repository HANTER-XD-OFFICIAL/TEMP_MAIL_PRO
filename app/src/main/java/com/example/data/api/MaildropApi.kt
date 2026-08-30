package com.example.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface MaildropApi {
    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("graphql")
    suspend fun query(@Body request: MaildropGraphqlRequest): Response<MaildropGraphqlResponse>
}
