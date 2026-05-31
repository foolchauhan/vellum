package com.example.vellum.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vellum.data.DataRepository
import com.example.vellum.data.SyncConstants
import com.example.vellum.data.local.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModel(private val repository: DataRepository) : ViewModel() {

    // Google Sign-In and Sync States
    private val _userEmail = MutableStateFlow<String?>(null)
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _userDisplayName = MutableStateFlow<String?>(null)
    val userDisplayName: StateFlow<String?> = _userDisplayName.asStateFlow()

    private val _userPhotoUrl = MutableStateFlow<String?>(null)
    val userPhotoUrl: StateFlow<String?> = _userPhotoUrl.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    // Current Date Selection Offset (in months/weeks/days depending on time_period preference)
    private val _currentPeriodOffset = MutableStateFlow(0)
    val currentPeriodOffset: StateFlow<Int> = _currentPeriodOffset.asStateFlow()

    // Active preferences loaded from Room
    val preferences: StateFlow<Map<String, String>> = repository.getPreferencesFlow()
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Current Time Period setting ("Daily", "Weekly", "Monthly", "Yearly")
    val timePeriod: StateFlow<String> = preferences
        .map { it["time_period"] ?: "Monthly" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Monthly")

    // Dynamic state of active time period range (start and end timestamps)
    val activePeriodRange: StateFlow<Pair<Long, Long>> = combine(currentPeriodOffset, timePeriod) { offset, period ->
        calculatePeriodRange(offset, period)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = calculatePeriodRange(0, "Monthly")
    )

    // Display string for the current period (e.g. "May", "May 28, 2026", "2026")
    val periodLabel: StateFlow<String> = combine(currentPeriodOffset, timePeriod) { offset, period ->
        calculatePeriodLabel(offset, period)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Filter selections
    private val _selectedFilterCategory = MutableStateFlow<CategoryEntity?>(null)
    val selectedFilterCategory: StateFlow<CategoryEntity?> = _selectedFilterCategory.asStateFlow()

    private val _selectedFilterAccount = MutableStateFlow<AccountEntity?>(null)
    val selectedFilterAccount: StateFlow<AccountEntity?> = _selectedFilterAccount.asStateFlow()

    // Expose lists
    val transactions: StateFlow<List<TransactionEntity>> = combine(
        activePeriodRange,
        _selectedFilterCategory,
        _selectedFilterAccount
    ) { range, filterCat, filterAcc ->
        Pair(range, Pair(filterCat, filterAcc))
    }.flatMapLatest { (range, filters) ->
        val (filterCat, filterAcc) = filters
        repository.getTransactionsForPeriodFlow(range.first, range.second)
            .map { list ->
                list.filter { tx ->
                    val matchCat = filterCat == null || tx.categoryId == filterCat.id
                    val matchAcc = filterAcc == null || tx.accountId == filterAcc.id
                    matchCat && matchAcc
                }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.getCategoriesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.getAccountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.getTransactionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var hasInitializedDefaultAccount = false

    init {
        viewModelScope.launch {
            repository.checkAndSeedDefaults()
        }
        viewModelScope.launch {
            accounts.collect { accList ->
                if (!hasInitializedDefaultAccount && accList.isNotEmpty()) {
                    val defaultAcc = accList.find { it.isDefault } ?: accList.find { it.name.lowercase() == "personal" }
                    if (defaultAcc != null) {
                        _selectedFilterAccount.value = defaultAcc
                        hasInitializedDefaultAccount = true
                    }
                }
            }
        }
    }

    // Metrics aggregates for the Spending screen
    val spendingMetrics = transactions.map { list ->
        var income = 0.0
        var expense = 0.0
        list.forEach { tx ->
            if (tx.type == "INCOME") {
                income += tx.amount
            } else {
                expense += tx.amount
            }
        }
        SpendingMetrics(income, expense, income - expense)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SpendingMetrics(0.0, 0.0, 0.0)
    )

    fun setFilterCategory(category: CategoryEntity?) {
        _selectedFilterCategory.value = category
    }

    fun setFilterAccount(account: AccountEntity?) {
        _selectedFilterAccount.value = account
    }

    fun clearFilters() {
        _selectedFilterCategory.value = null
        _selectedFilterAccount.value = null
    }

    fun navigatePeriod(forward: Boolean) {
        _currentPeriodOffset.value += if (forward) 1 else -1
    }

    fun resetPeriodOffset() {
        _currentPeriodOffset.value = 0
    }

    private fun autoSyncIfEnabled() {
        if (preferences.value["auto_backup"] == "On") {
            syncWithSheets()
        }
    }

    fun updatePreference(key: String, value: String) {
        viewModelScope.launch {
            repository.insertPreference(key, value)
            syncWithSheets()
        }
    }

    fun addTransaction(
        amount: Double,
        type: String,
        categoryId: String,
        categoryName: String,
        accountId: String,
        accountName: String,
        note: String,
        timestamp: Long = System.currentTimeMillis(),
        id: String? = null
    ) {
        viewModelScope.launch {
            val tx = if (id != null) {
                TransactionEntity(
                    id = id,
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    accountId = accountId,
                    accountName = accountName,
                    note = note,
                    timestamp = timestamp,
                    userEmail = _userEmail.value,
                    isSynced = false
                )
            } else {
                TransactionEntity(
                    amount = amount,
                    type = type,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    accountId = accountId,
                    accountName = accountName,
                    note = note,
                    timestamp = timestamp,
                    userEmail = _userEmail.value,
                    isSynced = false
                )
            }
            repository.insertTransaction(tx)
            syncWithSheets()
        }
    }

    fun addCategory(
        name: String,
        type: String,
        icon: String,
        chartColor: String,
        id: String? = null,
        isDefault: Boolean = false
    ) {
        viewModelScope.launch {
            val cat = if (id != null) {
                CategoryEntity(
                    id = id,
                    name = name,
                    type = type,
                    icon = icon,
                    isDefault = isDefault,
                    chartColor = chartColor,
                    userEmail = _userEmail.value,
                    isSynced = false
                )
            } else {
                CategoryEntity(
                    name = name,
                    type = type,
                    icon = icon,
                    isDefault = isDefault,
                    chartColor = chartColor,
                    userEmail = _userEmail.value,
                    isSynced = false
                )
            }
            repository.insertCategory(cat)
            syncWithSheets()
        }
    }

    fun addAccount(
        name: String,
        icon: String,
        color: String,
        id: String? = null,
        isDefault: Boolean = false,
        shareCode: String? = null,
        ownerEmail: String? = null
    ) {
        viewModelScope.launch {
            val acc = if (id != null) {
                AccountEntity(
                    id = id,
                    name = name,
                    icon = icon,
                    isDefault = isDefault,
                    color = color,
                    shareCode = shareCode,
                    ownerEmail = ownerEmail,
                    userEmail = _userEmail.value,
                    isSynced = false
                )
            } else {
                AccountEntity(
                    name = name,
                    icon = icon,
                    isDefault = isDefault,
                    color = color,
                    shareCode = shareCode,
                    ownerEmail = ownerEmail,
                    userEmail = _userEmail.value,
                    isSynced = false
                )
            }
            repository.insertAccount(acc)
            syncWithSheets()
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            // Track tombstone ID so sync can propagate the delete to the sheet
            val currentDeleted = repository.getPreferenceValue("deleted_transactions") ?: ""
            val deletedList = if (currentDeleted.isEmpty()) mutableListOf() else currentDeleted.split(",").toMutableList()
            if (!deletedList.contains(transaction.id)) {
                deletedList.add(transaction.id)
                repository.insertPreference("deleted_transactions", deletedList.joinToString(","))
            }
            repository.softDeleteTransaction(transaction)
            syncWithSheets()
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            // Track tombstone ID so sync can propagate the delete to the sheet
            val currentDeleted = repository.getPreferenceValue("deleted_categories") ?: ""
            val deletedList = if (currentDeleted.isEmpty()) mutableListOf() else currentDeleted.split(",").toMutableList()
            if (!deletedList.contains(category.id)) {
                deletedList.add(category.id)
                repository.insertPreference("deleted_categories", deletedList.joinToString(","))
            }
            repository.softDeleteCategory(category)
            syncWithSheets()
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            val currentDeleted = repository.getPreferenceValue("deleted_accounts") ?: ""
            val deletedList = if (currentDeleted.isEmpty()) mutableListOf() else currentDeleted.split(",").toMutableList()
            if (!deletedList.contains(account.id)) {
                deletedList.add(account.id)
                repository.insertPreference("deleted_accounts", deletedList.joinToString(","))
            }
            repository.softDeleteAccount(account)
            syncWithSheets()
        }
    }

    fun leaveSharedAccount(account: AccountEntity) {
        viewModelScope.launch {
            val currentLeft = repository.getPreferenceValue("left_accounts") ?: ""
            val leftList = if (currentLeft.isEmpty()) mutableListOf() else currentLeft.split(",").toMutableList()
            if (!leftList.contains(account.id)) {
                leftList.add(account.id)
                repository.insertPreference("left_accounts", leftList.joinToString(","))
            }
            repository.leaveAccount(account)
            syncWithSheets()
        }
    }

    fun signIn(email: String, displayName: String?, photoUrl: String?) {
        _userEmail.value = email
        _userDisplayName.value = displayName
        _userPhotoUrl.value = photoUrl
        hasInitializedDefaultAccount = true // Prevent auto-selecting Personal after sync updates
        viewModelScope.launch {
            repository.tagLocalDataToUser(email)
            syncWithSheets()
            // Ensure after sign in we show all accounts
            _selectedFilterAccount.value = null
            _selectedFilterCategory.value = null
        }
    }

    fun signOut() {
        _userEmail.value = null
        _userDisplayName.value = null
        _userPhotoUrl.value = null
        hasInitializedDefaultAccount = false
        _selectedFilterAccount.value = null
        _selectedFilterCategory.value = null
        viewModelScope.launch {
            repository.clearLocalCache()
        }
    }

    fun syncWithSheets() {
        val email = _userEmail.value ?: return
        val url = preferences.value["sheets_url"]?.takeIf { it.isNotEmpty() } ?: SyncConstants.DEFAULT_WEB_APP_URL
        if (url.trim().isEmpty()) return

        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            try {
                repository.syncWithSheets(url, email, _userDisplayName.value, _userPhotoUrl.value) {
                    // After sync: reset account filter to "All Accounts" so all synced
                    // transactions (including shared accounts) are immediately visible
                    _selectedFilterAccount.value = null
                    _selectedFilterCategory.value = null
                }
            } catch (e: Exception) {
                _syncError.value = e.message ?: "Unknown sync error"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun bulkUploadTransactions(csvText: String, onCompleted: (success: Int, failure: Int) -> Unit) {
        viewModelScope.launch {
            val result = repository.bulkUploadTransactions(csvText)
            syncWithSheets()
            onCompleted(result.first, result.second)
        }
    }

    fun createSharedAccount(name: String, color: String) {
        val email = _userEmail.value ?: return
        val shareCode = "FAM" + (100..999).random()
        viewModelScope.launch {
            repository.insertAccount(
                AccountEntity(
                    name = name,
                    icon = "app_icon_personal",
                    isDefault = false,
                    color = color,
                    shareCode = shareCode,
                    ownerEmail = email,
                    userEmail = email,
                    isSynced = false
                )
            )
            syncWithSheets()
        }
    }

    fun joinSharedAccount(shareCode: String, onResult: (Boolean) -> Unit) {
        val email = _userEmail.value
        val url = preferences.value["sheets_url"]?.takeIf { it.isNotEmpty() } ?: SyncConstants.DEFAULT_WEB_APP_URL
        if (email == null || url.trim().isEmpty()) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            val success = repository.joinSharedAccount(url, email, shareCode)
            if (success) {
                syncWithSheets()
            }
            _isSyncing.value = false
            onResult(success)
        }
    }

    // Helper calculators for time ranges
    private fun calculatePeriodRange(offset: Int, period: String): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        when (period) {
            "Daily" -> {
                calendar.add(Calendar.DAY_OF_YEAR, offset)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                return Pair(start, end)
            }
            "Weekly" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, offset)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                return Pair(start, end)
            }
            "Yearly" -> {
                calendar.add(Calendar.YEAR, offset)
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.MONTH, Calendar.DECEMBER)
                calendar.set(Calendar.DAY_OF_MONTH, 31)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                return Pair(start, end)
            }
            else -> { // Monthly
                calendar.add(Calendar.MONTH, offset)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                return Pair(start, end)
            }
        }
    }

    private fun calculatePeriodLabel(offset: Int, period: String): String {
        val calendar = Calendar.getInstance()
        when (period) {
            "Daily" -> {
                calendar.add(Calendar.DAY_OF_YEAR, offset)
                val format = SimpleDateFormat("MMM d, yyyy", Locale.US)
                return format.format(calendar.time)
            }
            "Weekly" -> {
                calendar.add(Calendar.WEEK_OF_YEAR, offset)
                val format = SimpleDateFormat("MMM d", Locale.US)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                val startStr = format.format(calendar.time)
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val endStr = format.format(calendar.time)
                return "$startStr - $endStr"
            }
            "Yearly" -> {
                calendar.add(Calendar.YEAR, offset)
                val format = SimpleDateFormat("yyyy", Locale.US)
                return format.format(calendar.time)
            }
            else -> { // Monthly
                calendar.add(Calendar.MONTH, offset)
                val format = SimpleDateFormat("MMMM", Locale.US) // e.g. "May"
                val monthName = format.format(calendar.time)
                val yearFormat = SimpleDateFormat("yyyy", Locale.US)
                val year = yearFormat.format(calendar.time)
                // If current year, just show Month name, else include year (to match screenshot "May")
                val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
                return if (year == currentYear) monthName else "$monthName $year"
            }
        }
    }
}

data class SpendingMetrics(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double
)
