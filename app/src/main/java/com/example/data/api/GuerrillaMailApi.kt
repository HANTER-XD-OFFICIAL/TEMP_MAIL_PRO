package com.example.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GuerrillaMailApi {

    @GET("ajax.php?f=get_email_address")
    suspend fun getEmailAddress(
        @Query("lang") lang: String = "en",
        @Query("sid_token") sidToken: String? = null
    ): Response<GuerrillaAddressResponse>

    @GET("ajax.php?f=set_email_user")
    suspend fun setEmailUser(
        @Query("email_user") emailUser: String,
        @Query("lang") lang: String = "en",
        @Query("site") site: String? = null,
        @Query("sid_token") sidToken: String? = null
    ): Response<GuerrillaAddressResponse>

    @GET("ajax.php?f=check_email")
    suspend fun checkEmail(
        @Query("seq") seq: Int = 0,
        @Query("sid_token") sidToken: String? = null
    ): Response<GuerrillaCheckEmailResponse>

    @GET("ajax.php?f=fetch_email")
    suspend fun fetchEmail(
        @Query("email_id") emailId: String,
        @Query("sid_token") sidToken: String? = null
    ): Response<GuerrillaFetchEmailResponse>
}
