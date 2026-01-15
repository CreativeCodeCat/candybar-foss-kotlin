package candybar.lib.adapters

import android.animation.AnimatorInflater
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.Preset
import candybar.lib.preferences.Preferences
import candybar.lib.utils.CandyBarGlideModule
import candybar.lib.utils.views.HeaderView
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.google.android.material.card.MaterialCardView
import org.kustom.api.preset.AssetPresetFile
import org.kustom.api.preset.PresetInfoLoader
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
class PresetsAdapter(
    private val mContext: Context,
    private val mPresets: List<Preset>
) : RecyclerView.Adapter<PresetsAdapter.ViewHolder>() {

    private var wallpaperDrawable: Drawable? = null

    init {
        try {
            wallpaperDrawable = WallpaperManager.getInstance(mContext).drawable
        } catch (ignored: Exception) {
            LogUtil.e("Unable to load wallpaper. Storage permission is not granted.")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = when (viewType) {
            TYPE_HEADER -> LayoutInflater.from(mContext).inflate(
                R.layout.fragment_presets_item_header, parent, false
            )

            TYPE_CONTENT -> LayoutInflater.from(mContext).inflate(
                R.layout.fragment_presets_item_grid, parent, false
            )

            else -> throw IllegalArgumentException("Invalid view type")
        }
        return ViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val preset = mPresets[position]

        if (holder.itemViewType == TYPE_HEADER) {
            holder.name.text = preset.headerText
            holder.setType(preset.headerText ?: "")
        } else if (holder.itemViewType == TYPE_CONTENT) {
            PresetInfoLoader.create(AssetPresetFile(preset.path))
                .load(mContext) { info ->
                    holder.name.text = info.title.replace("_", "")
                }

            if (CandyBarGlideModule.isValidContextForGlide(mContext)) {
                Glide.with(mContext)
                    .asBitmap()
                    .load(AssetPresetFile(preset.path))
                    .transition(BitmapTransitionOptions.withCrossFade(300))
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(holder.image!!)
            }
        }
    }

    fun isHeader(position: Int): Boolean = mPresets[position].isHeader

    override fun getItemCount(): Int = mPresets.size

    override fun getItemViewType(position: Int): Int {
        return if (isHeader(position)) TYPE_HEADER else TYPE_CONTENT
    }

    inner class ViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var image: HeaderView? = null
        val name: TextView = itemView.findViewById(R.id.name)
        val card: MaterialCardView = itemView.findViewById(R.id.card)

        init {
            if (viewType == TYPE_HEADER) {
                if (mContext.resources.getBoolean(R.bool.use_flat_card)) {
                    card.strokeWidth = mContext.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                    card.cardElevation = 0f
                    card.useCompatPadding = false
                    val marginTop = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_top)
                    val marginLeft = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_left)
                    val marginRight = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_right)
                    val marginBottom = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_bottom)
                    (card.layoutParams as? LinearLayout.LayoutParams)?.setMargins(
                        marginLeft, marginTop, marginRight, marginBottom
                    )
                }
            } else if (viewType == TYPE_CONTENT) {
                image = itemView.findViewById(R.id.image)

                if (CandyBarApplication.getConfiguration().wallpapersGrid == CandyBarApplication.GridStyle.FLAT) {
                    card.cardElevation = 0f
                    card.maxCardElevation = 0f
                }

                if (!Preferences.get(mContext).isCardShadowEnabled) {
                    card.cardElevation = 0f
                }

                val stateListAnimator = AnimatorInflater.loadStateListAnimator(mContext, R.animator.card_lift)
                card.stateListAnimator = stateListAnimator

                wallpaperDrawable?.let {
                    itemView.findViewById<HeaderView>(R.id.wallpaper_bg)?.setImageDrawable(it)
                }

                card.setOnClickListener(this)
            }
        }

        private fun isPackageInstalled(pkgName: String): Boolean {
            return try {
                mContext.packageManager.getPackageInfo(pkgName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        private fun getRequiredApps(type: String): List<Array<String>> {
            val typeLower = type.lowercase(Locale.ROOT)
            var nameBase = ""
            var pkgBase = ""

            when (typeLower) {
                "lockscreens" -> {
                    nameBase = "KLCK"
                    pkgBase = "org.kustom.lockscreen"
                }

                "wallpapers" -> {
                    nameBase = "KLWP"
                    pkgBase = "org.kustom.wallpaper"
                }

                "widgets" -> {
                    nameBase = "KWGT"
                    pkgBase = "org.kustom.widget"
                }
            }

            val namePro = "$nameBase Pro"
            val pkgPro = "$pkgBase.pro"

            val requiredApps = mutableListOf<Array<String>>()

            if (!isPackageInstalled(pkgBase)) {
                requiredApps.add(arrayOf(nameBase, pkgBase))
            }
            if (!isPackageInstalled(pkgPro)) {
                requiredApps.add(arrayOf(namePro, pkgPro))
            }

            return requiredApps
        }

        fun setType(type: String) {
            val requiredApps = getRequiredApps(type)
            val linearLayout = itemView.findViewById<LinearLayout>(R.id.container)

            if (requiredApps.isNotEmpty()) {
                for (requiredApp in requiredApps) {
                    val item = LayoutInflater.from(mContext).inflate(
                        R.layout.fragment_presets_item_header_list, linearLayout, false
                    )
                    (item.findViewById<View>(R.id.name) as TextView).text = requiredApp[0]
                    val color = ColorHelper.getAttributeColor(mContext, android.R.attr.textColorPrimary)
                    (item.findViewById<View>(R.id.kustom_icon) as ImageView).setImageDrawable(
                        DrawableHelper.getTintedDrawable(mContext, R.drawable.ic_drawer_presets, color)
                    )
                    item.setOnClickListener {
                        try {
                            val store = Intent(
                                Intent.ACTION_VIEW, Uri.parse(
                                    "https://play.google.com/store/apps/details?id=${requiredApp[1]}"
                                )
                            )
                            mContext.startActivity(store)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                mContext, mContext.resources.getString(
                                    R.string.no_browser
                                ), Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    (item.findViewById<View>(R.id.forward_icon) as ImageView).setImageDrawable(
                        DrawableHelper.getTintedDrawable(mContext, R.drawable.ic_arrow_forward, color)
                    )
                    linearLayout.addView(item)
                }
            } else {
                card.visibility = View.GONE
            }
        }

        override fun onClick(view: View) {
            val id = view.id
            val position = bindingAdapterPosition
            if (id == R.id.card) {
                val preset = mPresets[position]
                val type = preset.path.split("/")[0]

                if (type != "komponents") {
                    var pkg = ""
                    var cls = ""

                    when (type) {
                        "lockscreens" -> {
                            pkg = "org.kustom.lockscreen"
                            cls = "org.kustom.lib.editor.LockAdvancedEditorActivity"
                        }

                        "wallpapers" -> {
                            pkg = "org.kustom.wallpaper"
                            cls = "org.kustom.lib.editor.WpAdvancedEditorActivity"
                        }

                        "widgets" -> {
                            pkg = "org.kustom.widget"
                            cls = "org.kustom.widget.picker.WidgetPicker"
                        }
                    }

                    val intent = Intent()
                    intent.component = ComponentName(pkg, cls)

                    try {
                        intent.data = Uri.Builder()
                            .scheme("kfile")
                            .authority("${mContext.packageName}.kustom.provider")
                            .appendPath(preset.path)
                            .build()
                    } catch (e: Exception) {
                        intent.data = Uri.parse("kfile://${mContext.packageName}/${preset.path}")
                    }

                    if (getRequiredApps(type).isNotEmpty()) {
                        MaterialDialog.Builder(mContext)
                            .typeface(TypefaceHelper.getMedium(mContext), TypefaceHelper.getRegular(mContext))
                            .content(R.string.presets_required_apps_not_installed)
                            .positiveText(R.string.close)
                            .show()
                    } else {
                        mContext.startActivity(intent)
                    }
                } else {
                    // TODO: Handle Komponent click
                }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1
    }
}
