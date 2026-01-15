package candybar.lib.helpers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import candybar.lib.BuildConfig
import candybar.lib.R

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

object DeviceHelper {

    @JvmStatic
    fun getDeviceInfo(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        val height = displayMetrics.heightPixels
        val width = displayMetrics.widthPixels

        var appVersion = ""
        try {
            @Suppress("DEPRECATION")
            appVersion = context.packageManager.getPackageInfo(
                context.packageName, 0
            ).versionName ?: ""
        } catch (ignored: PackageManager.NameNotFoundException) {
        }

        return StringBuilder()
            .append("Manufacturer : ").append(Build.MANUFACTURER)
            .append("\r\nModel : ").append(Build.MODEL)
            .append("\r\nProduct : ").append(Build.PRODUCT)
            .append("\r\nScreen Resolution : ")
            .append(width).append(" x ").append(height).append(" pixels")
            .append("\r\nAndroid Version : ").append(Build.VERSION.RELEASE)
            .append("\r\nApp Version : ").append(appVersion)
            .append("\r\nCandyBar Version : ").append(BuildConfig.VERSION_NAME)
            .append("\r\n")
            .toString()
    }

    @JvmStatic
    fun getDeviceInfoForCrashReport(context: Context): String {
        return "Icon Pack Name : " + context.resources.getString(R.string.app_name) +
                "\r\n" + getDeviceInfo(context)
    }
}
