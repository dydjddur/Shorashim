package com.shorashim.feature.tehillim

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shorashim.R
import com.shorashim.core.common.HebrewDates
import com.shorashim.core.ui.component.Spacing

/** Warm "gold leaf" accent used for ornaments and the memorial (le-iluy nishmat) mode. */
private val Gold = Color(0xFFC9A24B)
private val GoldSoft = Color(0xFFE9D8A6)
private val Candle = Color(0xFFF2B84B)

@Composable
fun TehillimRoute(
    viewModel: TehillimViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TehillimScreen(
        uiState = uiState,
        onSelectChapter = { viewModel.selectChapter(it) },
        onSelectMemorialMode = { viewModel.enterMemorialMode() },
        onExitMemorialMode = { viewModel.exitMemorialMode() },
        onNextChapter = { viewModel.nextChapter() },
        onPreviousChapter = { viewModel.previousChapter() },
        onToggleDrawer = { viewModel.setDrawerOpen(it) },
        onAdjustFontSize = { viewModel.adjustFontSize(it) },
        modifier = modifier,
    )
}

/** 15 -> ט״ו, 5 -> ה׳ */
private fun hebrewNumeral(n: Int): String {
    val letters = HebrewDates.toGematria(n)
    return when {
        letters.length > 1 -> letters.dropLast(1) + "״" + letters.last()
        letters.length == 1 -> "$letters׳"
        else -> n.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TehillimScreen(
    uiState: TehillimUiState,
    onSelectChapter: (Int) -> Unit,
    onSelectMemorialMode: () -> Unit,
    onExitMemorialMode: () -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onToggleDrawer: (Boolean) -> Unit,
    onAdjustFontSize: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize(),
        topBar = {
            TehillimTopBar(
                uiState = uiState,
                onAdjustFontSize = onAdjustFontSize,
                onOpenChapters = { onToggleDrawer(true) },
            )
        },
        bottomBar = {
            TehillimBottomBar(
                uiState = uiState,
                onPrevious = onPreviousChapter,
                onNext = onNextChapter,
                onOpenChapters = { onToggleDrawer(true) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            if (uiState.isMemorialMode) {
                MemorialBanner(uiState = uiState, onExit = onExitMemorialMode)
                Spacer(Modifier.height(Spacing.lg))
            }

            val chapter = uiState.currentChapter
            if (chapter != null) {
                ChapterHeading(
                    title = chapter.hebrewTitle,
                    fontSizeSp = uiState.fontSizeSp,
                )
                Spacer(Modifier.height(Spacing.md))

                chapter.verses.forEachIndexed { index, verse ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        VerseBadge(
                            label = hebrewNumeral(index + 1),
                            fontSizeSp = uiState.fontSizeSp,
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = verse,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontSize = uiState.fontSizeSp.sp,
                                lineHeight = (uiState.fontSizeSp * 1.75).sp,
                            ),
                            color = scheme.onSurface,
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.lg))
                Ornament()
            } else {
                Text(
                    text = "טוען פרק...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(Spacing.xl))
        }
    }

    if (uiState.isDrawerOpen) {
        ModalBottomSheet(
            onDismissRequest = { onToggleDrawer(false) },
            sheetState = sheetState,
            containerColor = scheme.surface,
        ) {
            ChapterPicker(
                uiState = uiState,
                onSelectChapter = {
                    onToggleDrawer(false)
                    onSelectChapter(it)
                },
                onSelectMemorialMode = {
                    onToggleDrawer(false)
                    onSelectMemorialMode()
                },
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Top / bottom bars
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun TehillimTopBar(
    uiState: TehillimUiState,
    onAdjustFontSize: (Int) -> Unit,
    onOpenChapters: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(color = scheme.background) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(scheme.primaryContainer, scheme.surfaceVariant),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.MenuBook,
                    contentDescription = null,
                    tint = Gold,
                    modifier = Modifier.size(26.dp),
                )
            }
            Spacer(Modifier.width(Spacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (uiState.isMemorialMode) {
                        stringResource(R.string.tehillim_memorial_heading)
                    } else {
                        stringResource(R.string.tehillim_title)
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = scheme.onBackground,
                    maxLines = 1,
                )
                Text(
                    text = if (uiState.isMemorialMode) "לעילוי נשמה" else "מזמורי דוד המלך",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (uiState.isMemorialMode) Candle else Gold,
                )
            }

            // Font size stepper:  א−  |  א+
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = scheme.surfaceVariant,
                border = BorderStroke(1.dp, scheme.outlineVariant),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "א",
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        modifier = Modifier
                            .clickable { onAdjustFontSize(-2) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(scheme.outlineVariant),
                    )
                    Text(
                        text = "א",
                        fontSize = 22.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        modifier = Modifier
                            .clickable { onAdjustFontSize(2) }
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                    )
                }
            }
            Spacer(Modifier.width(Spacing.sm))
            FilledTonalIconButton(
                onClick = onOpenChapters,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = scheme.primaryContainer,
                    contentColor = scheme.primary,
                ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.GridView,
                    contentDescription = stringResource(R.string.tehillim_select_chapter),
                )
            }
        }
    }
}

@Composable
private fun TehillimBottomBar(
    uiState: TehillimUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onOpenChapters: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val chapterNumber = uiState.currentChapter?.chapter ?: 1
    val progress = if (uiState.isMemorialMode) {
        (uiState.memorialCurrentIndex + 1) / 7f
    } else {
        chapterNumber / 150f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = scheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, scheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = if (uiState.isMemorialMode) Candle else Gold,
                trackColor = scheme.outlineVariant.copy(alpha = 0.4f),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // In RTL "previous" sits on the right and points right; AutoMirrored.ArrowBack flips to that.
                NavCircle(
                    onClick = onPrevious,
                    icon = Icons.AutoMirrored.Outlined.ArrowBack,
                    label = stringResource(R.string.tehillim_previous_chapter),
                )

                Surface(
                    onClick = onOpenChapters,
                    shape = RoundedCornerShape(18.dp),
                    color = scheme.primaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.currentChapter?.hebrewTitle ?: "בחר פרק",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = scheme.onPrimaryContainer,
                            )
                            Text(
                                text = if (uiState.isMemorialMode) {
                                    "${uiState.memorialCurrentIndex + 1} מתוך 7"
                                } else {
                                    "$chapterNumber מתוך 150"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = scheme.onPrimaryContainer.copy(alpha = 0.7f),
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.ExpandMore,
                            contentDescription = null,
                            tint = scheme.onPrimaryContainer,
                        )
                    }
                }

                NavCircle(
                    onClick = onNext,
                    icon = Icons.AutoMirrored.Outlined.ArrowForward,
                    label = stringResource(R.string.tehillim_next_chapter),
                )
            }
        }
    }
}

@Composable
private fun NavCircle(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = Modifier.size(52.dp),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.primary,
            ),
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(26.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Reading area
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun ChapterHeading(title: String, fontSizeSp: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Ornament()
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = (fontSizeSp + 10).sp,
            ),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

/** ──── ✦ ──── divider in gold leaf. */
@Composable
private fun Ornament() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color.Transparent, Gold))),
        )
        Text(
            text = "✦",
            color = Gold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(Brush.horizontalGradient(listOf(Gold, Color.Transparent))),
        )
    }
}

@Composable
private fun VerseBadge(label: String, fontSizeSp: Int) {
    Box(
        modifier = Modifier
            .padding(top = 6.dp)
            .size(width = 34.dp, height = 26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Gold.copy(alpha = 0.16f))
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = (fontSizeSp - 7).coerceAtLeast(11).sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Serif,
            color = if (isSystemInDarkTheme()) GoldSoft else Color(0xFF7A5A12),
            maxLines = 1,
        )
    }
}

@Composable
private fun MemorialBanner(uiState: TehillimUiState, onExit: () -> Unit) {
    val dark = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = if (dark) Color(0xFF2A2412) else Color(0xFFFFF6DC)),
        border = BorderStroke(1.dp, Candle.copy(alpha = 0.7f)),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Candle.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "🕯️", fontSize = 20.sp)
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "סדר תהילים לעילוי נשמה",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = if (dark) GoldSoft else Color(0xFF4A3410),
                    )
                    Text(
                        text = "פרק ${uiState.memorialCurrentIndex + 1} מתוך 7",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (dark) Candle else Color(0xFF8A5A00),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            Text(
                text = uiState.dedicationText,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                ),
                color = if (dark) Color(0xFFF2E8CB) else Color(0xFF2D2208),
            )

            Spacer(Modifier.height(Spacing.md))

            Button(
                onClick = onExit,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (dark) Candle.copy(alpha = 0.2f) else Color(0xFF6B4A12),
                    contentColor = if (dark) GoldSoft else Color.White,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.tehillim_memorial_back_button), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------
// Chapter picker sheet
// ---------------------------------------------------------------------------------------------------------------

@Composable
private fun ChapterPicker(
    uiState: TehillimUiState,
    onSelectChapter: (Int) -> Unit,
    onSelectMemorialMode: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.tehillim_select_chapter),
            style = MaterialTheme.typography.titleLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
            ),
            color = scheme.primary,
            modifier = Modifier.padding(bottom = Spacing.sm),
        )

        // Highlighted: Tehillim le-iluy nishmat
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Spacing.md)
                .clickable(onClick = onSelectMemorialMode),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(1.5.dp, Candle),
        ) {
            Row(
                modifier = Modifier
                    .background(
                        Brush.horizontalGradient(
                            listOf(Candle.copy(alpha = 0.28f), Candle.copy(alpha = 0.10f)),
                        ),
                    )
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Candle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFF3E2A00),
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(Spacing.sm))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tehillim_memorial_item),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                        color = scheme.onSurface,
                    )
                    Text(
                        text = "7 פרקי הסגולה: ל״ג, ט״ז, י״ז, ע״ב, צ״א, ק״ד, ק״ל",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                    )
                }
                Text(text = "🕯️", fontSize = 24.sp)
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp),
            contentPadding = PaddingValues(bottom = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.allChapters, key = { it.chapter }) { chap ->
                val isSelected = !uiState.isMemorialMode && uiState.currentChapter?.chapter == chap.chapter
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clickable { onSelectChapter(chap.chapter) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) scheme.primary else scheme.surfaceVariant,
                    border = if (isSelected) null else BorderStroke(0.5.dp, scheme.outlineVariant),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = chap.hebrewTitle.replace("פרק ", ""),
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) scheme.onPrimary else scheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
