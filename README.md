# ⏰ BuzzBuddy – Smart Alarm Manager (Android + API)

BuzzBuddy is a feature-rich alarm management application built using Kotlin and XML-based UI.
The app demonstrates persistent alarm scheduling, reboot resilience, system service integration, and structured local data storage.

---

## 🚀 Features

- Add multiple alarms
- Swipe-to-delete with undo (snackbar sits above the + button)
- Toggle alarm on/off
- Duplicate alarm time validation on create and edit
- Alarm title display
- Gradual volume ramp-up for smooth wake-up
- Snooze duration control (1–60 minutes)
- Vibration toggle
- Sunrise vs Beep alarm sounds (system alarm vs notification tone)
- Dismiss alarm directly from notification panel
- Auto-dismiss ringing after 2 minutes (alarm stays enabled for the next day)
- Reboot, app-update, timezone, and cold-start rescheduling
- Optional login and alarm sync with the FastAPI backend

---

## 🛠 Tech Stack

- Kotlin
- XML Layouts
- Room Database
- AlarmManager
- BroadcastReceiver
- BootReceiver
- SharedPreferences
- RecyclerView
- ItemTouchHelper
- Notifications API

---

## 🏗 Architecture Overview

### 📦 Data Persistence
- Alarms are stored using **Room Database**
- Duplicate alarms are prevented via database-level validation
- User preferences stored using **SharedPreferences**

### ⏰ Alarm Scheduling
- Uses **AlarmManager** for scheduling alarms
- **AlarmReceiver** handles alarm trigger events
- **BootReceiver** listens for device reboot and reschedules active alarms
- `RECEIVE_BOOT_COMPLETED` permission implemented

### 🖱 User Interaction
- RecyclerView for alarm list
- Swipe-to-delete using ItemTouchHelper
- Undo delete logic using Snackbar
- Toggle switch updates persistent state

---

## ▶️ How to Run

1. Clone the repository
2. Open this repository root in Android Studio
3. Sync Gradle
4. Run on physical device or emulator

---

## 📌 Concepts Demonstrated

- Android system service integration
- Persistent background scheduling
- Reboot-safe alarm handling
- Structured local database design
- State management using SharedPreferences
- UI interaction with RecyclerView & swipe gestures

---

## 📸 Screenshots

### 🏠 Home Screen
![Home Screen](Screenshots/Homescreen.jpg)

### ⏰ Active Alarm Screen
![Active Alarm](Screenshots/ActiveAlarm.jpg)

### ✏️ Set Alarm Title
![Set Title](Screenshots/Set_title.jpg)

### 🔄 Update Alarm
![Update Alarm](Screenshots/Update.jpg)

### 🗑 Swipe to Delete with Undo
![Undo Delete](Screenshots/Undo_delete.jpg)

---

## Backend

Login and alarm sync. The API is FastAPI + SQLite. The Android app talks to it over JWT.

### Run the API

```bash
cd backend
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
uvicorn app.main:app --host 0.0.0.0 --port 8080
```

Docs: http://127.0.0.1:8080/docs

Set `api.base.url` in `local.properties` to `http://10.0.2.2:8080/` for the emulator, or `http://YOUR_LAN_IP:8080/` for a phone on the same Wi‑Fi. Then register and log in.

### Endpoints

| Method | Path | Auth |
|--------|------|------|
| POST | `/api/register` | No |
| POST | `/api/login` | No |
| POST | `/api/auth/refresh` | Refresh token |
| POST | `/api/auth/logout` | Yes |
| POST | `/api/auth/password-reset/request` | No |
| POST | `/api/auth/password-reset/confirm` | No |
| GET | `/api/account/me` | Yes |
| PUT | `/api/account/password` | Yes |
| DELETE | `/api/account` | Yes |
| GET, POST | `/api/alarms` | Yes |
| PUT, DELETE | `/api/alarms/{id}` | Yes |
