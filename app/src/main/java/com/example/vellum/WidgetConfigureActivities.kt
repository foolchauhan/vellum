package com.example.vellum

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vellum.theme.VellumTheme
import com.example.vellum.theme.ParchmentDarkBrown
import com.example.vellum.theme.ParchmentFontFamily
import com.example.vellum.theme.ParchmentLine
import com.example.vellum.ui.components.ParchmentBackground
import com.example.vellum.data.local.VellumDatabase
import com.example.vellum.data.local.AccountEntity
import com.example.vellum.data.local.CategoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ==========================================
// 1. Weekly Trend Widget Configure Activity
// ==========================================

class WeeklyTrendWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            VellumTheme {
                ParchmentBackground(modifier = Modifier.fillMaxSize()) {
                    WeeklyTrendConfigureScreen(
                        appWidgetId = appWidgetId,
                        onSave = { interval ->
                            val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("weekly_trend_interval_$appWidgetId", interval).apply()
                            
                            WeeklyTrendWidgetProvider.triggerUpdate(this@WeeklyTrendWidgetConfigureActivity)
                            
                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(Activity.RESULT_OK, resultValue)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyTrendConfigureScreen(appWidgetId: Int, onSave: (String) -> Unit) {
    val context = LocalContext.current
    val options = listOf(
        "weekly" to "Weekly Trend (Last 7 Days)",
        "monthly" to "Monthly Trend (Last 30 Days)",
        "yearly" to "Yearly Trend (Last 12 Months)",
        "all_time" to "All Time Trend"
    )
    val initialValue = remember {
        val prefs = context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
        prefs.getString("weekly_trend_interval_$appWidgetId", "weekly") ?: "weekly"
    }
    var selectedOption by remember { mutableStateOf(initialValue) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Configure Weekly Trend",
            fontFamily = ParchmentFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ParchmentDarkBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Select data interval for the chart",
            fontFamily = ParchmentFontFamily,
            fontSize = 16.sp,
            color = ParchmentDarkBrown.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedOption = value }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (selectedOption == value),
                    onClick = { selectedOption = value }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    fontFamily = ParchmentFontFamily,
                    fontSize = 18.sp,
                    color = ParchmentDarkBrown
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onSave(selectedOption) },
            colors = ButtonDefaults.buttonColors(containerColor = ParchmentDarkBrown)
        ) {
            Text(
                text = "Save Settings",
                color = MaterialTheme.colorScheme.background,
                fontFamily = ParchmentFontFamily,
                fontSize = 16.sp
            )
        }
    }
}

// ==========================================
// 2. Chalkboard Widget Configure Activity
// ==========================================

class ChalkboardWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            VellumTheme {
                ParchmentBackground(modifier = Modifier.fillMaxSize()) {
                    ChalkboardConfigureScreen(
                        appWidgetId = appWidgetId,
                        onSave = { accountId, period ->
                            val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                            prefs.edit()
                                .putString("chalkboard_account_$appWidgetId", accountId)
                                .putString("chalkboard_period_$appWidgetId", period)
                                .apply()
                            
                            ChalkboardWidgetProvider.triggerUpdate(this@ChalkboardWidgetConfigureActivity)
                            
                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(Activity.RESULT_OK, resultValue)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChalkboardConfigureScreen(appWidgetId: Int, onSave: (String, String) -> Unit) {
    val context = LocalContext.current
    var accounts by remember { mutableStateOf<List<AccountEntity>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val db = VellumDatabase.getInstance(context)
        accounts = withContext(Dispatchers.IO) {
            db.accountDao().getAllActiveAccounts()
        }
    }

    val prefs = remember { context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE) }
    val initialAccount = remember { prefs.getString("chalkboard_account_$appWidgetId", "all") ?: "all" }
    val initialPeriod = remember { prefs.getString("chalkboard_period_$appWidgetId", "daily") ?: "daily" }

    var selectedAccount by remember { mutableStateOf(initialAccount) }
    var selectedPeriod by remember { mutableStateOf(initialPeriod) }

    val periods = listOf(
        "daily" to "Daily (Today)",
        "weekly" to "Weekly Spend",
        "monthly" to "Monthly Spend",
        "yearly" to "Yearly Spend",
        "all_time" to "All Time Spend"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Configure Summary Widget",
            fontFamily = ParchmentFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ParchmentDarkBrown,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        Text(
            text = "Filter by Account:",
            fontFamily = ParchmentFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ParchmentDarkBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { selectedAccount = "all" }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = (selectedAccount == "all"), onClick = { selectedAccount = "all" })
            Spacer(modifier = Modifier.width(12.dp))
            Text("All Accounts", fontFamily = ParchmentFontFamily, fontSize = 16.sp, color = ParchmentDarkBrown)
        }

        accounts.forEach { acc ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedAccount = acc.id }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (selectedAccount == acc.id), onClick = { selectedAccount = acc.id })
                Spacer(modifier = Modifier.width(12.dp))
                Text(acc.name, fontFamily = ParchmentFontFamily, fontSize = 16.sp, color = ParchmentDarkBrown)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Select Period:",
            fontFamily = ParchmentFontFamily,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ParchmentDarkBrown,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        periods.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedPeriod = value }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = (selectedPeriod == value), onClick = { selectedPeriod = value })
                Spacer(modifier = Modifier.width(12.dp))
                Text(label, fontFamily = ParchmentFontFamily, fontSize = 16.sp, color = ParchmentDarkBrown)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { onSave(selectedAccount, selectedPeriod) },
            colors = ButtonDefaults.buttonColors(containerColor = ParchmentDarkBrown),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Save Settings",
                color = MaterialTheme.colorScheme.background,
                fontFamily = ParchmentFontFamily,
                fontSize = 16.sp
            )
        }
    }
}

// ==========================================
// 3. Quick Add Widget Configure Activity
// ==========================================

class QuickAddWidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)

        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            VellumTheme {
                ParchmentBackground(modifier = Modifier.fillMaxSize()) {
                    QuickAddConfigureScreen(
                        appWidgetId = appWidgetId,
                        onSave = { categoryIds ->
                            val prefs = getSharedPreferences("widget_prefs", Context.MODE_PRIVATE)
                            prefs.edit().putString("quick_add_categories_$appWidgetId", categoryIds.joinToString(",")).apply()
                            
                            QuickAddWidgetProvider.triggerUpdate(this@QuickAddWidgetConfigureActivity)
                            
                            val resultValue = Intent().apply {
                                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                            }
                            setResult(Activity.RESULT_OK, resultValue)
                            finish()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickAddConfigureScreen(appWidgetId: Int, onSave: (List<String>) -> Unit) {
    val context = LocalContext.current
    var categories by remember { mutableStateOf<List<CategoryEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        val db = VellumDatabase.getInstance(context)
        categories = withContext(Dispatchers.IO) {
            db.categoryDao().getAllActiveCategories().filter { it.type == "EXPENSE" }
        }
    }

    val prefs = remember { context.getSharedPreferences("widget_prefs", Context.MODE_PRIVATE) }
    val selectedIds = remember { mutableStateListOf<String>() }

    LaunchedEffect(categories) {
        if (categories.isNotEmpty() && selectedIds.isEmpty()) {
            val saved = prefs.getString("quick_add_categories_$appWidgetId", null)
            if (saved != null) {
                selectedIds.addAll(saved.split(",").filter { id -> categories.any { it.id == id } })
            } else {
                // Default to first 4 categories
                selectedIds.addAll(categories.take(4).map { it.id })
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "Configure Quick Add Widget",
            fontFamily = ParchmentFontFamily,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ParchmentDarkBrown,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
        )
        Text(
            text = "Select exactly 4 or 5 categories to show:",
            fontFamily = ParchmentFontFamily,
            fontSize = 16.sp,
            color = ParchmentDarkBrown.copy(alpha = 0.7f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 24.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(categories) { cat ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (selectedIds.contains(cat.id)) {
                                selectedIds.remove(cat.id)
                            } else {
                                if (selectedIds.size >= 5) {
                                    Toast.makeText(context, "Cannot select more than 5 categories", Toast.LENGTH_SHORT).show()
                                } else {
                                    selectedIds.add(cat.id)
                                }
                            }
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedIds.contains(cat.id),
                        onCheckedChange = { checked ->
                            if (checked) {
                                if (selectedIds.size >= 5) {
                                    Toast.makeText(context, "Cannot select more than 5 categories", Toast.LENGTH_SHORT).show()
                                } else {
                                    selectedIds.add(cat.id)
                                }
                            } else {
                                selectedIds.remove(cat.id)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "${cat.icon}  ${cat.name}",
                        fontFamily = ParchmentFontFamily,
                        fontSize = 18.sp,
                        color = ParchmentDarkBrown
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (selectedIds.size in 4..5) {
                    onSave(selectedIds)
                } else {
                    Toast.makeText(context, "Please select exactly 4 or 5 categories (Currently selected: ${selectedIds.size})", Toast.LENGTH_LONG).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = ParchmentDarkBrown),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Save Settings",
                color = MaterialTheme.colorScheme.background,
                fontFamily = ParchmentFontFamily,
                fontSize = 16.sp
            )
        }
    }
}
