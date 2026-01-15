package candybar.lib.adapters

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.TypedValue
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.fragments.IconsFragment
import candybar.lib.helpers.IconsHelper
import candybar.lib.helpers.IntentHelper
import candybar.lib.items.Icon
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import com.danimahardhika.android.helpers.core.SoftKeyboardHelper
import com.google.android.material.tabs.TabLayout
import java.lang.ref.WeakReference
import java.util.Locale

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
class IconsAdapter(
    private val mContext: Context,
    private var mIcons: List<Icon>,
    private val mFragment: Fragment,
    private val mIsBookmarkMode: Boolean
) : RecyclerView.Adapter<IconsAdapter.ViewHolder>() {

    private var mIconsAll: List<Icon>? = null
    private var mRecyclerView: WeakReference<RecyclerView>? = null
    private var mSelectedIcons: MutableList<Icon> = ArrayList()

    private var visibleStart: Int = 0
    private var visibleEnd: Int = 0

    private val mIsShowIconName: Boolean = mContext.resources.getBoolean(R.bool.show_icon_name)
    private var actionMode: ActionMode? = null

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            mode.menuInflater.inflate(R.menu.menu_bookmark_icons, menu)
            val activity = mContext as Activity
            val tabLayout = activity.findViewById<TabLayout>(R.id.tab)
            val shadow = activity.findViewById<View>(R.id.shadow)

            shadow?.animate()?.translationY(-tabLayout.height.toFloat())?.setDuration(200)?.start()

            tabLayout.animate().translationY(-tabLayout.height.toFloat()).setDuration(200)
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        tabLayout.visibility = View.GONE
                        shadow?.translationY = 0f
                        tabLayout.animate().setListener(null)
                    }
                }).start()

            activity.findViewById<ViewPager2>(R.id.pager).isUserInputEnabled = false
            activity.findViewById<DrawerLayout>(R.id.drawer_layout)
                .setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

            for (i in mIcons.indices) {
                getViewHolderAt(i)?.onActionModeChange()
            }
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.title = mContext.resources.getString(R.string.items_selected, mSelectedIcons.size)
            menu.findItem(R.id.menu_select_all).setIcon(
                if (mSelectedIcons.size == mIcons.size) R.drawable.ic_toolbar_select_all_selected
                else R.drawable.ic_toolbar_select_all
            )
            menu.findItem(R.id.menu_delete).isVisible = mSelectedIcons.isNotEmpty()
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.menu_delete -> {
                    val drawableNames = mSelectedIcons.map { it.drawableName }
                    Database.get(mContext).deleteBookmarkedIcons(drawableNames)
                    IconsFragment.reloadBookmarks()
                    mode.finish()
                    true
                }

                R.id.menu_select_all -> {
                    if (mSelectedIcons.size != mIcons.size) {
                        for (i in mIcons.indices) {
                            getViewHolderAt(i)?.setChecked(true, true)
                        }
                        mSelectedIcons = ArrayList(mIcons)
                    } else {
                        for (i in mIcons.indices) {
                            getViewHolderAt(i)?.setChecked(false, true)
                        }
                        mSelectedIcons = ArrayList()
                    }
                    actionMode?.invalidate()
                    true
                }

                else -> false
            }
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            actionMode = null
            mSelectedIcons = ArrayList()
            val activity = mContext as Activity
            val tabLayout = activity.findViewById<TabLayout>(R.id.tab)
            val shadow = activity.findViewById<View>(R.id.shadow)

            shadow?.let {
                it.translationY = -tabLayout.height.toFloat()
                it.animate().translationY(0f).setDuration(200).start()
            }
            tabLayout.visibility = View.VISIBLE
            tabLayout.animate().translationY(0f).setDuration(200).start()

            activity.findViewById<ViewPager2>(R.id.pager).isUserInputEnabled = true
            activity.findViewById<DrawerLayout>(R.id.drawer_layout)
                .setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

            for (i in mIcons.indices) {
                getViewHolderAt(i)?.onActionModeChange()
            }
        }
    }

    fun setIcons(icons: List<Icon>) {
        mIcons = icons
        notifyDataSetChanged()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        mRecyclerView = WeakReference(recyclerView)
        val manager = recyclerView.layoutManager
        if (manager is GridLayoutManager && itemCount > 0) {
            recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    visibleStart = manager.findFirstVisibleItemPosition()
                    visibleEnd = manager.findLastVisibleItemPosition()
                }
            })
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(mContext).inflate(
            R.layout.fragment_icons_item_grid, parent, false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val icon = mIcons[position]
        holder.name.text = icon.title
        loadIconInto(holder.icon, position)
        if (mIsBookmarkMode) {
            holder.setCheckChangedListener(null)
            holder.setChecked(mSelectedIcons.contains(icon), false)
            holder.setCheckChangedListener { isChecked ->
                if (isChecked) {
                    mSelectedIcons.add(icon)
                } else {
                    mSelectedIcons.remove(icon)
                }
                actionMode?.invalidate()
            }
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        Glide.with(mFragment).clear(holder.icon)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int = mIcons.size

    private fun getViewHolderAt(position: Int): ViewHolder? {
        return mRecyclerView?.get()?.findViewHolderForAdapterPosition(position) as? ViewHolder
    }

    private fun loadIconInto(imageView: ImageView, position: Int) {
        if (mFragment.activity == null) return
        Glide.with(mFragment)
            .load("drawable://" + mIcons[position].res)
            .skipMemoryCache(true)
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(imageView)
    }

    fun reloadIcons() {
        for (i in visibleStart..visibleEnd) {
            val holder = getViewHolderAt(i)
            if (holder != null) loadIconInto(holder.icon, i)
        }
    }

    internal fun interface CheckChangedListener {
        fun onCheckChanged(isChecked: Boolean)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val name: TextView = itemView.findViewById(R.id.name)
        private val container: View = itemView.findViewById(R.id.container)
        private val innerContainer: View = itemView.findViewById(R.id.inner_container)
        private val checkBackground: View = itemView.findViewById(R.id.check_background)
        private var isChecked: Boolean = false
        private var checkChangedListener: CheckChangedListener? = null

        init {
            container.setOnClickListener(this)
            if (mIsBookmarkMode) {
                container.setOnLongClickListener(this)
                val color = ColorHelper.getAttributeColor(mContext, com.google.android.material.R.attr.colorSecondary)
                (checkBackground.findViewById<View>(R.id.checkmark) as ImageView)
                    .setImageDrawable(DrawableHelper.getTintedDrawable(mContext, R.drawable.ic_check_circle, color))
            }

            if (!mIsShowIconName) {
                name.visibility = View.GONE
            }

            onActionModeChange()
        }

        fun onActionModeChange() {
            val outValue = TypedValue()
            if (actionMode != null) {
                mContext.theme.resolveAttribute(androidx.appcompat.R.attr.selectableItemBackground, outValue, true)
                container.setBackgroundResource(outValue.resourceId)
                innerContainer.setBackgroundResource(0)
            } else {
                mContext.theme.resolveAttribute(androidx.appcompat.R.attr.selectableItemBackgroundBorderless, outValue, true)
                container.setBackgroundResource(0)
                innerContainer.setBackgroundResource(outValue.resourceId)
                setChecked(false, true)
            }
        }

        internal fun setCheckChangedListener(checkChangedListener: CheckChangedListener?) {
            this.checkChangedListener = checkChangedListener
        }

        fun setChecked(isChecked: Boolean, animate: Boolean) {
            this.isChecked = isChecked
            val scale = if (isChecked) 0.6f else 1f
            if (animate) {
                checkBackground.animate().alpha(if (isChecked) 1f else 0f).setDuration(200).start()
                icon.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
            } else {
                checkBackground.alpha = if (isChecked) 1f else 0f
                icon.scaleX = scale
                icon.scaleY = scale
            }
            checkChangedListener?.onCheckChanged(isChecked)
        }

        override fun onClick(view: View) {
            val id = view.id
            val position = bindingAdapterPosition
            if (id == R.id.container) {
                if (position < 0 || position >= mIcons.size) return
                if (actionMode != null) {
                    setChecked(!isChecked, true)
                } else {
                    SoftKeyboardHelper.closeKeyboard(mContext)
                    IconsHelper.selectIcon(mContext, IntentHelper.sAction, mIcons[position])
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            if (actionMode == null) {
                (mContext as Activity).startActionMode(actionModeCallback)
            }
            setChecked(!isChecked, true)
            return true
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun search(string: String) {
        if (mIconsAll == null) {
            mIconsAll = mIcons
        }

        val query = string.lowercase(Locale.ENGLISH).trim()
        mIcons = if (query.isEmpty()) {
            mIconsAll!!
        } else {
            mIconsAll!!.filter { it.title.lowercase(Locale.ENGLISH).contains(query) }
        }

        val analyticsHandler = CandyBarApplication.getConfiguration().analyticsHandler
        val params = hashMapOf<String, Any>(
            "section" to "icons",
            "action" to "search",
            "item" to query,
            "found" to if (mIcons.isEmpty()) "no" else "yes",
            "number_of_icons" to mIcons.size
        )
        analyticsHandler?.logEvent("click", params)

        notifyDataSetChanged()
    }
}
