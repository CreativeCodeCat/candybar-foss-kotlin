package candybar.lib.fragments

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
import candybar.lib.adapters.AboutAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.preferences.Preferences
import com.danimahardhika.android.helpers.core.ViewHelper

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
class AboutFragment : Fragment() {

    private var mRecyclerView: RecyclerView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_about, container, false)
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
        params["section"] = "about"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        resetRecyclerViewPadding(requireActivity().resources.configuration.orientation)
        mRecyclerView?.itemAnimator = DefaultItemAnimator()

        val spanCount = requireActivity().resources.getInteger(R.integer.about_column_count)
        mRecyclerView?.layoutManager = StaggeredGridLayoutManager(
            spanCount, StaggeredGridLayoutManager.VERTICAL
        )
        mRecyclerView?.adapter = AboutAdapter(requireActivity(), spanCount)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        resetRecyclerViewPadding(newConfig.orientation)
        mRecyclerView?.let {
            ViewHelper.resetSpanCount(
                it,
                requireActivity().resources.getInteger(R.integer.about_column_count)
            )

            val manager = it.layoutManager as StaggeredGridLayoutManager?
            manager?.let { m ->
                it.adapter = AboutAdapter(requireActivity(), m.spanCount)
            }
        }
    }

    private fun resetRecyclerViewPadding(orientation: Int) {
        if (mRecyclerView == null) return

        var padding = 0
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            padding = requireActivity().resources.getDimensionPixelSize(R.dimen.content_padding)

            if (CandyBarApplication.getConfiguration().aboutStyle == CandyBarApplication.Style.PORTRAIT_FLAT_LANDSCAPE_FLAT) {
                padding = requireActivity().resources.getDimensionPixelSize(R.dimen.card_margin)
            }
        }

        mRecyclerView?.setPadding(padding, padding, 0, 150)
    }
}
