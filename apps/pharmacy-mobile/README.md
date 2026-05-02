# Pharmacy Mobile Uploader (Flutter)

Minimal pharmacist app for:
- login
- entering prescription ID
- taking camera photo
- uploading stamped prescription to pharmacy-service

## 1) Install Flutter on Windows

1. Download Flutter SDK (stable): https://docs.flutter.dev/get-started/install/windows  
2. Extract to `C:\src\flutter` (or any path without spaces).  
3. Add `C:\src\flutter\bin` to your `PATH`.
4. Install Android Studio + Android SDK + Android SDK Platform-Tools.
5. Run:

```powershell
flutter doctor
```

Fix all red errors.

## 2) Create platform folders (first time only)

From this folder:

```powershell
cd apps/pharmacy-mobile
flutter create .
```

This generates `android/`, `ios/`, etc. while keeping `lib/main.dart` and `pubspec.yaml`.

## 3) Install dependencies

```powershell
flutter pub get
```

## 4) Run on real Android phone

1. Enable **Developer options** + **USB debugging** on phone.
2. Connect phone by USB.
3. Verify device:

```powershell
adb devices
flutter devices
```

4. Run:

```powershell
flutter run
```

## 5) API URL note

In login screen, set API URL to your PC LAN IP (not localhost), for example:

`http://192.168.1.100:8082/api`

`localhost` on phone points to the phone itself.
