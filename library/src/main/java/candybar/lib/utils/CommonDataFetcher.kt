package candybar.lib.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.ContextCompat
import candybar.lib.helpers.DrawableHelper
import candybar.lib.preferences.Preferences
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.data.DataFetcher
import com.danimahardhika.android.helpers.core.utils.LogUtil
import sarsamurmu.adaptiveicon.AdaptiveIcon
import java.io.IOException

class CommonDataFetcher(private val mContext: Context, private val mModel: String) : DataFetcher<Bitmap> {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
        if (mModel.startsWith("drawable://")) {
            callback.onDataReady(getDrawable(mModel))
        } else if (mModel.startsWith("package://")) {
            callback.onDataReady(getPackage(mModel))
        } else if (mModel.startsWith("assets://")) {
            callback.onDataReady(getAsset(mModel))
        }
    }

    private fun getPackage(uri: String): Bitmap? {
        val componentName = uri.replaceFirst("package://".toRegex(), "")
        val drawable = DrawableHelper.getPackageIcon(mContext, componentName)

        return if (drawable != null) {
            DrawableHelper.toBitmap(drawable, AdaptiveIcon.PATH_CIRCLE)
        } else null
    }

    private fun getDrawable(uri: String): Bitmap? {
        val drawableIdStr = uri.replaceFirst("drawable://".toRegex(), "")
        val drawableId = drawableIdStr.toInt()
        val drawable = ContextCompat.getDrawable(mContext, drawableId)

        return if (drawable != null) {
            DrawableHelper.toBitmap(drawable, Preferences.get(mContext).iconShape)
        } else null
    }

    private fun getAsset(uri: String): Bitmap? {
        try {
            mContext.assets.open(uri.replaceFirst("assets://".toRegex(), "")).use { stream ->
                return BitmapFactory.decodeStream(stream)
            }
        } catch (e: IOException) {
            LogUtil.e(Log.getStackTraceString(e))
        }

        return null
    }

    override fun cleanup() {
    }

    override fun cancel() {
    }

    override fun getDataClass(): Class<Bitmap> {
        return Bitmap::class.java
    }

    override fun getDataSource(): DataSource {
        // Because transitions do not work with local resources
        return DataSource.REMOTE
    }
}
