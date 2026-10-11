package com.shorashim.feature.family

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.ui.component.Spacing
import com.shorashim.feature.auth.AuthCard
import com.shorashim.feature.auth.AuthPrimaryButton
import com.shorashim.feature.auth.AuthTextField

@Composable
fun AddEditPersonRoute(
    viewModel: AddEditPersonViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AddEditPersonScreen(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onFirstNameChanged = viewModel::onFirstNameChanged,
        onLastNameChanged = viewModel::onLastNameChanged,
        onAdditionalNamesChanged = viewModel::onAdditionalNamesChanged,
        onLifeStatusChanged = viewModel::onLifeStatusChanged,
        onBirthYearChanged = viewModel::onBirthYearChanged,
        onBirthMonthChanged = viewModel::onBirthMonthChanged,
        onBirthDayChanged = viewModel::onBirthDayChanged,
        onBirthPrecisionChanged = viewModel::onBirthPrecisionChanged,
        onDeathYearChanged = viewModel::onDeathYearChanged,
        onDeathMonthChanged = viewModel::onDeathMonthChanged,
        onDeathDayChanged = viewModel::onDeathDayChanged,
        onDeathPrecisionChanged = viewModel::onDeathPrecisionChanged,
        onBiographyChanged = viewModel::onBiographyChanged,
        onSaveClicked = { viewModel.save(onNavigateBack) },
        onDeleteClicked = { viewModel.setDeleteConfirmOpen(true) },
        onDeleteConfirmDismiss = { viewModel.setDeleteConfirmOpen(false) },
        onDeleteConfirmed = { viewModel.delete(onNavigateBack) },
        onCoParentSelected = viewModel::onCoParentSelected,
        onLinkToExistingParentChanged = viewModel::onLinkToExistingParentChanged,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPersonScreen(
    uiState: AddEditPersonUiState,
    onNavigateBack: () -> Unit,
    onFirstNameChanged: (String) -> Unit,
    onLastNameChanged: (String) -> Unit,
    onAdditionalNamesChanged: (String) -> Unit,
    onLifeStatusChanged: (LifeStatus) -> Unit,
    onBirthYearChanged: (String) -> Unit,
    onBirthMonthChanged: (String) -> Unit,
    onBirthDayChanged: (String) -> Unit,
    onBirthPrecisionChanged: (DatePrecision) -> Unit,
    onDeathYearChanged: (String) -> Unit,
    onDeathMonthChanged: (String) -> Unit,
    onDeathDayChanged: (String) -> Unit,
    onDeathPrecisionChanged: (DatePrecision) -> Unit,
    onBiographyChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onDeleteConfirmDismiss: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    onCoParentSelected: (String?) -> Unit = {},
    onLinkToExistingParentChanged: (Boolean) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    if (uiState.isDeleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = onDeleteConfirmDismiss,
            title = {
                Text(
                    text = stringResource(R.string.person_delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(stringResource(R.string.person_delete_dialog_message, uiState.firstName))
            },
            confirmButton = {
                Button(
                    onClick = onDeleteConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.person_delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteConfirmDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            uiState.isEditing -> stringResource(R.string.person_edit_title)
                            uiState.relation == null || uiState.anchorName.isBlank() -> stringResource(R.string.person_add_title)
                            else -> stringResource(
                                when (uiState.relation) {
                                    com.shorashim.core.model.NewPersonRelation.PARENT -> R.string.person_add_parent_title
                                    com.shorashim.core.model.NewPersonRelation.CHILD -> R.string.person_add_child_title
                                    com.shorashim.core.model.NewPersonRelation.SPOUSE -> R.string.person_add_spouse_title
                                    else -> R.string.person_add_sibling_title
                                },
                                uiState.anchorName,
                            )
                        },
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.cancel),
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        IconButton(onClick = onDeleteClicked) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = stringResource(R.string.person_delete_button),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            RelationOptions(
                uiState = uiState,
                onCoParentSelected = onCoParentSelected,
                onLinkToExistingParentChanged = onLinkToExistingParentChanged,
            )

            AuthCard {
                // Names Section
                AuthTextField(
                    value = uiState.firstName,
                    onValueChange = onFirstNameChanged,
                    label = stringResource(R.string.person_first_name_label),
                    leadingIcon = Icons.Outlined.Person,
                    isError = uiState.firstNameError != null,
                    errorMessage = uiState.firstNameError?.let { stringResource(it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !uiState.isLoading,
                )

                Spacer(Modifier.height(Spacing.sm))

                AuthTextField(
                    value = uiState.lastName,
                    onValueChange = onLastNameChanged,
                    label = stringResource(R.string.person_last_name_label),
                    leadingIcon = Icons.Outlined.Person,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !uiState.isLoading,
                )

                Spacer(Modifier.height(Spacing.sm))

                AuthTextField(
                    value = uiState.additionalNames,
                    onValueChange = onAdditionalNamesChanged,
                    label = stringResource(R.string.person_additional_names_label),
                    leadingIcon = Icons.Outlined.Person,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    enabled = !uiState.isLoading,
                )

                Spacer(Modifier.height(Spacing.md))

                // Life Status Toggle
                Text(
                    text = stringResource(R.string.person_life_status_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.align(Alignment.Start),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLifeStatusChanged(LifeStatus.LIVING) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = uiState.lifeStatus == LifeStatus.LIVING,
                            onClick = { onLifeStatusChanged(LifeStatus.LIVING) },
                        )
                        Text(stringResource(R.string.person_status_living))
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onLifeStatusChanged(LifeStatus.DECEASED) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = uiState.lifeStatus == LifeStatus.DECEASED,
                            onClick = { onLifeStatusChanged(LifeStatus.DECEASED) },
                        )
                        Text(stringResource(R.string.person_status_deceased))
                    }
                }

                Spacer(Modifier.height(Spacing.md))

                // Birth Date Section
                DateInputSection(
                    sectionTitle = stringResource(R.string.person_birth_date_label),
                    precision = uiState.birthPrecision,
                    onPrecisionChange = onBirthPrecisionChanged,
                    year = uiState.birthYear,
                    onYearChange = onBirthYearChanged,
                    month = uiState.birthMonth,
                    onMonthChange = onBirthMonthChanged,
                    day = uiState.birthDay,
                    onDayChange = onBirthDayChanged,
                )

                // Death Date Section (if deceased)
                if (uiState.lifeStatus == LifeStatus.DECEASED) {
                    Spacer(Modifier.height(Spacing.md))
                    DateInputSection(
                        sectionTitle = stringResource(R.string.person_death_date_label),
                        precision = uiState.deathPrecision,
                        onPrecisionChange = onDeathPrecisionChanged,
                        year = uiState.deathYear,
                        onYearChange = onDeathYearChanged,
                        month = uiState.deathMonth,
                        onMonthChange = onDeathMonthChanged,
                        day = uiState.deathDay,
                        onDayChange = onDeathDayChanged,
                    )
                }

                if (uiState.dateError != null) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = stringResource(uiState.dateError),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(Spacing.md))

                // Biography Field
                OutlinedTextField(
                    value = uiState.biography,
                    onValueChange = onBiographyChanged,
                    label = { Text(stringResource(R.string.person_biography_label)) },
                    placeholder = { Text(stringResource(R.string.person_biography_hint)) },
                    leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = null) },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (uiState.generalError != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = uiState.generalError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Spacer(Modifier.height(Spacing.lg))

                // Submit Button
                AuthPrimaryButton(
                    text = stringResource(R.string.person_save_button),
                    onClick = {
                        focusManager.clearFocus()
                        onSaveClicked()
                    },
                    isLoading = uiState.isLoading,
                    enabled = !uiState.isLoading,
                )
            }
        }
    }
}

@Composable
fun DateInputSection(
    sectionTitle: String,
    precision: DatePrecision,
    onPrecisionChange: (DatePrecision) -> Unit,
    year: String,
    onYearChange: (String) -> Unit,
    month: String,
    onMonthChange: (String) -> Unit,
    day: String,
    onDayChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = sectionTitle,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        )

        Spacer(Modifier.height(Spacing.xs))

        // Precision selector chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            FilterChip(
                selected = precision == DatePrecision.FULL_DATE,
                onClick = { onPrecisionChange(DatePrecision.FULL_DATE) },
                label = { Text("מלא", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = precision == DatePrecision.YEAR_MONTH,
                onClick = { onPrecisionChange(DatePrecision.YEAR_MONTH) },
                label = { Text("חודש ושנה", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = precision == DatePrecision.YEAR_ONLY,
                onClick = { onPrecisionChange(DatePrecision.YEAR_ONLY) },
                label = { Text("שנה", style = MaterialTheme.typography.labelSmall) },
            )
            FilterChip(
                selected = precision == DatePrecision.UNKNOWN,
                onClick = { onPrecisionChange(DatePrecision.UNKNOWN) },
                label = { Text("לא ידוע", style = MaterialTheme.typography.labelSmall) },
            )
        }

        if (precision != DatePrecision.UNKNOWN) {
            Spacer(Modifier.height(Spacing.xs))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                // Day field (if FULL_DATE)
                if (precision == DatePrecision.FULL_DATE) {
                    OutlinedTextField(
                        value = day,
                        onValueChange = onDayChange,
                        label = { Text("יום") },
                        placeholder = { Text("15") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.8f),
                    )
                }

                // Month field (if YEAR_MONTH or FULL_DATE)
                if (precision == DatePrecision.YEAR_MONTH || precision == DatePrecision.FULL_DATE) {
                    OutlinedTextField(
                        value = month,
                        onValueChange = onMonthChange,
                        label = { Text("חודש") },
                        placeholder = { Text("05") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(0.8f),
                    )
                }

                // Year field
                OutlinedTextField(
                    value = year,
                    onValueChange = onYearChange,
                    label = { Text("שנה") },
                    placeholder = { Text("1980") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Extra choices shown when the person is added as a relative of someone on the tree. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelationOptions(
    uiState: AddEditPersonUiState,
    onCoParentSelected: (String?) -> Unit,
    onLinkToExistingParentChanged: (Boolean) -> Unit,
) {
    if (uiState.relation == null || uiState.isEditing) return

    if (uiState.siblingBlocked) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(
                text = stringResource(R.string.person_relation_sibling_needs_parent),
                modifier = Modifier.padding(Spacing.md),
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
        return
    }

    if (uiState.coParentOptions.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(Spacing.md), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = stringResource(R.string.person_relation_coparent_label),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                )
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    uiState.coParentOptions.forEach { spouse ->
                        FilterChip(
                            selected = uiState.selectedCoParentId == spouse.id,
                            onClick = { onCoParentSelected(spouse.id) },
                            label = { Text(spouse.fullName) },
                        )
                    }
                    FilterChip(
                        selected = uiState.selectedCoParentId == null,
                        onClick = { onCoParentSelected(null) },
                        label = { Text(stringResource(R.string.person_relation_coparent_none)) },
                    )
                }
            }
        }
    }

    uiState.existingParent?.let { parent ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLinkToExistingParentChanged(!uiState.linkToExistingParent) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Checkbox(
                checked = uiState.linkToExistingParent,
                onCheckedChange = onLinkToExistingParentChanged,
            )
            Text(
                text = stringResource(R.string.person_relation_link_parent, parent.fullName),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
