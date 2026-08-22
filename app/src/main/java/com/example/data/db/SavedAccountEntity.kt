package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_accounts")
data class SavedAccountEntity(
    @PrimaryKey
    val address: String,
    val password: String,
    val token: String? = null,
    val accountId: String? = null,
    val label: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (10 * 60 * 1000L),
    val isActive: Boolean = false,
    val serverUrl: String = "https://api.mail.tm"
)

