package candybar.lib.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.adapters.IconsAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.fragments.dialog.IconShapeChooserFragment
import candybar.lib.helpers.IconsHelper
import candybar.lib.helpers.ViewHelper.setFastScrollColor
import candybar.lib.items.Icon
import candybar.lib.utils.AlphanumComparator
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.listeners.SearchListener
import com.bumptech.glide.Glide
import com.danimahardhika.android.helpers.core.SoftKeyboardHelper
import com.danimahardhika.android.helpers.core.ViewHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import java.lang.ref.WeakReference
import java.util.Collections

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
class IconsSearchFragment : Fragment() {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mFastScroll: RecyclerFastScroller
    private lateinit var mSearchResult: TextView
    private var mSearchInput: EditText? = null

    private var mAdapter: IconsAdapter? = null
    private var mAsyncTask: AsyncTaskBase? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_icons_search, container, false)
        mRecyclerView = view.findViewById(R.id.icons_grid)
        mFastScroll = view.findViewById(R.id.fastscroll)
        mSearchResult = view.findViewById(R.id.search_result)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val params = HashMap<String, Any>()
        params["section"] = "icons_search"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        setHasOptionsMenu(true)

        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.layoutManager = GridLayoutManager(
            activity,
            requireActivity().resources.getInteger(R.integer.icons_column_count)
        )

        setFastScrollColor(mFastScroll)
        mFastScroll.attachRecyclerView(mRecyclerView)
        mAsyncTask = IconsLoader().execute()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.menu_icons_search, menu)
        val search = menu.findItem(R.id.menu_search)
        val iconShape = menu.findItem(R.id.menu_icon_shape)
        val searchView = search.actionView!!

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            !requireActivity().resources.getBoolean(R.bool.includes_adaptive_icons)
        ) {
            iconShape.isVisible = false
        } else {
            searchView.findViewById<View>(R.id.container).setPadding(0, 0, 0, 0)
        }

        val clearQueryButton = searchView.findViewById<View>(R.id.clear_query_button)
        mSearchInput = searchView.findViewById(R.id.search_input)
        mSearchInput?.setHint(R.string.search_icon)

        search.expandActionView()

        search.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                requireActivity().supportFragmentManager.popBackStack()

                val activity = requireActivity()
                Handler(Looper.getMainLooper()).postDelayed({
                    (activity as SearchListener).onSearchExpanded(false)
                }, 500)
                return true
            }
        })

        mSearchInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val query = charSequence.toString()
                filterSearch(query)
                clearQueryButton.visibility = if (query == "") View.GONE else View.VISIBLE
            }
        })

        clearQueryButton.setOnClickListener { mSearchInput?.setText("") }

        iconShape.setOnMenuItemClickListener {
            IconShapeChooserFragment.showIconShapeChooser(requireActivity().supportFragmentManager)
            false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ViewHelper.resetSpanCount(
            mRecyclerView,
            requireActivity().resources.getInteger(R.integer.icons_column_count)
        )
    }

    override fun onDestroy() {
        if (mAsyncTask != null) mAsyncTask!!.cancel(true)
        currentAdapter = null
        val activity = activity
        if (activity != null) {
            Glide.get(activity).clearMemory()
        }
        super.onDestroy()
    }

    @SuppressLint("StringFormatInvalid")
    private fun filterSearch(query: String) {
        try {
            mAdapter?.search(query)
            if (mAdapter?.itemCount == 0) {
                val text = requireActivity().resources.getString(R.string.search_noresult, query)
                mSearchResult.text = text
                mSearchResult.visibility = View.VISIBLE
            } else mSearchResult.visibility = View.GONE
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        }
    }

    private inner class IconsLoader : AsyncTaskBase() {

        private var iconSet: MutableSet<Icon> = HashSet()
        private var iconList: MutableList<Icon> = ArrayList()
        private var excludedCategories: MutableSet<String> = HashSet()

        override fun preRun() {
            iconSet = HashSet()
            val exCategories = CandyBarApplication.getConfiguration().excludedCategoryForSearch
            excludedCategories = if (exCategories != null) HashSet(exCategories.toList()) else HashSet()
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)
                    if (CandyBarMainActivity.sSections == null) {
                        CandyBarMainActivity.sSections = IconsHelper.getIconsList(requireActivity())

                        for (section in CandyBarMainActivity.sSections!!) {
                            if (requireActivity().resources.getBoolean(R.bool.show_icon_name)) {
                                IconsHelper.computeTitles(requireActivity(), section.icons)
                            }
                        }

                        if (CandyBarApplication.getConfiguration().isShowTabAllIcons) {
                            val icons = IconsHelper.getTabAllIcons()
                            CandyBarMainActivity.sSections!!.add(
                                Icon(
                                    CandyBarApplication.getConfiguration().tabAllIconsTitle, icons
                                )
                            )
                        }
                    }

                    for (icon in CandyBarMainActivity.sSections!!) {
                        val isExcluded = excludedCategories.contains(icon.title)
                        val allIconsTabTitle = CandyBarApplication.getConfiguration().tabAllIconsTitle
                        if (CandyBarApplication.getConfiguration().isShowTabAllIcons) {
                            if (icon.title != allIconsTabTitle && !isExcluded) {
                                iconSet.addAll(icon.icons)
                            }
                        } else if (!isExcluded) {
                            iconSet.addAll(icon.icons)
                        }
                    }

                    iconList = ArrayList(iconSet)

                    // Sort them in lowercase
                    Collections.sort(iconList, object : AlphanumComparator() {
                        override fun compare(o1: Any, o2: Any): Int {
                            val s1 = (o1 as Icon).title.lowercase().trim()
                            val s2 = (o2 as Icon).title.lowercase().trim()
                            return super.compare(s1, s2)
                        }
                    })

                    return true
                } catch (e: Exception) {
                    LogUtil.e(Log.getStackTraceString(e))
                    return false
                }
            }
            return false
        }

        override fun postRun(ok: Boolean) {
            if (activity == null) return
            if (requireActivity().isFinishing) return

            mAsyncTask = null
            if (ok) {
                mAdapter = IconsAdapter(requireActivity(), iconList, this@IconsSearchFragment, false)
                currentAdapter = WeakReference(mAdapter)
                mRecyclerView.adapter = mAdapter
                filterSearch("")
                mSearchInput?.requestFocus()
                SoftKeyboardHelper.openKeyboard(requireActivity())
            } else {
                // Unable to load all icons
                Toast.makeText(
                    activity, R.string.icons_load_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        const val TAG = "icons_search"
        private var currentAdapter: WeakReference<IconsAdapter?>? = null

        @JvmStatic
        fun reloadIcons() {
            if (currentAdapter != null && currentAdapter!!.get() != null)
                currentAdapter!!.get()!!.reloadIcons()
        }
    }
}
