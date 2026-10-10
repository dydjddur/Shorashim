package com.shorashim.feature.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.shorashim.R
import com.shorashim.core.model.CalendarEvent
import com.shorashim.core.model.Family
import com.shorashim.core.model.Person
import com.shorashim.core.ui.component.Spacing

@Composable
fun AddEditMemorialEventDialog(
    families: List<Family>,
    deceasedPeople: List<Person>,
    editingMemorial: CalendarEvent.Memorial?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (familyId: String, personId: String?, personName: String, eventDate: String, hebrewDate: String?, description: String?) -> Unit,
) {
    var selectedFamilyId by remember {
        mutableStateOf(editingMemorial?.familyId ?: families.firstOrNull()?.id.orEmpty())
    }
    var selectedPersonId by remember { mutableStateOf(editingMemorial?.personId) }
    var personName by remember { mutableStateOf(editingMemorial?.personName.orEmpty()) }
    var eventDate by remember { mutableStateOf(editingMemorial?.originalDateStr.orEmpty()) }
    var hebrewDate by remember { mutableStateOf(editingMemorial?.hebrewDate.orEmpty()) }
    var description by remember { mutableStateOf(editingMemorial?.description.orEmpty()) }

    var familyDropdownExpanded by remember { mutableStateOf(false) }
    var personDropdownExpanded by remember { mutableStateOf(false) }

    var nameError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (editingMemorial != null) {
                    stringResource(R.string.events_edit_memorial_title)
                } else {
                    stringResource(R.string.events_create_memorial_title)
                },
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // 1. Select Family
                if (families.isNotEmpty()) {
                    val currentFamilyName = families.find { it.id == selectedFamilyId }?.name ?: "בחר עץ"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentFamilyName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.events_select_family_label)) },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { familyDropdownExpanded = true },
                            enabled = false,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { familyDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = familyDropdownExpanded,
                            onDismissRequest = { familyDropdownExpanded = false },
                        ) {
                            families.forEach { fam ->
                                DropdownMenuItem(
                                    text = { Text(fam.name) },
                                    onClick = {
                                        selectedFamilyId = fam.id
                                        familyDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                // 2. Associate Person (Optional)
                val relevantDeceased = deceasedPeople.filter { it.familyId == selectedFamilyId }
                if (relevantDeceased.isNotEmpty()) {
                    val selectedPerson = relevantDeceased.find { it.id == selectedPersonId }
                    val currentPersonLabel = selectedPerson?.fullName ?: "ללא שיוך לקרוב ספציפי"
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = currentPersonLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.events_associate_person_label)) },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = false,
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { personDropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = personDropdownExpanded,
                            onDismissRequest = { personDropdownExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("ללא שיוך (הזנה ידנית)") },
                                onClick = {
                                    selectedPersonId = null
                                    personDropdownExpanded = false
                                },
                            )
                            relevantDeceased.forEach { person ->
                                DropdownMenuItem(
                                    text = { Text(person.fullName) },
                                    onClick = {
                                        selectedPersonId = person.id
                                        personName = person.fullName
                                        if (!person.deathDate.isNullOrBlank()) {
                                            eventDate = person.deathDate
                                        }
                                        personDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }

                // 3. Person Name
                OutlinedTextField(
                    value = personName,
                    onValueChange = {
                        personName = it
                        nameError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.events_person_name_label)) },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 4. Date (YYYY-MM-DD)
                OutlinedTextField(
                    value = eventDate,
                    onValueChange = {
                        eventDate = it
                        dateError = it.isBlank()
                    },
                    label = { Text(stringResource(R.string.events_date_label)) },
                    placeholder = { Text("לדוגמה: 2024-05-15") },
                    isError = dateError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                // 5. Hebrew Date (Optional)
                OutlinedTextField(
                    value = hebrewDate,
                    onValueChange = { hebrewDate = it },
                    label = { Text(stringResource(R.string.events_hebrew_date_label)) },
                    placeholder = { Text("לדוגמה: י״ד באייר") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                // 6. Description / Notes
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.events_description_label)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val validName = personName.isNotBlank()
                    val validDate = eventDate.isNotBlank()
                    nameError = !validName
                    dateError = !validDate

                    if (validName && validDate && selectedFamilyId.isNotBlank()) {
                        onSave(
                            selectedFamilyId,
                            selectedPersonId,
                            personName,
                            eventDate,
                            hebrewDate.ifBlank { null },
                            description.ifBlank { null },
                        )
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.events_save_button))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
