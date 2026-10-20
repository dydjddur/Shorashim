package com.shorashim.feature.events

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import com.shorashim.core.ui.component.ShorashimConfirmDialog
import com.shorashim.core.ui.component.ShorashimDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.model.CalendarEvent
import com.shorashim.core.ui.component.ShorashimEmptyState
import com.shorashim.core.ui.component.ShorashimErrorState
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.Spacing

@Composable
fun EventsRoute(
    viewModel: EventsViewModel,
    onNavigateToMemorialTehillim: (deceasedName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.loadEvents(isSilent = true)
        viewModel.loadFamiliesAndPeople()
    }

    EventsScreen(
        uiState = uiState,
        onRetry = { viewModel.loadEvents() },
        onSelectEvent = { viewModel.onSelectEvent(it) },
        onNavigateToMemorialTehillim = onNavigateToMemorialTehillim,
        onAddMemorial = { viewModel.onOpenAddDialog() },
        onEditMemorial = { viewModel.onOpenEditDialog(it) },
        onDeleteMemorial = { viewModel.deleteMemorialEvent(it.memorialId) },
        onDismissAddEdit = { viewModel.onCloseAddEditDialog() },
        onSaveMemorial = { familyId, personId, name, date, hebrew, description ->
            viewModel.saveMemorialEvent(familyId, personId, name, date, hebrew, description)
        },
        modifier = modifier,
    )
}

private enum class EventsViewMode { LIST, CALENDAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventsScreen(
    uiState: EventsUiState,
    onRetry: () -> Unit,
    onSelectEvent: (CalendarEvent?) -> Unit,
    onNavigateToMemorialTehillim: (deceasedName: String) -> Unit,
    modifier: Modifier = Modifier,
    onAddMemorial: () -> Unit = {},
    onEditMemorial: (CalendarEvent.Memorial) -> Unit = {},
    onDeleteMemorial: (CalendarEvent.Memorial) -> Unit = {},
    onDismissAddEdit: () -> Unit = {},
    onSaveMemorial: (String, String?, String, String, String?, String?) -> Unit = { _, _, _, _, _, _ -> },
) {
    var viewMode by rememberSaveable { mutableStateOf(EventsViewMode.CALENDAR) }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.events_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                com.shorashim.core.ui.component.PillToggle(
                    options = listOf(
                        com.shorashim.core.ui.component.PillOption(stringResource(R.string.events_view_calendar)),
                        com.shorashim.core.ui.component.PillOption(stringResource(R.string.events_view_list)),
                    ),
                    selectedIndex = if (viewMode == EventsViewMode.CALENDAR) 0 else 1,
                    onSelect = { viewMode = if (it == 0) EventsViewMode.CALENDAR else EventsViewMode.LIST },
                    modifier = Modifier.width(200.dp),
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when {
                    uiState.isLoading && uiState.events.isEmpty() -> {
                        ShorashimLoadingState()
                    }
                    uiState.errorMessage != null && uiState.events.isEmpty() -> {
                        ShorashimErrorState(
                            message = uiState.errorMessage,
                            onRetry = onRetry,
                        )
                    }
                    viewMode == EventsViewMode.CALENDAR -> {
                        EventsCalendarView(
                            events = uiState.events,
                            onMemorialClick = { onSelectEvent(it) },
                        )
                    }
                    uiState.events.isEmpty() -> {
                        ShorashimEmptyState(
                            title = stringResource(R.string.events_empty_title),
                            message = stringResource(R.string.events_empty_message),
                            icon = Icons.Outlined.CalendarMonth,
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = Spacing.md,
                                end = Spacing.md,
                                top = Spacing.xs,
                                bottom = Spacing.md,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(uiState.events, key = { it.id }) { event ->
                                when (event) {
                                    is CalendarEvent.Birthday -> BirthdayCard(event = event)
                                    is CalendarEvent.Memorial -> MemorialCard(
                                        event = event,
                                        onClick = { onSelectEvent(event) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // Memorial Event Details Dialog
    val selected = uiState.selectedEvent
    if (selected is CalendarEvent.Memorial) {
        MemorialDetailsDialog(
            memorial = selected,
            onDismiss = { onSelectEvent(null) },
            onReadTehillim = {
                onSelectEvent(null)
                onNavigateToMemorialTehillim(selected.personName)
            },
            onEdit = { onEditMemorial(selected) },
            onDelete = { onDeleteMemorial(selected) },
        )
    }

    if (uiState.isAddEditOpen) {
        AddEditMemorialEventDialog(
            families = uiState.families,
            deceasedPeople = uiState.deceasedPeople,
            editingMemorial = uiState.editingMemorial,
            isSaving = uiState.isSaving,
            errorMessage = uiState.actionError,
            onDismiss = onDismissAddEdit,
            onSave = onSaveMemorial,
        )
    }
}

@Composable
internal fun BirthdayCard(
    event: CalendarEvent.Birthday,
    modifier: Modifier = Modifier,
) {
    val extra = com.shorashim.core.ui.theme.ShorashimTheme.colors
    val isToday = event.daysUntil == 0L
    com.shorashim.core.ui.component.ShorashimCard(
        modifier = modifier,
        borderColor = if (isToday) extra.clay.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.shorashim.core.ui.component.IconTile(
                icon = Icons.Outlined.Cake,
                tint = extra.clay,
                container = extra.clayContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = event.personName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                if (event.familyNames.isNotEmpty()) {
                    Text(
                        text = event.familyNames.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            }
            com.shorashim.core.ui.component.StatusPill(
                text = event.relativeLabel,
                container = if (isToday) extra.clay else extra.clayContainer,
                content = if (isToday) MaterialTheme.colorScheme.surface else extra.onClayContainer,
            )
        }
        if (event.message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = event.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun MemorialCard(
    event: CalendarEvent.Memorial,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val extra = com.shorashim.core.ui.theme.ShorashimTheme.colors
    com.shorashim.core.ui.component.ShorashimCard(
        onClick = onClick,
        modifier = modifier,
        borderColor = extra.gold.copy(alpha = 0.35f),
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.shorashim.core.ui.component.IconTile(
                icon = Icons.Outlined.LocalFireDepartment,
                tint = extra.gold,
                container = extra.goldContainer,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = event.personName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(event.hebrewDate?.ifBlank { null }, event.familyName.ifBlank { null })
                        .joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
            com.shorashim.core.ui.component.StatusPill(
                text = event.relativeLabel,
                container = extra.goldContainer,
                content = extra.onGoldContainer,
            )
        }

        if (!event.description.isNullOrBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun MemorialDetailsDialog(
    memorial: CalendarEvent.Memorial,
    onDismiss: () -> Unit,
    onReadTehillim: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    if (confirmDelete) {
        ShorashimConfirmDialog(
            onDismissRequest = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                onDelete()
            },
            title = stringResource(R.string.events_delete_memorial_confirm_title),
            message = stringResource(R.string.events_delete_memorial_confirm_msg),
            confirmText = stringResource(R.string.events_delete_action),
            dismissText = stringResource(R.string.cancel),
            icon = Icons.Outlined.Delete,
            isDestructive = true,
        )
    }

    ShorashimDialog(
        onDismissRequest = onDismiss,
        title = "אזכרה לזכרו של ${memorial.personName}",
        subtitle = "${memorial.relativeLabel} (${memorial.originalDateStr})",
        icon = Icons.Outlined.LocalFireDepartment,
        iconTint = com.shorashim.core.ui.theme.ShorashimTheme.colors.gold,
        iconBackground = com.shorashim.core.ui.theme.ShorashimTheme.colors.goldContainer,
        confirmButton = if (memorial.canEdit) null else {
            {
                androidx.compose.material3.TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("סגירה", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = if (memorial.canEdit) {
            {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.events_edit_action))
                    }
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.events_delete_action))
                    }
                }
            }
        } else null,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (!memorial.hebrewDate.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "תאריך עברי: ",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = memorial.hebrewDate,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "עץ משפחה: ",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = memorial.familyName,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (!memorial.description.isNullOrBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        )
                        Text(
                            text = memorial.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }

            // Button to read Tehillim for this deceased person
            Button(
                onClick = onReadTehillim,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.shorashim.core.ui.theme.ShorashimTheme.colors.gold,
                    contentColor = MaterialTheme.colorScheme.surface,
                ),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.events_read_tehillim_button),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}
