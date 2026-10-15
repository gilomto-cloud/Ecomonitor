# River 2 Monitor (Android)

Monitor your EcoFlow River 2 / 2 Max / 2 Pro from your phone: battery gauge, live power,
solar production and AC usage tracking (with cost), history chart, and alerts.

It signs in with your EcoFlow **app** account and uses EcoFlow's cloud MQTT (the same
route as the phone app). This is unofficial and EcoFlow may change it.

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
