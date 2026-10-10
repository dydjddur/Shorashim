package com.shorashim.feature.familytree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.model.ScreenContentState
import com.shorashim.core.ui.component.ShorashimEmptyState
import com.shorashim.core.ui.component.ShorashimErrorState
import com.shorashim.core.ui.component.ShorashimLoadingState

@Composable
fun FamilyTreeRoute(
    viewModel: FamilyTreeViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FamilyTreeScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun FamilyTreeScreen(
    uiState: FamilyTreeUiState,
    modifier: Modifier = Modifier,
) {
    when (val content = uiState.content) {
        ScreenContentState.Loading -> ShorashimLoadingState(modifier)
        ScreenContentState.Empty -> ShorashimEmptyState(
            title = stringResource(R.string.family_tree_empty_title),
            message = stringResource(R.string.family_tree_empty_message),
            modifier = modifier,
        )
        is ScreenContentState.Error -> ShorashimErrorState(
            message = content.message,
            modifier = modifier,
        )
        ScreenContentState.Ready -> ShorashimEmptyState(
            title = uiState.summary.name.orEmpty(),
            message = stringResource(R.string.family_tree_empty_message),
            modifier = modifier,
        )
    }
}
