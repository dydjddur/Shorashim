package com.shorashim.feature.familytree.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.core.model.Person
import kotlin.math.roundToInt

/**
 * Pointy-topped hexagon shape resembling a honeycomb cell.
 */
val HoneycombHexagonShape: Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(w * 0.5f, 0f)
    lineTo(w, h * 0.25f)
    lineTo(w, h * 0.75f)
    lineTo(w * 0.5f, h)
    lineTo(0f, h * 0.75f)
    lineTo(0f, h * 0.25f)
    close()
}

/**
 * Hexagonal honeycomb-style interface for the selected person in the family tree.
 *
 * Positions per specification:
 * - Top: הורים (emoji: 👪)
 * - Right: אח/אחות (emoji: 🤝)
 * - Left: בן/בת זוג (emoji: 💍)
 * - Bottom: ילד/ילדה (emoji: 👪)
 *
 * Beneath the selected person:
 * - Two small hexagonal buttons: Edit and Delete.
 */
@Composable
fun HexagonalRelationshipControls(
    node: TreeNode,
    d: Float, // screen density factor
    shiftX: Float,
    shiftY: Float,
    onAddChildClick: (Person) -> Unit,
    onAddParentClick: (Person) -> Unit,
    onAddSpouseClick: (Person) -> Unit,
    onAddSiblingClick: (Person) -> Unit,
    onEditPersonClick: (Person) -> Unit,
    onDeletePersonClick: (Person) -> Unit,
    modifier: Modifier = Modifier,
) {
    val person = node.person

    // Card dimensions in layout units (dp)
    val cardW = node.width
    val cardH = node.height
    val cardX = node.x + shiftX
    val cardY = node.y + shiftY

    val relHexSize = 56f
    val actionHexSize = 34f

    // 1. TOP: הורים (centered horizontally, above the card)
    HexagonRelationshipButton(
        emoji = "👪",
        label = "הורים",
        borderColor = MaterialTheme.colorScheme.primary,
        onClick = { onAddParentClick(person) },
        modifier = Modifier.absoluteOffset {
            IntOffset(
                x = ((cardX + cardW / 2f - relHexSize / 2f) * d).roundToInt(),
                y = ((cardY - relHexSize * 0.65f) * d).roundToInt(),
            )
        },
        sizeDp = relHexSize,
    )

    // 2. RIGHT: אח/אחות (to the right of the card, centered vertically)
    HexagonRelationshipButton(
        emoji = "🤝",
        label = "אח/אחות",
        borderColor = MaterialTheme.colorScheme.tertiary,
        onClick = { onAddSiblingClick(person) },
        modifier = Modifier.absoluteOffset {
            IntOffset(
                x = ((cardX + cardW + 8f) * d).roundToInt(),
                y = ((cardY + cardH / 2f - relHexSize / 2f) * d).roundToInt(),
            )
        },
        sizeDp = relHexSize,
    )

    // 3. LEFT: בן/בת זוג (to the left of the card, centered vertically)
    HexagonRelationshipButton(
        emoji = "💍",
        label = "בן/בת זוג",
        borderColor = Color(0xFFC48B47),
        onClick = { onAddSpouseClick(person) },
        modifier = Modifier.absoluteOffset {
            IntOffset(
                x = ((cardX - relHexSize - 8f) * d).roundToInt(),
                y = ((cardY + cardH / 2f - relHexSize / 2f) * d).roundToInt(),
            )
        },
        sizeDp = relHexSize,
    )

    // 4. ACTION BUTTONS: Edit & Delete (directly beneath the card)
    val editDeleteCenterY = cardY + cardH + 4f
    // Edit (left side of center)
    HexagonActionButton(
        icon = Icons.Outlined.Edit,
        iconTint = MaterialTheme.colorScheme.primary,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        onClick = { onEditPersonClick(person) },
        contentDescription = "ערוך פרטי אדם",
        modifier = Modifier.absoluteOffset {
            IntOffset(
                x = ((cardX + cardW / 2f - actionHexSize - 4f) * d).roundToInt(),
                y = (editDeleteCenterY * d).roundToInt(),
            )
        },
        sizeDp = actionHexSize,
    )

    // Delete (right side of center)
    HexagonActionButton(
        icon = Icons.Outlined.Delete,
        iconTint = MaterialTheme.colorScheme.error,
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
        onClick = { onDeletePersonClick(person) },
        contentDescription = "מחק אדם מהעץ",
        modifier = Modifier.absoluteOffset {
            IntOffset(
                x = ((cardX + cardW / 2f + 4f) * d).roundToInt(),
                y = (editDeleteCenterY * d).roundToInt(),
            )
        },
        sizeDp = actionHexSize,
    )

    // 5. BOTTOM: ילד/ילדה (centered horizontally, below the edit/delete buttons)
    HexagonRelationshipButton(
        emoji = "👪",
        label = "ילד/ילדה",
        borderColor = MaterialTheme.colorScheme.primary,
        onClick = { onAddChildClick(person) },
        modifier = Modifier.absoluteOffset {
            IntOffset(
                x = ((cardX + cardW / 2f - relHexSize / 2f) * d).roundToInt(),
                y = ((editDeleteCenterY + actionHexSize + 6f) * d).roundToInt(),
            )
        },
        sizeDp = relHexSize,
    )
}

@Composable
private fun HexagonRelationshipButton(
    emoji: String,
    label: String,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    sizeDp: Float = 56f,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(4.dp, shape = HoneycombHexagonShape)
            .clip(HoneycombHexagonShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .background(Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        // Outline border
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = HoneycombHexagonShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(2.dp, borderColor),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = emoji,
                    fontSize = 17.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun HexagonActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    borderColor: Color,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    sizeDp: Float = 34f,
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .shadow(3.dp, shape = HoneycombHexagonShape)
            .clip(HoneycombHexagonShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = HoneycombHexagonShape,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, borderColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}
