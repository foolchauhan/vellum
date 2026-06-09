# Vellum: Android Spending Tracker App

> [!NOTE]
> For active session context, screenshot analysis summary, and implementation checklists, see **[HANDOVER.md](HANDOVER.md)**.

## 📥 Downloads & Releases

Get the latest installable release of Vellum immediately:

*   **[Download Vellum.apk (v2.0.0 - Latest)](https://github.com/foolchauhan/vellum/raw/main/Vellum.apk)**
    *   *Signed with the debug keystore for easy installation on any Android device.*
    *   *No automated debug-mode startup database wipes.*

### Previous Versions
If you need to download older builds:

| Version | Release Date | Download Link |
|---|---|---|
| **v2.0.0** (Latest) | 2026-06-01 | [Vellum-2.0.0.apk](https://github.com/foolchauhan/vellum/raw/main/apks/Vellum-2.0.0.apk) |
| **v1.0.0** | 2026-05-31 | [Vellum-1.0.0.apk](https://github.com/foolchauhan/vellum/raw/main/apks/Vellum-1.0.0.apk) |

---

Vellum is a native Android spending tracker app that offers a peaceful, rustic, and chalkboard-style tracking experience. The app is written in Kotlin using Jetpack Compose for UI development, Kotlin Flows for reactive state streaming, and Room SQLite for offline-first local caching.

---

## Visual Themes

1. **Chalkboard (Spending Dashboard)**: A textured slate chalkboard interface. Displays income, expenses, and current balances using chalky handwriting fonts.
2. **Parchment (Logs & Managers)**: A textured beige parchment paper journal style. Used for list operations, settings rows, and database logs with organic, pencil-drawn boundaries.

---

## 🌟 Version 2.0.0 Features

Vellum v2.0.0 is a major release transforming the app into an award-winning finance tracker with professional utility and classroom aesthetics:

- **Custom Classroom Themes**: Includes Greenboard and Blueprint chalkboard themes in addition to classic Dark/Light modes.
- **Handwriting Typography Selector**: Choose from Cabin Sketch, Patrick Hand, Fredericka the Great, and Caveat handwriting fonts.
- **Chalkboard Animations**: Features smooth chalkboard eraser wiping sweep animations and count-up balance tallies.
- **Envelope Budgeting**: Set budget limits for categories and view hand-drawn chalk-style progress bars of remaining envelopes.
- **Split Transactions**: Split individual transaction entries into multiple sub-categories and accounts.
- **Smart Utilities**:
  - **AI Semantic & Natural Language Search**: Enter natural language questions in the transaction search bar (e.g., "how much did I spend on food in June?") to instantly calculate total expenses, income, averages, net balance, and transaction counts using a lightweight on-device vector matcher.
  - **Biometric Security**: Protect your finances with Fingerprint/FaceID lock screens on startup.
  - **Receipt OCR Scanner**: Instantly extract and populate price amounts using on-device ML Kit text recognition.
  - **AI Categorization**: Automatically categorizes transactions based on note keywords.
- **Conflict Resolution UI**: View and choose between local and server states side-by-side during sync conflicts.
- **Offline Sync Queue**: Track pending offline records with a top-bar chalk-drawn cloud icon and badge count.
- **Interactive Home Widgets**: Display today's spend and current balance on home screen widgets rendered in real chalkboard chalk fonts.
- **Massive Icon Library**: Choose from 80+ custom categories and account icons (53 Expense, 18 Income, 11 Account) with scrollable picker grid dialogs.
- **Premium Landscape Analytics**: Horizontally scrollable bar chart displays all categories side-by-side with 45-degree rotated labels, and automatically filters all charts by the active account.
- **Layout & Filter Polish**: Preserves selected account filters on screen rotations and defaults the startup landing page to the **Spending** tab.
- **Multi-App Side-by-Side Deployment**: Run Vellum 2.0 side-by-side with Vellum 1.0 on a single device for development and testing.

---

## Project Structure

```
Vellum/
├── build.gradle.kts       # Root gradle build configuration
├── settings.gradle.kts    # Gradle project settings
├── gradle.properties      # Compiler options
├── gradlew                # Gradle wrapper script (Unix)
├── gradlew.bat            # Gradle wrapper script (Windows)
├── local.properties       # Local Android SDK paths
├── PROJECT.md             # Systems architecture & Room DB schemas
├── AGENT.md               # Team role specifications
├── skills.md              # Engineering capabilities matrix
├── screenshots/           # Reference screens showing exact design layouts
└── app/
    ├── build.gradle.kts   # App-level build configurations
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── java/com/example/vellum/
            │   ├── MainActivity.kt        # Entry-point activity
            │   ├── Navigation.kt          # Compose Navigation routes
            │   ├── NavigationKeys.kt      # Typesafe navigation arguments
            │   ├── data/
            │   │   ├── DataRepository.kt  # Repository pattern exposing flows
            │   │   └── local/
            │   │       ├── Entities.kt    # Room database tables (transactions, categories, etc.)
            │   │       ├── Daos.kt        # Room database DAO definitions
            │   │       └── VellumDatabase.kt # Preloaded SQLite database instance
            │   ├── theme/
            │   │   ├── Color.kt           # Chalk and parchment hex tokens
            │   │   ├── Theme.kt           # Custom VellumComposeTheme
            │   │   └── Type.kt            # Google Fonts dynamic font loaders
            │   └── ui/main/
            │       ├── MainScreen.kt      # Parent Screen hosting the tabs
            │       ├── MainScreenViewModel.kt # State manager for database events
            │       ├── LandscapeReports.kt    # Orientation-specific reports (Pie, Bar, Line charts)
            │       └── AppScreens.kt      # Custom screen layouts matching screenshots
            └── res/
                ├── values/
                │   ├── strings.xml
                │   └── themes.xml
                └── xml/
```

---

## Compiling & Running Locally

Ensure you have Android SDK and Gradle set up on your machine. The local SDK path is configured under `local.properties`.

### Step 1: Compile the Project
Verify compilation and compile a debug APK using:
```bash
./gradlew assembleDebug
```

### Step 1b: Compile the Shareable Release APK
To compile the optimized release APK that is signed with the debug keystore (enabling immediate installation on physical devices for friends and family without database wipe-on-startup hooks):
```bash
./gradlew assembleRelease
```
The build process automatically renames and copies the output file to the root of the project:
`Vellum.apk`

### Step 2: List Virtual Devices
List connected devices or active emulator profiles using:
```bash
android emulator list
```

### Step 3: Run the App
Launch the app on a connected virtual emulator using:
```bash
android run --device=Pixel_4_XL_API_29
```
*(Swap the `--device` argument for any active emulator ID from step 2)*

---

## Git Branching Model

> [!IMPORTANT]
> **Never commit directly to `develop` or `release/*` branches.** All code changes go through a feature branch and a Pull Request.

### Branch Structure

```
main                              ← stable, production-ready
├── release/release-2.0.0        ← v2.0.0 frozen snapshot
├── release/release-1.0.0        ← v1.0.0 frozen snapshot
├── release/release-x.y.z        ← future release snapshots
└── develop                      ← active integration branch
    ├── feature/description      ← new features (branch from develop)
    ├── fix/description          ← bug fixes (branch from develop)
    └── chore/description        ← maintenance (branch from develop)
```

### Starting Development

```bash
# Always start by checking your current branch
git branch --show-current

# If not on a feature branch, create one from develop
git checkout develop && git pull origin develop
git checkout -b feature/my-new-feature
```

### Merging Back (Always via PR)
- `feature/*` → `develop`: open a PR on GitHub, get approval, then merge. **Keep the branch.**
- `develop` → `release/*`: create a new branch from the release branch, `git merge origin/develop`, resolve conflicts, push, open PR. **Keep the integration branch.**

> **Branches are never deleted** — all feature, fix, chore, and integration branches are retained permanently for full history traceability.

See [AGENT.md](AGENT.md) — Section 4 for the full detailed workflow.

---

## GitHub Repository

- **URL**: [github.com/foolchauhan/vellum](https://github.com/foolchauhan/vellum)
- **Remote protocol**: SSH (`git@github.com:foolchauhan/vellum.git`)
