# SpendCap

A lightweight Android expense tracker focused on simplicity, privacy, and reliable SMS-based transaction import.

---

## Table of Contents

1. [Overview](#overview)  
2. [Features](#features)  
3. [Architecture](#architecture)  
4. [Installation](#installation)  
5. [Usage](#usage)  
6. [Permissions](#permissions)  
7. [Development](#development)  
8. [License](#license)  

---

## Overview

SpendCap is an Android app that automates expense tracking by parsing bank SMS messages. It also offers:

- Manual entry  
- Smart categorization  
- Basic usage analytics  

All data is stored locally—no network access—ensuring privacy and offline reliability.

---

## Features

- **SMS Import**  
  Automatic parsing and import of bank transaction messages.  
- **Manual Entry**  
  Quick-add transactions with custom categories and tags.  
- **Duplicate Detection**  
  Prevents repeated entries based on amount, timestamp, and bank.  
- **Smart Categorization**  
  Auto-assigns categories with support for custom overrides.  
- **Filtering & Search**  
  Date range, category, bank, and amount filters plus keyword search.  
- **Usage Analytics**  
  Optional tracking of screen time, app usage, and unlock count.  

---

## Architecture

- **UI**: Jetpack Compose with Material Design 3 (MVVM)  
- **Navigation**: Jetpack Navigation Component  
- **Data**: Room (SQLite) with Repository & Use Case layers  
- **State Management**: Kotlin StateFlow & Coroutines  
- **Dependency Injection**: Manual DI via ViewModelFactory (expandable to Hilt/Koin)  
- **Testing**: Unit tests for repositories/use cases; migration tests for DB  

---

## Installation

### Prerequisites

- Android Studio Arctic Fox or later  
- Android SDK 21+  
- Kotlin 1.9+

### Steps

```bash
git clone https://github.com/ravimishra07/SpendCap.git
cd SpendCap
# Open in Android Studio and sync Gradle
# Build and run on emulator or device
