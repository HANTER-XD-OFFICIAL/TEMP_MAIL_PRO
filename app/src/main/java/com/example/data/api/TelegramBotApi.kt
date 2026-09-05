package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

@JsonClass(generateAdapter = true)
data class TelegramGetMeResponse(
    @Json(name = "ok") val ok: Boolean = false,
    @Json(name = "result") val result: TelegramBotInfo? = null,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramBotInfo(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "is_bot") val isBot: Boolean = true,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "can_join_groups") val canJoinGroups: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class TelegramSendMessageResponse(
    @Json(name = "ok") val ok: Boolean = false,
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramUpdatesResponse(
    @Json(name = "ok") val ok: Boolean = false,
    @Json(name = "result") val result: List<TelegramUpdateItem>? = emptyList(),
    @Json(name = "description") val description: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramUpdateItem(
    @Json(name = "update_id") val updateId: Long = 0,
    @Json(name = "message") val message: TelegramMessage? = null
)

@JsonClass(generateAdapter = true)
data class TelegramMessage(
    @Json(name = "message_id") val messageId: Long = 0,
    @Json(name = "chat") val chat: TelegramChat? = null,
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class TelegramChat(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "type") val type: String? = null
)

interface TelegramBotApi {
    @GET("bot{token}/getMe")
    suspend fun getMe(
        @Path("token") token: String
    ): Response<TelegramGetMeResponse>

    @FormUrlEncoded
    @POST("bot{token}/sendMessage")
    suspend fun sendMessage(
        @Path("token") token: String,
        @Field("chat_id") chatId: String,
        @Field("text") text: String,
        @Field("parse_mode") parseMode: String? = "HTML"
    ): Response<TelegramSendMessageResponse>

    @GET("bot{token}/getUpdates")
    suspend fun getUpdates(
        @Path("token") token: String,
        @Query("limit") limit: Int = 10
    ): Response<TelegramUpdatesResponse>
}
