# JetOverlay SDK

[![](https://jitpack.io/v/YazanAesmael/JetOverlay.svg)](https://jitpack.io/#YazanAesmael/JetOverlay)

A lightweight, **Jetpack Compose-first** SDK for managing floating Android overlays (System Alert Windows).

JetOverlay handles the complexity of `WindowManager`, `Service` lifecycle, and `Touch Events`, allowing you to render **native Composable content** floating over other apps with just a few lines of code.

## Demo

https://github.com/user-attachments/assets/038e0efe-9b18-47e1-9052-c830d65a6f26

## Features

* **Compose First:** Render standard Composable functions as overlays.
* **Built-in Dragging:** Physics-based dragging support out of the box.
* **Architecture Aware:** Each overlay has its own `Lifecycle`, `ViewModelStore`, and `SavedStateRegistry`.
* **Resilient:** Survives process death and restarts automatically via Foreground Service.
* **Customizable:** Full control over notification channels, icons, and text.
* **Safe:** Handles `ConcurrentModificationException` and "Zombie View" scenarios automatically.

## Installation

### Step 1. Add the JitPack repository

In your `settings.gradle.kts` file:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "[https://jitpack.io](https://jitpack.io)")
    }
}

```

### Step 2. Add the dependency

In your module's `build.gradle.kts` (usually `app/build.gradle.kts`):

```kotlin
dependencies {
    implementation("com.github.YazanAesmael:JetOverlay:1.0.0")
}

```

### Option B: Local Integration

If you are integrating this locally (cloned repo), add the `:jetoverlay` module to your `settings.gradle.kts`:

```kotlin
include(":jetoverlay")

```

Then add the dependency:

```kotlin
dependencies {
    implementation(project(":jetoverlay"))
}

```

## Quick Start

### 1. Initialize the SDK

In your `Application` class or `MainActivity.onCreate`, initialize the SDK factory. This tells the SDK **what** to render when an overlay is requested.

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        OverlaySdk.initialize(
            notificationConfig = OverlayNotificationConfig(
                title = "My App Overlay",
                message = "Tap to configure",
                iconResId = R.drawable.ic_my_icon
            )
        ) { modifier, id, payload ->
            // This is your Composable content!
            // 'modifier' handles the dragging logic provided by the SDK.
            
            Box(
                modifier = modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Red)
            ) {
                Text("Overlay: $id")
            }
        }
    }
}

```

### 2. Request Permission

Overlays require the `SYSTEM_ALERT_WINDOW` permission. The SDK does not handle the UI for requesting this, you must do it in your app:

```kotlin
if (!Settings.canDrawOverlays(context)) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName")
    )
    startActivity(intent)
}

```

### 3. Show an Overlay

Once permission is granted, you can show an overlay from anywhere (Activity, ViewModel, BroadcastReceiver).

```kotlin
OverlaySdk.show(
    context = context,
    config = OverlayConfig(
        id = "score_card_1",
        initialX = 100,
        initialY = 200
    ),
    payload = "Match ID: 12345" // Optional data passed to your factory
)

```

### 4. Hide an Overlay

```kotlin
OverlaySdk.hide("score_card_1")

```

## Advanced Usage

### Using ViewModels

Since each overlay has its own `ViewModelStore`, you can use Hilt or standard ViewModels inside your overlay content securely.

```kotlin
OverlaySdk.initialize { modifier, id, payload ->
    // This ViewModel is scoped to THIS specific overlay window
    val viewModel: MyOverlayViewModel = viewModel() 
    
    MyOverlayScreen(
        viewModel = viewModel,
        modifier = modifier
    )
}

```

### ProGuard / R8

The SDK includes its own `consumer-rules.pro`. You do **not** need to add any manual ProGuard rules to your app. The SDK automatically keeps:

* The Public API (`com.yazan.jetoverlay.api.**`)
* The Service reflection entry points.

## Requirements

* minSdk: 26 (Android 8.0)
* compileSdk: 36
* Kotlin: 2.0+
* Jetpack Compose

---

### **LICENSE** (MIT)

Create a file named `LICENSE` in the root of your project and paste this in:

```text
MIT License

Copyright (c) 2026 Yazan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

```
