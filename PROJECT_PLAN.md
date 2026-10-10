# PROJECT_PLAN — Shorashim (שורשים)

This document is the source of truth for implementation order, scope, and definition of done.

**Product:** Native Android app for family genealogy, memorial anniversaries, authorized family sharing, and Hebrew Tehillim reading.

**Stack:** Kotlin, Jetpack Compose, Material 3, Navigation Compose, ViewModel, Coroutines/StateFlow, repository pattern, Supabase (Auth, Postgres/PostgREST, Storage, Realtime, Edge Functions) in later phases.

**Rules:** Implement one phase at a time. Build and fix errors after each meaningful phase. Do not claim a feature works unless it is implemented and tested. Do not invent credentials. Do not put secrets in the client. Do not fake button actions.

---

## Current repository baseline (inspected)

Existing project:

- Gradle Groovy (`build.gradle`) + version catalog `gradle/libs.versions.toml`
- AGP 8.9.1, Kotlin 2.0.21, Compose BOM 2024.09.00, compileSdk 36, targetSdk 35, minSdk 24
- Application ID remains `com.example.remember_me` (matches the existing Android Studio run configuration). Source namespace is `com.shorashim`.
- Single `MainActivity` with Session Gate
- `android:supportsRtl="true"`; Hebrew copy, navigation, Supabase SDK integrated

---

## Phase 1 — Project foundation

**Goal:** A buildable Hebrew RTL shell with design system, bottom navigation, reusable UI states, and architecture stubs.

**Includes:**

- Planning documents: `PROJECT_PLAN.md`, `ARCHITECTURE.md`, `SUPABASE_SETUP.md`, `PROGRESS.md`
- Application ID `com.shorashim`, Application class, Hebrew default strings
- Shorashim Material 3 theme (specified palette, accessible type, RTL)
- Reusable loading / empty / error components
- Bottom navigation: Family Tree, Memorials, Tehillim, Settings
- Feature screens + ViewModels + repository interfaces with in-memory empty implementations
- Gradle dependencies required for navigation and ViewModel
- `assembleDebug` and unit tests

**Does not include:** authentication, persistent tree data, invitations, tree canvas, notifications, Tehillim text.

**Exit criteria:** App launches to four Hebrew destinations with honest empty states; project compiles.

---

## Phase 2 — Authentication (complete & verified with Supabase)

**Goal:** Email/password registration, login, logout, password reset, persistent session, Hebrew validation errors, session gate.

**Status:** Code implemented and verified with 13 unit tests, clean APK assembly, and verified on connected Samsung Galaxy A54 5G. Integrated with Supabase Auth (`io.github.jan-tennert.supabase:auth-kt:3.1.1`). Safe `.env` loading and runtime URL sanitization.

**Includes:** Auth data source (`SupabaseAuthRepository`), repository interface, screens (Login, Register, Forgot Password), session gate in `MainActivity` with `AuthNavHost`, account card & real logout in Settings, localized Hebrew validations and error mappings.

**Exit criteria:** Register/login/logout/reset work against Supabase Auth on device/emulator. Session survives process death. Errors are in Hebrew. No placeholder auth buttons.

---

## Phase 3 — Family tree data

**Goal:** Typed models, Firestore repositories, security rules, create/load family, add/edit people and relationships as separate entities.

**Includes:** `Family`, `Person`, `Relationship` documents (not one giant tree document), unique IDs, membership owner record on create, indexes, rules that deny non-members.

**Exit criteria:** Authenticated user can create a named family and persist people/relationships; reload shows the same data. Unauthorized UID cannot read another family.

---

## Phase 4 — Family tree UI

**Goal:** Interactive tree generated from persistent data, plus accessible list fallback.

**Includes:** Zoom/pan, multi-generation layout, select person → profile route, search by name, navigate parents/children/spouses, incomplete dates, no duplicate person nodes for the same ID.

**Exit criteria:** Tree is data-driven (not a static image). Complex relationships (multiple parents, multiple marriages) render without duplicating person records.

---

## Phase 5 — Shared family access

**Goal:** Invitations, roles, join flow, member list, revoke/expire, remove member, transfer ownership.

**Includes:** Hard-to-guess time-limited invitation codes; **server-side** validation (Cloud Functions or equivalent) so membership cannot be gained by editing a family ID on the client. Roles: owner, administrator, editor, viewer.

**Exit criteria:** Two test accounts — invitee joins only with a valid invitation; guessing `familyId` fails. Permissions enforced in rules + backend.

---

## Phase 6 — Person profiles and memory

**Goal:** Full profile fields, photos (Storage), biography/memories, life status, date validation, edit permissions, confirm-before-delete, audit history where practical.

**Includes:** Death fields only when deceased; no public URLs for living people; Storage rules membership-scoped.

**Exit criteria:** Editor can update a profile and photo; viewer cannot; living-person privacy constraints documented and enforced.

---

## Phase 7 — Memorial calendar

**Goal:** Upcoming list + monthly view, Gregorian and Hebrew anniversaries, per-event calendar system, documented conversion rules, unit tests for edge cases.

**Includes:** Trusted Hebrew-calendar library (KosherJava / Zmanim or equivalent — chosen and documented in this phase). Leap years, Hebrew leap months, unmappable dates, timezone, missing dates: **never invent a date**. Quiet-hours and reminder preferences stored (delivery in Phase 8).

**Exit criteria:** Tests cover conversion and “cannot determine anniversary” paths. UI shows the selected calendar convention.

---

## Phase 8 — Notifications

**Goal:** (A) Local reminders on-device with permission, reboot reschedule. (B) Family-wide FCM via trusted backend scheduling.

**Includes:** Consent, opt-out, quiet hours, idempotency keys, no duplicates on retry, no send after leaving family, no privileged keys in the APK.

**Exit criteria:** Local reminders fire on a physical device. Remote path documented and implemented in Cloud Functions; duplicate/retry tests described. Users who opted out receive nothing.

---

## Phase 9 — Tehillim

**Goal:** 150 chapters, reader, font size, bookmarks, last-read, deep link from memorial/notification.

**Includes:** Legally appropriate verified Hebrew text, source/edition/license documented. Text kept out of UI code. If text is not yet licensed, integration point only — **never label placeholders as scripture**.

**Exit criteria:** Opening a chapter shows verified text or a clear “text not integrated” state. Bookmarks persist per user.

---

## Phase 10 — Settings and privacy

**Goal:** Notification settings, member management entry points, data export, account deletion and cleanup, privacy copy, ownership policy.

**Exit criteria:** Account deletion removes or anonymizes user-scoped data per documented policy. Export produces a user-readable dump of families they own or are allowed to export.

---

## Phase 11 — Testing and release preparation

**Goal:** Unit + UI tests, permission tests, empty/loading/offline/error, notification device tests, no fake production buttons, release AAB/APK, Firebase release config documented.

**Exit criteria:** All definition-of-done items in the product spec are true.

---

## Definition of done (product)

1. User can register and log in.
2. User can create a family tree and save it.
3. People can be added, edited, and related without duplicate person records.
4. Tree is generated from persistent data.
5. Authorized relatives can join and collaborate.
6. Unauthorized users cannot access private family data.
7. Memorial dates use documented, tested rules.
8. Notifications respect consent and do not duplicate.
9. Tehillim uses verified text.
10. App builds; core flows tested.
11. Setup docs name every required external configuration.

---

## Explicit non-goals until their phase

| Capability | First honest phase |
|---|---|
| Supabase Auth | 2 |
| Supabase Postgres people/relationships | 3 |
| Interactive tree canvas | 4 |
| Invitations / roles | 5 |
| Photos / biography UX complete (Supabase Storage) | 6 |
| Hebrew calendar calculations | 7 |
| Reminders & Notifications | 8 |
| Verified Tehillim corpus | 9 |
| Account deletion / export | 10 |
| Release signing | 11 |
