# ⚡ RIDE ACCEPTOR — Install Guide

## What this app does
When a booking comes on Uber / Rapido / Ola / Loadshare:
- A BIG GREEN "ACCEPT RIDE" button pops up on your screen
- Works even if you are using another app
- Vibrates your phone loudly
- Shows 12-second countdown timer
- Tapping opens the ride app instantly

---

## HOW TO BUILD & INSTALL (Free, no laptop needed)

### Option 1: Use GitHub + Buildozer (Recommended, Free)

1. Go to https://github.com and create a free account
2. Create a new repository called "RideAcceptor"
3. Upload all these files keeping the same folder structure
4. Go to Actions tab → New Workflow → Android CI
5. The APK will be built automatically and downloadable

### Option 2: Use Android Studio on PC/laptop

1. Download Android Studio free from https://developer.android.com/studio
2. Open this folder as a project
3. Click Build → Build APK
4. Copy the APK to your phone and install

### Option 3: Use an online Android builder

1. Go to https://appetize.io or https://www.jdoodle.com
2. Upload the project ZIP
3. Download the compiled APK

---

## After you have the APK on your phone:

1. Go to Settings → Security → Enable "Install from Unknown Sources"
2. Open the APK file and tap Install
3. Open "Ride Acceptor" app
4. Follow the 3 steps inside the app:
   - Step 1: Grant Notification Access
   - Step 2: Grant Display Over Other Apps
   - Step 3: Tap ACTIVATE

That's it! The app runs in the background and shows the overlay whenever a booking comes.

---

## Permissions explained

| Permission | Why needed |
|-----------|-----------|
| Notification Listener | To read Uber/Rapido/Ola notifications |
| Display Over Other Apps | To show the ACCEPT button on top of screen |
| Vibrate | To buzz your phone when booking arrives |
| Boot completed | To restart automatically after phone reboot |

---

## Supported apps
- Uber Driver (com.ubercab.driver)
- Rapido Captain (com.rapido.captain)
- Ola Driver (com.olacabs.driver)  
- Loadshare Captain (com.loadshare.captain / in.loadshare.driver)
