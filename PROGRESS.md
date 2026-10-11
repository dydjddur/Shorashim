# PROGRESS — Shorashim (שורשים)

## Status

- **Phase 1 — Project foundation:** Completed.
- **Phase 2 — Authentication & User Accounts:** Completed with **Supabase**. Verified live on physical Samsung Galaxy A54 5G.
- **Phase 3 — Supabase Database, Family Trees, People, Relationships & Interactive Workflow Canvas:** Completed and verified live on physical Samsung Galaxy A54 5G.
- **Phase 4 — Events Tab, 150 Offline Psalms, Memorial Mode, User Profile Editing & Notifications:** Completed and verified live on physical Samsung Galaxy A54 5G.

- **Phase 4.5 — Events calendar, static smart tree layout, new logo, bug-fix pass:** Completed in code (unit tests + lint green; **not yet verified visually on a device**). See README "החלטות עיצוב קריטיות".

---

## Phase 4.5 Checklist

- [x] Events tab: month-grid calendar (default) / list toggle, Hebrew day per cell, day sheet, swipe between months.
- [x] Family tree map is static (pan/zoom only); `TreeLayoutEngine` rewritten (household bands, no overlaps, birth-order children, shared couple origin, orthogonal connectors, fit-to-screen).
- [x] New launcher icon (adaptive + legacy + monochrome) and monochrome notification icon.
- [x] Core library desugaring (java.time on API 24-25); memorial add/edit/delete wired up; date-format bug fixed.
- [x] Tests: 56 unit tests, 0 failures (`./gradlew testDebugUnitTest`); `lintDebug`: 0 errors.

---

## Phase 4 Checklist

- [x] **Events Tab ("אירועים"):**
  - Replaced Memorials tab with unified chronological list of upcoming Birthdays and Memorials.
  - Calculated next birthdays with `Asia/Jerusalem` timezone, leap-year handling (Feb 29 -> Feb 28 fallback), and deduplication across multiple trees.
  - Full CRUD operations for Memorial events with family tree and deceased person association.
  - Memorial card details dialog with 1-tap navigation to read Tehillim for deceased relative.
- [x] **Complete 150 Psalms ("ספר תהילים") Offline:**
  - Bundled authentic Masoretic vocalized Hebrew text of all 150 chapters (`assets/tehillim_150.json`).
  - Chapter selector bottom sheet with Hebrew gematria numerals (including ט"ו and ט"ז).
  - Special highlighted yellow item **"תהילים לעילוי נשמה"** with 7 prescribed chapters: 33, 16, 17, 72, 91, 104, 130.
  - Dedication banner formatting: `"לעילוי נשמת {שם מלא}, {שם מלא} בן/בת {שם האם}"`.
  - Font size controls (`א+`, `א-`) and next/previous chapter navigation.
- [x] **User Profile Editing:**
  - "עריכת פרופיל" button on Settings account card.
  - Full name and display name validation and updates to Supabase GoTrue `user_metadata` and `public.profiles`.
- [x] **Date Validation & RTL Layout Improvements:**
  - Day, Month, Year input fields reordered for natural Hebrew RTL flow (DD/MM/YYYY).
  - Pre-submission range and calendar validation preventing invalid date inputs (e.g. month 25) and PostgreSQL out of range errors.
- [x] **Notifications & Background Scheduling:**
  - 7 days before reminder for memorials at 09:00 AM (Israel time).
  - Same-day reminder for birthdays at 09:00 AM (Israel time).
  - `AlarmManager.setExactAndAllowWhileIdle` with `EventReminderReceiver` and `BootCompletedReceiver`.
- [x] **Automated Tests:**
  - 36 unit tests passed, 0 failures (`EventDateCalculatorTest`, `TehillimRepositoryTest`, `FamilyValidatorsTest`, etc.).
  - Verified live on physical Samsung Galaxy A54 5G.

---

## Build and Test Verification

Commands executed using Android Studio JBR (`C:\Program Files\Android\Android Studio\jbr`):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat testDebugUnitTest
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat assembleDebug
```

- **testDebugUnitTest:** 36 tests passed, 0 failures, 0 errors
  - `EventDateCalculatorTest`: 6 tests
  - `TehillimRepositoryTest`: 3 tests
  - `TreeLayoutEngineTest`: 5 tests
  - `FamilyValidatorsTest`: 7 tests
  - `AuthValidatorTest`: 5 tests
  - `AuthErrorMapperTest`: 2 tests
  - `AuthViewModelTest`: 3 tests
  - `FamilyTreeMappingTest`: 2 tests
  - `PersonRelationshipCalculatorTest`: 3 tests
  - `ShorashimDestinationTest`: 1 test
- **assembleDebug:** BUILD SUCCESSFUL
- **APK output:** `app/build/outputs/apk/debug/app-debug.apk`
- **Device installation:** Verified on Samsung Galaxy A54 5G via `adb install -r`
