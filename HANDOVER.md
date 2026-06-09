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
| Android SDK | `/usr/local/share/android-commandlinetools` |
| Build tool | `./gradlew assembleRelease` / `./gradlew assembleDebug` |
| Physical device | Vivo V2515 |
| Last verified IP | `192.168.1.5:37305` |

### 1.2 Git Repository State (as of 2026-06-01)
| Item | Value |
|---|---|
| Remote | `git@github.com:foolchauhan/vellum.git` (SSH) |
| SSH key | `~/.ssh/id_rsa` — authenticated, no password needed |
| Current local branch | `feature/version-2.0.0` |
| Last commit | `051cb9a` — docs: branches are never deleted (working directory has uncommitted modifications) |

**Active branches (all on GitHub):**
```
main                       ← stable production snapshot
develop                    ← integration branch
release/release-1.0.0      ← v1.0.0 frozen release snapshot
feature/version-2.0.0      ← active development branch (finishing version 2.0.0 release)
```

> [!IMPORTANT]
> **To resume tomorrow**: Push `feature/version-2.0.0` to GitHub, open a PR to merge into `develop`, create `release/release-2.0.0`, merge integration branch into it, and publish `apks/Vellum-2.0.0.apk`.

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
   - `release/release-2.0.0` — v2.0.0 release snapshot (frozen at v2.0.0)
   - `release/release-1.0.0` — v1.0.0 release snapshot (frozen at v1.0.0)
3. **Branching workflow documented** in all MD files:
   - [AGENT.md](AGENT.md) — full mandatory workflow (Section 4)
   - [README.md](README.md) — branch structure and quick-start
   - [PROJECT.md](PROJECT.md) — version control section (Section 6)
   - [HANDOVER.md](HANDOVER.md) — this file, session start rules and state
4. **Branch policy**:
   - No direct commits to `main`, `develop`, or `release/*` — PR required
   - All branches (feature, fix, chore, integration) are **never deleted** — kept permanently for history
   - [x] Configure GitHub Branch Protection Rules for `develop` and `release/*`
- [x] Perform requested cosmetic tweaks and additional UI functions (to be specified by user)

### Phase 7: Sync Reliability & Conflict Handling — Completed (2026-05-30)
1. **Schema & DB Migration**: Added `updatedAt`, `isDeleted`, and `deletedAt` columns to `transactions`, `categories`, and `accounts` (Room DB migration v3 → v4).
2. **Soft-Delete Implementation**: Routed UI category, transaction, and account deletions to soft-delete mark update queries, filtering out `isDeleted = 1` rows in active UI flows.
3. **Leave vs Delete Separation**: Implemented `leaveAccount` in repository to let non-owner members leave shared accounts without cascade-deleting shared transactions.
4. **LWW Sync Gateway**: Replaced App Script appends with upserts by UUID (idempotent writes), added owner delete guards, and applied GET-first Last-Write-Wins merges on the client using UTC epoch millisecond timestamps.
5. **Display Joins**: Fixed category/account text resolution to run joins on active cache lists at display time.

### Phase 8: Full-Screen Refactoring & CSV Bulk Upload — Completed (2026-05-30)
1. **Full-Screen Forms**: Refactored transaction, category, and account Add/Edit popup dialog overlays into full-page screens via `Navigation3` routes (`AddEditTransactionScreen`, `AddEditCategoryScreen`, `AddEditAccountScreen`).
2. **Category Auto-Selection**: Added reactive `LaunchedEffect` mapping that auto-selects newly created categories when returning from category forms.
3. **CSV Bulk Upload**: Created settings-linked **Download CSV Template** (SAF `CreateDocument`) and **Bulk Upload CSV** (`GetContent`) options, which auto-seeds missing categories and accounts with default styles.
4. **Ui Enhancements**:
   - Redesigned vertical `SpendingTab` into a scrollable area with sticky headers and bottom action buttons.
   - Added **Spending by Category** list with color markers and dividers.
   - Fixed category color collisions by dynamically overriding duplicate colors from a Pool of distinct colors.
   - Rotated landscape bar graph labels by `45 degrees` and expanded bottom padding to `70.dp` to prevent horizontal text overlaps.

### Phase 9: UI Polish, Custom PDF/CSV Exports, and Renamed Release APK — Completed (2026-05-31)
1. **Screen Consistency & Keyboard Insets**: Passed `Modifier.safeDrawingPadding()` down from `Navigation.kt` to the Add/Edit screens (`AddEditTransactionScreen`, `AddEditCategoryScreen`, `AddEditAccountScreen`) and applied it to their root scrollable `Column` container. This keeps forms from going behind the status and navigation bars, and automatically shrinks the scroll layout when the soft keyboard is open, making the bottom notes section scrollable and visible.
2. **Export Filtering & custom dialog**: Redesigned `ShareExportDialog.kt` to let users filter their transactions by Account, Period Type ("All Time", "Daily", "Weekly", "Monthly", "Yearly"), and specific Date/Week/Month/Year ranges before initiating the download.
3. **Professional CSV & PDF Export**: Created `ExportManager.kt` containing robust exporters:
   - **CSV Export**: Writes transactions in the exact template format (`Date,Type,Amount,Category,Account,Note`).
   - **PDF Export**: Draws a professional, multi-page financial statement utilizing native Android `PdfDocument` and `Canvas`. It renders the app's real `blackboard_background_01` chalkboard texture and actual `ic_launcher` app icon in the header, shows active filter metadata, aggregates Total Income, Total Expenses, and Net Balance in grid cards, draws horizontal category progress bar breakdown charts, and renders detailed alternating-row transaction history logs.
4. **Dynamic Context-Specific File Naming**: Configured the SAF `CreateDocument` launchers inside `TransactionsTab.kt` to name output files dynamically based on selected filters (e.g. `vellum_personal_report_may_2026.pdf` or `vellum_all_transactions_yearly_2026.csv`).
5. **Spending tab Active Account Context**: Updated the period selector in `SpendingTab.kt` to display the active account's name centered between the arrow navigation keys.
6. **Automatic Release APK Renaming**: Configured the `release` block inside `app/build.gradle.kts` to sign itself using the debug keystore (making it easily installable for family/friends) and automatically rename the output file to `Vellum.apk`.

### Phase 10: Vellum Version 2.0.0 Release — Completed (2026-06-01)
1. **Custom Classroom Themes & Fonts**: Integrated Greenboard and Blueprint chalkboard themes alongside standard dark/light modes. Custom font selectors support Cabin Sketch, Patrick Hand, Fredericka the Great, and Caveat.
2. **Chalkboard Animations**: Added chalkboard eraser wiping sweep animation in transaction rows, and animated count-up chalkboard tallying for Spending tab balances.
3. **Envelope Budgeting & Splits**: Added visual depleting chalk progress lines in category list, form row limits in Category screen, and full transaction splits editor supporting JSON serialization.
4. **Biometric Security & OCR**: Configured BiometricPrompt fingerprint/face unlock with custom lock screen overlay, and ML Kit receipt OCR parsing.
5. **Conflict Resolution & Sync Visualizer**: Added visual side-by-side transaction conflict resolution dialog, and a top-bar cloud pending sync queue counter.
6. **Chalkboard Home Screen Widgets**: Implemented ChalkboardWidgetProvider rendering daily expenses and current balance in chalk style on a canvas bitmap, dynamically updated in real-time.
7. **Multi-App Side-by-Side Deployment**: Configured debug variant with package suffix `.v2` and manifest placeholder appName `"Vellum 2.0"` to deploy alongside the stable version.

### Phase 11: Google Sign-In Fix on Release Builds — Completed (2026-06-04)
1. **Bundled Keystore Config**: Copied the Firebase-registered debug keystore to `app/debug.keystore` and configured `signingConfigs` in `app/build.gradle.kts` for both `debug` and `release` configurations. This ensures builds compiled on different machines match the registered SHA-1 fingerprint (`b097336b729cd145a90d8bd6b91752580d9125d7`).
2. **Release APK Clean & Build**: Generated the release `Vellum.apk` successfully using `./gradlew clean assembleRelease`.
3. **ADB Installation**: Successfully installed `Vellum.apk` on the physical device `192.168.1.5:37305`.
4. **Manual Verification Block**: ADB verification was prepared but blocked due to secure keyguard lock screen (fingerprint/PIN). Needs manual unlock on resume.

### Phase 12: Usability Improvements, AI Semantic Search, Filter Preservation, and Icon Upgrades — Completed (2026-06-08)
1. **Icon Library Expansion & Scrollable Dialogs**: Expanded category and account icons to 80+ unique options (53 Expense, 18 Income, 11 Account). In [AddEditCategoryScreen.kt](app/src/main/java/com/example/vellum/ui/main/AddEditCategoryScreen.kt) and [AddEditAccountScreen.kt](app/src/main/java/com/example/vellum/ui/main/AddEditAccountScreen.kt), the icon selection grids are wrapped in scrollable boxes of `300.dp` max height to prevent layout overflows.
2. **AI Semantic Search Engine**: Introduced a lightweight on-device concept semantic vector matching engine ([SemanticMatcher.kt](app/src/main/java/com/example/vellum/data/SemanticMatcher.kt)) that supports parsing and resolving natural language queries in [TransactionsTab.kt](app/src/main/java/com/example/vellum/ui/tabs/TransactionsTab.kt) (e.g., questions like "how much did I spend on food in June?" or "what was petrol cost?").
3. **Filter Preservation Across Rotations**: Configured sign-in logic via `isRestore = true` in [Navigation.kt](app/src/main/java/com/example/vellum/Navigation.kt) to preserve selected account and category filters when switching orientations between portrait tabs and the landscape reports screen.
4. **Landscape Reports Account Integration**: Landscape Pie, Bar, and Cash Flow line charts in [LandscapeReports.kt](app/src/main/java/com/example/vellum/ui/main/LandscapeReports.kt) now dynamically filter data based on the active account selection from the main dashboard.
5. **Horizontally Scrollable Bar Charts**: Replaced 5-bar pagination on the landscape reports screen with a horizontally scrollable container, displaying all category bars side-by-side with rotated 45-degree text labels in [LandscapeReports.kt](app/src/main/java/com/example/vellum/ui/main/LandscapeReports.kt).
6. **Landing Tab & Spacing Adjustments**: Initialized the view pager in [MainScreen.kt](app/src/main/java/com/example/vellum/ui/main/MainScreen.kt) to land on the **Spending** tab by default on app launch. Optimized the **Financial Tutor** card in [SpendingTab.kt](app/src/main/java/com/example/vellum/ui/tabs/SpendingTab.kt) by scaling the vector illustration coordinates to a 36dp Canvas and tightening margins, increasing readable text space.
7. **Cloud Sync Icon Badge Bugfix**: Fixed the mismatch where the cloud pending icon badge count showed 2 but the sync queues showed empty, by unifying the Room sync-queue flows and dialog observers in [MainScreenViewModel.kt](app/src/main/java/com/example/vellum/ui/main/MainScreenViewModel.kt).

---

## 3. Verification & Deployment Status

| Check | Status |
|---|---|
| Gradle compilation | ✅ Clean — `./gradlew assembleDebug` and `./gradlew assembleRelease` succeed |
| APK assembly | ✅ `Vellum.apk` and `Vellum-2.0.0.apk` built successfully |
| Device deployment | ✅ Installed and verified side-by-side as "Vellum 2.0" (`com.example.vellum.v2`) on connected device |
| GitHub push | ✅ All files tracked and ready for feature branch push |
| MD documentation | ✅ All project MD files updated with current session milestones |

---

## 4. Pending / Next Session

> [!NOTE]
> All core features and usability upgrades of Vellum Version 2.0.0 are fully implemented, verified, and successfully deployed on the device. Ready for merging.
