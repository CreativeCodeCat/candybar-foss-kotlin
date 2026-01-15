package candybar.lib.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
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
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import candybar.lib.R
import candybar.lib.adapters.WallpapersAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.helpers.JsonHelper
import candybar.lib.helpers.TapIntroHelper
import candybar.lib.helpers.WallpaperHelper
import candybar.lib.items.Wallpaper
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.listeners.WallpapersListener
import com.bumptech.glide.Glide
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.SoftKeyboardHelper
import com.danimahardhika.android.helpers.core.ViewHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import java.io.InputStream
import candybar.lib.helpers.ViewHelper as CBViewHelper

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
class WallpapersFragment : Fragment() {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mSwipe: SwipeRefreshLayout
    private lateinit var mProgress: ProgressBar
    private lateinit var mSearchResult: TextView
    private lateinit var mFastScroll: RecyclerFastScroller

    private var mAsyncTask: AsyncTaskBase? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wallpapers, container, false)
        mRecyclerView = view.findViewById(R.id.wallpapers_grid)
        mSwipe = view.findViewById(R.id.swipe)
        mProgress = view.findViewById(R.id.progress)
        mSearchResult = view.findViewById(R.id.search_result)
        mFastScroll = view.findViewById(R.id.fastscroll)

        if (!Preferences.get(requireActivity()).isToolbarShadowEnabled) {
            val shadow = view.findViewById<View>(R.id.shadow)
            shadow?.visibility = View.GONE
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val params = HashMap<String, Any>()
        params["section"] = "wallpapers"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        mRecyclerView.isNestedScrollingEnabled = false

        mProgress.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
            ColorHelper.getAttributeColor(requireActivity(), com.google.android.material.R.attr.colorSecondary),
            PorterDuff.Mode.SRC_IN
        )
        mSwipe.setColorSchemeColors(
            ColorHelper.getAttributeColor(requireActivity(), R.attr.cb_swipeRefresh)
        )
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.setHasFixedSize(false)
        mRecyclerView.layoutManager = GridLayoutManager(
            activity,
            requireActivity().resources.getInteger(R.integer.wallpapers_column_count)
        )

        CBViewHelper.setFastScrollColor(mFastScroll)
        mFastScroll.attachRecyclerView(mRecyclerView)

        mSwipe.setOnRefreshListener {
            if (mProgress.visibility == View.GONE)
                mAsyncTask = WallpapersLoader(true).execute()
            else mSwipe.isRefreshing = false
        }

        mAsyncTask = WallpapersLoader(false).execute()
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val search = menu.findItem(R.id.menu_search)

        val searchView = search.actionView
        val searchInput = searchView!!.findViewById<EditText>(R.id.search_input)
        val clearQueryButton = searchView.findViewById<View>(R.id.clear_query_button)

        searchInput.hint = requireActivity().resources.getString(R.string.search_wallpapers)
        searchInput.requestFocus()

        Handler(Looper.getMainLooper()).postDelayed({
            if (activity != null) {
                SoftKeyboardHelper.openKeyboard(requireActivity())
            }
        }, 1000)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val query = charSequence.toString()
                filterSearch(query)
                clearQueryButton.visibility = if (query == "") View.GONE else View.VISIBLE
            }
        })

        clearQueryButton.setOnClickListener { searchInput.setText("") }

        search.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                searchInput.requestFocus()

                Handler(Looper.getMainLooper()).postDelayed({
                    if (activity != null) {
                        SoftKeyboardHelper.openKeyboard(requireActivity())
                    }
                }, 1000)

                return true
            }

            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                searchInput.setText("")
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ViewHelper.resetSpanCount(
            mRecyclerView,
            requireActivity().resources.getInteger(R.integer.wallpapers_column_count)
        )
    }

    override fun onDestroy() {
        if (mAsyncTask != null) mAsyncTask!!.cancel(true)
        val activity = activity
        if (activity != null) Glide.get(activity).clearMemory()
        setHasOptionsMenu(false)
        super.onDestroy()
    }

    @SuppressLint("StringFormatInvalid")
    private fun filterSearch(query: String) {
        if (mRecyclerView.adapter != null) {
            val adapter = mRecyclerView.adapter as WallpapersAdapter
            adapter.search(query)
            if (adapter.itemCount == 0) {
                val text = requireActivity().resources.getString(R.string.search_noresult, query)
                mSearchResult.text = text
                mSearchResult.visibility = View.VISIBLE
            } else mSearchResult.visibility = View.GONE
        }
    }

    private inner class WallpapersLoader(private val refreshing: Boolean) : AsyncTaskBase() {

        private var wallpapers: List<Wallpaper>? = null

        override fun preRun() {
            if (!refreshing) mProgress.visibility = View.VISIBLE
            else mSwipe.isRefreshing = true
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)

                    val stream: InputStream? = WallpaperHelper.getJSONStream(requireActivity())

                    if (stream != null) {
                        val list = JsonHelper.parseList(stream)
                        if (list == null) {
                            LogUtil.e(
                                "Json error, no array with name: "
                                        + CandyBarApplication.getConfiguration().wallpaperJsonStructure.arrayName
                            )
                            return false
                        }

                        if (Database.get(requireActivity()).wallpapersCount > 0) {
                            Database.get(requireActivity()).deleteWallpapers()
                        }

                        Database.get(requireActivity()).addWallpapers(null, list)
                        wallpapers = Database.get(requireActivity()).getWallpapers(null)

                        return true
                    }
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
            mProgress.visibility = View.GONE
            mSwipe.isRefreshing = false

            if (ok) {
                setHasOptionsMenu(true)

                mRecyclerView.adapter = WallpapersAdapter(requireActivity(), wallpapers ?: ArrayList())

                (requireActivity() as WallpapersListener)
                    .onWallpapersChecked(Database.get(requireActivity()).wallpapersCount)

                try {
                    if (requireActivity().resources.getBoolean(R.bool.show_intro)) {
                        TapIntroHelper.showWallpapersIntro(requireActivity(), mRecyclerView)
                    }
                } catch (e: Exception) {
                    LogUtil.e(Log.getStackTraceString(e))
                }
            } else {
                Toast.makeText(
                    requireActivity(), R.string.connection_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
