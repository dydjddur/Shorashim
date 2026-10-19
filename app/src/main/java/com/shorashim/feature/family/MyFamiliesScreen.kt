package com.shorashim.feature.family

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.model.Family
import com.shorashim.core.model.MembershipRole
import com.shorashim.core.ui.component.IconTile
import com.shorashim.core.ui.component.ShorashimCard
import com.shorashim.core.ui.component.ShorashimEmptyState
import com.shorashim.core.ui.component.ShorashimErrorState
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.Spacing
import com.shorashim.core.ui.component.StatusPill
import com.shorashim.core.ui.theme.ShorashimTheme

@Composable
fun MyFamiliesRoute(
    viewModel: MyFamiliesViewModel,
    onNavigateToCreateFamily: () -> Unit,
    onNavigateToFamilyManagement: (String) -> Unit,
    onNavigateToInvitations: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.loadFamilies(isSilent = true)
    }

    MyFamiliesScreen(
        uiState = uiState,
        onRefresh = viewModel::loadFamilies,
        onCreateFamilyClick = onNavigateToCreateFamily,
        onFamilyClick = onNavigateToFamilyManagement,
        onNavigateToInvitations = onNavigateToInvitations,
        modifier = modifier,
    )
}

@Composable
fun MyFamiliesScreen(
    uiState: MyFamiliesUiState,
    onRefresh: () -> Unit,
    onCreateFamilyClick: () -> Unit,
    onFamilyClick: (String) -> Unit,
    onNavigateToInvitations: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (uiState is MyFamiliesUiState.Success) {
                ExtendedFloatingActionButton(
                    onClick = onCreateFamilyClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(18.dp),
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.families_create_button), fontWeight = FontWeight.Bold) },
                )
            }
        },
    ) { innerPadding ->
        when (uiState) {
            MyFamiliesUiState.Loading -> {
                ShorashimLoadingState(modifier = Modifier.padding(innerPadding))
            }
            is MyFamiliesUiState.Error -> {
                ShorashimErrorState(
                    message = uiState.message ?: stringResource(R.string.error_generic),
                    onRetry = onRefresh,
                    modifier = Modifier.padding(innerPadding),
                )
            }
            MyFamiliesUiState.Empty -> {
                ShorashimEmptyState(
                    title = stringResource(R.string.families_empty_title),
                    message = stringResource(R.string.families_empty_message),
                    icon = Icons.Outlined.Park,
                    modifier = Modifier.padding(innerPadding),
                    action = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = onCreateFamilyClick,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.height(52.dp),
                                contentPadding = PaddingValues(horizontal = 28.dp),
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.families_create_first_button),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                            Spacer(Modifier.height(Spacing.sm))
                            TextButton(onClick = onNavigateToInvitations) {
                                Icon(Icons.Outlined.MailOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(text = "קיבלתם הזמנה לעץ? ${stringResource(R.string.invitations_see_all)}")
                            }
                        }
                    },
                )
            }
            is MyFamiliesUiState.Success -> {
                val totalPeople = uiState.families.sumOf { it.personCount }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    // Bottom padding keeps the last card clear of the extended FAB.
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp)) {
                            Text(
                                text = stringResource(R.string.families_title),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "${uiState.families.size} עצים · $totalPeople אנשים",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    item {
                        InvitationsBannerButton(onClick = onNavigateToInvitations)
                    }

                    items(uiState.families, key = { it.id }) { family ->
                        FamilyCard(
                            family = family,
                            onClick = { onFamilyClick(family.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvitationsBannerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ShorashimCard(
        onClick = onClick,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = Icons.Outlined.MailOutline,
                container = MaterialTheme.colorScheme.surface,
                size = 40.dp,
                cornerRadius = 12.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "הזמנות לעצי משפחה",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    text = "הצטרפות לעצים שקרובים שיתפו איתכם",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun FamilyCard(
    family: Family,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = ShorashimTheme.colors
    val roleLabel = when (family.currentUserRole) {
        MembershipRole.OWNER -> "מנהל-על"
        MembershipRole.ADMIN -> "מנהל"
        MembershipRole.CONTRIBUTOR -> "משתתף"
        MembershipRole.VIEWER -> "צופה"
        else -> ""
    }
    val membersLine = stringResource(R.string.family_members_count, family.personCount) +
        if (roleLabel.isNotEmpty()) " · $roleLabel" else ""

    ShorashimCard(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                icon = Icons.Outlined.Park,
                size = 56.dp,
                iconSize = 30.dp,
                cornerRadius = 18.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = family.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = membersLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                if (family.isPrivate) {
                    StatusPill(
                        text = "פרטי",
                        icon = Icons.Outlined.Lock,
                        container = extra.warningContainer,
                        content = extra.warning,
                    )
                } else {
                    StatusPill(
                        text = "קהילתי",
                        icon = Icons.Outlined.Public,
                        container = extra.successContainer,
                        content = extra.success,
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
