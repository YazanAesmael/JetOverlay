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
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.yazan.jetoverlay.api.OverlaySdk
import com.yazan.jetoverlay.internal.DismissTargetView
import com.yazan.jetoverlay.internal.OverlayViewWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlin.math.hypot

/**
 * The core Service responsible for managing the lifecycle of floating overlay windows.
 */
class OverlayService : Service() {

    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val activeViews = mutableMapOf<String, OverlayViewWrapper>()

    // The single "Trash Can" view shared by all overlays
    private var dismissView: DismissTargetView? = null

    // Track whether the "Trash Can" is currently currently in a "Hovered" state
    private var isDismissHovering = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        setupDismissView()
        observeOverlays()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun setupDismissView() {
        if (!OverlaySdk.dismissConfig.enabled) return

        dismissView = DismissTargetView(this).apply {
            visibility = View.GONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            y = 100 // Margin from bottom
        }

        try {
            windowManager.addView(dismissView, params)
            dismissView?.resume()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun observeOverlays() {
        OverlaySdk.activeOverlays.onEach { overlayMap ->
            synchronizeViews(overlayMap)
        }.launchIn(serviceScope)
    }

    private fun synchronizeViews(requestedOverlays: Map<String, OverlaySdk.ActiveOverlay>) {
        val currentIds = activeViews.keys.toSet()
        val requestedIds = requestedOverlays.keys

        (currentIds - requestedIds).forEach { removeOverlay(it) }

        (requestedIds - currentIds).forEach { id ->
            requestedOverlays[id]?.let { overlayData ->
                addOverlay(id, overlayData)
            }
        }
    }

    private fun addOverlay(id: String, data: OverlaySdk.ActiveOverlay) {
        if (activeViews.containsKey(id)) return

        val viewWrapper = OverlayViewWrapper(this)

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
            // State for the "Swallowing" animation
            var scale by remember { mutableFloatStateOf(1f) }
            var alpha by remember { mutableFloatStateOf(1f) }

            val dragModifier = Modifier.pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        dismissView?.visibility = View.VISIBLE
                    },
                    onDragEnd = {
                        dismissView?.visibility = View.GONE

                        if (isDismissHovering) {
                            OverlaySdk.hide(id)
                        } else {
                            // Reset animation if not dismissed
                            scale = 1f
                            alpha = 1f
                        }

                        isDismissHovering = false
                        dismissView?.setHovering(false)
                    },
                    onDragCancel = {
                        dismissView?.visibility = View.GONE
                        scale = 1f
                        alpha = 1f
                        isDismissHovering = false
                        dismissView?.setHovering(false)
                    }
                ) { change, dragAmount ->
                    change.consume()
                    params.x += dragAmount.x.toInt()
                    params.y += dragAmount.y.toInt()

                    if (viewWrapper.isAttachedToWindow) {
                        windowManager.updateViewLayout(viewWrapper, params)
                    }

                    // Check collision and update the "Swallow" effects
                    val effects = checkDismissCollision(params.x, params.y, viewWrapper.width, viewWrapper.height)
                    scale = effects.scale
                    alpha = effects.alpha
                }
            }

            // Wrap content with graphicsLayer to handle the visual swallowing
            val contentFactory = OverlaySdk.getContentFactory()

            androidx.compose.foundation.layout.Box(
                modifier = dragModifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                contentFactory.Content(
                    modifier = Modifier, // Drag is handled by the parent Box
                    id = id,
                    payload = data.payload
                )
            }
        }

        try {
            windowManager.addView(viewWrapper, params)
            viewWrapper.resume()
            activeViews[id] = viewWrapper
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Helper class to return visual effects values based on collision distance
     */
    private data class DismissEffects(val scale: Float, val alpha: Float)

    /**
     * Checks distance to trash can and calculates "Swallow" effects.
     */
    private fun checkDismissCollision(x: Int, y: Int, w: Int, h: Int): DismissEffects {
        val target = dismissView ?: return DismissEffects(1f, 1f)

        val metrics = resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels

        val overlayCenterX = x + (w / 2)
        val overlayCenterY = y + (h / 2)

        // Target is approx at bottom center
        val targetCenterX = screenWidth / 2
        val targetCenterY = screenHeight - 150

        val distance = hypot(
            (overlayCenterX - targetCenterX).toDouble(),
            (overlayCenterY - targetCenterY).toDouble()
        )

        // Threshold in pixels where the effect starts
        val threshold = 350.0

        if (distance < threshold) {
            // Calculate how deep into the zone we are (0.0 = edge, 1.0 = center)
            val progress = ((threshold - distance) / threshold).coerceIn(0.0, 1.0).toFloat()

            // Trigger the Trash Can animation
            if (!isDismissHovering) {
                isDismissHovering = true
                target.setHovering(true)
            }

            // Calculate "Swallow" effects
            // Scale: 1.0 -> 0.4
            val scale = 1f - (progress * 0.6f)
            // Alpha: 1.0 -> 0.3
            val alpha = 1f - (progress * 0.7f)

            return DismissEffects(scale, alpha)
        } else {
            // Reset if out of range
            if (isDismissHovering) {
                isDismissHovering = false
                target.setHovering(false)
            }
            return DismissEffects(1f, 1f)
        }
    }

    private fun removeOverlay(id: String) {
        activeViews.remove(id)?.let { view ->
            try {
                view.destroy()
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (activeViews.isEmpty()) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        activeViews.keys.toList().forEach { removeOverlay(it) }

        dismissView?.let {
            try {
                it.destroy()
                windowManager.removeView(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startForegroundNotification() {
        val config = OverlaySdk.notificationConfig
        val manager = getSystemService(NotificationManager::class.java)

        val channel = NotificationChannel(
            config.channelId,
            config.channelName,
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        val smallIcon = config.iconResId ?: android.R.drawable.ic_dialog_info
        val builder =
            Notification.Builder(this, config.channelId)

        val notification = builder
            .setContentTitle(config.title)
            .setContentText(config.message)
            .setSmallIcon(smallIcon)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(101, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(101, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}