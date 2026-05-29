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
                    isSynced = true
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
            // 1. Gather all current local data
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
                        isSynced = true
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
                        isSynced = true
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
                        isSynced = true
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

            // Determine if the local database has any custom/offline user data
            // (i.e. transactions, or custom accounts/categories created offline)
            val hasOfflineData = localTx.isNotEmpty() || 
                                 localAccs.any { it.id != "default_account_personal" } ||
                                 localCats.any { !it.isDefault }

            val finalAccs = mutableListOf<AccountEntity>()
            val finalCats = mutableListOf<CategoryEntity>()
            val finalTx = mutableListOf<TransactionEntity>()
            val finalPrefs = mutableListOf<PreferenceEntity>()

            if (!hasOfflineData && remoteAccs.isNotEmpty()) {
                // Fresh login / No offline edits: trust the remote dataset completely
                finalAccs.addAll(remoteAccs)
                finalCats.addAll(remoteCats)
                finalTx.addAll(remoteTx)
                
                // For preferences: local wins, fallback to remote for missing
                val localPrefKeys = localPrefs.map { it.key }.toSet()
                finalPrefs.addAll(localPrefs)
                for (remote in remotePrefs) {
                    if (remote.key !in localPrefKeys) {
                        finalPrefs.add(remote)
                    }
                }

                // If remote doesn't have default_account_personal, ensure we record it as deleted
                val remoteHasPersonal = remoteAccs.any { it.id == "default_account_personal" }
                if (!remoteHasPersonal) {
                    db.preferenceDao().insertPreference(PreferenceEntity("default_account_personal_deleted", "true"))
                }
            } else {
                // Merge remote and local data
                
                // 1. Check if we need to delete local default_account_personal
                val remoteHasAccounts = remoteAccs.isNotEmpty()
                val remoteHasPersonal = remoteAccs.any { it.id == "default_account_personal" }
                val localPersonal = localAccs.find { it.id == "default_account_personal" }
                if (remoteHasAccounts && !remoteHasPersonal && localPersonal != null) {
                    // Check if there are local transactions pointing to personal
                    val hasTransactionsForPersonal = localTx.any { it.accountId == "default_account_personal" }
                    if (!hasTransactionsForPersonal) {
                        // Delete personal account locally and set preference
                        db.accountDao().deleteAccount(localPersonal)
                        db.preferenceDao().insertPreference(PreferenceEntity("default_account_personal_deleted", "true"))
                    }
                }

                // Re-read local accounts after potential deletion
                val localAccsAfterDelete = db.accountDao().getAllAccounts()

                // Merge Accounts
                for (remote in remoteAccs) {
                    val local = localAccsAfterDelete.find { it.id == remote.id }
                    if (local != null && !local.isSynced) {
                        // Keep local edits (name, icon, color) but keep remote sharing metadata
                        finalAccs.add(remote.copy(
                            name = local.name,
                            icon = local.icon,
                            color = local.color,
                            isSynced = false
                        ))
                    } else {
                        finalAccs.add(remote.copy(isSynced = true))
                    }
                }
                for (local in localAccsAfterDelete) {
                    if (remoteAccs.none { it.id == local.id }) {
                        finalAccs.add(local)
                    }
                }

                // Merge Categories
                for (remote in remoteCats) {
                    val local = localCats.find { it.id == remote.id }
                    if (local != null && !local.isSynced) {
                        finalCats.add(remote.copy(
                            name = local.name,
                            type = local.type,
                            icon = local.icon,
                            chartColor = local.chartColor,
                            isSynced = false
                        ))
                    } else {
                        finalCats.add(remote.copy(isSynced = true))
                    }
                }
                for (local in localCats) {
                    if (remoteCats.none { it.id == local.id }) {
                        finalCats.add(local)
                    }
                }

                // Merge Transactions
                for (remote in remoteTx) {
                    val local = localTx.find { it.id == remote.id }
                    if (local != null && !local.isSynced) {
                        finalTx.add(remote.copy(
                            amount = local.amount,
                            type = local.type,
                            categoryId = local.categoryId,
                            categoryName = local.categoryName,
                            accountId = local.accountId,
                            accountName = local.accountName,
                            note = local.note,
                            timestamp = local.timestamp,
                            isSynced = false
                        ))
                    } else {
                        finalTx.add(remote.copy(isSynced = true))
                    }
                }
                for (local in localTx) {
                    if (remoteTx.none { it.id == local.id }) {
                        finalTx.add(local)
                    }
                }

                // Merge Preferences: local changes win for a normal sync
                val localPrefKeys = localPrefs.map { it.key }.toSet()
                finalPrefs.addAll(localPrefs)
                for (remote in remotePrefs) {
                    if (remote.key !in localPrefKeys) {
                        finalPrefs.add(remote)
                    }
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

            val preferencesChanged = localPrefs.any { local ->
                val remote = remotePrefs.find { it.key == local.key }
                remote == null || remote.value != local.value
            }
            val hasDeletions = delTxStr.isNotEmpty() || delCatStr.isNotEmpty() || delAccStr.isNotEmpty()
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
