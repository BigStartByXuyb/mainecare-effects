package com.mainecare.effects.glass

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The only glass surface the MaineCare App adapter should call.
 *
 * Wrap the page content once with [GlassBackdrop] and place [MaineGlassCard] inside it:
 *
 * ```kotlin
 * GlassBackdrop(image = backdrop) {
 *     MaineGlassCard(
 *         modifier = Modifier.fillMaxWidth().height(196.dp),
 *         material = MaineGlassMaterials.Advice,
 *         interactive = true,
 *     ) {
 *         // page content
 *     }
 * }
 * ```
 *
 * [material] supplies the v21 tint, blur, border, sheen and shadow values. Override only
 * [params] for a one-off refraction change; keep the preset as the visual source of truth.
 * Page code must not construct [GlassSurface], `RuntimeShader` or the displacement maps.
 */
@Composable
fun MaineGlassCard(
    modifier: Modifier = Modifier,
    material: GlassMaterial = MaineGlassMaterials.Card,
    params: LiquidGlassParams = material.liquid,
    interactive: Boolean = true,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassSurface(
        modifier = modifier,
        material = material,
        params = params,
        interactive = interactive,
        contentAlignment = contentAlignment,
        content = content,
    )
}
