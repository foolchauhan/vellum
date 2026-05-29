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
3. The `SheetsSyncManager` POSTs all un-synchronized local changes to the Google Apps Script Web App.
4. The Web App updates the Google Sheet, merges rows from all active household participants sharing the account, and returns the merged datasets.
5. The local cache is replaced in a single Room database transaction to prevent UI flickering.

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
* **Orientation Redirect**: When the device orientation shifts to landscape, the application redirects the user to the `LandscapeReports` dashboard.
* **Canvas Charts**:
  - **Pie Chart**: Visualizes categories of expense distributions.
  - **Bar Chart**: Groups categories in a paginated bar chart (5 items per page) with centered layout and a clean baseline, using top-left page selectors (`-` and `+`).
  - **Cash Flow Line Chart**: Offers selector options (`D`, `W`, `M`, `Y`) to graph Income vs Expense over Daily, Weekly, Monthly, or Yearly intervals.
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

