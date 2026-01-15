package candybar.lib.preferences

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.LocaleHelper
import candybar.lib.helpers.ThemeHelper
import candybar.lib.items.Theme
import java.lang.ref.WeakReference
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

class Preferences private constructor(context: Context) {

    private val mContext: Context = context.applicationContext

    private val sharedPreferences: SharedPreferences
        get() = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun clearPreferences() {
        sharedPreferences.edit().clear().apply()
    }

    var isFirstRun: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_RUN, true)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_FIRST_RUN, bool).apply()

    var iconShape: Int
        get() = sharedPreferences.getInt(KEY_ICON_SHAPE, -1)
        set(shape) = sharedPreferences.edit().putInt(KEY_ICON_SHAPE, shape).apply()

    var isTimeToShowHomeIntro: Boolean
        get() = sharedPreferences.getBoolean(KEY_HOME_INTRO, true)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_HOME_INTRO, bool).apply()

    var isTimeToShowIconsIntro: Boolean
        get() = sharedPreferences.getBoolean(KEY_ICONS_INTRO, true)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_ICONS_INTRO, bool).apply()

    var isTimeToShowRequestIntro: Boolean
        get() = sharedPreferences.getBoolean(KEY_REQUEST_INTRO, true)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_REQUEST_INTRO, bool).apply()

    var isTimeToShowWallpapersIntro: Boolean
        get() = sharedPreferences.getBoolean(KEY_WALLPAPERS_INTRO, true)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_WALLPAPERS_INTRO, bool).apply()

    var isTimeToShowWallpaperPreviewIntro: Boolean
        get() = sharedPreferences.getBoolean(KEY_WALLPAPER_PREVIEW_INTRO, true)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_WALLPAPER_PREVIEW_INTRO, bool).apply()

    var theme: Theme
        get() = Theme.values()[sharedPreferences.getInt(
            KEY_THEME,
            ThemeHelper.getDefaultTheme(mContext).ordinal
        )]
        set(theme) {
            val params = HashMap<String, Any>()
            params["section"] = "settings"
            params["action"] = "change_theme"
            params["theme"] = theme.name
            CandyBarApplication.getConfiguration().analyticsHandler?.logEvent("click", params)
            sharedPreferences.edit().putInt(KEY_THEME, theme.ordinal).apply()
        }

    var isMaterialYou: Boolean
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
            return sharedPreferences.getBoolean(
                KEY_MATERIAL_YOU,
                mContext.resources.getBoolean(R.bool.material_you_by_default)
            )
        }
        set(bool) = sharedPreferences.edit().putBoolean(KEY_MATERIAL_YOU, bool).apply()

    var isNotificationsEnabled: Boolean
        get() = sharedPreferences.getBoolean(KEY_NOTIFICATIONS, true)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_NOTIFICATIONS, bool).apply()

    val isToolbarShadowEnabled: Boolean
        get() = CandyBarApplication.getConfiguration().shadowOptions.isToolbarEnabled

    val isCardShadowEnabled: Boolean
        get() = CandyBarApplication.getConfiguration().shadowOptions.isCardEnabled

    val isFabShadowEnabled: Boolean
        get() = CandyBarApplication.getConfiguration().shadowOptions.isFabEnabled

    val isTapIntroShadowEnabled: Boolean
        get() = CandyBarApplication.getConfiguration().shadowOptions.isTapIntroEnabled

    var isWifiOnly: Boolean
        get() = sharedPreferences.getBoolean(KEY_WIFI_ONLY, false)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_WIFI_ONLY, bool).apply()

    val wallsDirectory: String
        get() = sharedPreferences.getString(KEY_WALLS_DIRECTORY, "") ?: ""

    var isPremiumRequestEnabled: Boolean
        get() = sharedPreferences.getBoolean(
            KEY_PREMIUM_REQUEST_ENABLED,
            mContext.resources.getBoolean(R.bool.enable_premium_request)
        )
        set(bool) = sharedPreferences.edit().putBoolean(KEY_PREMIUM_REQUEST_ENABLED, bool).apply()

    var isPremiumRequest: Boolean
        get() = sharedPreferences.getBoolean(KEY_PREMIUM_REQUEST, false)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_PREMIUM_REQUEST, bool).apply()

    var premiumRequestProductId: String
        get() = sharedPreferences.getString(KEY_PREMIUM_REQUEST_PRODUCT, "") ?: ""
        set(productId) = sharedPreferences.edit().putString(KEY_PREMIUM_REQUEST_PRODUCT, productId).apply()

    var premiumRequestCount: Int
        get() = sharedPreferences.getInt(KEY_PREMIUM_REQUEST_COUNT, 0)
        set(count) = sharedPreferences.edit().putInt(KEY_PREMIUM_REQUEST_COUNT, count).apply()

    var premiumRequestTotal: Int
        get() {
            val count = premiumRequestCount
            return sharedPreferences.getInt(KEY_PREMIUM_REQUEST_TOTAL, count)
        }
        set(count) = sharedPreferences.edit().putInt(KEY_PREMIUM_REQUEST_TOTAL, count).apply()

    var regularRequestUsed: Int
        get() = sharedPreferences.getInt(KEY_REGULAR_REQUEST_USED, 0)
        set(used) = sharedPreferences.edit().putInt(KEY_REGULAR_REQUEST_USED, used).apply()

    val isRegularRequestLimit: Boolean
        get() = mContext.resources.getBoolean(R.bool.enable_icon_request_limit)

    var inAppBillingType: Int
        get() = sharedPreferences.getInt(KEY_INAPP_BILLING_TYPE, -1)
        set(type) = sharedPreferences.edit().putInt(KEY_INAPP_BILLING_TYPE, type).apply()

    var isCropWallpaper: Boolean
        get() = sharedPreferences.getBoolean(KEY_CROP_WALLPAPER, false)
        set(bool) = sharedPreferences.edit().putBoolean(KEY_CROP_WALLPAPER, bool).apply()

    var latestCrashLog: String
        get() = sharedPreferences.getString(KEY_LATEST_CRASHLOG, "") ?: ""
        set(string) = sharedPreferences.edit().putString(KEY_LATEST_CRASHLOG, string).apply()

    var availableWallpapersCount: Int
        get() = sharedPreferences.getInt(KEY_AVAILABLE_WALLPAPERS_COUNT, 0)
        set(count) = sharedPreferences.edit().putInt(KEY_AVAILABLE_WALLPAPERS_COUNT, count).apply()

    val isPlayStoreCheckEnabled: Boolean
        get() = mContext.resources.getBoolean(R.bool.playstore_check_enabled)

    private var version: Int
        get() = sharedPreferences.getInt(KEY_APP_VERSION, 0)
        set(version) = sharedPreferences.edit().putInt(KEY_APP_VERSION, version).apply()

    val isNewVersion: Boolean
        get() {
            var currentVersion = 0
            try {
                currentVersion = mContext.packageManager.getPackageInfo(mContext.packageName, 0).versionCode
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
            return if (currentVersion > version) {
                val resetLimit = mContext.resources.getBoolean(R.bool.reset_icon_request_limit)
                if (resetLimit) regularRequestUsed = 0
                version = currentVersion
                true
            } else {
                false
            }
        }

    var currentLocale: Locale
        get() {
            val code = sharedPreferences.getString(KEY_CURRENT_LOCALE, "en_US") ?: "en_US"
            return LocaleHelper.getLocale(code)
        }
        set(code) = setCurrentLocale(code.toString())

    fun setCurrentLocale(code: String) {
        sharedPreferences.edit().putString(KEY_CURRENT_LOCALE, code).apply()
    }

    var isTimeToSetLanguagePreference: Boolean
        get() = sharedPreferences.getBoolean(KEY_LANGUAGE_PREFERENCE, true)
        private set(bool) = sharedPreferences.edit().putBoolean(KEY_LANGUAGE_PREFERENCE, bool).apply()

    fun setLanguagePreference() {
        val locale = Locale.getDefault()
        val languages = LocaleHelper.getAvailableLanguages(mContext)

        var currentLocale: Locale? = null
        for (language in languages) {
            val l = language.locale
            if (locale.toString() == l.toString()) {
                currentLocale = l
                break
            }
        }

        if (currentLocale == null) {
            for (language in languages) {
                val l = language.locale
                if (locale.language == l.language) {
                    currentLocale = l
                    break
                }
            }
        }

        if (currentLocale != null) {
            setCurrentLocale(currentLocale.toString())
            LocaleHelper.setLocale(mContext)
            isTimeToSetLanguagePreference = false
        }
    }

    val isConnectedToNetwork: Boolean
        get() {
            return try {
                val connectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                activeNetworkInfo != null && activeNetworkInfo.isConnected
            } catch (e: Exception) {
                false
            }
        }

    val isConnectedAsPreferred: Boolean
        get() {
            return try {
                if (isWifiOnly) {
                    val connectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val activeNetworkInfo = connectivityManager.activeNetworkInfo
                    return activeNetworkInfo != null &&
                            activeNetworkInfo.type == ConnectivityManager.TYPE_WIFI &&
                            activeNetworkInfo.isConnected
                }
                true
            } catch (e: Exception) {
                false
            }
        }

    companion object {
        private const val PREFERENCES_NAME = "candybar_preferences"

        private const val KEY_FIRST_RUN = "first_run"
        private const val KEY_THEME = "theme"
        private const val KEY_MATERIAL_YOU = "material_you"
        private const val KEY_NOTIFICATIONS = "notifications"
        private const val KEY_ICON_SHAPE = "icon_shape"
        private const val KEY_APP_VERSION = "app_version"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_WALLS_DIRECTORY = "wallpaper_directory"
        private const val KEY_PREMIUM_REQUEST = "premium_request"
        private const val KEY_PREMIUM_REQUEST_PRODUCT = "premium_request_product"
        private const val KEY_PREMIUM_REQUEST_COUNT = "premium_request_count"
        private const val KEY_PREMIUM_REQUEST_TOTAL = "premium_request_total"
        private const val KEY_REGULAR_REQUEST_USED = "regular_request_used"
        private const val KEY_INAPP_BILLING_TYPE = "inapp_billing_type"
        private const val KEY_LATEST_CRASHLOG = "last_crashlog"
        private const val KEY_PREMIUM_REQUEST_ENABLED = "premium_request_enabled"
        private const val KEY_AVAILABLE_WALLPAPERS_COUNT = "available_wallpapers_count"
        private const val KEY_CROP_WALLPAPER = "crop_wallpaper"
        private const val KEY_HOME_INTRO = "home_intro"
        private const val KEY_ICONS_INTRO = "icons_intro"
        private const val KEY_REQUEST_INTRO = "request_intro"
        private const val KEY_WALLPAPERS_INTRO = "wallpapers_intro"
        private const val KEY_WALLPAPER_PREVIEW_INTRO = "wallpaper_preview_intro"

        private const val KEY_LANGUAGE_PREFERENCE = "language_preference"
        private const val KEY_CURRENT_LOCALE = "current_locale"

        private var mPreferences: WeakReference<Preferences>? = null

        @JvmStatic
        fun get(context: Context): Preferences {
            var prefs = mPreferences?.get()
            if (prefs == null) {
                prefs = Preferences(context)
                mPreferences = WeakReference(prefs)
            }
            return prefs
        }
    }
}
