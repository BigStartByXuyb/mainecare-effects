package com.mainecare.effects.glass

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    material: GlassMaterial = MaineGlassMaterials.Card,
    params: LiquidGlassParams = material.liquid,
    backdrop: GlassBackdropState? = LocalGlassBackdrop.current,
    interactive: Boolean = true,
    debugOutput: Float = 0f,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val shape = remember(params.cornerRadius) { RoundedCornerShape(params.cornerRadius) }
    var surfaceBounds by remember { mutableStateOf(Rect.Zero) }
    var surfaceSize by remember { mutableStateOf(Size.Zero) }
    var pointer by remember { mutableStateOf(Offset(0.72f, 0.08f)) }

    val mapBitmap = remember(params.mode) {
        BitmapFactory.decodeResource(context.resources, mapResource(params.mode))
    }
    val blurred by produceState<ImageBitmap?>(
        initialValue = null,
        backdrop,
        material.blurRadiusDp,
        density.density,
    ) {
        value = backdrop?.blurred(material.blurRadiusDp * density.density)
    }
    val shader = remember { RuntimeShader(LiquidGlassShader.AGSL) }
    val paint = remember { AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG) }
    val mapShader = remember(mapBitmap) {
        BitmapShader(mapBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    }
    val backdropBitmap = blurred?.asAndroidBitmap()
    val backdropShader = remember(backdropBitmap) {
        backdropBitmap?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
    }

    val interaction = if (interactive && backdrop != null) {
        Modifier.pointerInput(surfaceSize) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                pointer = down.position.normalized(surfaceSize)
                var pressed = true
                while (pressed) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    pointer = change.position.normalized(surfaceSize)
                    pressed = change.pressed
                }
                pointer = Offset(0.72f, 0.08f)
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier.onGloballyPositioned {
            surfaceBounds = it.boundsInRoot()
            surfaceSize = it.size.toSize()
        },
    ) {
        if (material.shadowAlpha > 0f && material.shadowBlurSigmaDp > 0f) {
            Box(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        translationY = material.shadowOffsetYDp.dp.toPx()
                        val sigma = material.shadowBlurSigmaDp.dp.toPx()
                        renderEffect = BlurEffect(sigma, sigma, TileMode.Decal)
                        alpha = material.shadowAlpha
                    }
                    .background(Color.Black, shape),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
                .drawBehind {
                    val canSample = backdrop != null &&
                        backdropBitmap != null &&
                        surfaceBounds.width > 0f &&
                        surfaceBounds.height > 0f &&
                        backdrop.boundsInRoot.width > 0f
                    if (canSample) {
                        bindShader(
                            shader = shader,
                            paint = paint,
                            backdrop = backdrop!!,
                            backdropBitmap = backdropBitmap!!,
                            backdropShader = backdropShader!!,
                            mapBitmap = mapBitmap,
                            mapShader = mapShader,
                            surfaceBounds = surfaceBounds,
                            surfaceSize = surfaceSize,
                            params = params,
                            material = material,
                            pointer = pointer,
                            debugOutput = debugOutput,
                            density = density.density,
                        )
                        drawIntoCanvas { canvas ->
                            canvas.nativeCanvas.drawRect(0f, 0f, size.width, size.height, paint)
                        }
                    } else {
                        drawRect(material.tint)
                    }
                    drawInnerShadow(material, size)
                }
                .drawWithContent {
                    drawContent()
                    drawBorderAndHighlights(material, params.cornerRadius.toPx(), size)
                }
                .then(interaction),
            contentAlignment = contentAlignment,
        ) {
            content()
        }
    }
}

private fun Offset.normalized(size: Size): Offset {
    if (size.width <= 0f || size.height <= 0f) return Offset(0.72f, 0.08f)
    return Offset(
        (x / size.width).coerceIn(0f, 1f),
        (y / size.height).coerceIn(0f, 1f),
    )
}

private fun mapResource(mode: GlassMode): Int = when (mode) {
    GlassMode.Standard -> R.raw.liquid_glass_displacement_standard
    GlassMode.Polar -> R.raw.liquid_glass_displacement_polar
    GlassMode.Prominent -> R.raw.liquid_glass_displacement_prominent
    GlassMode.Shader -> R.raw.liquid_glass_displacement_standard
}

private fun bindShader(
    shader: RuntimeShader,
    paint: AndroidPaint,
    backdrop: GlassBackdropState,
    backdropBitmap: Bitmap,
    backdropShader: BitmapShader,
    mapBitmap: Bitmap,
    mapShader: BitmapShader,
    surfaceBounds: Rect,
    surfaceSize: Size,
    params: LiquidGlassParams,
    material: GlassMaterial,
    pointer: Offset,
    debugOutput: Float,
    density: Float,
) {
    val bounds = backdrop.boundsInRoot
    val factor = backdrop.contentScale.computeScaleFactor(
        Size(backdrop.image.width.toFloat(), backdrop.image.height.toFloat()),
        Size(bounds.width, bounds.height),
    )
    val drawnWidth = backdrop.image.width * factor.scaleX
    val drawnHeight = backdrop.image.height * factor.scaleY
    val drawOffsetX = (bounds.width - drawnWidth) / 2f
    val drawOffsetY = (bounds.height - drawnHeight) / 2f

    val glassX = (surfaceBounds.left - bounds.left - drawOffsetX) / factor.scaleX
    val glassY = (surfaceBounds.top - bounds.top - drawOffsetY) / factor.scaleY
    val blurFactor = backdropBitmap.width / backdrop.image.width.toFloat()

    shader.setFloatUniform("uSize", surfaceSize.width, surfaceSize.height)
    shader.setFloatUniform("uBackdropOrigin", glassX * blurFactor, glassY * blurFactor)
    shader.setFloatUniform("uBackdropScale", blurFactor / factor.scaleX, blurFactor / factor.scaleY)
    shader.setFloatUniform("uMapSize", mapBitmap.width.toFloat())
    shader.setFloatUniform("uDisplacement", params.signedDisplacementScale * density)
    shader.setFloatUniform("uAberration", params.aberrationIntensity)
    shader.setFloatUniform("uSaturation", material.saturation)
    shader.setFloatUniform("uBrightness", material.brightness)
    shader.setFloatUniform(
        "uTint",
        material.tint.red,
        material.tint.green,
        material.tint.blue,
        material.tint.alpha,
    )
    shader.setFloatUniform("uPointer", pointer.x, pointer.y)
    shader.setFloatUniform("uSheen", material.sheenStrength)
    shader.setFloatUniform("uElasticity", params.elasticity)
    shader.setFloatUniform("uProcedural", if (params.mode == GlassMode.Shader) 1f else 0f)
    shader.setFloatUniform("uBeforeLayer", if (material.beforeLayer) 1f else 0f)
    shader.setFloatUniform("uBeforeStrength", material.beforeStrength)
    shader.setFloatUniform("uAfterGlow", if (material.afterGlow) 1f else 0f)
    shader.setFloatUniform("uDebug", debugOutput)
    shader.setFloatUniform("uSheenTopCenter", material.sheenTopCenter.x, material.sheenTopCenter.y)
    shader.setFloatUniform("uSheenTopRadii", material.sheenTopRadii.x, material.sheenTopRadii.y)
    shader.setFloatUniform("uSheenTopFade", material.sheenTopFade)
    setColorUniform(shader, "uSheenTopRight", material.sheenTopRight)
    setColorUniform(shader, "uSheenBottom", material.sheenBottom)
    setColorUniform(shader, "uSheenDiagonal", material.sheenDiagonal)
    setColorUniform(shader, "uSheenDark", material.sheenDark)
    setColorUniform(shader, "uSheenVerticalTop", material.sheenVerticalTop)
    setColorUniform(shader, "uSheenVerticalBottom", material.sheenVerticalBottom)
    shader.setInputBuffer("uMap", mapShader)
    shader.setInputBuffer("uBackdrop", backdropShader)
    paint.shader = shader
}

private fun setColorUniform(shader: RuntimeShader, name: String, color: Color) {
    shader.setFloatUniform(name, color.red, color.green, color.blue, color.alpha)
}

private fun DrawScope.drawInnerShadow(material: GlassMaterial, size: Size) {
    val depth = minOf(
        material.innerShadowDepthDp.dp.toPx(),
        size.height * 0.38f,
    )
    if (depth <= 0f || size.width <= 0f) return
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color.Transparent, material.bottomInnerShadowColor),
            startY = size.height - depth,
            endY = size.height,
        ),
        topLeft = Offset(0f, size.height - depth),
        size = Size(size.width, depth),
    )
}

private fun DrawScope.drawBorderAndHighlights(
    material: GlassMaterial,
    cornerRadiusPx: Float,
    size: Size,
) {
    if (size.width <= 1f || size.height <= 1f) return
    val stroke = 1.dp.toPx()
    val inset = (cornerRadiusPx * 0.55f)
        .coerceAtMost(size.width / 2f - 1f)
        .coerceAtLeast(0f)
    if (inset < size.width / 2f) {
        drawLine(
            color = material.topHighlightColor,
            start = Offset(inset, stroke * 1.5f),
            end = Offset(size.width - inset, stroke * 1.5f),
            strokeWidth = stroke,
        )
    }
    if (material.goldTopEdge) {
        val margin = 18.dp.toPx()
        if (size.width > margin * 2f) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.26f to Color(0xCCE3C08A),
                    0.64f to Color(0x57FFFFFF),
                    1f to Color.Transparent,
                    startX = margin,
                    endX = size.width - margin,
                ),
                topLeft = Offset(margin, 0f),
                size = Size(size.width - margin * 2f, stroke),
            )
        }
    }
    drawRoundRect(
        color = material.borderColor,
        topLeft = Offset(stroke / 2f, stroke / 2f),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius((cornerRadiusPx - stroke / 2f).coerceAtLeast(0f)),
        style = Stroke(width = stroke),
    )
}
