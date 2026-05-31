package com.example.vellum.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    // ── Active-only queries (used by UI) ─────────────────────────────────────
    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0")
    suspend fun getAllActiveTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND timestamp >= :start AND timestamp <= :end ORDER BY timestamp DESC")
    fun getTransactionsForPeriodFlow(start: Long, end: Long): Flow<List<TransactionEntity>>

    // ── Full queries including tombstones (used by SheetsSyncManager) ────────
    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactions(): List<TransactionEntity>

    // ── Writes ────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    // Soft-delete: marks row as deleted, stamps updatedAt for LWW propagation
    @Query("UPDATE transactions SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: Long, updatedAt: Long)

    // Hard-delete: internal use only (clearLocalCache, sync replacement)
    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // ── Utility ───────────────────────────────────────────────────────────────
    @Query("UPDATE transactions SET userEmail = :email WHERE userEmail IS NULL OR userEmail = ''")
    suspend fun tagTransactionsToUser(email: String)

    @Query("UPDATE transactions SET categoryId = 'default_category_general', categoryName = 'General' WHERE categoryId = :categoryId")
    suspend fun updateTransactionsCategoryToGeneral(categoryId: String)

    @Query("DELETE FROM transactions WHERE accountId = :accountId")
    suspend fun deleteTransactionsForAccount(accountId: String)
}

@Dao
interface CategoryDao {
    // ── Active-only queries (used by UI) ─────────────────────────────────────
    @Query("SELECT * FROM categories WHERE isDeleted = 0")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE isDeleted = 0")
    suspend fun getAllActiveCategories(): List<CategoryEntity>

    // ── Full queries including tombstones (used by SheetsSyncManager) ────────
    @Query("SELECT * FROM categories")
    suspend fun getAllCategories(): List<CategoryEntity>

    // ── Writes ────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    // Soft-delete: marks row as deleted, stamps updatedAt for LWW propagation
    @Query("UPDATE categories SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: Long, updatedAt: Long)

    // Hard-delete: internal use only (clearLocalCache, sync replacement)
    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories")
    suspend fun deleteAllCategories()

    // ── Utility ───────────────────────────────────────────────────────────────
    @Query("UPDATE categories SET userEmail = :email WHERE userEmail IS NULL OR userEmail = ''")
    suspend fun tagCategoriesToUser(email: String)
}

@Dao
interface AccountDao {
    // ── Active-only queries (used by UI) ─────────────────────────────────────
    @Query("SELECT * FROM accounts WHERE isDeleted = 0")
    fun getAllAccountsFlow(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE isDeleted = 0")
    suspend fun getAllActiveAccounts(): List<AccountEntity>

    // ── Full queries including tombstones (used by SheetsSyncManager) ────────
    @Query("SELECT * FROM accounts")
    suspend fun getAllAccounts(): List<AccountEntity>

    // ── Writes ────────────────────────────────────────────────────────────────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    // Soft-delete: marks row as deleted, stamps updatedAt for LWW propagation
    // Does NOT cascade-delete linked transactions — caller handles that if needed.
    @Query("UPDATE accounts SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt, isSynced = 0 WHERE id = :id")
    suspend fun markDeleted(id: String, deletedAt: Long, updatedAt: Long)

    // Hard-delete: internal use only (clearLocalCache, leaveAccount, sync replacement)
    @Delete
    suspend fun deleteAccount(account: AccountEntity)

    @Query("DELETE FROM accounts")
    suspend fun deleteAllAccounts()

    // ── Utility ───────────────────────────────────────────────────────────────
    @Query("UPDATE accounts SET userEmail = :email WHERE userEmail IS NULL OR userEmail = ''")
    suspend fun tagAccountsToUser(email: String)
}

@Dao
interface PreferenceDao {
    @Query("SELECT * FROM preferences")
    fun getAllPreferencesFlow(): Flow<List<PreferenceEntity>>

    @Query("SELECT * FROM preferences")
    suspend fun getAllPreferences(): List<PreferenceEntity>

    @Query("SELECT value FROM preferences WHERE `key` = :key LIMIT 1")
    suspend fun getPreferenceValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: PreferenceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: List<PreferenceEntity>)

    @Query("DELETE FROM preferences")
    suspend fun deleteAllPreferences()
}
