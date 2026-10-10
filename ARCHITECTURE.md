# ARCHITECTURE — Shorashim

## Layers

```
feature/*     Composables + ViewModels (UI state only)
data/*        Repositories and data sources (Supabase Client, Auth, Postgres, Storage)
core/*        Navigation, theme, shared components, models, common
service/*     Supabase wrappers, local notifications, and background sync (later phases)
```

UI never talks to Supabase or network libraries directly. ViewModels depend on repository interfaces. Data sources implement those interfaces.

## Package layout

```
app/src/main/java/com/shorashim/
  ShorashimApplication.kt
  MainActivity.kt
  core/
    common/          AppContainer, locale, ViewModel factories, AuthValidator, AuthErrorMapper
    model/           Domain models (AuthUser, AuthState, FamilyModels, ScreenContentState)
    navigation/      Destinations, NavHost, bottom bar, AuthNavHost, routes
    ui/theme/        Color, Type, Theme
    ui/component/    Empty / loading / error / spacing
  data/
    auth/            AuthRepository, SupabaseAuthRepository
    family/
    memorials/
    psalms/
    repositories/    Composition of repositories (AppContainer)
  feature/
    auth/            LoginScreen, RegisterScreen, ForgotPasswordScreen, AuthViewModel, AuthUiState
    familytree/
    memorials/
    tehillim/
    settings/
    personprofile/   Phase 4–6
    familymembers/   Phase 5
```

## Dependency injection

`ShorashimApplication` holds `AppContainer` with repository implementations:
- `authRepository`: `SupabaseAuthRepository` (uses official Supabase Auth SDK `io.github.jan-tennert.supabase:auth-kt:3.1.1`, observes `client.auth.sessionStatus`, returns Coroutines `Result`, handles missing `.env` gracefully)
- `familyRepository`: `SupabaseFamilyRepository` (persisted via Supabase PostgREST with Draft/Publish lifecycle, multi-tree switching, and RBAC member management)
- `personRepository`: `SupabasePersonRepository` (persisted via Supabase PostgREST with life status, partial dates, and canvas coordinates)
- `relationshipRepository`: `SupabaseRelationshipRepository` (persisted via Supabase PostgREST with PARENT_CHILD and SPOUSE undirected enforcement)
- `memorialRepository`: `InMemoryMemorialRepository` (Phase 7)
- `psalmsRepository`: `UnavailablePsalmsRepository` (Phase 9)

## Database, Family Trees & Interactive Workflow Canvas (Phase 3)

- **PostgreSQL Database & RLS**:
  - `families`: Supports Draft & Publish lifecycle (`status: DRAFT / PUBLISHED`), ownership, and metadata.
  - `family_members`: Role-Based Access Control (`OWNER`, `ADMIN`, `CONTRIBUTOR`, `VIEWER`).
  - `people`: Comprehensive person records with precision dates and canvas coordinates (`canvas_x`, `canvas_y`).
  - `relationships`: Parent-child and spouse relationships with cycle prevention and undirected spouse constraints.
- **Interactive Workflow Canvas**:
  - Engine: `TreeLayoutEngine` calculates generational levels and connector lines (Bezier drops for parent-child, dashed links for spouses).
  - UI: `FamilyTreeCanvas` supports smooth Pan & Zoom gestures (0.2x to 2.5x) and overlay controls.
  - Nodes: `FamilyTreeNodeCard` with quick relative creation ("הוספת ילד/ה", "הוספת הורה", "הוספת בן/בת זוג").
- **Tree Management & RBAC**:
  - `TreeSwitcherDropdown`: Seamless header switcher between multiple trees.
  - `TreeManagementBottomSheet`: Publish/unpublish, renaming, members, and owner-only tree deletion.
  - `FamilyMembersDialog`: Granular permissions management, email invites, and role promotion/demotion.

## Authentication & Session Gate (Phase 2)

The application roots its UI hierarchy in a reactive **Session Gate** in `MainActivity`:

- **`AuthState.Loading`**: Displays `ShorashimLoadingState` while initial Supabase session is verified (`SessionStatus.Initializing`). Prevents flashing the login screen to users with an existing session.
- **`AuthState.Unauthenticated` / `AuthState.Unconfigured`**: Renders `AuthNavHost` (`login`, `register`, `forgot_password`). Completely isolates the unauthenticated backstack and suppresses the bottom navigation bar.
- **`AuthState.Authenticated`**: Renders the authenticated application shell with `Scaffold`, `ShorashimBottomBar`, and `ShorashimNavHost`.
- **Logout**: Handled via `authRepository.logout()`. Resets session to `Unauthenticated`, tearing down the authenticated graph so back navigation cannot re-enter protected screens.

## UI state

Every data-driven screen maps repository output to:

- Loading
- Empty (no family / no memorials / Tehillim text not integrated)
- Error (message in Hebrew)
- Ready / success

Reusable composables live in `core/ui/component`.

## Navigation

Single activity with two distinct route zones managed by the Session Gate:

### 1. Unauthenticated Zone (`AuthNavHost`)
| Route | Hebrew label |
|---|---|
| `login` | התחברות |
| `register` | יצירת חשבון |
| `forgot_password` | איפוס סיסמה |

### 2. Authenticated Zone (`ShorashimNavHost`)
| Route | Hebrew label |
|---|---|
| `family_tree` | עץ משפחה |
| `memorials` | אזכרות |
| `tehillim` | תהילים |
| `settings` | הגדרות |

## Domain model (target; persisted from Phase 3)

People and relationships are **separate entities**. A family tree is never stored as one image or one giant document.

### User

- `uid`, `displayName`, `email`, `createdAt`

### Family

- `familyId`, `name`, `ownerUid`, `createdAt`, `updatedAt`

### FamilyMembership

- `familyId`, `uid`, `role` (owner / administrator / editor / viewer)
- `membershipStatus`, `notificationConsent`, `joinedAt`

### Person

- `personId`, `familyId`
- names (first, last, optional additional/former)
- `photoUrl`, `lifeStatus` (living / deceased)
- optional Gregorian/Hebrew birth and death, including partial/estimated flags
- `biography`, family memories
- `createdBy`, `createdAt`, `updatedAt`

### Relationship

- `relationshipId`, `familyId`
- `personId`, `relatedPersonId`
- `relationshipType` (parent, child, spouse, sibling, …)
- optional metadata (marriage dates, etc.)

### MemorialEvent

- `eventId`, `familyId`, `personId`
- `calendarType` (Gregorian / Hebrew)
- anniversary representation (never invented if incalculable)
- reminder configuration, timezone, enabled

### NotificationPreference / NotificationJob / TehillimBookmark

As specified in the product spec. `NotificationJob.idempotencyKey` is mandatory for remote scheduling (Phase 8).

## Data storage strategy

| Data | Store | Notes |
|---|---|---|
| Auth session | Firebase Auth | Phase 2 |
| Family, people, relationships, memberships, invitations | Cloud Firestore | Phase 3–5; membership-scoped queries |
| Profile photos | Firebase Storage | Phase 6; same membership checks |
| Tehillim text | Packaged assets or verified remote snapshot | Phase 9; not fabricated |
| Tehillim bookmarks / last-read | Firestore user doc and/or DataStore | Phase 9 |
| Local reminder schedule | WorkManager / AlarmManager | Phase 8 |
| Remote reminder jobs | Cloud Functions + Firestore jobs | Phase 8 |

Room is **not** used unless a measured offline cache is required. Prefer Firestore offline persistence first.

## Security (from Phase 3 onward)

- Firestore rules: every family-scoped document requires an active `FamilyMembership` for `request.auth.uid`.
- Privileged operations (join by code, transfer ownership, fan-out notifications) run in Cloud Functions with Admin SDK — **never** in the client.
- Invitation codes are high-entropy, expiry-checked server-side.
- Changing `familyId` in the client cannot grant membership.
- Living people’s data is not exposed via public URLs or unauthenticated reads.
- No service-account JSON in the Android module.

## RTL and locale

- Default copy is Hebrew (`values/strings.xml`).
- `Application` + `MainActivity` force `he` locale and `LayoutDirection.Rtl`.
- Manifest `supportsRtl=true` and `localeConfig` for Hebrew.

## Notifications (Phase 8 design)

Two independent channels:

1. **Local** — device-scheduled, permission-gated, restored after reboot.
2. **Remote** — FCM from Cloud Functions using membership, consent, timezone, quiet hours, idempotency. The app being closed must not prevent send.

## Hebrew calendar (Phase 7)

Use a maintained library (planned: KosherJava zmanim) for conversions. Documented rules for:

- Hebrew leap months
- Gregorian leap years
- Dates with no safe mapping
- Sunset / civil date boundaries (do not silently guess)
- Missing or partial dates → “cannot calculate” rather than a fabricated anniversary

## Testing

- Unit tests: date conversion, permission helpers, ViewModel mapping (later phases).
- Instrumented tests: navigation, auth forms, critical permissions (later).
- Phase 1: destination uniqueness + default empty-state mapping.
