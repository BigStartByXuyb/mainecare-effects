package com.mainecare.effects.glass

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdropState?> { null }

/**
 * Holds the captured backdrop bitmap and a small per-radius blur cache.
 *
 * Capture is snapshot based: the caller supplies the bitmap that sits behind the glass.
 * That is exact for the photo-background sample and for any static artwork, and it keeps
 * the shader deterministic enough for pixel gates. Live, per-frame backdrop capture is a
 * separate follow-up (the same shader can consume a refreshed bitmap).
 */
@Stable
class GlassBackdropState(
    val image: ImageBitmap,
    val contentScale: ContentScale = ContentScale.Crop,
    val sceneImage: ImageBitmap? = null,
    private val graphicsLayer: GraphicsLayer,
    private val blurSigmaScale: Float = 1.4f,
) {
    private var boundsState by mutableStateOf(Rect.Zero)

    var boundsInRoot: Rect
        get() = boundsState
        set(value) {
            if (boundsState != value) {
                boundsState = value
                clearBlurCache()
            }
        }

    private val blurCache = HashMap<Int, ImageBitmap>()

    /** Returns a Skia-blurred backdrop at the requested CSS radius. */
    suspend fun blurred(radiusPx: Float): ImageBitmap {
        val key = radiusPx.roundToInt().coerceAtLeast(0)
        synchronized(blurCache) { blurCache[key] }?.let { return it }
        val gpu = gpuBlur(key.toFloat())
        synchronized(blurCache) { blurCache[key] = gpu }
        return gpu
    }

    /**
     * GPU path through the same Skia blur Chromium uses. The backdrop is first rendered at
     * screen geometry (ContentScale applied), then blurred at up to 2160px; the shader
     * mapping uses this screen-space bitmap directly.
     */
    private suspend fun gpuBlur(radiusPx: Float): ImageBitmap = withContext(Dispatchers.Main) {
        val layer = graphicsLayer
        val bounds = awaitBounds()
        if (bounds.width <= 0f || bounds.height <= 0f) {
            error("GlassBackdrop has no layout bounds")
        }
        val scale = min(
            1f,
            GLASS_BLUR_MAX_DIMENSION / max(bounds.width, bounds.height),
        )
        val width = max(1, (bounds.width * scale).roundToInt())
        val height = max(1, (bounds.height * scale).roundToInt())

        val fit = contentScale.computeScaleFactor(
            Size(image.width.toFloat(), image.height.toFloat()),
            Size(bounds.width, bounds.height),
        )
        val dstWidth = image.width * fit.scaleX * scale
        val dstHeight = image.height * fit.scaleY * scale
        val dstX = ((bounds.width - image.width * fit.scaleX) / 2f) * scale
        val dstY = ((bounds.height - image.height * fit.scaleY) / 2f) * scale

        layer.record(
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            size = IntSize(width, height),
        ) {
            drawImage(
                image = image,
                dstOffset = IntOffset(dstX.roundToInt(), dstY.roundToInt()),
                dstSize = IntSize(dstWidth.roundToInt(), dstHeight.roundToInt()),
            )
        }
        val sigma = radiusPx * scale * blurSigmaScale
        layer.renderEffect = BlurEffect(sigma, sigma, TileMode.Clamp)
        val output = layer.toImageBitmap()
        layer.renderEffect = null
        output
    }

    private suspend fun awaitBounds(): Rect {
        val current = boundsInRoot
        if (current.width > 0f && current.height > 0f) return current
        return withContext(Dispatchers.Main) {
            snapshotFlow { boundsInRoot }.first { it.width > 0f && it.height > 0f }
        }
    }

    fun clearBlurCache() {
        synchronized(blurCache) { blurCache.clear() }
    }

    private companion object {
        const val GLASS_BLUR_MAX_DIMENSION = 2160f
    }
}

@Composable
fun rememberGlassBackdrop(
    image: ImageBitmap,
    contentScale: ContentScale = ContentScale.Crop,
    sceneImage: ImageBitmap? = null,
    blurSigmaScale: Float = 1.4f,
): GlassBackdropState {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(image, contentScale, sceneImage, graphicsLayer, blurSigmaScale) {
        GlassBackdropState(image, contentScale, sceneImage, graphicsLayer, blurSigmaScale)
    }
}

/**
 * Draws [image] as the page backdrop and exposes the captured region to descendant
 * [GlassSurface]s. The Box is clipped so a Crop-scaled image never paints outside.
 */
@Composable
fun GlassBackdrop(
    image: ImageBitmap,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    sceneImage: ImageBitmap? = null,
    blurSigmaScale: Float = 1.4f,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberGlassBackdrop(image, contentScale, sceneImage, blurSigmaScale)
    CompositionLocalProvider(LocalGlassBackdrop provides state) {
        Box(
            modifier = modifier
                .clipToBounds()
                .onGloballyPositioned { state.boundsInRoot = it.boundsInRoot() }
                .drawBehind {
                    val factor = contentScale.computeScaleFactor(
                        Size(image.width.toFloat(), image.height.toFloat()),
                        size,
                    )
                    val dstWidth = image.width * factor.scaleX
                    val dstHeight = image.height * factor.scaleY
                    drawImage(
                        image = image,
                        dstOffset = IntOffset(
                            ((size.width - dstWidth) / 2f).roundToInt(),
                            ((size.height - dstHeight) / 2f).roundToInt(),
                        ),
                        dstSize = IntSize(dstWidth.roundToInt(), dstHeight.roundToInt()),
                        filterQuality = FilterQuality.High,
                    )
                },
        ) {
            content()
        }
    }
}
