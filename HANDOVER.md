# Vellum Android Application - Handover & Status

This document preserves the current status, codebase architecture, achievements, and completed tasks for the Vellum Android project.

> [!CAUTION]
> **Session Start Rule**: Before touching any file, run `git branch --show-current`. If you are on `develop`, `main`, or `release/*` — STOP and create a feature branch first. See [AGENT.md](AGENT.md) Section 4 for the full workflow.

---

## 1. Active Context & Current State

### 1.1 Platform & Environment
| Item | Value |
|---|---|
| Platform | Native Android App — Kotlin, Jetpack Compose, Room SQLite |
| Android SDK | `~/Library/Android/sdk` |
| Build tool | `./gradlew assembleDebug` |
| Physical device | `adb-10BFBJ1Y4G001TE-06Tqpr._adb-tls-connect._tcp` |
| Last verified IP | `192.168.1.4:35535` |

### 1.2 Git Repository State (as of 2026-05-29)
| Item | Value |
|---|---|
| Remote | `git@github.com:foolchauhan/vellum.git` (SSH) |
| SSH key | `~/.ssh/id_rsa` — authenticated, no password needed |
| Current local branch | `main` |
| Last commit | `051cb9a` — docs: branches are never deleted |

**Active branches (all on GitHub):**
```
main                       ← stable production snapshot
develop                    ← integration branch (start work from here)
release/release-1.0.0      ← v1.0.0 frozen release snapshot
```

> [!IMPORTANT]
> **To start tomorrow**: `git checkout develop && git pull origin develop && git checkout -b feature/your-feature-name`

### 1.3 Branch Protection (TODO — must be configured on GitHub)
Go to → https://github.com/foolchauhan/vellum/settings/branches
- Add rules for `develop` and `release/*`
- Enable: "Require PR before merging", "Require approvals", "Block direct pushes", "Include administrators"

### 1.4 Codebase Navigation Guide

| File | Purpose |
|---|---|
| [MainActivity.kt](app/src/main/java/com/example/vellum/MainActivity.kt) | App entrypoint, Google Sign-in flow host |
| [Navigation.kt](app/src/main/java/com/example/vellum/Navigation.kt) | Compose Navigation routes & Google account sign-in restoration |
| [MainScreen.kt](app/src/main/java/com/example/vellum/ui/main/MainScreen.kt) | Main dashboard shell — HorizontalPager, tabs, top bar, landscape redirect |
| [LandscapeReports.kt](app/src/main/java/com/example/vellum/ui/main/LandscapeReports.kt) | Full landscape reports dashboard — Pie, Bar, Cash Flow charts |
| [MainScreenViewModel.kt](app/src/main/java/com/example/vellum/ui/main/MainScreenViewModel.kt) | State manager for all database events and period navigation |
| [AppScreens.kt](app/src/main/java/com/example/vellum/ui/main/AppScreens.kt) | Spending graph layouts, transaction row managers, all Add/Edit dialogs |
| [SheetsSyncManager.kt](app/src/main/java/com/example/vellum/data/SheetsSyncManager.kt) | Network sync engine, handles 302/307/308 redirects |
| [DataRepository.kt](app/src/main/java/com/example/vellum/data/DataRepository.kt) | Repository exposing Room flows to ViewModel |
| [Entities.kt](app/src/main/java/com/example/vellum/data/local/Entities.kt) | Room DB entity definitions (transactions, categories, accounts, preferences) |
| [Daos.kt](app/src/main/java/com/example/vellum/data/local/Daos.kt) | Room DAO query definitions |
| [VellumDatabase.kt](app/src/main/java/com/example/vellum/data/local/VellumDatabase.kt) | Preloaded SQLite database instance with migration support |
| [Color.kt](app/src/main/java/com/example/vellum/theme/Color.kt) | Chalkboard and parchment design token hex colors |
| [Type.kt](app/src/main/java/com/example/vellum/theme/Type.kt) | Typography using `patrick_hand.ttf` handwriting font |
| [scripts/Code.gs](scripts/Code.gs) | Google Apps Script backend — sync gateway, backup triggers, reset utility |

---

## 2. Completed Milestones

### Phase 1 & 2: Chalkboard UI Overhaul & Dialog Polish
1. **Global Chalkboard/Sketchbook Aesthetic**:
   - Loaded and applied the handwritten font (`patrick_hand.ttf`) globally.
   - Rendered chalkboard slate scratches and fine chalk dust on a Canvas programmatically.
   - Custom `HorizontalDivider` override rendering lines as dashed chalk lines.
   - Custom pastel amount colors: `ChalkGreen` (Bright Mint) for Income and `ChalkRed` (Pastel Coral/Red) for Expenses.
2. **Dialog Overlays**:
   - All forms (`AddTransactionDialog`, `AddCategoryDialog`, `AddAccountDialog`) are floating centered Dialog cards rather than full screen activities.
   - Clickable "Note" row opens `CreateNoteDialog` with auto-requesting keyboard focus.
   - Custom styled `ChalkboardDatePickerDialog` matches the chalkboard grid visual aesthetic.
3. **Interactive Features**:
   - Added swipe gestures between main tabs (Spending, Transactions, Categories, Accounts) using Compose `HorizontalPager`.
   - Wired the click handler on the Settings "Tabs Position" setting to open a custom Card-styled options selection popup. Tab row is dynamically placed Top or Bottom.
   - Backgrounds are blurred using `.blur(10.dp)` when dialog overlays are showing.
   - Spending tab arrows correctly cycle time periods (e.g. Month to Month) and update display labels.
   - Bottom bar in Transactions displays split Account/Category filter icons and a share dialog with CSV/PDF placeholders.

### Phase 3: Google Sign-In & Google Sheets Backend Sync
1. **Google Sign-In**:
   - Native integration with Google Play Services Auth.
   - Top-left circular profile avatar showing user's name initials.
   - Settings pane features a Google Profile Card for logging in and logging out.
2. **Google Sheets Sync Gateway**:
   - Migrated SQLite tables to Version 3 schema using unique UUID string primary keys to prevent multi-device write conflicts.
   - Implemented `SheetsSyncManager` to handle redirects (302/307/308) from Google Apps Script Web Apps.
   - Synced data is merged, and replaced locally in a single Room database transaction to prevent flickering.
   - Google Sheet Sync URL settings row opens custom `UrlInputDialog` for pasting script URL.
3. **Shared Household Accounts**:
   - Added a "Share with Household" toggle in `AddAccountDialog` which generates a 6-character sharing code (e.g. `FAM392`).
   - Added a "+ Join Shared Account" button at the bottom of the accounts list to join existing shared accounts using a code.
   - Shared accounts display their share code and render using the `Icons.Default.People` icon.

### Phase 4: Advanced Features & Sync Optimizations
1. **Dynamic Sync & Backups**:
   - Switched sync logic to a **GET-first bidirectional sync** inside `SheetsSyncManager.kt` to merge data correctly and prevent unshared personal defaults from overriding shared accounts.
   - Added settings-linked daily backups at 11:30 PM, dynamically creating or deleting Google Apps Script triggers based on whether any user has "Auto Backup" enabled.
2. **UI & UX Details**:
   - Pre-selects the profile's active account filter by default when creating new transactions.
   - Removed single-line limits and ellipsis truncation from transaction notes to show them in full.
   - Displays a rotating refresh icon in the toolbar while synchronization is active.
   - Sorts categories dynamically by usage frequency in both the Categories tab and the Add Transaction dialog.
3. **Developer Tools**:
   - Automatically wipes the Room database and logs out of Google Sign-in on the first startup of a debug installation to provide a clean slate.
   - Removed all references, navigation entries, and screen files for the unused Cafe Menu page.
   - Added a manual `resetSpreadsheet()` function in `Code.gs` for manual testing.
4. **Settings Sync Fix**:
   - Local settings preferences now correctly override remote preferences pulled from Google Sheets. Saving a setting no longer gets overwritten on the next sync.

### Phase 5: Seamless Transitions & Premium Landscape Reports Dashboard
1. **Seamless Tab Transitions**:
   - Removed the `ChalkboardBackground` fading overlay from `MainScreen.kt` that was causing a black flash during the Spending → Transactions swipe.
   - The `ParchmentBackground` is now drawn once, statically, behind all tabs — the background no longer moves or re-renders during swipes.
2. **System Boundaries in Landscape**:
   - `LandscapeReports` now respects system safe drawing boundaries (status bar + nav bar) in landscape orientation.
3. **Bar Chart Improvements**:
   - Bars are grouped closely together and centered on the canvas.
   - A clean horizontal baseline is drawn for better viewability.
   - Category data is paginated — 5 bars per page — with top-left `-` / `+` page navigation buttons.
4. **Cash Flow Interval Selector**:
   - Added `D` / `W` / `M` / `Y` toggle in the top-left of the Cash Flow report.
   - `calculateCashFlowData()` dynamically computes income and expense sums for Daily, Weekly, Monthly, or Yearly intervals over the last 12 periods.
   - Top-right shows a dynamic label: `"Last 12 Days"`, `"Last 12 Weeks"`, `"Last 12 Months"`, `"Last 12 Years"`.
   - Currency symbol from user preferences (`currencySymbol`) passed to all chart canvases — no hardcoded symbols.
5. **Pie Chart — not full screen**:
   - Pie chart is now rendered at a fixed `220.dp` size rather than filling the entire canvas, giving breathing room around the chart.

### Phase 6: GitHub Repository & Branching Setup (2026-05-29)
1. **GitHub Repository created**: `https://github.com/foolchauhan/vellum`
   - SSH remote configured: `git@github.com:foolchauhan/vellum.git`
   - SSH key `~/.ssh/id_rsa` authenticated — works without password.
2. **Branches pushed**:
   - `main` — stable production snapshot (initial commit `d26e5c4`)
   - `develop` — integration branch, synced from main
   - `release/release-1.0.0` — first release snapshot (frozen at v1.0.0)
3. **Branching workflow documented** in all MD files:
   - [AGENT.md](AGENT.md) — full mandatory workflow (Section 4)
   - [README.md](README.md) — branch structure and quick-start
   - [PROJECT.md](PROJECT.md) — version control section (Section 6)
   - [HANDOVER.md](HANDOVER.md) — this file, session start rules and state
4. **Branch policy**:
   - No direct commits to `main`, `develop`, or `release/*` — PR required
   - All branches (feature, fix, chore, integration) are **never deleted** — kept permanently for history
   - Branch protection rules to be configured on GitHub (see Section 1.3 above)

---

## 3. Verification & Deployment Status

| Check | Status |
|---|---|
| Gradle compilation | ✅ Clean — `./gradlew assembleDebug` succeeds |
| APK assembly | ✅ Debug APK built successfully |
| Device deployment | ✅ Installed and verified on `192.168.1.4:35535` |
| GitHub push | ✅ All 3 branches live on `github.com/foolchauhan/vellum` |
| MD documentation | ✅ All 4 MD files updated with full workflow |

---

## 4. Pending / Next Session

> [!NOTE]
> Nothing is broken. The app is fully functional. The next session is fresh feature development.

**Before writing a single line of code next session:**
```bash
# Step 1 — verify your branch
git branch --show-current

# Step 2 — if not on a feature branch, create one
git checkout develop
git pull origin develop
git checkout -b feature/your-next-feature

# Step 3 — confirm
git branch --show-current   # should show feature/your-next-feature
```

**Open TODOs / Nice-to-haves (no priority order):**
- [ ] Configure GitHub Branch Protection Rules for `develop` and `release/*` (see Section 1.3)
- [ ] Transactions CSV export (share dialog placeholder already in place)
- [ ] Transactions PDF export (share dialog placeholder already in place)
- [ ] Passcode/PIN lock screen (settings toggle exists, logic not implemented)
- [ ] Reminders / notification implementation (settings toggle exists, logic not implemented)
- [ ] Dropbox sync integration (settings toggle exists, logic not implemented)
- [ ] Remove Ads / monetisation implementation
- [ ] Backups screen full implementation
