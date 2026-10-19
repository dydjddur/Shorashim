package com.shorashim.feature.familytree.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.PartialDate
import com.shorashim.core.model.Person
import com.shorashim.core.ui.component.PersonAvatar

@Composable
fun FamilyTreeNodeCard(
    node: TreeNode,
    canEdit: Boolean,
    isSelected: Boolean = false,
    onCardClick: (Person) -> Unit,
    modifier: Modifier = Modifier,
) {
    val person = node.person
    val isDeceased = person.lifeStatus == LifeStatus.DECEASED

    val bDate = PartialDate.fromIso(person.birthDate, person.birthDatePrecision)
    val dDate = PartialDate.fromIso(person.deathDate, person.deathDatePrecision)

    val datesText: String? = remember(person) {
        val bYear = bDate.year?.toString()
        val dYear = dDate.year?.toString()
        when {
            bYear != null && dYear != null -> "$bYear – $dYear"
            bYear != null -> "נולד/ה ב-$bYear"
            dYear != null -> "נפטר/ה ב-$dYear"
            isDeceased -> "ז״ל 🕯️"
            else -> null
        }
    }

    val scheme = MaterialTheme.colorScheme
    val extra = com.shorashim.core.ui.theme.ShorashimTheme.colors
    val borderColor = when {
        isSelected -> scheme.primary
        isDeceased -> extra.gold.copy(alpha = 0.7f)
        else -> scheme.outlineVariant
    }

    val borderWidth = if (isSelected) 2.dp else 1.dp

    Surface(
        onClick = { onCardClick(person) },
        modifier = modifier
            .width(node.width.dp)
            .height(node.height.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (isDeceased) extra.goldContainer.copy(alpha = 0.55f) else scheme.surface,
        shadowElevation = if (isSelected) 10.dp else 2.dp,
        border = BorderStroke(width = borderWidth, color = borderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                // Avatar
                Box(contentAlignment = Alignment.BottomEnd) {
                    PersonAvatar(
                        photoPath = person.photoPath,
                        firstName = person.firstName,
                        isDeceased = isDeceased,
                        size = 52.dp,
                        shape = CircleShape,
                    )
                    if (person.isLinkedToUser) {
                        Surface(
                            shape = CircleShape,
                            color = scheme.primary,
                            modifier = Modifier.size(18.dp),
                            border = BorderStroke(1.5.dp, scheme.surface),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = "מקושר לחשבון",
                                    tint = scheme.onPrimary,
                                    modifier = Modifier.size(11.dp),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Name & Dates
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = person.fullName,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    if (datesText != null) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = datesText,
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
