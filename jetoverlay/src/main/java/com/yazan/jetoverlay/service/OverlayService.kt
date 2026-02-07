// jetoverlay/src/main/java/com/yazan/jetoverlay/service/OverlayService.kt

package com.yazan.jetoverlay.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.yazan.jetoverlay.api.OverlaySdk
import com.yazan.jetoverlay.internal.OverlayViewWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The core Service responsible for managing the lifecycle of floating overlay windows.
 *
 * This service runs in the foreground to ensure the process is not killed by the system
 * while overlays are active. It observes the [OverlaySdk.activeOverlays] state flow and
 * synchronizes the [WindowManager] views accordingly.
 */
class OverlayService : Service() {

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    // A SupervisorJob ensures that a failure in one child coroutine does not cancel the entire scope.
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Tracks currently attached views by their Overlay ID.
    private val activeViews = mutableMapOf<String, OverlayViewWrapper>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        observeOverlays()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If the system kills the service, recreate it automatically.
        return START_STICKY
    }

    /**
     * Observes the SDK's state flow and triggers view synchronization whenever the map changes.
     */
    private fun observeOverlays() {
        OverlaySdk.activeOverlays.onEach { overlayMap ->
            synchronizeViews(overlayMap)
        }.launchIn(serviceScope)
    }

    /**
     * Compares the current requested state with the actual active views and applies changes.
     *
     * 1. Removes views that are no longer in the requested map.
     * 2. Adds new views that are in the requested map but not yet on screen.
     * 3. (Implicitly) Updates existing views via recomposition (handled by Compose internals).
     */
    private fun synchronizeViews(requestedOverlays: Map<String, OverlaySdk.ActiveOverlay>) {
        val currentIds = activeViews.keys.toSet()
        val requestedIds = requestedOverlays.keys

        // Cleanup removed overlays
        (currentIds - requestedIds).forEach { removeOverlay(it) }

        // Add new overlays
        (requestedIds - currentIds).forEach { id ->
            requestedOverlays[id]?.let { overlayData ->
                addOverlay(id, overlayData)
            }
        }
    }

    private fun addOverlay(id: String, data: OverlaySdk.ActiveOverlay) {
        if (activeViews.containsKey(id)) return

        val viewWrapper = OverlayViewWrapper(this)

        // Configure the window parameters for a floating system overlay
        val params = WindowManager.LayoutParams(
            data.config.width.takeIf { it.value > 0 }?.value?.toInt() ?: WindowManager.LayoutParams.WRAP_CONTENT,
            data.config.height.takeIf { it.value > 0 }?.value?.toInt() ?: WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = data.config.initialX
            y = data.config.initialY
        }

        viewWrapper.setContent {
            // physics-based dragging logic
            val dragModifier = Modifier.pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    params.x += dragAmount.x.toInt()
                    params.y += dragAmount.y.toInt()

                    // Update the window position immediately
                    if (viewWrapper.isAttachedToWindow) {
                        windowManager.updateViewLayout(viewWrapper, params)
                    }
                }
            }

            OverlaySdk.getContentFactory().Content(
                modifier = dragModifier,
                id = id,
                payload = data.payload
            )
        }

        try {
            windowManager.addView(viewWrapper, params)
            // Important: Lifecycle must be resumed for Compose to run
            viewWrapper.resume()
            activeViews[id] = viewWrapper
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay(id: String) {
        activeViews.remove(id)?.let { view ->
            try {
                // Important: Cleanup ViewModelStore and Lifecycle
                view.destroy()
                windowManager.removeView(view)
            } catch (e: Exception) {
                // View might already be detached or window token is dead; safe to ignore.
                e.printStackTrace()
            }
        }

        // If no overlays are left, we can stop the service to save resources.
        if (activeViews.isEmpty()) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        // Ensure all views are cleanly removed to prevent "Window Leaked" errors
        activeViews.keys.toList().forEach { id ->
            removeOverlay(id)
        }
    }

    /**
     * Starts the persistent notification required for Foreground Services.
     * Handles Android 14 (UPSIDE_DOWN_CAKE) specific service type requirements.
     */
    private fun startForegroundNotification() {
        val config = OverlaySdk.notificationConfig
        val manager = getSystemService(NotificationManager::class.java)

        // Create the Notification Channel (required for Android O+)
        val channel = NotificationChannel(
            config.channelId,
            config.channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        // Use the configured icon or fallback to a system default
        val smallIcon = config.iconResId ?: android.R.drawable.ic_dialog_info

        // Build the notification
        val builder = Notification.Builder(this, config.channelId)

        val notification = builder
            .setContentTitle(config.title)
            .setContentText(config.message)
            .setSmallIcon(smallIcon)
            .build()

        try {
            // Android 14+ requires specifying the Foreground Service Type
            if (Build.VERSION.SDK_INT >= 34) { // UPSIDE_DOWN_CAKE
                startForeground(
                    101,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(101, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}