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
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.R
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.ui.component.PersonAvatar
import com.shorashim.core.model.PartialDate
import com.shorashim.core.model.Person

@Composable
fun FamilyTreeNodeCard(
    node: TreeNode,
    canEdit: Boolean,
    onAddChildClick: (Person) -> Unit,
    onAddParentClick: (Person) -> Unit,
    onAddSpouseClick: (Person) -> Unit,
    onAddSiblingClick: (Person) -> Unit = {},
    onEditPersonClick: (Person) -> Unit,
    onDeletePersonClick: (Person) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
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

    Card(
        modifier = modifier
            .width(node.width.dp)
            .height(node.height.dp)
            .clickable { menuExpanded = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDeceased) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isDeceased) {
                Color(0xFFD4AF37) // Gold/candle accent for memory
            } else {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            },
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
                PersonAvatar(
                    photoPath = person.photoPath,
                    firstName = person.firstName,
                    isDeceased = isDeceased,
                    size = 44.dp,
                    shape = CircleShape,
                )

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

                // Quick menu icon
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "פעולות",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Dropdown Menu for Node Actions
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                if (canEdit) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tree_add_child)) },
                        onClick = {
                            menuExpanded = false
                            onAddChildClick(person)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tree_add_parent)) },
                        onClick = {
                            menuExpanded = false
                            onAddParentClick(person)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tree_add_spouse)) },
                        onClick = {
                            menuExpanded = false
                            onAddSpouseClick(person)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.tree_add_sibling)) },
                        onClick = {
                            menuExpanded = false
                            onAddSiblingClick(person)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tree_quick_edit)) },
                    onClick = {
                        menuExpanded = false
                        onEditPersonClick(person)
                    },
                )
                if (canEdit) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.person_delete_button),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeletePersonClick(person)
                        },
                    )
                }
            }
        }
    }
}
