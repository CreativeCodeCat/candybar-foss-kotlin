package candybar.lib.fragments

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.applications.CandyBarApplication
import candybar.lib.fragments.dialog.IconShapeChooserFragment
import candybar.lib.helpers.IconsHelper
import candybar.lib.helpers.TapIntroHelper
import candybar.lib.items.Icon
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.listeners.SearchListener
import com.bumptech.glide.Glide
import com.danimahardhika.android.helpers.animation.AnimationHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

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
class IconsBaseFragment : Fragment() {

    private var mPager: ViewPager2? = null
    private var mProgress: ProgressBar? = null
    private var mTabLayout: TabLayout? = null
    private var mGetIcons: AsyncTaskBase? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_icons_base, container, false)
        mTabLayout = view.findViewById(R.id.tab)
        mPager = view.findViewById(R.id.pager)
        mProgress = view.findViewById(R.id.progress)
        initTabs()
        mPager!!.offscreenPageLimit = 2
        // Reduce sensitivity of ViewPager
        try {
            val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            recyclerViewField.isAccessible = true
            val recyclerView = recyclerViewField.get(mPager) as RecyclerView
            val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
            touchSlopField.isAccessible = true
            val touchSlop = touchSlopField.get(recyclerView) as Int
            touchSlopField.set(recyclerView, touchSlop * 3)
        } catch (e: Exception) {
            LogUtil.d(Log.getStackTraceString(e))
        }
        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_icons_search, menu)
        val search = menu.findItem(R.id.menu_search)
        val iconShape = menu.findItem(R.id.menu_icon_shape)

        search.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                val fm = requireActivity().supportFragmentManager
                setHasOptionsMenu(false)
                val view = requireActivity().findViewById<View>(R.id.shadow)
                if (view != null) view.animate().translationY(-mTabLayout!!.height.toFloat())
                    .setDuration(200).start()
                mTabLayout!!.animate().translationY(-mTabLayout!!.height.toFloat()).setDuration(200)
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            super.onAnimationEnd(animation)
                            val prev = fm.findFragmentByTag("home")
                            if (prev != null) return

                            val adapter = mPager!!.adapter as PagerIconsAdapter?
                            if (adapter == null) return

                            val listener = requireActivity() as SearchListener
                            listener.onSearchExpanded(true)

                            val ft = fm.beginTransaction()
                                .replace(
                                    R.id.container,
                                    IconsSearchFragment(),
                                    IconsSearchFragment.TAG
                                )
                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                                .addToBackStack(null)

                            try {
                                ft.commit()
                            } catch (e: Exception) {
                                ft.commitAllowingStateLoss()
                            }
                        }
                    }).start()

                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                return true
            }
        })

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            !requireActivity().resources.getBoolean(R.bool.includes_adaptive_icons)
        ) {
            iconShape.isVisible = false
        }

        iconShape.setOnMenuItemClickListener {
            IconShapeChooserFragment.showIconShapeChooser(requireActivity().supportFragmentManager)
            false
        }
    }

    override fun onDestroy() {
        if (mGetIcons != null) {
            mGetIcons!!.cancel(true)
        }
        val activity = activity
        if (activity != null) Glide.get(activity).clearMemory()
        super.onDestroy()
    }

    private fun initTabs() {
        AnimationHelper.slideDownIn(mTabLayout!!)
            .interpolator(LinearOutSlowInInterpolator())
            .callback(object : AnimationHelper.Callback {
                override fun onAnimationStart() {

                }

                override fun onAnimationEnd() {
                    if (activity == null) return

                    if (Preferences.get(requireActivity()).isToolbarShadowEnabled) {
                        AnimationHelper.fade(requireActivity().findViewById(R.id.shadow)).start()
                    }

                    mGetIcons = IconsLoader().execute()
                }
            })
            .start()
    }

    private inner class IconsLoader : AsyncTaskBase() {
        override fun preRun() {
            if (CandyBarMainActivity.sSections == null) {
                mProgress!!.visibility = View.VISIBLE
            }
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)
                    IconsHelper.loadIcons(requireActivity(), true)
                    return true;
                } catch (e: Exception) {
                    LogUtil.e(Log.getStackTraceString(e));
                    return false;
                }
            }
            return false;
        }

        override fun postRun(ok: Boolean) {
            if (activity == null) return
            if (requireActivity().isFinishing) return

            mGetIcons = null
            mProgress!!.visibility = View.GONE
            if (ok) {
                setHasOptionsMenu(true)

                val adapter = PagerIconsAdapter(
                    childFragmentManager, lifecycle, CandyBarMainActivity.sSections ?: emptyList()
                )
                mPager!!.adapter = adapter

                TabLayoutMediator(
                    mTabLayout!!, mPager!!
                ) { _, _ -> }.attach()
                mPager!!.currentItem = 1

                TabTypefaceChanger().executeOnThreadPool()

                if (requireActivity().resources.getBoolean(R.bool.show_intro)) {
                    TapIntroHelper.showIconsIntro(requireActivity())
                }
            } else {
                Toast.makeText(
                    activity, R.string.icons_load_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private inner class TabTypefaceChanger : AsyncTaskBase() {

        var adapter: PagerIconsAdapter? = null

        override fun preRun() {
            adapter = mPager!!.adapter as PagerIconsAdapter?
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)
                    for (i in 0 until adapter!!.itemCount) {
                        val j = i
                        runOnUiThread {
                            if (activity == null) return@runOnUiThread
                            if (requireActivity().isFinishing) return@runOnUiThread
                            if (mTabLayout == null) return@runOnUiThread

                            if (j < mTabLayout!!.tabCount) {
                                val tab = mTabLayout!!.getTabAt(j)
                                if (tab != null) {
                                    if (j == 0) {
                                        tab.setIcon(R.drawable.ic_bookmarks)
                                    } else if (j < adapter!!.itemCount) {
                                        tab.setCustomView(R.layout.fragment_icons_base_tab)
                                        tab.text = adapter!!.getPageTitle(j - 1)
                                    }
                                }
                            }
                        }
                    }
                    return true
                } catch (ignored: Exception) {
                    return false
                }
            }
            return false
        }
    }

    private class PagerIconsAdapter(
        fm: FragmentManager, lifecycle: Lifecycle,
        private val mIcons: List<Icon>
    ) : FragmentStateAdapter(fm, lifecycle) {

        fun getPageTitle(position: Int): CharSequence {
            var title = mIcons[position].title
            if (CandyBarApplication.getConfiguration().isShowTabIconsCount) {
                title += " (" + mIcons[position].icons.size + ")"
            }
            return title
        }

        override fun createFragment(position: Int): Fragment {
            return IconsFragment.newInstance(position - 1)
        }

        override fun getItemCount(): Int {
            return mIcons.size + 1
        }
    }
}
