package com.shorashim.feature.community

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.FilterAltOff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.model.PublicFamilyTree
import com.shorashim.core.ui.component.IconTile
import com.shorashim.core.ui.component.ScreenHeader
import com.shorashim.core.ui.component.ShorashimCard
import com.shorashim.core.ui.component.ShorashimEmptyState
import com.shorashim.core.ui.component.ShorashimErrorState
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.StatusPill
import com.shorashim.core.ui.component.Spacing
import com.shorashim.core.ui.theme.ShorashimTheme

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
    val focusManager = LocalFocusManager.current
    val hasActiveFilters = uiState.searchQuery.isNotBlank() ||
        uiState.selectedSurnameFilter != null ||
        uiState.onlyMySurnamesMatch

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
        topBar = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.padding(start = 4.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                }
                ScreenHeader(
                    title = "עצים קהילתיים",
                    subtitle = "איתור קרובים לפי שמות משפחה (צפייה בלבד)",
                    modifier = Modifier.weight(1f),
                ) {
                    IconButton(onClick = onRefresh, enabled = !uiState.isRefreshing && !uiState.isLoading) {
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
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                placeholder = { Text(text = "שם משפחה, שם עץ או יוצר…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(imageVector = Icons.Outlined.Clear, contentDescription = "נקה")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )

            // Quick filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // The selected surname may drop out of the "popular" list after a refresh; this chip keeps the
                // active filter visible and removable instead of silently hiding results.
                if (hasActiveFilters) {
                    FilterChip(
                        selected = false,
                        onClick = onClearFilters,
                        label = { Text("נקה הכל") },
                        leadingIcon = {
                            Icon(Icons.Outlined.FilterAltOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
                if (uiState.mySurnames.isNotEmpty()) {
                    FilterChip(
                        selected = uiState.onlyMySurnamesMatch,
                        onClick = onToggleOnlyMySurnames,
                        label = { Text("התאמה לשמות שלי (${uiState.mySurnames.size})") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (uiState.onlyMySurnamesMatch) Icons.Outlined.Check else Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = chipColors(),
                    )
                }
                val chipSurnames = buildList {
                    uiState.selectedSurnameFilter?.let { if (it !in uiState.popularSurnames) add(it) }
                    addAll(uiState.popularSurnames)
                }
                chipSurnames.forEach { surname ->
                    val isSelected = uiState.selectedSurnameFilter == surname
                    val isMySurname = uiState.mySurnames.any { it.equals(surname, ignoreCase = true) }
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectSurnameFilter(surname) },
                        label = { Text(surname) },
                        leadingIcon = if (isMySurname) {
                            {
                                Icon(
                                    Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = ShorashimTheme.colors.gold,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = chipColors(),
                    )
                }
            }

            when {
                uiState.isLoading -> ShorashimLoadingState(modifier = Modifier.fillMaxSize())
                // A failed manual refresh must not wipe out trees that are already on screen.
                uiState.error != null && uiState.trees.isEmpty() -> ShorashimErrorState(
                    message = uiState.error,
                    onRetry = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                )
                uiState.trees.isEmpty() -> ShorashimEmptyState(
                    title = "טרם פורסמו עצים קהילתיים",
                    message = "כשמשתמשים יפרסמו את עצי המשפחה שלהם, הם יופיעו כאן וניתן יהיה לאתר קרובים לפי שמות משפחה משותפים.",
                    icon = Icons.Outlined.Public,
                    action = {
                        Button(onClick = onNavigateBack ?: onRefresh, shape = RoundedCornerShape(14.dp)) {
                            Text(if (onNavigateBack != null) "חזרה לעצים שלי" else "רענון")
                        }
                    },
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = Spacing.xl),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (uiState.error != null) {
                        item(key = "error") {
                            ShorashimCard(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                onClick = onRefresh,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.CloudOff,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = uiState.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text(
                                        text = stringResource(R.string.retry),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.filteredTrees.isEmpty()) {
                        item(key = "no-results") {
                            ShorashimEmptyState(
                                title = "לא נמצאו עצים מתאימים",
                                message = "נסו לחפש שם משפחה אחר או לנקות את הסינונים.",
                                icon = Icons.Outlined.SearchOff,
                                modifier = Modifier.padding(top = Spacing.xl),
                                action = {
                                    Button(onClick = onClearFilters, shape = RoundedCornerShape(14.dp)) {
                                        Text("נקה סינונים")
                                    }
                                },
                            )
                        }
                    } else {
                        item(key = "count") {
                            Text(
                                text = "${uiState.filteredTrees.size} עצים",
                                style = MaterialTheme.typography.labelLarge,
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

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunityTreeCard(
    tree: PublicFamilyTree,
    mySurnames: List<String>,
    searchQuery: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ShorashimTheme.colors
    val mySurnamesLower = remember(mySurnames) { mySurnames.map { it.trim().lowercase() }.toSet() }
    val matchingSurnames = remember(tree.surnames, mySurnamesLower) {
        tree.surnames.filter { it.trim().lowercase() in mySurnamesLower }
    }
    val hasMatches = matchingSurnames.isNotEmpty()

    ShorashimCard(
        modifier = modifier,
        onClick = onClick,
        borderColor = if (hasMatches) colors.gold.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outlineVariant,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(
                icon = if (hasMatches) Icons.Outlined.Star else Icons.Outlined.Park,
                tint = if (hasMatches) colors.gold else MaterialTheme.colorScheme.primary,
                container = if (hasMatches) colors.goldContainer else MaterialTheme.colorScheme.primaryContainer,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tree.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "פורסם ע״י ${tree.ownerName ?: "משתמש באפליקציה"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusPill(
                text = "${tree.personCount}",
                icon = Icons.Outlined.Groups,
                container = MaterialTheme.colorScheme.surfaceVariant,
                content = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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

        if (hasMatches) {
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.successContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = colors.success,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "שמות תואמים לשלך: ${matchingSurnames.joinToString(", ")}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.success,
                    )
                }
            }
        }

        if (tree.surnames.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
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
                            isMatched -> colors.goldContainer
                            isSearched -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = if (isSearched && !isMatched) {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        } else {
                            null
                        },
                    ) {
                        Text(
                            text = surname,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isMatched || isSearched) FontWeight.Bold else FontWeight.Medium,
                            ),
                            color = when {
                                isMatched -> colors.onGoldContainer
                                isSearched -> MaterialTheme.colorScheme.onPrimaryContainer
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "צפייה במפת השושלת",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            // In RTL the "forward" arrow points left, i.e. the direction of reading.
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
