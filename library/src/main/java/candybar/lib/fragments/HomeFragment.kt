package candybar.lib.fragments

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.adapters.HomeAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.TapIntroHelper
import candybar.lib.helpers.WallpaperHelper
import candybar.lib.items.Home
import candybar.lib.preferences.Preferences
import candybar.lib.utils.listeners.HomeListener

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
class HomeFragment : Fragment(), HomeListener {

    private var mRecyclerView: RecyclerView? = null
    private var mManager: StaggeredGridLayoutManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        mRecyclerView = view.findViewById(R.id.recyclerview)

        if (!Preferences.get(requireActivity()).isToolbarShadowEnabled) {
            val shadow = view.findViewById<View>(R.id.shadow)
            shadow?.visibility = View.GONE
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val params = HashMap<String, Any>()
        params["section"] = "home"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        mManager = StaggeredGridLayoutManager(
            requireActivity().resources.getInteger(R.integer.home_column_count),
            StaggeredGridLayoutManager.VERTICAL
        )
        mRecyclerView?.let {
            it.setHasFixedSize(true)
            it.itemAnimator = DefaultItemAnimator()
            it.layoutManager = mManager

            if (CandyBarApplication.getConfiguration().homeGrid == CandyBarApplication.GridStyle.FLAT) {
                val padding = requireActivity().resources.getDimensionPixelSize(R.dimen.card_margin)
                it.setPadding(padding, padding, 0, 0)
            }
        }

        initHome()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val adapter = mRecyclerView?.adapter as HomeAdapter?
        adapter?.setOrientation(newConfig.orientation)
    }

    override fun onHomeDataUpdated(home: Home?) {
        if (mRecyclerView == null) return
        if (mRecyclerView?.adapter == null) return

        if (home != null) {
            val adapter = mRecyclerView?.adapter as HomeAdapter?
            adapter?.let {
                if (CandyBarApplication.getConfiguration().isAutomaticIconsCountEnabled) {
                    val index = it.iconsIndex
                    if (index >= 0 && index < it.itemCount) {
                        it.getItem(index).title = CandyBarMainActivity.sIconsCount.toString()
                        it.getItem(index).isLoading = false
                        it.notifyItemChanged(index)
                    }
                }

                val dimensionsIndex = it.dimensionsIndex
                if (dimensionsIndex < 0 && requireActivity().resources.getBoolean(R.bool.show_random_icon)) {
                    it.addNewContent(home)
                }
            }
            return
        }

        val adapter = mRecyclerView?.adapter
        adapter?.let {
            if (it.itemCount > 8) {
                // Probably the original adapter already modified
                it.notifyDataSetChanged()
                return
            }

            if (it is HomeAdapter) {
                val index = it.iconRequestIndex
                if (index >= 0 && index < it.itemCount) {
                    it.notifyItemChanged(index)
                }
            }
        }
    }

    override fun onHomeIntroInit() {
        if (requireActivity().resources.getBoolean(R.bool.show_intro)) {
            TapIntroHelper.showHomeIntros(
                requireActivity(),
                mRecyclerView, mManager,
                (mRecyclerView?.adapter as HomeAdapter).applyIndex
            )
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun initHome() {
        val homes: MutableList<Home> = ArrayList()
        val resources = requireActivity().resources

        if (resources.getBoolean(R.bool.enable_apply)) {
            homes.add(
                Home(
                    R.drawable.ic_toolbar_apply_launcher,
                    resources.getString(
                        R.string.home_apply_icon_pack,
                        resources.getString(R.string.app_name)
                    ),
                    "",
                    Home.Type.APPLY,
                    false
                )
            )
        }

        if (resources.getBoolean(R.bool.enable_donation)) {
            homes.add(
                Home(
                    R.drawable.ic_toolbar_donate,
                    resources.getString(R.string.home_donate),
                    resources.getString(R.string.home_donate_desc),
                    Home.Type.DONATE,
                    false
                )
            )
        }

        homes.add(
            Home(
                -1,
                if (CandyBarApplication.getConfiguration().isAutomaticIconsCountEnabled)
                    CandyBarMainActivity.sIconsCount.toString()
                else
                    CandyBarApplication.getConfiguration().customIconsCount.toString(),
                resources.getString(R.string.home_icons),
                Home.Type.ICONS,
                true
            )
        )

        val homeIcon = CandyBarMainActivity.sHomeIcon
        if (homeIcon != null && requireActivity().resources.getBoolean(R.bool.show_random_icon)) {
            homes.add(homeIcon)
        }

        mRecyclerView?.adapter = HomeAdapter(
            requireActivity(), homes,
            resources.configuration.orientation
        )
    }

    fun resetWallpapersCount() {
        if (WallpaperHelper.getWallpaperType(requireActivity()) == WallpaperHelper.CLOUD_WALLPAPERS) {
            if (mRecyclerView == null) return
            if (mRecyclerView?.adapter == null) return

            val adapter = mRecyclerView?.adapter
            adapter?.let {
                if (it.itemCount > 8) {
                    // Probably the original adapter already modified
                    it.notifyDataSetChanged()
                    return
                }

                if (it is HomeAdapter) {
                    val index = it.wallpapersIndex
                    if (index >= 0 && index < it.itemCount) {
                        it.notifyItemChanged(index)
                    }
                }
            }
        }
    }
}
