package candybar.lib.fragments

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.adapters.LauncherAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.LauncherHelper
import candybar.lib.items.Icon
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import com.danimahardhika.android.helpers.core.utils.LogUtil
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
class ApplyFragment : Fragment() {

    private var mRecyclerView: RecyclerView? = null
    private var mAsyncTask: AsyncTaskBase? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_apply, container, false)
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
        params["section"] = "icon_apply"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        mRecyclerView?.itemAnimator = DefaultItemAnimator()
        mRecyclerView?.layoutManager = GridLayoutManager(activity, 1)

        if (CandyBarApplication.getConfiguration().applyGrid == CandyBarApplication.GridStyle.FLAT) {
            val padding = requireActivity().resources.getDimensionPixelSize(R.dimen.card_margin)
            mRecyclerView?.setPadding(padding, padding, 0, 0)
        }

        mAsyncTask = LaunchersLoader().executeOnThreadPool()
    }

    override fun onDestroy() {
        if (mAsyncTask != null) {
            mAsyncTask!!.cancel(true)
        }
        super.onDestroy()
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return try {
            val packageInfo = requireActivity().packageManager.getPackageInfo(
                pkg, PackageManager.GET_ACTIVITIES
            )
            packageInfo != null
        } catch (e: Exception) {
            false
        }
    }

    private fun getInstalledPackage(pkgs: Array<String>): String? {
        for (pkg in pkgs) {
            if (isPackageInstalled(pkg)) {
                return pkg
            }
        }
        return null
    }

    private fun shouldLauncherBeAdded(packageName: String): Boolean {
        if ("com.lge.launcher2" == packageName || "com.lge.launcher3" == packageName) {
            val id = resources.getIdentifier("theme_resources", "xml", requireActivity().packageName)
            return id > 0
        }
        if ("com.oppo.launcher" == packageName) {
            return (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) &&
                    (Build.MANUFACTURER.equals("OPPO", ignoreCase = true) ||
                            Build.MANUFACTURER.equals("realme", ignoreCase = true))
        }
        if ("com.android.launcher" == packageName) {
            return (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) &&
                    (Build.MANUFACTURER.equals("OnePlus", ignoreCase = true) ||
                            Build.MANUFACTURER.equals("OPPO", ignoreCase = true) ||
                            Build.MANUFACTURER.equals("realme", ignoreCase = true)) ||
                    (Build.VERSION.SDK_INT == Build.VERSION_CODES.R &&
                            Build.MANUFACTURER.equals("realme", ignoreCase = true))
        }
        return true
    }

    private inner class LaunchersLoader : AsyncTaskBase() {

        private var launchers: MutableList<Icon> = ArrayList()

        override fun preRun() {
            launchers = ArrayList()
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)

                    val dataLaunchers = LauncherHelper.LauncherType.entries.toTypedArray()
                    val showableLauncherNames = resources.getStringArray(R.array.dashboard_launchers)

                    val installed: MutableList<Icon> = ArrayList()
                    val supported: MutableList<Icon> = ArrayList()

                    val showable: MutableList<String> = ArrayList()

                    for (name in showableLauncherNames) {
                        val filteredName = name.lowercase().replace(" ", "_")
                        showable.add(filteredName)
                    }

                    for (value in dataLaunchers) {
                        if (value.name == null) continue
                        if (value.packages == null) continue

                        val lowercaseLauncherName = value.name.lowercase().replace(" ", "_")

                        if (!showable.contains(lowercaseLauncherName)) {
                            LogUtil.d("Launcher Excluded: $lowercaseLauncherName")
                            continue
                        }

                        val installedPackage = getInstalledPackage(value.packages)

                        val launcher = Icon(value.name, value.icon, value.packages[0])
                        if (shouldLauncherBeAdded(value.packages[0])) {
                            if (installedPackage != null) {
                                installed.add(launcher)
                                launcher.setPackageName(installedPackage)
                            } else supported.add(launcher)
                        }
                    }

                    try {
                        Collections.sort(installed, Icon.TitleComparator)
                    } catch (ignored: Exception) {
                    }

                    try {
                        Collections.sort(supported, Icon.TitleComparator)
                    } catch (ignored: Exception) {
                    }

                    if (installed.size == 1) {
                        launchers.add(
                            Icon(
                                resources.getString(R.string.apply_installed), -1, null
                            )
                        )
                    } else {
                        launchers.add(
                            Icon(
                                resources.getString(R.string.apply_installed_launchers), -3, null
                            )
                        )
                    }

                    launchers.addAll(installed)
                    launchers.add(
                        Icon(
                            resources.getString(R.string.apply_supported), -2, null
                        )
                    )
                    launchers.addAll(supported)

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
                mRecyclerView?.adapter = LauncherAdapter(requireActivity(), launchers)
            }
        }
    }
}
