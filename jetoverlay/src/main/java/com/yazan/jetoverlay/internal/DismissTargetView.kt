package com.yazan.jetoverlay.internal

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yazan.jetoverlay.api.OverlaySdk
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An internal view that renders the "Trash Can" dismissal target.
 * It listens to a flow [_isHovering] to trigger animations (scale up/color change)
 * when the user drags an overlay over it.
 */
internal class DismissTargetView(context: Context) : OverlayViewWrapper(context) {

    private val _isHovering = MutableStateFlow(false)

    init {
        val config = OverlaySdk.dismissConfig

        setContent {
            val isHovering by _isHovering.collectAsStateWithLifecycle()

            // 1. Animation State
            val scale by animateFloatAsState(
                targetValue = if (isHovering) 1.5f else 1f,
                animationSpec = tween(300),
                label = "scale"
            )

            val backgroundColor by animateColorAsState(
                targetValue = if (isHovering) config.activeBackgroundColor else config.backgroundColor,
                animationSpec = tween(300),
                label = "color"
            )

            // 2. The UI
            Box(
                modifier = Modifier
                    .size(config.size)
                    .background(color = backgroundColor, shape = CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = config.icon ?: Icons.Filled.Close,
                    contentDescription = "Dismiss Overlay",
                    tint = config.iconColor,
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
            }
        }
    }

    fun setHovering(hovering: Boolean) {
        _isHovering.value = hovering
    }
}