package candybar.lib.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import candybar.lib.R
import candybar.lib.helpers.DeviceHelper
import candybar.lib.helpers.LocaleHelper
import candybar.lib.helpers.ReportBugsHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.danimahardhika.android.helpers.core.FileHelper

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

class CandyBarCrashReport : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val bundle = intent.extras
            if (bundle == null) {
                finish()
                return
            }

            LocaleHelper.setLocale(this)

            val stackTrace = bundle.getString(EXTRA_STACKTRACE) ?: ""
            var deviceInfo: String = DeviceHelper.getDeviceInfoForCrashReport(this) ?: ""

            val message = resources.getString(
                R.string.crash_report_message,
                resources.getString(R.string.app_name)
            )
            val emailAddress = resources.getString(R.string.regular_request_email)

            MaterialDialog.Builder(this)
                .title(R.string.crash_report)
                .content(message)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .positiveText(R.string.crash_report_send)
                .negativeText(R.string.close)
                .onPositive { dialog, _ ->
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                    intent.putExtra(Intent.EXTRA_SUBJECT, "CandyBar: Crash Report")

                    prepareUri(deviceInfo, stackTrace, intent)

                    startActivity(
                        Intent.createChooser(
                            intent,
                            resources.getString(R.string.app_client)
                        )
                    )
                    dialog.dismiss()
                }
                .dismissListener { finish() }
                .show()
        } catch (e: Exception) {
            finish()
        }
    }

    private fun prepareUri(deviceInfo: String, stackTrace: String, intent: Intent) {
        val crashLog = ReportBugsHelper.buildCrashLog(this, stackTrace)
        if (crashLog != null) {
            val uri = FileHelper.getUriFromFile(this, packageName, crashLog)
            if (uri != null) {
                intent.putExtra(Intent.EXTRA_TEXT, "$deviceInfo\r\n")
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                return
            }
        }

        intent.putExtra(Intent.EXTRA_TEXT, deviceInfo + stackTrace)
    }

    companion object {
        const val EXTRA_STACKTRACE = "stacktrace"
    }
}
