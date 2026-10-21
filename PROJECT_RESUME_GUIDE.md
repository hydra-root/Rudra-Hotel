# Hotel Rudra Amruttulya — Android Billing & POS Management System
**Programmer & Author:** Pavan Pawar  
**Role:** Android Application Developer  
**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Dagger-Hilt, Room ORM (SQLite), Coroutines, Flow, ZXing (UPI QR), Clean Architecture

---

## 📌 One-Line Project Summary (For Resume / LinkedIn / Portfolio)
> **"Architected and developed a full-featured, offline-first Hotel Point-of-Sale (POS) and B2B Supply Management Android application using Jetpack Compose, Room ORM, and Dagger-Hilt, featuring dynamic UPI Payment QR receipt generation, live 20-table dining management, daily expense auditing, and an intelligent delivery route prediction engine."**

---

## 📄 Ready-to-Copy Resume Bullet Points (ATS-Friendly)

### Option A: Standard Full Bullet Points (Recommended for Software Engineer Resumes)

**Hotel Rudra POS & Management System — Android Developer** | *Kotlin, Jetpack Compose, Room, Hilt*
* **Engineered an offline-first Hotel POS & Billing Android Application** using **Jetpack Compose**, **Kotlin Coroutines/Flow**, and **Clean Architecture (MVVM)**, streamlining order lifecycle and ledger tracking across 20+ dining tables and commercial clients.
* **Integrated Dynamic NPCI-compliant UPI Payment QR generation** utilizing **ZXing** and Android Graphics **Canvas**, automatically embedding pre-filled payment links with exact bill amounts and merchant VPA on exported PDF bills for instant Google Pay/PhonePe settlement.
* **Designed a robust Room database layer with 5 schema migrations**, indexing foreign-key relations across tables for customers, products, sales, payments, dining table orders, and daily expense tracking.
* **Built an intelligent Markov-chain inspired Route Prediction Engine** that analyzes historical customer delivery sequences with an exponential 10%/week time-decay weight to dynamically suggest optimal daily delivery stops.
* **Developed bilingual PDF invoice and report generators** (Marathi & English) supporting custom typography (Yatra One / Mukta), paginated customer statements, daily revenue reports, and categorized expense audits.
* **Implemented an offline Daily Expense Tracker** with real-time KPI aggregations (daily/monthly totals), category tagging (Milk, Gas, Salary, Grocery), and instant PDF expense sheet generation.
* **Ensured data security and durability** with local SQLite database file backup/restore mechanisms, CSV export workflows, and passcode-protected app lock.

---

### Option B: Compact Format (For 1-Page Resumes)

**Hotel Rudra POS & Billing System** | *Kotlin, Jetpack Compose, Room, Hilt, ZXing*
* Developed an offline-first Hotel POS & Supply Management app using Jetpack Compose and Clean Architecture.
* Implemented dynamic UPI QR code receipt generation, table ordering, and daily expense auditing.
* Engineered Room SQLite persistence with 5 schema migrations and reactive StateFlow streams.
* Built a predictive routing algorithm utilizing historical weighted transitions and time-decay for delivery runs.
* Integrated custom Canvas-based bilingual (Marathi/English) PDF billing and financial statement reporting.

---

## 🛠️ Technical Skills Demonstrated

| Category | Technologies & Tools |
| :--- | :--- |
| **Language** | Kotlin 1.9, Coroutines, StateFlow, SharedFlow |
| **UI & UX** | Jetpack Compose, Material 3, Glassmorphism, Canvas 2D Drawing, Compose Animation |
| **Architecture** | Clean Architecture (Presentation, Domain, Data), MVVM, Repository Pattern, Use Cases |
| **Dependency Injection** | Dagger-Hilt (`SingletonComponent`, `ViewModelScoped`) |
| **Database & Storage** | Room Database (SQLite), Manual Migration scripts (v1 to v5), DataStore Preferences |
| **Payment & QR Code** | ZXing Core (`BitMatrix`, `QRCodeWriter`), NPCI UPI Deep-Link Protocol (`upi://pay`) |
| **Document Engine** | Android `PdfDocument`, Custom Typography (Devanagari / TTF Fonts), Vector Graphics |
| **Data Backup** | Scoped Storage, CSV Exporter, SQLite DB Stream Importer/Exporter |
| **Build & Tooling** | Gradle Kotlin DSL (`build.gradle.kts`), ProGuard/R8, Android SDK 34, JDK 21 |

---

## 🏗️ System Architecture & Data Flow

```
┌────────────────────────────────────────────────────────┐
│             Jetpack Compose UI Layer                    │
│   Dashboard (20-Table Grid), TableOrder, Expense,      │
│   Billing, Customer Ledger, Settings (M3 Glassmorphism)│
└───────────────────────────┬────────────────────────────┘
                            │ User Actions / Events
                            ▼
┌────────────────────────────────────────────────────────┐
│             Presentation Layer (MVVM)                   │
│   StateFlow UI State, ViewModels, Hilt Injected        │
└───────────────────────────┬────────────────────────────┘
                            │ Executes Business Logic
                            ▼
┌────────────────────────────────────────────────────────┐
│                 Domain Layer                           │
│   • Use Cases (ReportUseCases, Balance Calculators)    │
│   • RoutePredictionEngine (Weighted Markov Decay)       │
│   • Repository Interfaces                              │
└───────────────────────────┬────────────────────────────┘
                            │ Read / Write Data
                            ▼
┌───────────────────────────┴────────────────────────────┐
│                  Data Layer                            │
│   • Repositories (Sale, Customer, Payment, Product)    │
│   • Room Database (SQLite with Migrations v1 -> v5)    │
│   • Jetpack DataStore Preferences                      │
│   • PdfGenerator (Android Canvas & Devanagari Fonts)   │
│   • QrCodeGenerator (ZXing UPI Deep-Link Encoder)      │
└────────────────────────────────────────────────────────┘
```

---

## 🌟 Key Engineering Highlights

### 1. Dynamic UPI QR Code Engine
* **Problem:** Hotel customers need to pay exact amounts at tables or delivery without manual UPI ID entry errors.
* **Solution:** Formatted NPCI UPI deep links:  
  `upi://pay?pa=<UPI_ID>&pn=<HOTEL_NAME>&am=<AMOUNT>&cu=INR&tn=<NOTE>`
* **Implementation:** Encoded with ZXing into a `BitMatrix`, mapped to an ARGB_8888 bitmap, and rendered directly onto the PDF Canvas.
* **Compatibility:** Scannable with Google Pay, PhonePe, Paytm, BHIM, and bank UPI apps.

### 2. Multi-Schema Room Database Migrations
* Managed 5 sequential database versions safely without data loss:
  * `MIGRATION_1_2`: Route transitions & first customer tracking.
  * `MIGRATION_2_3`: Multi-product support, dynamic product rates per customer.
  * `MIGRATION_3_4`: Hotel dining table orders table.
  * `MIGRATION_4_5`: Hotel daily expenses tracking table.

### 3. Route Prediction Engine
* Implemented a Markov transition matrix $T(A \rightarrow B)$ tracking historical customer supply sequences.
* Applied weekly exponential decay ($e^{-\lambda \Delta t}$) giving greater weight to recent customer supply sequences while gracefully fading old patterns.

### 4. Custom Bilingual PDF Generation
* Built from scratch using native Android `PdfDocument` and `Canvas` (avoiding heavy third-party dependencies).
* Embedded true-type Marathi Devanagari fonts (`YatraOne-Regular.ttf`, `Mukta-ExtraBold.ttf`) for local business identity.
* Supports multi-page pagination, zebra-striping rows, currency in words, grand total highlight cards, and merchant signature blocks.

---

## 🎯 Technical Interview Questions & Answers

### Q1: "Why did you choose Jetpack Compose over traditional XML views?"
> **Answer:** *"Jetpack Compose provided a reactive, declarative UI model where state drives the UI. For instance, dining table occupancy, real-time total order calculations, and bottom sheet pop-ups seamlessly react to Room Flow updates with minimal boilerplate and zero view-binding synchronization bugs."*

### Q2: "How does the UPI QR Code generation work on the PDF?"
> **Answer:** *"I constructed the standard NPCI UPI URI scheme with merchant VPA, URL-encoded hotel name, transaction note, and dynamic bill total. Using ZXing's QRCodeWriter, I generated an error-corrected BitMatrix, converted it to an Android Bitmap, and drew it directly into the PDF Canvas via `drawBitmap()` inside a custom-designed payment card container."*

### Q3: "How did you handle database migrations across app versions?"
> **Answer:** *"I created discrete `Migration` objects (`Migration(3, 4)`, `Migration(4, 5)`) utilizing SQL DDL statements for `CREATE TABLE`, `ALTER TABLE`, and foreign key indexes, and registered them with `Room.databaseBuilder().addMigrations(...)` in Dagger-Hilt. This ensured existing customer ledgers and historical sales were never erased when introducing new features."*

### Q4: "How does the app work offline?"
> **Answer:** *"The application is 100% offline-first. SQLite/Room handles all data persistence locally. PDF generation is rendered purely on-device via native Android graphics, and UPI QR codes are deep links that do not require an active server connection to generate."*
