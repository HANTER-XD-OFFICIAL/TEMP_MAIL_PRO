package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DomainListResponse(
    @Json(name = "hydra:member")
    val member: List<DomainItem> = emptyList(),
    @Json(name = "hydra:totalItems")
    val totalItems: Int = 0
)

@JsonClass(generateAdapter = true)
data class DomainItem(
    @Json(name = "id") val id: String,
    @Json(name = "domain") val domain: String,
    @Json(name = "isActive") val isActive: Boolean = true,
    @Json(name = "isPrivate") val isPrivate: Boolean = false,
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateAccountRequest(
    @Json(name = "address") val address: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AccountResponse(
    @Json(name = "id") val id: String,
    @Json(name = "address") val address: String,
    @Json(name = "quota") val quota: Long = 0,
    @Json(name = "used") val used: Long = 0,
    @Json(name = "isDisabled") val isDisabled: Boolean = false,
    @Json(name = "isDeleted") val isDeleted: Boolean = false,
    @Json(name = "createdAt") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class TokenRequest(
    @Json(name = "address") val address: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class TokenResponse(
    @Json(name = "token") val token: String,
    @Json(name = "id") val id: String
)

@JsonClass(generateAdapter = true)
data class MessageListResponse(
    @Json(name = "hydra:member")
    val member: List<MessageHeaderItem> = emptyList(),
    @Json(name = "hydra:totalItems")
    val totalItems: Int = 0
)

@JsonClass(generateAdapter = true)
data class EmailParticipant(
    @Json(name = "address") val address: String = "",
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageHeaderItem(
    @Json(name = "id") val id: String,
    @Json(name = "accountId") val accountId: String = "",
    @Json(name = "msgid") val msgid: String? = null,
    @Json(name = "from") val from: EmailParticipant = EmailParticipant(),
    @Json(name = "to") val to: List<EmailParticipant> = emptyList(),
    @Json(name = "subject") val subject: String? = null,
    @Json(name = "intro") val intro: String? = null,
    @Json(name = "seen") val seen: Boolean = false,
    @Json(name = "isDeleted") val isDeleted: Boolean = false,
    @Json(name = "hasAttachments") val hasAttachments: Boolean = false,
    @Json(name = "size") val size: Long = 0,
    @Json(name = "createdAt") val createdAt: String = ""
)

@JsonClass(generateAdapter = true)
data class AttachmentItem(
    @Json(name = "id") val id: String,
    @Json(name = "filename") val filename: String = "",
    @Json(name = "contentType") val contentType: String = "",
    @Json(name = "disposition") val disposition: String? = null,
    @Json(name = "size") val size: Long = 0,
    @Json(name = "downloadUrl") val downloadUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageDetailResponse(
    @Json(name = "id") val id: String,
    @Json(name = "accountId") val accountId: String = "",
    @Json(name = "msgid") val msgid: String? = null,
    @Json(name = "from") val from: EmailParticipant = EmailParticipant(),
    @Json(name = "to") val to: List<EmailParticipant> = emptyList(),
    @Json(name = "subject") val subject: String? = null,
    @Json(name = "intro") val intro: String? = null,
    @Json(name = "seen") val seen: Boolean = false,
    @Json(name = "isDeleted") val isDeleted: Boolean = false,
    @Json(name = "hasAttachments") val hasAttachments: Boolean = false,
    @Json(name = "size") val size: Long = 0,
    @Json(name = "createdAt") val createdAt: String = "",
    @Json(name = "text") val text: String? = null,
    @Json(name = "html") val html: List<String>? = null,
    @Json(name = "attachments") val attachments: List<AttachmentItem>? = null
)
