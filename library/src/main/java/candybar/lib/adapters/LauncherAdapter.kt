package candybar.lib.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.helpers.LauncherHelper
import candybar.lib.items.Icon
import candybar.lib.preferences.Preferences
import candybar.lib.utils.CandyBarGlideModule
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions

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
class LauncherAdapter(
    private val mContext: Context,
    private val mLaunchers: List<Icon>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(mContext)
        return when (viewType) {
            TYPE_HEADER -> {
                val view = layoutInflater.inflate(R.layout.fragment_apply_item_header, parent, false)
                ViewHolder(view, viewType)
            }

            TYPE_CONTENT -> {
                val view = layoutInflater.inflate(R.layout.fragment_apply_item_list, parent, false)
                ViewHolder(view, viewType)
            }

            TYPE_FOOTER -> {
                val view = layoutInflater.inflate(R.layout.fragment_apply_item_footer, parent, false)
                FooterViewHolder(view)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                if (holder.itemViewType == TYPE_HEADER) {
                    holder.name?.text = mLaunchers[position].title
                } else if (holder.itemViewType == TYPE_CONTENT) {
                    holder.name?.text = mLaunchers[position].title
                    if (CandyBarGlideModule.isValidContextForGlide(mContext)) {
                        Glide.with(mContext)
                            .asBitmap()
                            .load("drawable://" + mLaunchers[position].res)
                            .transition(BitmapTransitionOptions.withCrossFade(300))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .into(holder.icon!!)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = mLaunchers.size + 1

    override fun getItemViewType(position: Int): Int {
        if (position == firstHeaderPosition || position == lastHeaderPosition || position == middleHeaderPosition) {
            return TYPE_HEADER
        }
        return if (position == itemCount - 1) TYPE_FOOTER else TYPE_CONTENT
    }

    inner class ViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        var name: TextView? = null
        var icon: ImageView? = null

        init {
            if (viewType == TYPE_HEADER) {
                name = itemView.findViewById(R.id.name)
            } else if (viewType == TYPE_CONTENT) {
                icon = itemView.findViewById(R.id.icon)
                name = itemView.findViewById(R.id.name)
                val container = itemView.findViewById<LinearLayout>(R.id.container)
                container.setOnClickListener(this)
            }
        }

        @SuppressLint("StringFormatInvalid")
        override fun onClick(view: View) {
            val id = view.id
            val position = bindingAdapterPosition
            if (id == R.id.container) {
                if (position < 0 || position >= mLaunchers.size) return
                try {
                    LauncherHelper.getLauncher(mLaunchers[position].packageName).apply(mContext)
                } catch (e: Exception) {
                    Toast.makeText(
                        mContext,
                        mContext.resources.getString(
                            R.string.apply_launch_failed, mLaunchers[position].title
                        ),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            if (!Preferences.get(mContext).isCardShadowEnabled) {
                val shadow = itemView.findViewById<View>(R.id.shadow)
                shadow?.visibility = View.GONE
            }
        }
    }

    val firstHeaderPosition: Int
        get() = mLaunchers.indexOf(
            Icon(mContext.resources.getString(R.string.apply_installed), -1, null)
        )

    val middleHeaderPosition: Int
        get() = mLaunchers.indexOf(
            Icon(mContext.resources.getString(R.string.apply_installed_launchers), -3, null)
        )

    val lastHeaderPosition: Int
        get() = mLaunchers.indexOf(
            Icon(mContext.resources.getString(R.string.apply_supported), -2, null)
        )

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1
        private const val TYPE_FOOTER = 2
    }
}
