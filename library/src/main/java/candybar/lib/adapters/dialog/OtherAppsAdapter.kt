package candybar.lib.adapters.dialog

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.DrawableHelper
import candybar.lib.utils.CandyBarGlideModule
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
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
class OtherAppsAdapter(
    private val context: Context,
    private val otherApps: List<CandyBarApplication.OtherApp>
) : BaseAdapter() {

    override fun getCount(): Int {
        return otherApps.size
    }

    override fun getItem(position: Int): CandyBarApplication.OtherApp {
        return otherApps[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {
        var convertView = view
        val holder: ViewHolder
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.fragment_other_apps_item_list, null)
            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val otherApp = otherApps[position]
        var uri = otherApp.icon
        if (!URLUtil.isValidUrl(uri)) {
            uri = "drawable://" + DrawableHelper.getDrawableId(uri)
        }

        if (CandyBarGlideModule.isValidContextForGlide(context)) {
            Glide.with(context)
                .load(uri)
                .transition(DrawableTransitionOptions.withCrossFade(300))
                .skipMemoryCache(true)
                .diskCacheStrategy(
                    if (uri.contains("drawable://")) DiskCacheStrategy.NONE
                    else DiskCacheStrategy.RESOURCE
                )
                .into(holder.image)
        }

        holder.title.text = otherApp.title

        if (otherApp.description.isEmpty()) {
            holder.desc.visibility = View.GONE
        } else {
            holder.desc.text = otherApp.description
            holder.desc.visibility = View.VISIBLE
        }

        holder.container.setOnClickListener {
            if (!URLUtil.isValidUrl(otherApp.url)) return@setOnClickListener

            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(otherApp.url)))
            } catch (e: ActivityNotFoundException) {
                LogUtil.e(Log.getStackTraceString(e))
            }
        }
        return convertView!!
    }

    private class ViewHolder(view: View) {
        val container: LinearLayout = view.findViewById(R.id.container)
        val image: ImageView = view.findViewById(R.id.image)
        val title: TextView = view.findViewById(R.id.title)
        val desc: TextView = view.findViewById(R.id.desc)
    }
}
