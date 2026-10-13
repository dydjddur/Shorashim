package com.shorashim.feature.help

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Park
import androidx.compose.material.icons.outlined.QuestionAnswer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shorashim.R
import com.shorashim.core.ui.component.Spacing

@Composable
fun HelpScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.md, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        // Top Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(46.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = "מרכז עזרה ומידע",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    )
                    Text(
                        text = "מדריכים, שאלות נפוצות ומידע על פעילות האפליקציה",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            text = "מידע כללי ואודות",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )

        // 1. אודות שורשים (Moved from Settings)
        HelpInfoCard(
            icon = Icons.Outlined.Park,
            title = stringResource(R.string.settings_about_title),
            body = stringResource(R.string.settings_about_body),
        )

        // 2. פרטיות ואבטחת מידע (Moved from Settings + Expanded)
        HelpInfoCard(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.settings_privacy_title),
            body = "אפליקציית 'שורשים' מעניקה לכם שליטה מלאה בפרטיות המידע המשפחתי:\n\n" +
                "• עץ פרטי (PRIVATE): עץ שמוגדר כפרטי מוסתר לחלוטין מלשונית הקהילה. רק יוצר העץ ומשתמשים שהוזמנו במפורש יכולים לצפות בו.\n" +
                "• עץ קהילתי (PUBLISHED): עץ שמוצג בלשונית הקהילה לצפייה בלבד (ללא עריכה), במטרה לאפשר לאנשים נוספים לאתר קרובי משפחה לפי שמות משפחה משותפים.\n" +
                "• מצב פרטי בפרופיל: מסתיר את כל עצי המשפחה שלך ואת רשימת החברים שלך ממשתמשים אחרים באפליקציה.",
        )

        // 3. התראות וימי זיכרון (Moved from Settings)
        HelpInfoCard(
            icon = Icons.Outlined.Notifications,
            title = stringResource(R.string.settings_notifications_title),
            body = stringResource(R.string.settings_notifications_body),
        )

        Text(
            text = "שאלות נפוצות ומדריכים",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
        )

        // FAQ 1: יצירת עץ
        HelpInfoCard(
            icon = Icons.Outlined.QuestionAnswer,
            title = "כיצד מוסיפים אנשים לעץ המשפחה?",
            body = "בלשונית 'עץ משפחה', לחצו על כפתור '+' או פתחו את העץ שלכם. במפת השושלת לחצו על כפתור '+ הוספת אדם' להוספת אדם ראשון, או לחצו על כרטיס של בן משפחה קיים כדי להוסיף לו הורה, בן זוג או ילד.",
        )

        // FAQ 2: שיתוף והזמנת קרובים
        HelpInfoCard(
            icon = Icons.Outlined.QuestionAnswer,
            title = "כיצד להזמין בני משפחה לצפות ולערוך ביחד?",
            body = "במסך ניהול העץ, לחצו על כפתור 'חברים וניהול' ולאחר מכן 'הזמן בן משפחה'. תוכלו לשלוח הזמנה לפי שם משתמש באפליקציה, או להעתיק קישור הזמנה ישיר ולשלוח בוואטסאפ או במייל.",
        )

        // FAQ 3: עצי קהילה
        HelpInfoCard(
            icon = Icons.Outlined.QuestionAnswer,
            title = "איך עובד איתור קרובים בעצי קהילה?",
            body = "בלשונית 'קהילה' מוצגים עצי משפחה שהועלו לענן על ידי חברי הקהילה. תוכלו לחפש שמות משפחה, לסנן לפי שמות משפחה שלכם, ולפתוח את מפת השושלת כדי לבדוק האם אתם קשורים לאותו עץ משפחה.",
        )

        // Contact Support Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Mail,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "זקוקים לעזרה נוספת?",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "צוות התמיכה של שורשים זמין לשאלות, הצעות לשיפור ותמיכה טכנית.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
            )
        }
    }
}
