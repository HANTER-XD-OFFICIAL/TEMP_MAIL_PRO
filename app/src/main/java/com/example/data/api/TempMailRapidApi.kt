package com.example.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface TempMailRapidApi {

    @GET("request/domains/format/json/")
    suspend fun getDomains(
        @Header("x-rapidapi-key") apiKey: String = "",
        @Header("x-rapidapi-host") host: String = "privatix-temp-mail-v1.p.rapidapi.com"
    ): Response<List<String>>

    @GET("request/mail/id/{md5}/format/json/")
    suspend fun getMail(
        @Path("md5") md5: String,
        @Header("x-rapidapi-key") apiKey: String = "",
        @Header("x-rapidapi-host") host: String = "privatix-temp-mail-v1.p.rapidapi.com"
    ): Response<List<RapidApiTempMailItem>>

    @GET("request/delete/id/{mail_id}/format/json/")
    suspend fun deleteMail(
        @Path("mail_id") mailId: String,
        @Header("x-rapidapi-key") apiKey: String = "",
        @Header("x-rapidapi-host") host: String = "privatix-temp-mail-v1.p.rapidapi.com"
    ): Response<Unit>
}
