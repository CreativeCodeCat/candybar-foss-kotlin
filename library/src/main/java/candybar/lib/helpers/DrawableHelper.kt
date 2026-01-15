package candybar.lib.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.VectorDrawable
import android.os.Build
import android.util.Base64
import android.util.DisplayMetrics
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import com.danimahardhika.android.helpers.core.utils.LogUtil
import sarsamurmu.adaptiveicon.AdaptiveIcon
import java.io.ByteArrayOutputStream

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

object DrawableHelper {

    @JvmStatic
    fun getAppIcon(context: Context, info: android.content.pm.ResolveInfo): Drawable? {
        return try {
            info.activityInfo.loadIcon(context.packageManager)
        } catch (e: Exception) {
            ContextCompat.getDrawable(context, R.drawable.ic_app_default)
        } catch (e: OutOfMemoryError) {
            ContextCompat.getDrawable(context, R.drawable.ic_app_default)
        }
    }

    @JvmStatic
    fun getPackageIcon(context: Context, componentNameStr: String): Drawable? {
        val packageManager = context.packageManager

        val slashIndex = componentNameStr.indexOf("/")
        if (slashIndex == -1) return null

        val packageName = componentNameStr.substring(0, slashIndex)
        val activityName = componentNameStr.substring(slashIndex + 1)
        val componentName = ComponentName(packageName, activityName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Load Adaptive icon if possible
            val intent = Intent().apply {
                component = componentName
            }
            val resolveInfo = packageManager.resolveActivity(intent, 0)
            if (resolveInfo != null) {
                val adaptiveDrawable = resolveInfo.loadIcon(packageManager)
                if (adaptiveDrawable is AdaptiveIconDrawable) return adaptiveDrawable
            }
        }

        // Fallback to legacy icon if AdaptiveIcon is not found
        try {
            @Suppress("DEPRECATION")
            val appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val appResources = packageManager.getResourcesForApplication(appInfo)

            val drawable = ResourcesCompat.getDrawableForDensity(
                appResources, appInfo.icon,
                DisplayMetrics.DENSITY_XXXHIGH, null
            )

            if (drawable != null) return drawable
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        } catch (e: OutOfMemoryError) {
            LogUtil.e(Log.getStackTraceString(e))
        }

        LogUtil.e("DrawableHelper - drawable is null")
        return null
    }

    @JvmStatic
    fun toBitmap(drawable: Drawable): Bitmap? {
        // Using square shape for more detail (area) in icon image
        return toBitmap(drawable, AdaptiveIcon.PATH_SQUARE)
    }

    @JvmStatic
    fun toBitmap(drawable: Drawable, shape: Int): Bitmap? {
        if (drawable is BitmapDrawable) return drawable.bitmap
        if (drawable is LayerDrawable || drawable is VectorDrawable) {
            val isVector = drawable is VectorDrawable
            val width = if (isVector) 256 else drawable.intrinsicWidth
            val height = if (isVector) 256 else drawable.intrinsicHeight

            if (width <= 0 || height <= 0) return null

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(Canvas(bitmap))
            return bitmap
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && drawable is AdaptiveIconDrawable) {
            if (shape == -1) {
                // System default icon shape
                val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                drawable.setBounds(Rect(0, 0, 256, 256))
                drawable.draw(canvas)
                return bitmap
            }

            return AdaptiveIcon()
                .setDrawable(drawable)
                .setPath(shape)
                .render()
        }
        return null
    }

    @JvmStatic
    fun getReqIconBase64(drawable: Drawable): String {
        val appBitmap = toBitmap(drawable)
        val baos = ByteArrayOutputStream()
        appBitmap?.let {
            it.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val base64Icon = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
            return base64Icon.trim()
        }
        return ""
    }

    @JvmStatic
    fun getDrawableId(resource: String?): Int {
        if (resource == null) return -1
        return try {
            val mDrawableClass = CandyBarApplication.mDrawableClass
            if (mDrawableClass == null) {
                LogUtil.e("DrawableHelper - mDrawableClass is null")
                return -1
            }
            val idField = mDrawableClass.getDeclaredField(resource)
            idField.getInt(null)
        } catch (e: Exception) {
            LogUtil.e("Reflect resource not found with name - $resource")
            -1
        }
    }
}
