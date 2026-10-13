package com.shorashim.feature.familytree.canvas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.R
import com.shorashim.core.model.Family
import com.shorashim.core.model.Person
import com.shorashim.core.model.Relationship
import com.shorashim.core.model.RelationshipType
import kotlin.math.abs
import kotlin.math.roundToInt

private const val MIN_SCALE = 0.12f
private const val MAX_SCALE = 2.5f
private const val CONTENT_PADDING = 40f
private const val CORNER_RADIUS = 18f
private const val BADGE_SIZE = 28f

/** Pins children to the physical top-left corner regardless of layout direction (the app is RTL). */
private val TopLeftAbsolute = Alignment { _, _, _ -> IntOffset.Zero }

/**
 * Static family tree map. The layout is computed automatically by [TreeLayoutEngine]; the user can
 * pan and zoom the whole map (pinch / drag / buttons) but cannot move individual cards.
 */
@Composable
fun FamilyTreeCanvas(
    family: Family,
    people: List<Person>,
    relationships: List<Relationship>,
    onAddChildClick: (Person) -> Unit,
    onAddParentClick: (Person) -> Unit,
    onAddSpouseClick: (Person) -> Unit,
    onAddSiblingClick: (Person) -> Unit = {},
    onEditPersonClick: (Person) -> Unit,
    onDeletePersonClick: (Person) -> Unit,
    onPersonDetailsClick: ((Person) -> Unit)? = null,
    onPublishClick: () -> Unit,
    onEmptyAddPersonClick: () -> Unit,
    onRefreshClick: (() -> Unit)? = null,
    isRefreshing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var hasInitialView by remember { mutableStateOf(false) }
    var selectedPersonId by remember { mutableStateOf<String?>(null) }
    // Hexagon controls appear once the map has finished centring the selected card.
    var controlsReady by remember { mutableStateOf(false) }

    val (nodes, connections) = remember(people, relationships) {
        TreeLayoutEngine.calculateLayout(people, relationships)
    }
    val bounds = remember(nodes) { TreeLayoutEngine.contentBounds(nodes) }
    val markers = remember(connections) { TreeLayoutEngine.connectionMarkers(connections) }

    // Canvas coordinates are normalised so that the content starts at (CONTENT_PADDING, CONTENT_PADDING);
    // everything stays in positive space, which keeps hit-testing of the cards reliable.
    val shiftX = CONTENT_PADDING - bounds.left
    val shiftY = CONTENT_PADDING - bounds.top
    val contentWidth = bounds.width + CONTENT_PADDING * 2
    val contentHeight = bounds.height + CONTENT_PADDING * 2

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val viewportW = constraints.maxWidth.toFloat()
        val viewportH = constraints.maxHeight.toFloat()
        val density = LocalDensity.current
        // Layout units are dp (the cards are sized in dp); gestures and offsets work in pixels.
        val d = density.density
        val contentWidthPx = contentWidth * d
        val contentHeightPx = contentHeight * d

        fun clampOffset(candidate: Offset, atScale: Float): Offset {
            val margin = 80f
            val minX = margin - contentWidthPx * atScale
            val maxX = viewportW - margin
            val minY = margin - contentHeightPx * atScale
            val maxY = viewportH - margin
            return Offset(
                candidate.x.coerceIn(minOf(minX, maxX), maxOf(minX, maxX)),
                candidate.y.coerceIn(minOf(minY, maxY), maxOf(minY, maxY)),
            )
        }

        fun fitToScreen() {
            if (nodes.isEmpty() || viewportW <= 0f || viewportH <= 0f) return
            val fit = minOf(viewportW / contentWidthPx, viewportH / contentHeightPx)
            val s = fit.coerceIn(MIN_SCALE, 1.2f)
            scale = s
            offset = Offset(
                (viewportW - contentWidthPx * s) / 2f,
                ((viewportH - contentHeightPx * s) / 2f).coerceAtLeast(0f),
            )
        }

        fun zoomBy(factor: Float, focus: Offset) {
            val newScale = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
            val ratio = newScale / scale
            offset = clampOffset(focus - (focus - offset) * ratio, newScale)
            scale = newScale
        }

        // Initial fit to screen on first view only. Preserves zoom level and scroll/pan position on realtime updates!
        LaunchedEffect(nodes.isNotEmpty(), viewportW, viewportH) {
            if (!hasInitialView && nodes.isNotEmpty() && viewportW > 0f) {
                fitToScreen()
                hasInitialView = true
            }
        }

        // Selecting a card smoothly centres it (and shrinks the map if the card is huge) so there is always room
        // around it for the big hexagon controls, whatever the zoom level or position was.
        LaunchedEffect(selectedPersonId) {
            controlsReady = false
            val node = nodes.find { it.person.id == selectedPersonId } ?: return@LaunchedEffect
            if (viewportW <= 0f) return@LaunchedEffect
            val maxScale = (viewportW * 0.5f) / (node.width * d)
            val targetScale = minOf(scale, maxScale).coerceAtLeast(MIN_SCALE)
            val cardCx = (node.x + shiftX + node.width / 2f) * d
            val cardCy = (node.y + shiftY + node.height / 2f) * d
            val target = Offset(viewportW / 2f - cardCx * targetScale, viewportH * 0.46f - cardCy * targetScale)
            val startScale = scale
            val startOffset = offset
            animate(0f, 1f, animationSpec = tween(durationMillis = 260)) { t, _ ->
                scale = startScale + (targetScale - startScale) * t
                offset = Offset(
                    startOffset.x + (target.x - startOffset.x) * t,
                    startOffset.y + (target.y - startOffset.y) * t,
                )
            }
            controlsReady = true
        }

        if (people.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "🌱",
                            fontSize = 44.sp,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (family.canEditTreeContent) {
                                stringResource(R.string.tree_empty_add_first)
                            } else {
                                stringResource(R.string.community_tree_empty)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                        if (family.canEditTreeContent) {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = onEmptyAddPersonClick,
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.family_people_add))
                            }
                        }
                    }
                }
            }
        } else {
            val connectorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
            val spouseColor = Color(0xFFC48B47)

            // Pan & zoom surface. Cards cannot be dragged: only the whole map moves.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { selectedPersonId = null })
                    }
                    .pointerInput(contentWidth, contentHeight, viewportW, viewportH) {
                        detectTransformGestures { centroid, pan, zoom, _ ->
                            if (pan.getDistance() > 4f || zoom != 1f) {
                                selectedPersonId = null
                            }
                            val newScale = (scale * zoom).coerceIn(MIN_SCALE, MAX_SCALE)
                            val ratio = newScale / scale
                            val zoomed = centroid - (centroid - offset) * ratio
                            offset = clampOffset(zoomed + pan, newScale)
                            scale = newScale
                        }
                    },
            ) {
                Box(
                    modifier = Modifier
                        .wrapContentSize(align = TopLeftAbsolute, unbounded = true)
                        .requiredSize(
                            width = contentWidth.dp,
                            height = contentHeight.dp,
                        )
                        .graphicsLayer {
                            transformOrigin = TransformOrigin(0f, 0f)
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentAlignment = TopLeftAbsolute,
                ) {
                    // Connection lines
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val parentStroke = Stroke(
                            width = 2.5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        )
                        val spouseStroke = Stroke(
                            width = 2.5f,
                            cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                        )
                        val shift = Offset(shiftX, shiftY)

                        withTransform({ scale(d, d, Offset.Zero) }) {
                        for (conn in connections) {
                            val start = conn.start + shift
                            val end = conn.end + shift
                            when (conn.type) {
                                RelationshipType.PARENT_CHILD -> {
                                    drawPath(
                                        path = elbowPath(start, end),
                                        color = connectorColor,
                                        style = parentStroke,
                                    )
                                    drawCircle(
                                        color = connectorColor,
                                        radius = 3.5f,
                                        center = end,
                                    )
                                }
                                RelationshipType.SPOUSE -> {
                                    val path = Path().apply {
                                        moveTo(start.x, start.y)
                                        lineTo(end.x, end.y)
                                    }
                                    drawPath(path = path, color = spouseColor, style = spouseStroke)
                                }
                            }
                        }
                        }
                    }

                    // Relationship emoji in the middle of every connector
                    for (marker in markers) {
                        ConnectorBadge(
                            kind = marker.kind,
                            modifier = Modifier.absoluteOffset {
                                val half = (BADGE_SIZE / 2f) * d
                                IntOffset(
                                    x = ((marker.position.x + shiftX) * d - half).roundToInt(),
                                    y = ((marker.position.y + shiftY) * d - half).roundToInt(),
                                )
                            },
                        )
                    }

                    // Person cards
                    for (node in nodes) {
                        FamilyTreeNodeCard(
                            node = node,
                            canEdit = family.canEditTreeContent,
                            isSelected = (selectedPersonId == node.person.id),
                            onCardClick = { person ->
                                if (family.canEditTreeContent) {
                                    selectedPersonId = if (selectedPersonId == person.id) null else person.id
                                } else {
                                    onPersonDetailsClick?.invoke(person) ?: onEditPersonClick(person)
                                }
                            },
                            modifier = Modifier.absoluteOffset {
                                IntOffset(
                                    x = ((node.x + shiftX) * d).roundToInt(),
                                    y = ((node.y + shiftY) * d).roundToInt(),
                                )
                            },
                        )
                    }
                }
            }
        }

        // Honeycomb hexagonal relationship & action controls for the selected member. Drawn in screen space so
        // the touch targets stay comfortable at every zoom level.
        val selectedNode = if (selectedPersonId != null && family.canEditTreeContent) {
            nodes.find { it.person.id == selectedPersonId }
        } else null
        if (selectedNode != null && controlsReady) {
            // Absolute top-left origin: the app is RTL, so the default Start alignment would anchor at the right edge.
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = TopLeftAbsolute) {
            HexagonalRelationshipControls(
                node = selectedNode,
                scale = scale,
                mapOffset = offset,
                d = d,
                shiftX = shiftX,
                shiftY = shiftY,
                viewportW = viewportW,
                viewportH = viewportH,
                onAddChildClick = { p ->
                    selectedPersonId = null
                    onAddChildClick(p)
                },
                onAddParentClick = { p ->
                    selectedPersonId = null
                    onAddParentClick(p)
                },
                onAddSpouseClick = { p ->
                    selectedPersonId = null
                    onAddSpouseClick(p)
                },
                onAddSiblingClick = { p ->
                    selectedPersonId = null
                    onAddSiblingClick(p)
                },
                onEditPersonClick = { p ->
                    selectedPersonId = null
                    onEditPersonClick(p)
                },
                onDeletePersonClick = { p ->
                    selectedPersonId = null
                    onDeletePersonClick(p)
                },
            )
            }
        }

        // Private-tree notice & publish-to-community CTA
        if (family.isPrivate) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9A825).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFF9A825)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = "🌐", fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔒 העץ פרטי ומוסתר מהקהילה",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "לחצו על \"פרסום\" כדי שקרובי משפחה יוכלו למצוא אותו בקהילה",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (family.canManageMembers) {
                        Button(
                            onClick = onPublishClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF43A047),
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(
                                Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "פרסום",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        // Zoom controls
        if (people.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val viewportCenter = Offset(viewportW / 2f, viewportH / 2f)
                    IconButton(
                        onClick = { zoomBy(1.25f, viewportCenter) },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = stringResource(R.string.tree_zoom_in),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(
                        onClick = { fitToScreen() },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CenterFocusStrong,
                            contentDescription = stringResource(R.string.tree_reset_view),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { zoomBy(0.8f, viewportCenter) },
                        modifier = Modifier.size(38.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Remove,
                            contentDescription = stringResource(R.string.tree_zoom_out),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (onRefreshClick != null) {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(1.dp)
                                .height(20.dp)
                                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        )
                        IconButton(
                            onClick = onRefreshClick,
                            enabled = !isRefreshing,
                            modifier = Modifier.size(38.dp),
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = stringResource(R.string.tree_refresh_action),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Orthogonal "org-chart" connector with rounded corners: straight down from the parent, across on a
 * shared bus line, and straight down into the child. All children of one origin share the same bus
 * height, so a couple's children hang from one clean horizontal rail.
 */
private fun elbowPath(start: Offset, end: Offset): Path {
    val path = Path()
    path.moveTo(start.x, start.y)
    val dx = end.x - start.x
    if (abs(dx) < 1f) {
        path.lineTo(end.x, end.y)
        return path
    }
    val busY = TreeLayoutEngine.busY(start, end)
    val r = minOf(CORNER_RADIUS, abs(dx) / 2f, (busY - start.y).coerceAtLeast(1f), (end.y - busY).coerceAtLeast(1f))
    val dir = if (dx > 0) 1f else -1f

    path.lineTo(start.x, busY - r)
    path.quadraticTo(start.x, busY, start.x + dir * r, busY)
    path.lineTo(end.x - dir * r, busY)
    path.quadraticTo(end.x, busY, end.x, busY + r)
    path.lineTo(end.x, end.y)
    return path
}

/** Small round emoji chip that sits on a connector and names the relationship it represents. */
@Composable
private fun ConnectorBadge(kind: TreeLayoutEngine.MarkerKind, modifier: Modifier = Modifier) {
    val (emoji, tint) = when (kind) {
        TreeLayoutEngine.MarkerKind.PARENT_CHILD -> "👪" to MaterialTheme.colorScheme.primary
        TreeLayoutEngine.MarkerKind.SPOUSE -> "💍" to Color(0xFFC48B47)
        TreeLayoutEngine.MarkerKind.SIBLING -> "🤝" to MaterialTheme.colorScheme.tertiary
    }
    Surface(
        modifier = modifier.size(BADGE_SIZE.dp),
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, tint),
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(text = emoji, fontSize = 14.sp)
        }
    }
}
