package candybar.lib.adapters.dialog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import candybar.lib.R
import candybar.lib.items.License

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
class LicensesAdapter(private val context: Context, private val licenses: List<License>) : BaseAdapter() {

    override fun getCount(): Int {
        return licenses.size
    }

    override fun getItem(position: Int): License {
        return licenses[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {
        var convertView = view
        val holder: ViewHolder
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.fragment_licenses_item_list, null)
            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val license = licenses[position]
        holder.title.text = license.name
        holder.body.text = license.licenseText

        return convertView!!
    }

    private class ViewHolder(view: View) {
        val title: TextView = view.findViewById(R.id.title)
        val body: TextView = view.findViewById(R.id.body)
    }
}
