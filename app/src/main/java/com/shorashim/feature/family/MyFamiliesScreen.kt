package com.shorashim.feature.family

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.model.Family
import com.shorashim.core.ui.component.ShorashimEmptyState
import com.shorashim.core.ui.component.ShorashimErrorState
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.Spacing

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
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (uiState is MyFamiliesUiState.Success) {
                FloatingActionButton(
                    onClick = onCreateFamilyClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.families_create_button),
                    )
                }
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Park,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(54.dp),
                            )
                        }

                        Spacer(Modifier.height(Spacing.md))

                        Text(
                            text = stringResource(R.string.families_empty_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                        )

                        Spacer(Modifier.height(Spacing.xs))

                        Text(
                            text = stringResource(R.string.families_empty_message),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = Spacing.md),
                        )

                        Spacer(Modifier.height(Spacing.xl))

                        Button(
                            onClick = onCreateFamilyClick,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(52.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.size(Spacing.xs))
                            Text(
                                text = stringResource(R.string.families_create_first_button),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                            )
                        }

                        // Show "pending invitations" link even with no own trees
                        Spacer(Modifier.height(Spacing.md))
                        TextButton(onClick = onNavigateToInvitations) {
                            Text(
                                text = "הזמנות לעצי משפחה (${stringResource(R.string.invitations_see_all)})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            is MyFamiliesUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    // Pending Invitations section (shown above "המשפחות שלי")
                    item {
                        InvitationsBannerButton(onClick = onNavigateToInvitations)
                    }

                    item {
                        Text(
                            text = stringResource(R.string.families_title),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = Spacing.sm),
                        )
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
    androidx.compose.material3.OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.6f),
        ),
        colors = androidx.compose.material3.CardDefaults.outlinedCardColors(
            containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50).copy(alpha = 0.06f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color(0xFF43A047),
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "הזמנות לעץ משפחתי",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Park,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp),
                )
            }

            Spacer(Modifier.size(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = family.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (family.isDraft) Color(0xFFF9A825) else Color(0xFF43A047),
                    ) {
                        Text(
                            text = if (family.isDraft) "טיוטה" else "פורסם",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = stringResource(R.string.family_members_count, family.personCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "• ${when (family.currentUserRole) {
                            com.shorashim.core.model.MembershipRole.OWNER -> "מנהל-על"
                            com.shorashim.core.model.MembershipRole.ADMIN -> "מנהל"
                            com.shorashim.core.model.MembershipRole.CONTRIBUTOR -> "משתתף"
                            com.shorashim.core.model.MembershipRole.VIEWER -> "צופה"
                            else -> ""
                        }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
