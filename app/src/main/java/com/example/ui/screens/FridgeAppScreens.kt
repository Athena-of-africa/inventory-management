package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ActivityLog
import com.example.data.InventoryItem
import com.example.ui.InventoryViewModel
import com.example.ui.ScannedItemDetails
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FridgeAppMainScreen(viewModel: InventoryViewModel) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    if (!isAuthenticated) {
        AuthScreen(
            viewModel = viewModel,
            snackbarHostState = snackbarHostState
        )
    } else {
        var selectedTab by rememberSaveable { mutableStateOf(0) }
        var showManualAddDialog by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        // Fully reactive states connected to the Room database schema
        val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
        val currentHousehold by viewModel.currentHousehold.collectAsStateWithLifecycle()

        val currentUserName = currentUser?.name ?: "Chef Alex"
        val householdName = currentHousehold?.name ?: "Sweet family home"

        var profileMenuExpanded by remember { mutableStateOf(false) }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Kitchen,
                                    contentDescription = "Fridge Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Smart Fridge",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    letterSpacing = (-0.3).sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = householdName.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                letterSpacing = 1.2.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                        Box {
                            IconButton(
                                onClick = { profileMenuExpanded = true }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = currentUserName.firstOrNull()?.toString() ?: "U",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }

                            DropdownMenu(
                                expanded = profileMenuExpanded,
                                onDismissRequest = { profileMenuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text(
                                        text = currentUserName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = currentUser?.email ?: "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (currentUser?.isAdmin == true) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                                else MaterialTheme.colorScheme.secondaryContainer
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (currentUser?.isAdmin == true) "Household Admin" else "Standard Member",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentUser?.isAdmin == true) MaterialTheme.colorScheme.error
                                                    else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                DropdownMenuItem(
                                    text = { Text("Family portal info", fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        profileMenuExpanded = false
                                        selectedTab = 3
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Sign Out", color = MaterialTheme.colorScheme.error, fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        profileMenuExpanded = false
                                        viewModel.handleSignOut()
                                    }
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, "Dashboard") },
                        label = { Text("Inventory") },
                        modifier = Modifier.testTag("nav_tab_dashboard")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.QrCodeScanner, "Barcode Scanner") },
                        label = { Text("Scan") },
                        modifier = Modifier.testTag("nav_tab_scan")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.History, "Activity Log") },
                        label = { Text("Logs") },
                        modifier = Modifier.testTag("nav_tab_logs")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.Group, "Family Portal") },
                        label = { Text("Family") },
                        modifier = Modifier.testTag("nav_tab_family")
                    )
                }
            },
            floatingActionButton = {
                if (selectedTab == 0) {
                    ExtendedFloatingActionButton(
                        text = { Text("Add Manually") },
                        icon = { Icon(Icons.Default.Add, "Add manual item") },
                        onClick = { showManualAddDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_add_item")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(
                        viewModel = viewModel,
                        activeUser = currentUserName,
                        onNavigateToScan = { selectedTab = 1 }
                    )
                    1 -> ScanSimulatorScreen(
                        viewModel = viewModel,
                        activeUser = currentUserName,
                        snackbarHostState = snackbarHostState
                    )
                    2 -> ActivityHistoryScreen(viewModel = viewModel)
                    3 -> FamilyAndGroupsScreen(
                        viewModel = viewModel,
                        snackbarHostState = snackbarHostState
                    )
                }

                if (showManualAddDialog) {
                    ManualAddDialog(
                        onDismiss = { showManualAddDialog = false },
                        onConfirm = { name, quantity, unit, category ->
                            viewModel.addItem(name, quantity, unit, category, currentUserName)
                            showManualAddDialog = false
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 1: THE INVENTORY DASHBOARD
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: InventoryViewModel,
    activeUser: String,
    onNavigateToScan: () -> Unit
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var itemToDeletePortions by remember { mutableStateOf<InventoryItem?>(null) }

    val categories = listOf("All", "Produce", "Dairy", "Meat & Poultry", "Beverages", "Bakery", "Other")

    val filteredItems = items.filter { item ->
        val matchesSearch = item.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || item.category == selectedCategory
        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Stats Summary Header Cards
        StatsSummaryHeader(items = items)

        Spacer(modifier = Modifier.height(16.dp))

        // Search and Filter Elements
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search fridge items...") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, "Clear Search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_input_dashboard"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Category Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            categories.forEach { category ->
                val isSelected = selectedCategory == category
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = category },
                    label = { Text(category) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("chip_category_${category.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Items List or Empty state
        if (filteredItems.isEmpty()) {
            EmptyStateView(
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                onNavigateToScan = onNavigateToScan
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filteredItems,
                    key = { it.id }
                ) { item ->
                    InventoryItemCard(
                        item = item,
                        activeUser = activeUser,
                        onQuickAdd = {
                            viewModel.addItem(
                                name = item.name,
                                quantity = 1,
                                unit = item.unit,
                                category = item.category,
                                addedBy = activeUser,
                                barcode = item.barcode
                            )
                        },
                        onQuickTakeOut = {
                            viewModel.takeOutItem(item.id, 1, activeUser) {}
                        },
                        onDelete = {
                            itemToDeletePortions = item
                        },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    itemToDeletePortions?.let { item ->
        DeletePortionsDialog(
            item = item,
            activeUser = activeUser,
            onDismiss = { itemToDeletePortions = null },
            onDeletePortions = { portions ->
                viewModel.takeOutItem(item.id, portions, activeUser) {
                    itemToDeletePortions = null
                }
            },
            onDeleteEntirely = {
                viewModel.deleteItem(item)
                itemToDeletePortions = null
            }
        )
    }
}

@Composable
fun StatsSummaryHeader(items: List<InventoryItem>) {
    val totalItemsCount = items.filter { it.status == "In Stock" || it.status == "Running Low" }.sumOf { it.quantity }
    val runningLowCount = items.count { it.status == "Running Low" }
    val outOfStockCount = items.count { it.status == "Out of Stock" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Highlight metric mimicking Professional Polish banner
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth().testTag("stats_summary_highlight")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Active Food Count",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = totalItemsCount.toString(),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = " portions total",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.08f)),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Live Syncing",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Secondary metrics group
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsCard(
                title = "Running Low",
                value = runningLowCount.toString(),
                subtitle = "need restocking",
                containerColor = if (isSystemInDarkTheme()) Color(0xFF3B2F21) else Color(0xFFFCEDDD),
                contentColor = if (isSystemInDarkTheme()) Color(0xFFF6A94E) else Color(0xFF904F00),
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f)
            )
            StatsCard(
                title = "Out of Stock",
                value = outOfStockCount.toString(),
                subtitle = "empty shelves",
                containerColor = if (isSystemInDarkTheme()) Color(0xFF3F2123) else Color(0xFFFCDDDF),
                contentColor = if (isSystemInDarkTheme()) Color(0xFFEC8A8C) else Color(0xFF9E2A2B),
                icon = Icons.Default.ErrorOutline,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatsCard(
    title: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor.copy(alpha = 0.8f)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor
            )
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = contentColor.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun EmptyStateView(
    searchQuery: String,
    selectedCategory: String,
    onNavigateToScan: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = "Empty state icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotEmpty()) {
                    "No items matching \"$searchQuery\""
                } else if (selectedCategory != "All") {
                    "No items in \"$selectedCategory\" category"
                } else {
                    "Your Fridge is Empty!"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (searchQuery.isNotEmpty() || selectedCategory != "All") {
                    "Try checking your spelling, looking under another shelf category, or add the food as a custom item."
                } else {
                    "Scan item barcodes or tap manual input to start loading items into your refrigerator database."
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onNavigateToScan,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Barcode Simulator")
            }
        }
    }
}

@Composable
fun InventoryItemCard(
    item: InventoryItem,
    activeUser: String,
    onQuickAdd: () -> Unit,
    onQuickTakeOut: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (item.category) {
        "Dairy" -> Color(0xFFD3E4FF)      // Sophisticated Soft Blue
        "Produce" -> Color(0xFFE2F1E3)    // Sophisticated Soft Green
        "Meat & Poultry" -> Color(0xFFFCDDDF) // Sophisticated Soft Coral
        "Beverages" -> Color(0xFFFCF4DD)   // Sophisticated Soft Cream
        "Bakery" -> Color(0xFFFCEDDD)     // Sophisticated Soft Peach
        else -> Color(0xFFE2E2EC)         // Sophisticated Soft Grey
    }

    val icon = when (item.category) {
        "Dairy" -> Icons.Default.Icecream
        "Produce" -> Icons.Default.Grass
        "Meat & Poultry" -> Icons.Default.Dining
        "Beverages" -> Icons.Default.LocalCafe
        "Bakery" -> Icons.Default.BakeryDining
        else -> Icons.Default.Fastfood
    }

    val badgeColor = when (item.status) {
        "In Stock" -> if (isSystemInDarkTheme()) Color(0xFF223E2A) else Color(0xFFE2F1E3)
        "Running Low" -> if (isSystemInDarkTheme()) Color(0xFF3B2F21) else Color(0xFFFCEDDD)
        else -> if (isSystemInDarkTheme()) Color(0xFF3F2123) else Color(0xFFFCDDDF)
    }

    val badgeTextColor = when (item.status) {
        "In Stock" -> if (isSystemInDarkTheme()) Color(0xFF8CE1A3) else Color(0xFF2E6C3F)
        "Running Low" -> if (isSystemInDarkTheme()) Color(0xFFF6A94E) else Color(0xFF904F00)
        else -> if (isSystemInDarkTheme()) Color(0xFFEC8A8C) else Color(0xFF9E2A2B)
    }

    var showQuickAdjustPanel by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { showQuickAdjustPanel = !showQuickAdjustPanel }
            .testTag("item_card_${item.name.lowercase().replace(" ", "_")}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Colored Category Accent Banner
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(categoryColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = item.category,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Item Details - Professional Polish text pairing
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Added by ${item.addedBy} • ${item.category}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Trailing metrics (Quantity and status badge)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${item.quantity} ${item.unit}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = badgeTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            // Expanded Quick Adjust Panel (ADD/REMOVE slider style)
            AnimatedVisibility(
                visible = showQuickAdjustPanel,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Quick Adjust as $activeUser:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = onQuickTakeOut,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            enabled = item.quantity > 0,
                            modifier = Modifier.testTag("item_quick_take_${item.id}")
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Take Out", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Take Out (-1)", fontSize = 11.sp)
                        }

                        Button(
                            onClick = onQuickAdd,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("item_quick_add_${item.id}")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add (+1)", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: HIGH-FIDELITY CAMERA VIEW / SCAN SIMULATOR
// ==========================================
@Composable
fun ScanSimulatorScreen(
    viewModel: InventoryViewModel,
    activeUser: String,
    snackbarHostState: SnackbarHostState
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val scannedBarcode by viewModel.scannedBarcode.collectAsStateWithLifecycle()
    val scannedMetadata by viewModel.scannedMetadata.collectAsStateWithLifecycle()

    var customBarcode by remember { mutableStateOf("") }
    var adjustQty by remember { mutableStateOf("1") }
    var actionPersonName by remember { mutableStateOf(activeUser) }

    // Synchronize default action person name when top action user refreshes
    LaunchedEffect(activeUser) {
        actionPersonName = activeUser
    }

    val presetDatabaseList = viewModel.barcodePresetDatabase.values.toList()
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Mock Camera Viewfinder Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Simulated camera scanner overlay
            ScanViewfinderGuide()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = "CAM_SIM ACTIVE: SELECT PRESET SCAN",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Preset Scan Items Section
        Text(
            text = "🛒 Tap a Preset to Simulate Scan",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Tap a product below to simulate waving its barcode in front of the scanner.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Grid of Presets
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(145.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presetDatabaseList) { preset ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            preset.barcode?.let { viewModel.onBarcodeScanned(it) }
                        }
                        .testTag("scan_preset_${preset.name.lowercase().replace(" ", "_")}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val icon = when (preset.category) {
                            "Dairy" -> Icons.Default.Icecream
                            "Produce" -> Icons.Default.Grass
                            "Beverages" -> Icons.Default.LocalCafe
                            "Meat & Poultry" -> Icons.Default.Dining
                            else -> Icons.Default.Fastfood
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = preset.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Code: ${preset.barcode}",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            Text(
                "OR SCAN CUSTOM CODE",
                modifier = Modifier.padding(horizontal = 8.dp),
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Bold
            )
            Divider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Custom Barcode input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customBarcode,
                onValueChange = { customBarcode = it },
                label = { Text("Enter barcode number") },
                placeholder = { Text("E.g., 0120000424") },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("custom_barcode_input"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    if (customBarcode.isNotBlank()) {
                        viewModel.onBarcodeScanned(customBarcode)
                        customBarcode = ""
                    }
                },
                enabled = customBarcode.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(56.dp)
                    .testTag("trigger_custom_scan"),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.KeyboardTab, "Submit")
            }
        }

        // SCANNED METADATA DRAWER / INTERACTION CARD
        AnimatedVisibility(
            visible = scannedBarcode != null,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            scannedMetadata?.let { metadata ->
                Spacer(modifier = Modifier.height(24.dp))
                ScannedActionPanel(
                    metadata = metadata,
                    adjustQty = adjustQty,
                    onAdjustQtyChange = { adjustQty = it },
                    actionPersonName = actionPersonName,
                    onPersonNameChange = { actionPersonName = it },
                    onAddSubmit = {
                        val qty = adjustQty.toIntOrNull() ?: 1
                        val nameStr = metadata.name.ifBlank { "Custom Scanned Item" }
                        viewModel.addItem(
                            name = nameStr,
                            quantity = qty,
                            unit = metadata.unit,
                            category = metadata.category,
                            addedBy = actionPersonName,
                            barcode = metadata.barcode
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Successfully added $qty ${metadata.unit} of $nameStr")
                        }
                        viewModel.clearScannedResult()
                        adjustQty = "1"
                    },
                    onRemoveSubmit = {
                        val qty = adjustQty.toIntOrNull() ?: 1
                        val id = items.find { it.barcode == metadata.barcode }?.id
                        if (id != null) {
                            viewModel.takeOutItem(id, qty, actionPersonName) { success ->
                                scope.launch {
                                    if (success) {
                                        snackbarHostState.showSnackbar("Took out $qty ${metadata.unit} of ${metadata.name}")
                                    } else {
                                        snackbarHostState.showSnackbar("Failed: Not enough stock!")
                                    }
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar("Error: This scanned product is not in the fridge!")
                            }
                        }
                        viewModel.clearScannedResult()
                        adjustQty = "1"
                    },
                    onCustomDetailsAddSubmit = { customName, customCategory, customUnit, customQty, customPerson ->
                        viewModel.addItem(
                            name = customName,
                            quantity = customQty,
                            unit = customUnit,
                            category = customCategory,
                            addedBy = customPerson,
                            barcode = metadata.barcode
                        )
                        scope.launch {
                            snackbarHostState.showSnackbar("Created and added $customQty $customUnit of $customName")
                        }
                        viewModel.clearScannedResult()
                        adjustQty = "1"
                    },
                    onCancel = {
                        viewModel.clearScannedResult()
                        adjustQty = "1"
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun ScanViewfinderGuide() {
    val infiniteTransition = rememberInfiniteTransition(label = "Laser scanner line animation")
    val translationY by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Laser line displacement"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val strokeWidth = 3.dp.toPx()
                val linePosition = size.height / 2 + translationY.dp.toPx()

                // Draw red fluorescent scanning laser line
                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFF2D55),
                            Color(0xFFFF2D55),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, linePosition),
                    end = Offset(size.width, linePosition),
                    strokeWidth = strokeWidth
                )

                // Viewfinder rect layout corners
                val cornerSize = 20.dp.toPx()
                val rectW = size.width * 0.7f
                val rectH = size.height * 0.6f
                val borderCol = Color.White.copy(alpha = 0.4f)

                val leftX = (size.width - rectW) / 2
                val rightX = leftX + rectW
                val topY = (size.height - rectH) / 2
                val bottomY = topY + rectH

                // Top Left Corner
                drawLine(borderCol, Offset(leftX, topY), Offset(leftX + cornerSize, topY), 4f)
                drawLine(borderCol, Offset(leftX, topY), Offset(leftX, topY + cornerSize), 4f)

                // Top Right Corner
                drawLine(borderCol, Offset(rightX, topY), Offset(rightX - cornerSize, topY), 4f)
                drawLine(borderCol, Offset(rightX, topY), Offset(rightX, topY + cornerSize), 4f)

                // Bottom Left Corner
                drawLine(borderCol, Offset(leftX, bottomY), Offset(leftX + cornerSize, bottomY), 4f)
                drawLine(borderCol, Offset(leftX, bottomY), Offset(leftX, bottomY - cornerSize), 4f)

                // Bottom Right Corner
                drawLine(borderCol, Offset(rightX, bottomY), Offset(rightX - cornerSize, bottomY), 4f)
                drawLine(borderCol, Offset(rightX, bottomY), Offset(rightX, bottomY - cornerSize), 4f)
            }
    )
}

@Composable
fun ScannedActionPanel(
    metadata: ScannedItemDetails,
    adjustQty: String,
    onAdjustQtyChange: (String) -> Unit,
    actionPersonName: String,
    onPersonNameChange: (String) -> Unit,
    onAddSubmit: () -> Unit,
    onRemoveSubmit: () -> Unit,
    onCustomDetailsAddSubmit: (String, String, String, Int, String) -> Unit,
    onCancel: () -> Unit
) {
    val isNewCustomType = metadata.name.isBlank()
    var customNameInput by remember { mutableStateOf(metadata.name) }
    var customCategorySelection by remember { mutableStateOf(metadata.category) }
    var customUnitSelection by remember { mutableStateOf(metadata.unit) }

    val categories = listOf("Produce", "Dairy", "Meat & Poultry", "Beverages", "Bakery", "Other")
    val units = listOf("pcs", "bottle", "carton", "pack", "can", "dozen")

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("scanned_feedback_panel"),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "SCAN MATCH!",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                IconButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, "Dismiss")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (!isNewCustomType) {
                // Known / Preset Product Details
                Text(
                    text = metadata.name,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Category: ${metadata.category} | Barcode: ${metadata.barcode}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )

                if (metadata.isAlreadyInInventory) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Currently inside Fridge: ${metadata.currentQtyInInventory} ${metadata.unit}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity and Operator Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = adjustQty,
                        onValueChange = { onAdjustQtyChange(it) },
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scan_qty_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = actionPersonName,
                        onValueChange = { onPersonNameChange(it) },
                        label = { Text("Who is updating?") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(2f)
                            .testTag("scan_person_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: ADD to store vs REMOVE / TAKE out from store
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // TAKE OUT BUTTON
                    OutlinedButton(
                        onClick = onRemoveSubmit,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("scan_take_out_btn"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = metadata.isAlreadyInInventory && (adjustQty.toIntOrNull() ?: 0) > 0,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Remove, "Take Out")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Take OUT")
                    }

                    // ADD BUTTON
                    Button(
                        onClick = onAddSubmit,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("scan_add_btn"),
                        shape = RoundedCornerShape(12.dp),
                        enabled = (adjustQty.toIntOrNull() ?: 0) > 0 && actionPersonName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add IN")
                    }
                }
            } else {
                // UNKNOWN CUSTOM PRODUCT DETAILS INPUT FORM
                Text(
                    text = "New Code Detected!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "This item code (${metadata.barcode}) isn't in your fridge lookup presets yet. Create a custom template for it below:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = customNameInput,
                    onValueChange = { customNameInput = it },
                    label = { Text("Product Name") },
                    placeholder = { Text("E.g., Greek Yogurt, Diet Soda") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_scan_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Custom Category Selector
                Text("Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = customCategorySelection == cat,
                            onClick = { customCategorySelection = cat },
                            label = { Text(cat) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Custom Unit Selection
                Text("Unit Measure:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    units.forEach { unitVal ->
                        FilterChip(
                            selected = customUnitSelection == unitVal,
                            onClick = { customUnitSelection = unitVal },
                            label = { Text(unitVal) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = adjustQty,
                        onValueChange = { onAdjustQtyChange(it) },
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = actionPersonName,
                        onValueChange = { onPersonNameChange(it) },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.weight(2f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val qtyVal = adjustQty.toIntOrNull() ?: 1
                        if (customNameInput.isNotBlank() && actionPersonName.isNotBlank()) {
                            onCustomDetailsAddSubmit(
                                customNameInput,
                                customCategorySelection,
                                customUnitSelection,
                                qtyVal,
                                actionPersonName
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("custom_scan_submit_btn"),
                    shape = RoundedCornerShape(12.dp),
                    enabled = customNameInput.isNotBlank() && actionPersonName.isNotBlank() && (adjustQty.toIntOrNull() ?: 0) > 0
                ) {
                    Icon(Icons.Default.Check, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register & Save to Fridge")
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: ACTIVITY HISTORY LOGS
// ==========================================
@Composable
fun ActivityHistoryScreen(viewModel: InventoryViewModel) {
    val logs by viewModel.logs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Activity Log Tracker",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Logs who added or took out food, and when.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "${logs.size} actions",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Activities Recorded Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Actions will appear here as you add and remove items.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { log ->
                    ActivityLogItemCard(log = log)
                }
            }
        }
    }
}

@Composable
fun ActivityLogItemCard(log: ActivityLog) {
    val isAdd = log.actionType == "ADD"
    val timestampFormatted = remember(log.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
        sdf.format(Date(log.timestamp))
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Action Type visual Indicator Circle
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAdd) Color(0xFFE6F4EA) else Color(0xFFFCE8E6)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAdd) Icons.Default.AddCircle else Icons.Default.RemoveCircle,
                    contentDescription = null,
                    tint = if (isAdd) Color(0xFF137333) else Color(0xFFC5221F),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = buildString {
                        append(log.personName)
                        append(if (isAdd) " added " else " took out ")
                        append(log.quantity)
                        append(" portion(s) of ")
                        append(log.itemName)
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timestampFormatted,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// ==========================================
// FORM DIALOGS OR SHEETS
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }
    var selectedCategory by remember { mutableStateOf("Produce") }
    var selectedUnit by remember { mutableStateOf("pcs") }

    val categories = listOf("Produce", "Dairy", "Meat & Poultry", "Beverages", "Bakery", "Other")
    val units = listOf("pcs", "bottle", "carton", "pack", "can", "dozen")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Add Item Manually",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name") },
                    placeholder = { Text("E.g., Butter, Spinach") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Quantity") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("manual_qty_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Compact unit field
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Unit Grid", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            units.forEach { unitStr ->
                                FilterChip(
                                    selected = selectedUnit == unitStr,
                                    onClick = { selectedUnit = unitStr },
                                    label = { Text(unitStr, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Text("Category Shelf", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toIntOrNull() ?: 1
                    if (name.isNotBlank() && qty > 0) {
                        onConfirm(name, qty, selectedUnit, selectedCategory)
                    }
                },
                enabled = name.isNotBlank() && (quantityText.toIntOrNull() ?: 0) > 0,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Put in Fridge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

// ==========================================================
// SCREEN 4: FAMILY & GROUPS (MULTI-USER & REAL-TIME PORTAL)
// ==========================================================
@Composable
fun FamilyAndGroupsScreen(
    viewModel: InventoryViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val currentHousehold by viewModel.currentHousehold.collectAsStateWithLifecycle()
    val members by viewModel.currentHouseholdMembers.collectAsStateWithLifecycle()
    val allHouseholds by viewModel.households.collectAsStateWithLifecycle()

    var showAddUserDialog by remember { mutableStateOf(false) }
    var showJoinHouseholdDialog by remember { mutableStateOf(false) }
    var showCreateHouseholdDialog by remember { mutableStateOf(false) }

    // Manual Local authentication simulation fields
    var loginNameText by remember { mutableStateOf("") }
    var loginEmailText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Header
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "FAMILY COLLABORATION HUB",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Multi-User Cloud Synced Fridge",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Switch between households or click any member profile to switch sessions. The app list, logs, and updates respond instantly.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // Active Session Node Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Session Context",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentUser?.name?.firstOrNull()?.toString()?.uppercase() ?: "U",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active User: ${currentUser?.name ?: "No Name"}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = currentUser?.email ?: "anonymous@local.host",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Current Household ID: ${currentHousehold?.name ?: "None"}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Invite Sync Code: #${currentHousehold?.inviteCode ?: "NONE"}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        // Admin Provisioning panel or lock banner
        item {
            val isAdmin = currentUser?.isAdmin == true
            if (isAdmin) {
                var provNameText by remember { mutableStateOf("") }
                var provEmailText by remember { mutableStateOf("") }
                var provPasswordText by remember { mutableStateOf("") }
                var provIsAdminFlag by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Admin Member Provisioning",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "As the household administrator, you can provision securely synced account credentials for and add members.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                        )

                        OutlinedTextField(
                            value = provNameText,
                            onValueChange = { provNameText = it },
                            label = { Text("Display Name (Username)") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = provEmailText,
                            onValueChange = { provEmailText = it },
                            label = { Text("Email (for SignIn)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = provPasswordText,
                            onValueChange = { provPasswordText = it },
                            label = { Text("Assigned Login Password") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Grant Household Admin Privileges",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Switch(
                                checked = provIsAdminFlag,
                                onCheckedChange = { provIsAdminFlag = it }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                viewModel.handleAdminProvisionMember(
                                    provNameText,
                                    provEmailText,
                                    provPasswordText,
                                    provIsAdminFlag
                                ) { ok, msg ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                        if (ok) {
                                            provNameText = ""
                                            provEmailText = ""
                                            provPasswordText = ""
                                            provIsAdminFlag = false
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Provision Account Credentials")
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Administrative Tools Locked",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Only household administrators (e.g. Admin Dave) can provision credentials or subtract users from registration.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Active Group Directories (Members Directory)
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Household Members Directory",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${members.size} users linked in database",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (members.isEmpty()) {
                        Text(
                            text = "No linked users inside this household group.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        members.forEach { memberUser ->
                            val isActive = currentUser?.id == memberUser.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                           else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                                ),
                                color = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.selectUser(memberUser)
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Switched active user session to ${memberUser.name}")
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isActive) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = memberUser.name.firstOrNull()?.toString()?.uppercase() ?: "F",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                                                        else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = memberUser.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (isActive) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "Active Session",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            } else if (memberUser.isAdmin) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f))
                                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                                ) {
                                                    Text(
                                                        text = "Admin",
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = memberUser.email,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Remove user action representing user subtraction feature
                                    if (currentUser?.isAdmin == true) {
                                        IconButton(
                                            onClick = {
                                                viewModel.handleRemoveUserFromActiveGroup(memberUser) { ok, msg ->
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(msg)
                                                    }
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "Delete member",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Protected account",
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Household selection switcher grid/cards list
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Active Household Sync List",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Switching groups recreates synced listing in real-time.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            IconButton(
                                onClick = { showJoinHouseholdDialog = true },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.GroupAdd, contentDescription = "Join code")
                            }
                            IconButton(
                                onClick = { showCreateHouseholdDialog = true },
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(Icons.Default.AddHome, contentDescription = "Create Node")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    allHouseholds.forEach { houseObj ->
                        val isCurrent = currentHousehold?.id == houseObj.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                    else MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectHousehold(houseObj)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Synchronized lists connected to: ${houseObj.name}")
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = houseObj.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        if (isCurrent) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.CloudSync,
                                                contentDescription = "Syncing",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Sync Code: #${houseObj.inviteCode}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                                
                                Icon(
                                    imageVector = if (isCurrent) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogue prompts for and modeling relational DB settings

    if (showJoinHouseholdDialog) {
        var codeValue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showJoinHouseholdDialog = false },
            title = { Text("Join Household Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Connect to an existing family database using their Invite Sync Code (e.g., SMITH123 or WORK456).",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = codeValue,
                        onValueChange = { codeValue = it },
                        label = { Text("Sync Code") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleJoinByInviteCode(codeValue) { ok, msg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                                if (ok) showJoinHouseholdDialog = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinHouseholdDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showCreateHouseholdDialog) {
        var propHouseName by remember { mutableStateOf("") }
        var propCode by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateHouseholdDialog = false },
            title = { Text("Create New Group") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Build a brand new shared compartment for synchronized food items.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value = propHouseName,
                        onValueChange = { propHouseName = it },
                        label = { Text("Group Compartment Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = propCode,
                        onValueChange = { propCode = it },
                        label = { Text("Unique Sync Code") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.handleRegisterAndJoinHousehold(propHouseName, propCode) { ok, msg ->
                            scope.launch {
                                snackbarHostState.showSnackbar(msg)
                                if (ok) showCreateHouseholdDialog = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateHouseholdDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    viewModel: InventoryViewModel,
    snackbarHostState: SnackbarHostState
) {
    val scope = rememberCoroutineScope()
    var isSignUpMode by remember { mutableStateOf(false) }

    // Log In states
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    // Register states
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") } // Clear default so user can type or see empty
    var createNewHousehold by remember { mutableStateOf(false) }
    var registerHouseholdName by remember { mutableStateOf("") }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Kitchen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Smart Fridge Hub",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Display Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "COLLABORATIVE INVENTORY SYNC",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Single persistent database. Shared updates. Dynamic logs.",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Authentication Modes Swapper (Tab Row look-alike)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isSignUpMode) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isSignUpMode = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Sign In",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (!isSignUpMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSignUpMode) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isSignUpMode = true }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Create Account",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSignUpMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (!isSignUpMode) {
                    // Sign In Screen Card Flow
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Access Existing Household",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = loginEmail,
                                onValueChange = { loginEmail = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = loginPassword,
                                onValueChange = { loginPassword = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Button(
                                onClick = {
                                    viewModel.handleCredentialSignIn(loginEmail, loginPassword) { ok, msg ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(msg)
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Log Into Household", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Preconfigured logins cards for testing demonstration
                    Text(
                        text = "Quick Demo Selection Accounts:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val demos = listOf(
                            Triple("Admin Dave (Admin)", "admin@smith.com", "admin123"),
                            Triple("Chef Alex (Member)", "alex@smith.com", "alexpassword"),
                            Triple("Manager Kate (Admin @Office)", "kate@office.com", "katepassword")
                        )

                        demos.forEach { (label, email, password) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        loginEmail = email
                                        loginPassword = password
                                    },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(text = "$email • pass: $password", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                                    }
                                    Icon(
                                        imageVector = Icons.Default.NavigateNext,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Sign Up Screen Card Flow
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Register Into Household",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Unified selection hub
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (!createNewHousehold) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { createNewHousehold = false }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Group,
                                            contentDescription = null,
                                            tint = if (!createNewHousehold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Join Existing Code",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (!createNewHousehold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (createNewHousehold) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { createNewHousehold = true }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = if (createNewHousehold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Create Own Hub",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (createNewHousehold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            OutlinedTextField(
                                value = registerName,
                                onValueChange = { registerName = it },
                                label = { Text("Display Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = registerEmail,
                                onValueChange = { registerEmail = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = registerPassword,
                                onValueChange = { registerPassword = it },
                                label = { Text("Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!createNewHousehold) {
                                OutlinedTextField(
                                    value = inviteCode,
                                    onValueChange = { inviteCode = it },
                                    label = { Text("Household Sync Code") },
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Enter SMITH123 for Sweet family home or WORK456 for Office breakroom.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                Button(
                                    onClick = {
                                        viewModel.handleCredentialSignUp(
                                            fullName = registerName,
                                            emailText = registerEmail,
                                            passwordText = registerPassword,
                                            householdInviteCode = inviteCode
                                        ) { ok, msg ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Register & Join Household", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                OutlinedTextField(
                                    value = registerHouseholdName,
                                    onValueChange = { registerHouseholdName = it },
                                    label = { Text("Brand New Household Name") },
                                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = inviteCode,
                                    onValueChange = { inviteCode = it },
                                    label = { Text("Create Unique Sync Code") },
                                    leadingIcon = { Icon(Icons.Default.Group, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "Choose any non-existent code (e.g. MILLER77). You will register as the Admin of this new group context.",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                Button(
                                    onClick = {
                                        viewModel.handleCreateHouseholdAndSignUp(
                                            fullName = registerName,
                                            emailText = registerEmail,
                                            passwordText = registerPassword,
                                            householdNameText = registerHouseholdName,
                                            householdInviteCode = inviteCode
                                        ) { ok, msg ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Create Household & Sign Up", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletePortionsDialog(
    item: InventoryItem,
    activeUser: String,
    onDismiss: () -> Unit,
    onDeletePortions: (Int) -> Unit,
    onDeleteEntirely: () -> Unit
) {
    var portionsToDelete by remember { mutableStateOf(1) }
    var inputString by remember { mutableStateOf("1") }
    
    val maxQty = item.quantity
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Delete Portions",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Specify how many portions of \"${item.name}\" to delete or remove at once.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Stock:",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                        )
                        Text(
                            text = "$maxQty ${item.unit}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Portions to Delete: $portionsToDelete / $maxQty ${item.unit}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    
                    if (maxQty > 1) {
                        Slider(
                            value = portionsToDelete.toFloat(),
                            onValueChange = {
                                val intVal = it.toInt().coerceIn(1, maxQty)
                                portionsToDelete = intVal
                                inputString = intVal.toString()
                            },
                            valueRange = 1f..maxQty.toFloat(),
                            steps = if (maxQty > 2) maxQty - 2 else 0,
                            modifier = Modifier.fillMaxWidth().testTag("portion_slider")
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            portionsToDelete = 1
                            inputString = "1"
                        },
                        modifier = Modifier.weight(1f).testTag("quick_delete_1"),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("1 Unit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    if (maxQty >= 4) {
                        val quarter = maxQty / 4
                        FilledTonalButton(
                            onClick = {
                                portionsToDelete = quarter
                                inputString = quarter.toString()
                            },
                            modifier = Modifier.weight(1f).testTag("quick_delete_quarter"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("1/4 ($quarter)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    if (maxQty >= 2) {
                        val half = maxQty / 2
                        FilledTonalButton(
                            onClick = {
                                portionsToDelete = half
                                inputString = half.toString()
                            },
                            modifier = Modifier.weight(1f).testTag("quick_delete_half"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Half ($half)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Button(
                        onClick = {
                            portionsToDelete = maxQty
                            inputString = maxQty.toString()
                        },
                        modifier = Modifier.weight(1.2f).testTag("quick_delete_all"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("All ($maxQty)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                OutlinedTextField(
                    value = inputString,
                    onValueChange = { newVal ->
                        val filtered = newVal.filter { it.isDigit() }
                        inputString = filtered
                        if (filtered.isNotEmpty()) {
                            val parsed = filtered.toIntOrNull()
                            if (parsed != null) {
                                portionsToDelete = parsed.coerceIn(1, maxQty)
                            }
                        }
                    },
                    label = { Text("Or Type Portions to Remove") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("portion_custom_input"),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onDeletePortions(portionsToDelete)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("btn_confirm_delete_portions"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Selected Portions", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = onDeleteEntirely,
                    modifier = Modifier.fillMaxWidth().testTag("btn_confirm_delete_entirely"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Completely from Fridge", fontWeight = FontWeight.Bold)
                }
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().testTag("btn_cancel_delete_portions"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = null,
        shape = RoundedCornerShape(24.dp)
    )
}

