package com.shorashim.feature.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shorashim.R
import com.shorashim.core.ui.component.IconTile
import com.shorashim.core.ui.component.SectionLabel
import com.shorashim.core.ui.component.ShorashimCard
import com.shorashim.core.ui.component.Spacing
import com.shorashim.core.ui.theme.ShorashimTheme

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
) {
    val colors = ShorashimTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "מרכז עזרה",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "מדריכים, שאלות נפוצות ומידע על פעילות האפליקציה",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(4.dp))
        SectionLabel(text = "מידע כללי")

        HelpInfoCard(
            icon = Icons.Outlined.Park,
            title = stringResource(R.string.settings_about_title),
            body = stringResource(R.string.settings_about_body),
        )

        HelpInfoCard(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.settings_privacy_title),
            body = "אפליקציית 'שורשים' מעניקה לכם שליטה מלאה בפרטיות המידע המשפחתי:\n\n" +
                "• עץ פרטי: מוסתר לחלוטין מלשונית הקהילה. רק יוצר העץ ומשתמשים שהוזמנו במפורש יכולים לצפות בו.\n" +
                "• עץ קהילתי: מוצג בלשונית הקהילה לצפייה בלבד (ללא עריכה), כדי לאפשר לאנשים נוספים לאתר קרובי משפחה לפי שמות משפחה משותפים.\n" +
                "• מצב פרטי בפרופיל: מסתיר את כל עצי המשפחה שלך ואת רשימת החברים שלך ממשתמשים אחרים באפליקציה.",
            tint = colors.info,
            container = colors.infoContainer,
        )

        HelpInfoCard(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.settings_notifications_title),
            body = stringResource(R.string.settings_notifications_body),
            tint = colors.gold,
            container = colors.goldContainer,
        )

        Spacer(Modifier.height(4.dp))
        SectionLabel(text = "שאלות נפוצות")

        FaqCard(
            question = "כיצד מוסיפים אנשים לעץ המשפחה?",
            answer = "בלשונית 'עץ משפחה', לחצו על כפתור '+' או פתחו את העץ שלכם. במפת השושלת לחצו על '+ הוספת אדם' להוספת אדם ראשון, או לחצו על כרטיס של בן משפחה קיים כדי להוסיף לו הורה, בן זוג או ילד.",
        )
        FaqCard(
            question = "כיצד להזמין בני משפחה לצפות ולערוך ביחד?",
            answer = "במסך ניהול העץ, לחצו על 'חברים וניהול' ולאחר מכן 'הזמן בן משפחה'. תוכלו לשלוח הזמנה לפי שם משתמש באפליקציה, או להעתיק קישור הזמנה ישיר ולשלוח בוואטסאפ או במייל.",
        )
        FaqCard(
            question = "איך עובד איתור קרובים בעצי קהילה?",
            answer = "בלשונית 'קהילה' מוצגים עצי משפחה שפורסמו על ידי חברי הקהילה. תוכלו לחפש שמות משפחה, לסנן לפי שמות המשפחה שלכם, ולפתוח את מפת השושלת כדי לבדוק אם אתם קשורים לאותו עץ.",
        )

        Spacer(Modifier.height(4.dp))
        ShorashimCard(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            borderColor = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(
                    icon = Icons.Outlined.Mail,
                    container = MaterialTheme.colorScheme.surface,
                    size = 40.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = "זקוקים לעזרה נוספת?",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "צוות התמיכה של שורשים זמין לשאלות, הצעות לשיפור ותמיכה טכנית.",
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
private fun HelpInfoCard(
    icon: ImageVector,
    title: String,
    body: String,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    container: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primaryContainer,
) {
    ShorashimCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = icon, tint = tint, container = container, size = 40.dp)
            Spacer(Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FaqCard(question: String, answer: String) {
    var expanded by rememberSaveable(question) { mutableStateOf(false) }
    val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "faqChevron")
    ShorashimCard(
        onClick = { expanded = !expanded },
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = question,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "הסתר תשובה" else "הצג תשובה",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(rotation),
            )
        }
        AnimatedVisibility(visible = expanded) {
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, start = 32.dp),
            )
        }
    }
}
