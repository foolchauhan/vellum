package com.example.vellum.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        AccountEntity::class,
        PreferenceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VellumDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun preferenceDao(): PreferenceDao

    companion object {
        @Volatile
        private var INSTANCE: VellumDatabase? = null

        fun getInstance(context: Context): VellumDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VellumDatabase::class.java,
                    "vellum.db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(context.applicationContext))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val context: Context
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        prepopulateDatabase(database)
                    }
                }
            }
        }

        suspend fun prepopulateDatabase(db: VellumDatabase) {
            // Seed Default Categories (Expense)
            val expenseCategories = listOf(
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
                CategoryEntity(id = "default_category_travel", name = "Travel", type = "EXPENSE", icon = "app_icon_travel", isDefault = true)
            )
            db.categoryDao().insertCategories(expenseCategories)

            // Seed Default Categories (Income)
            val incomeCategories = listOf(
                CategoryEntity(id = "default_category_salary", name = "Salary", type = "INCOME", icon = "app_icon_salary", isDefault = true)
            )
            db.categoryDao().insertCategories(incomeCategories)

            // Seed Default Account
            val defaultAccount = AccountEntity(id = "default_account_personal", name = "Personal", icon = "app_icon_personal", isDefault = true)
            db.accountDao().insertAccount(defaultAccount)

            // Seed Default Preferences
            val defaultPrefs = listOf(
                PreferenceEntity("summary_font", "Chalk"),
                PreferenceEntity("time_period", "Monthly"),
                PreferenceEntity("budget_mode", "Off"),
                PreferenceEntity("carry_over", "Off"),
                PreferenceEntity("hide_future", "Off"),
                PreferenceEntity("dropbox_sync", "Off"),
                PreferenceEntity("theme", "System"),
                PreferenceEntity("show_notes", "On"),
                PreferenceEntity("currency_symbol", "Default"),
                PreferenceEntity("category_icon_style", "Filled"),
                PreferenceEntity("tabs_position", "Top"),
                PreferenceEntity("reminders", "Every Week"),
                PreferenceEntity("auto_backup", "Off"),
                PreferenceEntity("passcode", "Off")
            )
            for (pref in defaultPrefs) {
                db.preferenceDao().insertPreference(pref)
            }
        }
    }
}
