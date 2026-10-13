package com.shorashim.feature.community

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.model.PublicFamilyTree
import com.shorashim.core.ui.component.ShorashimErrorState
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.Spacing

@Composable
fun CommunityTreesRoute(
    viewModel: CommunityTreesViewModel,
    onNavigateToTree: (familyId: String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.loadData(isSilent = true)
    }

    CommunityTreesScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onRefresh = { viewModel.loadData(isSilent = false) },
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onSelectSurnameFilter = viewModel::onSelectSurnameFilter,
        onToggleOnlyMySurnames = viewModel::toggleOnlyMySurnamesMatch,
        onClearFilters = viewModel::clearFilters,
        onTreeClick = onNavigateToTree,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityTreesScreen(
    uiState: CommunityTreesUiState,
    onRefresh: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectSurnameFilter: (String?) -> Unit,
    onToggleOnlyMySurnames: () -> Unit,
    onClearFilters: () -> Unit,
    onTreeClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
) {
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (onNavigateBack != null) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "עצי משפחה קהילתיים",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "איתור קרובים לפי שמות משפחה בענן (צפייה בלבד)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.cancel),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                            if (uiState.isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.tree_refresh_action),
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "עצי משפחה קהילתיים",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "איתור קרובים לפי שמות משפחה בענן (צפייה בלבד)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = stringResource(R.string.tree_refresh_action),
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Search Input Field
            Box(modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs)) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    placeholder = {
                        Text(text = "חיפוש לפי שם משפחה, שם עץ או יוצר...")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotBlank()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Outlined.Clear,
                                    contentDescription = "נקה",
                                )
                            }
                        }
                    },
                    singleLine = true,
                )
            }

            // Quick Filter Chips Row (Horizontal Scroll)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // "My Surnames" filter chip if user has family surnames
                if (uiState.mySurnames.isNotEmpty()) {
                    FilterChip(
                        selected = uiState.onlyMySurnamesMatch,
                        onClick = onToggleOnlyMySurnames,
                        label = {
                            Text(
                                text = "🎯 התאמה לשמות שלי (${uiState.mySurnames.size})",
                                fontWeight = if (uiState.onlyMySurnamesMatch) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        leadingIcon = if (uiState.onlyMySurnamesMatch) {
                            {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = uiState.onlyMySurnamesMatch,
                            borderColor = if (uiState.onlyMySurnamesMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        ),
                    )
                }

                // Popular surname chips
                uiState.popularSurnames.forEach { surname ->
                    val isSelected = uiState.selectedSurnameFilter == surname
                    val isMySurname = uiState.mySurnames.any { it.equals(surname, ignoreCase = true) }

                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSurnameFilter(surname) },
                        label = {
                            Text(
                                text = if (isMySurname) "$surname ⭐" else surname,
                                fontWeight = if (isSelected || isMySurname) FontWeight.Bold else FontWeight.Normal,
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            // Main Content Area
            when {
                uiState.isLoading -> {
                    ShorashimLoadingState(modifier = Modifier.fillMaxSize())
                }
                uiState.error != null -> {
                    ShorashimErrorState(
                        message = uiState.error,
                        onRetry = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                uiState.trees.isEmpty() -> {
                    // No public trees in the whole database
                    EmptyCommunityTreesView(
                        title = "טרם הועלו עצי משפחה קהילתיים",
                        message = "כשמשתמשים יפרסמו את עצי המשפחה שלהם לענן, הם יופיעו כאן וניתן יהיה לאתר קרובים לפי שמות משפחה משותפים.",
                        buttonText = if (onNavigateBack != null) "חזרה לעצים שלי" else "רענון",
                        onButtonClick = onNavigateBack ?: onRefresh,
                    )
                }
                uiState.filteredTrees.isEmpty() -> {
                    // Filters returned no results
                    EmptyCommunityTreesView(
                        title = "לא נמצאו עצי משפחה מתאימים",
                        message = "לא נמצאו עצים התואמים לחיפוש או לסינון הנוכחי. נסו לחפש שם משפחה אחר או לנקות את הסינונים.",
                        buttonText = "נקה סינונים",
                        onButtonClick = onClearFilters,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        item {
                            Text(
                                text = "נמצאו ${uiState.filteredTrees.size} עצי משפחה בענן:",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        items(uiState.filteredTrees, key = { it.id }) { tree ->
                            CommunityTreeCard(
                                tree = tree,
                                mySurnames = uiState.mySurnames,
                                searchQuery = uiState.searchQuery,
                                onClick = { onTreeClick(tree.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunityTreeCard(
    tree: PublicFamilyTree,
    mySurnames: List<String>,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mySurnamesLower = remember(mySurnames) { mySurnames.map { it.trim().lowercase() }.toSet() }
    val matchingSurnames = remember(tree.surnames, mySurnamesLower) {
        tree.surnames.filter { it.trim().lowercase() in mySurnamesLower }
    }
    val hasMatches = matchingSurnames.isNotEmpty()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(
            1.5.dp,
            if (hasMatches) Color(0xFFD4AF37).copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            // Header: Icon + Name + Cloud Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (hasMatches) Color(0xFFD4AF37).copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (hasMatches) Icons.Outlined.Star else Icons.Outlined.Park,
                            contentDescription = null,
                            tint = if (hasMatches) Color(0xFFD4AF37) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = tree.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "פורסם ע״י ${tree.ownerName ?: "משתמש באפליקציה"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Person count badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "${tree.personCount} אנשים",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Description (if available)
            if (!tree.description.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = tree.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Matching Banner if any surnames match the current user's family
            if (hasMatches) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF4CAF50).copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "✨",
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "שמות משפחה תואמים לשלך: ${matchingSurnames.joinToString(", ")}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32),
                            ),
                        )
                    }
                }
            }

            // Surnames in this tree
            if (tree.surnames.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "שמות משפחה בעץ זה:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    tree.surnames.forEach { surname ->
                        val isMatched = surname.trim().lowercase() in mySurnamesLower
                        val isSearched = searchQuery.isNotBlank() && surname.contains(searchQuery.trim(), ignoreCase = true)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when {
                                isMatched -> Color(0xFFD4AF37).copy(alpha = 0.2f)
                                isSearched -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                            },
                            border = when {
                                isMatched -> BorderStroke(1.dp, Color(0xFFD4AF37))
                                isSearched -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                                else -> null
                            },
                        ) {
                            Text(
                                text = if (isMatched) "$surname ✨" else surname,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isMatched || isSearched) FontWeight.Bold else FontWeight.Normal,
                                ),
                                color = when {
                                    isMatched -> Color(0xFFB8860B)
                                    isSearched -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))

            // Action: View Tree Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "צפייה במפת השושלת",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    ),
                )

                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountTree,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "פתח עץ",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyCommunityTreesView(
    title: String,
    message: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp),
                )
            }

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(Spacing.xs))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )

            Spacer(Modifier.height(Spacing.xl))

            Button(
                onClick = onButtonClick,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(48.dp),
            ) {
                Text(
                    text = buttonText,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
