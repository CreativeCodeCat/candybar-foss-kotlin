package candybar.lib.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.util.Log
import android.widget.EditText
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.items.Icon
import candybar.lib.tasks.ReportBugsTask
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.google.android.material.textfield.TextInputLayout
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

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

object ReportBugsHelper {

    const val REPORT_BUGS = "reportbugs.zip"
    private const val BROKEN_APPFILTER = "broken_appfilter.xml"
    private const val BROKEN_DRAWABLES = "broken_drawables.xml"
    private const val ACTIVITY_LIST = "activity_list.xml"
    private const val CRASHLOG = "crashlog.txt"

    @JvmStatic
    fun prepareReportBugs(context: Context) {
        val dialog = MaterialDialog.Builder(context)
            .customView(R.layout.dialog_report_bugs, true)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .positiveText(R.string.report_bugs_send)
            .negativeText(R.string.close)
            .build()

        val editText = dialog.findViewById(R.id.input_desc) as EditText
        val inputLayout = dialog.findViewById(R.id.input_layout) as TextInputLayout

        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener {
            if (editText.text.isNotEmpty()) {
                inputLayout.isErrorEnabled = false
                ReportBugsTask(context, editText.text.toString()).executeOnThreadPool()
                dialog.dismiss()
                return@setOnClickListener
            }

            inputLayout.error = context.resources.getString(R.string.report_bugs_desc_empty)
        }
        dialog.show()
    }

    @JvmStatic
    fun buildBrokenAppFilter(context: Context): File? {
        return try {
            val activities = RequestHelper.getAppFilter(context, RequestHelper.Key.ACTIVITY)
            val brokenAppFilter = File(context.cacheDir, BROKEN_APPFILTER)
            val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(brokenAppFilter), StandardCharsets.UTF_8))

            var first = true
            for (entry in activities.entries) {
                if (first) {
                    first = false
                    writer.append("<!-- BROKEN APPFILTER -->")
                        .append("\r\n").append("<!-- Broken appfilter will check for activities that included in appfilter but doesn't have a drawable")
                        .append("\r\n").append("* ").append("The reason could because misnamed drawable or the drawable not copied to the project -->")
                        .append("\r\n\r\n\r\n")
                }

                val drawableId = context.resources.getIdentifier(entry.value, "drawable", context.packageName)
                if (drawableId == 0) {
                    writer.append("Activity: ").append(entry.key)
                        .append("\r\n")
                        .append("Drawable: ").append(entry.value).append(".png")
                        .append("\r\n\r\n")
                }
            }

            writer.flush()
            writer.close()
            brokenAppFilter
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
            null
        }
    }

    @JvmStatic
    fun buildBrokenDrawables(context: Context): File? {
        return try {
            val drawables = RequestHelper.getAppFilter(context, RequestHelper.Key.DRAWABLE)
            val iconList = IconsHelper.getIconsList(context)
            val icons = mutableListOf<Icon>()

            val brokenDrawables = File(context.cacheDir, BROKEN_DRAWABLES)
            val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(brokenDrawables), StandardCharsets.UTF_8))

            for (icon in iconList) {
                if (CandyBarApplication.getConfiguration().isShowTabAllIcons) {
                    if (icon.title != CandyBarApplication.getConfiguration().tabAllIconsTitle) {
                        icons.addAll(icon.icons)
                    }
                } else {
                    icons.addAll(icon.icons)
                }
            }

            IconsHelper.computeTitles(context, icons)
            val addedIcons = HashSet<String>()
            var first = true
            for (icon in icons) {
                if (first) {
                    first = false
                    writer.append("<!-- BROKEN DRAWABLES -->")
                        .append("\r\n").append("<!-- Broken drawables will read drawables that listed in drawable.xml")
                        .append("\r\n").append("* ").append("and try to match them with drawables that used in appfilter.xml")
                        .append("\r\n").append("* ").append("The reason could be drawable copied to the project but not used in appfilter.xml -->")
                        .append("\r\n\r\n\r\n")
                }

                val drawable = drawables[icon.drawableName]
                if (drawable.isNullOrEmpty() && !addedIcons.contains(icon.drawableName)) {
                    addedIcons.add(icon.drawableName)
                    writer.append("Drawable: ").append(icon.drawableName).append(".png")
                        .append("\r\n\r\n")
                }
            }

            writer.flush()
            writer.close()
            brokenDrawables
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
            null
        }
    }

    @JvmStatic
    fun buildActivityList(context: Context): File? {
        return try {
            val activityList = File(context.cacheDir, ACTIVITY_LIST)
            val out = BufferedWriter(OutputStreamWriter(FileOutputStream(activityList), StandardCharsets.UTF_8))

            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)

            val appList = context.packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
            try {
                appList.sortWith(ResolveInfo.DisplayNameComparator(context.packageManager))
            } catch (ignored: Exception) {
            }

            var first = true
            for (app in appList) {
                if (first) {
                    first = false
                    out.append("<!-- ACTIVITY LIST -->")
                        .append("\r\n").append("<!-- Activity list is a list that contains all activity from installed apps -->")
                        .append("\r\n\r\n\r\n")
                }

                val name = app.activityInfo.loadLabel(context.packageManager).toString()
                val activity = app.activityInfo.packageName + "/" + app.activityInfo.name
                out.append("<!-- ").append(name).append(" -->")
                out.append("\r\n").append(activity)
                out.append("\r\n\r\n")
            }

            out.flush()
            out.close()
            activityList
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
            null
        }
    }

    @JvmStatic
    fun buildCrashLog(context: Context, stackTrace: String): File? {
        return try {
            if (stackTrace.isEmpty()) return null

            val crashLog = File(context.cacheDir, CRASHLOG)
            val deviceInfo = DeviceHelper.getDeviceInfoForCrashReport(context)
            val out = BufferedWriter(OutputStreamWriter(FileOutputStream(crashLog), StandardCharsets.UTF_8))
            out.append(deviceInfo).append(stackTrace)
            out.flush()
            out.close()
            crashLog
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
            null
        }
    }
}
