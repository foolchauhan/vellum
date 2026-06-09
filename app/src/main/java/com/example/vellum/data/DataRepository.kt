package com.example.vellum.data

import com.example.vellum.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Locale

interface DataRepository {
    fun getTransactionsFlow(): Flow<List<TransactionEntity>>
    fun getTransactionsForPeriodFlow(start: Long, end: Long): Flow<List<TransactionEntity>>
    fun getCategoriesFlow(): Flow<List<CategoryEntity>>
    fun getAccountsFlow(): Flow<List<AccountEntity>>
    fun getPreferencesFlow(): Flow<List<PreferenceEntity>>
    fun getStickyNotesFlow(): Flow<List<StickyNoteEntity>>
    fun getStickyNotesForUserFlow(email: String?): Flow<List<StickyNoteEntity>>

    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun insertAccount(account: AccountEntity)
    suspend fun insertStickyNote(note: StickyNoteEntity)
    suspend fun insertPreference(key: String, value: String)
    suspend fun getPreferenceValue(key: String): String?
    suspend fun tagLocalDataToUser(email: String)

    // User-facing soft deletes (tombstone — row survives for sync propagation)
    suspend fun softDeleteTransaction(transaction: TransactionEntity)
    suspend fun softDeleteCategory(category: CategoryEntity)
    suspend fun softDeleteAccount(account: AccountEntity)
    suspend fun softDeleteStickyNote(note: StickyNoteEntity)

    // Non-owner leave: removes account row locally only, no transaction cascade
    suspend fun leaveAccount(account: AccountEntity)

    // Bulk upload transactions from CSV text (auto-creates categories/accounts)
    suspend fun bulkUploadTransactions(csvText: String): Pair<Int, Int>

    // Sync methods
    suspend fun syncWithSheets(webAppUrl: String, email: String, displayName: String? = null, photoUrl: String? = null, onComplete: (() -> Unit)? = null)
    suspend fun joinSharedAccount(webAppUrl: String, email: String, shareCode: String): Boolean
    suspend fun clearLocalCache()
    suspend fun checkAndSeedDefaults()
}

class DefaultDataRepository(private val db: VellumDatabase) : DataRepository {
    override fun getTransactionsFlow(): Flow<List<TransactionEntity>> =
        db.transactionDao().getAllTransactionsFlow()

    override fun getTransactionsForPeriodFlow(start: Long, end: Long): Flow<List<TransactionEntity>> =
        db.transactionDao().getTransactionsForPeriodFlow(start, end)

    override fun getCategoriesFlow(): Flow<List<CategoryEntity>> =
        db.categoryDao().getAllCategoriesFlow()

    override fun getAccountsFlow(): Flow<List<AccountEntity>> =
        db.accountDao().getAllAccountsFlow()

    override fun getPreferencesFlow(): Flow<List<PreferenceEntity>> =
        db.preferenceDao().getAllPreferencesFlow()

    override fun getStickyNotesFlow(): Flow<List<StickyNoteEntity>> =
        db.stickyNoteDao().getAllStickyNotesFlow()

    override fun getStickyNotesForUserFlow(email: String?): Flow<List<StickyNoteEntity>> =
        if (email == null) {
            db.stickyNoteDao().getLocalStickyNotesFlow()
        } else {
            db.stickyNoteDao().getStickyNotesForUserFlow(email)
        }

    override suspend fun insertTransaction(transaction: TransactionEntity) =
        db.transactionDao().insertTransaction(transaction)

    override suspend fun insertCategory(category: CategoryEntity) =
        db.categoryDao().insertCategory(category)

    override suspend fun insertAccount(account: AccountEntity) =
        db.accountDao().insertAccount(account)

    override suspend fun insertStickyNote(note: StickyNoteEntity) =
        db.stickyNoteDao().insertStickyNote(note)

    // ── Soft deletes (user-facing) ────────────────────────────────────────────

    override suspend fun softDeleteTransaction(transaction: TransactionEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.transactionDao().markDeleted(transaction.id, deletedAt = now, updatedAt = now)
    }

    override suspend fun softDeleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.categoryDao().markDeleted(category.id, deletedAt = now, updatedAt = now)
        // Reassign linked transactions to General so they stay visible
        db.transactionDao().updateTransactionsCategoryToGeneral(category.id)
    }

    override suspend fun softDeleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        if (account.id == "default_account_personal") {
            db.preferenceDao().insertPreference(PreferenceEntity("default_account_personal_deleted", "true"))
        }
        val now = System.currentTimeMillis()
        db.accountDao().markDeleted(account.id, deletedAt = now, updatedAt = now)
        // NOTE: transactions are NOT cascade-deleted. They remain visible under "(Deleted Account)".
    }

    override suspend fun softDeleteStickyNote(note: StickyNoteEntity) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        db.stickyNoteDao().markDeleted(note.id, deletedAt = now, updatedAt = now)
    }

    // ── Leave shared account (non-owner) ─────────────────────────────────────
    // Removes the account row locally only. Transactions are preserved.
    // Caller (ViewModel) writes the account ID to "left_accounts" preference before calling this.
    override suspend fun leaveAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        db.accountDao().deleteAccount(account)
    }

    override suspend fun bulkUploadTransactions(csvText: String): Pair<Int, Int> = withContext(Dispatchers.IO) {
        var success = 0
        var failure = 0
        try {
            val lines = csvText.split("\n")
            if (lines.isEmpty()) return@withContext Pair(0, 0)
            
            // Try to find headers
            val firstLine = lines.firstOrNull() ?: return@withContext Pair(0, 0)
            val headers = parseCsvLine(firstLine).map { it.trim().lowercase() }
            val hasHeaders = headers.contains("amount") || headers.contains("type") || headers.contains("category") || headers.contains("account")
            
            val startIndex = if (hasHeaders) 1 else 0
            val activeCats = db.categoryDao().getAllActiveCategories().toMutableList()
            val activeAccs = db.accountDao().getAllActiveAccounts().toMutableList()
            
            db.runInTransaction {
                runBlocking {
                    for (i in startIndex until lines.size) {
                        val line = lines[i]
                        if (line.trim().isEmpty()) continue
                        val parts = parseCsvLine(line)
                        if (parts.isEmpty()) continue
                        
                        try {
                            var dateStr = ""
                            var typeStr = ""
                            var amountStr = ""
                            var catNameStr = ""
                            var accNameStr = ""
                            var noteStr = ""
                            
                            if (hasHeaders) {
                                headers.forEachIndexed { idx, header ->
                                    if (idx < parts.size) {
                                        val valStr = parts[idx].trim()
                                        when (header) {
                                            "date" -> dateStr = valStr
                                            "type" -> typeStr = valStr
                                            "amount" -> amountStr = valStr
                                            "category" -> catNameStr = valStr
                                            "account" -> accNameStr = valStr
                                            "note" -> noteStr = valStr
                                        }
                                    }
                                }
                            } else {
                                if (parts.isNotEmpty()) dateStr = parts[0].trim()
                                if (parts.size > 1) typeStr = parts[1].trim()
                                if (parts.size > 2) amountStr = parts[2].trim()
                                if (parts.size > 3) catNameStr = parts[3].trim()
                                if (parts.size > 4) accNameStr = parts[4].trim()
                                if (parts.size > 5) noteStr = parts[5].trim()
                            }
                            
                            val parsedAmount = amountStr.toDoubleOrNull() ?: throw IllegalArgumentException("Invalid amount")
                            if (parsedAmount <= 0) throw IllegalArgumentException("Amount must be positive")
                            
                            val type = if (typeStr.uppercase() == "INCOME") "INCOME" else "EXPENSE"
                            val categoryName = catNameStr.takeIf { it.isNotEmpty() } ?: "General"
                            val accountName = accNameStr.takeIf { it.isNotEmpty() } ?: "Personal"
                            val note = noteStr
                            
                            val timestamp = parseCsvDate(dateStr)
                            
                            // Find or create category
                            var category = activeCats.find { it.name.trim().lowercase() == categoryName.trim().lowercase() && it.type == type }
                            if (category == null) {
                                category = CategoryEntity(
                                    name = categoryName.trim(),
                                    type = type,
                                    icon = "general",
                                    isDefault = false,
                                    chartColor = "#4E3C30",
                                    isSynced = false,
                                    updatedAt = System.currentTimeMillis()
                                )
                                db.categoryDao().insertCategory(category)
                                activeCats.add(category)
                            }
                            
                            // Find or create account
                            var account = activeAccs.find { it.name.trim().lowercase() == accountName.trim().lowercase() }
                            if (account == null) {
                                account = AccountEntity(
                                    name = accountName.trim(),
                                    icon = "personal",
                                    isDefault = false,
                                    color = "#4E3C30",
                                    isSynced = false,
                                    updatedAt = System.currentTimeMillis()
                                )
                                db.accountDao().insertAccount(account)
                                activeAccs.add(account)
                            }
                            
                            val tx = TransactionEntity(
                                amount = parsedAmount,
                                type = type,
                                categoryId = category.id,
                                categoryName = category.name,
                                accountId = account.id,
                                accountName = account.name,
                                note = note,
                                timestamp = timestamp,
                                isSynced = false,
                                updatedAt = System.currentTimeMillis()
                            )
                            db.transactionDao().insertTransaction(tx)
                            success++
                        } catch (e: Exception) {
                            failure++
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Entire process failed
        }
        Pair(success, failure)
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = java.lang.StringBuilder()
        var inQuotes = false
        for (ch in line) {
            if (ch == '\"') {
                inQuotes = !inQuotes
            } else if (ch == ',' && !inQuotes) {
                result.add(current.toString())
                current = java.lang.StringBuilder()
            } else {
                current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun parseCsvDate(dateStr: String): Long {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("dd/MM/yyyy", Locale.US),
            SimpleDateFormat("MM/dd/yyyy", Locale.US),
            SimpleDateFormat("dd-MM-yyyy", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US)
        )
        for (format in formats) {
            try {
                format.isLenient = false
                return format.parse(dateStr)?.time ?: continue
            } catch (e: Exception) {
                // try next
            }
        }
        return System.currentTimeMillis()
    }


    // ── Preferences ───────────────────────────────────────────────────────────
    override suspend fun insertPreference(key: String, value: String) =
        db.preferenceDao().insertPreference(PreferenceEntity(key, value))

    override suspend fun getPreferenceValue(key: String): String? =
        db.preferenceDao().getPreferenceValue(key)

    override suspend fun tagLocalDataToUser(email: String) = withContext(Dispatchers.IO) {
        db.transactionDao().tagTransactionsToUser(email)
        db.categoryDao().tagCategoriesToUser(email)
        db.accountDao().tagAccountsToUser(email)
        db.stickyNoteDao().tagStickyNotesToUser(email)
    }

    // ── Sync ──────────────────────────────────────────────────────────────────
    override suspend fun syncWithSheets(webAppUrl: String, email: String, displayName: String?, photoUrl: String?, onComplete: (() -> Unit)?) {
        SheetsSyncManager.sync(webAppUrl, email, displayName, photoUrl, db, onComplete)
    }

    override suspend fun joinSharedAccount(webAppUrl: String, email: String, shareCode: String): Boolean {
        val account = SheetsSyncManager.joinAccount(webAppUrl, email, shareCode)
        return if (account != null) {
            db.accountDao().insertAccount(account)
            true
        } else {
            false
        }
    }

    // ── Cache management ──────────────────────────────────────────────────────
    override suspend fun clearLocalCache() {
        val sheetsUrl = db.preferenceDao().getPreferenceValue("sheets_url")
        db.transactionDao().deleteAllTransactions()
        db.categoryDao().deleteAllCategories()
        db.accountDao().deleteAllAccounts()
        db.stickyNoteDao().deleteAllStickyNotes()
        db.preferenceDao().deleteAllPreferences()
        VellumDatabase.prepopulateDatabase(db)
        if (sheetsUrl != null) {
            db.preferenceDao().insertPreference(PreferenceEntity("sheets_url", sheetsUrl))
        }
    }

    override suspend fun checkAndSeedDefaults() {
        val allCats = db.categoryDao().getAllCategories()
        if (allCats.isEmpty()) {
            VellumDatabase.prepopulateDatabase(db)
        } else {
            val defaultCats = listOf(
                CategoryEntity(id = "default_category_clothes", name = "Clothes", type = "EXPENSE", icon = "app_icon_clothes", chartColor = "#E91E63", isDefault = true),
                CategoryEntity(id = "default_category_eating_out", name = "Eating Out", type = "EXPENSE", icon = "app_icon_eating_out", chartColor = "#FF9800", isDefault = true),
                CategoryEntity(id = "default_category_entertainment", name = "Entertainment", type = "EXPENSE", icon = "app_icon_entertainment", chartColor = "#9C27B0", isDefault = true),
                CategoryEntity(id = "default_category_fuel", name = "Fuel", type = "EXPENSE", icon = "app_icon_fuel", chartColor = "#FFEB3B", isDefault = true),
                CategoryEntity(id = "default_category_general", name = "General", type = "EXPENSE", icon = "app_icon_general", chartColor = "#4E3C30", isDefault = true),
                CategoryEntity(id = "default_category_gifts", name = "Gifts", type = "EXPENSE", icon = "app_icon_gifts", chartColor = "#E040FB", isDefault = true),
                CategoryEntity(id = "default_category_holidays", name = "Holidays", type = "EXPENSE", icon = "app_icon_holidays", chartColor = "#00BCD4", isDefault = true),
                CategoryEntity(id = "default_category_kids", name = "Kids", type = "EXPENSE", icon = "app_icon_kids", chartColor = "#4CAF50", isDefault = true),
                CategoryEntity(id = "default_category_shopping", name = "Shopping", type = "EXPENSE", icon = "app_icon_shopping", chartColor = "#E57373", isDefault = true),
                CategoryEntity(id = "default_category_sports", name = "Sports", type = "EXPENSE", icon = "app_icon_sports", chartColor = "#3F51B5", isDefault = true),
                CategoryEntity(id = "default_category_travel", name = "Travel", type = "EXPENSE", icon = "app_icon_travel", chartColor = "#03A9F4", isDefault = true),
                CategoryEntity(id = "default_category_salary", name = "Salary", type = "INCOME", icon = "app_icon_salary", chartColor = "#009688", isDefault = true)
            )
            val missingCats = defaultCats.filter { default -> allCats.none { it.id == default.id } }
            if (missingCats.isNotEmpty()) {
                db.categoryDao().insertCategories(missingCats)
            }

            val allAccs = db.accountDao().getAllAccounts()
            val personalDeleted = db.preferenceDao().getPreferenceValue("default_account_personal_deleted") == "true"
            if (!personalDeleted && allAccs.none { it.id == "default_account_personal" }) {
                db.accountDao().insertAccount(
                    AccountEntity(id = "default_account_personal", name = "Personal", icon = "app_icon_personal", isDefault = true)
                )
            }
        }
    }
}
