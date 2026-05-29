package com.example.vellum.data

import com.example.vellum.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface DataRepository {
    fun getTransactionsFlow(): Flow<List<TransactionEntity>>
    fun getTransactionsForPeriodFlow(start: Long, end: Long): Flow<List<TransactionEntity>>
    fun getCategoriesFlow(): Flow<List<CategoryEntity>>
    fun getAccountsFlow(): Flow<List<AccountEntity>>
    fun getPreferencesFlow(): Flow<List<PreferenceEntity>>

    suspend fun insertTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(transaction: TransactionEntity)
    suspend fun insertCategory(category: CategoryEntity)
    suspend fun deleteCategory(category: CategoryEntity)
    suspend fun insertAccount(account: AccountEntity)
    suspend fun deleteAccount(account: AccountEntity)
    suspend fun insertPreference(key: String, value: String)
    suspend fun getPreferenceValue(key: String): String?
    suspend fun tagLocalDataToUser(email: String)

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

    override suspend fun insertTransaction(transaction: TransactionEntity) =
        db.transactionDao().insertTransaction(transaction)

    override suspend fun deleteTransaction(transaction: TransactionEntity) =
        db.transactionDao().deleteTransaction(transaction)

    override suspend fun insertCategory(category: CategoryEntity) =
        db.categoryDao().insertCategory(category)

    override suspend fun deleteCategory(category: CategoryEntity) = withContext(Dispatchers.IO) {
        db.categoryDao().deleteCategory(category)
        db.transactionDao().updateTransactionsCategoryToGeneral(category.id)
    }

    override suspend fun insertAccount(account: AccountEntity) =
        db.accountDao().insertAccount(account)

    override suspend fun deleteAccount(account: AccountEntity) = withContext(Dispatchers.IO) {
        if (account.id == "default_account_personal") {
            db.preferenceDao().insertPreference(PreferenceEntity("default_account_personal_deleted", "true"))
        }
        db.accountDao().deleteAccount(account)
        db.transactionDao().deleteTransactionsForAccount(account.id)
    }

    override suspend fun insertPreference(key: String, value: String) =
        db.preferenceDao().insertPreference(PreferenceEntity(key, value))

    override suspend fun getPreferenceValue(key: String): String? =
        db.preferenceDao().getPreferenceValue(key)

    override suspend fun tagLocalDataToUser(email: String) = withContext(Dispatchers.IO) {
        db.transactionDao().tagTransactionsToUser(email)
        db.categoryDao().tagCategoriesToUser(email)
        db.accountDao().tagAccountsToUser(email)
    }

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

    override suspend fun clearLocalCache() {
        val sheetsUrl = db.preferenceDao().getPreferenceValue("sheets_url")
        db.transactionDao().deleteAllTransactions()
        db.categoryDao().deleteAllCategories()
        db.accountDao().deleteAllAccounts()
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
                CategoryEntity(id = "default_category_clothes", name = "Clothes", type = "EXPENSE", icon = "app_icon_clothes", isDefault = true),
                CategoryEntity(id = "default_category_eating_out", name = "Eating Out", type = "EXPENSE", icon = "app_icon_eating_out", isDefault = true),
                CategoryEntity(id = "default_category_entertainment", name = "Entertainment", type = "EXPENSE", icon = "app_icon_entertainment", isDefault = true),
                CategoryEntity(id = "default_category_fuel", name = "Fuel", type = "EXPENSE", icon = "app_icon_fuel", isDefault = true),
                CategoryEntity(id = "default_category_general", name = "General", type = "EXPENSE", icon = "app_icon_general", isDefault = true),
                CategoryEntity(id = "default_category_gifts", name = "Gifts", type = "EXPENSE", icon = "app_icon_gifts", isDefault = true),
                CategoryEntity(id = "default_category_holidays", name = "Holidays", type = "EXPENSE", icon = "app_icon_holidays", isDefault = true),
                CategoryEntity(id = "default_category_kids", name = "Kids", type = "EXPENSE", icon = "app_icon_kids", isDefault = true),
                CategoryEntity(id = "default_category_shopping", name = "Shopping", type = "EXPENSE", icon = "app_icon_shopping", isDefault = true),
                CategoryEntity(id = "default_category_sports", name = "Sports", type = "EXPENSE", icon = "app_icon_sports", isDefault = true),
                CategoryEntity(id = "default_category_travel", name = "Travel", type = "EXPENSE", icon = "app_icon_travel", isDefault = true),
                CategoryEntity(id = "default_category_salary", name = "Salary", type = "INCOME", icon = "app_icon_salary", isDefault = true)
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
