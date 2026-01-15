package candybar.lib.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.Request
import candybar.lib.preferences.Preferences
import candybar.lib.utils.CandyBarGlideModule
import candybar.lib.utils.listeners.RequestListener
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import com.google.android.material.card.MaterialCardView

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
class RequestAdapter(
    private val mContext: Context,
    private val mRequests: List<Request>,
    spanCount: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mSelectedItems = SparseBooleanArray()
    private val mTextColorSecondary: Int = ColorHelper.getAttributeColor(mContext, android.R.attr.textColorSecondary)
    private val mTextColorAccent: Int = ColorHelper.getAttributeColor(mContext, com.google.android.material.R.attr.colorSecondary)
    private var mSelectedAll = false

    private val mShowShadow: Boolean = spanCount == 1
    private val mShowPremiumRequest: Boolean = Preferences.get(mContext).isPremiumRequestEnabled
    private val mShowRegularRequestLimit: Boolean = Preferences.get(mContext).isRegularRequestLimit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(mContext).inflate(R.layout.fragment_request_item_header, parent, false)
                getLayoutParams(view)?.isFullSpan = false
                HeaderViewHolder(view)
            }

            TYPE_CONTENT -> {
                val view = LayoutInflater.from(mContext).inflate(R.layout.fragment_request_item_list, parent, false)
                getLayoutParams(view)?.isFullSpan = false
                ContentViewHolder(view)
            }

            TYPE_FOOTER -> {
                val view = LayoutInflater.from(mContext).inflate(R.layout.fragment_request_item_footer, parent, false)
                getLayoutParams(view)?.isFullSpan = true
                FooterViewHolder(view)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ContentViewHolder) {
            holder.content.setTextColor(mTextColorSecondary)
            if (mShowShadow) {
                holder.divider.visibility = View.VISIBLE
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is HeaderViewHolder -> {
                val preferences = Preferences.get(mContext)
                if (preferences.isPremiumRequestEnabled) {
                    if (preferences.isPremiumRequest) {
                        holder.button.visibility = View.GONE
                        holder.premContent.visibility = View.GONE
                        holder.premContainer.visibility = View.VISIBLE

                        val total = preferences.premiumRequestTotal
                        val available = preferences.premiumRequestCount

                        holder.premTotal.text = mContext.resources.getString(R.string.premium_request_count, total)
                        holder.premAvailable.text = mContext.resources.getString(R.string.premium_request_available, available)
                        holder.premUsed.text = mContext.resources.getString(R.string.premium_request_used, total - available)

                        holder.premProgress.max = total
                        holder.premProgress.progress = available
                    } else {
                        holder.button.visibility = View.VISIBLE
                        holder.premContent.visibility = View.VISIBLE
                        holder.premContainer.visibility = View.GONE
                    }
                } else {
                    holder.premWholeContainer.visibility = View.GONE
                }

                if (mShowRegularRequestLimit) {
                    val total = mContext.resources.getInteger(R.integer.icon_request_limit)
                    val used = preferences.regularRequestUsed
                    val available = total - used

                    holder.regTotal.text = mContext.resources.getString(R.string.regular_request_count, total)
                    holder.regAvailable.text = mContext.resources.getString(R.string.regular_request_available, available)
                    holder.regUsed.text = mContext.resources.getString(R.string.regular_request_used, used)

                    holder.regProgress.max = total
                    holder.regProgress.progress = available
                } else {
                    holder.regWholeContainer.visibility = View.GONE
                }

                if (!mContext.resources.getBoolean(R.bool.enable_icon_request)) {
                    holder.regWholeContainer.visibility = View.GONE
                }
            }

            is ContentViewHolder -> {
                var finalPosition = position
                if (mShowPremiumRequest || mShowRegularRequestLimit) finalPosition -= 1
                val request = mRequests[finalPosition]

                if (CandyBarGlideModule.isValidContextForGlide(mContext)) {
                    Glide.with(mContext)
                        .load("package://" + request.activity)
                        .override(272)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(holder.icon)
                }

                holder.title.text = request.name
                holder.infoIcon.visibility = View.GONE

                when {
                    request.isRequested -> {
                        holder.content.setTextColor(mTextColorAccent)
                        holder.content.text = mContext.resources.getString(R.string.request_already_requested)
                    }

                    request.isAvailableForRequest -> {
                        holder.content.text = mContext.resources.getString(R.string.request_not_requested)
                    }

                    else -> {
                        holder.content.text = mContext.resources.getString(R.string.request_not_available)
                    }
                }

                if (request.isRequested && !mContext.resources.getBoolean(R.bool.enable_icon_request_multiple)) {
                    holder.content.alpha = 1f
                    holder.title.alpha = 1f
                    holder.icon.alpha = 1f
                    holder.checkbox.isEnabled = false
                } else if (!request.isAvailableForRequest) {
                    holder.content.alpha = 0.5f
                    holder.title.alpha = 0.5f
                    holder.icon.alpha = 0.5f
                    holder.checkbox.isEnabled = false
                } else {
                    holder.content.alpha = 1f
                    holder.title.alpha = 1f
                    holder.icon.alpha = 1f
                    holder.checkbox.isEnabled = true
                }

                if (request.infoText.isNotEmpty()) {
                    holder.infoIcon.visibility = View.VISIBLE
                    holder.infoIcon.setImageDrawable(AppCompatResources.getDrawable(mContext, R.drawable.ic_drawer_about))
                    holder.infoIcon.setColorFilter(mTextColorSecondary)
                    holder.infoIcon.setOnClickListener {
                        MaterialDialog.Builder(mContext)
                            .typeface(TypefaceHelper.getMedium(mContext), TypefaceHelper.getRegular(mContext))
                            .title(request.name)
                            .content(request.infoText)
                            .positiveText(android.R.string.yes)
                            .show()
                    }
                }

                holder.checkbox.isChecked = mSelectedItems.get(finalPosition, false)

                if (finalPosition == mRequests.size - 1 && mShowShadow) {
                    holder.divider.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemCount(): Int {
        var count = mRequests.size
        if (mShowShadow) count += 1
        if (mShowPremiumRequest || mShowRegularRequestLimit) count += 1
        return count
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0 && (mShowPremiumRequest || mShowRegularRequestLimit)) return TYPE_HEADER
        return if (position == itemCount - 1 && mShowShadow) TYPE_FOOTER else TYPE_CONTENT
    }

    private inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val premContent: TextView = itemView.findViewById(R.id.premium_request_content)
        val premTotal: TextView = itemView.findViewById(R.id.premium_request_total)
        val premAvailable: TextView = itemView.findViewById(R.id.premium_request_available)
        val premUsed: TextView = itemView.findViewById(R.id.premium_request_used)
        val button: Button = itemView.findViewById(R.id.buy)
        val premContainer: LinearLayout = itemView.findViewById(R.id.premium_request)
        val premWholeContainer: LinearLayout = itemView.findViewById(R.id.premium_request_container)
        val premProgress: ProgressBar = itemView.findViewById(R.id.premium_request_progress)

        val regTotal: TextView = itemView.findViewById(R.id.regular_request_total)
        val regAvailable: TextView = itemView.findViewById(R.id.regular_request_available)
        val regUsed: TextView = itemView.findViewById(R.id.regular_request_used)
        val regWholeContainer: LinearLayout = itemView.findViewById(R.id.regular_request_container)
        val regProgress: ProgressBar = itemView.findViewById(R.id.regular_request_progress)

        init {
            val premTitle: TextView = itemView.findViewById(R.id.premium_request_title)
            val regTitle: TextView = itemView.findViewById(R.id.regular_request_title)
            val regContent: TextView = itemView.findViewById(R.id.regular_request_content)
            val regContainer: LinearLayout = itemView.findViewById(R.id.regular_request)

            val card: MaterialCardView? = itemView.findViewById(R.id.card)
            if (CandyBarApplication.getConfiguration().requestStyle == CandyBarApplication.Style.PORTRAIT_FLAT_LANDSCAPE_FLAT &&
                card != null
            ) {
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.let { params ->
                    card.radius = 0f
                    card.useCompatPadding = false
                    val margin = mContext.resources.getDimensionPixelSize(R.dimen.card_margin)
                    params.setMargins(0, 0, margin, margin)
                    params.marginEnd = margin
                }
            }

            if (mContext.resources.getBoolean(R.bool.use_flat_card) && card != null) {
                card.strokeWidth = mContext.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                card.cardElevation = 0f
                card.useCompatPadding = false
                val marginTop = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_top)
                val marginLeft = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_left)
                val marginRight = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_right)
                val marginBottom = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_bottom)
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.setMargins(
                    marginLeft, marginTop, marginRight, marginBottom
                )
            }

            if (!Preferences.get(mContext).isCardShadowEnabled && card != null) {
                card.cardElevation = 0f
            }

            val padding = mContext.resources.getDimensionPixelSize(R.dimen.content_margin) +
                    mContext.resources.getDimensionPixelSize(R.dimen.icon_size_small)
            premContent.setPadding(padding, 0, 0, 0)
            premContainer.setPadding(padding, 0, padding, 0)

            regContent.setPadding(padding, 0, 0, 0)
            regContainer.setPadding(padding, 0, padding, 0)

            val color = ColorHelper.getAttributeColor(mContext, android.R.attr.textColorPrimary)
            premTitle.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(mContext, R.drawable.ic_toolbar_premium_request, color),
                null, null, null
            )

            regTitle.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(mContext, R.drawable.ic_toolbar_icon_request, color),
                null, null, null
            )

            val primary = ColorHelper.getAttributeColor(mContext, androidx.appcompat.R.attr.colorPrimary)
            val accent = ColorHelper.getAttributeColor(mContext, com.google.android.material.R.attr.colorSecondary)
            button.setTextColor(ColorHelper.getTitleTextColor(primary))

            premProgress.progressDrawable.setColorFilter(accent, PorterDuff.Mode.SRC_IN)
            regProgress.progressDrawable.setColorFilter(accent, PorterDuff.Mode.SRC_IN)

            button.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view.id == R.id.buy) {
                (mContext as? RequestListener)?.let {
                    // Trigger purchase logic (handled in Fragment/Activity)
                }
            }
        }
    }

    internal interface ToggleResultListener {
        fun onPositiveResult()
        fun onNegativeResult()
    }

    private inner class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        val title: TextView = itemView.findViewById(R.id.name)
        val content: TextView = itemView.findViewById(R.id.requested)
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
        val infoIcon: ImageView = itemView.findViewById(R.id.requestedInfoIcon)
        val divider: View = itemView.findViewById(R.id.divider)

        init {
            val container: LinearLayout = itemView.findViewById(R.id.container)
            val card: MaterialCardView? = itemView.findViewById(R.id.card)

            if (CandyBarApplication.getConfiguration().requestStyle == CandyBarApplication.Style.PORTRAIT_FLAT_LANDSCAPE_FLAT &&
                card != null
            ) {
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.let { params ->
                    card.radius = 0f
                    card.useCompatPadding = false
                    val margin = mContext.resources.getDimensionPixelSize(R.dimen.card_margin)
                    params.setMargins(0, 0, margin, margin)
                    params.marginEnd = margin
                }
            }

            if (mContext.resources.getBoolean(R.bool.use_flat_card) && card != null) {
                card.strokeWidth = mContext.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                card.cardElevation = 0f
                card.useCompatPadding = false
                val marginTop = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_top)
                val marginLeft = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_left)
                val marginRight = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_right)
                val marginBottom = mContext.resources.getDimensionPixelSize(R.dimen.card_margin_bottom)
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.setMargins(
                    marginLeft, marginTop, marginRight, marginBottom
                )
            }

            if (!Preferences.get(mContext).isCardShadowEnabled) {
                card?.cardElevation = 0f
            }

            container.setOnClickListener(this)
            container.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            if (view.id == R.id.container) {
                val position = if (mShowPremiumRequest || mShowRegularRequestLimit) {
                    bindingAdapterPosition - 1
                } else {
                    bindingAdapterPosition
                }
                toggleSelection(position, object : ToggleResultListener {
                    override fun onPositiveResult() {
                        checkbox.toggle()
                        (mContext as? RequestListener)?.onRequestSelected(selectedItemsSize)
                    }

                    override fun onNegativeResult() {}
                })
            }
        }

        override fun onLongClick(view: View): Boolean {
            if (view.id == R.id.container) {
                val position = if (mShowPremiumRequest || mShowRegularRequestLimit) {
                    bindingAdapterPosition - 1
                } else {
                    bindingAdapterPosition
                }
                toggleSelection(position, object : ToggleResultListener {
                    override fun onPositiveResult() {
                        checkbox.toggle()
                        (mContext as? RequestListener)?.onRequestSelected(selectedItemsSize)
                    }

                    override fun onNegativeResult() {}
                })
                return true
            }
            return false
        }
    }

    private inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            val shadow: View = itemView.findViewById(R.id.shadow)
            if (!Preferences.get(mContext).isCardShadowEnabled) {
                shadow.visibility = View.GONE
            }
        }
    }

    private fun getLayoutParams(view: View?): StaggeredGridLayoutManager.LayoutParams? {
        return view?.layoutParams as? StaggeredGridLayoutManager.LayoutParams
    }

    private fun toggleSelection(position: Int, toggleListener: ToggleResultListener) {
        if (position in mRequests.indices) {
            val isSelected = mSelectedItems.get(position, false)
            val isRequested = mRequests[position].isRequested
            val isDuplicateRequestAllowed = mContext.resources.getBoolean(R.bool.enable_icon_request_multiple)

            if (isSelected) {
                mSelectedItems.delete(position)
                toggleListener.onPositiveResult()
            } else if (isRequested) {
                if (isDuplicateRequestAllowed) {
                    MaterialDialog.Builder(mContext)
                        .typeface(TypefaceHelper.getMedium(mContext), TypefaceHelper.getRegular(mContext))
                        .title(R.string.request_already_requested)
                        .content(R.string.request_requested_possible)
                        .cancelable(false)
                        .canceledOnTouchOutside(false)
                        .negativeText(R.string.request_requested_button_cancel)
                        .onNegative { _, _ -> toggleListener.onNegativeResult() }
                        .positiveText(R.string.request_requested_button_confirm)
                        .onPositive { _, _ ->
                            mSelectedItems.put(position, true)
                            toggleListener.onPositiveResult()
                        }
                        .show()
                } else {
                    toggleListener.onNegativeResult()
                    MaterialDialog.Builder(mContext)
                        .typeface(TypefaceHelper.getMedium(mContext), TypefaceHelper.getRegular(mContext))
                        .title(R.string.request_not_available)
                        .content(R.string.request_requested)
                        .negativeText(R.string.request_requested_button_cancel)
                        .show()
                }
            } else if (!mRequests[position].isAvailableForRequest) {
                toggleListener.onNegativeResult()
                if (mRequests[position].infoText.isNotEmpty()) {
                    MaterialDialog.Builder(mContext)
                        .typeface(TypefaceHelper.getMedium(mContext), TypefaceHelper.getRegular(mContext))
                        .title(mContext.resources.getString(R.string.request_not_available))
                        .content(mRequests[position].infoText)
                        .positiveText(android.R.string.yes)
                        .show()
                }
            } else {
                mSelectedItems.put(position, true)
                toggleListener.onPositiveResult()
            }
        } else {
            toggleListener.onNegativeResult()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll(): Boolean {
        if (mSelectedAll) {
            mSelectedAll = false
            resetSelectedItems()
            return false
        }

        mSelectedItems.clear()
        for (i in mRequests.indices) {
            if (!mRequests[i].isRequested && mRequests[i].isAvailableForRequest) {
                mSelectedItems.put(i, true)
            }
        }
        mSelectedAll = mSelectedItems.size() > 0
        notifyDataSetChanged()

        (mContext as? RequestListener)?.onRequestSelected(selectedItemsSize)
        return mSelectedAll
    }

    fun setRequested(position: Int, requested: Boolean) {
        mRequests[position].isRequested = requested
    }

    val selectedItemsSize: Int
        get() = mSelectedItems.size()

    val selectedItems: MutableList<Int>
        get() {
            val selected = ArrayList<Int>()
            for (i in 0 until mSelectedItems.size()) {
                selected.add(mSelectedItems.keyAt(i))
            }
            return selected
        }

    var selectedItemsArray: SparseBooleanArray
        get() = mSelectedItems
        @SuppressLint("NotifyDataSetChanged")
        set(selectedItems) {
            mSelectedItems = selectedItems
            notifyDataSetChanged()
        }

    @SuppressLint("NotifyDataSetChanged")
    fun resetSelectedItems() {
        mSelectedAll = false
        mSelectedItems.clear()
        (mContext as? RequestListener)?.onRequestSelected(selectedItemsSize)
        notifyDataSetChanged()
    }

    val selectedApps: List<Request>
        get() {
            val items = ArrayList<Request>(mSelectedItems.size())
            for (i in 0 until mSelectedItems.size()) {
                val position = mSelectedItems.keyAt(i)
                if (position in mRequests.indices) {
                    items.add(mRequests[position])
                }
            }
            return items
        }

    val isContainsRequested: Boolean
        get() {
            val requests = selectedApps
            return requests.any { it.isRequested }
        }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1
        private const val TYPE_FOOTER = 2
    }
}
