package candybar.lib.tasks

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.fragments.dialog.IntentChooserFragment
import candybar.lib.helpers.DeviceHelper
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

class PremiumRequestBuilderTask(context: Context, callback: PremiumRequestBuilderCallback?) : AsyncTaskBase() {

    private val mContext: WeakReference<Context> = WeakReference(context)
    private val mCallback: WeakReference<PremiumRequestBuilderCallback>? = if (callback != null) WeakReference(callback) else null
    private var mEmailBody: String? = null
    private var mError: Extras.Error? = null

    override fun run(): Boolean {
        if (!isCancelled) {
            try {
                Thread.sleep(1)
                val context = mContext.get() ?: return false
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

                val requests = Database.get(context).getPremiumRequest(null)

                for (request in requests) {
                    stringBuilder.append("\r\n\r\n")
                        .append(request.name)
                        .append("\r\n")
                        .append(request.activity)
                        .append("\r\n")
                        .append("https://play.google.com/store/apps/details?id=")
                        .append(request.packageName)
                        .append("\r\n")
                        .append("Order Id: ")
                        .append(request.orderId)
                        .append("\r\n")
                        .append("Product Id: ")
                        .append(request.productId)
                }

                mEmailBody = stringBuilder.toString()
                return true
            } catch (e: Exception) {
                CandyBarApplication.sRequestProperty = null
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
            try {
                mCallback?.get()?.onFinished()

                val listener = context as? RequestListener
                val requestProperty = CandyBarApplication.sRequestProperty ?: return
                val componentName = requestProperty.componentName ?: return

                listener?.onRequestBuilt(
                    getIntent(componentName, mEmailBody),
                    IntentChooserFragment.REBUILD_ICON_REQUEST
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
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
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

        var subject = context.resources.getString(R.string.premium_request_email_subject)
        if (subject.isEmpty())
            subject = context.resources.getString(R.string.app_name) + " Premium Icon Request"
        subject = "Rebuilt: $subject"

        val regularRequestEmail = context.resources.getString(R.string.regular_request_email)
        var emailAddress = context.resources.getString(R.string.premium_request_email)
        // Fallback to regular request email
        if (emailAddress.isEmpty()) emailAddress = regularRequestEmail

        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
        intent.putExtra(Intent.EXTRA_TEXT, emailBody)
        intent.addFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        )
    }

    fun interface PremiumRequestBuilderCallback {
        fun onFinished()
    }
}
