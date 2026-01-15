package candybar.lib.helpers

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.util.Log
import candybar.lib.R
import candybar.lib.items.Language
import candybar.lib.preferences.Preferences
import com.danimahardhika.android.helpers.core.utils.LogUtil
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

object LocaleHelper {

    @JvmStatic
    fun setLocale(context: Context) {
        val locale = Preferences.get(context).currentLocale
        Locale.setDefault(locale)
        val configuration = context.resources.configuration
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.setDefault(LocaleList(locale))
            configuration.setLocales(LocaleList(locale))
        } else {
            configuration.setLocale(locale)
        }

        // Todo:
        // Find out a solution to use context.createConfigurationContext(configuration);
        // It breaks onConfigurationChanged()
        // Still can't find a way to fix that
        // No other options, better use deprecated code for now
        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
    }

    @JvmStatic
    fun getAvailableLanguages(context: Context): MutableList<Language> {
        val languages = mutableListOf<Language>()
        val names = context.resources.getStringArray(R.array.languages_name)
        val codes = context.resources.getStringArray(R.array.languages_code)

        for (i in names.indices) {
            val language = Language(names[i], getLocale(codes[i]))
            languages.add(language)
        }
        return languages
    }

    @JvmStatic
    fun getCurrentLanguage(context: Context): Language {
        val languages = getAvailableLanguages(context)
        val locale = Preferences.get(context).currentLocale

        for (language in languages) {
            val l = language.locale
            if (locale.toString() == l.toString()) {
                return language
            }
        }
        return Language("English", Locale("en", "US"))
    }

    @JvmStatic
    fun getLocale(language: String): Locale {
        val codes = language.split("_").toTypedArray()
        if (codes.size == 2) {
            return Locale(codes[0], codes[1])
        }
        return Locale.getDefault()
    }

    @JvmStatic
    fun getOtherAppLocaleName(context: Context, locale: Locale, componentNameStr: String): String? {
        try {
            val slashIndex = componentNameStr.indexOf("/")
            val packageName = componentNameStr.substring(0, slashIndex)
            val activityName = componentNameStr.substring(slashIndex + 1)
            val componentName = ComponentName(packageName, activityName)

            val packageManager = context.packageManager
            val info = packageManager.getActivityInfo(componentName, PackageManager.GET_META_DATA)

            val res = packageManager.getResourcesForActivity(componentName)
            val configuration = Configuration()

            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            res.updateConfiguration(configuration, context.resources.displayMetrics)
            return info.loadLabel(packageManager).toString()
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        }
        return null
    }
}
