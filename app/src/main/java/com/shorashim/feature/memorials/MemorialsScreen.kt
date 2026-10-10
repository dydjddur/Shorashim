package com.shorashim.feature.memorials

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
fun MemorialsRoute(
    viewModel: MemorialsViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MemorialsScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun MemorialsScreen(
    uiState: MemorialsUiState,
    modifier: Modifier = Modifier,
) {
    when (val content = uiState.content) {
        ScreenContentState.Loading -> ShorashimLoadingState(modifier)
        ScreenContentState.Empty, ScreenContentState.Ready -> ShorashimEmptyState(
            title = stringResource(R.string.memorials_empty_title),
            message = stringResource(R.string.memorials_empty_message),
            modifier = modifier,
        )
        is ScreenContentState.Error -> ShorashimErrorState(
            message = content.message,
            modifier = modifier,
        )
    }
}
