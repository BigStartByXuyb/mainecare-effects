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
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.pow
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
            val result = gaussianBlur(down, radius.toFloat() * 0.75f).asImageBitmap()
            synchronized(blurCache) { blurCache[key] = result }
            result
        }
    }

    fun clearBlurCache() {
        synchronized(blurCache) { blurCache.clear() }
    }

    private companion object {
        const val BLUR_MAX_DIMENSION = 300
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
private fun gaussianBlur(source: Bitmap, sigma: Float): Bitmap {
    if (sigma <= 0.01f) return source
    val width = source.width
    val height = source.height
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    // CSS blur is a Gaussian sigma in linear light. Convert first so bright
    // highlights spread like the browser instead of staying too bright.
    val current = FloatArray(width * height * 3)
    for (i in pixels.indices) {
        val c = pixels[i]
        current[i * 3] = srgbToLinear(((c ushr 16) and 0xFF) / 255f)
        current[i * 3 + 1] = srgbToLinear(((c ushr 8) and 0xFF) / 255f)
        current[i * 3 + 2] = srgbToLinear((c and 0xFF) / 255f)
    }

    val radius = ceil(sigma * 3f).toInt().coerceAtLeast(1)
    val kernel = FloatArray(radius * 2 + 1)
    var kernelSum = 0f
    for (i in kernel.indices) {
        val x = (i - radius) / sigma
        kernel[i] = exp(-0.5f * x * x)
        kernelSum += kernel[i]
    }
    for (i in kernel.indices) {
        kernel[i] /= kernelSum
    }

    val scratch = FloatArray(current.size)
    blurHorizontalGaussian(current, scratch, width, height, radius, kernel)
    blurVerticalGaussian(scratch, current, width, height, radius, kernel)

    val out = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val outPixels = IntArray(width * height)
    for (i in outPixels.indices) {
        val r = (linearToSrgb(current[i * 3]) * 255f).roundToInt().coerceIn(0, 255)
        val g = (linearToSrgb(current[i * 3 + 1]) * 255f).roundToInt().coerceIn(0, 255)
        val b = (linearToSrgb(current[i * 3 + 2]) * 255f).roundToInt().coerceIn(0, 255)
        outPixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
    out.setPixels(outPixels, 0, width, 0, 0, width, height)
    return out
}

private fun srgbToLinear(value: Float): Float =
    if (value <= 0.04045f) value / 12.92f else ((value + 0.055f) / 1.055f).pow(2.4f)

private fun linearToSrgb(value: Float): Float =
    if (value <= 0.0031308f) value * 12.92f else 1.055f * value.pow(1f / 2.4f) - 0.055f

private fun blurHorizontalGaussian(
    src: FloatArray,
    dst: FloatArray,
    width: Int,
    height: Int,
    radius: Int,
    kernel: FloatArray,
) {
    for (y in 0 until height) {
        val row = y * width
        for (x in 0 until width) {
            var r = 0f
            var g = 0f
            var b = 0f
            for (k in -radius..radius) {
                val sample = (row + (x + k).coerceIn(0, width - 1)) * 3
                val weight = kernel[k + radius]
                r += src[sample] * weight
                g += src[sample + 1] * weight
                b += src[sample + 2] * weight
            }
            val out = (row + x) * 3
            dst[out] = r
            dst[out + 1] = g
            dst[out + 2] = b
        }
    }
}

private fun blurVerticalGaussian(
    src: FloatArray,
    dst: FloatArray,
    width: Int,
    height: Int,
    radius: Int,
    kernel: FloatArray,
) {
    for (y in 0 until height) {
        for (x in 0 until width) {
            var r = 0f
            var g = 0f
            var b = 0f
            for (k in -radius..radius) {
                val sample = ((y + k).coerceIn(0, height - 1) * width + x) * 3
                val weight = kernel[k + radius]
                r += src[sample] * weight
                g += src[sample + 1] * weight
                b += src[sample + 2] * weight
            }
            val out = (y * width + x) * 3
            dst[out] = r
            dst[out + 1] = g
            dst[out + 2] = b
        }
    }
}
