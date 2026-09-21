# The Ultimate UI/UX Design System
**Project:** Offline Habit & Routine Optimizer
**Document Purpose:** A comprehensive, master-level UI/UX specification transforming the "Spreadsheet Grid" aesthetic into a world-class, professional Android Habit Tracking application. 

---

## 1. Core Visual Philosophy
The aesthetic is inspired by high-density data tools (like Google Sheets, Notion databases, and developer tools). We are stripping away the "cutesy" UI typical of habit trackers (no avatars, no gamified badges) and replacing it with a **tactile, analytical, and brutalist-elegant grid**. 

**Design Principles:**
*   **Data Density Over White Space:** The user wants to see their entire month at a glance. We use compact, perfectly aligned grids.
*   **"Show the Math":** Progress is shown through raw numbers, ratios, and sparkline graphs. 
*   **Tactile Feedback:** Because the UI is visually minimal, interaction must feel incredible. Checking off a habit should trigger crisp haptic feedback and a rapid, satisfying color fill.
*   **System-Native Typography:** Crisp sans-serifs for readability, paired with Monospace fonts for all numbers, streaks, and percentages to align with the developer-centric vibe.

---

## 2. Global Design Tokens (Jetpack Compose)

### 2.1 Color Palette (Material 3 - Muted & Analytical)
Instead of bright, blinding primary colors, we use sophisticated, desaturated tones. The grid should look calm, not stressful.

**Dark Mode (Default/Preferred for Tech-focused UI):**
*   `Background`: `#0E1117` (Deep matte black/gray)
*   `Surface` (Grid Cells): `#161B22` (Slightly elevated gray)
*   `Outline / Grid Lines`: `#30363D` (Crisp, thin 1dp borders)
*   `Text Primary`: `#C9D1D9`
*   `Text Monospace (Math)`: `#8B949E`
*   **State Colors (The 4-State Machine):**
    *   `Completed`: `#238636` (Desaturated GitHub Green)
    *   `Pending`: Transparent / Surface color
    *   `Skipped`: `#484F58` (Muted gray)
    *   `Failed`: `#DA3633` (Muted crimson, used sparingly so it doesn't guilt-trip)

### 2.2 Typography Hierarchy
*   **Headers:** `Inter` or `Roboto` (Medium, tracking slightly tight).
*   **Grid Labels (Habits):** `Inter` (Regular, 14sp, max-lines: 1, ellipsize).
*   **Data & Numbers:** `JetBrains Mono` or `Roboto Mono`. 
    *   *Why?* Monospace fonts ensure that "70.5%" and "12.0%" align perfectly vertically in your summary rows, reinforcing the "spreadsheet" feel.

---

## 3. Screen-by-Screen UI Architecture

### Screen 1: "The Matrix" (Main Dashboard)
This is the hero screen. It completely replaces the standard "list of cards" with a powerful 2D scrollable grid.

**Layout Structure:**
*   **Top Bar:** Current Month Year (e.g., "September 2026"), aligned left. A small gear icon for Settings aligned right.
*   **The Grid (Lazy Layout):**
    *   **Sticky Left Column (Width: 120dp-140dp):** Contains the habit names. 
    *   **Scrollable Right Area:** Contains the days of the month.
    *   **Header Row:** "M 12", "T 13", "W 14". The current day is highlighted with a subtle top-border accent.
    *   **Cell Design:** 32x32dp squares. Border radius of 4dp (not fully rounded, keeps the tabular feel). 1dp gap between cells.
*   **The Summary Footer (Sticky at Bottom):**
    *   Just like a spreadsheet's calculation row.
    *   Shows: `Daily Completion %` for each column (day). 
    *   Renders a tiny SVG sparkline (line chart) showing the 7-day rolling consistency score.

**Interaction:**
*   **Tap Cell:** Cycles state (`Pending` -> `Completed` -> `Skipped` -> `Pending`).
*   **Haptics:** `HapticFeedbackType.TextHandleMove` for cycling, `HapticFeedbackType.LongPress` for marking `Completed`.

### Screen 2: Habit Detail & Analysis ("The Heatmap")
When a user taps the Habit Name in the left column, they drill down into the analytics.

**Layout Structure:**
*   **Hero Stat:** The current Consistency Score (e.g., **84%**) rendered massive (48sp Monospace).
*   **Streak Blocks:** 
    *   Current Streak: X days
    *   Longest Streak: Y days
    *   Target Quota: e.g., "3x/week"
*   **The GitHub-Style Heatmap:** 
    *   A grid of 52 columns (weeks) x 7 rows (days).
    *   Colors scale based on completion (for quota habits) or binary (for daily habits). 
*   **Trend Chart:** A simple, unstyled line graph charting the 30-day rolling average. 

### Screen 3: Routine Builder (Gantt Chart UI)
Adapting the spreadsheet layout for time-blocking.

*   **Y-Axis:** Time of day (00:00 to 23:59), scaling dynamically based on user's active hours.
*   **X-Axis:** Custom Routines (Morning, Deep Work, Evening).
*   **Blocks:** Habits are represented as colored blocks that can be dragged and dropped.
*   **Overlap Detection:** If `StartMinuteOfDay` + `Duration` overlaps another habit, the blocks stack side-by-side (like Google Calendar conflicts) and gain a red `errorContainer` border.

---

## 4. Jetpack Compose Implementation Blueprints

To achieve this "10,000 developer" polish, you must build custom layouts rather than relying on basic components.

### 4.1 The Sync-Scroll Grid
The hardest part of a spreadsheet UI in Compose is keeping the Left Column (Habits) vertically synced with the Right Matrix (Checkboxes), while allowing the Right Matrix to scroll horizontally.

**Implementation Strategy:**
Use a combination of `LazyColumn` for vertical scrolling and a nested `LazyRow` for the days. However, to keep headers perfectly synced, you need a custom `Layout` or synchronized scroll states.

```kotlin
// Conceptual Compose Architecture for The Matrix
@Composable
fun SpreadsheetMatrix(habits: List<Habit>, logs: Map<String, Map<LocalDate, HabitStatus>>) {
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()

    Column {
        // Sticky Date Header Row
        HeaderRow(horizontalScrollState)

        // Main Body
        Row(modifier = Modifier.verticalScroll(verticalScrollState)) {
            // Sticky Left Column (Habit Names)
            HabitNamesColumn(habits)
            
            // 2D Matrix (Checkboxes)
            Box(modifier = Modifier.horizontalScroll(horizontalScrollState)) {
                Column {
                    habits.forEach { habit ->
                        HabitCheckboxRow(habit, logs[habit.id])
                    }
                }
            }
        }
    }
}
```

### 4.2 Pixel-Perfect Borders
To make it look like a real spreadsheet, do NOT use default Compose dividers which can look muddy.
*   Use a `Modifier.drawBehind` to draw crisp 1px lines using `Stroke(width = 1f)`.
*   Draw borders *only* on the right and bottom of each cell to prevent double-thick lines where cells touch.

## 5. Animation & Polish

Professional apps feel alive, even when the UI is static and data-heavy.
1.  **State Transition:** When a cell changes from `Pending` to `Completed`, animate the background color using `animateColorAsState(tween(200))`.
2.  **Checkbox Icon:** The checkmark inside the square shouldn't just appear; use `AnimatedVisibility` with a `scaleIn()` and `fadeIn()` transition.
3.  **Loading Data:** No spinning circles. Use a subtle shimmer effect on the grid cells while the SQLite database runs its `StateFlow` emission on app startup.

## 6. Privacy & Offline-First Guarantees in the UI
Make the offline nature a feature of the UI.
*   In the Settings screen, include a section titled **"Data Vault"**.
*   Show text: *"This app does not have internet permissions. Your data physically cannot leave this device."*
*   Provide a massive, tactile "Export to SQLite/CSV" button to give the user complete ownership of their tracking data.
