package com.example.vellum

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey
@Serializable data object Settings : NavKey
@Serializable data class AddEditTransaction(val transactionId: String? = null, val predefinedType: String = "EXPENSE") : NavKey
@Serializable data class AddEditCategory(val categoryId: String? = null, val predefinedType: String = "EXPENSE") : NavKey
@Serializable data class AddEditAccount(val accountId: String? = null) : NavKey
