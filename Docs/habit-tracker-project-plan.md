# Offline Habit & Routine Optimizer — Project Plan

**Author:** Prasad Dhodamani
**Project type:** Student project — Android, offline-first
**Status:** Design / pre-implementation

This is the master planning reference for the project: concept, architecture, data model, UI, tech stack, and privacy/security decisions, all in one place.

## Table of Contents
1. Concept
2. Scope
3. Core Concepts
4. System Architecture
5. Data Model (Backend)
6. UI / UX Design
7. Notifications & Reminders
8. Tech Stack
9. Sensitive Info & Privacy/Security Design
10. Testing Strategy
11. Engineering Challenges
12. Roadmap
13. Suggested Repo Structure
14. Deferred / Cut From Core Scope
15. Next Steps

---

## 1. Concept

A privacy-first habit and routine tracker that runs entirely on-device, with zero cloud dependency. No accounts, no servers, no telemetry — every habit check-in, streak, and routine schedule lives in a local SQLite database. Insight comes from deterministic math (rolling averages, state machines, interval logic), not from an opaque AI model deciding what's "good" for the user.

The philosophy in one line: **math over magic, disk over cloud.**

## 2. Scope

### 2.1 Phase 1 — MVP (core deliverable)
- Habit CRUD (create / edit / delete / archive)
- Daily habits **and** quota-based habits ("every day" vs. "3x/week")
- A 4-state state machine per habit per day: `Pending`, `Completed`, `Skipped`, `Failed`
- Streak + consistency engine (7/30/90-day rolling windows)
- Today screen + basic stats view
- Local SQLite storage via Room, no network permission at all

### 2.2 Phase 2 — Rounds Out the App
- Local reminders/notifications for scheduled habits (§7)
- Time-blocked routine builder with overlap detection
- Contribution heatmap visualization

### 2.3 Phase 3 — Stretch Goals (only if time allows)
- Home-screen widget for one-tap complete/skip
- Encrypted local export/import (manual backup, not sync)
- On-device weekly "retro" summary from a tiny local model

### 2.4 Explicit Non-Goals
- No real-time multi-device sync (P2P/CRDT sync is a separate project's worth of work — see §14)
- No cloud backend, no user accounts, no analytics SDKs
- No AI-driven recommendations as a *primary* feature

## 3. Core Concepts

### 3.1 Habit State Machine
Every habit, for every calendar day, is in exactly one state:

| State | Meaning |
|---|---|
| `Pending` | Day hasn't been resolved yet |
| `Completed` | User marked it done |
| `Skipped` | User explicitly skipped — doesn't count against a quota habit's window the way `Failed` does |
| `Failed` | Day closed with the habit not done and not skipped |

A check on app open (or a lightweight background job) rolls `Pending` days that have passed the day boundary into `Failed`, unless the habit's quota window still has mathematical slack (see below).

### 3.2 Streak & Consistency Engine
Two habit types:
- **Daily habits** — streak = consecutive `Completed` days, resets on any `Failed`.
- **Quota habits** (e.g. "run 3x/week") — a single missed day doesn't break anything. Instead, compute a rolling completion rate over the window (7/30/90 days) and only mark a day `Failed` retroactively once the quota can no longer be mathematically met within the window.

Consistency score = completions ÷ expected-completions over the window, shown as a percentage. It's a plain ratio, not a black-box score — the user should always be able to see the math behind the number.

### 3.3 Day Boundary Handling
All timestamps are stored in UTC. Each user has a configurable "day boundary" (default midnight, adjustable — e.g. 3:00 AM for night owls) so a habit completed at 1:00 AM still counts for the *previous* day instead of silently breaking a streak. This setting lives in local `Settings` and is applied only at the query/display layer — it never mutates stored timestamps.

## 4. System Architecture

Standard layered Android architecture, nothing exotic:

```
┌───────────────────────────┐
│   UI (Jetpack Compose)    │
├───────────────────────────┤
│   ViewModel (StateFlow /  │
│   Compose state hoisting) │
├───────────────────────────┤
│   Repository (streak math,│
│   state machine logic)    │
├───────────────────────────┤
│   Room / SQLite           │
│   (local persistence)     │
└───────────────────────────┘
```

No network layer, anywhere. That's not an omission — it's a design constraint. The `INTERNET` permission can be left out of the manifest entirely, which is a hard, OS-enforced guarantee (not just a policy promise) that the app can't phone home.

## 5. Data Model (Backend)

Room entities, roughly:

```kotlin
enum class HabitType { DAILY, QUOTA }
enum class HabitStatus { PENDING, COMPLETED, SKIPPED, FAILED }

@Entity
data class Habit(
    @PrimaryKey val id: String,
    val name: String,
    val type: HabitType,
    val targetPerWindow: Int,     // e.g. 3 (for "3x/week")
    val windowDays: Int,          // e.g. 7
    val createdAt: Long,          // UTC millis
    val archived: Boolean = false
)

@Entity(indices = [Index(value = ["habitId", "date"])])
data class HabitLog(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: String,             // ISO date, day-boundary-adjusted
    val status: HabitStatus,
    val completedAtUtc: Long?,
    val note: String? = null
)

@Entity
data class Routine(
    @PrimaryKey val id: String,
    val name: String
)

@Entity
data class RoutineBlock(
    @PrimaryKey val id: String,
    val routineId: String,
    val habitId: String,
    val startMinuteOfDay: Int,
    val durationMinutes: Int
)

@Entity
data class Settings(
    @PrimaryKey val id: Int = 0,
    val dayBoundaryHour: Int = 0,  // 0 = midnight, 3 = 3 AM, etc.
    val theme: String = "system"
)
```

**Indexing:** the composite index on `(habitId, date)` above isn't decorative — the streak engine queries by habit + date range constantly, and that index keeps those lookups fast even with months of history.

### Streak calculation (simplified)
```kotlin
fun consistencyScore(logs: List<HabitLog>, target: Int): Float {
    val completed = logs.count { it.status == HabitStatus.COMPLETED }
    return (completed.toFloat() / target).coerceAtMost(1f)
}
```
The real implementation adds window-sliding and per-day quota math, but the core idea stays a plain ratio — easy to unit test, easy to explain in a viva.

## 6. UI / UX Design

Six core screens:

1. **Today** — today's habits as a checklist, one tap to mark `Completed` / `Skipped`. This is the screen opened 90% of the time; keep it to zero scrolling for a normal day.
2. **Habit Detail** — streak count, consistency %, and a GitHub-style contribution heatmap (calendar grid shaded by completion) — fits the "show the math" philosophy better than a vague progress bar.
3. **Routine Builder** *(Phase 2)* — drag-and-drop time blocks on a day timeline, red highlight on overlap.
4. **Add/Edit Habit** — name, daily vs. quota, target frequency, optional reminder time.
5. **Settings** — day-boundary hour, theme, export/import.
6. **Home Screen Widget** *(stretch)* — Jetpack Glance widget showing today's habits with tap-to-complete, so checking in doesn't always require opening the app.

Design direction: Material 3 via Compose, minimal chrome, no streak-shaming red banners or guilt-trip notifications — the whole pitch of this app is calm and honest about the numbers rather than built on manipulative engagement mechanics.

**Accessibility:** TalkBack labels on every interactive element, respect the system font-scale setting, and don't rely on color alone for state — pair heatmap/streak colors with an icon or label too.

## 7. Notifications & Reminders

Reminders are what turn this from a manual log into an actual habit tool — worth building even though the original pitch didn't call it out explicitly.

- **Scheduling:** `WorkManager`, or `AlarmManager` with `setExactAndAllowWhileIdle` for precise timing, fires a local notification at each habit's configured reminder time.
- **Notification actions:** include direct "Mark Done" / "Skip" actions on the notification itself (`NotificationCompat.Action`) so check-in doesn't require opening the app.
- **Permissions:** Android 13+ needs runtime `POST_NOTIFICATIONS`, and exact timing needs `SCHEDULE_EXACT_ALARM` — both need a graceful fallback if denied (inexact `WorkManager` scheduling still works fine for a "sometime this hour" reminder).
- **No push infrastructure needed** — everything is scheduled locally, consistent with the zero-cloud design.

## 8. Tech Stack

- **Kotlin + Jetpack Compose** — UI layer, consistent with your existing Android/Termux build pipeline
- **Room** — local persistence (SQLite under the hood)
- **Kotlin Coroutines + StateFlow** — reactive state without extra architecture overhead
- **WorkManager / AlarmManager** — local reminder scheduling (§7)
- **Jetpack Glance** *(stretch)* — home-screen widget, reuses Compose knowledge instead of the legacy RemoteViews widget API
- **No networking library** — deliberately absent
- Buildable end-to-end from Termux with the existing Gradle/Kotlin toolchain

## 9. Sensitive Info & Privacy/Security Design

Habit categories can get personal (sleep, medication reminders, mental-health-adjacent routines), so privacy needs real design decisions, not just a tagline:

- **No telemetry by default.** No analytics SDK, no crash reporter that phones home unless explicitly opt-in and clearly disclosed.
- **No `INTERNET` permission in the manifest.** Stronger than a privacy policy — the OS itself blocks any network call.
- **Local DB encryption (optional, recommended if shipping publicly):** SQLCipher for Room adds at-rest encryption; worth enabling if any habit categories could be sensitive.
- **Encrypted export/import:** any backup file written to shared storage should be passphrase-encrypted (Android Keystore-backed AES) rather than a raw `.db` copy — phones get lost, and shared folders get shared.
- **No accounts = no PII collection by design.** No username, email, or identifier tied to the data at all.

## 10. Testing Strategy

The streak/consistency math and state-machine transitions are pure functions — that makes them the highest-value, lowest-effort things to test:

- **Unit tests:** consistency-score calculation across known fixtures (3-of-7 quota, exactly-met quota, over-quota, empty history), state transitions (`Pending → Completed`, day-boundary rollover to `Failed`), and interval-overlap detection for the routine builder.
- **DAO/Room tests:** run against an in-memory Room database (`Room.inMemoryDatabaseBuilder`) to verify queries return the right rows across day-boundary edge cases.
- **UI tests** *(optional, time-permitting)*: Compose UI testing for the Today screen's tap-to-complete flow.

For a course submission, the unit tests double as a clean way to demonstrate the algorithm's correctness in a viva without live-clicking through the app.

## 11. Engineering Challenges

- **Cross-device sync** — genuinely hard (this is what CRDTs and tools like Syncthing exist for). Defer entirely for a student project; manual encrypted export/import is the only "sync" story for v1.
- **Time zone / day-boundary drift** — solved by UTC storage + the configurable day-boundary setting (§3.3), never by mutating raw timestamps.
- **Routine overlap detection** — sort blocks by start time, then one linear pass checking each block's end against the next block's start: O(n log n) from the sort, not a naive pairwise O(n²) check.
- **Changing the day-boundary setting after data exists** — don't retroactively recompute historical `HabitLog` rows; the setting only affects how *future* `Pending → Failed` rollovers are evaluated, so past data stays stable.
- **Deleting a habit** — cascade-delete its `HabitLog` and `RoutineBlock` rows (`@ForeignKey(onDelete = CASCADE)`) instead of leaving orphaned rows behind.

## 12. Roadmap

| Phase | Scope | Rough estimate |
|---|---|---|
| 1 — MVP | State machine, habit CRUD, streak engine, Today screen, basic stats | 2–4 weeks |
| 2 | Local reminders, routine builder, heatmap visualization, polish | 1–2 weeks |
| 3 — Stretch | Home-screen widget, encrypted export/import, local weekly retro, sync exploration | Post-deadline / optional |

## 13. Suggested Repo Structure

```
habit-optimizer/
├── app/
│   ├── src/main/java/com/prasad/habitoptimizer/
│   │   ├── data/
│   │   │   ├── entity/        # Habit, HabitLog, Routine, RoutineBlock, Settings
│   │   │   ├── dao/           # Room DAOs
│   │   │   └── AppDatabase.kt
│   │   ├── repository/        # streak math, state machine transitions
│   │   ├── notifications/     # WorkManager scheduling, notification builders
│   │   ├── ui/
│   │   │   ├── today/
│   │   │   ├── habitdetail/
│   │   │   ├── routinebuilder/
│   │   │   └── settings/
│   │   └── MainActivity.kt
│   └── build.gradle.kts
├── docs/
│   └── habit-tracker-project-plan.md   # this file
└── README.md
```

## 14. Deferred / Cut From Core Scope

These stay in this doc for completeness, but are **not** part of the deliverable — treat them as "future work" in any writeup rather than build targets:

- Local-first P2P sync (Syncthing / CRDT-based)
- On-device micro-LLM weekly retro generator
- Smart/adaptive reminder timing based on historical completion patterns (the original pitch's "friction & momentum scoring") — interesting, but it needs enough historical data to be meaningful, so it's a post-MVP enhancement, not a launch feature.

## 15. Next Steps

Start with the data layer and state machine — everything else (UI, streaks, heatmap, reminders) builds on top of that:
1. Scaffold Room entities + DAOs (§5)
2. Implement state machine transitions + day-boundary rollover (§3.1, §3.3)
3. Implement consistency-score math and unit test it against known cases (§10)
4. Build the Today screen against that repository layer
5. Wire up local reminders (§7)
