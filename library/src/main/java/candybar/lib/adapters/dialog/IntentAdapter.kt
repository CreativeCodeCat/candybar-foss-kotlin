package candybar.lib.adapters.dialog

import android.content.ComponentName
import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.fragments.dialog.IntentChooserFragment
import candybar.lib.helpers.DrawableHelper
import candybar.lib.items.IntentChooser
import candybar.lib.items.Request
import candybar.lib.tasks.IconRequestBuilderTask
import candybar.lib.tasks.PremiumRequestBuilderTask
import candybar.lib.utils.AsyncTaskBase
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import me.zhanghai.android.materialprogressbar.MaterialProgressBar

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
class IntentAdapter(
    private val context: Context,
    private val apps: List<IntentChooser>,
    private val type: Int
) : BaseAdapter() {

    private var asyncTask: AsyncTaskBase? = null

    override fun getCount(): Int {
        return apps.size
    }

    override fun getItem(position: Int): IntentChooser {
        return apps[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {
        var convertView = view
        val holder: ViewHolder
        if (convertView == null) {
            convertView = View.inflate(context, R.layout.fragment_intent_chooser_item_list, null)
            holder = ViewHolder(convertView)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        holder.icon.setImageDrawable(DrawableHelper.getAppIcon(context, apps[position].app))
        holder.name.text = apps[position].app.loadLabel(context.packageManager).toString()

        when (apps[position].type) {
            IntentChooser.TYPE_SUPPORTED -> {
                holder.type.setTextColor(ColorHelper.getAttributeColor(context, android.R.attr.textColorSecondary))
                holder.type.text = context.resources.getString(R.string.intent_email_supported)
            }

            IntentChooser.TYPE_RECOMMENDED -> {
                holder.type.setTextColor(ColorHelper.getAttributeColor(context, com.google.android.material.R.attr.colorSecondary))
                holder.type.text = context.resources.getString(R.string.intent_email_recommended)
            }

            else -> {
                holder.type.setTextColor(Color.parseColor("#F44336"))
                holder.type.text = context.resources.getString(R.string.intent_email_not_supported)
            }
        }

        holder.container.setOnClickListener {
            val app = apps[position].app.activityInfo
            if (apps[position].type == IntentChooser.TYPE_RECOMMENDED ||
                apps[position].type == IntentChooser.TYPE_SUPPORTED
            ) {
                if (asyncTask != null) return@setOnClickListener

                holder.icon.visibility = View.GONE
                holder.progressBar.visibility = View.VISIBLE

                if (CandyBarApplication.sRequestProperty == null) {
                    CandyBarApplication.sRequestProperty = Request.Property(null, null, null)
                }
                CandyBarApplication.sRequestProperty!!.componentName =
                    ComponentName(app.applicationInfo.packageName, app.name)

                if (type == IntentChooserFragment.ICON_REQUEST) {
                    asyncTask = IconRequestBuilderTask(context) {
                        asyncTask = null
                        val fm = (context as? AppCompatActivity)?.supportFragmentManager
                        if (fm != null) {
                            val dialog = fm.findFragmentByTag(IntentChooserFragment.TAG) as? DialogFragment
                            dialog?.dismiss()
                        }
                    }.executeOnThreadPool()
                } else if (type == IntentChooserFragment.REBUILD_ICON_REQUEST) {
                    asyncTask = PremiumRequestBuilderTask(context) {
                        asyncTask = null
                        val fm = (context as? AppCompatActivity)?.supportFragmentManager
                        if (fm != null) {
                            val dialog = fm.findFragmentByTag(IntentChooserFragment.TAG) as? DialogFragment
                            dialog?.dismiss()
                        }
                    }.executeOnThreadPool()
                } else {
                    LogUtil.e("Intent chooser type unknown: $type")
                }
                return@setOnClickListener
            }

            Toast.makeText(
                context, R.string.intent_email_not_supported_message,
                Toast.LENGTH_LONG
            ).show()
        }

        return convertView!!
    }

    val isAsyncTaskRunning: Boolean
        get() = asyncTask != null

    private class ViewHolder(view: View) {
        val name: TextView = view.findViewById(R.id.name)
        val type: TextView = view.findViewById(R.id.type)
        val icon: ImageView = view.findViewById(R.id.icon)
        val container: LinearLayout = view.findViewById(R.id.container)
        val progressBar: MaterialProgressBar = view.findViewById(R.id.progress)
    }
}
