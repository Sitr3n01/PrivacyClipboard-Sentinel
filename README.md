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

1. **Clone the repository**:
   ```bash
   git clone [https://github.com/SEU_NICK_AQUI/PrivacyClipboard-Sentinel.git](https://github.com/SEU_NICK_AQUI/PrivacyClipboard-Sentinel.git)
