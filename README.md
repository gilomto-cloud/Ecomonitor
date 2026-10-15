# Ecomonitor (Android) ⚡

A lightweight Android application to monitor your **EcoFlow River 2 / 2 Max / 2 Pro** power station directly from your smartphone. 

Track real-time telemetry including live power metrics, battery state of charge, solar energy production, and estimated AC usage costs—complete with historical charts and background alert notifications.

---

## 🚀 Getting Started

### Initial Configuration

1. **Account Login:** Enter your official EcoFlow account **email address** and **password**.
2. **Device Serial:** Input your unit's serial number (must start with **`R6...`**).
3. **Region & Model:** Select the region associated with your EcoFlow account and pick your power station model.
4. **Permissions:** Grant notification permissions when prompted to enable background alerts.

> 💡 **Tip:** To ensure continuous background telemetry and alert functionality, navigate to your phone's settings (`Settings > Apps > River 2 Monitor > Battery`) and set battery usage to **Unrestricted**.

---

## 📊 How It Works & Troubleshooting

### Energy Calculations
- **Energy Totals:** Tracked in real time via continuous calculation ($\text{Power} \times \text{Time}$) while the app's background service remains active.

### Live Data Stream Fix
EcoFlow's cloud infrastructure streams live MQTT telemetry only for devices marked as **active** (which is triggered when connected via the official EcoFlow app).

If no data displays in River 2 Monitor:
1. Open the official **EcoFlow app** once.
2. Return to **River 2 Monitor**—data streams typically start within seconds and remain active for approximately **15 minutes** after closing the official app.
3. Check **Raw Tab > Log** inside the app for active connection diagnostics (tap **Copy log** to copy details for bug reports).

---

## ⚖️ Disclaimer & Legal Disclosure

### 1. Unofficial Application Notice
This software is an **unofficial, third-party client** developed independently. It is **not** created, endorsed, certified, maintained, or supported by **EcoFlow Inc.** or any of its subsidiaries or affiliates.

### 2. Trademark Disclaimer
All product names, logos, brands, and registered trademarks—including **EcoFlow**, **River 2**, **River 2 Max**, and **River 2 Pro**—are the property of their respective owners. Use of these names does not imply any affiliation with or endorsement by the trademark holders.

### 3. API & Cloud Service Usage
- This app connects to EcoFlow's cloud servers via MQTT protocols. 
- Because this relies on private/undocumented cloud routes, **EcoFlow may modify, restrict, or terminate API endpoints at any time**, which may cause this application to temporarily or permanently cease functioning without prior notice.
- This application stores user account credentials locally on your device solely for authentication with EcoFlow's cloud services.

### 4. Warranty & Liability
THIS SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES, OR OTHER LIABILITY ARISING FROM, OUT OF, OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
