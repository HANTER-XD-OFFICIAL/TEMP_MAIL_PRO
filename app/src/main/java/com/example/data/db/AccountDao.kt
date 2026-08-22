package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM saved_accounts ORDER BY lastUsedAt DESC")
    fun getAllAccounts(): Flow<List<SavedAccountEntity>>

    @Query("SELECT * FROM saved_accounts WHERE isActive = 1 LIMIT 1")
    fun getActiveAccount(): Flow<SavedAccountEntity?>

    @Query("SELECT * FROM saved_accounts WHERE address = :address LIMIT 1")
    suspend fun getAccountByAddress(address: String): SavedAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: SavedAccountEntity)

    @Update
    suspend fun updateAccount(account: SavedAccountEntity)

    @Query("DELETE FROM saved_accounts WHERE address = :address")
    suspend fun deleteAccount(address: String)

    @Query("UPDATE saved_accounts SET isActive = 0")
    suspend fun clearAllActiveFlags()

    @Query("UPDATE saved_accounts SET isActive = 1, lastUsedAt = :timestamp WHERE address = :address")
    suspend fun markActive(address: String, timestamp: Long = System.currentTimeMillis())

    @Transaction
    suspend fun switchActiveAccount(address: String) {
        clearAllActiveFlags()
        markActive(address, System.currentTimeMillis())
    }

    @Query("SELECT COUNT(*) FROM saved_accounts")
    fun getAccountsCount(): Flow<Int>

    @Query("UPDATE saved_accounts SET expiresAt = :newExpiresAt WHERE address = :address")
    suspend fun updateExpiration(address: String, newExpiresAt: Long)
}

