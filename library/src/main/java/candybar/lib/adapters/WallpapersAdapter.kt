package candybar.lib.adapters

import android.animation.AnimatorInflater
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.activities.CandyBarWallpaperActivity
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.ViewHelper
import candybar.lib.items.PopupItem
import candybar.lib.items.Wallpaper
import candybar.lib.preferences.Preferences
import candybar.lib.tasks.WallpaperApplyTask
import candybar.lib.utils.CandyBarGlideModule
import candybar.lib.utils.Extras
import candybar.lib.utils.ImageConfig
import candybar.lib.utils.Popup
import candybar.lib.utils.WallpaperDownloader
import candybar.lib.utils.views.HeaderView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.card.MaterialCardView
import com.kogitune.activitytransition.ActivityTransitionLauncher
import java.util.Locale

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
class WallpapersAdapter(
    private val mContext: Context,
    private val mWallpapers: MutableList<Wallpaper>
) : RecyclerView.Adapter<WallpapersAdapter.ViewHolder>() {

    private val mWallpapersAll: List<Wallpaper> = ArrayList(mWallpapers)
    private val mIsShowName: Boolean = mContext.resources.getBoolean(R.bool.wallpaper_show_name_author)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (mIsShowName) {
            R.layout.fragment_wallpapers_item_grid
        } else {
            R.layout.fragment_wallpapers_item_grid_alt
        }
        val view = LayoutInflater.from(mContext).inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wallpaper = mWallpapers[position]
        if (mIsShowName) {
            holder.name?.text = wallpaper.name
            holder.author?.text = wallpaper.author
        }

        if (CandyBarGlideModule.isValidContextForGlide(mContext)) {
            Glide.with(mContext)
                .asBitmap()
                .load(wallpaper.thumbUrl)
                .override(ImageConfig.getThumbnailSize())
                .transition(BitmapTransitionOptions.withCrossFade(300))
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any?,
                        target: Target<Bitmap>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.thumbnailBitmap = resource
                        return false
                    }
                })
                .into(holder.image)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun search(string: String) {
        val query = string.lowercase(Locale.getDefault()).trim { it <= ' ' }
        mWallpapers.clear()
        if (query.isEmpty()) {
            mWallpapers.addAll(mWallpapersAll)
        } else {
            for (wallpaper in mWallpapersAll) {
                if (wallpaper.name.orEmpty().lowercase(Locale.getDefault()).contains(query)) {
                    mWallpapers.add(wallpaper)
                }
            }
        }
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return mWallpapers.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        val image: HeaderView = itemView.findViewById(R.id.image)
        var name: TextView? = null
        var author: TextView? = null
        var thumbnailBitmap: Bitmap? = null

        init {
            val viewStyle = mContext.resources.getString(R.string.wallpaper_grid_preview_style)
            val ratio = ViewHelper.getWallpaperViewRatio(viewStyle)
            image.setRatio(ratio.x, ratio.y)

            val card: MaterialCardView = itemView.findViewById(R.id.card)
            if (CandyBarApplication.getConfiguration().wallpapersGrid == CandyBarApplication.GridStyle.FLAT) {
                card.cardElevation = 0f
                card.maxCardElevation = 0f
            }

            if (!Preferences.get(mContext).isCardShadowEnabled) {
                card.cardElevation = 0f
            }

            val stateListAnimator = AnimatorInflater.loadStateListAnimator(mContext, R.animator.card_lift)
            card.stateListAnimator = stateListAnimator

            if (mIsShowName) {
                name = itemView.findViewById(R.id.name)
                author = itemView.findViewById(R.id.author)
            }

            card.setOnClickListener(this)
            card.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            val id = view.id
            val position = bindingAdapterPosition
            if (id == R.id.card) {
                if (sIsClickable) {
                    sIsClickable = false
                    try {
                        val intent = Intent(mContext, CandyBarWallpaperActivity::class.java)
                        intent.putExtra(Extras.EXTRA_URL, mWallpapers[position].url)
                        ActivityTransitionLauncher.`with`(mContext as AppCompatActivity)
                            .from(image, Extras.EXTRA_IMAGE)
                            .image(thumbnailBitmap)
                            .launch(intent)
                    } catch (e: Exception) {
                        sIsClickable = true
                    }
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            val id = view.id
            val position = bindingAdapterPosition
            if (id == R.id.card) {
                if (position !in mWallpapers.indices) {
                    return false
                }

                val popup = Popup.Builder(mContext)
                    .to(name ?: view)
                    .list(PopupItem.getApplyItems(mContext))
                    .callback { applyPopup, i ->
                        val item = applyPopup.items[i]
                        when (item.type) {
                            PopupItem.Type.WALLPAPER_CROP -> {
                                Preferences.get(mContext).isCropWallpaper = !item.checkboxValue
                                item.checkboxValue = Preferences.get(mContext).isCropWallpaper
                                applyPopup.updateItem(i, item)
                                return@callback
                            }

                            PopupItem.Type.DOWNLOAD -> {
                                WallpaperDownloader.prepare(mContext)
                                    .wallpaper(mWallpapers[position])
                                    .start()
                            }

                            else -> {
                                val task = WallpaperApplyTask(mContext, mWallpapers[position])
                                when (item.type) {
                                    PopupItem.Type.LOCKSCREEN -> task.to(WallpaperApplyTask.Apply.LOCKSCREEN)
                                    PopupItem.Type.HOMESCREEN -> task.to(WallpaperApplyTask.Apply.HOMESCREEN)
                                    PopupItem.Type.HOMESCREEN_LOCKSCREEN -> task.to(WallpaperApplyTask.Apply.HOMESCREEN_LOCKSCREEN)
                                    else -> {}
                                }
                                task.executeOnThreadPool()
                            }
                        }
                        applyPopup.dismiss()
                    }
                    .build()
                popup.show()
                return true
            }
            return false
        }
    }

    companion object {
        @JvmStatic
        var sIsClickable: Boolean = true
    }
}
