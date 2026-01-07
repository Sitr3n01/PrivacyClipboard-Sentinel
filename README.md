# Privacy Clipboard Sentinel 

**Privacy Clipboard Sentinel** is an Android privacy audit tool designed to detect and log unauthorized access to the system clipboard. In an era where data privacy is a growing concern, many applications silently read copied information—such as passwords, private messages, or sensitive links—without explicit user consent. 

This project provides transparency by monitoring system logs in real-time to alert users whenever their clipboard data is accessed or when an access attempt is blocked by the system.

---

## ✨ Features

- **Real-time Monitoring**: Instant detection of clipboard read events or access denials.
- **Smart Notifications**: Intelligent grouping of alerts to prevent notification fatigue during rapid system events.
- **Privacy Dashboard**: A "Top Apps" visual chart that highlights which applications are accessing your data most frequently.
- **Persistent History**: Local storage using Room Database to maintain a secure audit trail of all detected events.
- **ADB Integration**: Leverages `READ_LOGS` permissions to provide deep auditing capabilities without requiring root access.

---

## 🛠 How It Works

Modern Android versions restrict background apps from reading the clipboard. However, many processes still attempt to "peek" at this data. This app bridges the visibility gap by:

1. Running a **Foreground Service** that monitors low-level system logs (`logcat`).
2. Filtering for specific clipboard-related events (`ClipboardService`, `RestrictionPolicy`).
3. Using **Reactive Coroutines (SharedFlow)** to dispatch events from the log stream to the UI and Database.
4. Identifying the specific package names responsible for each access attempt.

---

## 🚀 Installation & Setup

Since this application requires access to protected system logs, you must grant the necessary permission via **ADB (Android Debug Bridge)**.

### Step 1: Clone the repository

```bash
git clone https:https://github.com/Sitr3n01/PrivacyClipboard-Sentinel.git
```

### Step 2: Build and Install

Open the project in Android Studio and build/install the app on your device.

### Step 3: Grant Permission via ADB

Connect your phone to your computer and execute:

```bash
adb shell pm grant com.example.privacyclipboard android.permission.READ_LOGS
```

### Step 4: Activate the Sentinel

Open the app and toggle the "Sentinel" switch to start monitoring.

---

## 💻 Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Database**: Room Persistence Library
- **Concurrency**: Kotlin Coroutines & Flow (SharedFlow)
- **Monitoring**: Low-level `Runtime.exec()` logcat analysis

---

## 📋 Setup Commands for GitHub

If you're setting up this repository for the first time, use these commands:

1. **Initialize the repository:**
   ```bash
   git init
   ```

2. **Add all files:**
   ```bash
   git add .
   ```

3. **Create initial commit:**
   ```bash
   git commit -m "Initial commit: Privacy Clipboard Sentinel funcional"
   ```

4. **Set main branch:**
   ```bash
   git branch -M main
   ```

5. **Connect to remote repository:**
   ```bash
   git remote add origin https://github.com/Sitr3n01/PrivacyClipboard-Sentinel.git
   ```

6. **Push to GitHub:**
   ```bash
   git push -u origin main
   ```

---

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## 👤 Author

Developed by @Sitr3n01(https://github.com/Sitr3n01)

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Feel free to check the [issues page](https://github.com/SEU_NICK_AQUI/NOME_DO_REPO/issues).

---

## ⭐ Show your support

Give a ⭐️ if this project helped you understand clipboard privacy on Android!
