package com.shorashim.feature.family

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import com.shorashim.core.ui.component.ShorashimConfirmDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.shorashim.core.common.HebrewDates
import com.shorashim.core.model.HebrewCalendarMath
import com.shorashim.feature.family.CalendarInputType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.ui.component.ImageUtils
import com.shorashim.core.ui.component.PersonAvatar
import com.shorashim.core.ui.component.Spacing
import com.shorashim.feature.auth.AuthCard
import com.shorashim.feature.auth.AuthPrimaryButton
import com.shorashim.feature.auth.AuthTextField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        onPhotoChanged = viewModel::onPhotoChanged,
        onLifeStatusChanged = viewModel::onLifeStatusChanged,
        onBirthDateChosen = viewModel::onBirthDateChosen,
        onBirthPrecisionChanged = viewModel::onBirthPrecisionChanged,
        onBirthCalendarTypeChanged = viewModel::onBirthCalendarTypeChanged,
        onDeathDateChosen = viewModel::onDeathDateChosen,
        onDeathPrecisionChanged = viewModel::onDeathPrecisionChanged,
        onDeathCalendarTypeChanged = viewModel::onDeathCalendarTypeChanged,
        onBiographyChanged = viewModel::onBiographyChanged,
        onSaveClicked = { viewModel.save(onNavigateBack) },
        onDeleteClicked = { viewModel.setDeleteConfirmOpen(true) },
        onDeleteConfirmDismiss = { viewModel.setDeleteConfirmOpen(false) },
        onDeleteConfirmed = { viewModel.delete(onNavigateBack) },
        onCoParentSelected = viewModel::onCoParentSelected,
        onLinkToExistingParentChanged = viewModel::onLinkToExistingParentChanged,
        linking = PersonLinkingActions(
            onLinkUser = { viewModel.linkUser(it) },
            onUnlinkUser = { viewModel.linkUser(null) },
            onSearchUser = viewModel::searchUser,
            onGetFriends = viewModel::getFriends,
            onGetMutualConnections = viewModel::getMutualConnections,
        ),
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
    onPhotoChanged: (String?) -> Unit = {},
    onLifeStatusChanged: (LifeStatus) -> Unit,
    onBirthDateChosen: (Int, Int?, Int?) -> Unit,
    onBirthPrecisionChanged: (DatePrecision) -> Unit,
    onBirthCalendarTypeChanged: (CalendarInputType) -> Unit = {},
    onDeathDateChosen: (Int, Int?, Int?) -> Unit,
    onDeathPrecisionChanged: (DatePrecision) -> Unit,
    onDeathCalendarTypeChanged: (CalendarInputType) -> Unit = {},
    onBiographyChanged: (String) -> Unit,
    onSaveClicked: () -> Unit,
    onDeleteClicked: () -> Unit,
    onDeleteConfirmDismiss: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    onCoParentSelected: (String?) -> Unit = {},
    onLinkToExistingParentChanged: (Boolean) -> Unit = {},
    linking: PersonLinkingActions? = null,
) {
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isCompressingPhoto by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            isCompressingPhoto = true
            coroutineScope.launch(Dispatchers.IO) {
                val base64 = ImageUtils.uriToCompressedBase64(context, uri)
                withContext(Dispatchers.Main) {
                    isCompressingPhoto = false
                    if (base64 != null) {
                        onPhotoChanged(base64)
                    }
                }
            }
        }
    }

    if (uiState.isDeleteConfirmOpen) {
        ShorashimConfirmDialog(
            onDismissRequest = onDeleteConfirmDismiss,
            onConfirm = onDeleteConfirmed,
            title = stringResource(R.string.person_delete_dialog_title),
            message = stringResource(R.string.person_delete_dialog_message, uiState.firstName),
            confirmText = stringResource(R.string.person_delete_button),
            dismissText = stringResource(R.string.cancel),
            icon = Icons.Outlined.Delete,
            isDestructive = true,
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
                // Photo Picker Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clickable(enabled = !uiState.isLoading && !isCompressingPhoto) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!uiState.photoPath.isNullOrBlank()) {
                            PersonAvatar(
                                photoPath = uiState.photoPath,
                                firstName = uiState.firstName.ifBlank { "אדם" },
                                isDeceased = uiState.lifeStatus == LifeStatus.DECEASED,
                                size = 110.dp,
                                shape = CircleShape,
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(34.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoCamera,
                                    contentDescription = stringResource(R.string.person_change_photo),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                modifier = Modifier.size(110.dp),
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AddAPhoto,
                                        contentDescription = stringResource(R.string.person_add_photo),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp),
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.person_add_photo),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }

                        if (isCompressingPhoto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(38.dp),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Spacer(Modifier.height(Spacing.xs))

                    Text(
                        text = stringResource(R.string.person_photo_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = Spacing.md),
                    )

                    Spacer(Modifier.height(Spacing.xs))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            enabled = !uiState.isLoading && !isCompressingPhoto,
                        ) {
                            Icon(
                                imageVector = if (uiState.photoPath.isNullOrBlank()) Icons.Outlined.AddAPhoto else Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (uiState.photoPath.isNullOrBlank()) {
                                    stringResource(R.string.person_add_photo)
                                } else {
                                    stringResource(R.string.person_change_photo)
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            )
                        }

                        if (!uiState.photoPath.isNullOrBlank()) {
                            TextButton(
                                onClick = { onPhotoChanged(null) },
                                enabled = !uiState.isLoading && !isCompressingPhoto,
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.person_remove_photo),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

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
                    calendarType = uiState.birthCalendarType,
                    onCalendarTypeChange = onBirthCalendarTypeChanged,
                    year = uiState.birthYear,
                    month = uiState.birthMonth,
                    day = uiState.birthDay,
                    onDateChosen = onBirthDateChosen,
                )

                // Death Date Section (if deceased)
                if (uiState.lifeStatus == LifeStatus.DECEASED) {
                    Spacer(Modifier.height(Spacing.md))
                    DateInputSection(
                        sectionTitle = stringResource(R.string.person_death_date_label),
                        precision = uiState.deathPrecision,
                        onPrecisionChange = onDeathPrecisionChanged,
                        calendarType = uiState.deathCalendarType,
                        onCalendarTypeChange = onDeathCalendarTypeChanged,
                        year = uiState.deathYear,
                        month = uiState.deathMonth,
                        day = uiState.deathDay,
                        onDateChosen = onDeathDateChosen,
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

                val person = uiState.loadedPerson
                if (uiState.isEditing && person != null && linking != null) {
                    Spacer(Modifier.height(Spacing.md))
                    PersonLinkSection(person = person, actions = linking)
                }

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

/** Extra choices shown when the person is added as a relative of someone on the tree. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
