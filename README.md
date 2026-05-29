# Vellum: Android Spending Tracker App

> [!NOTE]
> For active session context, screenshot analysis summary, and implementation checklists, see **[HANDOVER.md](HANDOVER.md)**.

Vellum is a native Android spending tracker app that offers a peaceful, rustic, and chalkboard-style tracking experience. The app is written in Kotlin using Jetpack Compose for UI development, Kotlin Flows for reactive state streaming, and Room SQLite for offline-first local caching.

---

## Visual Themes

1. **Chalkboard (Spending Dashboard)**: A textured slate chalkboard interface. Displays income, expenses, and current balances using chalky handwriting fonts.
2. **Parchment (Logs & Managers)**: A textured beige parchment paper journal style. Used for list operations, settings rows, and database logs with organic, pencil-drawn boundaries.

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
- `feature/*` → `develop`: open a PR on GitHub, get approval, then merge
- `develop` → `release/*`: create a new branch from the release branch, `git merge origin/develop`, resolve conflicts, push, open PR

See [AGENT.md](AGENT.md) — Section 4 for the full detailed workflow.

---

## GitHub Repository

- **URL**: [github.com/foolchauhan/vellum](https://github.com/foolchauhan/vellum)
- **Remote protocol**: SSH (`git@github.com:foolchauhan/vellum.git`)
