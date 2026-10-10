package com.shorashim.feature.tehillim

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
fun TehillimRoute(
    viewModel: TehillimViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TehillimScreen(uiState = uiState, modifier = modifier)
}

@Composable
fun TehillimScreen(
    uiState: TehillimUiState,
    modifier: Modifier = Modifier,
) {
    when (val content = uiState.content) {
        ScreenContentState.Loading -> ShorashimLoadingState(modifier)
        ScreenContentState.Empty, ScreenContentState.Ready -> ShorashimEmptyState(
            title = stringResource(R.string.tehillim_empty_title),
            message = stringResource(R.string.tehillim_empty_message),
            modifier = modifier,
        )
        is ScreenContentState.Error -> ShorashimErrorState(
            message = content.message,
            modifier = modifier,
        )
    }
}
