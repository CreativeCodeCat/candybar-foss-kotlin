package candybar.lib.helpers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.items.Request
import candybar.lib.preferences.Preferences
import com.afollestad.materialdialogs.MaterialDialog
import com.danimahardhika.android.helpers.core.TimeHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Locale
import java.util.regex.Matcher
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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

object RequestHelper {

    const val APPFILTER = "appfilter.xml"
    const val APPMAP = "appmap.xml"
    const val THEME_RESOURCES = "theme_resources.xml"
    const val ZIP = "icon_request.zip"
    const val REBUILD_ZIP = "rebuild_icon_request.zip"

    @JvmStatic
    fun getGeneratedZipName(baseName: String): String {
        return baseName.substring(0, baseName.lastIndexOf(".")) + "_" + TimeHelper.getDateTime(
            SimpleDateFormat("dd_MM_yyyy_HH_mm_ss", Locale.ENGLISH)
        ) + ".zip"
    }

    @JvmStatic
    fun fixNameForRequest(name: String): String {
        var normalized = Normalizer.normalize(name.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        try {
            normalized = normalized.replace("[\\p{InCombiningDiacriticalMarks}\\p{IsLm}\\p{IsSk}]+".toRegex(), "")
        } catch (ignored: Exception) {
        }
        normalized = normalized
            .replace("[.\"']".toRegex(), "")
            .replace("[ \\[\\]{}()=!/\\\\,?°|<>;:#~+*-]".toRegex(), "_")
            .replace("&", "_and_")
        if (normalized.isNotEmpty() && Character.isDigit(normalized[0])) normalized = "_$normalized"
        normalized = normalized.replace("_+".toRegex(), "_")
        return normalized
    }

    @JvmStatic
    fun buildXml(context: Context, requests: List<Request>, xmlType: XmlType): File? {
        try {
            when (xmlType) {
                XmlType.APPFILTER -> if (!CandyBarApplication.getConfiguration().isGenerateAppFilter) return null
                XmlType.APPMAP -> if (!CandyBarApplication.getConfiguration().isGenerateAppMap) return null
                XmlType.THEME_RESOURCES -> if (!CandyBarApplication.getConfiguration().isGenerateThemeResources) return null
            }

            val file = File(context.cacheDir.toString(), xmlType.fileName)
            val writer = BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8))
            writer.append(xmlType.header).append("\n\n")

            for (request in requests) {
                writer.append(xmlType.getContent(context, request))
            }
            writer.append(xmlType.footer)
            writer.flush()
            writer.close()
            return file
        } catch (e: IOException) {
            LogUtil.e(Log.getStackTraceString(e))
        }
        return null
    }

    @JvmStatic
    fun buildJsonForPacific(requests: List<Request>): String {
        val sb = StringBuilder()
        var isFirst = true
        sb.append("{ \"components\": [\n")
        for (request in requests) {
            if (!isFirst) sb.append(",\n")
            sb.append(
                String.format(
                    Locale.ROOT,
                    "{ \"name\": \"%s\", \"pkg\": \"%s\", \"componentInfo\": \"%s\", \"drawable\": \"%s\" }",
                    request.name,
                    request.packageName,
                    request.activity,
                    fixNameForRequest(request.name)
                )
            )
            isFirst = false
        }
        sb.append("]}")
        return sb.toString()
    }

    @JvmStatic
    fun buildJsonForMyAP(context: Context, requests: List<Request>): String {
        val sb = StringBuilder()
        var isFirst = true
        sb.append("{ \"projectUID\": \"ENTER UID\",")
        sb.append("\"icons\" : [")
        for (request in requests) {
            val packageIcon = DrawableHelper.getPackageIcon(context, request.activity) ?: continue
            val appBitmap = DrawableHelper.toBitmap(packageIcon)
            val baos = ByteArrayOutputStream()
            appBitmap?.compress(Bitmap.CompressFormat.PNG, 100, baos)
            val base64Icon = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
            if (!isFirst) sb.append(",\n")
            sb.append("\"name\": \"").append(request.name).append("\",")
                .append("\"packageName\": \"").append(request.packageName).append("\",")
                .append("\"imageStr\": \"").append(base64Icon).append("\",")
                .append("\"activities\": [\"").append(request.activity).append("\"]")
            isFirst = false
        }
        sb.append("]}")
        return sb.toString()
    }

    @JvmStatic
    fun getRegularPacificApiKey(context: Context): String {
        return context.resources.getString(R.string.regular_request_pacific_api_key)
    }

    @JvmStatic
    fun isRegularPacificEnabled(context: Context): Boolean {
        return if (context.resources.getString(R.string.regular_request_method).isNotEmpty())
            context.resources.getString(R.string.regular_request_method) == "pacific"
        else
            getRegularPacificApiKey(context).isNotEmpty()
    }

    @JvmStatic
    fun getPremiumPacificApiKey(context: Context): String {
        var pacificApiKey = context.resources.getString(R.string.premium_request_pacific_api_key)
        if (pacificApiKey.isEmpty()) pacificApiKey = getRegularPacificApiKey(context)
        return pacificApiKey
    }

    @JvmStatic
    fun isPremiumPacificEnabled(context: Context): Boolean {
        return if (context.resources.getString(R.string.premium_request_method).isNotEmpty())
            context.resources.getString(R.string.premium_request_method) == "pacific"
        else if (context.resources.getString(R.string.regular_request_method).isNotEmpty())
            context.resources.getString(R.string.regular_request_method) == "pacific"
        else
            getRegularPacificApiKey(context).isNotEmpty()
    }

    @JvmStatic
    fun sendPacificRequest(requests: List<Request>, iconFiles: List<String>, directory: File, apiKey: String): String? {
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "click",
            hashMapOf(
                "section" to "icon_request",
                "action" to "submit",
                "item" to "pacific",
                "number_of_icons" to requests.size
            )
        )
        val okRequestBody = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("apps", buildJsonForPacific(requests))
            .addFormDataPart(
                "archive", "icons.zip", getZipFile(iconFiles, directory.toString(), "icons.zip")!!
                    .asRequestBody("application/zip".toMediaType())
            )
            .build()

        val okRequest = okhttp3.Request.Builder()
            .url("https://pacificmanager.app/v1/request")
            .addHeader("TokenID", apiKey)
            .addHeader("Accept", "application/json")
            .addHeader("User-Agent", "afollestad/icon-request")
            .post(okRequestBody)
            .build()

        val okHttpClient = okhttp3.OkHttpClient()
        try {
            val responseJson: JSONObject
            okHttpClient.newCall(okRequest).execute().use { response ->
                val success = response.code in 200..299
                if (!success) return "Unknown error."
                responseJson = JSONObject(response.body?.string() ?: "")
            }
            if (responseJson.getString("status") == "error") {
                return responseJson.getString("error")
            }
        } catch (e: Exception) {
            LogUtil.e("PACIFIC_MANAGER: Error")
            return ""
        }
        return null
    }

    @JvmStatic
    fun isRegularCustomEnabled(context: Context): Boolean {
        return context.resources.getString(R.string.regular_request_method).isNotEmpty() &&
                context.resources.getString(R.string.regular_request_method) == "custom"
    }

    @JvmStatic
    fun isPremiumCustomEnabled(context: Context): Boolean {
        return (context.resources.getString(R.string.premium_request_method).isNotEmpty() &&
                context.resources.getString(R.string.premium_request_method) == "custom") ||
                (context.resources.getString(R.string.regular_request_method).isNotEmpty() &&
                        context.resources.getString(R.string.regular_request_method) == "custom")
    }

    @JvmStatic
    fun sendCustomRequest(requests: List<Request>, isPremium: Boolean): String? {
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "click",
            hashMapOf(
                "section" to "icon_request",
                "action" to "submit",
                "item" to "custom",
                "type" to if (isPremium) "premium" else "regular",
                "number_of_icons" to requests.size
            )
        )
        val iconRequestHandler = CandyBarApplication.getConfiguration().iconRequestHandler
        val errorMessage = iconRequestHandler?.submit(requests, isPremium)
            ?: run {
                val error = "Custom icon request failed: No handler configured"
                LogUtil.e(error)
                error
            }
        return if (errorMessage == "") null else {
            LogUtil.e(errorMessage)
            errorMessage
        }
    }

    @JvmStatic
    fun getZipFile(files: List<String>, filepath: String, filename: String): File? {
        return try {
            val buffer = 2048
            val file = File(filepath, filename)
            val dest = FileOutputStream(file)
            val out = ZipOutputStream(BufferedOutputStream(dest))
            val data = ByteArray(buffer)

            for (i in files.indices) {
                val fi = FileInputStream(files[i])
                val origin = BufferedInputStream(fi, buffer)
                val entry = ZipEntry(files[i].substring(files[i].lastIndexOf("/") + 1))
                out.putNextEntry(entry)
                var count: Int
                while (origin.read(data, 0, buffer).also { count = it } != -1) {
                    out.write(data, 0, count)
                }
                origin.close()
            }
            out.close()
            file
        } catch (ignored: Exception) {
            null
        }
    }

    @JvmStatic
    fun getAppFilter(context: Context, key: Key): HashMap<String, String> {
        return try {
            val activities = HashMap<String, String>()
            val xpp = context.resources.getXml(R.xml.appfilter)
            while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                if (xpp.eventType == XmlPullParser.START_TAG) {
                    if (xpp.name == "item") {
                        val sKey = xpp.getAttributeValue(null, key.key)
                        val sValue = xpp.getAttributeValue(null, key.value)
                        if (sKey != null && sValue != null) {
                            activities[sKey.replace("ComponentInfo{", "").replace("}", "")] =
                                sValue.replace("ComponentInfo{", "").replace("}", "")
                        } else {
                            LogUtil.e("Appfilter Error\nKey: $sKey\nValue: $sValue")
                        }
                    }
                }
                xpp.next()
            }
            activities
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
            HashMap()
        }
    }

    @JvmStatic
    fun getMissingApps(context: Context): List<Request> {
        val requests = mutableListOf<Request>()
        val appFilter = getAppFilter(context, Key.ACTIVITY)
        val packageManager = context.packageManager

        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val installedApps = packageManager.queryIntentActivities(intent, PackageManager.GET_RESOLVED_FILTER)
        CandyBarMainActivity.sInstalledAppsCount = installedApps.size

        try {
            Collections.sort(installedApps, ResolveInfo.DisplayNameComparator(packageManager))
        } catch (ignored: Exception) {
        }

        for (app in installedApps) {
            val packageName = app.activityInfo.packageName
            val activity = "$packageName/${app.activityInfo.name}"
            val value = appFilter[activity]
            if (value == null) {
                var name = LocaleHelper.getOtherAppLocaleName(context, Locale.ENGLISH, activity)
                if (name == null) {
                    name = app.activityInfo.loadLabel(packageManager).toString()
                }
                val requested = Database.get(context).isRequested(activity)
                val request = Request.Builder()
                    .name(name)
                    .packageName(packageName)
                    .activity(activity)
                    .requested(requested)
                    .build()
                requests.add(request)
            }
        }
        return requests
    }

    @JvmStatic
    fun showIconRequestLimitDialog(context: Context) {
        val reset = context.resources.getBoolean(R.bool.reset_icon_request_limit)
        val limit = context.resources.getInteger(R.integer.icon_request_limit)
        var message = context.resources.getString(R.string.request_limit, limit)
        message += " " + context.resources.getString(
            R.string.request_used,
            Preferences.get(context).regularRequestUsed
        )

        if (Preferences.get(context).isPremiumRequestEnabled)
            message += " " + context.resources.getString(R.string.request_limit_buy)

        if (reset)
            message += "\n\n" + context.resources.getString(R.string.request_limit_reset)
        MaterialDialog.Builder(context)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .title(R.string.request_title)
            .content(message)
            .positiveText(R.string.close)
            .show()
    }

    @JvmStatic
    fun showPremiumRequestRequired(context: Context) {
        MaterialDialog.Builder(context)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .title(R.string.request_title)
            .content(R.string.premium_request_required)
            .positiveText(R.string.close)
            .show()
    }

    @JvmStatic
    fun showPremiumRequestLimitDialog(context: Context, selected: Int) {
        var message = context.resources.getString(
            R.string.premium_request_limit,
            Preferences.get(context).premiumRequestCount
        )
        message += " " + context.resources.getString(R.string.premium_request_limit1, selected)
        MaterialDialog.Builder(context)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .title(R.string.premium_request)
            .content(message)
            .positiveText(R.string.close)
            .show()
    }

    @JvmStatic
    fun showPremiumRequestStillAvailable(context: Context) {
        val message = context.resources.getString(
            R.string.premium_request_already_purchased,
            Preferences.get(context).premiumRequestCount
        )
        MaterialDialog.Builder(context)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .title(R.string.premium_request)
            .content(message)
            .positiveText(R.string.close)
            .show()
    }

    @JvmStatic
    fun isReadyToSendPremiumRequest(context: Context): Boolean {
        val isReady = Preferences.get(context).isConnectedToNetwork
        if (!isReady) {
            MaterialDialog.Builder(context)
                .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
                .title(R.string.premium_request)
                .content(R.string.premium_request_no_internet)
                .positiveText(R.string.close)
                .show()
        }
        return isReady
    }

    @JvmStatic
    fun showPremiumRequestConsumeFailed(context: Context) {
        MaterialDialog.Builder(context)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .title(R.string.premium_request)
            .content(R.string.premium_request_consume_failed)
            .positiveText(R.string.close)
            .show()
    }

    @JvmStatic
    fun showPremiumRequestExist(context: Context) {
        MaterialDialog.Builder(context)
            .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
            .title(R.string.premium_request)
            .content(R.string.premium_request_exist)
            .positiveText(R.string.close)
            .show()
    }

    @JvmStatic
    fun checkPiracyApp(context: Context) {
        val premiumRequest = context.resources.getBoolean(R.bool.enable_premium_request)
        if (!premiumRequest) {
            Preferences.get(context).isPremiumRequestEnabled = false
            return
        }

        val strings = arrayOf(
            "com.chelpus.lackypatch",
            "com.dimonvideo.luckypatcher",
            "com.forpda.lp",
            "com.android.vending.billing.InAppBillingService.LUCK",
            "com.android.vending.billing.InAppBillingService.LOCK",
            "cc.madkite.freedom",
            "com.android.vending.billing.InAppBillingService.LACK",
            "com.android.vending.billing.InAppBillingService.CLON",
            "com.android.vending.billing.InAppBillingService.CRAC",
            "com.android.vending.billing.InAppBillingService.COIN"
        )

        var isPiracyAppInstalled = false
        for (string in strings) {
            try {
                val packageInfo = context.packageManager.getPackageInfo(string, PackageManager.GET_ACTIVITIES)
                if (packageInfo != null) {
                    isPiracyAppInstalled = true
                    break
                }
            } catch (ignored: Exception) {
            }
        }
        Preferences.get(context).isPremiumRequestEnabled = !isPiracyAppInstalled
    }

    enum class XmlType(val fileName: String, val header: String, val footer: String) {
        APPFILTER(RequestHelper.APPFILTER, "<resources>", "</resources>"),
        APPMAP(RequestHelper.APPMAP, "<appmap>", "</appmap>"),
        THEME_RESOURCES(RequestHelper.THEME_RESOURCES, "<Theme version=\"1\">", "</Theme>");

        fun getContent(context: Context, request: Request): String {
            val fileName = request.fileName ?: fixNameForRequest(request.name)
            return when (this) {
                APPFILTER -> {
                    "\t<!-- ${request.name} -->" +
                            "\n" +
                            "\t" + context.getString(R.string.appfilter_item)
                        .replace("{{component}}", Matcher.quoteReplacement(request.activity))
                        .replace("{{drawable}}", fileName) +
                            "\n\n"
                }

                APPMAP -> {
                    val packageName = request.packageName + "/"
                    val className = request.activity.replaceFirst(packageName.toRegex(), "")
                    "\t<!-- ${request.name} -->" +
                            "\n" +
                            "\t<item class=\"$className\" name=\"$fileName\"/>" +
                            "\n\n"
                }

                THEME_RESOURCES -> {
                    "\t<!-- ${request.name} -->" +
                            "\n" +
                            "\t<AppIcon name=\"${request.activity}\" image=\"$fileName\"/>" +
                            "\n\n"
                }
            }
        }
    }

    enum class Key(val key: String, val value: String) {
        ACTIVITY("component", "drawable"),
        DRAWABLE("drawable", "component");
    }
}
