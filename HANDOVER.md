# Vellum Android Application - Handover & Status

This document preserves the current status, codebase architecture, achievements, and completed tasks for the Vellum Android project.

> [!CAUTION]
> **Session Start Rule**: Before touching any file, run `git branch --show-current`. If you are on `develop`, `main`, or `release/*` — STOP and create a feature branch first. See [AGENT.md](AGENT.md) Section 4 for full workflow.

---

## 1. Active Context & Current State

1. **Platform**: Native Android App (Kotlin, Jetpack Compose, Room SQLite database).
2. **Local Environment**:
   - SDK: `~/Library/Android/sdk`
   - Gradle wrapper: `./gradlew`
   - Device: Connected physical phone: `adb-10BFBJ1Y4G001TE-06Tqpr._adb-tls-connect._tcp`
3. **Git Repository**:
   - Remote: `git@github.com:foolchauhan/vellum.git` (SSH)
   - Active branches: `main`, `develop`, `release/release-1.0.0`
   - **Current working branch**: `main` *(switch to `develop` and create a feature branch before starting work)*
   - Branch protection required on `develop` and `release/*` — all changes via PR only
4. **Codebase Navigation Guide**:
   - [MainActivity.kt](app/src/main/java/com/example/vellum/MainActivity.kt): App entrypoint, Google Sign-in flow host.
   - [Navigation.kt](app/src/main/java/com/example/vellum/Navigation.kt): Main navigation entry provider and Google account sign-in restoration.
   - [MainScreen.kt](app/src/main/java/com/example/vellum/ui/main/MainScreen.kt): Main dashboard page shell, horizontal pager container, tab indicators, and Google Sheets manual refresh button.
   - [AppScreens.kt](app/src/main/java/com/example/vellum/ui/main/AppScreens.kt): Spending graph layouts, transaction row managers, Add transaction/account/category forms, and custom Google Profile Card & UrlInputDialog.
   - [SheetsSyncManager.kt](app/src/main/java/com/example/vellum/data/SheetsSyncManager.kt): Network sync engine resolving 302/307/308 redirection responses.
   - [Type.kt](app/src/main/java/com/example/vellum/theme/Type.kt): Typography configuration using handwriting `patrick_hand.ttf` font.
   - [Color.kt](app/src/main/java/com/example/vellum/theme/Color.kt): Remaps design tokens to chalkboard slate aesthetics.

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
   - settings pane features a Google Profile Card for logging in and logging out.
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

### Phase 5: Seamless Transitions & Premium Landscape Reports Dashboard
1. **Seamless Transitions**:
   - Made the background completely static and seamless across all tabs by removing the overlay background fade in `MainScreen.kt`, eliminating transition lag and visual gaps.
2. **System Boundaries**:
   - Restored standard system bar margins (status bar and bottom nav bar boundaries) in landscape orientation, respecting safe drawing boundaries.
3. **Pagination & Centered Bar Charts**:
   - Groups category bars closely together and centers them on the canvas.
   - Drawn a clean horizontal baseline supporting better viewability.
   - Sliced the category data into pages of 5 items, allowing users to scroll page-by-page using the top-left `-` and `+` action buttons.
4. **Cash Flow Interval Selector & Dynamic Currency Labels**:
   - Implemented an interval selector toggle (`D`, `W`, `M`, `Y`) in the top-left of the Cash Flow screen.
   - Added `calculateCashFlowData` to dynamically partition transaction statistics into Daily, Weekly, Monthly, and Yearly intervals over the past 12 periods.
   - Added a dynamic period title in the top-right (e.g. `"Last 12 Days"`, `"Last 12 Weeks"`) based on the selected interval.
   - Retrieved the user's active currency symbol (`currencySymbol`) and passed it to the chart canvases, replacing hardcoded symbols.

---

## 3. Verification & Deployment Status

- **Compilation**: Successfully compiles with Gradle without warnings or errors.
- **Packaging**: Successful APK assembly using `./gradlew assembleDebug`.
- **Deployment**: Installed and verified on the connected target device (`192.168.1.4:35535`).
