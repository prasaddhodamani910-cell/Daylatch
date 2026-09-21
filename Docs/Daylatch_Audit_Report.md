# Daylatch — Full Bug, Crash & UI Audit

**Project:** Daylatch (offline-first habit tracker, Android / Kotlin / Jetpack Compose / Room)
**Author of project:** Prasad Dhodamani
**Audit date:** 20 Sep 2026
**Scope:** everything in `Daylatch.zip` — source, Gradle config, generated Room code, the built APK, the Kotlin error log, and the three design documents in `Docs/`.

> **You asked for the problems only, not the fixes.** Each finding below states *what* is wrong, *where*, *why it matters* and *how to confirm it*. A short "Fix direction" is included on most items purely so you know which way to go; there is no code to paste.

---

## Table of contents

1. [How this audit was done (and its limits)](#1-how-this-audit-was-done-and-its-limits)
2. [Executive summary](#2-executive-summary)
3. [Project layout — which module is actually running](#3-project-layout--which-module-is-actually-running)
4. [Crash analysis](#4-crash-analysis)
5. [Build & configuration issues](#5-build--configuration-issues)
6. [Data-layer (Room) issues](#6-data-layer-room-issues)
7. [Logic bugs](#7-logic-bugs)
8. [ViewModel, state & architecture issues](#8-viewmodel-state--architecture-issues)
9. [Performance issues](#9-performance-issues)
10. [UI / UX problems (professional-polish review)](#10-ui--ux-problems-professional-polish-review)
11. [Accessibility](#11-accessibility)
12. [Privacy claims audit](#12-privacy-claims-audit)
13. [Spec vs implementation gap table](#13-spec-vs-implementation-gap-table)
14. [Testing gaps](#14-testing-gaps)
15. [Things I checked that are fine](#15-things-i-checked-that-are-fine)
16. [Recommended fix order](#16-recommended-fix-order)
17. [Appendix A — How to get the real crash log](#appendix-a--how-to-get-the-real-crash-log)
18. [Appendix B — File / line index](#appendix-b--file--line-index)

---

## 1. How this audit was done (and its limits)

**What I did**
- Read every source file line by line (`app/` module, plus the older `android-app/` module).
- Decoded the **binary `AndroidManifest.xml` inside the built `app-debug.apk`** to see what actually shipped.
- Confirmed the APK contains strings from the *current* source (`"Small steps, every day"`, `"DATA VAULT"`, `"Your month at a glance"`), so the APK is a build of the current `app/` code.
- Compared the **Room-generated schema and identity hash** of both modules (`AppDatabase_Impl.java` vs `DaylatchDatabase_Impl.java`).
- Computed real **WCAG contrast ratios** for every colour pair used in the UI.
- Cross-checked the code against `habit-tracker-project-plan.md`, `Habit_Tracker_UI_Design_System.md`, and the React reference (`HabitDashboard.tsx.txt`, `index.css`).

**What I could not do**
- I cannot run an Android device or emulator here, so **I have not seen a live stack trace**. The crash diagnosis in §4 is based on evidence in your files, not on a captured log. I label every finding with a confidence level:
  - **[Confirmed]** — provable from the code/config alone.
  - **[Likely]** — strong evidence, but depends on device state or history.
  - **[Hypothesis]** — plausible; needs a log to confirm.

**Severity legend**

| Level | Meaning |
|---|---|
| **Critical** | Crashes the app, loses data, or makes a core feature give wrong answers |
| **High** | Clearly wrong behaviour users will hit, or a broken promise in the docs/UI |
| **Medium** | Real defect or maintainability/UX problem, less likely to be hit |
| **Low** | Polish, cleanup, minor inconsistency |

---

## 2. Executive summary

| Area | Critical | High | Medium | Low |
|---|---:|---:|---:|---:|
| Crash | 1 | 0 | 0 | 0 |
| Build / config | 0 | 4 | 5 | 3 |
| Data layer | 0 | 1 | 3 | 3 |
| Logic | 1 | 6 | 6 | 3 |
| State / architecture | 0 | 0 | 4 | 4 |
| Performance | 0 | 0 | 3 | 1 |
| UI / UX | 0 | 5 | 12 | 8 |
| Accessibility | 0 | 3 | 3 | 1 |

### The five things to look at first

1. **Crash (C-01):** `android-app/` and `app/` share the same package, DB filename and Room version (`1`) but have *different schemas and identity hashes*. Installing the newer APK over the older one makes Room throw `IllegalStateException: Room cannot verify the data integrity` on the first query — i.e. right after launch. **Quick test:** uninstall the app or clear its storage and reinstall.
2. **Streaks and consistency score are wrong for every daily habit (L-01):** daily habits are saved with `windowDays = 1`, and the stats function uses that as its date range, so score is 0 % or 100 % and streak is at most 1.
3. **The day-boundary feature is only half-wired (L-03 / L-04 / L-05):** never loaded on startup, ignored by the matrix and stats, and frozen at midnight rollover.
4. **The main screen is not the screen your plan describes (§13):** the plan says "Today" is a one-tap checklist opened 90 % of the time; the app's "Today" tab is a 31-column spreadsheet with 26 dp tap targets.
5. **Three conflicting design directions (U-01):** plan (calm Material 3), design doc (dark, brutalist, spreadsheet), and code (pastel "Wantui" / Habit Garden with emojis). The design doc explicitly says *no cutesy UI* — the code has 🔥 ☀ ✦ 🔒.

---

## 3. Project layout — which module is actually running

```
Daylatch/
├── settings.gradle.kts          → include(":app")
├── build.gradle.kts             → AGP 8.5.2, Kotlin 1.9.22, KSP 1.9.22-1.0.17
├── gradle.properties            → Termux aapt2 override (!)
├── app/                         ← CURRENT module (edited 20 Sep 15:38, APK built 15:45)
│   └── src/main/java/com/prasad/daylatch/
│       ├── MainActivity.kt      (791 lines: ViewModel + all screens + dialogs)
│       ├── MatrixScreen.kt      (381 lines)
│       └── data/ {Models, AppDatabase, HabitRepository}.kt
├── android-app/                 ← OLD copy (18 Sep), its own settings/build files, own APK
│   └── src/.../ {MainActivity.kt (90 lines, dense), Screens.kt, StoreTest.kt}
└── Docs/                        ← plan, design system, React/Tailwind reference
```

| | `app/` (root) | `android-app/` (old) |
|---|---|---|
| Package / applicationId | `com.prasad.daylatch` | `com.prasad.daylatch` (**identical**) |
| Room DB file | `daylatch.db` | `daylatch.db` (**identical**) |
| Room version | 1 | 1 |
| Habit column for quota target | `targetPerWindow` | `target` |
| `habit_logs.note` | yes | no |
| `settings.theme` | yes | no |
| Identity hash | `ba379e741576fc97b81ee68f9c25b456` | `b4621eb226401641650b875b3f39a466` |
| Latest APK | 20 Sep 15:45 | 18 Sep 20:56 |

The `android-app/README.md` says it exists only because "the shared-storage workspace prevented safe in-place amendments to the initial root scaffold". That reason is gone, but the folder is still there — and it is the root cause of the crash below.

---

## 4. Crash analysis

### C-01 — Room identity-hash mismatch after installing over the older build  **[Critical · Likely]**

**Evidence**
- Both modules declare `@Database(version = 1, exportSchema = false)`.
- Both use `Room.databaseBuilder(..., "daylatch.db")` and the same `applicationId`.
- The generated `RoomOpenHelper` for each embeds a different identity hash (see §3 table), because the entity definitions differ.
- Android treats an APK with the same `applicationId` as an *upgrade* and keeps `/data/data/com.prasad.daylatch/databases/daylatch.db`.

**What happens at runtime**
1. Old APK creates `daylatch.db` with hash `b4621eb2…` and `PRAGMA user_version = 1`.
2. New APK opens the file. Version on disk (1) equals declared version (1), so **no migration runs**.
3. `RoomOpenHelper.onOpen()` compares stored hash with the compiled hash → mismatch.
4. Room throws:
   `java.lang.IllegalStateException: Room cannot verify the data integrity. Looks like you've changed schema but forgot to update the version number. You can simply fix this by increasing the version number.`
5. This fires on the **first database access**, which is when the ViewModel's `stateIn(...)` starts collecting `observeToday()` / `observeMatrix()` — i.e. immediately after the Activity appears. That matches your "crashes after launch" symptom.

**Why `fallbackToDestructiveMigration()` doesn't save you** (`AppDatabase.kt:61`)
It only applies when Room sees a *version change* (`onUpgrade` / `onDowngrade`). With equal versions there is no migration callback, so the fallback never triggers.

**How to confirm**
- Uninstall the app (or *Settings → Apps → Daylatch → Storage → Clear data*) and run again. If it launches, this was the cause.
- Or capture the stack trace (Appendix A) and look for `Room cannot verify the data integrity`.

**Recurrence risk (same mechanism, no second APK needed)**
Any time you edit an `@Entity` during development without bumping `version`, and a DB already exists on the device, you get the same crash. `Models.kt` and `AppDatabase.kt` were edited across 18–19 Sep, so intermediate builds of the *root* module may also have left an incompatible DB on the phone.

**Side note:** with `allowBackup="true"` (B-06), a Play/internal-test install may *restore* an old backed-up DB after reinstall and bring the crash back. Clearing data via system settings is safer than uninstall/reinstall in that scenario.

**Fix direction:** delete `android-app/`; during development clear data on every schema change; set `exportSchema = true` and start bumping the version with real migrations once you have data you care about.

### C-02 — Nothing else in the code crashes on a fresh install  **[Confirmed by reading; Hypothesis that no other crash exists]**

I traced the full launch path (`onCreate` → factory → ViewModel → repository → DAO flows → `Scaffold` → `MatrixScreen`) looking for:
- infinite-constraint layout crashes (nested scroll containers) — none; heights are bounded via `weight(1f)`,
- shared `ScrollState` misuse — valid pattern,
- nullable `Flow<Settings?>` handling — supported by Room,
- enum `rememberSaveable` — enums are `Serializable`, fine,
- manifest/theme/icon resolution — fine (verified in the built APK),
- Compose-compiler / Kotlin version mismatch — 1.5.8 ↔ 1.9.22 is a supported pair.

**If the app still crashes after a clean install, the cause is something I cannot see from static files — please send the `logcat` stack trace (Appendix A).**

### C-03 — Any Room / coroutine exception in a ViewModel action will crash the app  **[Medium · Confirmed]**

`updateStatusForDate`, `add`, `archive`, `saveBoundary` (`MainActivity.kt:91–107`) call `viewModelScope.launch { … }` with no `try/catch` and no `CoroutineExceptionHandler`. Disk-full, DB-locked, or a future migration error becomes an uncaught exception and kills the process instead of showing a message.

---

## 5. Build & configuration issues

### B-01 — Kotlin daemon crash in the build log  **[Medium · Confirmed]**
`.gradle/kotlin/errors/errors-1789899049692.log`: `DaemonCrashedException: Connection to the Kotlin daemon has been unexpectedly lost … killed by another process or the operating system, or by JVM crash.`
- Almost certainly memory/process-killing on Termux (Android's phantom-process killer or OOM).
- Gradle **fell back** to non-daemon compilation, which is why the APK still built at 15:45 — so this is *not* the app crash, but it makes builds flaky and slow.
- `gradle.properties` sets only `org.gradle.jvmargs=-Xmx2048m`; there is no `kotlin.daemon.jvmargs` and no `kotlin.compiler.execution.strategy`.

### B-02 — Hard-coded Termux `aapt2` path  **[High · Confirmed]**
`gradle.properties:4` → `android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2`
This makes the project **unbuildable on any other machine** (Android Studio, CI, a teammate, your examiner). It should live in `~/.gradle/gradle.properties` on the phone, not in the repo.

### B-03 — No Gradle wrapper  **[High · Confirmed]**
There is no `gradlew`, `gradlew.bat` or `gradle/wrapper/gradle-wrapper.properties`. The build depends on whatever Gradle happens to be installed (the cache shows `9.7.1`). Android Studio cannot sync reliably, and results differ between machines.

### B-04 — Gradle version is outside AGP 8.5.2's supported range  **[Medium · Likely]**
AGP 8.5.x officially targets Gradle 8.7. The build cache shows Gradle 9.7.1. It evidently worked (the APK exists), but it is outside the supported matrix and may break on the next AGP/Kotlin update. Pin a wrapper to a supported Gradle version, or upgrade AGP to the version that documents Gradle 9 support.

### B-05 — Stale duplicate module `android-app/`  **[High · Confirmed]**
Same package, same DB name, different schema, its own APK, its own `.gradle` and `build/`. It caused C-01. It also makes the zip ~10× larger than needed because `.gradle/` and `build/` folders are included in both trees.

### B-06 — `allowBackup="true"` with no backup rules  **[High · Confirmed]**
`app/src/main/AndroidManifest.xml:5`. Android Auto Backup will copy `daylatch.db` to the user's Google Drive backup and device-to-device transfer. There is no `android:dataExtractionRules` / `android:fullBackupContent` to exclude it. See §12 — this contradicts the app's own privacy statement.

### B-07 — Launcher icon problems  **[Medium · Confirmed]**
- `res/mipmap-xxxhdpi/ic_launcher.png` is **1254 × 1254 px, 1.1 MB** (a proper xxxhdpi icon is 192 × 192). Decoded, that is ~6.3 MB of bitmap for every launcher/recents render.
- It exists **only** in `xxxhdpi`; no other densities.
- **No adaptive icon** (`mipmap-anydpi-v26/ic_launcher.xml` with foreground/background) and no `android:roundIcon` — on Android 8+ launchers the icon is placed on a plate or masked awkwardly, which is the single most "amateur" visual cue on the home screen.
- `android:label="Daylatch"` is hard-coded rather than a string resource.

### B-08 — No resource files beyond one style  **[Medium · Confirmed]**
`res/values/` contains only `styles.xml`. No `strings.xml` (41 hard-coded `Text("…")` literals in `MainActivity.kt`, 6 in `MatrixScreen.kt`, 0 `stringResource` calls), no `colors.xml`, no dark-theme resources. Localisation, TalkBack phrasing and theming are all blocked by this.

### B-09 — Theme parent is the framework theme  **[Medium · Confirmed]**
`res/values/styles.xml`: `parent="android:Theme.Material.Light.NoActionBar"`.
- Gives a **black/dark status bar** under your white `TopAppBar`, and no control over status/navigation-bar colours or edge-to-edge.
- Window background before the first Compose frame is the framework default, not `BG` (`#FEFEFE`) — causes a brief colour flash at launch.
- No splash-screen theme (`androidx.core:core-splashscreen`) — on Android 12+ the system splash shows the oversized icon (B-07) on a default background.

### B-10 — No `buildTypes` / release configuration  **[Low · Confirmed]**
`app/build.gradle.kts` has no `release` block: no signing config, no `isMinifyEnabled`, no `isShrinkResources`. `buildFeatures.buildConfig = true` is enabled but never used.

### B-11 — `targetSdk = 34`  **[Low · Confirmed]**
Fine for local testing, but Google Play requires newer target levels for new apps/updates (35 as of Aug 2025; check the current requirement). Moving to 35+ also **forces edge-to-edge**, which the current UI (white top bar, black status bar, no inset handling beyond `Scaffold` padding) is not prepared for.

### B-12 — Compiler warnings and dead code  **[Low · Confirmed]**
- Unused imports — `MainActivity.kt`: `AnimatedVisibility`, `fadeIn`, `scaleIn`, `clickable`, `HapticFeedbackType`, `LocalHapticFeedback`, `java.time.format.TextStyle`, `java.util.Locale`. `HabitRepository.kt`: `ZoneId`.
- `HabitRepository.kt:23,25,33,37`: destructured `active` / `settings` variables are unused in some lambdas.
- `flatMapLatest` (`HabitRepository.kt:43`) is `@ExperimentalCoroutinesApi` and used without `@OptIn` (warning).
- Unused API: `HabitRepository.setStatus()` (line 51), `Settings.theme`, `HabitLog.note`, `HabitStatus.FAILED` (never written).

### B-13 — No tests in the current module  **[Medium · Confirmed]**
`app/src/test` does not exist. The only test (`StoreTest.kt`) is in the old module and references `Store.day(...)`, a class that doesn't exist in `app/`. Your project plan (§10) calls the streak/state-machine maths "the highest-value, lowest-effort things to test" and says the tests are what you'd demonstrate in a viva.

### B-14 — `exportSchema = false`  **[Medium · Confirmed]**
`AppDatabase.kt:52`. Without exported schema JSON you cannot write migration tests, cannot diff schema changes, and cannot see *why* a hash changed (which is exactly what bit you in C-01).

---

## 6. Data-layer (Room) issues

### D-01 — A new `AppDatabase` instance is created on every Activity creation  **[High · Confirmed]**
`MainActivity.kt:67` → `HabitRepository(AppDatabase.create(this))` runs in `onCreate`, and `AppDatabase.create` (`AppDatabase.kt:59`) is not a singleton.
- On rotation / theme change / language change / multi-window resize, the Activity is recreated, a **second** `RoomDatabase` opens the same file, and the first (still referenced by the retained ViewModel's repository) is never closed.
- Room logs a warning about multiple instances; two open connections/invalidation trackers mean flow updates written through one instance may not reach observers on the other.
- Only the *first* repository is ever used (ViewModel factory runs once), so the extra instances are pure leaks — but they are real file handles.

### D-02 — No foreign key from `habit_logs.habitId` to `habits.id`  **[Medium · Confirmed]**
`Models.kt:21–32`. Project plan §11 says: *"Deleting a habit — cascade-delete its HabitLog … rows (`@ForeignKey(onDelete = CASCADE)`) instead of leaving orphaned rows."* Not implemented. Currently you only archive, so orphans don't appear yet — but the moment you add delete (which the plan's "CRUD" requires) they will.

### D-03 — Missing index for date-only queries  **[Medium · Confirmed]**
`observeForDate(:date)` and `observeLogsInRange(:from,:to)` (`AppDatabase.kt:27,33`) filter on `date` alone. The only index is `(habitId, date)`, which cannot serve a `date`-only predicate efficiently → full table scan that grows with history. `observeToday` and the matrix run these on every DB change.

### D-04 — `PENDING` rows are written to disk  **[Medium · Confirmed]**
`MainActivity.kt:220–225` cycles `COMPLETED → SKIPPED → PENDING`, and `setStatusForDate` saves a `HabitLog` with `PENDING`. Result: one meaningless row per touched cell. In the state machine (plan §3.1) *Pending* is the **absence** of a resolved log. It also makes it impossible to distinguish "never touched" from "user cleared it".

### D-05 — Redundant / fragile type converters  **[Low · Confirmed]**
`AppDatabase.kt:15–20`. Room 2.6 stores enums natively; the custom converters are unnecessary, and `valueOf(value)` will throw for any unknown string (e.g. after you rename an enum constant), turning a data issue into a crash.

### D-06 — `saveSettings` overwrites the whole singleton row  **[Low · Confirmed]**
`MainActivity.kt:106` → `Settings(dayBoundaryHour = hour)` resets `theme` to its default every time the boundary is saved. Harmless today because `theme` is unused; a bug the moment you implement theming.

### D-07 — `createdAt` is ignored by every calculation  **[Low · Confirmed]**
Stats, matrix and monthly score treat every habit as if it existed since forever (see L-08, L-07). `createdAt` should bound the range.

---

## 7. Logic bugs

### L-01 — Daily habits always have a 1-day stats window → streak and score are broken  **[Critical · Confirmed]**
- `AddHabitDialog` saves daily habits with `window = 1` (`MainActivity.kt:683`: `val win = if (isDaily) 1 …`).
- `statsFor(habit, days = habit.windowDays)` (`HabitRepository.kt:66`) uses that as the look-back → `start = end.minusDays(0)`, i.e. **today only**.
- Consequences for every daily habit:
  - `expected = days = 1` → **score is 0 % or 100 %**, nothing in between.
  - `dailyStreak(logs, end)` only sees today's log → **streak ≤ 1** even after 30 perfect days.
  - Detail dialog shows "Completed 1 / Expected 1" forever.
  - Insights list shows "🔥 1 day streak" at best.
- This breaks the app's headline feature ("show the math"). The plan (§3.2) wants 7/30/90-day rolling windows — none exist.

### L-02 — Streak is anchored to *today*, so it resets to 0 every morning  **[High · Confirmed]**
`dailyStreak` (`HabitRepository.kt:83–89`) starts `cursor = end` (today). If today isn't ticked yet, `while (cursor in completedDates)` fails immediately → **streak = 0**, even with a 100-day run ending yesterday. Standard behaviour: allow the run to end yesterday while today is still open.

### L-03 — Saved day-boundary is never loaded into the ViewModel  **[High · Confirmed]**
`MainActivity.kt:89` → `var boundaryHour by mutableIntStateOf(0)`. Nothing reads `settings` from the DB into it. After every app restart:
- Settings screen shows "Currently: Midnight" and an input of `0`, although the DB may hold `3`.
- `SettingsScreen` keeps `editHour` in `rememberSaveable` initialised from that stale `0` (`MainActivity.kt:498`), and never re-syncs. Pressing Save writes `0` back and **silently destroys the saved setting**.

### L-04 — Day boundary is ignored almost everywhere  **[High · Confirmed]**
Only `observeToday` uses `habitDate(boundary)`. These use the raw calendar date instead:
- `MatrixScreen.kt:225` — "today" highlight in the header.
- `HabitRepository.kt:67` — `statsFor` uses `LocalDate.now()`.
- `MainActivity.kt:284` — Insights header "TODAY, Sunday 20 Sep".

**Night-owl example (boundary = 3 AM, it's 01:00 on the 21st):** the matrix highlights the 21st, the Insights checklist shows the 20th's statuses, the header says the 21st, and stats count from the 21st. Four screens, two different "todays".

### L-05 — "Today" freezes its date once collected  **[High · Confirmed]**
`observeToday()` (`HabitRepository.kt:19–27`) computes `habitDate(...)` *inside* the `flatMapLatest` lambda, which only re-runs when settings or the habit list change. Leave the app open past midnight (or past the boundary hour) and the Insights "Today" card keeps showing **yesterday's** logs until something else emits. There is no ticker/observer of the clock.

### L-06 — Month is frozen and cannot be changed  **[High · Confirmed]**
`MainActivity.kt:81` → `var currentMonth by mutableStateOf(YearMonth.now())` is set once when the ViewModel is created. No UI changes it (the design doc's month header with navigation was not built), and it doesn't roll over if the process lives across a month end. You can never look at last month's data — which makes history invisible even though it's stored.

### L-07 — Future dates (and dates before the habit existed) can be marked  **[High · Confirmed]**
`MatrixScreen.kt:292–295`: every cell is clickable, including days that haven't happened, and days before `habit.createdAt`. A user tapping ahead corrupts streaks/scores; the "Upcoming" legend entry implies those cells are inert.

### L-08 — Monthly score maths is wrong  **[Medium · Confirmed]**
`MainActivity.kt:402–409`:
- Denominator `habits.size * daysInMonth` includes **future days** → on the 5th of a 30-day month the maximum possible score is ~17 %.
- Includes days **before each habit was created**.
- Treats **quota habits as daily** (a "3× per week" habit can never score above ~43 %).
- Counts only `COMPLETED`; `SKIPPED` days are treated as failures even though the plan says skips are neutral.

### L-09 — Stats go stale  **[Medium · Confirmed]**
- Insights: `LaunchedEffect(habit.id)` (`MainActivity.kt:446`) runs once per habit; ticking a cell later does **not** recompute, so the list shows old score/streak until the tab is recreated.
- `items(habits)` (`:444`) has no `key`, so `remember`ed `stats` belongs to a list *position*: after archiving a habit the row below briefly shows the archived habit's numbers.
- `DetailDialog` (`:705`) computes once when opened.

### L-10 — "13:00 AM"  **[Medium · Confirmed]**
`MainActivity.kt:575`: `if (boundaryHour == 0) "Midnight" else "${boundaryHour}:00 AM"`. The input accepts 0–23, so hours 12–23 display as "12:00 AM" … "23:00 AM".

### L-11 — Quota habit design gaps  **[Medium · Confirmed]**
- No validation that `target ≤ window` (e.g. "10 times per 3 days" is accepted; fields allow up to 99).
- Rolling window from *today*, not calendar-week aligned; a "3×/week" habit's score jumps day to day.
- Blank "Times" silently becomes 1, blank "Per X days" silently becomes 7 (`MainActivity.kt:682–683`).
- Quota habits have **no streak concept** at all (`statsFor` sets streak = 0), so the streak line never appears for them.

### L-12 — `FAILED` state is never produced  **[High · Confirmed vs plan]**
Plan §3.1 describes a rollover that turns unresolved past days into `Failed` unless the quota still has slack. Nothing does this. The tap cycle can't reach `FAILED`, and no background/app-open job creates it. The red cell colour, the "✗" glyph and the `FAILED` enum value are dead UI.

### L-13 — Skipped days break daily streaks  **[Medium · Design decision]**
`dailyStreak` counts only `COMPLETED` (`HabitRepository.kt:84`). A `SKIPPED` day ends the streak. The plan says skip is neutral relative to failure; you need to decide what skip means for *daily* habits (pause the streak vs break it) and make code, UI copy and tests agree.

### L-14 — Rapid taps can be swallowed  **[Low · Likely]**
`onToggle` derives `newStatus` from the `logs` snapshot captured at composition (`MainActivity.kt:219–225`). Two quick taps before Room re-emits both compute the same next state, so the second tap appears to do nothing (or overwrites with the same value).

### L-15 — Add-habit validation is silent  **[Low · Confirmed]**
- Empty name: the "Add habit" button does nothing, no disabled state, no error text (`MainActivity.kt:680`).
- No maximum name length; no duplicate-name check.
- Settings: empty hour → saves `0`; `25` → silently clamped to `23`; no confirmation that anything saved.

### L-16 — No edit, delete, or un-archive  **[High · Confirmed vs plan]**
Plan §2.1 lists "Habit CRUD (create / edit / delete / archive)". The app has create and archive only. An archived habit disappears with **no way to get it back**, and a typo in a name is permanent.

### L-17 — Archive has no confirmation  **[Medium · Confirmed]**
`MainActivity.kt:772`: "Archive" sits beside "Close" in the detail dialog and executes immediately. Combined with L-16 it behaves like a delete.

### L-18 — Haptic feedback type doesn't match the spec  **[Low · Confirmed]**
`MatrixScreen.kt:293` always uses `TextHandleMove`. The design doc says `TextHandleMove` for cycling and `LongPress` when marking `Completed`.

---

## 8. ViewModel, state & architecture issues

### V-01 — `ColorScheme` rebuilt on every recomposition  **[Medium · Confirmed]**
`MainActivity.kt:129–143` calls `lightColorScheme(...)` inside `DaylatchApp` without `remember`. Each recomposition provides a new object through `MaterialTheme`, invalidating every composable that reads a theme colour. Combined with `val today by …collectAsStateWithLifecycle()` at the same level, every habit tick recomposes the whole app tree.

### V-02 — Business logic lives inside composables  **[Medium · Confirmed]**
- Status-cycle rule: inline lambda in `DaylatchApp` (`:219–224`).
- Monthly score: computed in the body of `InsightsScreen` (`:402–409`).
- Footer percentages: computed in `MatrixScreen` (`:338–341`).
Plan §4 says the repository owns "streak math, state machine logic". These can't be unit-tested where they are.

### V-03 — Compose state inside the ViewModel  **[Medium · Confirmed]**
`currentMonth`, `selected`, `boundaryHour` are `mutableStateOf` in `DaylatchViewModel`; `matrix` uses `snapshotFlow` to bridge back to a Flow. It works, but couples the VM to the Compose runtime; `MutableStateFlow` + a single `UiState` data class is the standard pattern and is easier to test.

### V-04 — No loading state; UI flashes "empty"  **[Medium · Confirmed]**
`stateIn(..., emptyList())` (`:79, :86`) means the first frame renders "no habits" until Room delivers data. The design doc asks for a shimmer here; today users briefly see an empty grid / "No habits yet".

### V-05 — Composables take the ViewModel directly  **[Low · Confirmed]**
`InsightsScreen(today, viewModel)` and `DetailDialog(habit, viewModel, …)` — prevents `@Preview`, UI tests and reuse. Pass state and lambdas instead.

### V-06 — Everything is in one 791-line file  **[Low · Confirmed]**
ViewModel, theme, 3 screens, 2 dialogs and helper composables share `MainActivity.kt`. The plan's own repo structure (§13) splits `ui/today`, `ui/habitdetail`, `ui/settings`.

### V-07 — Duplicated design tokens  **[Low · Confirmed]**
The same 13 colours are declared privately in both `MainActivity.kt:49–62` and `MatrixScreen.kt:37–49` (plus `TERTIARY_TEXT` only in the latter). Change one and the other silently drifts. There's no `Theme.kt`, `Color.kt`, `Type.kt`.

### V-08 — `combineLatestLogs` is a misleading private extension  **[Low · Confirmed]**
`HabitRepository.kt:41–45` is just `flatMapLatest { other(it).map { transform(it, …) } }` with a name that suggests `combineLatest`. It hides that the "other" flow is rebuilt on every upstream emission.

---

## 9. Performance issues

### P-01 — The matrix composes every cell eagerly  **[Medium · Confirmed]**
`MatrixScreen.kt:255–326`: plain `Column`/`Row` + `forEach` (not lazy). Each cell has its own `animateColorAsState`, `clickable`, and `clip`. 10 habits ≈ 310 cells; 20 habits ≈ 620, plus the footer — all composed and animated on first frame and on every log change. Expect jank on low-end phones and slow first paint.

### P-02 — `logsMap` recomputed every recomposition  **[Medium · Confirmed]**
`MatrixScreen.kt:67–69`: `groupBy` + `associateBy` + `LocalDate.parse` for every log, with no `remember(logs)`.
Also `LocalDate.now()` is called **once per header cell** (`:225`) and `currentMonth.atDay(day)` per cell per row.

### P-03 — Monthly score is O(habits × days × logs) on each recomposition  **[Medium · Confirmed]**
`MainActivity.kt:402–409` calls `logs.any { … }` inside two nested loops, plus `date.toString()` allocations, inside a composable body.

### P-04 — Theme rebuild triggers app-wide recomposition  **[Low]**
See V-01.

---

## 10. UI / UX problems (professional-polish review)

### 10.1 Visual identity & consistency

**U-01 — Three conflicting design directions  [High]**

| Source | Direction |
|---|---|
| `habit-tracker-project-plan.md` §6 | Material 3, minimal chrome, calm, **no gamification** |
| `Habit_Tracker_UI_Design_System.md` | **Dark-first, brutalist spreadsheet**, GitHub-style greens, Inter / JetBrains Mono, 32 dp cells with 4 dp radius, "no cutesy UI, no avatars, no badges" |
| Actual code (from `HabitDashboard.tsx` / `index.css`) | **Pastel light "Wantui"** palette, rounded 12–16 dp cards, "Habit Garden" tone, emoji decoration |

The code contradicts the design doc's explicit rules with `🔥` streak flames (`MainActivity.kt:480`), `☀` (`:297`), `✦` (`:398`), `🔒` (`:515`). Pick **one** direction; a professional app has one visual language.

**U-02 — Design tokens drifted during the Android port  [High]**
The React reference defines `--primary-foreground: #423D53` (dark plum text on the green fill). The Android code instead uses `PRIMARY_TEXT = #3F8445` (green text on green fill) for buttons, FAB, ticks and nav. That single change turns a **7.55 : 1** pair into a **3.32 : 1** pair (see contrast table in §10.2). Similarly the reference's `--destructive-foreground: #2F2F2F` (4.51 : 1 on the pink) became white text (2.97 : 1).

**U-03 — Elevation and colour usage are inconsistent  [Medium]**
Insights: card 1 = `SECONDARY` flat, card 2 = `CARD` with 2 dp shadow, habit rows = `CARD` with 1 dp shadow. Settings: `TERTIARY` flat, `CARD` 2 dp, `CARD` 1 dp. Same component type, three treatments, no system.

**U-04 — Typography is default Roboto + system monospace  [Medium]**
No `Typography` passed to `MaterialTheme`; 39 hard-coded `fontSize` values in `MainActivity.kt` and 14 in `MatrixScreen.kt` (e.g. 9, 10, 11, 12, 13, 14, 15, 16, 18, 20, 24, 48 sp) — no type scale. Both the reference (Nunito + JetBrains Mono) and the design doc (Inter + JetBrains Mono) specify fonts that are **not bundled** in the app. Numbers use `FontFamily.Monospace` (system Droid Sans Mono), not the specified face.

**U-05 — Text glyphs used as icons  [High]**
- Bottom nav: `◫`, `◈`, `⚙` (`MainActivity.kt:184–188`) — Unicode geometric shapes that render differently (or as tofu ▯) across manufacturers' fonts, sit on different baselines, and have no accessible name.
- FAB is the text `"+"` (`:172`).
- Cells: `✓`, `–`, `✗`, `□` (`MatrixScreen.kt:298–322`).
- Decoration: `☀`, `✦`, `🔥`, `🔒`.
Use vector icons (Material Symbols / your own drawables) at consistent size and stroke weight. Note the project has no `material-icons` dependency and no drawable resources at all.

**U-06 — Status bar / window chrome  [Medium]**
Framework theme (B-09) → dark status bar over a white `TopAppBar`; no `enableEdgeToEdge()`; navigation bar colour uncontrolled. The result looks like a default template, not a designed app.

**U-07 — Light theme only  [Medium]**
`lightColorScheme` is hard-wired (`MainActivity.kt:129`). The design doc says dark is the **default/preferred** mode; the plan has a `theme` setting ("system"); the DB has a `theme` column. None of it is wired up, so phones in dark mode get a blinding white app.

### 10.2 Contrast (computed WCAG ratios)

| Pair (where used) | Ratio | Target | Verdict |
|---|---:|---:|---|
| `PRIMARY_TEXT #3F8445` on `PRIMARY #A5EDA8` (✓ in done cells, primary buttons, FAB, nav indicator) | **3.32 : 1** | 4.5 (text) | Fail |
| `PRIMARY_TEXT` on nav indicator (`PRIMARY` @ 20 %) | 4.09 : 1 | 4.5 | Fail |
| `PRIMARY_TEXT` on `TERTIARY #D0EAED` | 3.63 : 1 | 4.5 | Fail |
| `ACCENT_TEXT #B45974` on `ACCENT #FEDFE9` (– in skipped cells) | **3.67 : 1** | 4.5 | Fail |
| White ✗ on `DESTRUCTIVE #D87993` (failed cells) | **2.97 : 1** | 4.5 | Fail |
| `TERTIARY_TEXT #637A7D` on `MUTED` (50–79 % footer values, 9 sp) | 4.16 : 1 | 4.5 | Fail |
| `MUTED_FG` @ 40 % on `MUTED` (the `□` placeholder) | **1.71 : 1** | 3 (graphics) | Fail |
| Done cell `#A5EDA8` vs white card | **1.37 : 1** | 3 (graphics) | Fail |
| Pending cell `#F5F4F8` vs white card | **1.09 : 1** | 3 (graphics) | Fail |
| Grid line `BORDER #D7D2E1` vs white | 1.48 : 1 | 3 (graphics) | Fail |
| `PRIMARY_TEXT` on white (labels, %) | 4.57 : 1 | 4.5 | Pass (barely) |
| `MUTED_FG #6E687C` on white | 5.34 : 1 | 4.5 | Pass |
| `ACCENT_TEXT` on white (Archive button) | 4.54 : 1 | 4.5 | Pass (barely) |
| *Reference:* `#423D53` on `PRIMARY` | 7.55 : 1 | 4.5 | Pass |
| *Reference:* `#423D53` on `ACCENT` | 8.37 : 1 | 4.5 | Pass |

Takeaway: the matrix — the hero screen — has near-invisible cell boundaries and tiny, low-contrast glyphs. The plan (§6) also says "don't rely on color alone for state"; today, colour is the only *visible* differentiator, since the glyphs are 12 sp and low contrast.

### 10.3 The matrix ("Today" tab) screen

**U-08 — Wrong primary screen  [High]**
Plan §6: *"Today — today's habits as a checklist, one tap to mark Completed / Skipped. This is the screen opened 90 % of the time; keep it to zero scrolling."*
Actual: the "Today" tab is a month grid. The only tappable habit UI for logging is the 26 dp cells. The Insights tab has a "today" checklist but its rows are **not tappable** (`MainActivity.kt:305–340`). There is no Complete/Skip control anywhere else.

**U-09 — Tap targets are too small  [High]**
`Modifier.size(30.dp).padding(2.dp)…clickable` (`MatrixScreen.kt:286–295`) → the clickable area is **26 × 26 dp** (padding is applied *before* `clickable`). Material minimum is 48 dp; the React reference itself uses `min-h-11` (44 px). Mis-taps on a 31-column grid are guaranteed.

**U-10 — Header and footer are not sticky  [High]**
The date header row and SCORE footer are *inside* the vertically-scrolling columns (`MatrixScreen.kt:211–216, 217–252, 329–359`). With more than ~10 habits the header scrolls away and you lose which column is which day. The design doc §3 explicitly specifies a sticky header and a sticky summary footer.

**U-11 — Doesn't open on today  [Medium]**
`rememberScrollState()` starts at 0 (`:63`). On the 25th the user must scroll right every time to reach today. Auto-scroll to today's column on first composition.

**U-12 — No month navigation and no year  [Medium]**
Month label shows only the month name (`:85–92`), no year, no previous/next controls (see L-06). The design doc wants "September 2026" left and a gear icon right.

**U-13 — Weak "today" indicator  [Medium]**
Today is shown only by green text on the day number (`:238, 245`). No column highlight, no top-border accent (design doc §3, Screen 1).

**U-14 — Legend is incomplete/misleading  [Medium]**
`Done`, `Skipped`, `Upcoming` (`:114–116`) — no entry for `Failed` or for **past, unresolved** cells, which use the same `MUTED` colour as "Upcoming".

**U-15 — Noise from `□` placeholders  [Medium]**
Every pending cell prints a 10 sp `□` at 40 % opacity (`:317–321`). On a 20-habit grid that's hundreds of faint boxes; a clean empty cell (or a subtle dot) is calmer and reads as "professional data table".

**U-16 — Grid doesn't look like a table  [Medium]**
Design doc §4.2: crisp 1 px lines on the *right and bottom* of each cell via `drawBehind`. Actual: floating 6 dp-radius chips with a horizontal line per row only. No column separators, no week separators, no weekend shading.

**U-17 — FAB overlaps content  [Medium]**
The FAB floats over the bottom-right of the matrix surface and covers the last row / footer cells. The layout doesn't reserve space for it.

**U-18 — No empty state on the matrix  [Medium]**
With zero habits the matrix shows an empty card with only the header and no message. "No habits yet — tap +" exists only on the Insights tab (`:343–350`).

**U-19 — Name column limitations  [Low]**
`NAME_WIDTH = 140.dp`, one line, ellipsised; no way to read a long name except by opening the detail. Row height is a fixed 40 dp, so larger system font sizes will clip (see §11).

**U-20 — Habit name tap target is invisible  [Low]**
The hint text "tap names for stats" is the only affordance; there's no chevron or pressed state distinct from cells.

### 10.4 Navigation & information architecture

**U-21 — Tab names don't match content  [High]**
- Tab **"Today"** → shows the **month** matrix.
- Tab **"Insights"** → opens with a **"TODAY"** card (date, checklist, today's score) before the monthly score.
The user's mental model breaks on the first switch. The app title bar subtitle "Small steps, every day" is static on every tab.

**U-22 — Top bar duplicates and wastes space  [Low]**
A two-line top bar ("Daylatch" + tagline) on every tab, then *another* header inside the matrix ("Your month at a glance" + hint), then a legend, then the card — four stacked headers before any data on a phone screen.

**U-23 — Settings is a tab, not a gear  [Low]**
Design doc puts a gear icon in the top bar. Not a defect, but it takes one of only three bottom slots.

### 10.5 Insights screen

**U-24 — "Habit Streaks" section isn't about streaks  [Medium]**
It lists consistency % and (rarely) a streak; because of L-01/L-09 the numbers are wrong or stale.

**U-25 — Decorative, low-information cards  [Medium]**
The "Monthly Score" card shows one big number, "You're showing up", and a `✦`. The design doc's "show the math" principle wants ratios (`23 / 31`), sparklines, 7/30/90-day rolling windows, longest streak, and a heatmap — none present. `Docs/HabitDashboard.tsx.txt` even includes `chart.js` line/doughnut charts that were not ported.

**U-26 — "Today's score" repeats the Today tab  [Low]**
Checklist rows show a "Done / Pending" label but can't be tapped (U-08).

**U-27 — Same colour for good/neutral states  [Low]**
Score colour thresholds (≥80 green, ≥50 plain, else pink) use a pink `ACCENT_TEXT` for low scores — pink reads as "brand accent", not "needs attention", and the plan asks to avoid guilt-tripping, so the semantics need a deliberate decision.

### 10.6 Settings screen

**U-28 — Boundary input is a free-text field  [Medium]**
"Hour (0–23)" text field with no number keyboard (`KeyboardOptions(imeAction = Done)` only, `:553`), no stepper/slider/time-picker, no validation message, no save confirmation. The old module had a proper − / + stepper.

**U-29 — Missing settings the plan/design promise  [Medium]**
- Theme (system/light/dark) — plan §5/§6, DB column exists.
- **Export to SQLite/CSV** ("massive, tactile button" — design doc §6) and encrypted export/import (plan §9).
- Reminder settings (plan §7).

**U-30 — "Data Vault" overstates the guarantee  [High]** — see §12.

**U-31 — About card has a hard-coded version  [Low]**
`"Daylatch v1.0"` (`:595`) should read `BuildConfig.VERSION_NAME` (you already enabled `buildConfig`).

### 10.7 Dialogs

**U-32 — Add-habit dialog  [Medium]**
- Number fields use the text keyboard (`:659–672`).
- Chip labels "Daily" / "Quota" with no explanation of what "Quota" means; field labels "Times" and "Per X days" read awkwardly ("Times … Per X days").
- No preview line ("3 times every 7 days").
- Disabled-state feedback missing (L-15).
- No optional reminder time (plan §6 screen 4).

**U-33 — Detail dialog  [Medium]**
- Uses an `AlertDialog` for what the design doc calls a full "Habit Detail & Analysis" screen (Screen 2: heatmap, longest streak, target, 30-day trend).
- Shows a spinner while stats load (`CircularProgressIndicator`) — design doc says "no spinning circles".
- Stat blocks use pastel fills with 10 sp labels; content isn't scrollable at large font scales.
- "Archive" (destructive) and "Close" are adjacent text buttons with similar weight.

**U-34 — Stat blocks arbitrarily hide "Streak"  [Low]**
`if (s.streak > 0)` (`:754`) makes the row change shape (2 vs 3 blocks) between habits, which looks like a layout bug.

### 10.8 Motion & feedback

**U-35 — Animations from the spec are missing  [Low]**
Imports for `AnimatedVisibility`, `fadeIn`, `scaleIn` are present but unused (design doc §5.2: animate the checkmark). No shimmer loading (§5.3). Only cell colour animates (`tween(200)`).

**U-36 — No snackbars / undo  [Medium]**
Archive, add, save-boundary, and status toggles give no confirmation or undo. An accidental archive (L-17) can't be recovered or even acknowledged.

**U-37 — Ripple / pressed states on cells are hidden  [Low]**
`clickable` is applied after `clip` + `background`, and cell size is tiny, so the ripple is barely perceptible; haptics are the only feedback.

### 10.9 Launcher / first impression

**U-38 — Icon and splash  [Medium]**
See B-07 / B-09: oversized non-adaptive icon, no splash theme, dark status bar, background flash — the first 2 seconds of the app look unpolished before any of your UI appears.

---

## 11. Accessibility

Plan §6: *"TalkBack labels on every interactive element, respect the system font-scale setting, and don't rely on color alone for state."* The current code does none of the three.

**A-01 — No content descriptions or semantics anywhere  [High]**
`contentDescription` occurrences: **0** in both UI files. `semantics {}` occurrences: **0**. TalkBack reads a cell as "white square" / "check mark" / "en dash" with no habit name or date, and the nav icons as "white square with lower right shadow" etc.

**A-02 — Status conveyed by colour + tiny glyph  [High]**
See contrast table: glyphs are 12 sp at 3–3.7 : 1; cell fills are ~1.1–1.4 : 1 against the card. Colour-blind users (and anyone in sunlight) can't distinguish Done / Skipped / Pending.

**A-03 — Touch targets 26 dp  [High]**
See U-09. Fails Android/WCAG target-size guidance.

**A-04 — Tiny text  [Medium]**
11 of 39 font sizes in `MainActivity.kt` and 8 of 14 in `MatrixScreen.kt` are ≤ 11 sp (day letters and footer values are **9 sp**; section labels **10 sp**).

**A-05 — Fixed dp row heights with sp text  [Medium]**
Header 36 dp, rows 40 dp, footer 32 dp (`MatrixScreen.kt`). At 130–200 % font scale, text overflows/clips and the left (names) and right (grid) columns can fall out of alignment.

**A-06 — Cells lack role / click label  [Medium]**
No `Modifier.semantics { role = Role.Checkbox … }`, no `onClickLabel` ("Mark done" / "Skip" / "Clear"), no state description ("Completed, 12 September").

**A-07 — Emoji in text  [Low]**
"🔥 5 day streak", "🔒 DATA VAULT" — screen readers speak the emoji name; keep them out of accessible text.

---

## 12. Privacy claims audit

The Settings screen says (`MainActivity.kt:518`):
> "This app does not have internet permissions. Your data physically cannot leave this device."

| Claim | Reality |
|---|---|
| "No internet permission" | **True** — I decoded the merged manifest inside the APK; the only permission is the auto-generated `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. |
| "Data physically cannot leave this device" | **Not accurate.** `allowBackup="true"` with no exclusion rules lets Android Auto Backup upload the database to the user's Google account and lets device-to-device transfer copy it. Also a user can share a screenshot, and any future export feature will move data off-device. |

Also plan §9 lists SQLCipher (optional) and passphrase-encrypted export; neither exists. Either set `allowBackup="false"` (or add `dataExtractionRules`) and keep the claim, or soften the wording to "The app never connects to the internet."

---

## 13. Spec vs implementation gap table

Legend: ✅ done · ⚠️ partial/wrong · ❌ missing

### Project plan (`habit-tracker-project-plan.md`)

| Plan item | Status | Notes |
|---|:--:|---|
| Habit create | ✅ | |
| Habit edit | ❌ | L-16 |
| Habit delete | ❌ | L-16 |
| Habit archive | ⚠️ | No confirm, no un-archive (L-17) |
| Daily + quota habits | ⚠️ | Quota has no streak, no validation (L-11) |
| 4-state machine | ⚠️ | `FAILED` never produced (L-12); `PENDING` stored (D-04) |
| Pending → Failed rollover | ❌ | §3.1 |
| Streak engine, 7/30/90-day windows | ❌ | Only one window; broken for daily (L-01, L-02) |
| Consistency score = completions ÷ expected | ⚠️ | Formula right, inputs wrong (L-01) |
| Day boundary (configurable) | ⚠️ | Saved but not loaded/used consistently (L-03/04/05) |
| Timestamps stored in UTC | ⚠️ | `completedAtUtc` yes; `date` is local string (matches plan's "boundary-adjusted ISO date") |
| Room, no network | ✅ | Verified in APK manifest |
| `(habitId,date)` composite index | ✅ | Made unique |
| FK cascade on delete | ❌ | D-02 |
| **Today screen: checklist, one tap, zero scrolling** | ❌ | U-08 |
| Habit detail with **heatmap** | ❌ | Only an AlertDialog with 3 numbers |
| Routine builder (Phase 2) | ❌ | Not started (acceptable — Phase 2) |
| Add/Edit habit incl. reminder time | ⚠️ | Add only, no reminder |
| Settings: boundary, theme, export/import | ⚠️ | Boundary only (buggy) |
| Reminders / notifications (Phase 2) | ❌ | Acceptable — Phase 2 |
| Accessibility (TalkBack, font scale, not colour-only) | ❌ | §11 |
| Unit tests for score/state/boundary | ❌ | B-13 |
| DAO tests (in-memory Room) | ❌ | |
| Repo structure (`ui/today`, `ui/settings`…) | ❌ | V-06 |
| Optional SQLCipher / encrypted export | ❌ | §12 |

### UI design system (`Habit_Tracker_UI_Design_System.md`)

| Spec item | Status | Notes |
|---|:--:|---|
| Dark-first palette | ❌ | Light pastel only (U-07) |
| Inter / Roboto + JetBrains/Roboto Mono | ⚠️ | Roboto + system mono; no bundled fonts |
| 4-state colours (green/transparent/grey/crimson) | ⚠️ | Different palette; `FAILED` unreachable |
| Top bar "September 2026" + gear | ❌ | U-12 |
| Sticky left column | ✅ | |
| Sticky header row | ❌ | U-10 |
| Sticky summary footer with sparkline | ❌ | Footer scrolls; no sparkline |
| Cells 32 dp, 4 dp radius, 1 dp gap | ⚠️ | 30/26 dp, 6 dp radius |
| Today highlighted with top-border accent | ❌ | U-13 |
| Tap cycles Pending→Completed→Skipped→Pending | ✅ | |
| Haptics: LongPress for Completed | ❌ | L-18 |
| Habit Detail: 48 sp hero, streaks, **longest streak**, quota, heatmap, trend chart | ⚠️ | Hero + current streak only (U-33) |
| Routine builder Gantt | ❌ | Phase 2 |
| Pixel-perfect `drawBehind` borders on right+bottom | ⚠️ | Bottom only (U-16) |
| `AnimatedVisibility` checkmark | ❌ | U-35 |
| Shimmer loading, no spinners | ❌ | V-04, U-33 |
| "Data Vault" section | ⚠️ | Present, claim overstated (§12) |
| Big "Export to SQLite/CSV" button | ❌ | U-29 |
| "No cutesy UI, no avatars, no badges" | ❌ | Emojis, ✦, "You're showing up" (U-01) |

---

## 14. Testing gaps

Nothing is tested in the current module. These are the highest-value cases, all pure functions (no Android needed), matching plan §10:

**`habitDate(boundaryHour, now)`**
- boundary 0 at 00:00 → same day; at 23:59 → same day.
- boundary 3, now 01:00 → **previous** day; now 03:00 → same day; now 02:59 → previous day.
- boundary 23, now 22:59 → previous day; 23:00 → same day.
- Out-of-range hours (−1, 24, 99) are coerced (line 78 uses `coerceIn`).

**`consistencyScore(completed, expected)`**
- expected 0 → 0; completed > expected → capped at 1.0; 3 of 7; exactly met.

**`dailyStreak`**
- Empty; only today; run ending yesterday with today open (should not be 0 — currently is, L-02); gap in the middle; skipped day in the middle (decide expected result — L-13); 100+ day run.

**`statsFor`**
- Daily habit with 30 completed days → expect 100 % over 30-day window and streak 30 (currently 1 — L-01).
- Quota 3/7: 0, 2, 3, 5 completions.
- Habit created 2 days ago: window must clamp to `createdAt`.

**Status cycle (extract it from the composable)**
- PENDING→COMPLETED→SKIPPED→PENDING; FAILED handling; future date is rejected.

**Room (in-memory)**
- `logsInRange` boundaries inclusive on both ends; unique `(habitId,date)` REPLACE behaviour; `observeToday` re-emits when the clock passes the boundary.

**Schema**
- With `exportSchema = true`: an `AutoMigration`/`MigrationTestHelper` test so C-01 can't recur silently.

---

## 15. Things I checked that are fine

So you don't waste time on these:

- `AndroidManifest.xml`: launcher activity exported correctly; **no `INTERNET` permission** (verified in the *built* manifest).
- Kotlin 1.9.22 ↔ Compose compiler 1.5.8 ↔ KSP 1.9.22-1.0.17 ↔ Room 2.6.1 — a supported combination; KSP generated `AppDatabase_Impl`, `HabitDao_Impl`, `SettingsDao_Impl` successfully.
- `Flow<Settings?>` from Room for a missing singleton row — supported.
- `rememberSaveable { mutableStateOf(Tab.TODAY) }` with a private enum — fine (enums are `Serializable`).
- Two `verticalScroll` modifiers sharing one `ScrollState` to sync the names column and the grid — a valid, well-known pattern; the heights on both sides (36 / 40 / 32 dp) are consistent.
- `LinearProgressIndicator(progress = { … })` lambda overload — available in Material3 1.2 (BOM 2024.06).
- `Tab.entries` — valid on Kotlin 1.9.
- Unique index on `(habitId, date)` plus `OnConflictStrategy.REPLACE` — works correctly for the one-log-per-day rule.
- `minSdk 26` + `java.time` — no desugaring needed.
- The APK truly reflects your current source (strings and schema hash verified inside the dex).

---

## 16. Recommended fix order

**Step 0 — Unblock (10 min)**
- [ ] Clear app data / uninstall, reinstall → does it launch? (C-01)
- [ ] If not, capture logcat (Appendix A) and send the stack trace.

**Step 1 — Stop the bleeding**
- [ ] Delete `android-app/` (B-05, C-01).
- [ ] Set `allowBackup="false"` or add extraction rules (B-06); fix the Data Vault wording (§12).
- [ ] Make `AppDatabase` a singleton / create it once in `Application` (D-01).
- [ ] Turn on `exportSchema`, decide your versioning rule (B-14).
- [ ] Wrap ViewModel actions in error handling (C-03).

**Step 2 — Make the numbers right (the app's core promise)**
- [ ] Fix daily-habit window / rolling 7-30-90 windows (L-01).
- [ ] Let streaks run through yesterday (L-02).
- [ ] Load boundary from DB; use one "today" everywhere; make "today" reactive to the clock (L-03, L-04, L-05).
- [ ] Block future dates and pre-creation dates (L-07).
- [ ] Correct monthly score denominator (L-08).
- [ ] Refresh stats when logs change; add `key`s (L-09).
- [ ] Fix "13:00 AM" (L-10); validate quota inputs (L-11, L-15).
- [ ] Decide skip semantics (L-13) and implement `FAILED` rollover (L-12).
- [ ] Add unit tests from §14 as you go.

**Step 3 — Complete the promised features**
- [ ] Edit / delete / un-archive habit, with confirmation + undo (L-16, L-17, U-36).
- [ ] Month navigation with year (L-06, U-12).
- [ ] Real "Today" checklist screen (U-08).

**Step 4 — Make it look professional**
- [ ] Choose one design direction; remove conflicting decoration (U-01).
- [ ] Single `Theme.kt` with colour, type, shape tokens; light + dark; fix token drift and contrast (U-02, U-04, U-07, V-07, §10.2).
- [ ] Replace glyphs with vector icons (U-05); proper adaptive icon + splash + edge-to-edge (B-07, B-09, U-06).
- [ ] Matrix: sticky header/footer, auto-scroll to today, larger hit area, cell borders, remove `□` noise, better legend, FAB clearance, empty state (U-09 … U-18).
- [ ] Rename tabs / restructure Insights (U-21 … U-27).
- [ ] Settings: stepper/time picker, export button, theme (U-28, U-29).
- [ ] Dialog polish (U-32, U-33).
- [ ] Accessibility pass: semantics, descriptions, font-scale-safe heights (§11).

**Step 5 — Build hygiene**
- [ ] Add Gradle wrapper; move Termux `aapt2` override out of the repo (B-02, B-03).
- [ ] Add `kotlin.compiler.execution.strategy=in-process` / daemon memory limits for Termux (B-01).
- [ ] Extract strings to `strings.xml` (B-08); remove unused imports/dead API (B-12).
- [ ] Split `MainActivity.kt` into `ui/*` packages (V-06).

---

## Appendix A — How to get the real crash log

You are building in Termux, which can't read other apps' logs. Use one of:

1. **Android Studio Logcat** — connect the phone, filter by package `com.prasad.daylatch`, or search for `FATAL EXCEPTION`.
2. **adb over Wi-Fi from a PC** (Developer options → Wireless debugging):
   ```
   adb logcat -c
   adb shell am start -n com.prasad.daylatch/.MainActivity
   adb logcat -d | grep -E "FATAL|AndroidRuntime|Room|Daylatch"
   ```
3. **On-device:** Developer options → *Take bug report* → share the zip; the crash appears under `FATAL EXCEPTION`.

Useful commands while testing C-01:
```
adb shell pm clear com.prasad.daylatch      # wipe data, keep app
adb uninstall com.prasad.daylatch           # remove completely
```

**What to send me:** the block starting at `FATAL EXCEPTION: main` through the last `Caused by:` line.

---

## Appendix B — File / line index

| File | Lines | Issue IDs |
|---|---|---|
| `gradle.properties` | 4 | B-02 |
| `settings.gradle.kts` / root | — | B-03, B-04 |
| `app/build.gradle.kts` | whole | B-10, B-11, B-13 |
| `app/src/main/AndroidManifest.xml` | 5 | B-06, B-07 |
| `res/values/styles.xml` | 1–3 | B-08, B-09, U-06 |
| `res/mipmap-xxxhdpi/ic_launcher.png` | — | B-07 |
| `data/Models.kt` | 10–39 | D-02, D-04, D-06, D-07 |
| `data/AppDatabase.kt` | 15–20 | D-05 |
| `data/AppDatabase.kt` | 27, 33 | D-03 |
| `data/AppDatabase.kt` | 52 | B-14 |
| `data/AppDatabase.kt` | 59–61 | C-01, D-01 |
| `data/HabitRepository.kt` | 19–27, 41–45 | L-05, V-08 |
| `data/HabitRepository.kt` | 51–54 | B-12 (unused `setStatus`) |
| `data/HabitRepository.kt` | 66–74 | L-01, L-04 |
| `data/HabitRepository.kt` | 83–89 | L-02, L-13 |
| `MainActivity.kt` | 49–62 | V-07 |
| `MainActivity.kt` | 67 | D-01 |
| `MainActivity.kt` | 79–107 | C-03, V-03, V-04 |
| `MainActivity.kt` | 81 | L-06 |
| `MainActivity.kt` | 89 | L-03 |
| `MainActivity.kt` | 129–143 | V-01, U-07 |
| `MainActivity.kt` | 164–175, 183–189 | U-05, U-17 |
| `MainActivity.kt` | 219–225 | L-14, V-02, D-04 |
| `MainActivity.kt` | 284 | L-04 |
| `MainActivity.kt` | 297, 398, 480, 515 | U-01, A-07 |
| `MainActivity.kt` | 305–340 | U-08, U-26 |
| `MainActivity.kt` | 402–409 | L-08, P-03, V-02 |
| `MainActivity.kt` | 444–448 | L-09 |
| `MainActivity.kt` | 498 | L-03 |
| `MainActivity.kt` | 518 | §12 |
| `MainActivity.kt` | 553, 659–672 | U-28, U-32 |
| `MainActivity.kt` | 575 | L-10 |
| `MainActivity.kt` | 595 | U-31 |
| `MainActivity.kt` | 680–685 | L-11, L-15 |
| `MainActivity.kt` | 705, 754, 763, 772 | L-09, U-33, U-34, L-17 |
| `MatrixScreen.kt` | 37–52 | V-07 |
| `MatrixScreen.kt` | 63, 85–92 | U-11, U-12 |
| `MatrixScreen.kt` | 67–69 | P-02 |
| `MatrixScreen.kt` | 114–116 | U-14 |
| `MatrixScreen.kt` | 211–252, 329–359 | U-10 |
| `MatrixScreen.kt` | 225 | L-04, P-02, U-13 |
| `MatrixScreen.kt` | 255–326 | P-01 |
| `MatrixScreen.kt` | 286–295 | U-09, L-07, U-37, A-03 |
| `MatrixScreen.kt` | 293 | L-18 |
| `MatrixScreen.kt` | 298–322 | U-05, U-15, A-01, A-02 |
| `.gradle/kotlin/errors/*.log` | — | B-01 |
| `android-app/**` | — | B-05, C-01 |

---

*End of report.*
