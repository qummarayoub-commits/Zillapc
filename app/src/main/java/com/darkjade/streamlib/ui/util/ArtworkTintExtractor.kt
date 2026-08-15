package com.darkjade.streamlib.ui.util

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts a color "inspired by" a piece of artwork for the details-page
 * background — deliberately NOT the raw bright/vibrant color. We darken and
 * desaturate whatever Palette finds so the result stays cinematic, low
 * contrast behind text, and easy on the eyes, matching how modern streaming
 * apps do this (a subtle colored gradient, not a bright color wash).
 */
object ArtworkTintExtractor {

    /** Cache so re-opening the same title doesn't redo the work. */
    private val cache = mutableMapOf<String, Color?>()

    suspend fun extractTint(context: Context, imageUrl: String?): Color? {
        if (imageUrl.isNullOrBlank()) return null
        cache[imageUrl]?.let { return it }
        if (cache.containsKey(imageUrl)) return null // cached "no result"

        val result = withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    // Small target size — Palette doesn't need full resolution
                    // and this keeps extraction fast and light on memory.
                    .size(120, 180)
                    .allowHardware(false) // Palette needs a software bitmap
                    .build()
                val bitmapResult = loader.execute(request)
                val bitmap = (bitmapResult as? SuccessResult)?.drawable
                    ?.let { drawable ->
                        val b = Bitmap.createBitmap(
                            drawable.intrinsicWidth.coerceAtLeast(1),
                            drawable.intrinsicHeight.coerceAtLeast(1),
                            Bitmap.Config.ARGB_8888,
                        )
                        val canvas = android.graphics.Canvas(b)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        b
                    } ?: return@withContext null

                val palette = Palette.from(bitmap).generate()
                val swatch = palette.darkVibrantSwatch
                    ?: palette.darkMutedSwatch
                    ?: palette.vibrantSwatch
                    ?: palette.mutedSwatch
                    ?: palette.dominantSwatch
                    ?: return@withContext null

                darkenForBackground(swatch.rgb)
            } catch (e: Exception) {
                null
            }
        }

        cache[imageUrl] = result
        return result
    }

    /** Pulls a raw swatch color down to something safe/subtle behind text. */
    private fun darkenForBackground(argb: Int): Color {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        // Keep the hue, cap saturation, and pin lightness low — this is what
        // keeps the result "dark, subtle, eye-friendly" instead of a bright
        // color wash, regardless of how vivid the source swatch was.
        hsl[1] = hsl[1].coerceAtMost(0.45f)
        hsl[2] = hsl[2].coerceIn(0.08f, 0.16f)
        return Color(ColorUtils.HSLToColor(hsl))
    }
}
