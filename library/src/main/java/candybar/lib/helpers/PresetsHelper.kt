package candybar.lib.helpers

import android.content.Context
import android.util.Log
import com.danimahardhika.android.helpers.core.utils.LogUtil

object PresetsHelper {

    @JvmStatic
    fun getPresetsCount(context: Context): Int {
        return try {
            val assets = context.assets
            val komponents = assets.list("komponents") ?: emptyArray()
            val lockscreens = assets.list("lockscreens") ?: emptyArray()
            val wallpapers = assets.list("wallpapers") ?: emptyArray()
            val widgets = assets.list("widgets") ?: emptyArray()

            komponents.size + lockscreens.size + wallpapers.size + widgets.size
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
            0
        }
    }
}
