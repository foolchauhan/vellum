package com.example.vellum.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.vellum.Settings
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import com.example.vellum.theme.*
import com.example.vellum.ui.tabs.*
import com.example.vellum.ui.dialogs.*

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    viewModel: MainScreenViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
        LandscapeReports(
            viewModel = viewModel,
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val context = LocalContext.current

    var showPeriodDialog by remember { mutableStateOf(false) }
    var showSwitchAccountDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showCategoryFilterDialog by remember { mutableStateOf(false) }
    var showAccountFilterDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val periodLabel by viewModel.periodLabel.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val userDisplayName by viewModel.userDisplayName.collectAsState()
    val userPhotoUrl by viewModel.userPhotoUrl.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val tabsPosition = preferences["tabs_position"] ?: "Top"

    val tabs = listOf("Spending", "Transactions", "Categories", "Accounts")
    val tabIcons = listOf(
        Icons.Default.LocalOffer,
        Icons.Default.Assignment,
        Icons.Default.Storage,
        Icons.Default.People
    )

    var pageBeforeSettings by remember { mutableStateOf(0) }
    var settingsOpenedFromSwipe by remember { mutableStateOf(true) }

    val pagerState = rememberPagerState(pageCount = { tabs.size + 1 })
    val coroutineScope = rememberCoroutineScope()
    val selectedTab = pagerState.currentPage
    val selectedTabForTabRow = selectedTab.coerceAtMost(tabs.size - 1)

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != 4) {
            settingsOpenedFromSwipe = true
        }
    }

    BackHandler(enabled = pagerState.currentPage == 4) {
        coroutineScope.launch {
            if (settingsOpenedFromSwipe) {
                pagerState.animateScrollToPage(3)
            } else {
                pagerState.animateScrollToPage(pageBeforeSettings)
            }
        }
    }

    val isBlurActive = showPeriodDialog ||
            showSwitchAccountDialog ||
            showShareDialog ||
            showCategoryFilterDialog ||
            showAccountFilterDialog

    Box(modifier = Modifier.fillMaxSize()) {
        // Base static background - drawn edge-to-edge
        com.example.vellum.ui.components.ParchmentBackground(
            modifier = if (isBlurActive) Modifier.fillMaxSize().blur(10.dp) else Modifier.fillMaxSize()
        ) {}

        Column(modifier = modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = pagerState.currentPage != 4,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                // 1. Global Top Bar (May on Left, Actions on Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Side: Period Button (Only for Spending and Transactions)
                    Box(modifier = Modifier.width(100.dp)) {
                        if (selectedTabForTabRow == 0 || selectedTabForTabRow == 1) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ParchmentBackground)
                                    .border(1.dp, ParchmentDarkBrown, RoundedCornerShape(6.dp))
                                    .clickable { showPeriodDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = periodLabel,
                                    color = ParchmentDarkBrown,
                                    fontFamily = ParchmentFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Right Side: Quick Add (+) and Overflow menu (three-dots)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (selectedTabForTabRow != 0) {
                            IconButton(onClick = {
                                when (selectedTabForTabRow) {
                                    1 -> onItemClick(com.example.vellum.AddEditTransaction(predefinedType = "EXPENSE"))
                                    2 -> onItemClick(com.example.vellum.AddEditCategory(predefinedType = "EXPENSE"))
                                    3 -> onItemClick(com.example.vellum.AddEditAccount())
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Item",
                                    tint = ParchmentDarkBrown,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        val initials = remember(userEmail, userDisplayName) {
                            val name = userDisplayName ?: userEmail ?: ""
                            if (name.isNotEmpty()) name.take(1).uppercase() else "?"
                        }

                        if (userEmail != null) {
                            val rotationModifier = if (isSyncing) {
                                val infiniteTransition = rememberInfiniteTransition(label = "SyncRotation")
                                val angle by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "Angle"
                                )
                                Modifier.graphicsLayer { rotationZ = angle }
                            } else {
                                Modifier
                            }
                            IconButton(
                                onClick = { viewModel.syncWithSheets() },
                                enabled = !isSyncing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync with Google Sheets",
                                    tint = if (isSyncing) ParchmentBlueText else ParchmentDarkBrown,
                                    modifier = Modifier.size(24.dp).then(rotationModifier)
                                )
                            }
                        }

                        IconButton(onClick = { showSwitchAccountDialog = true }) {
                            if (userEmail != null) {
                                if (userPhotoUrl != null) {
                                    AsyncImage(
                                        model = userPhotoUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .border(1.dp, ParchmentLine, androidx.compose.foundation.shape.CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(ParchmentDarkBrown)
                                            .border(1.dp, ParchmentLine, androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = initials,
                                            color = ParchmentBackground,
                                            fontFamily = ParchmentFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Switch Account",
                                    tint = ParchmentDarkBrown,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Box {
                            IconButton(onClick = { menuExpanded = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Options Menu",
                                    tint = ParchmentDarkBrown
                                )
                            }

                            DropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = { menuExpanded = false },
                                modifier = Modifier.background(ParchmentBackground)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Remove Ads", color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = {
                                        menuExpanded = false
                                        Toast.makeText(context, "Ads removed placeholder", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Backups", color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = { menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Like", color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = { menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Privacy Policy", color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = { menuExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Settings", color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = {
                                        menuExpanded = false
                                        pageBeforeSettings = pagerState.currentPage
                                        settingsOpenedFromSwipe = false
                                        coroutineScope.launch { pagerState.scrollToPage(4) }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Help", color = ParchmentDarkBrown, fontFamily = ParchmentFontFamily) },
                                    onClick = { menuExpanded = false }
                                )
                            }
                        }
                    }
                }
            }

            val tabRowContent = @Composable {
                TabRow(
                    selectedTabIndex = selectedTabForTabRow,
                    containerColor = Color.Transparent,
                    contentColor = ParchmentDarkBrown,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabForTabRow]),
                            color = ParchmentDarkBrown
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = (selectedTabForTabRow == index),
                            onClick = {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontFamily = ParchmentFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = tabIcons[index],
                                    contentDescription = title
                                )
                            },
                            selectedContentColor = ParchmentDarkBrown,
                            unselectedContentColor = TabUnselected
                        )
                    }
                }
            }

            val pagerContent = @Composable {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    userScrollEnabled = pagerState.currentPage != 4 || settingsOpenedFromSwipe
                ) { page ->
                    when (page) {
                        0 -> SpendingTab(
                            viewModel = viewModel,
                            onAddTransactionClicked = { type -> onItemClick(com.example.vellum.AddEditTransaction(predefinedType = type)) }
                        )
                        1 -> TransactionsTab(
                            viewModel = viewModel,
                            showShareDialog = showShareDialog,
                            onDismissShareDialog = { showShareDialog = false },
                            onShareClicked = { showShareDialog = true },
                            showCategoryFilterDialog = showCategoryFilterDialog,
                            onDismissCategoryFilterDialog = { showCategoryFilterDialog = false },
                            onCategoryFilterClicked = { showCategoryFilterDialog = true },
                            showAccountFilterDialog = showAccountFilterDialog,
                            onDismissAccountFilterDialog = { showAccountFilterDialog = false },
                            onAccountFilterClicked = { showAccountFilterDialog = true },
                            onNavigate = onItemClick
                        )
                        2 -> CategoriesTab(
                            viewModel = viewModel,
                            onNavigate = onItemClick
                        )
                        3 -> AccountsTab(
                            viewModel = viewModel,
                            onNavigate = onItemClick
                        )
                        4 -> SettingsScreen(
                            viewModel = viewModel,
                            onBackClicked = {
                                coroutineScope.launch {
                                    if (settingsOpenedFromSwipe) {
                                        pagerState.animateScrollToPage(3)
                                    } else {
                                        pagerState.animateScrollToPage(pageBeforeSettings)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> Box(modifier = Modifier.fillMaxSize())
                    }
                }
            }

            if (tabsPosition == "Top") {
                AnimatedVisibility(
                    visible = pagerState.currentPage != 4,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    tabRowContent()
                }
                pagerContent()
            } else {
                pagerContent()
                AnimatedVisibility(
                    visible = pagerState.currentPage != 4,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    tabRowContent()
                }
            }
        }
    }



    if (showPeriodDialog) {
        ShowSpendingDialog(
            viewModel = viewModel,
            onDismiss = { showPeriodDialog = false }
        )
    }

    if (showSwitchAccountDialog) {
        SwitchAccountDialog(
            viewModel = viewModel,
            onDismiss = { showSwitchAccountDialog = false }
        )
    }
}
