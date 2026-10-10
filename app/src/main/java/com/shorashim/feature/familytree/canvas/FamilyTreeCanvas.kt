package com.shorashim.feature.familytree.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.roundToInt

@Composable
fun FamilyTreeCanvas(
    family: Family,
    people: List<Person>,
    relationships: List<Relationship>,
    onAddChildClick: (Person) -> Unit,
    onAddParentClick: (Person) -> Unit,
    onAddSpouseClick: (Person) -> Unit,
    onEditPersonClick: (Person) -> Unit,
    onDeletePersonClick: (Person) -> Unit,
    onPublishClick: () -> Unit,
    onEmptyAddPersonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset(0f, 100f)) }

    val (nodes, connections) = remember(people, relationships) {
        TreeLayoutEngine.calculateLayout(people, relationships)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val centerX = constraints.maxWidth / 2f
        val centerY = constraints.maxHeight / 3f

        if (people.isEmpty()) {
            // Empty state on canvas
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
                            text = stringResource(R.string.tree_empty_add_first),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
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
        } else {
            // Interactive Pan & Zoom Area
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(0.2f, 2.5f)
                            offset += pan
                        }
                    },
            ) {
                // Background Connection Lines
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x + centerX
                            translationY = offset.y + centerY
                        },
                ) {
                    val primaryColor = Color(0xFF6C7C59) // Olive accent
                    val spouseColor = Color(0xFFC48B47) // Warm amber

                    for (conn in connections) {
                        when (conn.type) {
                            RelationshipType.PARENT_CHILD -> {
                                val path = Path().apply {
                                    moveTo(conn.start.x, conn.start.y)
                                    val midY = (conn.start.y + conn.end.y) / 2f
                                    cubicTo(
                                        conn.start.x, midY,
                                        conn.end.x, midY,
                                        conn.end.x, conn.end.y,
                                    )
                                }
                                drawPath(
                                    path = path,
                                    color = primaryColor,
                                    style = Stroke(
                                        width = 3.dp.toPx(),
                                        cap = StrokeCap.Round,
                                    ),
                                )
                                // Arrow or dot at child top
                                drawCircle(
                                    color = primaryColor,
                                    radius = 4.dp.toPx(),
                                    center = conn.end,
                                )
                            }
                            RelationshipType.SPOUSE -> {
                                val path = Path().apply {
                                    moveTo(conn.start.x, conn.start.y)
                                    lineTo(conn.end.x, conn.end.y)
                                }
                                drawPath(
                                    path = path,
                                    color = spouseColor,
                                    style = Stroke(
                                        width = 2.5.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(12f, 8f),
                                            0f,
                                        ),
                                    ),
                                )
                                // Double ring indicator in middle
                                val midX = (conn.start.x + conn.end.x) / 2f
                                val midY = (conn.start.y + conn.end.y) / 2f
                                drawCircle(
                                    color = spouseColor,
                                    radius = 5.dp.toPx(),
                                    center = Offset(midX, midY),
                                )
                            }
                        }
                    }
                }

                // Interactive Nodes Layer
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x + centerX
                            translationY = offset.y + centerY
                        },
                ) {
                    for (node in nodes) {
                        FamilyTreeNodeCard(
                            node = node,
                            canEdit = family.canEditTreeContent,
                            onAddChildClick = onAddChildClick,
                            onAddParentClick = onAddParentClick,
                            onAddSpouseClick = onAddSpouseClick,
                            onEditPersonClick = onEditPersonClick,
                            onDeletePersonClick = onDeletePersonClick,
                            modifier = Modifier.offset {
                                IntOffset(
                                    x = node.x.roundToInt(),
                                    y = node.y.roundToInt(),
                                )
                            },
                        )
                    }
                }
            }
        }

        // Top Status Banner: Draft Notice & Publish CTA
        if (family.isDraft) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF9A825).copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF9A825)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "📝",
                        fontSize = 18.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "עץ זה נמצא במצב טיוטה",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "לחצו להעלאה כדי לשתף עם המשפחה",
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
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                horizontal = 12.dp,
                                vertical = 6.dp,
                            ),
                        ) {
                            Icon(
                                Icons.Outlined.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "העלאה",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        // Floating Zoom Controls (Overlay at Bottom Left / Bottom Start)
        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant,
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { scale = (scale + 0.2f).coerceAtMost(2.5f) },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = stringResource(R.string.tree_zoom_in),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }

                IconButton(
                    onClick = {
                        scale = 1f
                        offset = Offset(0f, 100f)
                    },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CenterFocusStrong,
                        contentDescription = stringResource(R.string.tree_reset_view),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                IconButton(
                    onClick = { scale = (scale - 0.2f).coerceAtLeast(0.25f) },
                    modifier = Modifier.size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Remove,
                        contentDescription = stringResource(R.string.tree_zoom_out),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
