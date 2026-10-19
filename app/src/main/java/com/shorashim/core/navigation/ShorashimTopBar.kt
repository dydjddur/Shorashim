package com.shorashim.core.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shorashim.R
import com.shorashim.core.ui.component.PersonAvatar

/**
 * App header: brand mark + the current section on the start side, compact round actions (friends, help,
 * donations, profile avatar) on the end side. Replaces the old row of four chunky hard-coded dark tiles,
 * which ignored the theme and pushed the content far down.
 */
@Composable
fun ShorashimTopBar(
    currentRoute: String?,
    userAvatarUrl: String? = null,
    userDisplayName: String = "",
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.background,
    ) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(start = 16.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BrandMark()
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "שורשים",
                        style = MaterialTheme.typography.headlineSmall,
                        color = scheme.onBackground,
                        maxLines = 1,
                    )
                    Text(
                        text = sectionTitle(currentRoute),
                        style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    HeaderAction(
                        icon = Icons.Outlined.People,
                        label = "חברים",
                        selected = currentRoute == ShorashimRoutes.FRIENDS,
                        onClick = { onNavigate(ShorashimRoutes.FRIENDS) },
                    )
                    HeaderAction(
                        icon = Icons.Outlined.VolunteerActivism,
                        label = "תרומות",
                        selected = currentRoute == ShorashimRoutes.DONATIONS,
                        onClick = { onNavigate(ShorashimRoutes.DONATIONS) },
                    )
                    HeaderAction(
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        label = "עזרה",
                        selected = currentRoute == ShorashimRoutes.HELP,
                        onClick = { onNavigate(ShorashimRoutes.HELP) },
                    )
                    Spacer(Modifier.width(4.dp))
                    ProfileAction(
                        avatarUrl = userAvatarUrl,
                        displayName = userDisplayName,
                        selected = currentRoute == ShorashimRoutes.PROFILE,
                        onClick = { onNavigate(ShorashimRoutes.PROFILE) },
                    )
                }
            }
            HorizontalDivider(color = scheme.outlineVariant.copy(alpha = 0.7f))
        }
    }
}

private fun sectionTitle(route: String?): String = when (route?.substringBefore('?')) {
    ShorashimRoutes.FAMILY_TREE, null -> "עצי המשפחה שלי"
    ShorashimRoutes.COMMUNITY_TREES -> "קהילה"
    ShorashimRoutes.MEMORIALS -> "אירועים ואזכרות"
    ShorashimRoutes.TEHILLIM -> "ספר תהילים"
    ShorashimRoutes.FRIENDS -> "חברים והודעות"
    ShorashimRoutes.HELP -> "עזרה ומדריך"
    ShorashimRoutes.DONATIONS -> "תמיכה בפרויקט"
    ShorashimRoutes.PROFILE -> "הפרופיל שלי"
    else -> ""
}

@Composable
private fun BrandMark() {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(
                Brush.linearGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_notification),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun HeaderAction(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val container by animateColorAsState(
        if (selected) scheme.primaryContainer else Color.Transparent,
        label = "headerActionContainer",
    )
    val tint by animateColorAsState(
        if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
        label = "headerActionTint",
    )
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = container,
        modifier = Modifier
            .size(42.dp)
            .semantics {
                this.selected = selected
                role = Role.Tab
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ProfileAction(
    avatarUrl: String?,
    displayName: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        border = BorderStroke(2.dp, if (selected) scheme.primary else scheme.outlineVariant),
        modifier = Modifier
            .size(42.dp)
            .semantics {
                this.selected = selected
                role = Role.Tab
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            PersonAvatar(
                photoPath = avatarUrl,
                firstName = displayName,
                size = 34.dp,
            )
        }
    }
}
