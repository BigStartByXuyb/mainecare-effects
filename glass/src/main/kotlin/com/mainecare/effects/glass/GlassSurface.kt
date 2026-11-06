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
import androidx.compose.ui.graphics.BlendMode
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
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

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
    val debugNoOverlays = debugOutput > 0.5f
    var surfaceBounds by remember { mutableStateOf(Rect.Zero) }
    var surfaceSize by remember { mutableStateOf(Size.Zero) }
    val neutralPointer = if (material.edgeStyle == GlassEdgeStyle.Upstream) {
        Offset(0.5f, 0.5f)
    } else {
        Offset(0.72f, 0.08f)
    }
    var pointer by remember(material.edgeStyle) { mutableStateOf(neutralPointer) }

    val mapBitmap = remember(params.mode) {
        BitmapFactory.decodeResource(context.resources, mapResource(params.mode))
    }
    val blurred by produceState<ImageBitmap?>(
        initialValue = null,
        backdrop,
        material.blurRadiusDp,
        density.density,
        backdrop?.boundsInRoot,
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
    val sceneImage = backdrop?.sceneImage ?: backdrop?.image
    val sceneShader = remember(sceneImage) {
        sceneImage?.let {
            BitmapShader(it.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
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
                pointer = neutralPointer
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
                            sceneShader = sceneShader!!,
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
                    if (!debugNoOverlays) drawInnerShadow(material, size)
                }
                .drawWithContent {
                    drawContent()
                    if (!debugNoOverlays) {
                        if (material.edgeStyle == GlassEdgeStyle.Upstream) {
                            drawUpstreamEdge(material, params.cornerRadius.toPx(), size, pointer)
                        } else {
                            drawBorderAndHighlights(material, params.cornerRadius.toPx(), size)
                        }
                    }
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
    sceneShader: BitmapShader,
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
    val factorX = backdropBitmap.width / bounds.width
    val factorY = backdropBitmap.height / bounds.height

    val inset = density
    shader.setFloatUniform("uSize", surfaceSize.width, surfaceSize.height)
    shader.setFloatUniform("uInset", inset)
    shader.setFloatUniform(
        "uWarpSize",
        (surfaceSize.width - 2f * inset).coerceAtLeast(1f),
        (surfaceSize.height - 2f * inset).coerceAtLeast(1f),
    )
    shader.setFloatUniform(
        "uBackdropOrigin",
        (surfaceBounds.left + inset - bounds.left) * factorX,
        (surfaceBounds.top + inset - bounds.top) * factorY,
    )
    shader.setFloatUniform("uBackdropScale", factorX, factorY)
    val scene = backdrop.sceneImage ?: backdrop.image
    val fit = backdrop.contentScale.computeScaleFactor(
        Size(scene.width.toFloat(), scene.height.toFloat()),
        Size(bounds.width, bounds.height),
    )
    val drawnWidth = scene.width * fit.scaleX
    val drawnHeight = scene.height * fit.scaleY
    shader.setFloatUniform(
        "uSceneOrigin",
        (surfaceBounds.left + inset - bounds.left - (bounds.width - drawnWidth) / 2f) / fit.scaleX,
        (surfaceBounds.top + inset - bounds.top - (bounds.height - drawnHeight) / 2f) / fit.scaleY,
    )
    shader.setFloatUniform("uSceneScale", 1f / fit.scaleX, 1f / fit.scaleY)
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
    shader.setFloatUniform("uDensity", density)
    shader.setFloatUniform("uBlurSigma", material.blurRadiusDp * density)
    shader.setFloatUniform(
        "uOpaqueWarp",
        if (material.edgeStyle == GlassEdgeStyle.Upstream) 1f else 0f,
    )
    shader.setFloatUniform("uUnderlayAlpha", material.backgroundUnderlayAlpha)
    setColorUniform(shader, "uBorderColor", material.borderColor)
    shader.setFloatUniform("uBorderWidth", floor(density).coerceAtLeast(1f))
    shader.setFloatUniform("uCornerRadius", params.cornerRadius.value * density)
    shader.setFloatUniform("uAfterGlow", if (material.afterGlow) 1f else 0f)
    shader.setFloatUniform("uDebug", debugOutput)
    shader.setFloatUniform("uSheenTopCenter", material.sheenTopCenter.x, material.sheenTopCenter.y)
    shader.setFloatUniform("uSheenTopRadii", material.sheenTopRadii.x, material.sheenTopRadii.y)
    shader.setFloatUniform("uSheenTopFade", material.sheenTopFade)
    setColorUniform(shader, "uSheenTopRight", material.sheenTopRight)
    setColorUniform(shader, "uSheenBottom", material.sheenBottom)
    setColorUniform(shader, "uSheenDiagonal", material.sheenDiagonal)
    shader.setFloatUniform("uSheenDiagonalAngle", material.sheenDiagonalAngle)
    shader.setFloatUniform("uSheenDiagonalStart", material.sheenDiagonalStart)
    shader.setFloatUniform("uSheenDiagonalPeak", material.sheenDiagonalPeak)
    shader.setFloatUniform("uSheenDiagonalEnd", material.sheenDiagonalEnd)
    setColorUniform(shader, "uSheenDiagonal2", material.sheenDiagonal2)
    shader.setFloatUniform("uSheenDiagonal2Start", material.sheenDiagonal2Start)
    shader.setFloatUniform("uSheenDiagonal2Peak", material.sheenDiagonal2Peak)
    shader.setFloatUniform("uSheenDiagonal2End", material.sheenDiagonal2End)
    setColorUniform(shader, "uSheenDark", material.sheenDark)
    setColorUniform(shader, "uSheenVerticalTop", material.sheenVerticalTop)
    setColorUniform(shader, "uSheenVerticalMid", material.sheenVerticalMid)
    shader.setFloatUniform("uSheenVerticalMidPos", material.sheenVerticalMidPos)
    setColorUniform(shader, "uSheenVerticalBottom", material.sheenVerticalBottom)
    shader.setInputBuffer("uMap", mapShader)
    shader.setInputBuffer("uBackdrop", backdropShader)
    shader.setInputBuffer("uScene", sceneShader)
    paint.shader = shader
}

private fun setColorUniform(shader: RuntimeShader, name: String, color: Color) {
    shader.setFloatUniform(name, color.red, color.green, color.blue, color.alpha)
}

private fun DrawScope.drawInnerShadow(material: GlassMaterial, size: Size) {
    if (material.bottomInnerShadowColor.alpha <= 0f || size.width <= 0f) return
    // CSS inset box-shadow: the shadow edge is offset from the bottom, then blurred.
    // Chrome's blur covers the whole short v21 pill, so model the full Gaussian tail.
    val sigma = (material.innerShadowDepthDp * 0.5f).dp.toPx().coerceAtLeast(0.5f)
    val edge = size.height + material.innerShadowOffsetYDp.dp.toPx()
    val steps = 32
    val colors = ArrayList<Color>(steps + 1)
    for (i in 0..steps) {
        val y = size.height * i / steps
        val alpha = material.bottomInnerShadowColor.alpha * gaussianCdf((y - edge) / sigma)
        colors += material.bottomInnerShadowColor.copy(alpha = alpha)
    }
    drawRect(
        brush = Brush.verticalGradient(
            colors = colors,
            startY = 0f,
            endY = size.height,
        ),
        topLeft = Offset(0f, 0f),
        size = size,
    )
}

private fun gaussianCdf(z: Float): Float {
    val x = z.toDouble()
    val t = 1.0 / (1.0 + 0.3275911 * kotlin.math.abs(x))
    val poly = (((((1.061405429 * t - 1.453152027) * t) + 1.421413741) * t -
        0.284496736) * t + 0.254829592) * t
    val erf = (1.0 - poly * kotlin.math.exp(-x * x)) * if (x < 0) -1.0 else 1.0
    return (0.5 * (1.0 + erf)).toFloat()
}

private fun DrawScope.drawUpstreamEdge(
    material: GlassMaterial,
    cornerRadiusPx: Float,
    size: Size,
    pointer: Offset,
) {
    if (size.width <= 1f || size.height <= 1f) return
    val border = 1.5.dp.toPx()
    val offsetX = (pointer.x - 0.5f) * 100f
    val offsetY = (pointer.y - 0.5f) * 100f
    val angle = (135f + offsetX * 1.2f) * kotlin.math.PI.toFloat() / 180f
    val p1 = ((33f + offsetY * 0.3f) / 100f).coerceIn(0.1f, 0.9f)
    val p2 = ((66f + offsetY * 0.4f) / 100f).coerceIn(0.1f, 0.9f)
    val a1 = (0.12f + abs(offsetX) * 0.008f).coerceAtMost(0.9f)
    val a2 = (0.40f + abs(offsetX) * 0.012f).coerceAtMost(0.9f)
    val b1 = (0.32f + abs(offsetX) * 0.008f).coerceAtMost(0.95f)
    val b2 = (0.60f + abs(offsetX) * 0.012f).coerceAtMost(0.98f)
    val dir = Offset(sin(angle), -cos(angle))
    val length = abs(size.width * dir.x) + abs(size.height * dir.y)
    val center = Offset(size.width / 2f, size.height / 2f)
    val start = center - dir * (length / 2f)
    val end = center + dir * (length / 2f)
    fun brush(first: Float, second: Float): Brush = Brush.linearGradient(
        0f to Color.Transparent,
        p1 to Color.White.copy(alpha = first),
        p2 to Color.White.copy(alpha = second),
        1f to Color.Transparent,
        start = start,
        end = end,
    )
    val inset = border / 2f
    val innerRadius = (cornerRadiusPx - inset).coerceAtLeast(0f)
    drawRoundRect(
        brush = brush(a1, a2),
        topLeft = Offset(inset, inset),
        size = Size(size.width - border, size.height - border),
        cornerRadius = CornerRadius(innerRadius),
        style = Stroke(width = border),
        alpha = 0.2f,
        blendMode = BlendMode.Screen,
    )
    drawRoundRect(
        brush = brush(b1, b2),
        topLeft = Offset(inset, inset),
        size = Size(size.width - border, size.height - border),
        cornerRadius = CornerRadius(innerRadius),
        style = Stroke(width = border),
        blendMode = BlendMode.Overlay,
    )
    val thin = 0.5.dp.toPx()
    drawRoundRect(
        color = Color.White.copy(alpha = 0.5f),
        topLeft = Offset(thin / 2f, thin / 2f),
        size = Size(size.width - thin, size.height - thin),
        cornerRadius = CornerRadius((cornerRadiusPx - thin / 2f).coerceAtLeast(0f)),
        style = Stroke(width = thin),
        blendMode = BlendMode.Screen,
    )
    val highlight = 1.dp.toPx()
    val margin = (cornerRadiusPx * 0.7f).coerceAtMost(size.width / 2f - 1f)
    drawLine(
        color = Color.White.copy(alpha = 0.25f),
        start = Offset(margin, highlight * 1.5f),
        end = Offset(size.width - margin, highlight * 1.5f),
        strokeWidth = highlight,
        blendMode = BlendMode.Screen,
    )
}

private fun DrawScope.drawBorderAndHighlights(
    material: GlassMaterial,
    cornerRadiusPx: Float,
    size: Size,
) {
    if (size.width <= 1f || size.height <= 1f) return
    // Chrome snaps a 1 CSS px border to whole device pixels at fractional DPR.
    val stroke = floor(1.dp.toPx()).coerceAtLeast(1f)
    val highlightWidth = 1.dp.toPx()
    val highlightCenter = stroke + highlightWidth / 2f
    val inset = (cornerRadiusPx * 0.55f)
        .coerceAtMost(size.width / 2f - 1f)
        .coerceAtLeast(0f)
    if (inset < size.width / 2f) {
        drawLine(
            color = material.topHighlightColor,
            start = Offset(inset, highlightCenter),
            end = Offset(size.width - inset, highlightCenter),
            strokeWidth = highlightWidth,
        )
        drawLine(
            color = material.topHighlightColor.copy(alpha = material.topHighlightColor.alpha * 0.14f),
            start = Offset(highlightCenter, inset),
            end = Offset(highlightCenter, size.height - inset),
            strokeWidth = highlightWidth,
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
}
