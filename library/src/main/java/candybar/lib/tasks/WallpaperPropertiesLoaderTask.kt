package candybar.lib.tasks

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import candybar.lib.databases.Database
import candybar.lib.helpers.WallpaperHelper
import candybar.lib.items.ImageSize
import candybar.lib.items.Wallpaper
import candybar.lib.utils.AsyncTaskBase
import com.bumptech.glide.Glide
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.lang.ref.WeakReference

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

class WallpaperPropertiesLoaderTask(
    context: Context,
    private val mWallpaper: Wallpaper?,
    callback: Callback?
) : AsyncTaskBase() {

    private val mContext: WeakReference<Context> = WeakReference(context)
    private val mCallback: WeakReference<Callback>? = if (callback != null) WeakReference(callback) else null

    override fun run(): Boolean {
        if (!isCancelled) {
            try {
                Thread.sleep(1)
                val wallpaper = mWallpaper ?: return false
                val context = mContext.get() ?: return false

                if (wallpaper.dimensions != null &&
                    wallpaper.mimeType != null &&
                    wallpaper.size > 0
                ) {
                    return false
                }

                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true

                val stream = WallpaperHelper.getStream(context, wallpaper.url)

                if (stream != null) {
                    BitmapFactory.decodeStream(stream, null, options)

                    val imageSize = ImageSize(options.outWidth, options.outHeight)
                    wallpaper.dimensions = imageSize
                    wallpaper.mimeType = options.outMimeType

                    // In legacy Java, decoding stream with JustDecodeBounds=true returns a null bitmap, 
                    // but the original code was allocating a bitmap to get its size? 
                    // Wait, let's re-read the Java code:
                    // Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
                    // int contentLength = bitmap.getAllocationByteCount();
                    // If inJustDecodeBounds is true, decodeStream returns null. 
                    // So bitmap.getAllocationByteCount() would throw NPE.
                    // But the original code had:
                    // Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
                    // int contentLength = bitmap.getAllocationByteCount();
                    // This implies inJustDecodeBounds was NOT true when it got the bitmap.
                    // But line 68 was options.inJustDecodeBounds = true;
                    // Ah, maybe the stream was decoded twice? No.

                    // Actually, if inJustDecodeBounds is true, decodeStream returns null.
                    // Let's check the original code again.
                    // 73: Bitmap bitmap = BitmapFactory.decodeStream(stream, null, options);
                    // 79: int contentLength = bitmap.getAllocationByteCount();
                    // Yes, this is definitely an NPE if inJustDecodeBounds is true.

                    // Let's assume the user meant to get the size from the stream or some other way.
                    // Glide is used in postRun to get size from cache if not available.

                    Database.get(context).updateWallpaper(wallpaper)
                    stream.close()
                    return true
                }
                return false
            } catch (e: Exception) {
                LogUtil.e(Log.getStackTraceString(e))
                return false
            }
        }
        return false
    }

    override fun postRun(ok: Boolean) {
        val context = mContext.get() ?: return
        val wallpaper = mWallpaper ?: return

        if (ok && (context as? AppCompatActivity)?.isFinishing == false) {
            if (wallpaper.size <= 0) {
                try {
                    val target = Glide.with(context)
                        .asFile()
                        .load(wallpaper.url)
                        .onlyRetrieveFromCache(true)
                        .submit()
                        .get()
                    if (target != null && target.exists()) {
                        wallpaper.size = target.length().toInt()
                    }
                } catch (ignored: Exception) {
                }
            }
        }

        mCallback?.get()?.onPropertiesReceived(wallpaper)
    }

    fun interface Callback {
        fun onPropertiesReceived(wallpaper: Wallpaper)
    }
}
