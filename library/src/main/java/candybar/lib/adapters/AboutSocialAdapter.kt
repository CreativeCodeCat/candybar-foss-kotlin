package candybar.lib.adapters

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.UrlHelper
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
class AboutSocialAdapter(private val context: Context, private val urls: Array<String>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layout = if (CandyBarApplication.getConfiguration().socialIconColor == CandyBarApplication.IconColor.ACCENT) {
            R.layout.fragment_about_item_social_accent
        } else {
            R.layout.fragment_about_item_social
        }
        val view = LayoutInflater.from(context).inflate(layout, parent, false)
        return SocialViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val socialViewHolder = holder as SocialViewHolder
        val type = UrlHelper.getType(urls[position])
        val drawable = UrlHelper.getSocialIcon(context, type)

        socialViewHolder.itemView.contentDescription = context.getString(R.string.about_item_social_content_description) + type.toString()

        if (drawable != null && type != UrlHelper.Type.INVALID) {
            socialViewHolder.image.setImageDrawable(drawable)
            socialViewHolder.image.visibility = View.VISIBLE
        } else {
            socialViewHolder.image.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = urls.size

    private inner class SocialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        val image: ImageView = itemView.findViewById(R.id.image)

        init {
            image.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val id = view.id
            val position = bindingAdapterPosition
            if (position < 0 || position >= urls.size) return

            CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                "click",
                hashMapOf(
                    "section" to "about",
                    "action" to "open_social",
                    "url" to urls[position]
                )
            )

            if (id == R.id.image) {
                val type = UrlHelper.getType(urls[position])
                if (type == UrlHelper.Type.INVALID) return

                if (type == UrlHelper.Type.EMAIL) {
                    try {
                        val emailIntent = Intent(
                            Intent.ACTION_SENDTO, Uri.fromParts(
                                "mailto", urls[position], null
                            )
                        )
                        emailIntent.putExtra(
                            Intent.EXTRA_SUBJECT,
                            context.resources.getString(R.string.app_name)
                        )
                        context.startActivity(
                            Intent.createChooser(
                                emailIntent,
                                context.resources.getString(R.string.app_client)
                            )
                        )
                    } catch (e: ActivityNotFoundException) {
                        LogUtil.e(Log.getStackTraceString(e))
                    }
                    return
                }

                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urls[position])))
                } catch (e: ActivityNotFoundException) {
                    LogUtil.e(Log.getStackTraceString(e))
                }
            }
        }
    }
}
