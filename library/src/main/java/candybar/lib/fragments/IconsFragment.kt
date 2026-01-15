package candybar.lib.fragments

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.adapters.IconsAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.items.Icon
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.ViewHelper
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import java.lang.ref.WeakReference
import com.danimahardhika.android.helpers.core.DrawableHelper as CoreDrawableHelper

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
class IconsFragment : Fragment() {

    private lateinit var mNoBookmarksFoundView: View
    private lateinit var mRecyclerView: RecyclerView
    private var mAdapter: IconsAdapter? = null

    private var mIcons: MutableList<Icon> = mutableListOf()
    private var isBookmarksFragment = false
    private var prevIsEmpty = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_icons, container, false)
        mNoBookmarksFoundView = view.findViewById(R.id.no_bookmarks_found_container)
        mRecyclerView = view.findViewById(R.id.icons_grid)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mIcons = mutableListOf()
        val index = requireArguments().getInt(INDEX)
        if (index == -1) {
            mIcons = Database.get(requireActivity()).getBookmarkedIcons(requireActivity()) as MutableList<Icon>
            bookmarksIconFragment = WeakReference(this)
            isBookmarksFragment = true
            prevIsEmpty = mIcons.isEmpty()
        } else if (CandyBarMainActivity.sSections != null) {
            val section = CandyBarMainActivity.sSections?.get(index)
            if (section != null) {
                mIcons = section.icons.toMutableList()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val params = HashMap<String, Any>()
        params["section"] = "icons"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        setupViewVisibility()

        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.layoutManager = GridLayoutManager(
            activity,
            requireActivity().resources.getInteger(R.integer.icons_column_count)
        )

        FastScrollerBuilder(mRecyclerView)
            .useMd2Style()
            .build()

        (mNoBookmarksFoundView.findViewById<View>(R.id.bookmark_image) as ImageView)
            .setImageDrawable(
                CoreDrawableHelper.getTintedDrawable(
                    requireActivity(), R.drawable.ic_bookmark,
                    ColorHelper.getAttributeColor(requireActivity(), android.R.attr.textColorSecondary)
                )
            )

        mAdapter = IconsAdapter(requireActivity(), mIcons, this, isBookmarksFragment)
        mRecyclerView.adapter = mAdapter
        iconsAdapters.add(WeakReference(mAdapter))
    }

    private fun setupViewVisibility() {
        if (isBookmarksFragment && mIcons.isEmpty()) {
            mNoBookmarksFoundView.visibility = View.VISIBLE
            mRecyclerView.visibility = View.GONE
        } else {
            mNoBookmarksFoundView.visibility = View.GONE
            mRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        ViewHelper.resetSpanCount(
            mRecyclerView,
            requireActivity().resources.getInteger(R.integer.icons_column_count)
        )
    }

    fun refreshBookmarks() {
        if (isBookmarksFragment && isAdded) {
            mIcons = Database.get(requireActivity()).getBookmarkedIcons(requireActivity()) as MutableList<Icon>
            mAdapter?.setIcons(mIcons)
            setupViewVisibility()
        }
    }

    companion object {
        private const val INDEX = "index"
        private val iconsAdapters: MutableList<WeakReference<IconsAdapter?>> = ArrayList()
        private var bookmarksIconFragment: WeakReference<IconsFragment?> = WeakReference(null)

        fun newInstance(index: Int): IconsFragment {
            val fragment = IconsFragment()
            val bundle = Bundle()
            bundle.putInt(INDEX, index)
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun reloadIcons() {
            for (adapterRef in iconsAdapters) {
                adapterRef.get()?.reloadIcons()
            }
        }

        @JvmStatic
        fun reloadBookmarks() {
            bookmarksIconFragment.get()?.refreshBookmarks()
        }
    }
}
