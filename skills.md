# Expert Capabilities & Engineering Skills: Vellum (Android App)

The implementation of Vellum leverages specific advanced engineering methodologies across Android development, database systems, and layout design.

---

## 1. Native Kotlin & Coroutines Flow
*   **Asynchronous Flow Streams**: Exposing database queries as cold Kotlin Flow streams, allowing reactive UI components to auto-refresh whenever transactions are written or modified.
*   **Structured Concurrency**: Using Kotlin Coroutines with appropriate dispatcher scopes (`Dispatchers.IO` for Room DB calls, `Dispatchers.Main` for layout updates).

---

## 2. Jetpack Compose UI
*   **Declarative Interface Components**: Defining reusable Compose elements with state hoisting and preview support.
*   **Custom Layout Theme Drawing**: Drawing realistic chalkboard slate (charcoal shade with grain details) and parchment paper (cream beige shade with outline borders) using canvas modifiers, custom shape drawings, and Google Fonts.
*   **Segmented Bars**: Creating progress layouts with custom Canvas draws representing income relative to expenses.
*   **Dynamic Canvas Coordinate Scaling**: Utilizing `withTransform` and `scale` blocks inside Jetpack Compose `Canvas` drawing scopes to scale vector components and complex shapes dynamically depending on current layouts.
*   **Scrollable Custom Charting**: Designing horizontally scrollable custom canvas charts (e.g., bar graphs displaying dozens of categories side-by-side) with 45-degree rotated label boundaries.

---

## 3. Local SQLite Storage (Room DB)
*   **Schema Layout**: Implementing structured SQL tables for transactions, category tags, accounts, and preferences.
*   **Database Pre-population**: Seeding the database with default categories (Clothes, Shopping, Fuel, Salary, etc.) and accounts on the first launch of the database.
*   **Settings Preferences Cache**: Storing toggles (Chalk font, dark theme, lock passcode) inside key-value preference tables.

---

## 4. Gradle & Android Environment CLI
*   **Dependency Management**: Managing SDK packages and build configurations using Gradle Kotlin DSL (`build.gradle.kts` and `libs.versions.toml`).
*   **Emulator Launching**: Deploying and launching debug builds onto connected Virtual Devices using the `android` developer tool.

---

## 5. File Exporting & PDF Document Generation
*   **Structured CSV Generation**: Serializing SQLite transaction entities into a standardized CSV format matching the household upload template.
*   **Native PDF Canvas Layouts**: Drawing professional, multi-page financial summary reports using Android's native `PdfDocument` and `Canvas` API. Features pixel-perfect positioning, drawing of complex image resources (chalkboard background texture and launcher mipmap icon), category breakdown progress bars, summary metric cards, alternating-row data tables, and dynamic footer page numbering.

---

## 6. On-Device AI & Semantic Matching
*   **Vector Embeddings & Cosine Similarity**: Computing lightweight concept vector embeddings and calculating cosine similarity in Kotlin ([SemanticMatcher.kt](app/src/main/java/com/example/vellum/data/SemanticMatcher.kt)) to answer semantic natural language queries locally without external APIs.
*   **OCR Receipt Parsing**: Utilizing Google ML Kit Text Recognition to process receipts, extract line contents, and parse transaction properties (e.g., price and transaction category) automatically.

