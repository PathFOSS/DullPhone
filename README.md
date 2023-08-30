# A merciless phone blocker for Android

Savage Blocker is the self-control block timer that every digital native desperately needs to stop the mindless checking and scrolling on the phone.

The application is completely Free and Open Source (FOSS) and falls under the GPLv3 license.

## How to use the app?

1. Pick a day (today or tomorrow)
2. Pick a valid time (00:00 - 23.59)
3. Swipe and confirm block

## What happens during the block?

You are facing a screen overlay that you cannot bypass in any way. You will be able to answer calls through the notification bar and call emergency calls from the lock screen.

If you restart the app, the overlay will return.

If you find a loophole to bypass the screen, please create an issue in the repository.

## Is this malware?

Absolutely not. You can verify this yourself by examining the few hundred lines of Java code posted in the repository.

The app blocks your device rather harshly, but only when you request it to.

There are zero online features in the app and therefore no malware.

Please always verify the validity of the application you download.

## Known issues and potential fixes

NB! If you cannot find your issue here please make sure to first delete storage and cache for the app and check that the "Display over other apps" and "Permit usage access" permissions are checked. If it doesn't help please create an issue in the repository.

### Home button escape method

Problem: You are able to "escape" the overlay for about 5.5 seconds using the home button if your default home launcher is not com.android.launcher3 (Android prevents app restarts for 5 seconds when home launches is visible)

Solution: Press only the home button once and wait for Android to show the app overlay again. Now the app recognizes your home screen and blocks it by default.

### Recent apps button escape method

Problem: You are able to "escape" the overlay for some time (and swipe the app away)  using the recent apps button if your default task manager is not com.android.launcher3

Solution: Press only the recent apps button once and wait for Android to show the app overlay again. Now the app recognizes your task manager and blocks it

### Spam clicking every button imaginable

Problem: You may be able to escape by spamming and swiping the system navigation buttons and notifications and freeze the system temporarily (should be quite difficult)

Solution: Enable Developer Options and disable all animations 

### Timer is up but overlay is still showing

Problem: Timer runs out but you still cannot close the app. This is quite unlikely.

Solution: Restart the phone. The overlay service recognizes that the timer is less than current time and will not block the phone anymore.

## How to end the block early?

Short answer: You probably can't and shouldn't try.

Long answer: You need to have Developer Options and USB debugging enabled while plugged into the computer. Then run this terminal command: adb shell am force-stop com.pathfoss.savageblocker.

## How to have zero chance of exiting the block early?

1. During the block press the home button once and wait for the overlay to return (only once ever)
2. During the block press the recent apps button once and wait for the overlay to return (only once ever)
3. Disable all animations in Settings > Developer Options
4. Disable USB debugging in Settings > Developer Options

## Specifications

Name: Savage Blocker
Supported Android version: 8.0 and above
Application size: ~ 5 MB
Maximum blocking time: ~ 48h
Officially supported devices: Phones only (tablets and wearables have never been tested)
Online features: None
Analytics: None
Developer: PathFOSS
License: GPLv3

