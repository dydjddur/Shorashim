package com.shorashim.feature.family

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.ui.component.Spacing
import com.shorashim.feature.auth.AuthCard
import com.shorashim.feature.auth.AuthPrimaryButton
import com.shorashim.feature.auth.AuthTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFamilyRoute(
    viewModel: CreateFamilyViewModel,
    onNavigateBack: () -> Unit,
    onFamilyCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    CreateFamilyScreen(
        uiState = uiState,
        onNameChanged = viewModel::onNameChanged,
        onSubmit = { viewModel.createFamily(onFamilyCreated) },
        onNavigateBack = onNavigateBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFamilyScreen(
    uiState: CreateFamilyUiState,
    onNameChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.family_create_title),
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
        ) {
            AuthCard {
                Text(
                    text = stringResource(R.string.family_create_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = Spacing.md),
                )

                AuthTextField(
                    value = uiState.name,
                    onValueChange = onNameChanged,
                    label = stringResource(R.string.family_name_label),
                    placeholder = stringResource(R.string.family_name_hint),
                    leadingIcon = Icons.Outlined.Park,
                    isError = uiState.nameError != null,
                    errorMessage = uiState.nameError?.let { stringResource(it) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            onSubmit()
                        },
                    ),
                    enabled = !uiState.isLoading,
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

                AuthPrimaryButton(
                    text = stringResource(R.string.family_create_submit),
                    onClick = {
                        focusManager.clearFocus()
                        onSubmit()
                    },
                    isLoading = uiState.isLoading,
                    enabled = !uiState.isLoading && uiState.name.isNotBlank(),
                )
            }
        }
    }
}
