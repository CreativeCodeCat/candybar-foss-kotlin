package candybar.lib.utils

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory

final class CommonModelLoaderFactory(private val mContext: Context) : ModelLoaderFactory<String, Bitmap> {

    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, Bitmap> {
        return CommonModelLoader(mContext)
    }

    override fun teardown() {
    }
}
