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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
        viewModel.loadEvents()
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
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.events_title),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.width(184.dp)) {
                    SegmentedButton(
                        selected = viewMode == EventsViewMode.CALENDAR,
                        onClick = { viewMode = EventsViewMode.CALENDAR },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {},
                    ) {
                        Text(stringResource(R.string.events_view_calendar), fontSize = 13.sp, maxLines = 1)
                    }
                    SegmentedButton(
                        selected = viewMode == EventsViewMode.LIST,
                        onClick = { viewMode = EventsViewMode.LIST },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {},
                    ) {
                        Text(stringResource(R.string.events_view_list), fontSize = 13.sp, maxLines = 1)
                    }
                }
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
                            verticalArrangement = Arrangement.spacedBy(Spacing.md),
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
            if (uiState.families.isNotEmpty()) {
                FilledTonalButton(
                    onClick = onAddMemorial,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.events_add_memorial_button), fontWeight = FontWeight.SemiBold)
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
        ),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Cake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Text(
                            text = event.personName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (event.familyNames.isNotEmpty()) {
                            Text(
                                text = "עץ: " + event.familyNames.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (event.daysUntil == 0L) Color(0xFFE91E63) else MaterialTheme.colorScheme.primary,
                ) {
                    Text(
                        text = event.relativeLabel,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = event.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
        }
    }
}

@Composable
internal fun MemorialCard(
    event: CalendarEvent.Memorial,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFFFA000).copy(alpha = 0.5f), // Warm amber for memorial
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFFFA000),
                            modifier = Modifier.size(26.dp),
                        )
                    }
                    Spacer(Modifier.width(Spacing.sm))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = event.personName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.width(Spacing.xs))
                            Text(text = "🕯️", fontSize = 14.sp)
                        }
                        Text(
                            text = "עץ: ${event.familyName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFA000),
                ) {
                    Text(
                        text = event.relativeLabel,
                        color = Color.Black,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }

            if (!event.hebrewDate.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = "תאריך עברי: ${event.hebrewDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (!event.description.isNullOrBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                )
            }
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
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.events_delete_memorial_confirm_title)) },
            text = { Text(stringResource(R.string.events_delete_memorial_confirm_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) {
                    Text(stringResource(R.string.events_delete_action), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🕯️", fontSize = 20.sp)
                Spacer(Modifier.width(Spacing.xs))
                Text(
                    text = "אזכרה לזכרו של ${memorial.personName}",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = "מועד קרוב: ${memorial.relativeLabel} (${memorial.originalDateStr})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!memorial.hebrewDate.isNullOrBlank()) {
                    Text(
                        text = "תאריך עברי: ${memorial.hebrewDate}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = "עץ משפחה: ${memorial.familyName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!memorial.description.isNullOrBlank()) {
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        text = memorial.description,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(Spacing.sm))

                // Button to read Tehillim for this deceased person
                Button(
                    onClick = onReadTehillim,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF9A825), // Warm gold
                        contentColor = Color.Black,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.events_read_tehillim_button),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
        dismissButton = if (memorial.canEdit) {
            {
                Row {
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.events_edit_action)) }
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(stringResource(R.string.events_delete_action), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        } else {
            null
        },
    )
}
