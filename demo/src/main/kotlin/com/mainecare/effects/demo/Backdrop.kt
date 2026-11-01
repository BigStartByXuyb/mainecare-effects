package com.mainecare.effects.demo

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberBackdropAsset(name: String): ImageBitmap? {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, name) {
        value = withContext(Dispatchers.IO) {
            context.assets.open(name).use { BitmapFactory.decodeStream(it) }?.asImageBitmap()
        }
    }
    return image
}
