SpendCap

A lightweight Android expense tracker focused on simplicity, privacy, and reliable SMS-based transaction import.

⸻

Table of Contents
	1.	Overview
	2.	Features
	3.	Architecture
	4.	Installation
	5.	Usage
	6.	Permissions
	7.	Development
	8.	License

⸻

Overview

SamStudio is an Android app that automates expense tracking by parsing bank SMS messages and offers manual entry, smart categorization, and basic usage analytics. All data is stored locally—no network access—ensuring user privacy and offline reliability.

⸻

Features
	•	SMS Import: Automatic parsing and import of bank transaction messages.
	•	Manual Entry: Quick-add transactions with custom categories and tags.
	•	Duplicate Detection: Prevents repeated entries based on amount, timestamp, and bank.
	•	Smart Categorization: Auto-assigns categories with support for custom overrides.
	•	Filtering & Search: Date range, category, bank, and amount filters plus keyword search.
	•	Usage Analytics: Optional tracking of screen time, app usage, and unlock count.

⸻

Architecture
	•	UI: Jetpack Compose with Material Design 3, MVVM pattern
	•	Navigation: Jetpack Navigation Component
	•	Data: Room (SQLite) database, Repository and Use Case layers
	•	State Management: Kotlin StateFlow and Coroutines for asynchronous operations
	•	Dependency Injection: Manual DI via ViewModelFactory (expandable to Hilt or Koin)
	•	Testing: Unit tests for repositories and use cases; migration tests for database

⸻

Installation

Prerequisites
	•	Android Studio Arctic Fox or later
	•	Android SDK 21+
	•	Kotlin 1.9+

Steps

# Clone the repository
git clone https://github.com/your-username/SamStudio.git
cd SamStudio
# Open in Android Studio and sync Gradle
# Build and run on emulator or device


⸻

Usage
	1.	Grant Permissions: On first launch, allow SMS and usage-access permissions.
	2.	Sync SMS: Tap Sync SMS to import existing bank transactions.
	3.	Review & Categorize: Verify imported entries and adjust categories or tags as needed.
	4.	Add Transaction: Tap + to manually add a new expense.
	5.	Analytics: View spending trends and basic usage statistics on the dashboard.

⸻

Permissions
	•	READ_SMS: To import and parse transaction messages.
	•	PACKAGE_USAGE_STATS: To enable optional device usage analytics.

⸻

Development
	•	Code Style: Follow the Kotlin coding conventions.
	•	Testing: Run ./gradlew test for unit tests and ./gradlew connectedAndroidTest for instrumentation tests.
	•	Database Migrations: Verify schema changes with DatabaseMigrationTest.
	•	Logging: Enable DEBUG logs in AppConfig for troubleshooting.

⸻

License

This project is licensed under the MIT License. See LICENSE for details.
