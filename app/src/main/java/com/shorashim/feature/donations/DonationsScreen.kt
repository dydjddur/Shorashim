package com.shorashim.feature.donations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shorashim.core.ui.component.IconTile
import com.shorashim.core.ui.component.SectionLabel
import com.shorashim.core.ui.component.ShorashimCard
import com.shorashim.core.ui.component.ShorashimDialog
import com.shorashim.core.ui.component.Spacing
import com.shorashim.core.ui.theme.ShorashimTheme

private const val MAX_CUSTOM_AMOUNT_DIGITS = 6

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DonationsScreen(
    modifier: Modifier = Modifier,
) {
    val colors = ShorashimTheme.colors
    // Saveable so a rotation / process recreation does not silently reset the form.
    var selectedAmount by rememberSaveable { mutableIntStateOf(36) }
    var customAmountText by rememberSaveable { mutableStateOf("") }
    var dedicationText by rememberSaveable { mutableStateOf("") }
    var selectedMethod by rememberSaveable { mutableStateOf("bit") }
    var showThankYouDialog by rememberSaveable { mutableStateOf(false) }

    val amounts = listOf(18, 36, 72, 100, 180)

    val finalAmount = if (selectedAmount > 0) selectedAmount else customAmountText.toIntOrNull() ?: 0

    if (showThankYouDialog) {
        ShorashimDialog(
            onDismissRequest = { showThankYouDialog = false },
            icon = Icons.Outlined.Favorite,
            iconTint = colors.clay,
            iconBackground = colors.clayContainer,
            title = "תודה מקרב לב על תרומתך!",
            subtitle = "תרומתך בסך $finalAmount ₪ התקבלה בהצלחה",
            confirmButton = {
                Button(
                    onClick = { showThankYouDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text(text = "תזכו למצוות, תודה!")
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
                )
                if (dedicationText.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.goldContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "הקדשה: ${dedicationText.trim()}",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            textAlign = TextAlign.Center,
                            color = colors.onGoldContainer,
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Hero
        ShorashimCard(
            containerColor = colors.goldContainer.copy(alpha = 0.35f),
            borderColor = colors.gold.copy(alpha = 0.35f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconTile(
                    icon = Icons.Outlined.VolunteerActivism,
                    tint = colors.gold,
                    container = colors.goldContainer,
                    size = 64.dp,
                    iconSize = 32.dp,
                    cornerRadius = 20.dp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "עוזרים לשורשים לצמוח",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "'שורשים' הוא מיזם קהילתי חינמי לשימור מורשת המשפחה, שושלת הדורות, אזכרות וקריאת תהילים. תרומתכם מאפשרת לנו להחזיק שרתים מאובטחים ולהמשיך לפתח.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SectionLabel(text = "סכום התרומה (ש״ח)")
        ShorashimCard {
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
                                    18 -> " · חי"
                                    36 -> " · פעמיים חי"
                                    180 -> " · שותף זהב"
                                    else -> ""
                                },
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = amountChipColors(),
                    )
                }
                FilterChip(
                    selected = selectedAmount == 0,
                    onClick = { selectedAmount = 0 },
                    label = { Text("סכום אחר") },
                    shape = RoundedCornerShape(12.dp),
                    colors = amountChipColors(),
                )
            }

            if (selectedAmount == 0) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { input ->
                        customAmountText = input.filter { c -> c.isDigit() }.take(MAX_CUSTOM_AMOUNT_DIGITS)
                    },
                    label = { Text("סכום בש״ח") },
                    suffix = { Text("₪") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                )
            }
        }

        SectionLabel(text = "הקדשה (אופציונלי)")
        ShorashimCard {
            Text(
                text = "ניתן להקדיש את התרומה לעילוי נשמת, לרפואת קרוב או להצלחה.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = dedicationText,
                onValueChange = { dedicationText = it },
                placeholder = { Text("לדוגמה: לעילוי נשמת ישראל בן שרה ז״ל") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
            )
        }

        SectionLabel(text = "אמצעי תשלום")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PaymentMethodButton(
                name = "Bit",
                icon = Icons.Outlined.PhoneIphone,
                isSelected = selectedMethod == "bit",
                onClick = { selectedMethod = "bit" },
                modifier = Modifier.weight(1f),
            )
            PaymentMethodButton(
                name = "PayBox",
                icon = Icons.Outlined.Wallet,
                isSelected = selectedMethod == "paybox",
                onClick = { selectedMethod = "paybox" },
                modifier = Modifier.weight(1f),
            )
            PaymentMethodButton(
                name = "אשראי",
                icon = Icons.Outlined.CreditCard,
                isSelected = selectedMethod == "credit",
                onClick = { selectedMethod = "credit" },
                modifier = Modifier.weight(1f),
            )
            PaymentMethodButton(
                name = "PayPal",
                icon = Icons.Outlined.AccountBalanceWallet,
                isSelected = selectedMethod == "paypal",
                onClick = { selectedMethod = "paypal" },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = { showThankYouDialog = true },
            enabled = finalAmount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (finalAmount > 0) "לתרומה מאובטחת בסך $finalAmount ₪" else "בחרו סכום לתרומה",
                style = MaterialTheme.typography.titleSmall,
            )
        }

        ShorashimCard(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            borderColor = MaterialTheme.colorScheme.surfaceVariant,
            contentPadding = PaddingValues(14.dp),
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "שקיפות ומטרה",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "כל התרומות מיועדות במלואן לכיסוי עלויות אחסון ענן, אבטחת מידע ופיתוח תכונות חופשיות לכל המשפחות.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
    }
}

@Composable
private fun amountChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = MaterialTheme.colorScheme.surface,
    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
)

@Composable
private fun PaymentMethodButton(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) scheme.primaryContainer else scheme.surface,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) scheme.primary else scheme.outlineVariant,
        ),
        modifier = modifier.height(72.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) scheme.primary else scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) scheme.onPrimaryContainer else scheme.onSurface,
                maxLines = 1,
            )
        }
    }
}
