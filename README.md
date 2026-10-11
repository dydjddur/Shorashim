# שורשים (Shorashim) 🌳

אפליקציית Android לניהול **עצי משפחה דינמיים**, שימור מורשת והנצחה יהודית.
Kotlin · Jetpack Compose · Material 3 · Supabase (Auth + Postgres/RLS) · ממשק עברי מלא (RTL).

> **למודל/מפתח שממשיך את הפרויקט:** התחילו מ-[מפת הקוד](#-מפת-הקוד-למי-שממשיך), קראו את
> [החלטות העיצוב הקריטיות](#-החלטות-עיצוב-קריטיות) ואת [בעיות ידועות ומשימות המשך](#-בעיות-ידועות-ומשימות-המשך).
> מסמכי עומק: [ARCHITECTURE.md](ARCHITECTURE.md) · [PROGRESS.md](PROGRESS.md) · [PROJECT_PLAN.md](PROJECT_PLAN.md) · [SUPABASE_SETUP.md](SUPABASE_SETUP.md).

---

## 🌟 יכולות

| תחום | מה יש |
|---|---|
| **עצי משפחה** | מספר עצים למשתמש, מחזור חיים טיוטה → פורסם, הרשאות לפי תפקיד (Owner / Admin / Contributor / Viewer), הזמנות בקישור עמוק (App Link) + קבלה בתוך האפליקציה |
| **מפת עץ** | מפה **סטטית** עם סידור אוטומטי חכם (ללא חפיפות), זום וגלילה בלבד — אין גרירת כרטיסים. הוספת הורה / ילד / בן-בת זוג מכל כרטיס |
| **אירועים** | תצוגת **לוח שנה חודשי** (ברירת מחדל) או **רשימה**. ימי הולדת (לחיים) ואזכרות (לנפטרים) מכל העצים, תאריך עברי בכל תא, חיפוש יום בלחיצה, החלקה בין חודשים |
| **תזכורות** | התראות מקומיות ב-09:00 (שעון ישראל): אזכרה 7 ימים מראש, יום הולדת ביום עצמו |
| **תהילים** | כל 150 הפרקים מנוקדים, אופליין; נוסח "לעילוי נשמה" עם הקדשה לנפטר |
| **חשבון** | הרשמה / התחברות / איפוס סיסמה (Supabase Auth), עריכת פרופיל |

---

## 🧱 סטאק

- **UI:** Jetpack Compose + Material 3, כל המסכים תחת `LayoutDirection.Rtl` (נאכף ב-`Theme.kt` וב-`LocaleHelper`).
- **ארכיטקטורה:** `feature/*` (Composable + ViewModel) ← `data/*` (Repository interfaces + מימושי Supabase) ← `core/*` (מודלים, ניווט, ערכת נושא).
  ה-UI לא ניגש ל-Supabase ישירות; כל התלות עוברת דרך `AppContainer` (DI ידני).
- **Backend:** Supabase (`supabase-kt` 3.x) — Auth, PostgREST, RLS. סכמות ב-`supabase/migrations/`.
- **SDK:** `minSdk 24`, `targetSdk 35`, `compileSdk 36`. **Core library desugaring פעיל** — מותר להשתמש ב-`java.time` (נדרש ל-API 24–25).
- **Web:** `web/` + `vercel.json` + `.well-known/assetlinks.json` — דף נחיתה והפניה לקישורי הזמנה (App Links).

---

## 🚀 הרצה מקומית

דרישות: Android Studio (JBR 21) או JDK 17+, Android SDK 36.

```bash
./gradlew testDebugUnitTest     # בדיקות יחידה (56 בדיקות)
./gradlew assembleDebug         # APK ב-app/build/outputs/apk/debug/
./gradlew lintDebug             # 0 שגיאות; אזהרות גרסה בלבד
```

**Supabase:** ברירת המחדל מובנית ב-`app/build.gradle` (מפתח `anon` ציבורי; ההגנה בפועל היא RLS).
לשרת אחר — העתיקו `.env.example` ל-`.env` (לא נכנס ל-git) ועדכנו `SUPABASE_URL` ו-`SUPABASE_ANON_KEY`.
הקמת פרויקט Supabase חדש: [SUPABASE_SETUP.md](SUPABASE_SETUP.md), והריצו את המיגרציות לפי הסדר:
`phase3_schema` → `phase4_events_and_profiles` → `phase5_invitations`.

> ב-Windows, `gradle.properties` מקומי עשוי להכיל `org.gradle.java.home` — זו הגדרה של המכונה, אל תעלו אותה ל-git.

---

## 🗺️ מפת הקוד (למי שממשיך)

```
app/src/main/java/com/shorashim/
├─ core/
│  ├─ common/        AuthValidator, FamilyValidators, HebrewDates (ICU), LocaleHelper, ViewModelFactory
│  ├─ model/         FamilyModels, EventModels (CalendarEvent + EventDateCalculator), EventCalendarMath
│  ├─ navigation/    ShorashimNavHost / AuthNavHost / BottomBar / Routes (כולל deep link להזמנות)
│  ├─ notifications/ EventNotificationScheduler, EventReminderReceiver, BootCompletedReceiver
│  └─ ui/            theme (Color/Theme/Type), component (מצבי טעינה/ריק/שגיאה, Spacing)
├─ data/             auth · family · invitations · memorials · profile · psalms · events (+ AppContainer)
└─ feature/
   ├─ familytree/    FamilyTreeScreen/ViewModel + canvas/{TreeLayoutEngine, FamilyTreeCanvas, FamilyTreeNodeCard}
   ├─ events/        EventsScreen (רשימה/לוח + FAB), EventsCalendar (רשת חודשית), EventsViewModel, AddEditMemorialEventDialog
   ├─ family/ invitations/ memorials/ tehillim/ settings/ auth/
```

בדיקות: `app/src/test/java/com/shorashim/` (JUnit, ללא מכשיר). מנוע הפריסה והמתמטיקה של לוח השנה הם **פונקציות טהורות** ולכן נבדקות ב-JVM בלבד.

---

## 🧭 החלטות עיצוב קריטיות

### 1. מפת העץ סטטית ו"חכמה" — `TreeLayoutEngine`
- **אין גרירה של כרטיסים.** המשתמש רק מזיז את המפה כולה ומבצע zoom (צביטה, כפתורי +/−, "מיקוד" שמתאים את העץ למסך). `canvasX/canvasY` שנשמרים ב-DB **מתעלמים** — העמודות נשארות רק לתאימות לאחור.
- **משק בית (household)** = אנשים המחוברים ב-`SPOUSE` — שורה אחת של כרטיסים צמודים. מארח (host) = בן/בת הזוג שיש לו הורים בעץ; משק הבית תלוי תחת הוריו.
- **רצועה (band)** לכל משק בית: רוחבה `max(רוחב שורת הזוג, סכום רצועות הילדים)`. רצועות אחים לעולם לא נחתכות ⇒ כרטיס של כלה/חתן שנכנסו למשפחה **לא יכול** לשבת על כרטיס של אח; כל תת-העץ של האח נדחף הצידה. (נבדק ב-`TreeLayoutEngineTest`.)
- **דורות:** ילד תמיד מתחת להורה; בני זוג באותו דור; אב קדמון בלי הורים יורד עד שורה מעל ילדו הצעיר (בלי "חורים"). אילוצים נפתרים באיטרציה עם תקרה — מוגן ממעגלים/נתונים פגומים.
- **סדר ילדים:** לפי תאריך לידה, אחר כך סדר הקלט. **קווי חיבור:** ילדי זוג יוצאים מנקודה משותפת בין ההורים בקו "ארגוני" אורתוגונלי עם פינות מעוגלות.
- **Hit-testing:** כל הקואורדינטות בקנבס מנורמלות לחיוביות (`CONTENT_PADDING`) — קואורדינטות שליליות גורמות לכרטיסים שלא מגיבים ללחיצה ב-Compose.

### 2. אירועים כלוח שנה — `EventsCalendar.kt`
- כיוון עיצובי נקבע ע"י המשתמש (לוח מודרני בסגנון "לוח שנה עברי"): כותרת חודש גדולה (שם דק + שנה עבה) עם טווח חודשים עבריים, פס כותרת טורקיז עם שמות הימים (ראשון מימין), עמודת שבת מוצללת, מספר לועזי גדול + אות עברית קטנה בכל תא, יום נוכחי מסומן.
- אירועים שנתיים ⇒ `EventCalendarMath.occursOn` משווה חודש/יום (29 בפברואר → 28 בשנה לא מעוברת). שימו לב: `CalendarEvent.targetDate` הוא רק המופע *הבא*.
- תאריך עברי: `HebrewDates` מעל `android.icu.util.HebrewCalendar` (API 24+). `toGematria` טהורה ונבדקת.
- בחירת התצוגה נשמרת ב-`rememberSaveable` (ברירת מחדל: לוח שנה).

### 3. לוגו
Adaptive icon: רקע ירוק-יער `#0F3D30` + עץ אורן קווי בירוק-מנטה `#4FD8A5` (`drawable/ic_launcher_{background,foreground}.xml`), כולל שכבת `monochrome` ל-Themed Icons. ל-API 24–25: `mipmap-anydpi/` (layer-list). אייקון התראות נפרד חד-גוני: `drawable/ic_notification.xml` (אייקון צבעוני בסטטוס-בר מוצג ככתם לבן).

---

## 🐞 תיקונים אחרונים (סבב זה)
- הוסף **FAB "הוספת אזכרה"** וחובר הדיאלוג `AddEditMemorialEventDialog` שלא היה מחובר כלל; נוספו עריכה ומחיקה (עם אישור) לאזכרות ידניות.
- עריכת אזכרה: התאריך הוצג `dd/MM/yyyy` בטופס שמצפה ל-`yyyy-MM-dd` ⇒ נשמר פגום. נוספה המרה + ולידציה אמיתית של התאריך + מקלדת טקסט (קלט מספרי לא כולל "-").
- **קריסה פוטנציאלית ב-Android 7.x** (`java.time` ב-API 24–25): הופעל core library desugaring.
- כרטיסי עץ בצד השמאלי לא הגיבו ללחיצה (קואורדינטות שליליות) — תוקן בנרמול הקנבס.
- אייקון התראה צבעוני → אייקון חד-גוני.
- ריענון רשימת העצים/נפטרים ב-`ON_RESUME` של מסך האירועים (לא רק האירועים).

---

## ⚠️ בעיות ידועות ומשימות המשך
1. **לא נבדק על מכשיר/אמולטור בסבב האחרון** — השינויים הוויזואליים (לוח שנה, קנבס, לוגו) עברו קומפילציה, lint ובדיקות יחידה, אך לא נבדקו חזותית. התחילו בהרצה ובדיקה על טלפון קטן (≈360dp) וגדול.
2. אם **שני בני הזוג** שייכים לעצים-אב נפרדים, מופיע הכרטיס רק ליד המארח; קו ההורה של השני ארוך יותר. אפשר לשפר עם קיבוץ משפחות סמוכות.
3. הלוח מציג אירועים שנתיים לפי חודש/יום לועזי בכל שנה (מכוון); שקלו להציג גיל / "שנה לפטירה" לפי השנה המוצגת.
4. אזכרות מחושבות לפי התאריך הלועזי בלבד (לא לפי יום הפטירה העברי); `hebrewDate` הוא שדה ידני לתצוגה.
5. `targetSdk 35` ותלויות (Compose BOM 2024.09, AGP 8.9) ישנות — עדכון עתידי.
6. עדיפות מוצר הבאה לפי [PROJECT_PLAN.md](PROJECT_PLAN.md): פרופיל אדם, תמונות (Storage), הגדרות פרטיות.

---

## 🤝 קונבנציות
- Kotlin רשמי, Compose ללא ViewModel בתוך Composable רגיל (יש `*Route` שחושף ViewModel ו-`*Screen` ללא מצב — קל לבדיקה/Preview).
- טקסטים לממשק ב-`res/values/strings.xml` (עברית). פעולות Repository מחזירות `Result<T>`.
- כל לוגיקה שניתן — טהורה ונבדקת ב-JVM; אל תוסיפו תלות ב-`android.*` לתוך `core/model` או `TreeLayoutEngine`.
- אל תעלו `local.properties`, `.env`, או נתיבי JDK מקומיים.

רישיון: לא הוגדר.
