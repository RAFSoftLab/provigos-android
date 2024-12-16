# Provigos - Android App

Provigos is an open-source project aimed at creating a **personal life dashboard** that empowers users to manage and analyze their data from various services. This repository contains the **Kotlin Android App**, providing a mobile interface for accessing and visualizing personalized insights on the go.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Screenshots](#screenshots)
- [Getting Started](#getting-started)
- [Development](#development)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

The Provigos Android App allows users to:
- View interactive dashboards and widgets for their data.
- Access personalized insights and trends from various integrated services.
- Manage integrations and settings directly from their mobile device.

---

## Features

- **Interactive Dashboards**: View modular, customizable data insights.
- **Offline Mode**: Access previously fetched data without an internet connection.
- **Secure Authentication**: Supports OAuth2 and token-based login.
- **Push Notifications**: Stay informed with timely updates about your data trends.
- **Material Design**: A clean and modern UI adhering to Android best practices.

---

## Tech Stack

- **Kotlin**: The primary language for app development.
- **Android Jetpack**: Components such as ViewModel, LiveData, and Room for MVVM architecture.
- **RxJava**: For concurrent behavior. 
- **Koin**: For dependency injection.
- **Retrofit**: For API communication with the backend services.
- **Moshi**: For serialization and deserialization. 
- **Coroutines**: For asynchronous programming.
- **Glide**: For image loading and caching.
- **Firebase** (optional): For push notifications and analytics.

---

## Screenshots

*WIP*  

---

## Getting Started

### Prerequisites

- **Android Studio**: Download and install the latest version from [developer.android.com](https://developer.android.com/studio).
- **Minimum SDK**: 26 (Android 8 Oreo).

### Clone the Repository

```bash
git clone https://github.com/your-username/provigos-android-app.git
cd provigos-android-app
```
Open in Android Studio
Open Android Studio.
Click on File -> Open and select the provigos-android-app directory.
Wait for Gradle to sync the project.
Build and Run
Connect an Android device or start an emulator.
Click on the Run button or use the shortcut Shift + F10.

## Development

### Folder Structure

```bash
app/
│
├── src/
│   ├── main/
│   │   ├── java/com/provigos/android
│   │   │   ├── ui/ (Activities, Fragments, Adapters)
│   │   │   ├── data/ (Repositories, Data Sources)
│   │   │   └── utils/ (Helpers, Extensions)
│   │   └── res/ (Layouts, Drawables, Values)
│   └── test/ (Unit Tests)
│
└── build.gradle (Project-level Gradle configurations)
```
### Key Directories
```
ui/: Contains the user interface logic (Activities, Fragments, and Adapters).
data/: Repositories and data sources for handling data operations.
utils/: Helper functions, extensions, and utilities.
res/: Resources such as layouts, drawables, and strings.
```
## Contributing
We welcome contributions to the Provigos Android App! Please read our CONTRIBUTING.md to get started.

### How to Contribute
```
Fork this repository.
Create a new branch (git checkout -b feature-xyz).
Commit your changes (git commit -m "Add feature XYZ").
Push to the branch (git push origin feature-xyz).
Open a Pull Request.
```

## License
Provigos is licensed under the MIT License.
Feel free to use, modify, and distribute this project as long as proper attribution is provided.
