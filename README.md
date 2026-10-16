# EcoMonitor (Android)

Monitor your EcoFlow River 2 / 2 Max / 2 Pro from your phone: battery gauge, live power,
solar production and AC usage tracking (with cost), history chart, and alerts.

It signs in with your EcoFlow **app** account and uses EcoFlow's cloud MQTT (the same
route as the phone app). This is unofficial and EcoFlow may change it.

## Build
1. Install Android Studio (Koala or newer) with JDK 17.
2. File > Open > select this folder, let Gradle sync (it downloads dependencies).
3. Plug in your phone (USB debugging on) and press Run, or Build > Build APK(s).
   The APK is in app/build/outputs/apk/debug/.

## First run
Enter your EcoFlow email, password, device serial number (starts with R6...), pick the region
that matches your EcoFlow account, and the model. Allow notifications when asked.
Tip: on some phones set battery usage for the app to "Unrestricted" so the live
connection and alerts keep running in the background.

## Notes
- Energy totals are measured by the app (power x time) while its service is running.

## No data showing?
EcoFlow's cloud only streams data for a device it considers "active", which is tied to the
official EcoFlow app being used. Open the official EcoFlow app once, then return here: data
usually starts within seconds and keeps flowing for roughly 15 minutes after you close it.
Also check the Raw tab > Log for connection details (tap "Copy log" to share it).

## Get an APK without installing Android Studio (GitHub Actions)
1. Create a free GitHub account and a new **private** repository.
2. Upload everything in this folder to it (including the hidden `.github` folder).
3. Open the repo > **Actions** > **Build APK** > **Run workflow**.
4. When it finishes (about 5 minutes), open the run and download **River2Monitor-debug-apk**.
5. Unzip it, copy `app-debug.apk` to your phone and install it (allow "install unknown apps").
