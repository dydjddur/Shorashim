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
    onDetailsClick: (Person) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val person = node.person
    val isDeceased = person.lifeStatus == LifeStatus.DECEASED

    val bDate = PartialDate.fromIso(person.birthDate, person.birthDatePrecision)
    val dDate = PartialDate.fromIso(person.deathDate, person.deathDatePrecision)

    val datesText = remember(person) {
        val bYear = bDate.year?.toString()
        val dYear = dDate.year?.toString()
        when {
            bYear != null && dYear != null -> "$bYear – $dYear"
            bYear != null -> "נולד/ה ב-$bYear"
            dYear != null -> "נפטר/ה ב-$dYear"
            isDeceased -> "ז״ל 🕯️"
            else -> "חי/ה"
        }
    }

    val borderColor = when {
        isSelected -> Color(0xFF22C55E) // Vivid green highlight when selected
        isDeceased -> Color(0xFFD4AF37) // Gold/candle accent for memory
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
    }

    val borderWidth = if (isSelected) 2.5.dp else 1.5.dp

    Card(
        modifier = modifier
            .width(node.width.dp)
            .height(node.height.dp)
            .clickable { onCardClick(person) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDeceased) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 10.dp else 6.dp),
        border = BorderStroke(
            width = borderWidth,
            color = borderColor,
        ),
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
                        size = 44.dp,
                        shape = CircleShape,
                    )
                    if (person.isLinkedToUser) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF22C55E),
                            modifier = Modifier.size(16.dp),
                            border = BorderStroke(1.dp, Color.White),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = "מקושר לחשבון",
                                    tint = Color.White,
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
                            fontSize = 13.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = datesText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Quick details and link indicator
                IconButton(
                    onClick = { onDetailsClick(person) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = if (canEdit) Icons.Outlined.MoreVert else Icons.Outlined.Info,
                        contentDescription = if (canEdit) "פרטים וקישור חשבון" else "פרטים",
                        tint = if (isSelected) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}
