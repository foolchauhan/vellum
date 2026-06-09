# Technical Architecture & System Specifications: Vellum (Android App)

> [!NOTE]
> For active session context, screenshot analysis summary, and implementation checklists, see **[HANDOVER.md](HANDOVER.md)**.

Vellum is a native Android spending tracker app built using modern Android development practices (Kotlin, Jetpack Compose, Room SQLite database, and Kotlin Coroutines/Flow). It replicates the chalkboard-style Spending tracker and parchment-style transaction journals shown in the sample screenshots.

---

## 1. System Architecture Overview

Vellum follows a clean architecture pattern for Android, structured into three layers:

```mermaid
graph TD
    subgraph UI_Layer [UI Layer: Jetpack Compose]
        MainActivity[MainActivity] --> MainNavigation[MainNavigation]
        MainNavigation --> MainScreen[MainScreen Tabs]
        MainScreen --> SpendingTab[Spending View]
        MainScreen --> TransactionsTab[Transactions Journal]
        MainScreen --> CategoriesTab[Categories Manager]
        MainScreen --> AccountsTab[Accounts List]
        MainNavigation --> SettingsScreen[Settings Screen]
        MainNavigation --> AddTransactionDialog[Add Transaction dialog/screen]
        MainScreen --> JoinSharedAccountDialog[Join Shared Account dialog]
        SettingsScreen --> UrlInputDialog[URL Input dialog]
    end

    subgraph Domain_ViewModel_Layer [Domain & ViewModel Layer]
        MainScreenViewModel[MainScreenViewModel] --> DataRepository[DataRepository]
    end

    subgraph Data_Layer [Data & Storage Layer: Room SQLite & Google Sheets Sync]
        DataRepository --> VellumDatabase[VellumDatabase]
        DataRepository --> SheetsSyncManager[SheetsSyncManager]
        VellumDatabase --> TransactionDao[TransactionDao]
        VellumDatabase --> CategoryDao[CategoryDao]
        VellumDatabase --> AccountDao[AccountDao]
        VellumDatabase --> PreferenceDao[PreferenceDao]
        SheetsSyncManager -.-> GoogleSheets[Google Sheets Web App Gateway]
    end
```

### Decoupling & Sync Strategy
The UI controls consume flow streams exposed by the `DataRepository` via ViewModels. Storage operations are local-first. When users are offline/logged out, data is saved locally inside Room DB.

When signed in via Google and configured with a Google Sheets Web App Sync URL:
1. Operations write locally to the Room SQLite database and mark the record's `isSynced` flag to `false`.
2. A synchronization procedure is triggered automatically in the background (or manually via the top-bar Refresh button).
3. The `SheetsSyncManager` GETs the current sheet state first, then POSTs all un-synchronized local changes to the Google Apps Script Web App (GET-first bidirectional sync).
4. The Web App updates the Google Sheet, merges rows from all active household participants sharing the account, and returns the merged datasets.
5. The local cache is replaced in a single Room database transaction to prevent UI flickering.

### Sync Conflict Resolution Model

Merge priority order (highest wins):

| Priority | Rule | Mechanism |
|----------|------|-----------|
| 1 | Tombstone wins over live row | `deleted_accounts` / `deleted_categories` / `deleted_transactions` preference strings |
| 2 | Last-Write-Wins for edits | `updatedAt` timestamp field *(planned — not yet in schema)* |
| 3 | Local-new row wins | Unsynced local UUID rows not present on sheet → POSTed |
| 4 | Remote-new row wins | Sheet rows not present locally → inserted on GET |

**Preferences**: Local always wins when set by the user. On a fresh device install, sheet values are pulled in first. Some preferences are device-local (UI), others are syncable (financial/cross-device).

| Preference Key | Syncs to Sheet? | Reason |
|---|---|---|
| `sheets_url` | ✅ Yes | Must be identical across devices |
| `currency_symbol` | ✅ Yes | Financial data consistency |
| `auto_backup` | ✅ Yes | Controls server-side Apps Script trigger |
| `budget_mode` | ✅ Yes | Financial behaviour |
| `dark_theme` | ❌ No | Device UI preference |
| `tabs_position` | ❌ No | Device UI preference |
| `summary_font` | ❌ No | Device UI preference |

### Shared Account Rules

- Only the `ownerEmail` may **delete** a shared account.
- Non-owners may only **leave** (removes them from the `shares` tab on the sheet; account and all its transactions remain intact for other members).
- An account that has linked transactions is **never hard-deleted** from the sheet. It is tombstoned (`isDeleted=true`) so dangling `accountId` references in transactions resolve gracefully.
- Before allowing an owner delete, the Apps Script checks the `shares` tab. If other members are still active, the delete is blocked and an error is returned.

### Resolved Sync Gaps & Issues (Implemented in v2.0.0)

All previously identified sync gaps and conflict handling limitations have been fully resolved:

| Resolved Gap | Resolution Details | Status |
| :--- | :--- | :--- |
| Non-owner "Leave" vs Owner "Delete" logic | Branched UI flow in account screen: owners delete the account (tombstoned), non-owners leave (removing local record only). | ✅ Resolved |
| Cascade hard-deletes for non-owners | Non-owners leaving calls `leaveAccount` which preserves transactions on remote sheet. | ✅ Resolved |
| Tombstone cleanups on sign-out | Switched from local temp preferences to durable `isDeleted` DB columns. | ✅ Resolved |
| No `updatedAt` for LWW conflicts | Added `updatedAt` epoch millisecond timestamps to all primary entities. | ✅ Resolved |
| No `isDeleted` tombstones | Added `isDeleted` and `deletedAt` DB columns for clean soft-delete synchronization. | ✅ Resolved |
| Appends causing duplicates | App Script backend rewritten to use idempotent upserts by UUID. | ✅ Resolved |
| Orphaned `accountId` UI bugs | Dynamic display-time joins fallback to cached name or "(Deleted Account)" safely. | ✅ Resolved |

---

## 2. Local Database Schema (Room Entities)

The local SQLite database (`vellum.db`) contains four entity tables. Primary Keys are represented as UUID Strings to allow safe multi-user offline creation without key conflicts.

### 1. `transactions` (TransactionEntity)
Stores the ledger transaction entries.

| Column | SQLite Type | Description |
| :--- | :--- | :--- |
| `id` | TEXT (Primary Key) | Unique UUID String |
| `amount` | REAL | Absolute monetary value |
| `type` | TEXT | `EXPENSE` or `INCOME` |
| `categoryId` | TEXT | Linked category UUID |
| `categoryName` | TEXT | Cache of category name |
| `accountId` | TEXT | Linked account UUID |
| `accountName` | TEXT | Cache of account name |
| `note` | TEXT | Transaction description note |
| `timestamp` | INTEGER | Time of transaction (epoch ms) |
| `userEmail` | TEXT | Creator email address (nullable) |
| `isSynced` | INTEGER (Boolean) | Synchronization status flag |
| `updatedAt` | INTEGER | Last-Write-Wins (LWW) timestamp (epoch ms) |
| `isDeleted` | INTEGER (Boolean) | Soft-delete tombstone flag |
| `deletedAt` | INTEGER | Deletion timestamp (epoch ms, nullable) |
| `splits` | TEXT | Serialized JSON array of TransactionSplit items |

### 2. `categories` (CategoryEntity)
Stores categories. It is pre-populated with defaults.

| Column | SQLite Type | Description |
| :--- | :--- | :--- |
| `id` | TEXT (Primary Key) | Unique UUID String |
| `name` | TEXT | Display name |
| `type` | TEXT | `EXPENSE` or `INCOME` |
| `icon` | TEXT | Name of icon resource |
| `isDefault` | INTEGER (Boolean) | Flag indicating a default app category |
| `chartColor` | TEXT | Hex color string |
| `userEmail` | TEXT | Creator email address (nullable) |
| `isSynced` | INTEGER (Boolean) | Synchronization status flag |
| `updatedAt` | INTEGER | Last-Write-Wins (LWW) timestamp (epoch ms) |
| `isDeleted` | INTEGER (Boolean) | Soft-delete tombstone flag |
| `deletedAt` | INTEGER | Deletion timestamp (epoch ms, nullable) |
| `budget` | REAL | Budget limit for Expense categories |

### 3. `accounts` (AccountEntity)
Stores active user/household accounts.

| Column | SQLite Type | Description |
| :--- | :--- | :--- |
| `id` | TEXT (Primary Key) | Unique UUID String |
| `name` | TEXT | Account name |
| `icon` | TEXT | Icon resource name |
| `isDefault` | INTEGER (Boolean) | Flag indicating primary default account |
| `color` | TEXT | Hex color string |
| `shareCode` | TEXT | 6-character random household code (nullable) |
| `ownerEmail` | TEXT | Creator owner email address (nullable) |
| `userEmail` | TEXT | Associated user email address (nullable) |
| `isSynced` | INTEGER (Boolean) | Synchronization status flag |
| `updatedAt` | INTEGER | Last-Write-Wins (LWW) timestamp (epoch ms) |
| `isDeleted` | INTEGER (Boolean) | Soft-delete tombstone flag |
| `deletedAt` | INTEGER | Deletion timestamp (epoch ms, nullable) |
| `carryOver` | INTEGER (Boolean) | Carry balance over between periods |

### 4. `preferences` (PreferenceEntity)
Stores persistent settings preferences.

| Column | SQLite Type | Description |
| :--- | :--- | :--- |
| `key` | TEXT (Primary Key) | Preference key (e.g. `sheets_url`, `tabs_position`) |
| `value` | TEXT | Preference string value |

---

## 3. UI Aesthetics & Themes

The application dynamically renders two distinct themes matching the visual style of the sample screenshots:

### A. Chalkboard Theme (Used in the Spending Tab)
* **Background**: Slate charcoal dark gray (`#2A2B2D` with chalk texture grain).
* **Typography**: Chalk-drawn handwriting font (Google Fonts: "Patrick Hand" loaded via `patrick_hand.ttf`).
* **Accents**:
  * Income Text: Chalk green (`#8FCE5E`).
  * Expense Text: Chalk red (`#F07D7D`).
  * Balance Text: Chalk blue (`#87CEEB`).
  * Action Buttons: Slate gray fill with white chalk-outline borders.
* **Segmented Progress Bar**: Visual balance indicator (green portion representing relative income share, red representing expense share).

### B. Parchment Theme (Used in Transactions, Categories, Accounts, and Settings)
* **Background**: Slate charcoal dark gray (`#2A2B2D` with chalk grain, globally aligned with chalkboard aesthetics).
* **Typography**: Chalk-drawn handwriting font (`patrick_hand.ttf`).
* **Lines / Borders**: Dashed chalk-drawn borders and separators using dark gray (`#8B8C8D`).

### C. Landscape Reports (Chalkboard Canvas charts)
* **Orientation Redirect**: When the device orientation shifts to landscape, the application redirects the user to the [LandscapeReports.kt](app/src/main/java/com/example/vellum/ui/main/LandscapeReports.kt) dashboard.
* **Account-Level Filtering**: When any account filter is selected on the main screen, the landscape dashboard automatically displays charts and cash flows filtered specifically to that account.
* **Canvas Charts**:
  - **Pie Chart**: Visualizes categories of expense distributions for the filtered account.
  - **Bar Chart**: Draws all active category expense bars side-by-side. The container dynamically calculates width and supports smooth horizontal scrolling if the categories exceed the screen width. Category labels are rotated 45 degrees to avoid overlapping.
  - **Cash Flow Line Chart**: Offers selector options (`D`, `W`, `M`, `Y`) to graph Income vs Expense over Daily, Weekly, Monthly, or Yearly intervals over the last 12 periods.
* **Layout Constraints**: The landscape reports utilize the screen's safe boundaries, preserving the default system status and navigation bar visibility.

---

## 4. Settings Reference Specifications

The settings pane contains:
1. **Spending**:
   * *Time Period*: Show spending based on Daily, Weekly, Monthly (Default), or Yearly intervals.
   * *Budget Mode*: Toggle budget settings.
   * *Carry Over*: Carry balance over between periods.
   * *Hide Future Transactions*: Hide future entries.
2. **Automatic Syncing**:
   * *Dropbox Sync*: Authenticate and sync with Dropbox storage.
   * *Google Sheet Sync URL*: Paste deployed Google Apps Script Web App gateway URL.
3. **User Interface**:
   * *Dark Theme*: Force global dark theme.
   * *Show Transaction Note*: Display descriptions in transaction lists.
   * *Currency Symbol*: Currency symbol settings.
   * *Summary Font*: Toggle summary screen between "Chalk" and standard font.
   * *Category Icon Style*: Outlined or filled icons.
   * *Tabs Position*: Top (default) or bottom.
4. **General**:
   * *Reminders*: Notification preferences.
   * *Auto Backup*: Automated database exporting.
   * *Passcode*: Toggle PIN code authentication before opening the app.

---

## 5. Advanced Features & Developer Utilities

### 1. Usage-Based Category Sorting
To optimize the user experience during entry input, both the **Categories Tab** and the **Add Transaction** dialog sort category options dynamically:
- Collects the complete transaction history flow (`allTransactions`).
- Maps each category to its usage count in existing transactions.
- Sorts categories descending by usage frequency, placing the most active categories at the top.

### 2. Settings-Driven Spreadsheet Backups
Google Apps Script (`Code.gs`) integrates directly with the synced user preference `auto_backup`:
- **Trigger Management (`manageBackupTrigger`)**: Runs during sheet setup and data synchronization. If `auto_backup` is enabled (`"On"`) for any user, it dynamically schedules a time-driven trigger daily at 11:30 PM. If disabled (`"Off"`), it deletes the trigger to save Apps Script quota.
- **Daily Backup (`runDailyBackup`)**: Automatically copies all active sheet ranges (`transactions`, `categories`, `accounts`, `preferences`, `shares`, `users`) to backup sheets (prefixed with `backup_`).

### 3. Debug Mode Clean-Slate
For testing and development velocity, the app enforces a clean slate on debug installations:
- On the first run of a debug build, the app triggers a sign-out of the Google account and completely clears and re-seeds the local SQLite Room database, ensuring that developers start from a pristine onboarding state.
- Checked using a `"first_run_debug"` flag in Android `SharedPreferences`.

### 4. Apps Script Manual Reset Utility
A manual database reset utility `resetSpreadsheet()` is provided in the Apps Script backend for manual testing. When executed from the Apps Script editor, it drops custom sheets, resets the standard tables to empty states with schema headers, and deletes all active triggers.

---

## 6. Version Control & Repository

### Repository
- **GitHub**: [github.com/foolchauhan/vellum](https://github.com/foolchauhan/vellum)
- **Remote protocol**: SSH (`git@github.com:foolchauhan/vellum.git`)

### Branch Model

| Branch | Purpose |
|---|---|
| `main` | Stable production-ready code. No direct commits. |
| `develop` | Integration branch for completed features. No direct commits. |
| `release/release-x.y.z` | Frozen release snapshots. No direct commits. |
| `feature/name` | Active development — branch from `develop` |
| `fix/name` | Bug fixes — branch from `develop` |
| `chore/name` | Non-functional changes — branch from `develop` |

### Rules
1. **No direct commits** to `main`, `develop`, or `release/*` — enforced via GitHub Branch Protection Rules.
2. All changes require a **Pull Request** and **approval** before merging.
3. To move changes from `develop` into a `release/*` branch: create a branch from the release branch, merge develop into it, resolve conflicts, push, open PR.
4. See [AGENT.md](AGENT.md) — Section 4 for the complete step-by-step workflow.

---

## 7. Sync Reliability & Conflict Handling (Implemented — Phase 7)

We have fully implemented the conflict resolution and sync reliability plan on the `feature/sync-reliability-and-conflict-handling` branch.

### 7.1 Schema Changes — `Entities.kt` (Room DB Migration Version 3 → 4)
* **Added columns** to `TransactionEntity`, `CategoryEntity`, and `AccountEntity`:
  * `updatedAt` (`Long`): Last-Write-Wins (LWW) timestamp.
  * `isDeleted` (`Boolean`): Soft-delete tombstone flag.
  * `deletedAt` (`Long?`): Deletion timestamp.

### 7.2 DAO Queries & Soft-Delete — `Daos.kt`
* Filtered out `isDeleted = 1` rows from active UI flows.
* Added `markDeleted` soft-delete update queries.
* Retained raw `@Delete` calls only for cache clearing and sync replacement.

### 7.3 Repository & ViewModel soft-deletes
* Implemented `softDeleteTransaction`, `softDeleteCategory`, and `softDeleteAccount` in `DataRepository`.
* Added `leaveAccount()` (removes local account row only, preserving linked transactions) for non-owner member departures.
* Branched UI delete buttons in `AddEditAccountScreen` on `isOwner`: calls `deleteAccount` if owner, or `leaveSharedAccount` if non-owner member.

### 7.4 Display-Time Joins
* Transactions lists resolve category and account names dynamically at display time using live lists. Falls back to "(Deleted Account)" or the cached column as a safety net if missing.

### 7.5 Idempotency and LWW Sync Gateway
* Switched sync engine (`SheetsSyncManager.kt`) to POST `left_accounts` tombstones and perform standard GET-first LWW merges.
* Updated `Code.gs` to perform upserts by UUID (not appends) on the sheet, making sync requests completely idempotent.
* Implemented owner delete guards in `Code.gs` preventing account deletion if other members are still active.

---

## 8. Full-Screen Refactoring & CSV Bulk Upload (Implemented — Phase 8)

We have refactored all popup dialog forms into dedicated full screens, added category auto-selection, and introduced settings-based CSV bulk upload.

### 8.1 Full-Screen Form Architecture
* Refactored transaction, category, and account Add/Edit popup dialogs into distinct composable screens: `AddEditTransactionScreen`, `AddEditCategoryScreen`, and `AddEditAccountScreen`.
* Switched form routing to utilize `Navigation3` screen transitions.
* Implemented category auto-selection when returning from category creation inside the transaction screen using a reactive `LaunchedEffect` that compares category ID listings.

### 8.2 CSV Bulk Upload & Template Download
* Added `bulkUploadTransactions(csvText)` in `DataRepository` and `MainScreenViewModel` to parse standard templates and auto-create/tag missing categories or accounts.
* Appended an **Advanced Options** section to `SettingsScreen` with **Download CSV Template** (via SAF `CreateDocument`) and **Bulk Upload CSV** (via `GetContent`) options.
* Defined custom Settings row icons for download/upload in `TransactionRow.kt` mapping to `ArrowDownward` and `ArrowUpward` Material vectors.

### 8.3 Spending Tab breakdown & Rotated Labels
* Restructured vertical `SpendingTab` into a scrollable container with sticky headers and action buttons. Added a **Spending by Category** list showing colored category expense breakdowns for the selected account.
* Implemented an inline **category color collision resolver** that substitutes duplicate colors from a pool of distinct colors, ensuring active categories have unique colors on both vertical and landscape charts.
* Increased bottom canvas padding in `BarChartCanvas` to `70.dp` and rotated category labels by `45 degrees` using `withTransform` to resolve text overlapping.

---

## 9. UI Polish, Custom PDF/CSV Exports, and Renamed Release APK (Implemented — Phase 9)

We have polished form accessibility/consistency, introduced customizable CSV and PDF document exports, and automated shareable APK compilation.

### 9.1 UI Consistency & Keyboard Avoidance
- Modified Navigation flows to pass down `safeDrawingPadding()` to `AddEditTransactionScreen`, `AddEditCategoryScreen`, and `AddEditAccountScreen`.
- Standardized form boundaries to run within safe app regions (excluding system status and navigation bars).
- When the soft keyboard opens, the main container's layout shrinks by the IME height, ensuring notes and action buttons at the bottom of forms remain fully viewable and scrollable.

### 9.2 Spending Period Account Context
- Configured the centered period navigation switcher on the Spending tab to display the active account's name directly below the period label, providing instantaneous context on what data is being graphed.

### 9.3 Custom PDF/CSV Export Engine
- Implemented `ExportManager.kt` containing robust PDF and CSV generators.
- PDF Statement Generation: Renders multi-page A4 PDF documents. Features an elegant blackboard background header utilizing the app's real `blackboard_background_01` resource, draws the actual `ic_launcher` app icon, prints active filters metadata, renders grid summary metrics cards, computes category-wise progress bars, and lists transaction tables with green/red formatted amounts and page-numbered footers.
- Share Settings Dialog: Added filters inside `ShareExportDialog.kt` allowing users to configure accounts, period types, and specific date/week/month/year ranges before running the SAF `CreateDocument` file saving activity. Filenames are constructed dynamically (e.g. `vellum_personal_report_may_2026.pdf`).

### 9.4 Release APK Automation
- Custom variant rules in `app/build.gradle.kts` output the optimized release package directly as `Vellum.apk`.
- Configured to use the debug keystore signing config so that friends and family can immediately install it, while avoiding the startup database wipes triggered by `BuildConfig.DEBUG` checks in debug builds.

---

## 10. Vellum Version 2.0.0 Release (Implemented — Phase 10)

We have successfully released Vellum Version 2.0.0 on the `feature/version-2.0.0` branch, introducing classroom themes, count-up animations, eraser wipes, split transactions, biometric lock screen, OCR scanning, AI categorization, visual conflict UI, offline indicators, home screen widgets, and multi-app side-by-side deployment.

### 10.1 Schema & Migrations — `VellumDatabase.kt` (Room DB Migration Version 5 → 6)
* Added columns:
  * `budget` (`Double`): Remaining expense limits on `CategoryEntity` (default `0.0`).
  * `splits` (`String`): JSON array of serialized split transaction objects on `TransactionEntity` (default `""`).
* Registered `MIGRATION_5_6` executing the `ALTER TABLE` schema expansion statements.

### 10.2 Envelope Budgeting & Split Transactions
* Added a **Budget Limit** input field in the Category screen for expense categories.
* Spending Tab draws hand-drawn chalk-style progress bars showing depleted/remaining envelopes.
* Integrated a **Split Categories/Accounts** form inside the transaction screen, serializing sub-transactions as JSON arrays inside the main transaction record.
* Updated `spendingMetrics` and combine flows to accurately calculate total income/expense/balance using transaction sub-splits.

### 10.3 Biometric Security & ML Kit OCR Scanner
* Configured settings-controlled FaceID/Fingerprint lock on startup, showing a beautiful custom chalkboard lock screen overlay if biometric lock is turned on.
* Integrated **Scan Receipt** OCR engine in transaction form. It uses ML Kit text recognition to process receipt text and populate the largest parsed price heuristically.
* Integrated notes-keyword AI rule categorization that matches keywords to auto-select categories.

### 10.4 Conflict Resolution & Offline Sync Visualizer
* Implemented visual **Sync Conflict Dialog** comparing local versus remote transactions side-by-side. It lets the user keep the local or cloud version.
* Implemented a topbar **Offline Sync Queue** indicator. It draws a chalk-drawn cloud icon displaying the number of pending unsynced records dynamically.

### 10.5 Chalkboard Widgets & Side-by-Side Deployment
* Added `ChalkboardWidgetProvider` rendering Today's Spend and Current Balance onto a chalkboard canvas bitmap, updating automatically in real-time.
* Configured debug variant with package suffix `.v2` and manifest placeholder appName `"Vellum 2.0"` to deploy alongside the stable version.

---

## 11. Vellum Usability, AI Semantic Search, and Visual Polish (Implemented — Phase 12)

We have implemented visual optimizations, extended the available set of icons, added a concept-based natural language search engine, and improved navigation and layout behavior.

### 11.1 On-Device Concept Semantic Matcher
* Introduced [SemanticMatcher.kt](app/src/main/java/com/example/vellum/data/SemanticMatcher.kt), a lightweight concept-matching engine mapping transaction queries to 5 specific financial dimensions (Food, transport, shopping, income, utilities).
* Queries split search phrases, average vector weights, compute cosine similarity, and resolve semantic searches.
* Integrates inside [TransactionsTab.kt](app/src/main/java/com/example/vellum/ui/tabs/TransactionsTab.kt) and parses natural language questions (e.g., "how much did I spend on food") with direct summary statistics (sum, average, count, balance).

### 11.2 Massive Icon Library Expansion & Scrollable Layouts
* Upgraded the icon set in [TransactionRow.kt](app/src/main/java/com/example/vellum/ui/components/TransactionRow.kt) to include over 80 custom expense, income, and account icons.
* Wrapped the category and account icon selection grids inside [AddEditCategoryScreen.kt](app/src/main/java/com/example/vellum/ui/main/AddEditCategoryScreen.kt) and [AddEditAccountScreen.kt](app/src/main/java/com/example/vellum/ui/main/AddEditAccountScreen.kt) in a scrollable Box with a maximum height of `300.dp`. This resolves vertical page overflows and keeps action forms compact.

### 11.3 Layout Polish & Spacing Optimizations
* **Landing Page redirection**: Initialized pager state to page index `1` inside [MainScreen.kt](app/src/main/java/com/example/vellum/ui/main/MainScreen.kt) to boot the application directly into the **Spending** tab.
* **Financial Tutor Card Compact Layout**: Optimized left-side spacing on the Tutor Card in [SpendingTab.kt](app/src/main/java/com/example/vellum/ui/tabs/SpendingTab.kt). Shrinks the canvas drawing boundaries of the tutor owl from `56.dp` to `36.dp` and utilizes `withTransform` scale modifiers to scale vector coordinates dynamically, freeing up a significant amount of screen width for the text blocks.
* **Filter State Persistence**: Modified [Navigation.kt](app/src/main/java/com/example/vellum/Navigation.kt) and [MainScreenViewModel.kt](app/src/main/java/com/example/vellum/ui/main/MainScreenViewModel.kt) sign-in flows to pass `isRestore = true` and preserve selected account and category filters across screen rotation, avoiding default reset behavior.


