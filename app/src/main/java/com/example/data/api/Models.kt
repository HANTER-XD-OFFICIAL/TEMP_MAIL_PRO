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

@JsonClass(generateAdapter = true)
data class SecMailMessageHeader(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "from") val from: String = "",
    @Json(name = "subject") val subject: String = "",
    @Json(name = "date") val date: String = ""
)

@JsonClass(generateAdapter = true)
data class SecMailAttachment(
    @Json(name = "filename") val filename: String = "",
    @Json(name = "contentType") val contentType: String = "",
    @Json(name = "size") val size: Long = 0
)

@JsonClass(generateAdapter = true)
data class SecMailMessageDetail(
    @Json(name = "id") val id: Long = 0,
    @Json(name = "from") val from: String = "",
    @Json(name = "subject") val subject: String = "",
    @Json(name = "date") val date: String = "",
    @Json(name = "attachments") val attachments: List<SecMailAttachment> = emptyList(),
    @Json(name = "body") val body: String? = null,
    @Json(name = "textBody") val textBody: String? = null,
    @Json(name = "htmlBody") val htmlBody: String? = null
)

@JsonClass(generateAdapter = true)
data class GuerrillaAddressResponse(
    @Json(name = "email_addr") val emailAddr: String = "",
    @Json(name = "email_timestamp") val emailTimestamp: Long = 0,
    @Json(name = "alias") val alias: String = "",
    @Json(name = "sid_token") val sidToken: String = ""
)

@JsonClass(generateAdapter = true)
data class GuerrillaMailItem(
    @Json(name = "mail_id") val mailId: String = "",
    @Json(name = "mail_from") val mailFrom: String = "",
    @Json(name = "mail_subject") val mailSubject: String = "",
    @Json(name = "mail_excerpt") val mailExcerpt: String = "",
    @Json(name = "mail_timestamp") val mailTimestamp: String = "",
    @Json(name = "mail_read") val mailRead: String = "0",
    @Json(name = "mail_date") val mailDate: String = "",
    @Json(name = "att") val att: String = "0",
    @Json(name = "mail_size") val mailSize: String = "0"
)

@JsonClass(generateAdapter = true)
data class GuerrillaCheckEmailResponse(
    @Json(name = "list") val list: List<GuerrillaMailItem> = emptyList(),
    @Json(name = "count") val count: String = "0",
    @Json(name = "email") val email: String = "",
    @Json(name = "alias") val alias: String = "",
    @Json(name = "sid_token") val sidToken: String = ""
)

@JsonClass(generateAdapter = true)
data class GuerrillaFetchEmailResponse(
    @Json(name = "mail_id") val mailId: String = "",
    @Json(name = "mail_from") val mailFrom: String = "",
    @Json(name = "mail_recipient") val mailRecipient: String = "",
    @Json(name = "mail_subject") val mailSubject: String = "",
    @Json(name = "mail_excerpt") val mailExcerpt: String = "",
    @Json(name = "mail_body") val mailBody: String = "",
    @Json(name = "mail_timestamp") val mailTimestamp: String = "",
    @Json(name = "mail_date") val mailDate: String = ""
)

// ==================== GETNADA / INBOXES.COM MODELS ====================

@JsonClass(generateAdapter = true)
data class GetnadaDomainItem(
    @Json(name = "qdn") val qdn: String? = null,
    @Json(name = "name") val name: String? = null
)

@JsonClass(generateAdapter = true)
data class GetnadaDomainsResponse(
    @Json(name = "domains") val domains: List<GetnadaDomainItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GetnadaInboxCreateResponse(
    @Json(name = "inbox") val inbox: String? = null
)

@JsonClass(generateAdapter = true)
data class GetnadaMessageItem(
    @Json(name = "uid") val uid: String = "",
    @Json(name = "f") val f: String? = null,
    @Json(name = "fe") val fe: String? = null,
    @Json(name = "s") val s: String? = null,
    @Json(name = "r") val r: Long? = null,
    @Json(name = "ibx") val ibx: String? = null,
    @Json(name = "text") val text: String? = null,
    @Json(name = "html") val html: String? = null
)

@JsonClass(generateAdapter = true)
data class GetnadaInboxResponse(
    @Json(name = "msgs") val msgs: List<GetnadaMessageItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GetnadaMessageDetailResponse(
    @Json(name = "msg") val msg: GetnadaMessageItem? = null
)

@JsonClass(generateAdapter = true)
data class GetnadaDeleteRequest(
    @Json(name = "ids") val ids: List<String> = emptyList()
)

// ==================== TEMP-MAIL (RAPIDAPI) MODELS ====================

@JsonClass(generateAdapter = true)
data class RapidApiAttachment(
    @Json(name = "filename") val filename: String = "",
    @Json(name = "size") val size: Long = 0
)

@JsonClass(generateAdapter = true)
data class RapidApiTempMailItem(
    @Json(name = "mail_id") val mailId: String = "",
    @Json(name = "mail_address_id") val mailAddressId: String = "",
    @Json(name = "mail_from") val mailFrom: String = "",
    @Json(name = "mail_subject") val mailSubject: String = "",
    @Json(name = "mail_preview") val mailPreview: String = "",
    @Json(name = "mail_text_only") val mailTextOnly: String? = null,
    @Json(name = "mail_text") val mailText: String? = null,
    @Json(name = "mail_html") val mailHtml: String? = null,
    @Json(name = "mail_timestamp") val mailTimestamp: Double? = null,
    @Json(name = "attachments") val attachments: List<RapidApiAttachment>? = null
)

// ==================== MAILDROP (GRAPHQL) MODELS ====================

@JsonClass(generateAdapter = true)
data class MaildropGraphqlRequest(
    @Json(name = "query") val query: String,
    @Json(name = "variables") val variables: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class MaildropGraphqlResponse(
    @Json(name = "data") val data: MaildropData? = null
)

@JsonClass(generateAdapter = true)
data class MaildropData(
    @Json(name = "inbox") val inbox: List<MaildropInboxItem>? = null,
    @Json(name = "message") val message: MaildropMessageDetail? = null,
    @Json(name = "delete") val delete: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class MaildropInboxItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "subject") val subject: String? = null,
    @Json(name = "date") val date: String? = null,
    @Json(name = "headerfrom") val headerfrom: String? = null
)

@JsonClass(generateAdapter = true)
data class MaildropMessageDetail(
    @Json(name = "id") val id: String = "",
    @Json(name = "date") val date: String? = null,
    @Json(name = "mailfrom") val mailfrom: String? = null,
    @Json(name = "headerfrom") val headerfrom: String? = null,
    @Json(name = "subject") val subject: String? = null,
    @Json(name = "data") val data: String? = null,
    @Json(name = "html") val html: String? = null
)



