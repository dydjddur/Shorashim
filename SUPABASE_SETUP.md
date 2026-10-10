# SUPABASE_SETUP — Shorashim (שורשים)

Phase 2 authentication code and dependencies are integrated with **Supabase**.
To connect the application to your Supabase project, follow the instructions below.

Do not commit `.env` or sensitive service-role keys to Git.

---

## 1. Supabase Project Credentials

Shorashim requires two parameters provided via a local `.env` file at the root of the project:

```env
SUPABASE_URL=https://<your-project-id>.supabase.co
SUPABASE_ANON_KEY=<your-anon-public-key>
```

> **Note:**
> - The `SUPABASE_URL` must be the root project URL (e.g. `https://xyzcompany.supabase.co`).
> - The build configuration in `app/build.gradle` automatically sanitizes trailing slashes, `/rest/v1`, quotes, and whitespace.
> - The `.env` file is git-ignored and will never be committed to source control.
> - A template `.env.example` is provided in the repository.

---

## 2. Supabase Authentication Setup

1. In the Supabase Dashboard, navigate to **Authentication** -> **Providers**.
2. Verify that **Email** provider is enabled.
3. (Optional for development) Under **Authentication** -> **URL Configuration** or **Email Templates**, you can disable email confirmation if you want users to log in immediately upon registration without confirming their email first:
   - Go to **Authentication** -> **Providers** -> **Email**.
   - Toggle **Confirm email** off if immediate sign-in upon registration is desired during development.

---

## 3. Architecture & Integration

- **SDK:** Official Supabase Kotlin Multiplatform Client (`io.github.jan-tennert.supabase:auth-kt:3.1.1` + Ktor OkHttp engine).
- **Injection:** Managed centrally through `AppContainer.kt` and initialized as `SupabaseAuthRepository`.
- **Session Gate:** Real-time session monitoring using `client.auth.sessionStatus`:
  - `SessionStatus.Authenticated`: Directly navigates to the authenticated shell.
  - `SessionStatus.Initializing`: Displays `ShorashimLoadingState`.
  - `SessionStatus.NotAuthenticated`: Displays `AuthNavHost` (Login, Register, Password Reset).
- **Error Mapping:** Localized Hebrew error mapping via `AuthErrorMapper.kt` with support for `RestException` and `HttpRequestException`.

---

## 4. Run and Verify

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat assembleDebug
```

Verify with ADB on connected device:
```powershell
C:\Users\user\AppData\Local\Android\Sdk\platform-tools\adb.exe install -r app\build\outputs\apk\debug\app-debug.apk
```
