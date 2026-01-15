package candybar.lib.helpers

import android.content.Context
import android.content.res.Configuration
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.items.Theme
import candybar.lib.preferences.Preferences
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

object ThemeHelper {

    @JvmStatic
    fun getDefaultTheme(context: Context): Theme {
        return try {
            Theme.valueOf(context.resources.getString(R.string.default_theme).uppercase(Locale.getDefault()))
        } catch (e: Exception) {
            Theme.AUTO
        }
    }

    @JvmStatic
    fun isDarkTheme(context: Context): Boolean {
        val isThemingEnabled = CandyBarApplication.getConfiguration().isDashboardThemingEnabled
        if (!isThemingEnabled) return getDefaultTheme(context) == Theme.DARK

        val currentTheme = Preferences.get(context).theme
        if (currentTheme == Theme.AUTO) {
            return when (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> true
                Configuration.UI_MODE_NIGHT_NO -> false
                else -> false
            }
        }

        return currentTheme == Theme.DARK
    }
}
