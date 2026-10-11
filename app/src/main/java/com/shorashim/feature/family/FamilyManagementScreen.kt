package com.shorashim.feature.family

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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.shorashim.core.model.DatePrecision
import com.shorashim.core.model.LifeStatus
import com.shorashim.core.model.PartialDate
import com.shorashim.core.model.Person
import com.shorashim.core.model.RelationshipType
import com.shorashim.core.ui.component.ShorashimErrorState
import com.shorashim.core.ui.component.ShorashimLoadingState
import com.shorashim.core.ui.component.Spacing
import com.shorashim.feature.familytree.canvas.FamilyTreeCanvas

import androidx.compose.ui.platform.LocalContext
import com.shorashim.data.invitations.InvitationRepository
import com.shorashim.feature.invitations.ShareInviteViewModel

@Composable
fun FamilyManagementRoute(
    viewModel: FamilyManagementViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddPerson: (familyId: String) -> Unit,
    onNavigateToAddRelative: (familyId: String, relation: com.shorashim.core.model.NewPersonRelation, anchorId: String) -> Unit = { _, _, _ -> },
    onNavigateToEditPerson: (familyId: String, personId: String) -> Unit,
    onNavigateToCreateFamily: () -> Unit = {},
    invitationRepository: InvitationRepository? = null,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val shareInviteViewModel: ShareInviteViewModel? = invitationRepository?.let {
        remember(uiState.currentFamilyId) {
            ShareInviteViewModel(uiState.currentFamilyId, it)
        }
    }

    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.loadData()
    }

    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    FamilyManagementScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onNavigateBack = onNavigateBack,
        onSelectFamily = viewModel::selectFamily,
        onCreateNewTreeClick = onNavigateToCreateFamily,
        onSetTab = viewModel::setTab,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onPublish = viewModel::publishFamily,
        onUnpublish = viewModel::unpublishFamily,
        onRenameClicked = { viewModel.setRenaming(true) },
        onRenameDismiss = { viewModel.setRenaming(false) },
        onRenameConfirm = viewModel::renameFamily,
        onOpenManagementSheet = { viewModel.setManagementSheetOpen(true) },
        onCloseManagementSheet = { viewModel.setManagementSheetOpen(false) },
        onOpenMembersDialog = {
            viewModel.setManagementSheetOpen(false)
            viewModel.setMembersDialogOpen(true)
        },
        onCloseMembersDialog = { viewModel.setMembersDialogOpen(false) },
        onInviteMember = viewModel::inviteMember,
        onUpdateMemberRole = viewModel::updateMemberRole,
        onRemoveMember = viewModel::removeMember,
        onDeleteFamilyClicked = {
            viewModel.setManagementSheetOpen(false)
            viewModel.setDeleteFamilyConfirmOpen(true)
        },
        onDeleteFamilyDismiss = { viewModel.setDeleteFamilyConfirmOpen(false) },
        onDeleteFamilyConfirm = { viewModel.deleteFamily(onDeleted = onNavigateBack) },
        onAddPersonClick = { onNavigateToAddPerson(uiState.currentFamilyId) },
        onPersonClick = { personId -> onNavigateToEditPerson(uiState.currentFamilyId, personId) },
        onDeletePerson = viewModel::deletePerson,
        onOpenAddRelationship = { viewModel.setAddRelationshipOpen(true) },
        onCloseAddRelationship = { viewModel.setAddRelationshipOpen(false) },
        onCreateRelationship = { pA, pB, type ->
            viewModel.createRelationship(
                personAId = pA,
                personBId = pB,
                type = type,
                onSuccess = { viewModel.setAddRelationshipOpen(false) },
                onError = { /* handled in state */ },
            )
        },
        onAddChildClick = { parent ->
            onNavigateToAddRelative(uiState.currentFamilyId, com.shorashim.core.model.NewPersonRelation.CHILD, parent.id)
        },
        onAddParentClick = { child ->
            onNavigateToAddRelative(uiState.currentFamilyId, com.shorashim.core.model.NewPersonRelation.PARENT, child.id)
        },
        onAddSpouseClick = { spouse ->
            onNavigateToAddRelative(uiState.currentFamilyId, com.shorashim.core.model.NewPersonRelation.SPOUSE, spouse.id)
        },
        onAddSiblingClick = { sibling ->
            onNavigateToAddRelative(uiState.currentFamilyId, com.shorashim.core.model.NewPersonRelation.SIBLING, sibling.id)
        },
        onShareInvite = {
            shareInviteViewModel?.createAndShare(context, familyName = uiState.family?.name)
        },
        onRefresh = viewModel::loadData,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyManagementScreen(
    uiState: FamilyManagementUiState,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit,
    onSelectFamily: (String) -> Unit,
    onCreateNewTreeClick: () -> Unit,
    onSetTab: (FamilyViewTab) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onPublish: () -> Unit,
    onUnpublish: () -> Unit,
    onRenameClicked: () -> Unit,
    onRenameDismiss: () -> Unit,
    onRenameConfirm: (String) -> Unit,
    onOpenManagementSheet: () -> Unit,
    onCloseManagementSheet: () -> Unit,
    onOpenMembersDialog: () -> Unit,
    onCloseMembersDialog: () -> Unit,
    onInviteMember: (email: String, role: com.shorashim.core.model.MembershipRole) -> Unit,
    onUpdateMemberRole: (userId: String, role: com.shorashim.core.model.MembershipRole) -> Unit,
    onRemoveMember: (userId: String) -> Unit,
    onDeleteFamilyClicked: () -> Unit,
    onDeleteFamilyDismiss: () -> Unit,
    onDeleteFamilyConfirm: () -> Unit,
    onAddPersonClick: () -> Unit,
    onPersonClick: (String) -> Unit,
    onDeletePerson: (String) -> Unit,
    onOpenAddRelationship: () -> Unit,
    onCloseAddRelationship: () -> Unit,
    onCreateRelationship: (String, String, RelationshipType) -> Unit,
    onAddChildClick: (Person) -> Unit,
    onAddParentClick: (Person) -> Unit,
    onAddSpouseClick: (Person) -> Unit,
    onAddSiblingClick: (Person) -> Unit = {},
    onShareInvite: () -> Unit = {},
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var renameText by remember(uiState.family?.name) { mutableStateOf(uiState.family?.name.orEmpty()) }
    var personPendingDelete by remember { mutableStateOf<Person?>(null) }

    // Deleting a person also removes their relationships, so always ask first.
    personPendingDelete?.let { victim ->
        AlertDialog(
            onDismissRequest = { personPendingDelete = null },
            title = {
                Text(
                    text = stringResource(R.string.person_delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = { Text(stringResource(R.string.person_delete_dialog_message, victim.fullName)) },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePerson(victim.id)
                        personPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.person_delete_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { personPendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Rename Dialog
    if (uiState.isRenaming) {
        AlertDialog(
            onDismissRequest = onRenameDismiss,
            title = {
                Text(
                    text = stringResource(R.string.family_rename_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.family_name_label)) },
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(
                    onClick = { onRenameConfirm(renameText) },
                    enabled = renameText.isNotBlank(),
                ) {
                    Text(stringResource(R.string.family_rename_submit))
                }
            },
            dismissButton = {
                TextButton(onClick = onRenameDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    // Add Relationship Dialog
    if (uiState.isAddRelationshipOpen) {
        AddRelationshipDialog(
            people = uiState.people,
            onDismiss = onCloseAddRelationship,
            onSubmit = onCreateRelationship,
            errorMessage = uiState.error,
        )
    }

    // Tree Management Bottom Sheet
    if (uiState.isManagementSheetOpen && uiState.family != null) {
        TreeManagementBottomSheet(
            family = uiState.family,
            onPublish = onPublish,
            onUnpublish = onUnpublish,
            onOpenMembers = onOpenMembersDialog,
            onShareInvite = {
                onCloseManagementSheet()
                onShareInvite()
            },
            onRenameClick = {
                onCloseManagementSheet()
                onRenameClicked()
            },
            onDeleteClick = onDeleteFamilyClicked,
            onDismiss = onCloseManagementSheet,
        )
    }

    // Members & Roles Dialog
    if (uiState.isMembersDialogOpen && uiState.family != null) {
        FamilyMembersDialog(
            members = uiState.members,
            canManageRoles = uiState.family.canManageMembers,
            isOwner = uiState.family.isOwner,
            onInviteMember = onInviteMember,
            onUpdateRole = onUpdateMemberRole,
            onRemoveMember = onRemoveMember,
            onDismiss = onCloseMembersDialog,
        )
    }

    // Delete Family Confirm Dialog
    if (uiState.isDeleteFamilyConfirmOpen) {
        AlertDialog(
            onDismissRequest = onDeleteFamilyDismiss,
            title = {
                Text(
                    text = stringResource(R.string.family_delete_dialog_title),
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(stringResource(R.string.family_delete_dialog_message, uiState.family?.name.orEmpty()))
            },
            confirmButton = {
                Button(
                    onClick = onDeleteFamilyConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = onDeleteFamilyDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    TreeSwitcherDropdown(
                        currentFamily = uiState.family,
                        allFamilies = uiState.allFamilies,
                        onSelectFamily = onSelectFamily,
                        onCreateNewTreeClick = onCreateNewTreeClick,
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
                    IconButton(onClick = onOpenManagementSheet) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "ניהול עץ",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            if (uiState.family?.canEditTreeContent == true) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FloatingActionButton(
                        onClick = onOpenAddRelationship,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GroupAdd,
                            contentDescription = stringResource(R.string.relationship_dialog_title),
                        )
                    }

                    FloatingActionButton(
                        onClick = onAddPersonClick,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = stringResource(R.string.family_people_add),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // View Mode Tabs: Workflow Map (Canvas) vs List View
            TabRow(
                selectedTabIndex = if (uiState.activeTab == FamilyViewTab.CANVAS) 0 else 1,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(
                    selected = uiState.activeTab == FamilyViewTab.CANVAS,
                    onClick = { onSetTab(FamilyViewTab.CANVAS) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.AccountTree, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("מפת שושלת", fontWeight = FontWeight.Bold)
                        }
                    },
                )
                Tab(
                    selected = uiState.activeTab == FamilyViewTab.LIST,
                    onClick = { onSetTab(FamilyViewTab.LIST) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.FormatListBulleted, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("רשימה וחיפוש", fontWeight = FontWeight.Bold)
                        }
                    },
                )
            }

            when {
                uiState.isLoading && uiState.family == null -> {
                    ShorashimLoadingState(modifier = Modifier.fillMaxSize())
                }
                uiState.error != null && uiState.family == null -> {
                    ShorashimErrorState(
                        message = uiState.error,
                        onRetry = onRefresh,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                uiState.activeTab == FamilyViewTab.CANVAS -> {
                    // Interactive Canvas View
                    FamilyTreeCanvas(
                        family = uiState.family ?: return@Column,
                        people = uiState.people,
                        relationships = uiState.relationships,
                        onAddChildClick = onAddChildClick,
                        onAddParentClick = onAddParentClick,
                        onAddSpouseClick = onAddSpouseClick,
                        onAddSiblingClick = onAddSiblingClick,
                        onEditPersonClick = { p -> onPersonClick(p.id) },
                        onDeletePersonClick = { p -> personPendingDelete = p },
                        onPublishClick = onPublish,
                        onEmptyAddPersonClick = onAddPersonClick,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                else -> {
                    // List View with Search
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = Spacing.md),
                    ) {
                        Spacer(Modifier.height(Spacing.sm))

                        // Search Field
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = onSearchQueryChanged,
                            label = { Text(stringResource(R.string.family_search_hint)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                        )

                        Spacer(Modifier.height(Spacing.sm))

                        if (uiState.filteredPeople.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = stringResource(R.string.family_people_empty),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = Spacing.sm),
                                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                            ) {
                                items(uiState.filteredPeople, key = { it.id }) { person ->
                                    PersonListItemCard(
                                        person = person,
                                        onClick = { onPersonClick(person.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonListItemCard(
    person: Person,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDeceased = person.lifeStatus == LifeStatus.DECEASED
    val bDate = PartialDate.fromIso(person.birthDate, person.birthDatePrecision)
    val dDate = PartialDate.fromIso(person.deathDate, person.deathDatePrecision)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            1.dp,
            if (isDeceased) Color(0xFFD4AF37).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (isDeceased) Color(0xFFD4AF37).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(46.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isDeceased) {
                        Text(text = "🕯️", fontSize = 20.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = person.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                val dateInfo = when {
                    bDate.precision != DatePrecision.UNKNOWN && dDate.precision != DatePrecision.UNKNOWN ->
                        "${bDate.toDisplayString()} – ${dDate.toDisplayString()}"
                    bDate.precision != DatePrecision.UNKNOWN -> "לידה: ${bDate.toDisplayString()}"
                    dDate.precision != DatePrecision.UNKNOWN -> "פטירה: ${dDate.toDisplayString()}"
                    isDeceased -> "ז״ל 🕯️"
                    else -> "חי/ה"
                }
                Text(
                    text = dateInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
