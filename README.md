# שורשים (Shorashim) 🌳

אפליקציית Android לניהול **עצי משפחה דינמיים**, שימור מורשת והנצחה יהודית.
Kotlin · Jetpack Compose · Material 3 · Supabase (Auth + Postgres/RLS) · ממשק עברי מלא (RTL).

> **למודל/מפתח שממשיך את הפרויקט:** התחילו מ-[מפת הקוד](#-מפת-הקוד-למי-שממשיך), קראו את
> [החלטות העיצוב הקריטיות](#-החלטות-עיצוב-קריטיות) ואת [מה צריך להריץ בשרת](#-מה-צריך-להריץ-בשרת-חשוב) ו[בעיות ידועות](#-בעיות-ידועות-ומשימות-המשך).
> מסמכי עומק: [ARCHITECTURE.md](ARCHITECTURE.md) · [PROGRESS.md](PROGRESS.md) · [PROJECT_PLAN.md](PROJECT_PLAN.md) · [SUPABASE_SETUP.md](SUPABASE_SETUP.md).

---

## 🌟 יכולות

| תחום | מה יש |
|---|---|
| **עצי משפחה** | מספר עצים למשתמש, טיוטה → פורסם, הרשאות (Owner / Admin / Contributor / Viewer) |
| **מפת עץ** | מפה **סטטית** עם סידור אוטומטי חכם, זום וגלילה בלבד. מכל כרטיס: הוספת **ילד / הורה / בן-בת זוג / אח-אחות** — הקשר נשמר אוטומטית. על כל קו חיבור סמל: 👪 הורה-ילד · 💍 זוג · 🤝 אחים |
| **אירועים** | **לוח שנה חודשי** (ברירת מחדל) או רשימה. ימי הולדת, אזכרות, **כל החגים והמועדים** (חול המועד, צומות, חנוכה, פורים, ימי זיכרון ועצמאות, ראשי חודשים), **פרשת השבוע** ושבתות מיוחדות, תאריך עברי בכל תא |
| **תהילים** | 150 פרקים מנוקדים, אופליין; עיצוב ספרי (גופן סריף, מספרי פסוקים באותיות, קישוט זהב); "לעילוי נשמה" עם הקדשה |
| **הזמנות** | קישור הזמנה **רב-שימושי** (אחד לכל המשפחה, תקף 30 יום): שולחים בקבוצת וואטסאפ, כל בן משפחה פותח, מתחבר ומאשר — בלי מייל. דף נחיתה מציג את שם העץ ושולח ההזמנה |
| **תזכורות** | התראות מקומיות 09:00 (שעון ישראל): אזכרה 7 ימים מראש, יום הולדת ביום עצמו |
| **חשבון** | הרשמה / התחברות / איפוס, עריכת פרופיל |

---

## 🧱 סטאק

- **UI:** Compose + Material 3, תמיד `LayoutDirection.Rtl` (`Theme.kt`, `LocaleHelper`).
- **ארכיטקטורה:** `feature/*` (Composable + ViewModel) ← `data/*` (Repository interfaces + מימושי Supabase) ← `core/*`. DI ידני דרך `AppContainer`. ה-UI לא ניגש ל-Supabase ישירות.
- **Backend:** Supabase (`supabase-kt` 3.x) — Auth, PostgREST, RLS, RPC. סכמות ב-`supabase/migrations/`.
- **SDK:** `minSdk 24`, `targetSdk 35`, `compileSdk 36`. **Core library desugaring פעיל** (מותר `java.time`).
- **Web:** `web/public/` (+ עותקים בשורש ו-`web/`) + `vercel.json` + `.well-known/assetlinks.json` — דף ההזמנה וקישורי App Links.

---

## 🚀 הרצה מקומית

דרישות: Android Studio (JBR 21) או JDK 17+, Android SDK 36.

```bash
./gradlew testDebugUnitTest     # בדיקות יחידה
./gradlew assembleDebug         # APK ב-app/build/outputs/apk/debug/
./gradlew lintDebug             # 0 שגיאות
adb install -r app/build/outputs/apk/debug/app-debug.apk   # applicationId: com.example.remember_me
```

**Supabase:** ברירת המחדל מובנית ב-`app/build.gradle` (מפתח `anon` ציבורי; ההגנה היא RLS). לשרת אחר: `.env` (ראו `.env.example`).

> `gradle.properties` מקומי עשוי להכיל `org.gradle.java.home` — הגדרת מכונה, לא לעלות ל-git.

---

## 🗄️ מה צריך להריץ בשרת (חשוב)

הרצה ידנית ב-**Supabase → SQL Editor**, לפי הסדר (כולם idempotent):

1. `20261011_phase3_schema.sql`
2. `20261011_phase4_events_and_profiles.sql` ← **בשרת הנוכחי חסר!** הטבלה `memorial_events` לא קיימת, ולכן הוספת אזכרה ידנית נכשלת (`Could not find the table 'public.memorial_events'`). האפליקציה מציגה כעת הודעה ברורה.
3. `20261011_phase5_invitations.sql`
4. `20261012_phase6_group_invite_links.sql` ← **קישורי הזמנה רב-שימושיים.** בלעדיו הקישור חד-פעמי (הראשון שאישר "שורף" אותו). מוסיף `uses_count/max_uses`, `invitation_acceptances`, מחליף `accept_invitation`, ופותח את `resolve_invitation` ל-`anon` כדי שדף הנחיתה יציג את שם העץ.

**Vercel (דומיין ההזמנות `shorashim-invite.vercel.app`):** הפרויקט שייך למחבר המקורי (לא נגיש מהכלים של מודל זה). אחרי `git push` ייתכן שנדרש Redeploy. ראו [הזמנות והדומיין](#3-הזמנות-קישור-לקבוצה-והדומיין) לשורש הבעיה שתוקנה.

---

## 🗺️ מפת הקוד (למי שממשיך)

```
app/src/main/java/com/shorashim/
├─ core/
│  ├─ common/        HebrewDates (תצוגת תאריך עברי), Validators, LocaleHelper, ViewModelFactory
│  ├─ model/         FamilyModels (+NewPersonRelation), EventModels, EventCalendarMath,
│  │                 HebrewCalendarMath (לוח עברי טהור), JewishCalendar (חגים+פרשות)
│  ├─ navigation/    NavHost, BottomBar, Routes (כולל add_edit_person?relation=&anchorId=)
│  └─ notifications/ מתזמן התראות, Receivers
├─ data/             auth · family · invitations · memorials · profile · psalms · events (+AppContainer)
└─ feature/
   ├─ familytree/    canvas/{TreeLayoutEngine, FamilyTreeCanvas, FamilyTreeNodeCard}
   ├─ family/        ניהול עצים, AddEditPerson (מקבל relation+anchor ויוצר קשרים), שיתוף הזמנה
   ├─ events/        EventsScreen (רשימה/לוח), EventsCalendar (רשת חודשית), דיאלוג אזכרה
   ├─ tehillim/ settings/ invitations/ auth/ memorials/
supabase/migrations/   סכמות ו-RPC          web/public/   דף הזמנה + assetlinks + vercel.json
app/src/test/          JUnit (ללא מכשיר) + resources/hebcal_israel_2024_2036.txt (נתוני אמת לבדיקה)
```

---

## 🧭 החלטות עיצוב קריטיות

### 1. מפת העץ — `TreeLayoutEngine` + `FamilyTreeCanvas`
- **סטטית:** רק pan/zoom (צביטה, כפתורים, "מיקוד" = התאמה למסך; מתאים מחדש כשמספר האנשים משתנה). `canvasX/Y` מהשרת **מתעלמים**.
- **משק בית (household)** = אנשים המחוברים ב-SPOUSE (שורה אחת). **מארח** = בן/בת הזוג עם הורים בעץ; משק הבית תלוי תחת הוריו.
- **רצועה (band)** לכל משק בית: `max(רוחב שורת הזוג, סכום רצועות הילדים)` ⇒ רצועות אחים לא נחתכות, כלה/חתן לא יושבים על אח. **יישור:** ההורים ממורכזים מעל נקודות החיבור של הילדים; ילד יחיד תלוי ישר מתחת להורה; ילדי זוג יוצאים מנקודה משותפת בין ההורים.
- **דורות:** ילד מתחת להורה, זוג באותו דור, אב קדמון בלי הורים יורד עד שורה מעל ילדו (בלי "חורים"). מוגן ממעגלים/כפילויות.
- **סדר:** ילדים לפי תאריך לידה; **RTL** — הבכור/הראשון מימין. עצים שקשורים בנישואין נשארים צמודים.
- **סמלי קשר** (`connectionMarkers`): 👪 על ירידת כל ילד, 💍 על קו הזוג, 🤝 על הפס בין אחים צמודים.
- **יחידות:** פריסה ב-**dp**; `graphicsLayer` ו-hit-test בפיקסלים. האפליקציה RTL ⇒ `TopLeftAbsolute` + `absoluteOffset` (אחרת הכול זז ל-RTL ונעלם).
- **הוספת קרוב:** `AddEditPersonViewModel` מקבל `relation` + `anchorId`, שומר את האדם **ואת הקשר** (ילד: אפשרות לבחור בן/בת זוג כהורה שני; הורה: קישור אוטומטי להורה הקיים כבני זוג; אח: אותם הורים; ללא הורים — חסום עם הסבר). כישלון בקישור מוחק את הכרטיס שנוצר (אין יתומים). מחיקת אדם — בדיאלוג אישור.

### 2. לוח שנה יהודי — `HebrewCalendarMath` + `JewishCalendar`
- המרה עברית↔לועזית **בקוד טהור** (לא ICU; ICU החזיר שמות חודשים לועזיים). נבדק מול **Hebcal (ישראל)**: כל החגים, הצומות, ראשי חודשים, שבתות מיוחדות וכל פרשות השבוע (כולל כפולות) תואמים ל-2024–2036 (`JewishCalendarTest`).
- **לוח ישראל:** יום טוב אחד, שמיני עצרת=שמחת תורה, חול המועד, דחיות (צום גדליה, י״ז בתמוז, ט׳ באב, תענית אסתר, יום השואה/זיכרון/עצמאות, סיגד).
- **פרשות:** סימולציית מחזור קריאה מסימחת תורה; חיבור פרשות לפי סוג השנה (אורך+יום ר״ה) ו-`נצבים-וילך` לפי ר״ה הבא — הכללים נגזרו ואומתו מול נתוני אמת.
- UI (`EventsCalendar.kt`): ירוק (לא כחול), עמודת שבת מוצללת, תא יום טוב בזהב, תגיות ימי הולדת/אזכרות, גיליון תחתון ליום (חגים + אירועי משפחה), החלקה בין חודשים.

### 3. הזמנות (קישור לקבוצה) והדומיין
- **שורשי הבעיות שנמצאו:** (א) הטוקן היה חד-פעמי ⇒ `20261012_phase6_group_invite_links.sql`; (ב) `https://shorashim-invite.vercel.app/invite/<token>` החזיר **404** — Vercel שירת את `web/public` ללא `vercel.json` (ה-rewrites לא פעלו) ⇒ נוסף `vercel.json` + `404.html` בכל המיקומים; (ג) ה-App Link **לא אומת** כי טביעת האצבע של החתימה בטלפון (`A3:DE:47:…`) לא הייתה ב-`assetlinks.json` ⇒ נוספה. לחתימת release יש להוסיף את טביעת האצבע שלה.
- **זרימה:** הקישור נפתח → (אם App Link מאומת, ישר באפליקציה; אחרת דף נחיתה עם `intent://` / `shorashim://`) → התחברות → מסך הזמנה → אישור → כניסה לעץ. הטוקן ממתין עד לסיום ההתחברות.
- דף הנחיתה (`web/public/index.html`) שולף `resolve_invitation` עם מפתח anon ומציג שם עץ, שולח ותפקיד. קישור פתוח לא יכול להעניק ניהול (נחתך ל-CONTRIBUTOR בשרת).

### 4. עיצוב
פלטת ירוק-יער + זהב (`Gold #C9A24B`, נר `#F2B84B`). תהילים: גופן סריף, מספרי פסוקים באותיות, מפריד ✦, סרגל תחתון עם התקדמות וחיצי RTL נכונים, באנר נר ללעילוי נשמה. הגדרות: כרטיס פרופיל עם אווטאר ושורות עם אייקונים. כרטיס אדם בעץ: אות ראשונה כאווטאר.

### 5. לוגו
Adaptive icon: רקע `#0F3D30` + עץ אורן קווי `#4FD8A5`, שכבת monochrome; ל-API 24–25: `mipmap-anydpi/`; אייקון התראות חד-גוני נפרד.

---

## 🐞 תיקונים עיקריים (סבב זה)
- **הוספת הורה/ילד/בן זוג מהכרטיס לא יצרה קשר כלל** (רק פתחה טופס ריק) — לכן "ההורה הודבק ליד הילד". תוקן + נוסף "אח/אחות".
- יחידות הפריסה (px מול dp) גרמו לכרטיסים לחפוף; ב-RTL המפה נעלמה/זזה — תוקן.
- אזכרות: דיאלוג מנותק, תאריך עריכה פגום, **כשל שמירה שקט** (טבלה חסרה) — עכשיו הודעת שגיאה בדיאלוג.
- `java.time` ב-Android 7 (desugaring); אייקון התראה; מחיקת אדם בלי אישור.
- תאריך עברי שגוי בלוח (ICU) — הוחלף במימוש טהור ובדוק.
- כפתורי "לוח שנה/רשימה" נחתכו; כפתור "הוספת אזכרה" הועבר לתחתית.

---

## ⚠️ בעיות ידועות ומשימות המשך
1. **להריץ migrations 4 ו-6 בשרת** (ראו למעלה) ולבצע Redeploy ל-Vercel. עד אז: אזכרות ידניות לא נשמרות, וקישור הזמנה חד-פעמי.
2. כמה שורשים לא קשורים מוצבים בשורה אחת (רחב מאוד) — אפשר לעטוף לשורות.
3. תצוגת רשימת אירועים: טרם עוצבה מחדש כמו הלוח; אזכרות לפי יום עברי טרם (יש כעת `JewishCalendar` לבסיס).
4. `applicationId` הוא `com.example.remember_me` — להחליף לפני פרסום, ולעדכן `assetlinks.json`/`AndroidManifest`/`web/public/index.html`.
5. עדכון תלויות (Compose BOM, AGP) וקידום `targetSdk`.

---

## 🤝 קונבנציות
- Kotlin רשמי; `*Route` (עם ViewModel) ו-`*Screen` ללא מצב. מחרוזות ב-`strings.xml`. Repository מחזיר `Result<T>`.
- כל לוגיקה שאפשר — טהורה ונבדקת ב-JVM (`core/model`, `TreeLayoutEngine`); בלי `android.*` שם.
- בדיקות נתונים: לא לערוך ידנית את `hebcal_israel_2024_2036.txt` (נוצר מ-API של Hebcal).
- אל תעלו `local.properties`, `.env`, או נתיבי JDK מקומיים.

רישיון: לא הוגדר.
