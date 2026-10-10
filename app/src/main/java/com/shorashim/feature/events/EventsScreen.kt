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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

    EventsScreen(
        uiState = uiState,
        onRetry = { viewModel.loadEvents() },
        onSelectEvent = { viewModel.onSelectEvent(it) },
        onOpenAddDialog = { viewModel.onOpenAddDialog() },
        onOpenEditDialog = { viewModel.onOpenEditDialog(it) },
        onCloseAddEditDialog = { viewModel.onCloseAddEditDialog() },
        onSaveMemorial = { famId, pId, name, date, hebDate, desc ->
            viewModel.saveMemorialEvent(famId, pId, name, date, hebDate, desc)
        },
        onDeleteMemorial = { viewModel.deleteMemorialEvent(it) },
        onNavigateToMemorialTehillim = onNavigateToMemorialTehillim,
        modifier = modifier,
    )
}

@Composable
fun EventsScreen(
    uiState: EventsUiState,
    onRetry: () -> Unit,
    onSelectEvent: (CalendarEvent?) -> Unit,
    onOpenAddDialog: () -> Unit,
    onOpenEditDialog: (CalendarEvent.Memorial) -> Unit,
    onCloseAddEditDialog: () -> Unit,
    onSaveMemorial: (familyId: String, personId: String?, personName: String, eventDate: String, hebrewDate: String?, description: String?) -> Unit,
    onDeleteMemorial: (memorialId: String) -> Unit,
    onNavigateToMemorialTehillim: (deceasedName: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteConfirmDialog by remember { mutableStateOf<CalendarEvent.Memorial?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenAddDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(Spacing.xs))
                    Text(
                        text = stringResource(R.string.events_add_memorial_button),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
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
                            top = Spacing.md,
                            bottom = 80.dp, // Space for FAB
                        ),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.events_title),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = Spacing.xs),
                            )
                        }

                        items(uiState.events, key = { it.id }) { event ->
                            when (event) {
                                is CalendarEvent.Birthday -> {
                                    BirthdayCard(event = event)
                                }
                                is CalendarEvent.Memorial -> {
                                    MemorialCard(
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
            onEdit = {
                onOpenEditDialog(selected)
            },
            onDelete = {
                showDeleteConfirmDialog = selected
            },
        )
    }

    // Delete confirmation dialog
    val toDelete = showDeleteConfirmDialog
    if (toDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text(stringResource(R.string.events_delete_memorial_confirm_title)) },
            text = { Text(stringResource(R.string.events_delete_memorial_confirm_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        val id = toDelete.memorialId
                        showDeleteConfirmDialog = null
                        onDeleteMemorial(id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Add / Edit Dialog
    if (uiState.isAddEditOpen) {
        AddEditMemorialEventDialog(
            families = uiState.families,
            deceasedPeople = uiState.deceasedPeople,
            editingMemorial = uiState.editingMemorial,
            isSaving = uiState.isSaving,
            onDismiss = onCloseAddEditDialog,
            onSave = onSaveMemorial,
        )
    }
}

@Composable
private fun BirthdayCard(
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
private fun MemorialCard(
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
                        text = "הערות: ${memorial.description}",
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

                if (memorial.canEdit) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("עריכה")
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("מחיקה")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
