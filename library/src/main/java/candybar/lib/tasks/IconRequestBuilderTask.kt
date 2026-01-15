package candybar.lib.tasks

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.fragments.RequestFragment
import candybar.lib.fragments.dialog.IntentChooserFragment
import candybar.lib.helpers.DeviceHelper
import candybar.lib.items.Request
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.Extras
import candybar.lib.utils.listeners.RequestListener
import com.danimahardhika.android.helpers.core.FileHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.io.File
import java.lang.ref.WeakReference

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

class IconRequestBuilderTask(context: Context, callback: IconRequestBuilderCallback?) : AsyncTaskBase() {

    private val mContext: WeakReference<Context> = WeakReference(context)
    private val mCallback: WeakReference<IconRequestBuilderCallback>? = if (callback != null) WeakReference(callback) else null
    private var mEmailBody: String? = null
    private var mError: Extras.Error? = null

    override fun run(): Boolean {
        if (!isCancelled) {
            try {
                Thread.sleep(1)
                val context = mContext.get() ?: return false
                val selectedRequests = RequestFragment.sSelectedRequests ?: run {
                    mError = Extras.Error.ICON_REQUEST_NULL
                    return false
                }

                val requestProperty = CandyBarApplication.sRequestProperty ?: run {
                    mError = Extras.Error.ICON_REQUEST_PROPERTY_NULL
                    return false
                }

                val componentName = requestProperty.componentName ?: run {
                    mError = Extras.Error.ICON_REQUEST_PROPERTY_COMPONENT_NULL
                    return false
                }

                val stringBuilder = StringBuilder()
                stringBuilder.append(DeviceHelper.getDeviceInfo(context))

                if (Preferences.get(context).isPremiumRequest) {
                    requestProperty.orderId?.let {
                        stringBuilder.append("Order Id: ").append(it)
                    }

                    requestProperty.productId?.let {
                        stringBuilder.append("\r\nProduct Id: ").append(it)
                    }
                }

                val requestsForGenerator = mutableListOf<Request>()
                val emailBodyGenerator = CandyBarApplication.getConfiguration().emailBodyGenerator
                val emailBodyGeneratorEnabled = emailBodyGenerator != null

                val missedApps = CandyBarMainActivity.sMissedApps ?: return false

                for (i in selectedRequests.indices) {
                    val request = missedApps[selectedRequests[i]]
                    Database.get(context).addRequest(null, request)

                    if (Preferences.get(context).isPremiumRequest) {
                        val premiumRequest = Request.Builder()
                            .name(request.name)
                            .activity(request.activity)
                            .productId(requestProperty.productId)
                            .orderId(requestProperty.orderId)
                            .build()
                        Database.get(context).addPremiumRequest(null, premiumRequest)
                    }

                    if (CandyBarApplication.getConfiguration().isIncludeIconRequestToEmailBody) {
                        if (emailBodyGeneratorEnabled) {
                            requestsForGenerator.add(request)
                        } else {
                            stringBuilder.append("\r\n\r\n")
                                .append(request.name)
                                .append("\r\n")
                                .append(request.activity)
                                .append("\r\n")
                                .append("https://play.google.com/store/apps/details?id=")
                                .append(request.packageName)
                        }
                    }
                }

                mEmailBody = stringBuilder.toString()
                if (emailBodyGeneratorEnabled) {
                    mEmailBody += "\r\n\r\n" + emailBodyGenerator!!.generate(requestsForGenerator)
                }
                return true
            } catch (e: Exception) {
                CandyBarApplication.sRequestProperty = null
                RequestFragment.sSelectedRequests = null
                LogUtil.e(Log.getStackTraceString(e))
                return false
            }
        }
        return false
    }

    override fun postRun(ok: Boolean) {
        val context = mContext.get() ?: return
        if (ok) {
            try {
                mCallback?.get()?.onFinished()

                val listener = context as? RequestListener
                val requestProperty = CandyBarApplication.sRequestProperty ?: return
                val componentName = requestProperty.componentName ?: return

                listener?.onRequestBuilt(
                    getIntent(componentName, mEmailBody),
                    IntentChooserFragment.ICON_REQUEST
                )
            } catch (e: Exception) {
                LogUtil.e(Log.getStackTraceString(e))
            }
        } else {
            mError?.let {
                LogUtil.e(it.message)
                it.showToast(context)
            }
        }
    }

    private fun getIntent(name: ComponentName, emailBody: String?): Intent? {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            addIntentExtra(intent, emailBody)
            intent.component = name
            if (Build.VERSION.SDK_INT < 32) {
                intent.addCategory(Intent.CATEGORY_LAUNCHER)
            }
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return intent
        } catch (e: IllegalArgumentException) {
            try {
                val intent = Intent(Intent.ACTION_SEND)
                addIntentExtra(intent, emailBody)
                return intent
            } catch (e1: ActivityNotFoundException) {
                LogUtil.e(Log.getStackTraceString(e1))
            }
        }
        return null
    }

    private fun addIntentExtra(intent: Intent, emailBody: String?) {
        intent.type = "application/zip"

        val context = mContext.get() ?: return

        if (CandyBarApplication.sZipPath != null) {
            val zip = File(CandyBarApplication.sZipPath!!)
            if (zip.exists()) {
                var uri = FileHelper.getUriFromFile(context, context.packageName, zip)
                if (uri == null) uri = Uri.fromFile(zip)
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        val appName = context.resources.getString(R.string.app_name)

        var regularRequestSubject = context.resources.getString(R.string.regular_request_email_subject)
        if (regularRequestSubject.isEmpty()) regularRequestSubject = "$appName Icon Request"

        var premiumRequestSubject = context.resources.getString(R.string.premium_request_email_subject)
        if (premiumRequestSubject.isEmpty())
            premiumRequestSubject = "$appName Premium Icon Request"

        val regularRequestEmail = context.resources.getString(R.string.regular_request_email)
        var premiumRequestEmail = context.resources.getString(R.string.premium_request_email)
        // Fallback to regular request email
        if (premiumRequestEmail.isEmpty()) premiumRequestEmail = regularRequestEmail

        val isPremium = Preferences.get(context).isPremiumRequest
        val subject = if (isPremium) premiumRequestSubject else regularRequestSubject
        val emailAddress = if (isPremium) premiumRequestEmail else regularRequestEmail

        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, emailBody)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
    }

    fun interface IconRequestBuilderCallback {
        fun onFinished()
    }
}
