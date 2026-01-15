package candybar.lib.tasks

import android.content.Context
import android.util.Log
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.JsonHelper
import candybar.lib.helpers.WallpaperHelper
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.ImageConfig
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.lang.ref.WeakReference

class WallpaperThumbPreloaderTask(context: Context) : AsyncTaskBase() {

    private val mContext: WeakReference<Context> = WeakReference(context)

    override fun run(): Boolean {
        if (!isCancelled) {
            try {
                Thread.sleep(1)
                val context = mContext.get() ?: return false

                if (WallpaperHelper.getWallpaperType(context) != WallpaperHelper.CLOUD_WALLPAPERS)
                    return true

                val stream = WallpaperHelper.getJSONStream(context)

                if (stream != null) {
                    val list = JsonHelper.parseList(stream)
                    if (list == null) {
                        LogUtil.e(
                            "Json error, no array with name: "
                                    + CandyBarApplication.getConfiguration().wallpaperJsonStructure.arrayName
                        )
                        return false
                    }

                    if (list.isNotEmpty()) {
                        val map = list[0]
                        if (map is Map<*, *>) {
                            val thumbUrl = JsonHelper.getThumbUrl(map)

                            // Preload the first wallpaper's thumbnail
                            // It should show up immediately without any delay on first run
                            // so that the intro popup works correctly
                            Glide.with(context)
                                .load(thumbUrl)
                                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                                .override(ImageConfig.getThumbnailSize())
                                .preload()
                        }
                    }
                    stream.close()
                }
                return true
            } catch (e: Exception) {
                LogUtil.e(Log.getStackTraceString(e))
                return false
            }
        }
        return false
    }
}
