package candybar.lib.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.adapters.PresetsAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.helpers.ViewHelper.setFastScrollColor
import candybar.lib.items.Preset
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import com.bumptech.glide.Glide
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.danimahardhika.cafebar.CafeBar
import com.danimahardhika.cafebar.CafeBarTheme
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import java.io.IOException

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
class PresetsFragment : Fragment() {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mProgress: ProgressBar
    private lateinit var mFastScroll: RecyclerFastScroller

    private var mAsyncTask: AsyncTaskBase? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            CafeBar.builder(requireActivity())
                .theme(CafeBarTheme.Custom(ColorHelper.getAttributeColor(requireActivity(), R.attr.cb_cardBackground)))
                .floating(true)
                .fitSystemWindow()
                .duration(CafeBar.Duration.MEDIUM)
                .typeface(TypefaceHelper.getRegular(requireActivity()), TypefaceHelper.getBold(requireActivity()))
                .content(R.string.presets_storage_permission)
                .show()
        }
        mAsyncTask = PresetsLoader().execute()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_presets, container, false)
        mRecyclerView = view.findViewById(R.id.presets_grid)
        mProgress = view.findViewById(R.id.progress)
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
        params["section"] = "presets"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        ViewCompat.setNestedScrollingEnabled(mRecyclerView, false)

        mProgress.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
            ColorHelper.getAttributeColor(requireActivity(), com.google.android.material.R.attr.colorSecondary),
            PorterDuff.Mode.SRC_IN
        )

        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.setHasFixedSize(false)
        mRecyclerView.layoutManager = GridLayoutManager(
            activity,
            requireActivity().resources.getInteger(R.integer.presets_column_count)
        )

        setFastScrollColor(mFastScroll)
        mFastScroll.attachRecyclerView(mRecyclerView)

        if (ContextCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            mAsyncTask = PresetsLoader().execute()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        resetSpanSizeLookUp()
    }

    override fun onDestroy() {
        if (mAsyncTask != null) mAsyncTask!!.cancel(true)
        val activity = activity
        if (activity != null) Glide.get(activity).clearMemory()
        setHasOptionsMenu(false)
        super.onDestroy()
    }

    private fun resetSpanSizeLookUp() {
        val column = requireActivity().resources.getInteger(R.integer.presets_column_count)
        val adapter = mRecyclerView.adapter as PresetsAdapter? ?: return
        val manager = mRecyclerView.layoutManager as GridLayoutManager? ?: return

        try {
            manager.spanCount = column

            manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return if (adapter.isHeader(position)) column else 1
                }
            }
        } catch (ignored: Exception) {
        }
    }

    private inner class PresetsLoader : AsyncTaskBase() {

        private val presets: MutableList<Preset> = ArrayList()

        @Throws(IOException::class)
        private fun loadPresets(sectionName: String, directory: String): List<Preset> {
            val presets: MutableList<Preset> = ArrayList()
            presets.add(Preset("", sectionName))
            val list = requireActivity().assets.list(directory)
            if (list != null) {
                for (item in list) {
                    presets.add(Preset("$directory/$item", null))
                }
            }
            if (presets.size == 1) presets.clear()
            return presets
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)

                    presets.addAll(loadPresets("Komponents", "komponents"))
                    presets.addAll(loadPresets("Lockscreens", "lockscreens"))
                    presets.addAll(loadPresets("Wallpapers", "wallpapers"))
                    presets.addAll(loadPresets("Widgets", "widgets"))

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
            mProgress.visibility = View.GONE

            if (ok) {
                mRecyclerView.adapter = PresetsAdapter(requireActivity(), presets)
                resetSpanSizeLookUp()
            } else {
                Toast.makeText(
                    activity, R.string.presets_load_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
