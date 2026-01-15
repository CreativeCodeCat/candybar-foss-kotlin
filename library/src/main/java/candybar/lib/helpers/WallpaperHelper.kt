package candybar.lib.helpers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.RectF
import android.net.Uri
import android.webkit.URLUtil
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import candybar.lib.applications.CandyBarApplication
import candybar.lib.items.ImageSize
import com.danimahardhika.android.helpers.core.WindowHelper
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

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

object WallpaperHelper {

    const val UNKNOWN = 0
    const val CLOUD_WALLPAPERS = 1
    const val EXTERNAL_APP = 2

    @JvmStatic
    fun getWallpaperType(@NonNull context: Context): Int {
        val url = CandyBarApplication.getConfiguration().configHandler!!.wallpaperJson(context)
        return when {
            url.startsWith("assets://") || URLUtil.isValidUrl(url) -> CLOUD_WALLPAPERS
            url.isNotEmpty() -> EXTERNAL_APP
            else -> UNKNOWN
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun getJSONStream(@NonNull context: Context): InputStream? {
        return getStream(context, CandyBarApplication.getConfiguration().configHandler!!.wallpaperJson(context))
    }

    /**
     * This method adds support for `assets` protocol for loading file from `assets` directory
     */
    @JvmStatic
    @Throws(IOException::class)
    fun getStream(context: Context, urlStr: String): InputStream? {
        var stream: InputStream? = null

        if (urlStr.startsWith("assets://")) {
            stream = context.assets.open(urlStr.replaceFirst("assets://".toRegex(), ""))
        } else {
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                stream = connection.inputStream
            }
        }

        return stream
    }

    @JvmStatic
    fun launchExternalApp(@NonNull context: Context) {
        val packageName = CandyBarApplication.getConfiguration().configHandler!!.wallpaperJson(context)

        val pm = context.packageManager
        val intent = pm.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            try {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            } catch (ignored: Exception) {
            }
        }

        try {
            val store = Intent(
                Intent.ACTION_VIEW, Uri.parse(
                    "https://play.google.com/store/apps/details?id=$packageName"
                )
            )
            context.startActivity(store)
        } catch (ignored: ActivityNotFoundException) {
        }
    }

    @JvmStatic
    fun getFormat(mimeType: String?): String {
        if (mimeType == null) return "jpg"
        return if ("image/png" == mimeType) {
            "png"
        } else "jpg"
    }

    @JvmStatic
    fun getTargetSize(@NonNull context: Context): ImageSize {
        val point = WindowHelper.getScreenSize(context)
        var targetHeight = point.y
        var targetWidth = point.x

        if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            targetHeight = point.x
            targetWidth = point.y
        }

        return ImageSize(targetWidth, targetHeight)
    }

    @JvmStatic
    @Nullable
    fun getScaledRectF(@Nullable rectF: RectF?, heightFactor: Float, widthFactor: Float): RectF? {
        if (rectF == null) return null

        val scaledRectF = RectF(rectF)
        scaledRectF.top *= heightFactor
        scaledRectF.bottom *= heightFactor
        scaledRectF.left *= widthFactor
        scaledRectF.right *= widthFactor
        return scaledRectF
    }
}
