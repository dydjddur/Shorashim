package com.shorashim.feature.donations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shorashim.core.ui.component.IconTile
import com.shorashim.core.ui.component.SectionLabel
import com.shorashim.core.ui.component.ShorashimCard
import com.shorashim.core.ui.component.Spacing
import com.shorashim.core.ui.theme.ShorashimTheme

/** Bit payment page for donations. Opens in the Bit app when installed, otherwise in the browser. */
private const val BIT_DONATION_URL = "https://www.bitpay.co.il/app/me/C546426F-239C-7AE9-D648-27BB470F73865811"

@Composable
fun DonationsScreen(
    modifier: Modifier = Modifier,
) {
    val colors = ShorashimTheme.colors
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
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

        SectionLabel(text = "תרומה ב-Bit")
        ShorashimCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(icon = Icons.Outlined.PhoneIphone)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Bit",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "הסכום וההקדשה נבחרים בעמוד התשלום של Bit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BIT_DONATION_URL))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { context.startActivity(intent) }
                        .onFailure { android.util.Log.e("Donations", "Could not open Bit link", it) }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) {
                Text(text = "לתרומה ב-Bit", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
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
