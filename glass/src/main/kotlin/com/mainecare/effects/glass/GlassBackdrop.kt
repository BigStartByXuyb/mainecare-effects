package com.mainecare.effects.glass

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
) {
    var boundsInRoot by mutableStateOf(Rect.Zero)

    private val blurCache = HashMap<Int, ImageBitmap>()

    /** Returns a downscaled, three-pass box blur approximating the CSS Gaussian. */
    suspend fun blurred(radiusPx: Float): ImageBitmap {
        val key = radiusPx.roundToInt().coerceAtLeast(0)
        synchronized(blurCache) { blurCache[key] }?.let { return it }
        return withContext(Dispatchers.Default) {
            synchronized(blurCache) { blurCache[key] }?.let { return@withContext it }
            val source = image.asAndroidBitmap()
            val down = downscale(source, BLUR_MAX_DIMENSION)
            val factor = down.width / source.width.toFloat()
            val radius = (key * factor).roundToInt().coerceAtLeast(0)
            val result = boxBlur(down, radius, passes = 3).asImageBitmap()
            synchronized(blurCache) { blurCache[key] = result }
            result
        }
    }

    fun clearBlurCache() {
        synchronized(blurCache) { blurCache.clear() }
    }

    private companion object {
        const val BLUR_MAX_DIMENSION = 360
    }
}

@Composable
fun rememberGlassBackdrop(
    image: ImageBitmap,
    contentScale: ContentScale = ContentScale.Crop,
): GlassBackdropState = remember(image, contentScale) {
    GlassBackdropState(image, contentScale)
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
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberGlassBackdrop(image, contentScale)
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

private fun downscale(source: Bitmap, maxDimension: Int): Bitmap {
    val largest = max(source.width, source.height)
    if (largest <= maxDimension) return source
    val scale = maxDimension.toFloat() / largest
    return Bitmap.createScaledBitmap(
        source,
        max(1, (source.width * scale).roundToInt()),
        max(1, (source.height * scale).roundToInt()),
        true,
    )
}

/** Three box passes, each separable, approximating a Gaussian with clamped edges. */
private fun boxBlur(source: Bitmap, radius: Int, passes: Int): Bitmap {
    if (radius <= 0) return source
    val width = source.width
    val height = source.height
    var pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val scratch = IntArray(width * height)
    repeat(passes) {
        blurHorizontal(pixels, scratch, width, height, radius)
        blurVertical(scratch, pixels, width, height, radius)
    }
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

private fun blurHorizontal(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
    val divisor = radius * 2 + 1
    for (y in 0 until height) {
        val row = y * width
        var a = 0
        var r = 0
        var g = 0
        var b = 0
        for (i in -radius..radius) {
            val c = src[row + i.coerceIn(0, width - 1)]
            a += (c ushr 24) and 0xFF
            r += (c ushr 16) and 0xFF
            g += (c ushr 8) and 0xFF
            b += c and 0xFF
        }
        for (x in 0 until width) {
            dst[row + x] = ((a / divisor) shl 24) or
                ((r / divisor) shl 16) or
                ((g / divisor) shl 8) or
                (b / divisor)
            val outgoing = src[row + (x - radius).coerceIn(0, width - 1)]
            val incoming = src[row + (x + radius + 1).coerceIn(0, width - 1)]
            a += ((incoming ushr 24) and 0xFF) - ((outgoing ushr 24) and 0xFF)
            r += ((incoming ushr 16) and 0xFF) - ((outgoing ushr 16) and 0xFF)
            g += ((incoming ushr 8) and 0xFF) - ((outgoing ushr 8) and 0xFF)
            b += (incoming and 0xFF) - (outgoing and 0xFF)
        }
    }
}

private fun blurVertical(src: IntArray, dst: IntArray, width: Int, height: Int, radius: Int) {
    val divisor = radius * 2 + 1
    for (x in 0 until width) {
        var a = 0
        var r = 0
        var g = 0
        var b = 0
        for (i in -radius..radius) {
            val c = src[i.coerceIn(0, height - 1) * width + x]
            a += (c ushr 24) and 0xFF
            r += (c ushr 16) and 0xFF
            g += (c ushr 8) and 0xFF
            b += c and 0xFF
        }
        for (y in 0 until height) {
            dst[y * width + x] = ((a / divisor) shl 24) or
                ((r / divisor) shl 16) or
                ((g / divisor) shl 8) or
                (b / divisor)
            val outgoing = src[(y - radius).coerceIn(0, height - 1) * width + x]
            val incoming = src[(y + radius + 1).coerceIn(0, height - 1) * width + x]
            a += ((incoming ushr 24) and 0xFF) - ((outgoing ushr 24) and 0xFF)
            r += ((incoming ushr 16) and 0xFF) - ((outgoing ushr 16) and 0xFF)
            g += ((incoming ushr 8) and 0xFF) - ((outgoing ushr 8) and 0xFF)
            b += (incoming and 0xFF) - (outgoing and 0xFF)
        }
    }
}
