package candybar.lib.tasks

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.helpers.LocaleHelper
import candybar.lib.helpers.RequestHelper
import candybar.lib.items.Request
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.Extras
import candybar.lib.utils.listeners.HomeListener
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.Locale

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

class IconRequestTask(context: Context) : AsyncTaskBase() {

    private val mContext: WeakReference<Context> = WeakReference(context)
    private var mError: Extras.Error? = null

    override fun run(): Boolean {
        if (!isCancelled) {
            try {
                Thread.sleep(1)
                val context = mContext.get() ?: return false
                if (context.resources.getBoolean(R.bool.enable_icon_request) ||
                    context.resources.getBoolean(R.bool.enable_premium_request)
                ) {
                    val requests = mutableListOf<Request>()
                    val appFilter = RequestHelper.getAppFilter(context, RequestHelper.Key.ACTIVITY)
                    if (appFilter.isEmpty()) {
                        mError = Extras.Error.APPFILTER_NULL
                        return false
                    }

                    val packageManager = context.packageManager

                    val intent = Intent(Intent.ACTION_MAIN)
                    intent.addCategory(Intent.CATEGORY_LAUNCHER)
                    val installedApps = packageManager.queryIntentActivities(
                        intent, PackageManager.GET_RESOLVED_FILTER
                    )
                    if (installedApps.isEmpty()) {
                        mError = Extras.Error.INSTALLED_APPS_NULL
                        return false
                    }

                    CandyBarMainActivity.sInstalledAppsCount = installedApps.size

                    try {
                        Collections.sort(
                            installedApps,
                            ResolveInfo.DisplayNameComparator(packageManager)
                        )
                    } catch (ignored: Exception) {
                    }

                    for (app in installedApps) {
                        val packageName = app.activityInfo.packageName
                        val activity = packageName + "/" + app.activityInfo.name

                        val value = appFilter[activity]

                        if (value == null) {
                            var name = LocaleHelper.getOtherAppLocaleName(context, Locale("en"), activity)
                            if (name == null) {
                                name = app.activityInfo.loadLabel(packageManager).toString()
                            }

                            val requested = Database.get(context).isRequested(activity)
                            val request = Request.Builder()
                                .name(name)
                                .packageName(app.activityInfo.packageName)
                                .activity(activity)
                                .requested(requested)
                                .build()

                            if (CandyBarApplication.getConfiguration().filterRequestHandler
                                    ?.filterRequest(request) == true
                            ) {
                                requests.add(request)
                            }
                        }
                    }

                    CandyBarMainActivity.sMissedApps = requests
                }
                return true
            } catch (e: Exception) {
                CandyBarMainActivity.sMissedApps = null
                mError = Extras.Error.DATABASE_ERROR
                LogUtil.e(Log.getStackTraceString(e))
                return false
            }
        }
        return false
    }

    override fun postRun(ok: Boolean) {
        val context = mContext.get() ?: return
        if ((context as? AppCompatActivity)?.isFinishing == true) return

        if (ok) {
            val fm = (context as? AppCompatActivity)?.supportFragmentManager ?: return

            val fragment = fm.findFragmentByTag("home") ?: return

            val listener = fragment as? HomeListener
            listener?.onHomeDataUpdated(null)
        } else {
            mError?.showToast(context)
        }
    }
}
