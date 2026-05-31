package com.example.vellum.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Entity(tableName = "transactions")
@Serializable
data class TransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val type: String, // EXPENSE or INCOME
    val categoryId: String,
    val categoryName: String,
    val accountId: String,
    val accountName: String,
    val note: String,
    val timestamp: Long,
    val userEmail: String? = null,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(tableName = "categories")
@Serializable
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String, // EXPENSE or INCOME
    val icon: String,
    val isDefault: Boolean = false,
    val chartColor: String = "#4E3C30",
    val userEmail: String? = null,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

@Entity(tableName = "accounts")
@Serializable
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val isDefault: Boolean = false,
    val color: String = "#4E3C30",
    val shareCode: String? = null,
    val ownerEmail: String? = null,
    val userEmail: String? = null,
    val isSynced: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)


@Entity(tableName = "preferences")
@Serializable
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)
