# PROGRESS — Shorashim (שורשים)

## Status

- **Phase 1 — Project foundation:** Completed.
- **Phase 2 — Authentication & User Accounts:** Completed and verified with **Supabase**. Verified live on physical Samsung Galaxy A54 5G.

**Do not start Phase 3** until approved.

---

## Phase 2 Checklist

- [x] Migrate backend from Firebase to Supabase
- [x] Integrate Supabase Kotlin Multiplatform SDK (`supabaseBom = "3.1.1"`, `auth-kt`, `postgrest-kt`, `storage-kt`, `realtime-kt`, `ktor-client-okhttp = "3.0.3"`)
- [x] Safe `.env` loading and automatic sanitization (removes trailing slashes, `/rest/v1`, quotes) in `app/build.gradle` and `AppContainer.kt`
- [x] Domain models: `AuthUser` (`uid`, `email`, `displayName`) and `AuthState` (`Loading`, `Unauthenticated`, `Authenticated`, `Unconfigured`)
- [x] Core validation utility: `AuthValidator` (name, RFC email format, password min 6 chars, password confirmation, terms acceptance)
- [x] Localized Hebrew error mapper: `AuthErrorMapper` (handles Supabase `RestException`, network errors, and generic exceptions)
- [x] Repository layer: `AuthRepository` interface and `SupabaseAuthRepository` (observes `client.auth.sessionStatus`, coroutines `Result`)
- [x] Dependency injection container: Updated `AppContainer` with Supabase client and `SupabaseAuthRepository`
- [x] UI State & ViewModel: `AuthUiState` and `AuthViewModel` with double-submission guards
- [x] Login screen (`LoginScreen.kt`): Shorashim branding, Hebrew RTL inputs, password show/hide, forgot password & register links, accessible errors
- [x] Registration screen (`RegisterScreen.kt`): Full name, email, password, confirm password, terms dialog, validation feedback
- [x] Password reset screen (`ForgotPasswordScreen.kt`): Email input, reset email trigger, privacy-preserving neutral success message
- [x] Session Gate routing (`MainActivity.kt` & `AuthNavHost.kt`):
  - `Loading`: Displays splash `ShorashimLoadingState` (no flicker of login screen for active sessions)
  - `Unauthenticated` / `Unconfigured`: Renders `AuthNavHost` (no bottom navigation, isolated backstack)
  - `Authenticated`: Renders full app shell with bottom bar and 4 tabs
- [x] Account details & Logout in Settings (`SettingsScreen.kt`): Connected user card (name, email, status) and logout action with Hebrew confirmation dialog
- [x] Unit tests: 13 unit tests covering validators, error mappers, and ViewModel validation logic
- [x] Build verification: `testDebugUnitTest` passed (13/13 tests), `assembleDebug` passed (debug APK generated)
- [x] Hardware verification: Deployed and tested via ADB on connected Samsung Galaxy A54 5G (`SM_A546E`)
- [x] Documentation: Created `SUPABASE_SETUP.md`, updated `ARCHITECTURE.md`, `PROGRESS.md`, `PROJECT_PLAN.md`

---

## Build and Test Verification

Commands executed using Android Studio JBR (`C:\Program Files\Android\Android Studio\jbr`):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat testDebugUnitTest
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat assembleDebug
```

- **testDebugUnitTest:** 13 tests, 0 failures, 0 errors
  - `AuthValidatorTest`: 5 tests
  - `AuthErrorMapperTest`: 2 tests
  - `AuthViewModelTest`: 3 tests
  - `FamilyTreeMappingTest`: 2 tests
  - `ShorashimDestinationTest`: 1 test
- **assembleDebug:** BUILD SUCCESSFUL
- **APK output:** `app/build/outputs/apk/debug/app-debug.apk`
- **Device installation:** Verified on Samsung Galaxy A54 5G via `adb install -r`

---

## Files Created or Changed in Phase 2

### Created / Replaced
- `SUPABASE_SETUP.md`
- `app/src/main/java/com/shorashim/data/auth/SupabaseAuthRepository.kt`
- `app/src/main/java/com/shorashim/core/model/AuthUser.kt`
- `app/src/main/java/com/shorashim/core/model/AuthState.kt`
- `app/src/main/java/com/shorashim/core/common/AuthValidator.kt`
- `app/src/main/java/com/shorashim/core/common/AuthErrorMapper.kt`
- `app/src/main/java/com/shorashim/data/auth/AuthRepository.kt`
- `app/src/main/java/com/shorashim/feature/auth/AuthUiState.kt`
- `app/src/main/java/com/shorashim/feature/auth/AuthViewModel.kt`
- `app/src/main/java/com/shorashim/feature/auth/LoginScreen.kt`
- `app/src/main/java/com/shorashim/feature/auth/RegisterScreen.kt`
- `app/src/main/java/com/shorashim/feature/auth/ForgotPasswordScreen.kt`
- `app/src/main/java/com/shorashim/core/navigation/AuthNavHost.kt`
- `app/src/test/java/com/shorashim/AuthValidatorTest.kt`
- `app/src/test/java/com/shorashim/AuthErrorMapperTest.kt`
- `app/src/test/java/com/shorashim/AuthViewModelTest.kt`

### Modified
- `gradle/libs.versions.toml`: Added Supabase BOM (`3.1.1`), `auth-kt`, `postgrest-kt`, `storage-kt`, `realtime-kt`, `ktor-client-okhttp`
- `app/build.gradle`: Configured `BuildConfig` for `.env` Supabase properties, added Supabase dependencies
- `app/src/main/AndroidManifest.xml`: Added `INTERNET` permission
- `app/src/main/res/values/strings.xml`: Added Hebrew strings for auth, validation, errors
- `app/src/main/java/com/shorashim/data/repositories/AppContainer.kt`: Initialized Supabase client and `SupabaseAuthRepository`
- `app/src/main/java/com/shorashim/core/navigation/ShorashimRoutes.kt`: Added auth routes
- `app/src/main/java/com/shorashim/core/navigation/ShorashimNavHost.kt`: Connected `authState` and `onLogout` to Settings
- `app/src/main/java/com/shorashim/feature/settings/SettingsScreen.kt`: Added user profile card and logout
- `app/src/main/java/com/shorashim/MainActivity.kt`: Root Session Gate

---

## Known Limitations & Supabase Configuration Reference

1. **`.env` Configuration:**
   - Requires valid `SUPABASE_URL` and `SUPABASE_ANON_KEY` in root `.env`.
   - Never committed to Git; covered by `.gitignore`.
2. **Phase Boundaries Respected:**
   - No family tree database models, invitations, or canvas logic added yet. These belong strictly to Phase 3 onward.

---

## Next Steps for the User

1. Follow `SUPABASE_SETUP.md` for any additional project settings if needed.
2. Review and approve moving to **Phase 3 — Family tree data**.
