package com.yazan.jetoverlay.api

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Configuration for the "Drag-to-Dismiss" functionality.
 *
 * @property enabled Whether dragging the overlay to the bottom of the screen should show a dismissal target.
 * @property icon Optional custom icon for the trash can. If null, a default trash icon is used.
 * @property iconColor The color of the trash icon.
 * @property backgroundColor The background circle color of the dismissal target.
 * @property activeBackgroundColor The background circle color when the overlay is hovering over the target (ready to delete).
 * @property size The size of the dismissal target circle.
 */
data class OverlayDismissConfig(
    val enabled: Boolean = true,
    val icon: ImageVector? = null,
    val iconColor: Color = Color.White,
    val backgroundColor: Color = Color(0x80000000), // Semi-transparent black
    val activeBackgroundColor: Color = Color(0xFFE53935), // Red
    val size: Dp = 60.dp
)