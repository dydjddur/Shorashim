package com.shorashim.feature.donations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.VolunteerActivism
import com.shorashim.core.ui.component.ShorashimDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.core.ui.component.Spacing

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonationsScreen(
    modifier: Modifier = Modifier,
) {
    var selectedAmount by remember { mutableIntStateOf(36) }
    var customAmountText by remember { mutableStateOf("") }
    var dedicationText by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf("bit") }
    var showThankYouDialog by remember { mutableStateOf(false) }

    val amounts = listOf(18, 36, 72, 100, 180)

    val finalAmount = if (selectedAmount > 0) selectedAmount else customAmountText.toIntOrNull() ?: 0

    if (showThankYouDialog) {
        ShorashimDialog(
            onDismissRequest = { showThankYouDialog = false },
            icon = Icons.Outlined.Favorite,
            iconTint = Color(0xFFE91E63),
            iconBackground = Color(0xFFE91E63).copy(alpha = 0.14f),
            title = "תודה מקרב לב על תרומתך! ❤️",
            subtitle = "תרומתך בסך $finalAmount ₪ התקבלה בהצלחה",
            confirmButton = {
                Button(
                    onClick = { showThankYouDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        text = "תזכו למצוות, תודה!",
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "בזכותך נוכל להמשיך לתחזק את שרתי הענן, לשמור על עצי המשפחה ולפתח תכונות חדשות למען כל עם ישראל. תבורכו מפי עליון!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )
                if (dedicationText.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "הקדשה: $dedicationText",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.5.dp, Color(0xFFD4AF37).copy(alpha = 0.6f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFD4AF37).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.VolunteerActivism,
                        contentDescription = null,
                        tint = Color(0xFFD4AF37),
                        modifier = Modifier.size(38.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "עוזרים לשורשים לצמוח 🌱",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "אפליקציית 'שורשים' הינה מיזם קהילתי חינמי לשימור מורשת המשפחה, שושלת הדורות, אזכרות וקריאת תהילים. תרומתכם מאפשרת לנו להמשיך להחזיק שרתים מאובטחים ולפתח את האפליקציה.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
            }
        }

        // Amount Selection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "בחר סכום תרומה (ש״ח):",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(10.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    amounts.forEach { amount ->
                        val isSelected = selectedAmount == amount
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedAmount = amount
                                customAmountText = ""
                            },
                            label = {
                                Text(
                                    text = "$amount ₪" + when (amount) {
                                        18 -> " (חי)"
                                        36 -> " (פעמיים חי)"
                                        180 -> " (שותף זהב)"
                                        else -> ""
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            ),
                        )
                    }

                    // Custom Amount Chip
                    FilterChip(
                        selected = selectedAmount == 0,
                        onClick = { selectedAmount = 0 },
                        label = { Text("סכום אחר", fontWeight = if (selectedAmount == 0) FontWeight.Bold else FontWeight.Normal) },
                    )
                }

                if (selectedAmount == 0) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { customAmountText = it.filter { c -> c.isDigit() } },
                        label = { Text("הזן סכום תרומה בש״ח") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                    )
                }
            }
        }

        // Dedication Card (Optional)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "הקדשה (אופציונלי):",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "ניתן להקדיש את התרומה לעילוי נשמת, לרפואת קרוב או להצלחה:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = dedicationText,
                    onValueChange = { dedicationText = it },
                    placeholder = { Text("לדוגמה: לעילוי נשמת ישראל בן שרה ז״ל") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                )
            }
        }

        // Payment Method Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "אמצעי תשלום:",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary,
                )

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PaymentMethodButton(
                        name = "Bit",
                        icon = "💙",
                        isSelected = selectedMethod == "bit",
                        onClick = { selectedMethod = "bit" },
                        modifier = Modifier.weight(1f),
                    )
                    PaymentMethodButton(
                        name = "PayBox",
                        icon = "📦",
                        isSelected = selectedMethod == "paybox",
                        onClick = { selectedMethod = "paybox" },
                        modifier = Modifier.weight(1f),
                    )
                    PaymentMethodButton(
                        name = "אשראי",
                        icon = "💳",
                        isSelected = selectedMethod == "credit",
                        onClick = { selectedMethod = "credit" },
                        modifier = Modifier.weight(1f),
                    )
                    PaymentMethodButton(
                        name = "PayPal",
                        icon = "🅿️",
                        isSelected = selectedMethod == "paypal",
                        onClick = { selectedMethod = "paypal" },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Submit Button
        Button(
            onClick = { showThankYouDialog = true },
            enabled = finalAmount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "לתרומה מאובטחת בסך $finalAmount ₪",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }

        // Transparency Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🛡️ שקיפות ומטרה",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "כל התרומות מיועדות במלואן לכיסוי עלויות אחסון ענן (Supabase/Vercel), אבטחת מידע, ופיתוח תכונות חופשיות לכל המשפחות בישראל.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun PaymentMethodButton(
    name: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
        ),
        modifier = modifier.height(60.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp),
        ) {
            Text(text = icon, fontSize = 18.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
