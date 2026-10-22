# EcoMonitor (Android)

<img width="180" height="374.375" alt="4087" src="https://github.com/user-attachments/assets/e9f1c5a3-24dd-461f-956d-d53ec7bce797" />
<img width="180" height="374.375" alt="4086" src="https://github.com/user-attachments/assets/6567d9da-08bf-42ff-9d18-568101ef8c85" />
<img width="180" height="374.375" alt="4085" src="https://github.com/user-attachments/assets/4bb07b44-5fed-4818-9cf6-1d34371e65fe" />
<img width="180" height="374.375" alt="4084" src="https://github.com/user-attachments/assets/4688c99d-8cb6-4f0c-aae6-4c59e6516f3d" />
<img width="180" height="374.375" alt="4082" src="https://github.com/user-attachments/assets/2bf3611d-d156-4891-874d-49b78520afb7" />

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
