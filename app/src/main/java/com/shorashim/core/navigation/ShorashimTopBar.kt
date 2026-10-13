package com.shorashim.core.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.core.ui.component.PersonAvatar

@Composable
fun ShorashimTopBar(
    currentRoute: String?,
    userAvatarUrl: String? = null,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 1. Right: חברים (Friends)
            TopBarCard(
                title = "חברים",
                icon = Icons.Outlined.People,
                isSelected = currentRoute == ShorashimRoutes.FRIENDS,
                onClick = { onNavigate(ShorashimRoutes.FRIENDS) },
            )

            // 2. עזרה (Help)
            TopBarCard(
                title = "עזרה",
                icon = Icons.AutoMirrored.Outlined.HelpOutline,
                isSelected = currentRoute == ShorashimRoutes.HELP,
                onClick = { onNavigate(ShorashimRoutes.HELP) },
            )

            // 3. תרומות (Donations)
            TopBarCard(
                title = "תרומות",
                icon = Icons.Outlined.VolunteerActivism,
                isSelected = currentRoute == ShorashimRoutes.DONATIONS,
                onClick = { onNavigate(ShorashimRoutes.DONATIONS) },
            )

            // 4. Left: פרופיל (Profile) with user image/icon
            TopBarCard(
                title = "פרופיל",
                icon = Icons.Outlined.Person,
                avatarUrl = userAvatarUrl,
                isSelected = currentRoute == ShorashimRoutes.PROFILE,
                onClick = { onNavigate(ShorashimRoutes.PROFILE) },
            )
        }
    }
}

@Composable
private fun RowScope.TopBarCard(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    avatarUrl: String? = null,
) {
    val activeColor = Color(0xFF22C55E) // Bright vibrant green matching reference design
    val inactiveBg = Color(0xFF1E2623) // Sleek dark card background
    val borderColor = if (isSelected) activeColor else Color(0xFF2E3832)

    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .height(58.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) activeColor else inactiveBg,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isSelected) 4.dp else 0.dp,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                PersonAvatar(
                    photoPath = avatarUrl,
                    firstName = title,
                    modifier = Modifier.size(24.dp),
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(22.dp),
                    tint = if (isSelected) Color(0xFF0F172A) else Color.White,
                )
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                ),
                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}
