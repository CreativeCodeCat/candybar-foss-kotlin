package candybar.lib.fragments

import android.content.Intent
import android.content.res.Configuration
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.adapters.RequestAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.fragments.dialog.IntentChooserFragment
import candybar.lib.helpers.DrawableHelper
import candybar.lib.helpers.IconsHelper
import candybar.lib.helpers.RequestHelper
import candybar.lib.helpers.TapIntroHelper
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.Request
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.listeners.RequestListener
import com.afollestad.materialdialogs.MaterialDialog
import com.danimahardhika.android.helpers.animation.AnimationHelper
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.FileHelper
import com.danimahardhika.android.helpers.core.ViewHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
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
class RequestFragment : Fragment(), View.OnClickListener {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mFab: FloatingActionButton
    private lateinit var mFastScroll: RecyclerFastScroller
    private lateinit var mProgress: ProgressBar

    private var mMenuItem: MenuItem? = null
    private var mAdapter: RequestAdapter? = null
    private var mManager: StaggeredGridLayoutManager? = null
    private var mAsyncTask: AsyncTaskBase? = null

    private var noEmailClientError = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_request, container, false)
        mRecyclerView = view.findViewById(R.id.request_list)
        mFab = view.findViewById(R.id.fab)
        mFastScroll = view.findViewById(R.id.fastscroll)
        mProgress = view.findViewById(R.id.progress)

        ViewCompat.setOnApplyWindowInsetsListener(mFab) { v, insets ->
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom + 50
            WindowInsetsCompat.CONSUMED
        }

        if (!Preferences.get(requireActivity()).isToolbarShadowEnabled) {
            val shadow = view.findViewById<View>(R.id.shadow)
            shadow?.visibility = View.GONE
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val params = HashMap<String, Any>()
        params["section"] = "icon_request"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        resetRecyclerViewPadding(resources.configuration.orientation)

        mProgress.indeterminateDrawable.colorFilter = PorterDuffColorFilter(
            ColorHelper.getAttributeColor(requireActivity(), com.google.android.material.R.attr.colorSecondary),
            PorterDuff.Mode.SRC_IN
        )

        val color = ColorHelper.getTitleTextColor(
            ColorHelper.getAttributeColor(requireActivity(), com.google.android.material.R.attr.colorSecondary)
        )
        val tintedDrawable = ResourcesCompat.getDrawable(requireActivity().resources, R.drawable.ic_fab_send, null)
        tintedDrawable?.let {
            it.mutate().colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            mFab.setImageDrawable(it)
        }
        mFab.setOnClickListener(this)

        if (!Preferences.get(requireActivity()).isFabShadowEnabled) {
            mFab.compatElevation = 0f
        }

        val itemAnimator = DefaultItemAnimator()
        itemAnimator.changeDuration = 0
        mRecyclerView.itemAnimator = itemAnimator
        mManager = StaggeredGridLayoutManager(
            requireActivity().resources.getInteger(R.integer.request_column_count),
            StaggeredGridLayoutManager.VERTICAL
        )
        mRecyclerView.layoutManager = mManager

        CBViewHelper.setFastScrollColor(mFastScroll)
        mFastScroll.attachRecyclerView(mRecyclerView)

        mAsyncTask = MissingAppsLoader().execute()
    }

    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_request, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                val id = item.itemId
                if (id == R.id.menu_select_all) {
                    mMenuItem = item
                    val adapter = mAdapter ?: return false
                    if (adapter.selectAll()) {
                        item.setIcon(R.drawable.ic_toolbar_select_all_selected)
                        return true
                    }

                    item.setIcon(R.drawable.ic_toolbar_select_all)
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        resetRecyclerViewPadding(newConfig.orientation)
        if (mAsyncTask != null) return

        val positions = mManager?.findFirstVisibleItemPositions(null)

        val selectedItems = mAdapter?.selectedItemsArray ?: SparseBooleanArray()
        mRecyclerView.let {
            ViewHelper.resetSpanCount(
                it,
                requireActivity().resources.getInteger(R.integer.request_column_count)
            )

            mAdapter = RequestAdapter(
                requireActivity(),
                CandyBarMainActivity.sMissedApps ?: ArrayList(),
                mManager?.spanCount ?: 1
            )
            it.adapter = mAdapter
            mAdapter?.selectedItemsArray = selectedItems

            if (positions != null && positions.isNotEmpty())
                it.scrollToPosition(positions[0])
        }
    }


    override fun onDestroy() {
        if (mAsyncTask != null) {
            mAsyncTask!!.cancel(true)
        }
        super.onDestroy()
    }


    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.fab) {
            val adapter = mAdapter ?: return

            val selected = adapter.selectedItemsSize
            if (selected > 0) {
                val requestLimit = resources.getBoolean(R.bool.enable_icon_request_limit)
                val iconRequest = resources.getBoolean(R.bool.enable_icon_request)
                val premiumRequest = resources.getBoolean(R.bool.enable_premium_request)

                if (Preferences.get(requireActivity()).isPremiumRequest) {
                    val count = Preferences.get(requireActivity()).premiumRequestCount
                    if (selected > count) {
                        RequestHelper.showPremiumRequestLimitDialog(requireActivity(), selected)
                        return
                    }

                    if (!RequestHelper.isReadyToSendPremiumRequest(requireActivity())) return

                    return
                }

                if (!iconRequest && premiumRequest) {
                    RequestHelper.showPremiumRequestRequired(requireActivity())
                    return
                }

                if (requestLimit) {
                    val limit = requireActivity().resources.getInteger(R.integer.icon_request_limit)
                    val used = Preferences.get(requireActivity()).regularRequestUsed
                    if (selected > (limit - used)) {
                        RequestHelper.showIconRequestLimitDialog(requireActivity())
                        return
                    }
                }

                val configHandler = CandyBarApplication.getConfiguration().configHandler
                if (requireActivity().resources.getBoolean(R.bool.json_check_before_request) &&
                    configHandler?.configJson(requireActivity())?.isNotEmpty() == true
                ) {
                    mAsyncTask = CheckConfig().executeOnThreadPool()
                } else {
                    mAsyncTask = RequestLoader().executeOnThreadPool()
                }

            } else {
                Toast.makeText(
                    activity, R.string.request_not_selected,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun resetRecyclerViewPadding(orientation: Int) {
        if (mRecyclerView == null) return

        var padding = 0
        val tabletMode = resources.getBoolean(com.danimahardhika.android.helpers.core.R.bool.android_helpers_tablet_mode)
        if (tabletMode || orientation == Configuration.ORIENTATION_LANDSCAPE) {
            padding = requireActivity().resources.getDimensionPixelSize(R.dimen.content_padding)

            if (CandyBarApplication.getConfiguration().requestStyle == CandyBarApplication.Style.PORTRAIT_FLAT_LANDSCAPE_FLAT) {
                padding = requireActivity().resources.getDimensionPixelSize(R.dimen.card_margin)
            }
        }

        val size = requireActivity().resources.getDimensionPixelSize(R.dimen.fab_size)
        val marginGlobal = requireActivity().resources.getDimensionPixelSize(R.dimen.fab_margin_global)

        mRecyclerView.setPadding(padding, padding, 0, size + (marginGlobal * 2))
    }

    fun prepareRequest() {
        if (mAsyncTask != null) return
        mAsyncTask = RequestLoader().executeOnThreadPool()
    }

    fun refreshIconRequest() {
        if (mAdapter == null) {
            sSelectedRequests = null
            return
        }

        if (sSelectedRequests == null) {
            mAdapter!!.notifyItemChanged(0)
            return
        }

        for (integer in sSelectedRequests!!) {
            mAdapter!!.setRequested(integer, true)
        }

        mAdapter!!.notifyDataSetChanged()
        sSelectedRequests = null
    }

    private inner class MissingAppsLoader : AsyncTaskBase() {

        private var requests: MutableList<Request>? = null

        override fun preRun() {
            if (CandyBarMainActivity.sMissedApps == null) {
                mProgress.visibility = View.VISIBLE
            }
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)
                    if (CandyBarMainActivity.sMissedApps == null) {
                        CandyBarMainActivity.sMissedApps = RequestHelper.getMissingApps(requireActivity()).toMutableList()
                    }

                    requests = CandyBarMainActivity.sMissedApps
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
                setupMenu()
                mAdapter = RequestAdapter(
                    requireActivity(),
                    requests ?: ArrayList(), mManager?.spanCount ?: 1
                )
                mRecyclerView.adapter = mAdapter

                AnimationHelper.show(mFab)
                    .interpolator(LinearOutSlowInInterpolator())
                    .start()

                if (requireActivity().resources.getBoolean(R.bool.show_intro)) {
                    TapIntroHelper.showRequestIntro(requireActivity(), mRecyclerView)
                }
            } else {
                mRecyclerView.adapter = null
                Toast.makeText(requireActivity(), R.string.request_appfilter_failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    private inner class RequestLoader : AsyncTaskBase() {

        private var dialog: MaterialDialog? = null
        private var isPacific = false
        private var pacificApiKey: String? = null
        private var isCustom = false
        private var isPremium = false
        private var errorMessage: String? = null

        override fun preRun() {
            if (Preferences.get(requireActivity()).isPremiumRequest) {
                isPremium = true
                isCustom = RequestHelper.isPremiumCustomEnabled(requireActivity())
                isPacific = RequestHelper.isPremiumPacificEnabled(requireActivity())
                pacificApiKey = RequestHelper.getPremiumPacificApiKey(requireActivity())
            } else {
                isPremium = false
                isCustom = RequestHelper.isRegularCustomEnabled(requireActivity())
                isPacific = RequestHelper.isRegularPacificEnabled(requireActivity())
                pacificApiKey = RequestHelper.getRegularPacificApiKey(requireActivity())
            }

            dialog = MaterialDialog.Builder(requireActivity())
                .typeface(TypefaceHelper.getMedium(requireActivity()), TypefaceHelper.getRegular(requireActivity()))
                .content(R.string.request_building)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .build()

            dialog?.show()
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(2)

                    sSelectedRequests = mAdapter?.selectedItems
                    val requests = mAdapter?.selectedApps

                    val directory = requireActivity().cacheDir
                    val files: MutableList<String> = ArrayList()

                    if (requests != null) {
                        for (request in requests) {
                            val drawable = DrawableHelper.getPackageIcon(requireActivity(), request.activity!!)
                            if (drawable != null) {
                                val icon = IconsHelper.saveIcon(
                                    files, directory, drawable,
                                    if (isPacific) request.packageName else RequestHelper.fixNameForRequest(request.name),
                                    { fileName -> request.fileName = fileName }
                                )
                                if (icon != null) files.add(icon)
                                if (isCustom) {
                                    request.iconBase64 = DrawableHelper.getReqIconBase64(drawable)
                                }
                            }
                        }
                    }

                    if (isPacific) {
                        errorMessage = RequestHelper.sendPacificRequest(requests ?: emptyList(), files, directory, pacificApiKey ?: "")
                        if (errorMessage == null) {
                            if (requests != null) {
                                for (request in requests) {
                                    Database.get(requireActivity()).addRequest(null, request)
                                }
                            }
                        }
                        return errorMessage == null
                    } else if (isCustom) {
                        errorMessage = RequestHelper.sendCustomRequest(requests ?: emptyList(), isPremium)
                        if (errorMessage == null) {
                            if (requests != null) {
                                for (request in requests) {
                                    Database.get(requireActivity()).addRequest(null, request)
                                }
                            }
                        }
                        return errorMessage == null
                    } else {
                        val nonMailingAppSend = resources.getBoolean(R.bool.enable_non_mail_app_request)
                        val intent: Intent

                        if (!nonMailingAppSend) {
                            intent = Intent(Intent.ACTION_SENDTO)
                            intent.data = Uri.parse("mailto:")
                        } else {
                            intent = Intent(Intent.ACTION_SEND)
                            intent.type = "application/zip"
                        }

                        val resolveInfos = requireActivity().packageManager
                            .queryIntentActivities(intent, 0)
                        if (resolveInfos.isEmpty()) {
                            noEmailClientError = true
                            return false
                        }

                        val appFilter = RequestHelper.buildXml(requireActivity(), requests!!, RequestHelper.XmlType.APPFILTER)
                        val appMap = RequestHelper.buildXml(requireActivity(), requests, RequestHelper.XmlType.APPMAP)
                        val themeResources = RequestHelper.buildXml(requireActivity(), requests, RequestHelper.XmlType.THEME_RESOURCES)

                        if (appFilter != null) files.add(appFilter.toString())

                        if (appMap != null) files.add(appMap.toString())

                        if (themeResources != null) files.add(themeResources.toString())

                        CandyBarApplication.sZipPath = FileHelper.createZip(
                            files, File(
                                directory.toString(),
                                RequestHelper.getGeneratedZipName(RequestHelper.ZIP)
                            )
                        )
                    }
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

            dialog?.dismiss()
            mAsyncTask = null
            dialog = null

            if (ok) {
                if (isPacific || isCustom) {
                    val toastText = if (isPacific) R.string.request_pacific_success else R.string.request_custom_success
                    Toast.makeText(requireActivity(), toastText, Toast.LENGTH_LONG).show()
                    (requireActivity() as RequestListener).onRequestBuilt(null, IntentChooserFragment.ICON_REQUEST)
                } else {
                    IntentChooserFragment.showIntentChooserDialog(
                        requireActivity().supportFragmentManager,
                        IntentChooserFragment.ICON_REQUEST
                    )
                }
                mAdapter?.resetSelectedItems()
                if (mMenuItem != null) mMenuItem!!.setIcon(R.drawable.ic_toolbar_select_all)
            } else {
                if (isPacific || isCustom) {
                    val content = if (isPacific) R.string.request_pacific_error else R.string.request_custom_error
                    MaterialDialog.Builder(requireActivity())
                        .typeface(TypefaceHelper.getMedium(requireActivity()), TypefaceHelper.getRegular(requireActivity()))
                        .content(content, "\"" + errorMessage + "\"")
                        .cancelable(true)
                        .canceledOnTouchOutside(false)
                        .positiveText(R.string.close)
                        .build()
                        .show()
                } else if (noEmailClientError) {
                    Toast.makeText(
                        requireActivity(), R.string.no_email_app,
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        requireActivity(), R.string.request_build_failed,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private inner class CheckConfig : AsyncTaskBase() {

        private var dialog: MaterialDialog? = null
        private var canRequest = true
        private var updateUrl: String? = null

        override fun preRun() {
            dialog = MaterialDialog.Builder(requireActivity())
                .typeface(TypefaceHelper.getMedium(requireActivity()), TypefaceHelper.getRegular(requireActivity()))
                .content(R.string.request_fetching_data)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .build()

            dialog?.show()
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                val configHandler = CandyBarApplication.getConfiguration().configHandler
                val configJsonUrl = configHandler?.configJson(requireActivity())
                if (configJsonUrl == null) return false
                val bufferedReader: BufferedReader?

                try {
                    val urlConnection = URL(configJsonUrl).openConnection()
                    bufferedReader = BufferedReader(InputStreamReader(urlConnection.getInputStream()))

                    val stringBuilder = StringBuilder()
                    var line: String?
                    while (bufferedReader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                    bufferedReader.close()

                    val packageInfo = requireActivity().packageManager
                        .getPackageInfo(requireActivity().packageName, 0)
                    val configJson = JSONObject(stringBuilder.toString())
                    updateUrl = if (configJson.isNull("url")) {
                        // Default to Play Store
                        "https://play.google.com/store/apps/details?id=" + packageInfo.packageName
                    } else {
                        configJson.getString("url")
                    }

                    val disableRequestObj = configJson.getJSONObject("disableRequest")
                    val disableRequestBelow = disableRequestObj.optLong("below", 0)
                    val disableRequestOn = disableRequestObj.optString("on", "")
                    val appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        packageInfo.longVersionCode else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }

                    if ((appVersionCode < disableRequestBelow) ||
                        disableRequestOn.contains("\\b$appVersionCode\\b".toRegex())
                    ) {
                        canRequest = false
                    }

                    return true
                } catch (ex: Exception) {
                    LogUtil.e("Error loading Configuration JSON " + Log.getStackTraceString(ex))
                }
            }
            return false
        }

        override fun postRun(ok: Boolean) {
            if (activity == null) return
            if (requireActivity().isFinishing) return

            dialog?.dismiss()
            dialog = null

            if (ok) {
                if (!canRequest) {
                    MaterialDialog.Builder(requireActivity())
                        .typeface(TypefaceHelper.getMedium(requireActivity()), TypefaceHelper.getRegular(requireActivity()))
                        .content(R.string.request_app_disabled)
                        .negativeText(R.string.close)
                        .positiveText(R.string.update)
                        .onPositive { _, _ ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                            requireActivity().startActivity(intent)
                        }
                        .cancelable(false)
                        .canceledOnTouchOutside(false)
                        .build()
                        .show()

                    mAdapter?.resetSelectedItems()
                    if (mMenuItem != null) mMenuItem!!.setIcon(R.drawable.ic_toolbar_select_all)
                } else {
                    mAsyncTask = RequestLoader().executeOnThreadPool()
                }
            } else {
                MaterialDialog.Builder(requireActivity())
                    .typeface(TypefaceHelper.getMedium(requireActivity()), TypefaceHelper.getRegular(requireActivity()))
                    .content(R.string.unable_to_load_config)
                    .canceledOnTouchOutside(false)
                    .positiveText(R.string.close)
                    .build()
                    .show()
            }
        }
    }

    companion object {
        @JvmField
        var sSelectedRequests: MutableList<Int>? = null
    }
}
