# A merciless phone blocker for Android

Dull Phone is the self-control block timer that every digital native desperately needs to stop the mindless checking and scrolling on the phone.

The application is completely Free and Open Source (FOSS) and falls under the GPLv3 license.

## How to use the app?

1. Select whitelisted apps in the top right corner
2. Pick the number of days, hours, and minutes to block your device
3. Tap the button, review the block duration, and confirm block

## What happens during the block?

You are facing a screen overlay showing the time and progress.

You will have three functionalities:

1. Open the default phone app
2. Enable tap to unlock (select number of taps from settings default is 5000)
3. View whitelist and open allowed applications

If you restart the phone, the overlay will return.

If you press the home button, the overlay will return.

If you open recent apps, the overlay will return.

If you press the back button, the overlay will return.

If you open an non-whitelist application, the overlay will return.

If you find a loophole to bypass the screen, please create an issue in the repository.

## Is this malware?

Absolutely not. You can verify this yourself by examining the couple hundred lines of Java code posted in the repository.

The app blocks your device rather harshly, but only when you request it to.

There are zero online features and therefore no tangible way to undermine your privacy and security.

Please always verify the validity of the application you download.

## Known issues and potential fixes

NB! If you cannot find your issue here please make sure to first delete storage and cache for the app and check that the "Display over other apps" and "Permit usage access" permissions are checked. If it doesn't help please create an issue in the repository.

#### Spam clicking every button imaginable

Problem: You may be able to temporarily escape by spamming and swiping the system navigation buttons and notifications and freeze the system temporarily (should be quite difficult)

Solution: Enable Developer Options and disable all animations

#### Time is up but overlay is still showing

Problem: Timer runs out but you still cannot close the app. This is quite unlikely.

Solution: Restart the phone. The overlay service recognizes that the timer is less than current time and will not block the phone anymore.

#### App return when opening a whitelisted app

Problem: The overlay returns to foreground when requesting to open a whitelisted app.

Solution: Most times requesting the whitelist app to open again should fix this.

## How to end the block early?

User-friendly method: You can enable tap to unlock mode and tap the screen 5000 times which takes on average 15 minutes of quick tapping.

Advanced method: You need to have Developer Options and USB debugging enabled while plugged into the computer. Then run this terminal command: adb shell am force-stop com.pathfoss.dullphone.

## How to have zero chance of exiting the block early?

1. Disable all animations in Settings > Developer Options
2. Disable USB debugging in Settings > Developer Options
3. Add a minimal amount of apps to whitelist (limits RAM)

## Why is the screen time showing the wrong time?

The screen time notification is an experimental feature and provides widely inaccurate results. Please use with caution.

In the future there will be attempts to improve the accuracy of screen time.

## Specifications

Name: Dull Phone

Newest version: 2.0.1

Supported Android version: 8.0 and above

Application size: ~ 5 MB

Maximum blocking time: ~ 31 days

Officially supported devices: Phones only (tablets and wearables have never been tested)

Online features: None

Analytics: None

Developer: PathFOSS

License: GPLv3