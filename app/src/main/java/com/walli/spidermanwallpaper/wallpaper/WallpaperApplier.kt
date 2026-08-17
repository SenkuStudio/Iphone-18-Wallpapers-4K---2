package com.walli.spidermanwallpaper.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.os.Build
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import com.walli.spidermanwallpaper.domain.model.WallpaperTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class WallpaperApplier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun apply(
        url: String,
        target: WallpaperTarget,
        scale: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
        rotation: Float = 0f,
        brightness: Float = 1f,
        contrast: Float = 1f,
        blur: Float = 0f,
        saturation: Float = 1f,
        hue: Float = 0f,
        structure: Float = 0f,
        details: Float = 0f
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)
            if (!wallpaperManager.isWallpaperSupported) {
                error("Wallpapers are not supported on this device")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !wallpaperManager.isSetWallpaperAllowed) {
                error("Wallpaper changes are restricted on this device")
            }

            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) error("Image fetch failed with ${response.code}")
            val stream = response.body?.byteStream() ?: error("Empty image body")
            val originalBitmap = BitmapFactory.decodeStream(stream) ?: error("Unable to decode bitmap")

            val transformedBitmap = transformBitmap(
                originalBitmap, scale, offsetX, offsetY, rotation,
                brightness, contrast, blur, saturation, hue, structure, details
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val flags = when (target) {
                    WallpaperTarget.HOME -> WallpaperManager.FLAG_SYSTEM
                    WallpaperTarget.LOCK -> WallpaperManager.FLAG_LOCK
                    WallpaperTarget.BOTH -> WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                }
                wallpaperManager.setBitmap(transformedBitmap, null, true, flags)
            } else {
                wallpaperManager.setBitmap(transformedBitmap)
            }
            Unit
        }
    }

    fun transformBitmap(
        bitmap: Bitmap,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        rotation: Float,
        brightness: Float = 1f,
        contrast: Float = 1f,
        blur: Float = 0f,
        saturation: Float = 1f,
        hue: Float = 0f,
        structure: Float = 0f,
        details: Float = 0f
    ): Bitmap {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val result = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Color transformations
        val cm = ColorMatrix()
        
        // 1. Saturation
        if (saturation != 1f) {
            cm.setSaturation(saturation)
        }
        
        // 2. Brightness & Contrast
        if (brightness != 1f || contrast != 1f) {
            val brightnessOffset = (brightness - 1f) * 255f
            val bcMatrix = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, brightnessOffset,
                0f, contrast, 0f, 0f, brightnessOffset,
                0f, 0f, contrast, 0f, brightnessOffset,
                0f, 0f, 0f, 1f, 0f
            ))
            cm.postConcat(bcMatrix)
        }

        // 3. Hue (Rotation)
        if (hue != 0f) {
            val hueMatrix = ColorMatrix()
            // Hue rotation is done by rotating R, G, B channels.
            // We use individual rotations to match the Compose UI implementation.
            hueMatrix.setRotate(0, hue)
            cm.postConcat(hueMatrix)
            hueMatrix.setRotate(1, hue)
            cm.postConcat(hueMatrix)
            hueMatrix.setRotate(2, hue)
            cm.postConcat(hueMatrix)
        }

        paint.colorFilter = ColorMatrixColorFilter(cm)

        val matrix = Matrix()

        // 1. Calculate initial scale to fill screen (Center Crop)
        val scaleX = screenWidth.toFloat() / bitmap.width
        val scaleY = screenHeight.toFloat() / bitmap.height
        val baseScale = maxOf(scaleX, scaleY)

        matrix.postScale(baseScale, baseScale)

        // 2. Center the image initially
        val centeredX = (screenWidth - bitmap.width * baseScale) / 2
        val centeredY = (screenHeight - bitmap.height * baseScale) / 2
        matrix.postTranslate(centeredX, centeredY)

        // 3. Apply user transformations
        matrix.postScale(scale, scale, screenWidth / 2f, screenHeight / 2f)
        matrix.postRotate(rotation, screenWidth / 2f, screenHeight / 2f)
        matrix.postTranslate(offsetX, offsetY)

        canvas.drawBitmap(bitmap, matrix, paint)

        var finalResult = result

        // 4. Structure & Details (API 31+ for RenderEffect)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val density = displayMetrics.density
            
            // Helper to set render effect via reflection to avoid compile issues in some environments
            fun Paint.setRenderEffectCompat(effect: RenderEffect?) {
                try {
                    Paint::class.java.getMethod("setRenderEffect", RenderEffect::class.java)
                        .invoke(this, effect)
                } catch (e: Exception) {}
            }

            // Helper to create a High Pass bitmap for sharpening/structure
            fun createHighPass(source: Bitmap, radius: Float): Bitmap {
                val blurred = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                val bCanvas = Canvas(blurred)
                val bPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    setRenderEffectCompat(RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP))
                }
                bCanvas.drawBitmap(source, 0f, 0f, bPaint)

                val highPass = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
                val hpCanvas = Canvas(highPass)
                
                // High Pass = Original - Blurred + 0.5
                // 1. Draw Inverted Blurred: (1 - Blurred)
                val invertPaint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(floatArrayOf(
                        -1f,  0f,  0f, 0f, 255f,
                         0f, -1f,  0f, 0f, 255f,
                         0f,  0f, -1f, 0f, 255f,
                         0f,  0f,  0f, 1f, 0f
                    ))
                }
                hpCanvas.drawBitmap(blurred, 0f, 0f, invertPaint)
                
                // 2. Blend Original with 50% alpha: 0.5 * Original + 0.5 * (1 - Blurred) = 0.5 * (Original - Blurred + 1)
                hpCanvas.drawBitmap(source, 0f, 0f, Paint().apply { alpha = 128 })
                
                blurred.recycle()
                return highPass
            }

            if (details > 0f) {
                // Sharpening: Small radius high pass
                val hp = createHighPass(finalResult, 0.8f * density)
                val output = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                canvas.drawBitmap(finalResult, 0f, 0f, null)
                
                // Overlay for sharpening
                val p = Paint().apply {
                    blendMode = android.graphics.BlendMode.OVERLAY
                    alpha = ((details / 10f) * 255).toInt().coerceIn(0, 255)
                }
                canvas.drawBitmap(hp, 0f, 0f, p)
                
                if (finalResult != result) finalResult.recycle()
                finalResult = output
                hp.recycle()
            }

            if (structure > 0f) {
                // Structure: Large radius high pass for local contrast
                val hp = createHighPass(finalResult, 12f * density)
                val output = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(output)
                canvas.drawBitmap(finalResult, 0f, 0f, null)
                
                // Hard Light gives a stronger local contrast effect (Structure)
                val p = Paint().apply {
                    blendMode = android.graphics.BlendMode.HARD_LIGHT
                    alpha = ((structure / 10f) * 200).toInt().coerceIn(0, 255)
                }
                canvas.drawBitmap(hp, 0f, 0f, p)
                
                if (finalResult != result) finalResult.recycle()
                finalResult = output
                hp.recycle()
            }
        }

        // 5. Apply Blur (API 31+)
        if (blur > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val blurredBitmap = Bitmap.createBitmap(screenWidth, screenHeight, Bitmap.Config.ARGB_8888)
            val blurCanvas = Canvas(blurredBitmap)
            val blurPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                try {
                    Paint::class.java.getMethod("setRenderEffect", RenderEffect::class.java)
                        .invoke(this, RenderEffect.createBlurEffect(blur, blur, Shader.TileMode.CLAMP))
                } catch (e: Exception) {}
            }
            
            blurCanvas.drawBitmap(finalResult, 0f, 0f, blurPaint)
            val toReturn = blurredBitmap
            if (finalResult != result) finalResult.recycle()
            result.recycle() // Clean up original result
            return toReturn
        }

        if (finalResult != result) {
            result.recycle() // Clean up original result if we created a new one
        }
        return finalResult
    }
}
