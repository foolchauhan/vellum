package com.example.vellum.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vellum.data.DataRepository
import com.example.vellum.data.SyncConstants
import com.example.vellum.data.local.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.serialization.json.Json
import kotlin.coroutines.resume

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

    private fun getSplitsList(splitsJson: String): List<TransactionSplit> {
        if (splitsJson.isEmpty()) return emptyList()
        return try {
            Json.decodeFromString<List<TransactionSplit>>(splitsJson)
        } catch (e: Exception) {
            emptyList()
        }
    }

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
                    val splits = getSplitsList(tx.splits)
                    val matchCat = filterCat == null || tx.categoryId == filterCat.id || splits.any { it.categoryId == filterCat.id }
                    val matchAcc = filterAcc == null || tx.accountId == filterAcc.id || splits.any { it.accountId == filterAcc.id }
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

    val unsyncedTransactions: StateFlow<List<TransactionEntity>> = allTransactions
        .map { list -> list.filter { !it.isSynced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unsyncedCategories: StateFlow<List<CategoryEntity>> = categories
        .map { list -> list.filter { !it.isSynced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unsyncedAccounts: StateFlow<List<AccountEntity>> = accounts
        .map { list -> list.filter { !it.isSynced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiInsights: StateFlow<List<String>> = combine(
        allTransactions,
        categories,
        selectedFilterAccount,
        preferences
    ) { txs, cats, filterAcc, prefs ->
        val activeTxs = txs.filter { tx ->
            if (tx.isDeleted || tx.type != "EXPENSE") return@filter false
            val splits = getSplitsList(tx.splits)
            if (filterAcc != null) {
                tx.accountId == filterAcc.id || splits.any { it.accountId == filterAcc.id }
            } else {
                true
            }
        }
        val activeIncomes = txs.filter { tx ->
            if (tx.isDeleted || tx.type != "INCOME") return@filter false
            val splits = getSplitsList(tx.splits)
            if (filterAcc != null) {
                tx.accountId == filterAcc.id || splits.any { it.accountId == filterAcc.id }
            } else {
                true
            }
        }
        val rawSymbol = prefs["currency_symbol"]
        val currencySymbol = when (rawSymbol) {
            "Default" -> "₹"
            "INR" -> "₹"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            else -> rawSymbol ?: "₹"
        }
        
        val list = mutableListOf<String>()
        
        if (activeTxs.isEmpty()) {
            list.add("No expenses recorded yet. Tap + Expense below to start your chalkboard ledger!")
        } else {
            // 1. Budget Alerts
            for (cat in cats.filter { !it.isDeleted }) {
                if (cat.budget > 0.0) {
                    val spent = activeTxs.sumOf { tx ->
                        val splits = getSplitsList(tx.splits)
                        if (splits.isNotEmpty()) {
                            splits.filter { s -> s.categoryId == cat.id && (filterAcc == null || s.accountId == filterAcc.id) }.sumOf { it.amount }
                        } else if (tx.categoryId == cat.id) {
                            tx.amount
                        } else {
                            0.0
                        }
                    }
                    if (spent > cat.budget) {
                        list.add("Budget Alert! We've exceeded the budget for ${cat.icon} ${cat.name} by ${currencySymbol}${String.format(Locale.US, "%.2f", spent - cat.budget)}. Let's review our ledger!")
                    } else if (spent > cat.budget * 0.8) {
                        list.add("Careful! We have used over 80% of our budget for ${cat.icon} ${cat.name} (${currencySymbol}${String.format(Locale.US, "%.2f", spent)} of ${currencySymbol}${String.format(Locale.US, "%.2f", cat.budget)}).")
                    }
                }
            }
            
            // 2. Largest Expense Tip
            val txWithAmount = activeTxs.map { tx ->
                val splits = getSplitsList(tx.splits)
                val effectiveAmount = if (splits.isNotEmpty()) {
                    splits.filter { filterAcc == null || it.accountId == filterAcc.id }.sumOf { it.amount }
                } else {
                    tx.amount
                }
                Pair(tx, effectiveAmount)
            }.filter { it.second > 0.0 }
            val highest = txWithAmount.maxByOrNull { it.second }
            if (highest != null && highest.second > 0.0) {
                val tx = highest.first
                val amt = highest.second
                list.add("Tutor Tip: Our largest single expense was ${currencySymbol}${String.format(Locale.US, "%.2f", amt)} on ${tx.categoryName}${if (tx.note.isNotEmpty()) " (${tx.note})" else ""}. Keeping this down helps a lot!")
            }
            
            // 3. Highest Spending Category
            val categorySpends = activeTxs.flatMap { tx ->
                val splits = getSplitsList(tx.splits)
                if (splits.isNotEmpty()) {
                    splits.filter { filterAcc == null || it.accountId == filterAcc.id }.map { Pair(it.categoryId, it.amount) }
                } else {
                    listOf(Pair(tx.categoryId, tx.amount))
                }
            }.groupBy { it.first }.mapValues { entry -> entry.value.sumOf { it.second } }
            val topCat = categorySpends.maxByOrNull { it.value }
            if (topCat != null) {
                val cat = cats.find { it.id == topCat.key }
                if (cat != null) {
                    val totalSpent = topCat.value
                    list.add("Insight: ${cat.icon} ${cat.name} is our highest spending category, totaling ${currencySymbol}${String.format(Locale.US, "%.2f", totalSpent)}. Can we find ways to save here?")
                }
            }
            
            // 4. Subscription check
            val hasSubscription = activeTxs.any { 
                com.example.vellum.data.SemanticMatcher.isSemanticMatch("subscription", it.note) || 
                com.example.vellum.data.SemanticMatcher.isSemanticMatch("subscription", it.categoryName) 
            }
            if (hasSubscription) {
                list.add("AI Tip: We detected subscription charges on the blackboard. Audit them regularly to keep fixed costs low!")
            }

            // 5. Savings Rate Check
            val totalExpense = activeTxs.sumOf { tx ->
                val splits = getSplitsList(tx.splits)
                if (splits.isNotEmpty()) {
                    splits.filter { filterAcc == null || it.accountId == filterAcc.id }.sumOf { it.amount }
                } else {
                    tx.amount
                }
            }
            val totalIncome = activeIncomes.sumOf { tx ->
                val splits = getSplitsList(tx.splits)
                if (splits.isNotEmpty()) {
                    splits.filter { filterAcc == null || it.accountId == filterAcc.id }.sumOf { it.amount }
                } else {
                    tx.amount
                }
            }
            if (totalIncome > 0.0) {
                val savings = totalIncome - totalExpense
                val rate = (savings / totalIncome) * 100
                if (rate > 0.0) {
                    list.add("Savings Goal: You have saved ${currencySymbol}${String.format(Locale.US, "%.2f", savings)} this period (${String.format(Locale.US, "%.1f", rate)}% savings rate). Excellent progress, keep it up!")
                } else {
                    list.add("Savings Alert: Your expenses exceed your income by ${currencySymbol}${String.format(Locale.US, "%.2f", -savings)} this period. Let's look for areas to cut back.")
                }
            }

            // 6. Logging habit tracker
            val loggedCount = activeTxs.size + activeIncomes.size
            if (loggedCount > 5) {
                list.add("Habit Tip: You've logged $loggedCount transactions this period! Consistency on the chalkboard leads to better financial clarity.")
            }
        }
        
        val dynamicTipsCount = list.size
        val staticTipsNeeded = (6 - dynamicTipsCount).coerceAtLeast(2)
        val staticTips = listOf(
            "Financial Tip: Try to allocate 50% of your income for needs, 30% for wants, and save/invest 20% (the 50/30/20 rule).",
            "Smart Spending: Before making a non-essential purchase, wait 24 hours. This rule helps eliminate impulsive spending on wants.",
            "Chalkboard Advice: Review your subscriptions annually. Unused memberships are silent leaks in your net balance.",
            "Ledger Wisdom: Categorizing every expense gives you an honest picture of where your cash goes. Small leaks sink great ships!",
            "Emergency Fund: Keep 3 to 6 months of living expenses saved up. It serves as a financial shock absorber when unexpected things happen.",
            "Invest in Yourself: Reading books, learning new skills, or taking technical courses is the highest-return investment you can make.",
            "Budget Strategy: Track your small daily expenses (like coffee or snacks). Over a month, these minor cash leaks accumulate into large sums.",
            "Debt Management: Prioritize paying off high-interest debt first (the avalanche method) to build momentum.",
            "Smart Investing: Start investing early, even with small amounts. Compound interest works best over longer horizons.",
            "Subscription Wisdom: Check for duplicate subscriptions or services with overlapping features. Consolidating them saves money automatically.",
            "Goal Setting: Define short-term (vacation), medium-term (car), and long-term (retirement) financial goals to keep your savings motivated.",
            "Chalkboard Pro-Tip: Match your pay cycles with your recurring bill dates. This keeps your cash flow balanced and avoids late fees."
        ).shuffled().take(staticTipsNeeded)
        
        list.addAll(staticTips)
        
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stickyNotes: StateFlow<List<StickyNoteEntity>> = userEmail.flatMapLatest { email ->
        repository.getStickyNotesForUserFlow(email)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val unsyncedNotes: StateFlow<List<StickyNoteEntity>> = stickyNotes
        .map { list -> list.filter { !it.isSynced } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var hasInitializedDefaultAccount = false

    private val _activeConflict = MutableStateFlow<Pair<TransactionEntity, TransactionEntity>?>(null)
    val activeConflict: StateFlow<Pair<TransactionEntity, TransactionEntity>?> = _activeConflict.asStateFlow()
    private var conflictContinuation: kotlinx.coroutines.CancellableContinuation<Boolean>? = null

    val unsyncedCount: StateFlow<Int> = combine(
        allTransactions,
        categories,
        accounts,
        stickyNotes
    ) { txs, cats, accs, notes ->
        val unsyncedTxs = txs.count { !it.isSynced }
        val unsyncedCats = cats.count { !it.isSynced }
        val unsyncedAccs = accs.count { !it.isSynced }
        val unsyncedNotes = notes.count { !it.isSynced }
        unsyncedTxs + unsyncedCats + unsyncedAccs + unsyncedNotes
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun resolveConflict(useLocal: Boolean) {
        conflictContinuation?.resume(useLocal)
        conflictContinuation = null
        _activeConflict.value = null
    }

    init {
        com.example.vellum.data.SheetsSyncManager.transactionConflictResolver = { local, remote ->
            kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
                conflictContinuation = continuation
                _activeConflict.value = Pair(local, remote)
            }
        }
        viewModelScope.launch {
            repository.checkAndSeedDefaults()
        }
        viewModelScope.launch {
            combine(accounts, repository.getPreferencesFlow()) { accList, prefsList ->
                Pair(accList, prefsList.associate { it.key to it.value })
            }.collect { (accList, prefs) ->
                if (!hasInitializedDefaultAccount && accList.isNotEmpty()) {
                    val savedAccId = prefs["selected_account_id"]
                    if (savedAccId != null) {
                        if (savedAccId == "all") {
                            _selectedFilterAccount.value = null
                        } else {
                            val acc = accList.find { it.id == savedAccId }
                            if (acc != null) {
                                _selectedFilterAccount.value = acc
                            } else {
                                val defaultAcc = accList.find { it.isDefault } ?: accList.find { it.name.lowercase() == "personal" }
                                _selectedFilterAccount.value = defaultAcc
                            }
                        }
                        hasInitializedDefaultAccount = true
                    } else {
                        val defaultAcc = accList.find { it.isDefault } ?: accList.find { it.name.lowercase() == "personal" }
                        if (defaultAcc != null) {
                            _selectedFilterAccount.value = defaultAcc
                            hasInitializedDefaultAccount = true
                        }
                    }
                }
            }
        }
    }

    // Metrics aggregates for the Spending screen
    val spendingMetrics = combine(
        transactions,
        allTransactions,
        activePeriodRange,
        _selectedFilterAccount,
        accounts
    ) { currentPeriodList, allList, range, filterAcc, accList ->
        var income = 0.0
        var expense = 0.0
        currentPeriodList.forEach { tx ->
            val splits = getSplitsList(tx.splits)
            if (splits.isNotEmpty()) {
                splits.forEach { split ->
                    val matchAcc = filterAcc == null || split.accountId == filterAcc.id
                    if (matchAcc) {
                        if (tx.type == "INCOME") {
                            income += split.amount
                        } else {
                            expense += split.amount
                        }
                    }
                }
            } else {
                if (tx.type == "INCOME") {
                    income += tx.amount
                } else {
                    expense += tx.amount
                }
            }
        }

        // Calculate carryover starting balance from prior periods
        var startingBalance = 0.0
        if (filterAcc != null) {
            val acc = accList.find { it.id == filterAcc.id } ?: filterAcc
            if (acc.carryOver) {
                allList.forEach { tx ->
                    if (tx.timestamp < range.first && !tx.isDeleted) {
                        val splits = getSplitsList(tx.splits)
                        if (splits.isNotEmpty()) {
                            splits.forEach { split ->
                                if (split.accountId == acc.id) {
                                    if (tx.type == "INCOME") {
                                        startingBalance += split.amount
                                    } else {
                                        startingBalance -= split.amount
                                    }
                                }
                            }
                        } else {
                            if (tx.accountId == acc.id) {
                                if (tx.type == "INCOME") {
                                    startingBalance += tx.amount
                                } else {
                                    startingBalance -= tx.amount
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val carryOverAccountIds = accList.filter { it.carryOver }.map { it.id }.toSet()
            if (carryOverAccountIds.isNotEmpty()) {
                allList.forEach { tx ->
                    if (tx.timestamp < range.first && !tx.isDeleted) {
                        val splits = getSplitsList(tx.splits)
                        if (splits.isNotEmpty()) {
                            splits.forEach { split ->
                                if (split.accountId in carryOverAccountIds) {
                                    if (tx.type == "INCOME") {
                                        startingBalance += split.amount
                                    } else {
                                        startingBalance -= split.amount
                                    }
                                }
                            }
                        } else {
                            if (tx.accountId in carryOverAccountIds) {
                                if (tx.type == "INCOME") {
                                    startingBalance += tx.amount
                                } else {
                                    startingBalance -= tx.amount
                                }
                            }
                        }
                    }
                }
            }
        }

        SpendingMetrics(income, expense, startingBalance + (income - expense))
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
        viewModelScope.launch {
            repository.insertPreference("selected_account_id", account?.id ?: "all")
        }
    }

    fun clearFilters() {
        _selectedFilterCategory.value = null
        _selectedFilterAccount.value = null
        viewModelScope.launch {
            repository.insertPreference("selected_account_id", "all")
        }
    }

    fun navigatePeriod(forward: Boolean) {
        val period = timePeriod.value
        if (period == "All" || period == "Custom") return
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
        id: String? = null,
        splits: String = ""
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
                    isSynced = false,
                    splits = splits
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
                    isSynced = false,
                    splits = splits
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
        isDefault: Boolean = false,
        budget: Double = 0.0
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
                    isSynced = false,
                    budget = budget
                )
            } else {
                CategoryEntity(
                    name = name,
                    type = type,
                    icon = icon,
                    isDefault = isDefault,
                    chartColor = chartColor,
                    userEmail = _userEmail.value,
                    isSynced = false,
                    budget = budget
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
        ownerEmail: String? = null,
        carryOver: Boolean = false
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
                    isSynced = false,
                    carryOver = carryOver
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
                    isSynced = false,
                    carryOver = carryOver
                )
            }
            repository.insertAccount(acc)
            syncWithSheets()
        }
    }

    fun updateAccountCarryOver(account: AccountEntity, carryOver: Boolean) {
        viewModelScope.launch {
            val updated = account.copy(
                carryOver = carryOver,
                isSynced = false,
                updatedAt = System.currentTimeMillis()
            )
            repository.insertAccount(updated)
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

    fun addStickyNote(content: String, colorHex: String, id: String? = null) {
        viewModelScope.launch {
            val note = if (id != null) {
                StickyNoteEntity(
                    id = id,
                    content = content,
                    colorHex = colorHex,
                    userEmail = _userEmail.value,
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                StickyNoteEntity(
                    content = content,
                    colorHex = colorHex,
                    userEmail = _userEmail.value,
                    isSynced = false,
                    updatedAt = System.currentTimeMillis()
                )
            }
            repository.insertStickyNote(note)
            syncWithSheets()
        }
    }

    fun deleteStickyNote(note: StickyNoteEntity) {
        viewModelScope.launch {
            val currentDeleted = repository.getPreferenceValue("deleted_sticky_notes") ?: ""
            val deletedList = if (currentDeleted.isEmpty()) mutableListOf() else currentDeleted.split(",").toMutableList()
            if (!deletedList.contains(note.id)) {
                deletedList.add(note.id)
                repository.insertPreference("deleted_sticky_notes", deletedList.joinToString(","))
            }
            repository.softDeleteStickyNote(note)
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

    fun signIn(email: String, displayName: String?, photoUrl: String?, isRestore: Boolean = false) {
        _userEmail.value = email
        _userDisplayName.value = displayName
        _userPhotoUrl.value = photoUrl
        hasInitializedDefaultAccount = true // Prevent auto-selecting Personal after sync updates
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.tagLocalDataToUser(email)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            syncWithSheets()
            if (!isRestore) {
                // Ensure after explicit sign in we show all accounts
                _selectedFilterAccount.value = null
                _selectedFilterCategory.value = null
                repository.insertPreference("selected_account_id", "all")
            }
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
                    // Sync complete - keep active filter selections intact
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
            "All" -> {
                return Pair(0L, Long.MAX_VALUE)
            }
            "Last 6 Months" -> {
                calendar.add(Calendar.MONTH, offset * 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                
                calendar.add(Calendar.MONTH, -6)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                return Pair(start, end)
            }
            "Last 1 Year" -> {
                calendar.add(Calendar.YEAR, offset)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                calendar.set(Calendar.SECOND, 59)
                calendar.set(Calendar.MILLISECOND, 999)
                val end = calendar.timeInMillis
                
                calendar.add(Calendar.YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val start = calendar.timeInMillis
                return Pair(start, end)
            }
            "Custom" -> {
                val startStr = preferences.value["custom_start_date"]
                val endStr = preferences.value["custom_end_date"]
                val start = startStr?.toLongOrNull() ?: 0L
                val end = endStr?.toLongOrNull() ?: Long.MAX_VALUE
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
            "All" -> {
                return "All Time"
            }
            "Last 6 Months" -> {
                val format = SimpleDateFormat("MMM yyyy", Locale.US)
                calendar.add(Calendar.MONTH, offset * 6)
                val endStr = format.format(calendar.time)
                calendar.add(Calendar.MONTH, -6)
                val startStr = format.format(calendar.time)
                return "$startStr - $endStr"
            }
            "Last 1 Year" -> {
                val format = SimpleDateFormat("MMM yyyy", Locale.US)
                calendar.add(Calendar.YEAR, offset)
                val endStr = format.format(calendar.time)
                calendar.add(Calendar.YEAR, -1)
                val startStr = format.format(calendar.time)
                return "$startStr - $endStr"
            }
            "Custom" -> {
                val startStr = preferences.value["custom_start_date"]
                val endStr = preferences.value["custom_end_date"]
                val start = startStr?.toLongOrNull()
                val end = endStr?.toLongOrNull()
                if (start != null && end != null) {
                    val format = SimpleDateFormat("MMM d, yyyy", Locale.US)
                    val startLabel = format.format(Date(start))
                    val endLabel = format.format(Date(end))
                    return "$startLabel - $endLabel"
                }
                return "Custom Range"
            }
            else -> { // Monthly
                calendar.add(Calendar.MONTH, offset)
                val format = SimpleDateFormat("MMMM", Locale.US) // e.g. "May"
                val monthName = format.format(calendar.time)
                val yearFormat = SimpleDateFormat("yyyy", Locale.US)
                val year = yearFormat.format(calendar.time)
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
