package com.example.vellum.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
    version = 4,
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
                .addMigrations(MIGRATION_3_4)
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

        // ── Room Migration v3 → v4 ────────────────────────────────────────────
        // Adds three columns to transactions, categories, and accounts:
        //   updatedAt  — epoch ms for Last-Write-Wins conflict resolution.
        //                Default 0 for existing rows: remote rows with real timestamps
        //                will win on first sync, which is the correct LWW outcome.
        //   isDeleted  — tombstone flag; survives sync instead of hard-delete.
        //   deletedAt  — epoch ms of soft-delete, nullable.
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE transactions ADD COLUMN deletedAt INTEGER")

                database.execSQL("ALTER TABLE categories ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE categories ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE categories ADD COLUMN deletedAt INTEGER")

                database.execSQL("ALTER TABLE accounts ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE accounts ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE accounts ADD COLUMN deletedAt INTEGER")
            }
        }

        suspend fun prepopulateDatabase(db: VellumDatabase) {
            // Seed Default Categories (Expense)
            val expenseCategories = listOf(
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
                CategoryEntity(id = "default_category_travel", name = "Travel", type = "EXPENSE", icon = "app_icon_travel", chartColor = "#03A9F4", isDefault = true)
            )
            db.categoryDao().insertCategories(expenseCategories)

            // Seed Default Categories (Income)
            val incomeCategories = listOf(
                CategoryEntity(id = "default_category_salary", name = "Salary", type = "INCOME", icon = "app_icon_salary", chartColor = "#009688", isDefault = true)
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
