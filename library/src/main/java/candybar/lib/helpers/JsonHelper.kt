package candybar.lib.helpers

import android.annotation.SuppressLint
import android.util.Log
import candybar.lib.applications.CandyBarApplication
import candybar.lib.items.Wallpaper
import com.bluelinelabs.logansquare.LoganSquare
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.io.IOException
import java.io.InputStream

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

object JsonHelper {

    @SuppressLint("Raw")
    @JvmStatic
    fun parseList(stream: InputStream): List<*>? {
        var list: List<*>? = null
        val jsonStructure = CandyBarApplication.getConfiguration().wallpaperJsonStructure

        try {
            if (jsonStructure.arrayName == null) {
                list = LoganSquare.parseList(stream, Map::class.java)
            } else {
                val map = LoganSquare.parseMap(stream, List::class.java)
                list = map[jsonStructure.arrayName]
            }
        } catch (e: IOException) {
            LogUtil.e(Log.getStackTraceString(e))
        }
        return list
    }

    @JvmStatic
    fun getWallpaper(obj: Any): Wallpaper? {
        if (obj is Map<*, *>) {
            val jsonStructure = CandyBarApplication.getConfiguration().wallpaperJsonStructure
            return Wallpaper.Builder()
                .name(obj[jsonStructure.name] as? String)
                .author(getAuthor(obj))
                .url((obj[jsonStructure.url] as? String).orEmpty())
                .thumbUrl(getThumbUrl(obj).orEmpty())
                .build()
        }
        return null
    }

    @JvmStatic
    fun getThumbUrl(map: Map<*, *>): String? {
        val jsonStructure = CandyBarApplication.getConfiguration().wallpaperJsonStructure
        val url = map[jsonStructure.url] as? String
        if (jsonStructure.thumbUrl == null) return url

        val thumbUrlSelectors = arrayOf("thumb", "thumbnail", "thumbUrl", "url-thumb", "urlThumb")

        var thumbUrl = map[jsonStructure.thumbUrl] as? String
        for (selector in thumbUrlSelectors) {
            if (thumbUrl == null) thumbUrl = map[selector] as? String
        }
        return thumbUrl ?: url
    }

    @JvmStatic
    private fun getAuthor(map: Map<*, *>): String {
        val jsonStructure = CandyBarApplication.getConfiguration().wallpaperJsonStructure
        val authorName = map[jsonStructure.author] as? String
        return authorName ?: ""
    }
}
