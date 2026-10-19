package com.shorashim.feature.family

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import com.shorashim.core.ui.component.ShorashimDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shorashim.R
import com.shorashim.core.model.Person
import com.shorashim.core.model.RelationshipType
import com.shorashim.core.ui.component.Spacing

@Composable
fun AddRelationshipDialog(
    people: List<Person>,
    onDismiss: () -> Unit,
    onSubmit: (personAId: String, personBId: String, type: RelationshipType) -> Unit,
    errorMessage: String? = null,
) {
    var selectedPersonA by remember { mutableStateOf<Person?>(null) }
    var selectedPersonB by remember { mutableStateOf<Person?>(null) }
    var selectedType by remember { mutableStateOf(RelationshipType.PARENT_CHILD) }

    var expandedA by remember { mutableStateOf(false) }
    var expandedB by remember { mutableStateOf(false) }

    ShorashimDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.relationship_dialog_title),
        subtitle = "הגדרת קשר משפחתי בין שתי דמויות בעץ",
        heroIcon = Icons.Outlined.Share,
        content = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Relationship type selector
                Text(
                    text = stringResource(R.string.relationship_type_label),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = RelationshipType.PARENT_CHILD },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedType == RelationshipType.PARENT_CHILD,
                            onClick = { selectedType = RelationshipType.PARENT_CHILD },
                        )
                        Text(stringResource(R.string.relationship_type_parent_child))
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedType = RelationshipType.SPOUSE },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selectedType == RelationshipType.SPOUSE,
                            onClick = { selectedType = RelationshipType.SPOUSE },
                        )
                        Text(stringResource(R.string.relationship_type_spouse))
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                // Person A Picker
                val labelA = if (selectedType == RelationshipType.PARENT_CHILD) {
                    stringResource(R.string.relationship_parent_label)
                } else {
                    stringResource(R.string.relationship_spouse_1_label)
                }

                Text(
                    text = labelA,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )

                Box {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedA = true },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = selectedPersonA?.fullName
                                    ?: stringResource(R.string.relationship_select_person_placeholder),
                                color = if (selectedPersonA != null) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedA,
                        onDismissRequest = { expandedA = false },
                    ) {
                        people.forEach { person ->
                            DropdownMenuItem(
                                text = { Text(person.fullName) },
                                onClick = {
                                    selectedPersonA = person
                                    expandedA = false
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                // Person B Picker
                val labelB = if (selectedType == RelationshipType.PARENT_CHILD) {
                    stringResource(R.string.relationship_child_label)
                } else {
                    stringResource(R.string.relationship_spouse_2_label)
                }

                Text(
                    text = labelB,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )

                Box {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedB = true },
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.md),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = selectedPersonB?.fullName
                                    ?: stringResource(R.string.relationship_select_person_placeholder),
                                color = if (selectedPersonB != null) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = expandedB,
                        onDismissRequest = { expandedB = false },
                    ) {
                        people.forEach { person ->
                            DropdownMenuItem(
                                text = { Text(person.fullName) },
                                onClick = {
                                    selectedPersonB = person
                                    expandedB = false
                                },
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = Spacing.xs),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pA = selectedPersonA?.id
                    val pB = selectedPersonB?.id
                    if (pA != null && pB != null) {
                        onSubmit(pA, pB, selectedType)
                    }
                },
                enabled = selectedPersonA != null && selectedPersonB != null,
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.relationship_submit_button))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
