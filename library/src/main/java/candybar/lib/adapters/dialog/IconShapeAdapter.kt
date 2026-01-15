package candybar.lib.adapters.dialog

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import candybar.lib.R
import candybar.lib.fragments.dialog.IconShapeChooserFragment
import candybar.lib.items.IconShape

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
class IconShapeAdapter(
    private val context: Context,
    private val shapes: List<IconShape>,
    private val selectedIndex: Int
) : BaseAdapter() {

    private val holders = ArrayList<ViewHolder>()

    override fun getCount(): Int {
        return shapes.size
    }

    override fun getItem(position: Int): IconShape {
        return shapes[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {
        var convertView = view
        val holder: ViewHolder

        if (convertView == null) {
            convertView = View.inflate(context, R.layout.fragment_inapp_dialog_item_list, null)
            holder = ViewHolder(convertView)
            convertView.tag = holder
            holders.add(holder)
        } else {
            holder = convertView.tag as ViewHolder
        }

        holder.radio.isChecked = selectedIndex == position
        holder.name.text = shapes[position].name

        holder.container.setOnClickListener {
            for (aHolder in holders) {
                if (aHolder != holder) aHolder.radio.isChecked = false
            }
            holder.radio.isChecked = true

            val fm = (context as AppCompatActivity).supportFragmentManager
            val fragment = fm.findFragmentByTag(IconShapeChooserFragment.TAG)

            if (fragment is IconShapeChooserFragment) {
                fragment.setShape(shapes[position].shape)
            }
        }

        return convertView!!
    }

    private class ViewHolder(view: View) {
        val radio: RadioButton = view.findViewById(R.id.radio)
        val name: TextView = view.findViewById(R.id.name)
        val container: LinearLayout = view.findViewById(R.id.container)
    }
}
