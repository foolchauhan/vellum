package com.example.vellum.data

import android.util.Log
import com.example.vellum.data.local.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object SheetsSyncManager {
    private const val TAG = "SheetsSyncManager"

    // Execute HTTP request manually following redirects across domains (required for Google Apps Script Web Apps)
    private fun executeHttp(urlStr: String, method: String, jsonBody: String? = null): String {
        var currentUrl = urlStr
        var redirects = 0
        while (redirects < 5) {
            val url = URL(currentUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.requestMethod = method
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            
            if (jsonBody != null && (method == "POST" || method == "PUT")) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { os ->
                    os.write(jsonBody.toByteArray(Charsets.UTF_8))
                }
            }

            val status = conn.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || 
                status == HttpURLConnection.HTTP_MOVED_PERM || 
                status == 307 || status == 308) {
                val newUrl = conn.getHeaderField("Location")
                if (newUrl == null) {
                    throw RuntimeException("Redirect location missing")
                }
                currentUrl = newUrl
                redirects++
                Log.d(TAG, "Redirecting to: $currentUrl")
            } else {
                if (status >= 400) {
                    val errorText = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    throw RuntimeException("HTTP Error $status: $errorText")
                }
                return conn.inputStream.bufferedReader().use { it.readText() }
            }
        }
        throw RuntimeException("Too many redirects")
    }

    // Join a shared household account via share code
    suspend fun joinAccount(webAppUrl: String, email: String, shareCode: String): AccountEntity? = withContext(Dispatchers.IO) {
        try {
            val queryUrl = "$webAppUrl?action=join&email=${java.net.URLEncoder.encode(email, "UTF-8")}&shareCode=${java.net.URLEncoder.encode(shareCode, "UTF-8")}"
            val response = executeHttp(queryUrl, "GET")
            val json = JSONObject(response)
            if (json.optString("status") == "success") {
                val accJson = json.getJSONObject("account")
                return@withContext AccountEntity(
                    id = accJson.getString("id"),
                    name = accJson.getString("name"),
                    icon = accJson.getString("icon"),
                    isDefault = accJson.optBoolean("isDefault", false),
                    color = accJson.optString("color", "#4E3C30"),
                    shareCode = accJson.optString("shareCode"),
                    ownerEmail = accJson.optString("ownerEmail"),
                    userEmail = email,
                    isSynced = true,
                    carryOver = accJson.optBoolean("carryOver", false)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to join account: ", e)
        }
        return@withContext null
    }

    // Perform bidirectional sync: push local changes and replace cache with full remote set
    suspend fun sync(
        webAppUrl: String,
        email: String,
        displayName: String?,
        photoUrl: String?,
        db: VellumDatabase,
        onComplete: (() -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        try {
            // 1. Gather all current local data (including tombstones)
            val localTx = db.transactionDao().getAllTransactions()
            val localCats = db.categoryDao().getAllCategories()
            val localAccs = db.accountDao().getAllAccounts()
            val localPrefs = db.preferenceDao().getAllPreferences()

            // 2. Fetch the full remote dataset (GET sync action)
            val queryUrl = "$webAppUrl?action=sync&email=${java.net.URLEncoder.encode(email, "UTF-8")}&displayName=${java.net.URLEncoder.encode(displayName ?: "", "UTF-8")}&photoUrl=${java.net.URLEncoder.encode(photoUrl ?: "", "UTF-8")}"
            val getResponse = executeHttp(queryUrl, "GET")
            val responseJson = JSONObject(getResponse)

            // Parse remote data
            val remoteAccsArray = responseJson.getJSONArray("accounts")
            val remoteAccs = mutableListOf<AccountEntity>()
            for (i in 0 until remoteAccsArray.length()) {
                val o = remoteAccsArray.getJSONObject(i)
                remoteAccs.add(
                    AccountEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        icon = o.getString("icon"),
                        isDefault = o.optBoolean("isDefault", false),
                        color = o.optString("color", "#4E3C30"),
                        shareCode = o.optString("shareCode").takeIf { it.isNotEmpty() },
                        ownerEmail = o.optString("ownerEmail").takeIf { it.isNotEmpty() },
                        userEmail = o.optString("userEmail").takeIf { it.isNotEmpty() },
                        isSynced = true,
                        updatedAt = o.optLong("updatedAt", 0L),
                        isDeleted = o.optBoolean("isDeleted", false),
                        deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt"),
                        carryOver = o.optBoolean("carryOver", false)
                    )
                )
            }

            val remoteCatsArray = responseJson.getJSONArray("categories")
            val remoteCats = mutableListOf<CategoryEntity>()
            for (i in 0 until remoteCatsArray.length()) {
                val o = remoteCatsArray.getJSONObject(i)
                remoteCats.add(
                    CategoryEntity(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        type = o.getString("type"),
                        icon = o.getString("icon"),
                        isDefault = o.optBoolean("isDefault", false),
                        chartColor = o.optString("chartColor", "#4E3C30"),
                        userEmail = o.optString("userEmail").takeIf { it.isNotEmpty() },
                        isSynced = true,
                        updatedAt = o.optLong("updatedAt", 0L),
                        isDeleted = o.optBoolean("isDeleted", false),
                        deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
                    )
                )
            }

            val remoteTxArray = responseJson.getJSONArray("transactions")
            val remoteTx = mutableListOf<TransactionEntity>()
            for (i in 0 until remoteTxArray.length()) {
                val o = remoteTxArray.getJSONObject(i)
                remoteTx.add(
                    TransactionEntity(
                        id = o.getString("id"),
                        amount = o.getDouble("amount"),
                        type = o.getString("type"),
                        categoryId = o.getString("categoryId"),
                        categoryName = o.getString("categoryName"),
                        accountId = o.getString("accountId"),
                        accountName = o.getString("accountName"),
                        note = o.optString("note", ""),
                        timestamp = o.getLong("timestamp"),
                        userEmail = o.optString("userEmail").takeIf { it.isNotEmpty() },
                        isSynced = true,
                        updatedAt = o.optLong("updatedAt", 0L),
                        isDeleted = o.optBoolean("isDeleted", false),
                        deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt")
                    )
                )
            }

            val remotePrefsArray = responseJson.getJSONArray("preferences")
            val remotePrefs = mutableListOf<PreferenceEntity>()
            for (i in 0 until remotePrefsArray.length()) {
                val o = remotePrefsArray.getJSONObject(i)
                remotePrefs.add(
                    PreferenceEntity(
                        key = o.getString("key"),
                        value = o.getString("value")
                    )
                )
            }

            // 1. Resolve left accounts first
            val leftAccountsStr = db.preferenceDao().getPreferenceValue("left_accounts") ?: ""
            val leftAccountsSet = if (leftAccountsStr.isEmpty()) emptySet() else leftAccountsStr.split(",").toSet()

            // 2. Perform LWW Merge
            
            // Merge Accounts
            val mergedAccs = mutableMapOf<String, AccountEntity>()
            remoteAccs.forEach { mergedAccs[it.id] = it }
            localAccs.forEach { local ->
                val remote = mergedAccs[local.id]
                if (remote != null) {
                    if (local.updatedAt >= remote.updatedAt) {
                        val isSame = local.name == remote.name &&
                                     local.icon == remote.icon &&
                                     local.color == remote.color &&
                                     local.isDeleted == remote.isDeleted &&
                                     local.carryOver == remote.carryOver &&
                                     local.updatedAt == remote.updatedAt
                        mergedAccs[local.id] = local.copy(isSynced = isSame)
                    }
                } else {
                    if (!local.isSynced) {
                        mergedAccs[local.id] = local
                    }
                }
            }
            val finalAccs = mergedAccs.values.filter { it.id !in leftAccountsSet }

            // Merge Categories
            val mergedCats = mutableMapOf<String, CategoryEntity>()
            remoteCats.forEach { mergedCats[it.id] = it }
            localCats.forEach { local ->
                val remote = mergedCats[local.id]
                if (remote != null) {
                    if (local.updatedAt >= remote.updatedAt) {
                        val isSame = local.name == remote.name &&
                                     local.type == remote.type &&
                                     local.icon == remote.icon &&
                                     local.chartColor == remote.chartColor &&
                                     local.isDeleted == remote.isDeleted &&
                                     local.updatedAt == remote.updatedAt
                        mergedCats[local.id] = local.copy(isSynced = isSame)
                    }
                } else {
                    if (!local.isSynced) {
                        mergedCats[local.id] = local
                    }
                }
            }
            val finalCats = mergedCats.values.toList()

            // Merge Transactions
            val mergedTx = mutableMapOf<String, TransactionEntity>()
            remoteTx.forEach { mergedTx[it.id] = it }
            localTx.forEach { local ->
                val remote = mergedTx[local.id]
                if (remote != null) {
                    if (local.updatedAt >= remote.updatedAt) {
                        val isSame = local.amount == remote.amount &&
                                     local.type == remote.type &&
                                     local.categoryId == remote.categoryId &&
                                     local.categoryName == remote.categoryName &&
                                     local.accountId == remote.accountId &&
                                     local.accountName == remote.accountName &&
                                     local.note == remote.note &&
                                     local.timestamp == remote.timestamp &&
                                     local.isDeleted == remote.isDeleted &&
                                     local.updatedAt == remote.updatedAt
                        mergedTx[local.id] = local.copy(isSynced = isSame)
                    }
                } else {
                    if (!local.isSynced) {
                        mergedTx[local.id] = local
                    }
                }
            }
            val finalTx = mergedTx.values.toList()

            // Merge Preferences: local changes win for a normal sync
            val finalPrefs = mutableListOf<PreferenceEntity>()
            val localPrefKeys = localPrefs.map { it.key }.toSet()
            finalPrefs.addAll(localPrefs)
            for (remote in remotePrefs) {
                if (remote.key !in localPrefKeys) {
                    finalPrefs.add(remote)
                }
            }

            // Save merged state to Room in a transaction
            db.runInTransaction {
                kotlinx.coroutines.runBlocking {
                    db.transactionDao().deleteAllTransactions()
                    db.categoryDao().deleteAllCategories()
                    db.accountDao().deleteAllAccounts()
                    db.preferenceDao().deleteAllPreferences()

                    if (finalAccs.isNotEmpty()) db.accountDao().insertAccounts(finalAccs)
                    if (finalCats.isNotEmpty()) db.categoryDao().insertCategories(finalCats)
                    if (finalTx.isNotEmpty()) db.transactionDao().insertTransactions(finalTx)

                    // Seed default preferences if any are missing
                    val defaultPrefs = listOf(
                        PreferenceEntity("summary_font", "Chalk"),
                        PreferenceEntity("time_period", "Monthly"),
                        PreferenceEntity("budget_mode", "Off"),
                        PreferenceEntity("carry_over", "Off"),
                        PreferenceEntity("hide_future", "Off"),
                        PreferenceEntity("theme", "System"),
                        PreferenceEntity("show_notes", "On"),
                        PreferenceEntity("currency_symbol", "Default"),
                        PreferenceEntity("category_icon_style", "Filled"),
                        PreferenceEntity("tabs_position", "Top"),
                        PreferenceEntity("reminders", "Every Week"),
                        PreferenceEntity("auto_backup", "Off"),
                        PreferenceEntity("passcode", "Off")
                    )
                    val finalPrefKeys = finalPrefs.map { it.key }.toSet()
                    val prefsToSave = finalPrefs.toMutableList()
                    for (defaultPref in defaultPrefs) {
                        if (defaultPref.key !in finalPrefKeys) {
                            prefsToSave.add(defaultPref)
                        }
                    }
                    if (prefsToSave.isNotEmpty()) db.preferenceDao().insertPreferences(prefsToSave)
                }
            }

            // 3. Compile and POST dirty changes if there are any
            val dirtyTx = db.transactionDao().getAllTransactions().filter { !it.isSynced }
            val dirtyCats = db.categoryDao().getAllCategories().filter { !it.isSynced }
            val dirtyAccs = db.accountDao().getAllAccounts().filter { !it.isSynced }
            val dirtyPrefs = db.preferenceDao().getAllPreferences() // always upload all prefs

            val delTxStr = db.preferenceDao().getPreferenceValue("deleted_transactions") ?: ""
            val delCatStr = db.preferenceDao().getPreferenceValue("deleted_categories") ?: ""
            val delAccStr = db.preferenceDao().getPreferenceValue("deleted_accounts") ?: ""
            val leftAccsStrAfterMerge = db.preferenceDao().getPreferenceValue("left_accounts") ?: ""

            val preferencesChanged = localPrefs.any { local ->
                val remote = remotePrefs.find { it.key == local.key }
                remote == null || remote.value != local.value
            }
            val hasDeletions = delTxStr.isNotEmpty() || delCatStr.isNotEmpty() || delAccStr.isNotEmpty() || leftAccsStrAfterMerge.isNotEmpty()
            val hasDirtyChanges = dirtyTx.isNotEmpty() || dirtyCats.isNotEmpty() || dirtyAccs.isNotEmpty() || hasDeletions || preferencesChanged

            if (hasDirtyChanges) {
                val requestJson = JSONObject()
                requestJson.put("email", email)
                requestJson.put("displayName", displayName ?: "")
                requestJson.put("photoUrl", photoUrl ?: "")

                val delTxArray = JSONArray()
                if (delTxStr.isNotEmpty()) delTxStr.split(",").forEach { delTxArray.put(it) }
                requestJson.put("deleted_transactions", delTxArray)

                val delCatArray = JSONArray()
                if (delCatStr.isNotEmpty()) delCatStr.split(",").forEach { delCatArray.put(it) }
                requestJson.put("deleted_categories", delCatArray)

                val delAccArray = JSONArray()
                if (delAccStr.isNotEmpty()) delAccStr.split(",").forEach { delAccArray.put(it) }
                requestJson.put("deleted_accounts", delAccArray)

                val leftAccArray = JSONArray()
                if (leftAccsStrAfterMerge.isNotEmpty()) leftAccsStrAfterMerge.split(",").forEach { leftAccArray.put(it) }
                requestJson.put("left_accounts", leftAccArray)

                val txArray = JSONArray()
                dirtyTx.forEach { tx ->
                    val o = JSONObject()
                    o.put("id", tx.id)
                    o.put("amount", tx.amount)
                    o.put("type", tx.type)
                    o.put("categoryId", tx.categoryId)
                    o.put("categoryName", tx.categoryName)
                    o.put("accountId", tx.accountId)
                    o.put("accountName", tx.accountName)
                    o.put("note", tx.note)
                    o.put("timestamp", tx.timestamp)
                    o.put("userEmail", tx.userEmail ?: email)
                    o.put("updatedAt", tx.updatedAt)
                    o.put("isDeleted", tx.isDeleted)
                    o.put("deletedAt", tx.deletedAt ?: JSONObject.NULL)
                    txArray.put(o)
                }
                requestJson.put("transactions", txArray)

                val catArray = JSONArray()
                dirtyCats.forEach { cat ->
                    val o = JSONObject()
                    o.put("id", cat.id)
                    o.put("name", cat.name)
                    o.put("type", cat.type)
                    o.put("icon", cat.icon)
                    o.put("isDefault", cat.isDefault)
                    o.put("chartColor", cat.chartColor)
                    o.put("userEmail", cat.userEmail ?: email)
                    o.put("updatedAt", cat.updatedAt)
                    o.put("isDeleted", cat.isDeleted)
                    o.put("deletedAt", cat.deletedAt ?: JSONObject.NULL)
                    catArray.put(o)
                }
                requestJson.put("categories", catArray)

                val accArray = JSONArray()
                dirtyAccs.forEach { acc ->
                    val o = JSONObject()
                    o.put("id", acc.id)
                    o.put("name", acc.name)
                    o.put("icon", acc.icon)
                    o.put("isDefault", acc.isDefault)
                    o.put("color", acc.color)
                    o.put("shareCode", acc.shareCode ?: "")
                    o.put("ownerEmail", acc.ownerEmail ?: email)
                    o.put("userEmail", acc.userEmail ?: email)
                    o.put("updatedAt", acc.updatedAt)
                    o.put("isDeleted", acc.isDeleted)
                    o.put("deletedAt", acc.deletedAt ?: JSONObject.NULL)
                    o.put("carryOver", acc.carryOver)
                    accArray.put(o)
                }
                requestJson.put("accounts", accArray)

                val prefArray = JSONArray()
                dirtyPrefs.forEach { pref ->
                    val o = JSONObject()
                    o.put("key", pref.key)
                    o.put("value", pref.value)
                    o.put("userEmail", email)
                    prefArray.put(o)
                }
                requestJson.put("preferences", prefArray)

                val responseStr = executeHttp(webAppUrl, "POST", requestJson.toString())
                Log.d(TAG, "Push response: $responseStr")

                // Mark local items as synced and clear deletion preferences
                db.runInTransaction {
                    kotlinx.coroutines.runBlocking {
                        // Mark as synced in DB
                        dirtyTx.forEach { db.transactionDao().insertTransaction(it.copy(isSynced = true)) }
                        dirtyCats.forEach { db.categoryDao().insertCategory(it.copy(isSynced = true)) }
                        dirtyAccs.forEach { db.accountDao().insertAccount(it.copy(isSynced = true)) }

                        db.preferenceDao().insertPreference(PreferenceEntity("deleted_transactions", ""))
                        db.preferenceDao().insertPreference(PreferenceEntity("deleted_categories", ""))
                        db.preferenceDao().insertPreference(PreferenceEntity("deleted_accounts", ""))
                        db.preferenceDao().insertPreference(PreferenceEntity("left_accounts", ""))
                    }
                }
            }

            Log.d(TAG, "Sync complete and database updated successfully.")
            onComplete?.invoke()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ", e)
            throw e
        }
    }

}
