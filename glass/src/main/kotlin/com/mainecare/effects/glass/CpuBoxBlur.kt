package com.mainecare.effects.glass

import android.graphics.Bitmap

/**
 * Offscreen sRGB box-Gaussian used only to test Chromium alignment.
 *
 * Three separable box passes approximate a Gaussian. A box of radius r has variance
 * r(r+1)/3; three boxes give variance r(r+1), so r = round(sqrt(sigma^2 + 0.25) - 0.5)
 * matches the requested sigma. This is intentionally a CPU path so its kernel can be
 * compared with the Skia BlurEffect without sharing the same implementation.
 */
internal object CpuBoxBlur {
    fun blur(source: Bitmap, sigma: Float): Bitmap {
        if (sigma <= 0f) return source.copy(Bitmap.Config.ARGB_8888, false)
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val radius = maxOf(1, (kotlin.math.sqrt(sigma * sigma + 0.25f) - 0.5f).toInt())
        var buffer = pixels
        repeat(3) {
            buffer = boxPass(buffer, width, height, radius, horizontal = true)
            buffer = boxPass(buffer, width, height, radius, horizontal = false)
        }

        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        output.setPixels(buffer, 0, width, 0, 0, width, height)
        return output
    }

    private fun boxPass(
        source: IntArray,
        width: Int,
        height: Int,
        radius: Int,
        horizontal: Boolean,
    ): IntArray {
        val output = IntArray(source.size)
        val window = radius * 2 + 1
        val length = if (horizontal) width else height
        val lines = if (horizontal) height else width

        for (line in 0 until lines) {
            var sumR = 0
            var sumG = 0
            var sumB = 0
            fun index(i: Int) = if (horizontal) line * width + i else i * width + line
            fun clamp(i: Int) = i.coerceIn(0, length - 1)
            for (i in -radius..radius) {
                val pixel = source[index(clamp(i))]
                sumR += (pixel ushr 16) and 0xFF
                sumG += (pixel ushr 8) and 0xFF
                sumB += pixel and 0xFF
            }
            for (i in 0 until length) {
                output[index(i)] = (0xFF shl 24) or
                    ((sumR / window) shl 16) or
                    ((sumG / window) shl 8) or
                    (sumB / window)
                val remove = source[index(clamp(i - radius))]
                val add = source[index(clamp(i + radius + 1))]
                sumR += ((add ushr 16) and 0xFF) - ((remove ushr 16) and 0xFF)
                sumG += ((add ushr 8) and 0xFF) - ((remove ushr 8) and 0xFF)
                sumB += (add and 0xFF) - (remove and 0xFF)
            }
        }
        return output
    }
}
