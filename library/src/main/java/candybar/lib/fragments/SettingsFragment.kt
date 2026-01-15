package candybar.lib.fragments

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.adapters.SettingsAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.fragments.dialog.IntentChooserFragment
import candybar.lib.helpers.DrawableHelper
import candybar.lib.helpers.IconsHelper
import candybar.lib.helpers.LocaleHelper
import candybar.lib.helpers.RequestHelper
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.Request
import candybar.lib.items.Setting
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.listeners.RequestListener
import com.afollestad.materialdialogs.MaterialDialog
import com.danimahardhika.android.helpers.core.FileHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.io.File
import java.text.DecimalFormat
import java.text.NumberFormat

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
class SettingsFragment : Fragment() {

    private lateinit var mRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)
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
        params["section"] = "settings"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.layoutManager = LinearLayoutManager(activity)

        initSettings()
    }

    fun restorePurchases(
        productsId: List<String>, premiumRequestProductsId: Array<String>,
        premiumRequestProductsCount: IntArray
    ) {
        var index = -1
        for (productId in productsId) {
            for (i in premiumRequestProductsId.indices) {
                if (premiumRequestProductsId[i] == productId) {
                    index = i
                    break
                }
            }
            if (index > -1 && index < premiumRequestProductsCount.size) {
                val preferences = Preferences.get(requireActivity())
                if (!preferences.isPremiumRequest) {
                    preferences.premiumRequestProductId = productId
                    preferences.premiumRequestCount = premiumRequestProductsCount[index]
                    preferences.premiumRequestTotal = premiumRequestProductsCount[index]
                    preferences.isPremiumRequest = true
                }
            }
        }
        val message = if (index > -1)
            R.string.pref_premium_request_restore_success
        else
            R.string.pref_premium_request_restore_empty
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
    }

    private fun initSettings() {
        val settings: MutableList<Setting> = ArrayList()

        val cache = FileHelper.getDirectorySize(requireActivity().cacheDir).toDouble() / FileHelper.MB
        val formatter: NumberFormat = DecimalFormat("#0.00")
        val resources = requireActivity().resources

        settings.add(
            Setting(
                R.drawable.ic_toolbar_storage,
                resources.getString(R.string.pref_data_header),
                "", "", "", Setting.Type.HEADER
            )
        )

        settings.add(
            Setting(
                -1, "",
                resources.getString(R.string.pref_data_cache),
                resources.getString(R.string.pref_data_cache_desc),
                resources.getString(
                    R.string.pref_data_cache_size,
                    formatter.format(cache) + " MB"
                ),
                Setting.Type.CACHE
            )
        )

        if (resources.getBoolean(R.bool.enable_icon_request) ||
            Preferences.get(requireActivity()).isPremiumRequestEnabled &&
            !resources.getBoolean(R.bool.enable_icon_request_limit)
        ) {
            settings.add(
                Setting(
                    -1, "",
                    resources.getString(R.string.pref_data_request),
                    resources.getString(R.string.pref_data_request_desc),
                    "", Setting.Type.ICON_REQUEST
                )
            )
        }

        if (Preferences.get(requireActivity()).isPremiumRequestEnabled) {
            settings.add(
                Setting(
                    R.drawable.ic_toolbar_premium_request,
                    resources.getString(R.string.pref_premium_request_header),
                    "", "", "", Setting.Type.HEADER
                )
            )

            if (requireActivity().resources.getBoolean(R.bool.enable_restore_purchases)) {
                settings.add(
                    Setting(
                        -1, "",
                        resources.getString(R.string.pref_premium_request_restore),
                        resources.getString(R.string.pref_premium_request_restore_desc),
                        "", Setting.Type.RESTORE
                    )
                )
            }

            settings.add(
                Setting(
                    -1, "",
                    resources.getString(R.string.pref_premium_request_rebuild),
                    resources.getString(R.string.pref_premium_request_rebuild_desc),
                    "", Setting.Type.PREMIUM_REQUEST
                )
            )
        }

        if (CandyBarApplication.getConfiguration().isDashboardThemingEnabled) {
            settings.add(
                Setting(
                    R.drawable.ic_toolbar_theme,
                    resources.getString(R.string.pref_theme_header),
                    "", "", "", Setting.Type.HEADER
                )
            )

            settings.add(
                Setting(
                    -1, "",
                    Preferences.get(requireActivity()).theme.displayName(requireActivity()),
                    "", "", Setting.Type.THEME
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                settings.add(
                    Setting(
                        -1, "", resources.getString(R.string.theme_name_material_you), "", "", Setting.Type.MATERIAL_YOU
                    )
                )
            }
        }

        if (CandyBarApplication.getConfiguration().isNotificationEnabled) {
            settings.add(
                Setting(
                    R.drawable.ic_toolbar_notifications,
                    resources.getString(R.string.pref_notifications_header),
                    "", "", "", Setting.Type.HEADER
                )
            )

            settings.add(
                Setting(
                    -1, "", resources.getString(R.string.pref_notifications), "", "", Setting.Type.NOTIFICATIONS
                )
            )
        }

        settings.add(
            Setting(
                R.drawable.ic_toolbar_language,
                resources.getString(R.string.pref_language_header),
                "", "", "", Setting.Type.HEADER
            )
        )

        val language = LocaleHelper.getCurrentLanguage(requireActivity())
        settings.add(
            Setting(
                -1, "",
                language.name,
                "", "", Setting.Type.LANGUAGE
            )
        )

        settings.add(
            Setting(
                R.drawable.ic_toolbar_others,
                resources.getString(R.string.pref_others_header),
                "", "", "", Setting.Type.HEADER
            )
        )

        settings.add(
            Setting(
                -1, "",
                resources.getString(R.string.pref_others_changelog),
                "", "", Setting.Type.CHANGELOG
            )
        )

        if (resources.getBoolean(R.bool.enable_apply)) {
            settings.add(
                Setting(
                    -1, "",
                    resources.getString(R.string.pref_others_report_bugs),
                    "", "", Setting.Type.REPORT_BUGS
                )
            )
        }

        if (resources.getBoolean(R.bool.show_intro)) {
            settings.add(
                Setting(
                    -1, "",
                    resources.getString(R.string.pref_others_reset_tutorial),
                    "", "", Setting.Type.RESET_TUTORIAL
                )
            )
        }

        mRecyclerView.adapter = SettingsAdapter(requireActivity(), settings)
    }

    fun rebuildPremiumRequest() {
        PremiumRequestRebuilder().execute()
    }

    private inner class PremiumRequestRebuilder : AsyncTaskBase() {

        private var dialog: MaterialDialog? = null
        private var isPacific = false
        private var pacificApiKey: String? = null
        private var isCustom = false
        private var isPremium = false
        private var requests: List<Request>? = null
        private var errorMessage: String? = null

        override fun preRun() {
            isPacific = RequestHelper.isPremiumPacificEnabled(requireActivity())
            pacificApiKey = RequestHelper.getPremiumPacificApiKey(requireActivity())
            isCustom = RequestHelper.isPremiumCustomEnabled(requireActivity())
            isPremium = true

            dialog = MaterialDialog.Builder(requireActivity())
                .typeface(TypefaceHelper.getMedium(requireActivity()), TypefaceHelper.getRegular(requireActivity()))
                .content(R.string.premium_request_rebuilding)
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
                    Thread.sleep(1)
                    val directory = requireActivity().cacheDir
                    requests = Database.get(requireActivity()).getPremiumRequest(null)
                    val currentRequests = requests ?: return true
                    if (currentRequests.isEmpty()) return true

                    val files: MutableList<String> = ArrayList()

                    for (request in currentRequests) {
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

                    if (isPacific) {
                        errorMessage = RequestHelper.sendPacificRequest(currentRequests, files, directory, pacificApiKey ?: "")
                        if (errorMessage == null) {
                            for (request in currentRequests) {
                                Database.get(requireActivity()).addRequest(null, request)
                                Database.get(requireActivity()).addPremiumRequest(null, request)
                            }
                        }
                        return errorMessage == null
                    } else if (isCustom) {
                        errorMessage = RequestHelper.sendCustomRequest(currentRequests, isPremium)
                        if (errorMessage == null) {
                            for (request in currentRequests) {
                                Database.get(requireActivity()).addRequest(null, request)
                                Database.get(requireActivity()).addPremiumRequest(null, request)
                            }
                        }
                        return errorMessage == null
                    } else {
                        val appFilter = RequestHelper.buildXml(requireActivity(), currentRequests, RequestHelper.XmlType.APPFILTER)
                        val appMap = RequestHelper.buildXml(requireActivity(), currentRequests, RequestHelper.XmlType.APPMAP)
                        val themeResources = RequestHelper.buildXml(requireActivity(), currentRequests, RequestHelper.XmlType.THEME_RESOURCES)

                        if (appFilter != null) {
                            files.add(appFilter.toString())
                        }

                        if (appMap != null) {
                            files.add(appMap.toString())
                        }

                        if (themeResources != null) {
                            files.add(themeResources.toString())
                        }

                        CandyBarApplication.sZipPath = FileHelper.createZip(
                            files, File(
                                directory.toString(),
                                RequestHelper.getGeneratedZipName(RequestHelper.REBUILD_ZIP)
                            )
                        )
                    }
                    return true
                } catch (e: Exception) {
                    errorMessage = e.toString()
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
            dialog = null

            if (ok) {
                if (requests!!.isEmpty()) {
                    Toast.makeText(
                        requireActivity(), R.string.premium_request_rebuilding_empty,
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                if (isPacific || isCustom) {
                    val toastText = if (isPacific) R.string.request_pacific_success else R.string.request_custom_success
                    Toast.makeText(requireActivity(), toastText, Toast.LENGTH_LONG).show()
                    (requireActivity() as RequestListener).onRequestBuilt(null, IntentChooserFragment.REBUILD_ICON_REQUEST)
                } else {
                    IntentChooserFragment.showIntentChooserDialog(
                        requireActivity().supportFragmentManager, IntentChooserFragment.REBUILD_ICON_REQUEST
                    )
                }
            } else {
                Toast.makeText(requireActivity(), "Failed: $errorMessage", Toast.LENGTH_LONG).show()
            }
        }
    }
}
