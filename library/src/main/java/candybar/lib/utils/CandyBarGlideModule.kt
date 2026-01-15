package candybar.lib.utils

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule

@GlideModule
final class CandyBarGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.prepend(String::class.java, Bitmap::class.java, CommonModelLoaderFactory(context))
    }

    companion object {
        // Kindly provided by @farhan on GitHub
        // https://github.com/bumptech/glide/issues/1484#issuecomment-365625087
        @JvmStatic
        fun isValidContextForGlide(context: Context?): Boolean {
            if (context == null) {
                return false
            }
            if (context is Activity) {
                return !context.isDestroyed && !context.isFinishing
            }
            return true
        }
    }
}
