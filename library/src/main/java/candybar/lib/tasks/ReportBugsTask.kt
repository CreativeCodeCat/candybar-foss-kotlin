package candybar.lib.tasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import candybar.lib.R
import candybar.lib.helpers.DeviceHelper
import candybar.lib.helpers.ReportBugsHelper
import candybar.lib.helpers.RequestHelper
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import com.afollestad.materialdialogs.MaterialDialog
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

class ReportBugsTask(context: Context, private val mDescription: String) : AsyncTaskBase() {

    private val mContext: WeakReference<Context> = WeakReference(context)
    private var mZipPath: String? = null
    private var mStringBuilder: StringBuilder? = null
    private var mDialog: MaterialDialog? = null

    override fun preRun() {
        val context = mContext.get() ?: return
        mDialog = MaterialDialog.Builder(context)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .content(R.string.report_bugs_building)
            .progress(true, 0)
            .progressIndeterminateStyle(true)
            .cancelable(false)
            .canceledOnTouchOutside(false)
            .build()
        mDialog?.show()
        mStringBuilder = StringBuilder()
    }

    override fun run(): Boolean {
        if (!isCancelled) {
            try {
                Thread.sleep(1)
                val context = mContext.get() ?: return false
                val files = mutableListOf<String>()

                mStringBuilder?.append(DeviceHelper.getDeviceInfo(context))
                    ?.append("\r\n")?.append(mDescription)?.append("\r\n")

                ReportBugsHelper.buildBrokenAppFilter(context)?.let { files.add(it.toString()) }
                ReportBugsHelper.buildBrokenDrawables(context)?.let { files.add(it.toString()) }
                ReportBugsHelper.buildActivityList(context)?.let { files.add(it.toString()) }

                val stackTrace = Preferences.get(context).latestCrashLog
                ReportBugsHelper.buildCrashLog(context, stackTrace)?.let { files.add(it.toString()) }

                mZipPath = FileHelper.createZip(
                    files, File(
                        context.cacheDir,
                        RequestHelper.getGeneratedZipName(ReportBugsHelper.REPORT_BUGS)
                    )
                )
                return true
            } catch (e: Exception) {
                LogUtil.e(Log.getStackTraceString(e))
                return false
            }
        }
        return false
    }

    override fun postRun(ok: Boolean) {
        val context = mContext.get() ?: return
        if ((context as? AppCompatActivity)?.isFinishing == true) return

        mDialog?.dismiss()
        if (ok) {
            val emailAddress = context.getString(R.string.regular_request_email)

            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/zip"
            intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            intent.putExtra(
                Intent.EXTRA_SUBJECT,
                "Report Bugs ${context.getString(R.string.app_name)}"
            )
            intent.putExtra(Intent.EXTRA_TEXT, mStringBuilder.toString())

            mZipPath?.let { path ->
                val zip = File(path)
                if (zip.exists()) {
                    var uri = FileHelper.getUriFromFile(context, context.packageName, zip)
                    if (uri == null) uri = Uri.fromFile(zip)
                    intent.putExtra(Intent.EXTRA_STREAM, uri)
                    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.resources.getString(R.string.app_client)
                )
            )
        } else {
            Toast.makeText(
                context, R.string.report_bugs_failed,
                Toast.LENGTH_LONG
            ).show()
        }

        mZipPath = null
    }
}
