# PROGRESS — Shorashim (שורשים)

## Status

- **Phase 1 — Project foundation:** Completed.
- **Phase 2 — Authentication & User Accounts:** Completed with **Supabase**. Verified live on physical Samsung Galaxy A54 5G.
- **Phase 3 — Supabase Database, Family Trees, People, Relationships & Interactive Workflow Canvas:** Completed and verified live on physical Samsung Galaxy A54 5G.

---

## Phase 3 Checklist

- [x] **PostgreSQL Schema & Security Migration (`supabase/migrations/20261011_phase3_schema.sql`):**
  - `families`: `id`, `name`, `description`, `owner_id`, `status` ('DRAFT', 'PUBLISHED', 'ARCHIVED'), `schema_version`.
  - `family_members`: `family_id`, `user_id`, `role` ('OWNER', 'ADMIN', 'CONTRIBUTOR', 'VIEWER'), `invited_by`.
  - `people`: `id`, `family_id`, `first_name`, `last_name`, `additional_names`, `life_status`, `birth_date`, `birth_date_precision`, `death_date`, `death_date_precision`, `biography`, `canvas_x`, `canvas_y`.
  - `relationships`: `PARENT_CHILD`, `SPOUSE` (undirected unique constraint).
  - Row Level Security (RLS) policies enforcing view/edit/delete permissions based on member roles.
  - Database functions: `create_family` (atomic family + owner membership with status 'DRAFT') and `validate_relationship_integrity` (cycle prevention).
- [x] **Draft & Publish Lifecycle:**
  - Trees are created by default in `DRAFT` status ("טיוטה").
  - Prominent "העלאה / פרסום" button and status banner in the tree view to publish live.
  - Option to revert published trees back to draft.
- [x] **Role-Based Access Control (RBAC):**
  - `OWNER` (מנהל-על / יוצר): Full control, exclusively can delete tree and promote/demote admins.
  - `ADMIN` (מנהל): Can invite members, edit tree metadata, and manage family relatives.
  - `CONTRIBUTOR` (משתתף): Default permission for invited members. Can add and edit relatives (spouses, children, parents, grandparents) and relationships, but cannot delete tree or manage member roles.
  - `VIEWER` (צופה): Read-only view of the tree.
  - Dedicated `FamilyMembersDialog` for viewing members, inviting via email, and changing roles.
- [x] **Interactive Workflow Canvas (`TreeLayoutEngine`, `FamilyTreeCanvas`, `FamilyTreeNodeCard`):**
  - Smooth Pan & Zoom navigation (gesture-based and overlay controls: +, -, fit center).
  - Generational layout engine grouping root ancestors, spouses side-by-side, and descendants.
  - Connectors: Bezier drop lines for parent-child relations and dashed links for spouses.
  - Person Node cards displaying photo/avatar, Hebrew names, living/deceased indicators (🕯️), and life dates.
  - Node action menu on tap: "הוספת ילד/ה", "הוספת הורה", "הוספת בן/בת זוג", "עריכת פרטים", and "מחיקת אדם".
- [x] **Multi-Tree Switcher & View Modes:**
  - `TreeSwitcherDropdown`: Quick header switcher between all user's trees with status chips (טיוטה / פורסם) and "+ צור עץ חדש".
  - Tab switcher between "מפת שושלת" (Interactive Workflow Map) and "רשימה וחיפוש" (Member search and list).
- [x] **Unit Tests & Quality Verification:**
  - 23 unit tests pass cleanly in ~5s (`TreeLayoutEngineTest`, `FamilyValidatorsTest`, `AuthValidatorTest`, `AuthErrorMapperTest`, etc.).
  - APK compiled and verified via ADB install and live screen capture on Samsung Galaxy A54 5G.

---

## Build and Test Verification

Commands executed using Android Studio JBR (`C:\Program Files\Android\Android Studio\jbr`):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat testDebugUnitTest
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat assembleDebug
```

- **testDebugUnitTest:** 23 tests passed, 0 failures, 0 errors
  - `TreeLayoutEngineTest`: 5 tests
  - `FamilyValidatorsTest`: 5 tests
  - `AuthValidatorTest`: 5 tests
  - `AuthErrorMapperTest`: 2 tests
  - `AuthViewModelTest`: 3 tests
  - `FamilyTreeMappingTest`: 2 tests
  - `ShorashimDestinationTest`: 1 test
- **assembleDebug:** BUILD SUCCESSFUL
- **APK output:** `app/build/outputs/apk/debug/app-debug.apk`
- **Device installation:** Verified on Samsung Galaxy A54 5G via `adb install -r`
