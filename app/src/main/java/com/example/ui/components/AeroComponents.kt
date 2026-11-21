package com.example.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.R
import com.example.ui.browser.BrowserTab
import com.example.ui.browser.WebViewRegistry
import com.example.ui.theme.LocalAeroPalette

@Composable
fun AeroBackdrop(
    modifier: Modifier = Modifier,
    animateOrbs: Boolean = true,
) {
    val palette = LocalAeroPalette.current
    val transition = rememberInfiniteTransition(label = "aero_backdrop")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animateOrbs) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb_drift",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(palette.backdropGradient))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_aero_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (palette.preset.isDark) 0.22f else 0.36f),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.backdropTint)
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.accentGlow.copy(alpha = 0.34f),
                        Color.Transparent,
                    ),
                    center = Offset(w * (0.22f + 0.08f * drift), h * 0.18f),
                    radius = w * 0.55f,
                ),
                center = Offset(w * (0.22f + 0.08f * drift), h * 0.18f),
                radius = w * 0.55f,
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = if (palette.preset.isDark) 0.12f else 0.32f),
                        Color.Transparent,
                    ),
                    center = Offset(w * (0.82f - 0.08f * drift), h * 0.58f),
                    radius = w * 0.48f,
                ),
                center = Offset(w * (0.82f - 0.08f * drift), h * 0.58f),
                radius = w * 0.48f,
            )
        }
    }
}

@Composable
fun AeroGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    elevated: Boolean = false,
    shadowElevation: Dp = 8.dp,
    glossySheen: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalAeroPalette.current
    val fillColor = if (elevated) palette.glassSurfaceElevated else palette.glassSurface

    Box(
        modifier = modifier
            .shadow(
                elevation = shadowElevation,
                shape = shape,
                ambientColor = palette.glassBorderShadow,
                spotColor = palette.glassBorderShadow,
            )
            .clip(shape)
            .background(fillColor)
            .drawWithContent {
                drawContent()
                if (glossySheen) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(palette.glassSheenTop, palette.glassSheenBottom),
                            startY = 0f,
                            endY = size.height * 0.48f,
                        ),
                        size = Size(size.width, size.height * 0.48f),
                        cornerRadius = CornerRadius(24f, 24f),
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(palette.glassBorderHighlight, palette.glassBorderShadow),
                ),
                shape = shape,
            ),
        content = content,
    )
}

@Composable
fun AeroIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = Color.Transparent,
    size: Dp = 48.dp,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .alpha(if (enabled) 1f else 0.38f),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun AeroPillChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    selected: Boolean = false,
) {
    val palette = LocalAeroPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        palette.glassSurfaceElevated
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(palette.glassBorderHighlight, palette.glassBorderShadow)
                ),
                shape = RoundedCornerShape(50),
            )
            .clickable(
                role = Role.Button,
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
        )
    }
}

@Composable
fun BrowserWebViewContainer(
    tab: BrowserTab,
    registry: WebViewRegistry,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            FrameLayout(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
        },
        update = { container ->
            val webView: WebView = registry.getOrCreateWebView(tab)
            val currentChild = if (container.childCount > 0) container.getChildAt(0) else null
            if (currentChild !== webView) {
                (webView.parent as? ViewGroup)?.removeView(webView)
                container.removeAllViews()
                container.addView(
                    webView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
    )
}
