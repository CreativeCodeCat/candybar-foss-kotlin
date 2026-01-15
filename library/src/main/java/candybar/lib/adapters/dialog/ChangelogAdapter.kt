package candybar.lib.adapters.dialog

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.text.HtmlCompat
import candybar.lib.R
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper

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
class ChangelogAdapter(private val context: Context, private val changelog: Array<String>) : BaseAdapter() {

    override fun getCount(): Int {
        return changelog.size
    }

    override fun getItem(position: Int): String {
        return changelog[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {
        var convertView = view
        val holder: ViewHolder
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.fragment_changelog_item_list, null)
            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        holder.changelog.text = HtmlCompat.fromHtml(changelog[position], HtmlCompat.FROM_HTML_MODE_COMPACT)
        holder.changelog.movementMethod = LinkMovementMethod.getInstance()

        return convertView!!
    }

    private inner class ViewHolder(view: View) {
        val changelog: TextView = view.findViewById(R.id.changelog)

        init {
            val color = ColorHelper.getAttributeColor(context, com.google.android.material.R.attr.colorSecondary)
            changelog.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(context, R.drawable.ic_changelog_dot, color),
                null, null, null
            )
        }
    }
}
