package candybar.lib.utils

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.signature.ObjectKey

final class CommonModelLoader(private val mContext: Context) : ModelLoader<String, Bitmap> {

    override fun handles(model: String): Boolean {
        return model.startsWith("drawable://") || model.startsWith("package://") || model.startsWith("assets://")
    }

    override fun buildLoadData(model: String, width: Int, height: Int, options: Options): LoadData<Bitmap> {
        return LoadData(ObjectKey(model), CommonDataFetcher(mContext, model))
    }
}
