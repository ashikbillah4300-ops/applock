# GlassLock

Glassmorphism Android App Lock & Hidden Vault built with Kotlin, Jetpack Compose (Material 3), Room, DataStore, Hilt, BiometricPrompt, CameraX, and Shizuku.

## Features

- **App Lock:** Lock installed apps using a 4–6 digit PIN, Pattern, or Biometric (`BiometricPrompt`) with configurable relock timing (`Immediate`, `1 min`, `On screen off`).
- **Intruder Selfie & Security Shields:** Front-camera intruder photo after 3 wrong attempts, optional fake crash screen, notification content hiding, and Device Admin uninstall protection.
- **Hidden Vault (Shizuku):** Hide/unhide installed apps via Shizuku (`pm disable-user --user 0` / `pm enable`), encrypted Room storage (Android Keystore AES-256-GCM), temporary launch with auto re-hide, and safety guardrails against hiding critical system packages.
- **Stealth & Disguise:** Calculator launcher disguise (`<activity-alias>`), `excludeFromRecents`, discreet foreground notification, 5-tap shield access, and `*#1234#` secret dialer code (`SECRET_CODE`).

## Automatic APK Build on GitHub Push

This repository includes a GitHub Actions workflow that automatically runs unit/Robolectric tests and builds an installable APK every time you push to GitHub:

1. Push your code to GitHub:
   ```bash
   git init
   git add .
   git commit -m "Initial commit: GlassLock"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
2. Open your repository on GitHub and go to the **Actions** tab.
3. Click the latest **Build GlassLock APK** workflow run and download **`GlassLock-APKs`** from the **Artifacts** section (`GlassLock-debug.apk`).

### Automatic GitHub Releases (Optional)

To attach `.apk` files directly under the GitHub **Releases** page, push a version tag:

```bash
git tag v1.0.0
git push origin v1.0.0
```

### Signed Release APKs (Optional)

To also generate `GlassLock-release.apk` in CI, add these **Repository Secrets** in **Settings → Secrets and variables → Actions**:

- `RELEASE_KEYSTORE_BASE64`: Base64-encoded `.jks` keystore file (`upload` key alias)
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key password
