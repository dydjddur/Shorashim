package com.shorashim.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Universal PersonAvatar component:
 * - If photo exists: displays the face image cropped to shape.
 * - If deceased: adds a respectful golden accent and candle indicator.
 * - If no photo: displays initial letter or person placeholder icon.
 */
@Composable
fun PersonAvatar(
    photoPath: String?,
    firstName: String,
    isDeceased: Boolean = false,
    size: Dp = 46.dp,
    shape: Shape = CircleShape,
    modifier: Modifier = Modifier,
) {
    val syncBitmap = remember(photoPath) { ImageUtils.decodeToBitmap(photoPath) }
    var displayedBitmap by remember(photoPath) { mutableStateOf(syncBitmap) }

    LaunchedEffect(photoPath) {
        if (syncBitmap == null && photoPath?.startsWith("http") == true) {
            val loaded = withContext(Dispatchers.IO) {
                ImageUtils.loadHttpBitmap(photoPath)
            }
            displayedBitmap = loaded
        } else {
            displayedBitmap = syncBitmap
        }
    }

    val finalBitmap = displayedBitmap

    if (finalBitmap != null) {
        Box(
            modifier = modifier.size(size),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = finalBitmap,
                contentDescription = firstName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .border(
                        width = if (isDeceased) 2.dp else 1.dp,
                        color = if (isDeceased) com.shorashim.core.ui.theme.ShorashimTheme.colors.gold else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        shape = shape,
                    ),
            )

            if (isDeceased) {
                val badgeSize = (size.value * 0.42f).dp.coerceIn(16.dp, 28.dp)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(badgeSize)
                        .background(Color.Black.copy(alpha = 0.75f), CircleShape)
                        .border(1.dp, com.shorashim.core.ui.theme.ShorashimTheme.colors.gold, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "🕯️",
                        fontSize = (badgeSize.value * 0.55f).coerceIn(9f, 16f).sp,
                    )
                }
            }
        }
    } else {
        Surface(
            shape = shape,
            color = if (isDeceased) {
                com.shorashim.core.ui.theme.ShorashimTheme.colors.gold.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            border = if (isDeceased) BorderStroke(1.5.dp, com.shorashim.core.ui.theme.ShorashimTheme.colors.gold) else null,
            modifier = modifier.size(size),
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (isDeceased) {
                    Text(
                        text = "🕯️",
                        fontSize = (size.value * 0.42f).coerceIn(16f, 36f).sp,
                    )
                } else {
                    val initial = firstName.trim().firstOrNull()?.toString()
                    if (initial != null) {
                        Text(
                            text = initial,
                            fontSize = (size.value * 0.44f).coerceIn(15f, 36f).sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(size * 0.55f),
                        )
                    }
                }
            }
        }
    }
}
