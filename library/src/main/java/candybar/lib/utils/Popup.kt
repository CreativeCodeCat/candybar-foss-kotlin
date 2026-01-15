package candybar.lib.utils

import android.app.Activity
import android.content.Context
import android.graphics.PorterDuff
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import candybar.lib.R
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.PopupItem
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

class Popup private constructor(builder: Builder) {

    private val mPopupWindow: ListPopupWindow = ListPopupWindow(builder.context)
    private val mAdapter: PopupAdapter = PopupAdapter(builder.context, builder.items)

    init {
        val width = getMeasuredWidth(builder.context)
        mPopupWindow.setContentWidth(width)
        val drawable = mPopupWindow.background
        drawable?.setColorFilter(
            ColorHelper.getAttributeColor(
                builder.context, R.attr.cb_cardBackground
            ), PorterDuff.Mode.SRC_IN
        )

        mPopupWindow.anchorView = builder.to
        mPopupWindow.setAdapter(mAdapter)
        mPopupWindow.setOnItemClickListener { _, _, i, _ ->
            if (builder.callback != null) {
                builder.callback!!.onClick(this, i)
                return@setOnItemClickListener
            }

            mPopupWindow.dismiss()
        }
    }

    fun show() {
        if (mAdapter.count == 0) {
            LogUtil.e("Popup size = 0, show() ignored")
            return
        }
        mPopupWindow.show()
    }

    fun dismiss() {
        if (mPopupWindow.isShowing)
            mPopupWindow.dismiss()
    }

    val items: List<PopupItem>
        get() = mAdapter.getItems()

    fun updateItem(position: Int, item: PopupItem) {
        mAdapter.updateItem(position, item)
    }

    fun removeItem(position: Int) {
        mAdapter.removeItem(position)
    }

    private fun getMeasuredWidth(context: Context): Int {
        val metrics = DisplayMetrics()
        (context as Activity).windowManager.defaultDisplay.getMetrics(metrics)

        val maxWidth = context.resources.getDimensionPixelSize(R.dimen.popup_max_width)
        val minWidth = context.resources.getDimensionPixelSize(R.dimen.popup_min_width)
        var longestText = ""
        for (item in mAdapter.getItems()) {
            if (item.title.length > longestText.length)
                longestText = item.title
        }

        val padding = context.resources.getDimensionPixelSize(R.dimen.content_margin)
        val iconSize = context.resources.getDimensionPixelSize(R.dimen.icon_size_small)
        val textView = TextView(context)
        textView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.typeface = TypefaceHelper.getRegular(context)
        textView.setTextSize(
            TypedValue.COMPLEX_UNIT_PX, context.resources
                .getDimension(R.dimen.text_content_subtitle)
        )
        textView.setPadding(padding + iconSize + padding, 0, padding, 0)
        textView.text = longestText

        val widthMeasureSpec =
            View.MeasureSpec.makeMeasureSpec(metrics.widthPixels, View.MeasureSpec.AT_MOST)
        val heightMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        textView.measure(widthMeasureSpec, heightMeasureSpec)

        val measuredWidth = textView.measuredWidth + padding
        if (measuredWidth <= minWidth) {
            return minWidth
        }

        if (measuredWidth <= maxWidth) {
            return measuredWidth
        }
        return maxWidth
    }

    class Builder(val context: Context) {

        var callback: Callback? = null
            private set
        var to: View? = null
            private set
        var items: List<PopupItem> = ArrayList()
            private set

        fun to(to: View?): Builder {
            this.to = to
            return this
        }

        fun list(items: List<PopupItem>): Builder {
            this.items = items
            return this
        }

        fun callback(callback: Callback?): Builder {
            this.callback = callback
            return this
        }

        fun build(): Popup {
            return Popup(this)
        }

        fun show() {
            build().show()
        }
    }

    internal class PopupAdapter(
        private val mContext: Context,
        items: List<PopupItem>
    ) : BaseAdapter() {

        private val mItems: MutableList<PopupItem> = items.toMutableList()

        override fun getCount(): Int {
            return mItems.size
        }

        override fun getItem(position: Int): PopupItem {
            return mItems[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, view: View?, viewGroup: ViewGroup): View {
            var v = view
            val holder: ViewHolder
            if (v == null) {
                v = View.inflate(mContext, R.layout.popup_item_list, null)
                holder = ViewHolder(v!!)
                v.tag = holder
            } else {
                holder = v.tag as ViewHolder
            }

            val item = mItems[position]
            holder.checkBox.visibility = View.GONE
            if (item.isShowCheckbox) {
                holder.checkBox.isChecked = item.checkboxValue
                holder.checkBox.visibility = View.VISIBLE
            }

            var color = ColorHelper.getAttributeColor(mContext, android.R.attr.textColorPrimary)
            if (item.isSelected) {
                color =
                    ColorHelper.getAttributeColor(mContext, com.google.android.material.R.attr.colorSecondary)
            }

            if (item.icon != 0) {
                val drawable =
                    DrawableHelper.getTintedDrawable(mContext, item.icon, color)
                holder.title.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
            }

            holder.title.text = item.title
            holder.title.setTextColor(color)
            return v!!
        }

        internal class ViewHolder(view: View) {
            val checkBox: CheckBox = view.findViewById(R.id.checkbox)
            val title: TextView = view.findViewById(R.id.title)
        }

        fun getItems(): List<PopupItem> {
            return mItems
        }

        fun updateItem(position: Int, item: PopupItem) {
            mItems[position] = item
            notifyDataSetChanged()
        }

        fun removeItem(position: Int) {
            mItems.removeAt(position)
            notifyDataSetChanged()
        }
    }

    fun interface Callback {
        fun onClick(popup: Popup, position: Int)
    }
}
