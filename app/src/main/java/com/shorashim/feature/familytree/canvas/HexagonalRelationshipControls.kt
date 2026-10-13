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
 * It is drawn in **screen space** (not inside the zoomed map layer) so the hexagons keep a comfortable touch size
 * at every zoom level: their size is a fraction of the viewport width (clamped to 64-104dp), and they are anchored
 * to the card's current on-screen rectangle and nudged back inside the viewport if they would be clipped.
 *
 * Positions: top = parents, right = sibling, left = spouse, bottom = child; edit and delete sit right under the card.
 */
@Composable
fun HexagonalRelationshipControls(
    node: TreeNode,
    scale: Float,
    mapOffset: androidx.compose.ui.geometry.Offset,
    d: Float, // screen density factor
    shiftX: Float,
    shiftY: Float,
    viewportW: Float,
    viewportH: Float,
    onAddChildClick: (Person) -> Unit,
    onAddParentClick: (Person) -> Unit,
    onAddSpouseClick: (Person) -> Unit,
    onAddSiblingClick: (Person) -> Unit,
    onEditPersonClick: (Person) -> Unit,
    onDeletePersonClick: (Person) -> Unit,
    modifier: Modifier = Modifier,
) {
    val person = node.person

    // Card rectangle on screen, in pixels.
    val cardW = node.width * d * scale
    val cardH = node.height * d * scale
    val cardLeft = mapOffset.x + (node.x + shiftX) * d * scale
    val cardTop = mapOffset.y + (node.y + shiftY) * d * scale
    val cardRight = cardLeft + cardW
    val cardBottom = cardTop + cardH
    val cx = cardLeft + cardW / 2f
    val cy = cardTop + cardH / 2f

    // Touch targets relative to the viewport (never tiny, never huge).
    val bigW = (viewportW * 0.22f).coerceIn(64f * d, 104f * d)
    val bigH = bigW * 1.1f
    val smallW = bigW * 0.62f
    val smallH = smallW * 1.1f
    val gap = 6f * d
    val edge = 4f * d
    val top = 12f * d

    val card = Box4(cardLeft, cardTop, cardRight, cardBottom).inflate(2f * d)
    val placed = mutableListOf<Box4>()

    // Choose the first candidate that is fully on screen and overlaps neither the card nor an earlier control;
    // when nothing fits, keep the preferred spot clamped into the viewport.
    fun place(w: Float, h: Float, candidates: List<Pair<Float, Float>>): Pair<Float, Float> {
        val chosen = candidates.firstOrNull { (x, y) ->
            val r = Box4(x, y, x + w, y + h)
            x >= edge && y >= top && r.right <= viewportW - edge && r.bottom <= viewportH - edge &&
                !r.intersects(card) && placed.none { it.intersects(r) }
        } ?: candidates.first().let { (x, y) ->
            x.coerceIn(edge, maxOf(edge, viewportW - w - edge)) to y.coerceIn(top, maxOf(top, viewportH - h - edge))
        }
        placed.add(Box4(chosen.first, chosen.second, chosen.first + w, chosen.second + h))
        return chosen
    }

    // Candidate anchors around the card (top-left corner of the hexagon), most natural first.
    val above = cardTop - gap - bigH
    val below = cardBottom + gap
    val leftX = cardLeft - gap - bigW
    val rightX = cardRight + gap
    val midY = cy - bigH / 2f
    val cxBig = cx - bigW / 2f

    // 1. TOP: parents
    val parents = place(bigW, bigH, listOf(cxBig to above, (cardLeft - bigW * 0.3f) to above, (cardRight - bigW * 0.7f) to above))
    // 2. BOTTOM: child
    val child = place(bigW, bigH, listOf(cxBig to below, (cardLeft - bigW * 0.3f) to below, (cardRight - bigW * 0.7f) to below))
    // 3. RIGHT: sibling
    val sibling = place(bigW, bigH, listOf(
        rightX to midY,
        (rightX + bigW + gap) to midY,
        (cardRight - bigW * 0.7f) to above,
        (cardRight - bigW * 0.7f) to below,
        leftX to midY,
    ))
    // 4. LEFT: spouse
    val spouse = place(bigW, bigH, listOf(
        leftX to midY,
        (leftX - bigW - gap) to midY,
        (cardLeft - bigW * 0.3f) to above,
        (cardLeft - bigW * 0.3f) to below,
        rightX to midY,
        (rightX + bigW + gap) to midY,
    ))
    // 5. Edit & delete: small hexagons flanking the parents hexagon (top row), so they never fight with the sides
    val rowY = parents.second + (bigH - smallH) / 2f
    val edit = place(smallW, smallH, listOf(
        (parents.first - smallW - gap) to rowY,
        (parents.first - smallW - gap) to (below + bigH + gap),
    ))
    val delete = place(smallW, smallH, listOf(
        (parents.first + bigW + gap) to rowY,
        (parents.first + bigW + gap) to (below + bigH + gap),
    ))

    HexagonRelationshipButton(
        emoji = "👪",
        label = "הורים",
        borderColor = MaterialTheme.colorScheme.primary,
        onClick = { onAddParentClick(person) },
        modifier = Modifier.absoluteOffset { IntOffset(parents.first.roundToInt(), parents.second.roundToInt()) },
        widthPx = bigW,
        heightPx = bigH,
        densityFactor = d,
    )
    HexagonRelationshipButton(
        emoji = "👶",
        label = "ילד/ילדה",
        borderColor = MaterialTheme.colorScheme.primary,
        onClick = { onAddChildClick(person) },
        modifier = Modifier.absoluteOffset { IntOffset(child.first.roundToInt(), child.second.roundToInt()) },
        widthPx = bigW,
        heightPx = bigH,
        densityFactor = d,
    )
    HexagonRelationshipButton(
        emoji = "🤝",
        label = "אח/אחות",
        borderColor = MaterialTheme.colorScheme.tertiary,
        onClick = { onAddSiblingClick(person) },
        modifier = Modifier.absoluteOffset { IntOffset(sibling.first.roundToInt(), sibling.second.roundToInt()) },
        widthPx = bigW,
        heightPx = bigH,
        densityFactor = d,
    )
    HexagonRelationshipButton(
        emoji = "💍",
        label = "בן/בת זוג",
        borderColor = Color(0xFFC48B47),
        onClick = { onAddSpouseClick(person) },
        modifier = Modifier.absoluteOffset { IntOffset(spouse.first.roundToInt(), spouse.second.roundToInt()) },
        widthPx = bigW,
        heightPx = bigH,
        densityFactor = d,
    )
    HexagonActionButton(
        icon = Icons.Outlined.Edit,
        iconTint = MaterialTheme.colorScheme.primary,
        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
        onClick = { onEditPersonClick(person) },
        contentDescription = "ערוך פרטי אדם",
        modifier = Modifier.absoluteOffset { IntOffset(edit.first.roundToInt(), edit.second.roundToInt()) },
        widthPx = smallW,
        heightPx = smallH,
    )
    HexagonActionButton(
        icon = Icons.Outlined.Delete,
        iconTint = MaterialTheme.colorScheme.error,
        borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
        onClick = { onDeletePersonClick(person) },
        contentDescription = "מחק אדם מהעץ",
        modifier = Modifier.absoluteOffset { IntOffset(delete.first.roundToInt(), delete.second.roundToInt()) },
        widthPx = smallW,
        heightPx = smallH,
    )
}

/** Axis-aligned screen rectangle in pixels, used to keep the controls from covering each other or the card. */
private data class Box4(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun inflate(by: Float) = Box4(left - by, top - by, right + by, bottom + by)
    fun intersects(o: Box4) = left < o.right && o.left < right && top < o.bottom && o.top < bottom
}

@Composable
private fun HexagonRelationshipButton(
    emoji: String,
    label: String,
    borderColor: Color,
    onClick: () -> Unit,
    widthPx: Float,
    heightPx: Float,
    densityFactor: Float,
    modifier: Modifier = Modifier,
) {
    val widthDp = (widthPx / densityFactor).dp
    val heightDp = (heightPx / densityFactor).dp
    val emojiSp = (widthPx / densityFactor * 0.30f).sp
    val labelSp = (widthPx / densityFactor * 0.15f).coerceAtLeast(11f).sp
    Surface(
        modifier = modifier
            .size(width = widthDp, height = heightDp)
            .shadow(6.dp, shape = HoneycombHexagonShape)
            .clip(HoneycombHexagonShape)
            .clickable(onClick = onClick),
        shape = HoneycombHexagonShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.5.dp, borderColor),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = emoji, fontSize = emojiSp, lineHeight = emojiSp * 1.05f)
            Spacer(Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = labelSp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = labelSp * 1.05f,
                maxLines = 1,
            )
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
    widthPx: Float,
    heightPx: Float,
    modifier: Modifier = Modifier,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val widthDp = (widthPx / density).dp
    val heightDp = (heightPx / density).dp
    Surface(
        modifier = modifier
            .size(width = widthDp, height = heightDp)
            .shadow(4.dp, shape = HoneycombHexagonShape)
            .clip(HoneycombHexagonShape)
            .clickable(onClick = onClick),
        shape = HoneycombHexagonShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, borderColor),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(widthDp * 0.42f),
            )
        }
    }
}
