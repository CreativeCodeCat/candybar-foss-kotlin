package candybar.lib.adapters.dialog

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import candybar.lib.R
import candybar.lib.items.Credit
import candybar.lib.utils.CandyBarGlideModule
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil

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
class CreditsAdapter(private val context: Context, private val credits: List<Credit>) : BaseAdapter() {

    private val placeholder: Drawable?

    init {
        val color = ColorHelper.getAttributeColor(context, android.R.attr.textColorSecondary)
        placeholder = DrawableHelper.getTintedDrawable(
            context, R.drawable.ic_toolbar_default_profile, color
        )
    }

    override fun getCount(): Int {
        return credits.size
    }

    override fun getItem(position: Int): Credit {
        return credits[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {
        var convertView = view
        val holder: ViewHolder
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.fragment_credits_item_list, null)
            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val credit = credits[position]
        holder.title.text = credit.name
        holder.subtitle.text = credit.contribution
        holder.container.setOnClickListener {
            val link = credit.link
            if (URLUtil.isValidUrl(link)) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                } catch (e: ActivityNotFoundException) {
                    LogUtil.e(Log.getStackTraceString(e))
                }
            }
        }

        if (credit.contribution.isEmpty()) {
            holder.subtitle.visibility = View.GONE
        } else {
            holder.subtitle.visibility = View.VISIBLE
        }

        if (CandyBarGlideModule.isValidContextForGlide(context)) {
            Glide.with(context)
                .load(credit.image)
                .override(144)
                .optionalCenterInside()
                .circleCrop()
                .placeholder(placeholder)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(holder.image)
        }

        return convertView!!
    }

    private inner class ViewHolder(view: View) {
        val container: LinearLayout = view.findViewById(R.id.container)
        val title: TextView = view.findViewById(R.id.title)
        val subtitle: TextView = view.findViewById(R.id.subtitle)
        val image: ImageView = view.findViewById(R.id.image)

        init {
            val color = ColorHelper.getAttributeColor(context, android.R.attr.textColorSecondary)
            ViewCompat.setBackground(
                image, DrawableHelper.getTintedDrawable(
                    context, R.drawable.ic_toolbar_circle, ColorHelper.setColorAlpha(color, 0.4f)
                )
            )
        }
    }
}
