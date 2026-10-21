# Hotel Rudra Amruttulya — Android Billing & POS System

<div align="center">

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-purple?style=for-the-badge&logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=android)
![Room Database](https://img.shields.io/badge/Room-SQLite%20(v5)-orange?style=for-the-badge&logo=sqlite)
![Dagger Hilt](https://img.shields.io/badge/Dagger%20Hilt-2.50-green?style=for-the-badge)
![Offline First](https://img.shields.io/badge/Architecture-Offline--First-blue?style=for-the-badge)

<br/>

**A modern, offline-first Hotel Point-of-Sale (POS), Table Order Management, B2B Tea Supply Billing, and Expense Tracking Android application.**

*Designed & Programmed by **Pavan Pawar***

</div>

---

## 📖 Overview

**Hotel Rudra Amruttulya POS** is a custom enterprise Android application designed for restaurants, tea franchises (*Amruttulya*), and hospitality businesses. It bridges on-premise hotel table dining management with commercial B2B daily tea supply billing, featuring **instant UPI Payment QR Codes**, **custom bilingual PDF invoice generation (Marathi & English)**, and **automated daily expense auditing**.

---

## ✨ Key Features

### 🍽️ 1. Live Dining Table Management
* **Interactive 20-Table Grid**: Live visual status for every dining table (`Free` vs `Occupied`).
* **Instant Order Modal**: Tap any table to pop up the quick-order menu, increment/decrement quantities with one tap, and calculate running totals in real time.
* **Table Clear & Settlement**: Instant table clearance upon payment receipt generation.

### 💳 2. Dynamic UPI Payment QR Code Receipts
* **NPCI-Compliant Deep Links**: Automatically generates `upi://pay?pa=<UPI_ID>&pn=<HOTEL_NAME>&am=<AMOUNT>&cu=INR&tn=<NOTE>` QR codes.
* **Direct on Bills**: Printed on both Table Order Bills and Customer Monthly Statements.
* **Zero-Error Payments**: Customers simply scan using **Google Pay, PhonePe, Paytm, BHIM**, or any banking app with the exact bill total pre-filled.

### 📊 3. Daily Hotel Expense Tracking
* **Comprehensive Expense Ledger**: Record daily expenses (Milk 🥛, Gas/LPG 🔥, Salary 👷, Vegetables 🥦, Grocery 🛒, Electricity ⚡, Tea Powder 🍵).
* **Summary KPI Cards**: Live cards tracking *Today's Expense*, *This Month's Expense*, and *Filtered Total*.
* **Date Range Filters**: Quick filters for *Today*, *This Month*, or custom date pickers.
* **PDF Expense Sheet**: Export formatted, paginated expense sheets for tax auditing and accounting.

### 🍵 4. Commercial Tea Supply & Customer Ledger
* **Bulk Order Entry**: Rapid daily supply entry for partner shops, offices, and regular clients.
* **Custom Product Rates**: Set unique pricing per customer per product.
* **Credit & Payment Ledger**: Track outstanding debit balances, payment logs (Cash, UPI, Bank), and running credit statements.

### 🤖 5. Markov Route Prediction Engine
* **Intelligent Sequence Optimization**: Mathematical algorithm analyzing transition matrices to predict the next delivery stop on delivery runs.
* **Exponential Time Decay**: Applies a $10\%/\text{week}$ decay weight ($e^{-\lambda \Delta t}$) prioritizing recent supply habits over obsolete patterns.

### 📄 6. Native Bilingual PDF Generator
* **Devanagari & English Typography**: True-Type Marathi fonts (`YatraOne`, `Mukta`) embedded for authentic regional hotel identity (*हॉटेल रुद्रा अमृततुल्य*).
* **Comprehensive Documents**:
  * Thermal-style Table Dining Receipts with UPI QR codes.
  * Monthly B2B Customer Statements with amount in words.
  * Master Bills and Daily Revenue Sheets.

### 🔒 7. Data Safety & App Security
* **100% Offline-First**: Operates seamlessly without internet; all data resides securely in SQLite.
* **Database Backup & Restore**: One-tap SQLite database export/import and CSV customer exports.
* **Passcode App Lock**: Automated trial and security lock protection.

---

## 🛠️ Tech Stack & Architecture

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

| Component | Technology |
| :--- | :--- |
| **Language** | Kotlin 1.9 |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | Clean Architecture + MVVM |
| **Dependency Injection** | Dagger-Hilt |
| **Database** | Room ORM (SQLite) with 5 Schema Migrations |
| **Asynchronous Engine**| Coroutines & StateFlow |
| **QR Code Engine** | ZXing Core (3.5.3) |
| **PDF Rendering** | Android Native `PdfDocument` & Canvas |
| **Configuration** | Jetpack DataStore Preferences |

---

## 📂 Project Structure

```
app/src/main/java/com/teasupplier/app/
├── MainActivity.kt                # Application entry & app lock validation
├── MainViewModel.kt               # Security & license management
├── TeaSupplierApp.kt              # Hilt Application initialization
├── core/
│   ├── navigation/AppNavigation.kt# Central Compose navigation graph & routes
│   └── ui/theme/                  # Material 3 colors, glassmorphism, & typography
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt         # Room database with Migrations 1 through 5
│   │   ├── dao/                   # Customer, Sale, Payment, Expense, TableOrder DAOs
│   │   ├── di/DatabaseModule.kt   # Hilt database & DAO injection module
│   │   └── entity/                # Room entities (Expense, TableOrder, Sale, etc.)
│   └── preferences/AppPreferences.kt # DataStore settings & hotel profile
├── domain/
│   ├── engine/RoutePredictionEngine.kt # Markov-chain route prediction
│   ├── repository/                # Domain repository interfaces
│   └── usecase/ReportUseCases.kt  # Business reporting calculations
├── feature/
│   ├── billing/                   # Bill generator & PDF export screen
│   ├── bulkorder/                 # Fast bulk tea entry
│   ├── customer/                  # Customer profiles & directory
│   ├── dashboard/                 # 20-Table grid & hotel revenue hub
│   ├── expense/                   # Daily hotel expense tracker & PDF sheet
│   ├── ledger/                    # Customer payment ledger & balance statements
│   ├── lock/                      # Security passcode screen
│   ├── pdfviewer/                 # In-app PDF viewer with WhatsApp sharing
│   ├── product/                   # Product catalog & custom pricing
│   ├── reports/                   # Daily & monthly sales analytics
│   ├── settings/                  # Business profile & database backup
│   └── tableorder/                # Dining table ordering & instant billing
└── utils/
    ├── BackupHelper.kt            # Database & CSV export/import engine
    ├── CurrencyUtils.kt           # Indian currency formatter (₹)
    ├── DateUtils.kt               # Epoch date utilities & range converters
    ├── NumberToWordsConverter.kt  # Numeric amount to English words
    ├── PdfGenerator.kt            # Native Canvas PDF document builder
    └── QrCodeGenerator.kt         # NPCI UPI QR code bitmap generator
```

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Hedgehog (2023.1.1) or newer
* **JDK**: OpenJDK 17 or 21
* **Android SDK**: Min SDK 26, Target SDK 34

### Build & Run
1. Clone the repository:
   ```bash
   git clone https://github.com/hydra-root/Rudra-Hotel.git
   cd Rudra-Hotel
   ```
2. Build debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install to connected device or emulator:
   ```bash
   ./gradlew installDebug
   ```

---

## 👨‍💻 Developer & Author

**Pavan Pawar**  
*Software Programmer & Android Developer*  
Project: Hotel Rudra Billing & Management System  
GitHub: [@hydra-root](https://github.com/hydra-root)

---

## 📄 License

This software is developed by **Pavan Pawar**. All rights reserved.
