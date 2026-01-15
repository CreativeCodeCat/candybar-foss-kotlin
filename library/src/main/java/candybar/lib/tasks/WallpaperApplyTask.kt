package candybar.lib.tasks

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import candybar.lib.R
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.helpers.WallpaperHelper
import candybar.lib.items.ImageSize
import candybar.lib.items.Wallpaper
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.danimahardhika.cafebar.CafeBar
import com.danimahardhika.cafebar.CafeBarTheme
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.ExecutorService

/*
 * CandyBar - Material Dashboard
 *
 * Copyright (c) 2014-2016 Dani Mahardhika
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class WallpaperApplyTask(context: Context, private var mWallpaper: Wallpaper?) : AsyncTaskBase(), WallpaperPropertiesLoaderTask.Callback {

    private val mContext: WeakReference<Context> = WeakReference(context)
    private var mApply: Apply = Apply.HOMESCREEN
    private var mRectF: RectF? = null
    private var mDialog: MaterialDialog? = null

    fun to(apply: Apply): WallpaperApplyTask {
        mApply = apply
        return this
    }

    fun crop(rectF: RectF?): WallpaperApplyTask {
        mRectF = rectF
        return this
    }

    override fun execute(executorService: ExecutorService): AsyncTaskBase {
        val context = mContext.get() ?: return this
        val wallpaper = mWallpaper ?: run {
            LogUtil.e("WallpaperApply cancelled, wallpaper is null")
            return this
        }

        if (mDialog == null) {
            var color = wallpaper.color
            if (color == 0) {
                color = ColorHelper.getAttributeColor(context, com.google.android.material.R.attr.colorSecondary)
            }

            val builder = MaterialDialog.Builder(context)
            builder.widgetColor(color)
                .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
                .progress(true, 0)
                .cancelable(false)
                .progressIndeterminateStyle(true)
                .content(R.string.wallpaper_loading)
                .positiveColor(color)
                .positiveText(android.R.string.cancel)
                .onPositive { _, _ -> cancel(true) }

            mDialog = builder.build()
        }

        if (mDialog?.isShowing == false) mDialog?.show()

        if (wallpaper.dimensions == null) {
            return WallpaperPropertiesLoaderTask(context, wallpaper, this)
                .executeOnThreadPool()
        }

        return super.execute(executorService)
    }

    override fun onPropertiesReceived(wallpaper: Wallpaper) {
        mWallpaper = wallpaper
        val context = mContext.get() ?: return

        if (wallpaper.dimensions == null) {
            LogUtil.e("WallpaperApply cancelled, unable to retrieve wallpaper dimensions")

            if (context is Activity && context.isFinishing) return

            if (mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }

            Toast.makeText(context, R.string.wallpaper_apply_failed, Toast.LENGTH_LONG).show()
            return
        }

        try {
            executeOnThreadPool()
        } catch (e: IllegalStateException) {
            LogUtil.e(Log.getStackTraceString(e))
        }
    }

    override fun run(): Boolean {
        if (!isCancelled) {
            try {
                Thread.sleep(1)
                val context = mContext.get() ?: return false
                val wallpaper = mWallpaper ?: return false
                val wallpaperDimensions = wallpaper.dimensions ?: return false
                val imageSize = WallpaperHelper.getTargetSize(context)

                LogUtil.d("original rectF: $mRectF")

                if (mRectF == null && Preferences.get(context).isCropWallpaper) {
                    val widthScaleFactor = imageSize.height.toFloat() / wallpaperDimensions.height.toFloat()
                    val side = (wallpaperDimensions.width.toFloat() * widthScaleFactor - imageSize.width.toFloat()) / 2f
                    val leftRectF = 0f - side
                    val rightRectF = wallpaperDimensions.width.toFloat() * widthScaleFactor - side
                    val topRectF = 0f
                    val bottomRectF = imageSize.height.toFloat()
                    mRectF = RectF(leftRectF, topRectF, rightRectF, bottomRectF)
                    LogUtil.d("created center crop rectF: $mRectF")
                }

                var adjustedSize = imageSize
                var adjustedRectF = mRectF

                val scaleFactor = wallpaperDimensions.height.toFloat() / imageSize.height.toFloat()
                if (scaleFactor > 1f) {
                    val widthScaleFactor = imageSize.height.toFloat() / wallpaperDimensions.height.toFloat()
                    val adjustedWidth = (wallpaperDimensions.width.toFloat() * widthScaleFactor).toInt()
                    adjustedSize = ImageSize(adjustedWidth, imageSize.height)

                    if (adjustedRectF != null) {
                        adjustedSize = ImageSize(wallpaperDimensions.width, wallpaperDimensions.height)
                        adjustedRectF = WallpaperHelper.getScaledRectF(mRectF, scaleFactor, scaleFactor)
                        LogUtil.d("adjusted rectF: $adjustedRectF")
                    }

                    LogUtil.d(String.format(Locale.getDefault(), "adjusted bitmap: %d x %d", adjustedSize.width, adjustedSize.height))
                }

                var call = 1
                do {
                    val loadedBitmap = Glide.with(context)
                        .asBitmap()
                        .load(wallpaper.url)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .submit()
                        .get()

                    if (loadedBitmap != null) {
                        try {
                            val bitmapTemp = Bitmap.createBitmap(
                                loadedBitmap.width,
                                loadedBitmap.height,
                                loadedBitmap.config ?: Bitmap.Config.ARGB_8888
                            )
                            bitmapTemp.recycle()

                            LogUtil.d(String.format(Locale.getDefault(), "loaded bitmap: %d x %d", loadedBitmap.width, loadedBitmap.height))
                            runOnUiThread { mDialog?.setContent(R.string.wallpaper_applying) }

                            var bitmap = loadedBitmap
                            if (Preferences.get(context).isCropWallpaper && adjustedRectF != null) {
                                LogUtil.d("rectF: $adjustedRectF")
                                val targetSize = WallpaperHelper.getTargetSize(context)
                                val targetWidth = ((loadedBitmap.height.toDouble() / targetSize.height.toDouble()) * targetSize.width.toDouble()).toInt()

                                bitmap = Bitmap.createBitmap(targetWidth, loadedBitmap.height, loadedBitmap.config ?: Bitmap.Config.ARGB_8888)
                                val paint = Paint().apply {
                                    isFilterBitmap = true
                                    isAntiAlias = true
                                    isDither = true
                                }

                                val canvas = Canvas(bitmap)
                                canvas.drawBitmap(loadedBitmap, null, adjustedRectF, paint)

                                val scale = targetSize.height.toFloat() / bitmap.height.toFloat()
                                if (scale < 1f) {
                                    LogUtil.d("bitmap size is bigger than screen resolution, resizing bitmap")
                                    val resizedWidth = (bitmap.width.toFloat() * scale).toInt()
                                    bitmap = Bitmap.createScaledBitmap(bitmap, resizedWidth, targetSize.height, true)
                                }
                            }

                            LogUtil.d(String.format(Locale.getDefault(), "generated bitmap: %d x %d ", bitmap.width, bitmap.height))

                            val wm = WallpaperManager.getInstance(context.applicationContext)
                            when (mApply) {
                                Apply.HOMESCREEN_LOCKSCREEN -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK or WallpaperManager.FLAG_SYSTEM)
                                        return true
                                    }
                                }

                                Apply.HOMESCREEN -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                        return true
                                    }
                                    wm.setBitmap(bitmap)
                                    return true
                                }

                                Apply.LOCKSCREEN -> {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                                        return true
                                    }
                                }
                            }
                        } catch (e: OutOfMemoryError) {
                            LogUtil.e("loaded bitmap is too big, resizing it ...")
                            val scale = 1.0 - (0.1 * call)
                            val scaledWidth = (adjustedSize.width * scale).toInt()
                            val scaledHeight = (adjustedSize.height * scale).toInt()

                            adjustedRectF = WallpaperHelper.getScaledRectF(adjustedRectF, scale.toFloat(), scale.toFloat())
                            adjustedSize = ImageSize(scaledWidth, scaledHeight)
                        }
                    }
                    call++
                } while (call <= 5 && !isCancelled)
                return false
            } catch (e: Exception) {
                LogUtil.e(Log.getStackTraceString(e))
                return false
            }
        }
        return false
    }

    override fun cancel(mayInterruptIfRunning: Boolean) {
        super.cancel(mayInterruptIfRunning)
        mContext.get()?.let {
            Toast.makeText(it, R.string.wallpaper_apply_cancelled, Toast.LENGTH_LONG).show()
        }
    }

    override fun postRun(ok: Boolean) {
        val context = mContext.get() ?: return
        if ((context as? AppCompatActivity)?.let { it.isFinishing || it.isDestroyed } == true) return

        mDialog?.let { if (it.isShowing) it.dismiss() }

        if (ok) {
            CafeBar.builder(context)
                .theme(CafeBarTheme.Custom(ColorHelper.getAttributeColor(context, R.attr.cb_cardBackground)))
                .contentTypeface(TypefaceHelper.getRegular(context))
                .floating(true)
                .fitSystemWindow()
                .content(R.string.wallpaper_applied)
                .show()
        } else {
            Toast.makeText(context, R.string.wallpaper_apply_failed, Toast.LENGTH_LONG).show()
        }
    }

    enum class Apply {
        LOCKSCREEN,
        HOMESCREEN,
        HOMESCREEN_LOCKSCREEN
    }
}
