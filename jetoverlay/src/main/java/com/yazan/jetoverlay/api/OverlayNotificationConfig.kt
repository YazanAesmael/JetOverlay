// jetoverlay/src/main/java/com/yazan/jetoverlay/api/OverlayNotificationConfig.kt

package com.yazan.jetoverlay.api

import androidx.annotation.DrawableRes

/**
 * Configuration for the Foreground Service Notification.
 *
 * Android requires a persistent notification for any service running in the foreground.
 * This class allows you to customize the appearance of that notification to match your app's branding.
 *
 * @property title The title text of the notification (e.g., "Live Scores"). Defaults to "Overlay Active".
 * @property message The body text of the notification (e.g., "Overlays are running"). Defaults to "Tap to manage active overlays".
 * @property iconResId The resource ID for the small notification icon. If null, the SDK will attempt to use the app's default launcher icon.
 * @property channelId The unique ID for the notification channel. Defaults to "overlay_service_channel".
 * @property channelName The user-visible name of the channel in System Settings. Defaults to "Overlay Service".
 */
data class OverlayNotificationConfig(
    val title: String = "Overlay Active",
    val message: String = "Tap to manage active overlays",
    @DrawableRes val iconResId: Int? = null,
    val channelId: String = "overlay_service_channel",
    val channelName: String = "Overlay Service"
)