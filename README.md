# JetOverlay 🚀

[![](https://jitpack.io/v/YazanAesmael/JetOverlay.svg)](https://jitpack.io/#YazanAesmael/JetOverlay)
![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)
![API](https://img.shields.io/badge/API-26%2B-brightgreen.svg)

**JetOverlay** is a modern, lightweight Android library built entirely with **Jetpack Compose**. It allows you to easily display floating overlay windows (like Chat Heads or Bubbles) over other apps, managing the complex `WindowManager` lifecycle and permissions for you.

## ✨ Features

* **100% Jetpack Compose:** Write your overlay UI using standard Composable functions.
* **Physics-Based Dragging:** Smooth, natural movement logic built-in.
* **Lifecycle Aware:** Automatically handles `ViewModelStore` and `LifecycleOwner` for your overlays, preventing memory leaks.
* **Drag-to-Dismiss:** Built-in "Trash Can" target to easily close overlays by dragging them to the bottom.
* **Customizable:** Control notification channels, dismiss targets, and overlay configuration with a simple API.
* **Permission Handling:** Helper functions to check and request `SYSTEM_ALERT_WINDOW` permission.
* **Android 14 Ready:** Compliant with the latest Foreground Service policies.

## 📱 Demo

https://github.com/user-attachments/assets/cf25213e-fc61-42c8-bfa7-6d7de557eb32

## 📦 Installation

Add the repository to your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("[https://jitpack.io](https://jitpack.io)") }
    }
}

```

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.YazanAesmael:JetOverlay:1.1.0")
}

```

---

## 🚀 Quick Start

### 1. Initialize the SDK

Initialize the SDK in your `Application` class. This is where you define your global configuration and the factory that renders your content.

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        OverlaySdk.initialize(
            notificationConfig = OverlayNotificationConfig(
                title = "My App Overlay",
                message = "Tap to manage active overlays"
            ),
            // Define how to render content based on an ID
            factory = { id, payload ->
                MyOverlayContent(id, payload)
            }
        )
    }
}

// Your Composable Content
@Composable
fun MyOverlayContent(id: String, payload: Any?) {
    Card(
        shape = CircleShape,
        modifier = Modifier.size(60.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("☘️")
        }
    }
}

```

### 2. Check Permissions & Show Overlay

In your Activity or ViewModel, check for permission and trigger the overlay.

```kotlin
val context = LocalContext.current

Button(onClick = {
    if (OverlaySdk.checkPermission(context)) {
        // Permission granted, show the overlay
        OverlaySdk.show(
            id = "chat_head_1",
            config = OverlayConfig(initialX = 100, initialY = 200)
        )
    } else {
        // Request permission
        OverlaySdk.requestPermission(context)
    }
}) {
    Text("Show Overlay")
}

```

---

## 🎨 Detailed Customization

### 1. Drag-to-Dismiss (Trash Can)

By default, dragging an overlay to the bottom of the screen shows a "Trash Can" icon. You can fully customize this behavior in the `initialize` method.

```kotlin
OverlaySdk.initialize(
    // ... other configs
    dismissConfig = OverlayDismissConfig(
        enabled = true,                     // Set false to disable completely
        icon = Icons.Filled.Delete,         // Use your own icon
        iconColor = Color.White,            // Icon tint
        backgroundColor = Color(0x80000000),// Normal background (semi-transparent black)
        activeBackgroundColor = Color.Red,  // Background when hovering (ready to delete)
        size = 80.dp                        // Size of the target circle
    )
)

```

### 2. Notifications

Android requires a persistent notification for foreground services. You can customize the text and channel details.

```kotlin
OverlaySdk.initialize(
    notificationConfig = OverlayNotificationConfig(
        channelId = "my_overlay_channel",
        channelName = "Active Floating Widgets",
        title = "Widget Active",
        message = "Drag to move, drag to bottom to close",
        iconResId = R.drawable.ic_my_logo // Optional custom small icon
    ),
    // ...
)

```

### 3. Per-Overlay Configuration

When calling `OverlaySdk.show()`, you can pass specific configurations for that window instance.

```kotlin
OverlaySdk.show(
    id = "video_player",
    payload = VideoData(url = "..."), // Pass data to your Composable
    config = OverlayConfig(
        initialX = 50,
        initialY = 50,
        width = 200.dp,   // Optional: Fix the window size
        height = 120.dp
    )
)

```

---

## 🧑🏻‍💻 Contributing

Contributions are welcome!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 🤝 Support & Community

Join the conversation! We want to hear from you.

* **💬 [Discussions](https://github.com/YazanAesmael/JetOverlay/discussions):** Ask questions, share ideas, or show off what you've built.
* **🐛 [Issues](https://github.com/YazanAesmael/JetOverlay/issues):** Report bugs or request specific features.
* **⭐ Star this repo:** It helps the project grow!

##📰 Medium Article:
* **🗞️ [Article](https://medium.com/@yazanaesmael/stop-fighting-windowmanager-build-android-floating-windows-with-jetpack-compose-c9ebfaf4afc2)


---

## 📄 License

Distributed under the Apache 2.0 License. See `LICENSE` for more information.

```
Copyright 2024 Yazan Aesmael

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---
