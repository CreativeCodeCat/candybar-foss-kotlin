package candybar.lib.applications

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.annotation.IntRange
import candybar.lib.R
import candybar.lib.activities.CandyBarCrashReport
import candybar.lib.databases.Database
import candybar.lib.helpers.LocaleHelper
import candybar.lib.items.Request
import candybar.lib.preferences.Preferences
import candybar.lib.utils.JsonStructure
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.text.SimpleDateFormat
import java.util.Date
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

abstract class CandyBarApplication : Application() {

    private var mHandler: Thread.UncaughtExceptionHandler? = null

    abstract fun onInit(): Configuration

    abstract fun getDrawableClass(): Class<*>

    override fun onCreate() {
        super.onCreate()
        Database.get(this).openDatabase()

        // Enable or disable logging
        LogUtil.setLoggingTag(getString(R.string.app_name))
        LogUtil.setLoggingEnabled(true)

        configuration = onInit()
        mDrawableClass = getDrawableClass()

        if (configuration.isCrashReportEnabled) {
            mHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                handleUncaughtException(thread, throwable)
            }
        }

        if (Preferences.get(this).isTimeToSetLanguagePreference) {
            Preferences.get(this).setLanguagePreference()
            return
        }

        LocaleHelper.setLocale(this)
    }

    private fun handleUncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sb = StringBuilder()
            val dateFormat = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", Locale.getDefault()
            )
            val dateTime = dateFormat.format(Date())
            sb.append("Crash Time : ").append(dateTime).append("\r\n")
            sb.append("Class Name : ").append(throwable.javaClass.name).append("\r\n")
            sb.append("Caused By : ").append(throwable).append("\r\n")

            for (element in throwable.stackTrace) {
                sb.append("\r\n")
                sb.append(element.toString())
            }

            Preferences.get(this).latestCrashLog = sb.toString()

            val intent = Intent(this, CandyBarCrashReport::class.java)
            intent.putExtra(CandyBarCrashReport.EXTRA_STACKTRACE, sb.toString())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            startActivity(intent)
        } catch (e: Exception) {
            if (mHandler != null) {
                mHandler!!.uncaughtException(thread, throwable)
                return
            }
        }
        System.exit(1)
    }

    class Configuration {
        interface EmailBodyGenerator {
            fun generate(requests: List<Request>): String
        }

        interface IconRequestHandler {
            fun submit(requests: List<Request>, isPremium: Boolean): String
        }

        interface ConfigHandler {
            fun wallpaperJson(context: Context): String
            fun configJson(context: Context): String
        }

        interface AnalyticsHandler {
            fun logEvent(eventName: String, params: HashMap<String, Any>?)
            fun logException(exception: Exception)
        }

        interface FilterRequestHandler {
            fun filterRequest(request: Request): Boolean
        }

        interface NotificationHandler {
            fun setMode(isEnable: Boolean)
        }

        var emailBodyGenerator: EmailBodyGenerator? = null
            private set

        var iconRequestHandler: IconRequestHandler? = null
            private set

        var analyticsHandler: AnalyticsHandler? = null
            get() {
                if (field == null) {
                    field = object : AnalyticsHandler {
                        override fun logEvent(eventName: String, params: HashMap<String, Any>?) {
                            val sb = StringBuilder()
                            params?.forEach { (key, value) ->
                                sb.append(" ")
                                sb.append(key)
                                sb.append("=")
                                sb.append(value)
                            }
                            LogUtil.d("ANALYTICS EVENT: $eventName$sb")
                        }

                        override fun logException(exception: Exception) {
                            LogUtil.e(exception.stackTrace.toString())
                        }
                    }
                }
                return field
            }
            private set

        var filterRequestHandler: FilterRequestHandler? = null
            get() {
                if (field == null) {
                    // By default allow all requests
                    field = object : FilterRequestHandler {
                        override fun filterRequest(request: Request): Boolean {
                            return true
                        }
                    }
                }
                return field
            }
            private set

        var configHandler: ConfigHandler? = null
            get() {
                if (field == null) {
                    field = object : ConfigHandler {
                        override fun wallpaperJson(context: Context): String {
                            return context.getString(R.string.wallpaper_json)
                        }

                        override fun configJson(context: Context): String {
                            return context.getString(R.string.config_json)
                        }
                    }
                }
                return field
            }
            private set

        var isNotificationEnabled = false
            private set

        var notificationHandler: NotificationHandler? = null
            private set

        var navigationIcon = NavigationIcon.STYLE_1
            private set

        var navigationViewHeader = NavigationViewHeader.NORMAL
            private set

        var homeGrid = GridStyle.CARD
            private set

        var applyGrid = GridStyle.CARD
            private set

        var requestStyle = Style.PORTRAIT_FLAT_LANDSCAPE_CARD
            private set

        var wallpapersGrid = GridStyle.CARD
            private set

        var aboutStyle = Style.PORTRAIT_FLAT_LANDSCAPE_CARD
            private set

        var socialIconColor = IconColor.PRIMARY_TEXT
            private set

        var otherApps: List<OtherApp>? = null
            private set

        var donationLinks: List<DonationLink>? = null
            private set

        var isHighQualityPreviewEnabled = false
            private set

        var isColoredApplyCard = true
            private set

        var isAutomaticIconsCountEnabled = true
            private set

        var customIconsCount = 0
            private set

        var isShowTabIconsCount = false
            private set

        var isShowTabAllIcons = false
            private set

        var tabAllIconsTitle = "All Icons"
            private set

        var categoryForTabAllIcons: Array<String>? = null
            private set

        var excludedCategoryForSearch: Array<String>? = null
            private set

        var shadowOptions = ShadowOptions()
            private set

        var isDashboardThemingEnabled = true
            private set

        var wallpaperGridPreviewQuality = 4
            private set

        var isGenerateAppFilter = true
            private set

        var isGenerateAppMap = false
            private set

        var isGenerateThemeResources = false
            private set

        var isIncludeIconRequestToEmailBody = true
            private set

        var isCrashReportEnabled = true
            private set

        var wallpaperJsonStructure = JsonStructure.Builder(null).build()
            private set

        fun setEmailBodyGenerator(emailBodyGenerator: EmailBodyGenerator): Configuration {
            this.emailBodyGenerator = emailBodyGenerator
            return this
        }

        fun setIconRequestHandler(iconRequestHandler: IconRequestHandler): Configuration {
            this.iconRequestHandler = iconRequestHandler
            return this
        }

        fun setConfigHandler(configHandler: ConfigHandler): Configuration {
            this.configHandler = configHandler
            return this
        }

        fun setAnalyticsHandler(analyticsHandler: AnalyticsHandler): Configuration {
            this.analyticsHandler = analyticsHandler
            return this
        }

        fun setFilterRequestHandler(filterRequestHandler: FilterRequestHandler): Configuration {
            this.filterRequestHandler = filterRequestHandler
            return this
        }

        fun setNotificationEnabled(isEnabled: Boolean, handler: NotificationHandler?): Configuration {
            this.isNotificationEnabled = isEnabled
            this.notificationHandler = handler
            return this
        }

        fun setDonationLinks(donationLinks: Array<DonationLink>): Configuration {
            this.donationLinks = donationLinks.toList()
            return this
        }

        fun setNavigationIcon(navigationIcon: NavigationIcon): Configuration {
            this.navigationIcon = navigationIcon
            return this
        }

        fun setNavigationViewHeaderStyle(navigationViewHeader: NavigationViewHeader): Configuration {
            this.navigationViewHeader = navigationViewHeader
            return this
        }

        fun setAutomaticIconsCountEnabled(automaticIconsCountEnabled: Boolean): Configuration {
            this.isAutomaticIconsCountEnabled = automaticIconsCountEnabled
            return this
        }

        fun setHomeGridStyle(gridStyle: GridStyle): Configuration {
            this.homeGrid = gridStyle
            return this
        }

        fun setApplyGridStyle(gridStyle: GridStyle): Configuration {
            this.applyGrid = gridStyle
            return this
        }

        fun setRequestStyle(style: Style): Configuration {
            this.requestStyle = style
            return this
        }

        fun setWallpapersGridStyle(gridStyle: GridStyle): Configuration {
            this.wallpapersGrid = gridStyle
            return this
        }

        fun setAboutStyle(style: Style): Configuration {
            this.aboutStyle = style
            return this
        }

        fun setSocialIconColor(iconColor: IconColor): Configuration {
            this.socialIconColor = iconColor
            return this
        }

        fun setColoredApplyCard(coloredApplyCard: Boolean): Configuration {
            this.isColoredApplyCard = coloredApplyCard
            return this
        }

        fun setCustomIconsCount(customIconsCount: Int): Configuration {
            this.customIconsCount = customIconsCount
            return this
        }

        fun setShowTabIconsCount(showTabIconsCount: Boolean): Configuration {
            this.isShowTabIconsCount = showTabIconsCount
            return this
        }

        fun setShowTabAllIcons(showTabAllIcons: Boolean): Configuration {
            this.isShowTabAllIcons = showTabAllIcons
            return this
        }

        fun setTabAllIconsTitle(title: String): Configuration {
            this.tabAllIconsTitle = title
            if (this.tabAllIconsTitle.isEmpty()) this.tabAllIconsTitle = "All Icons"
            return this
        }

        fun setCategoryForTabAllIcons(categories: Array<String>): Configuration {
            this.categoryForTabAllIcons = categories
            return this
        }

        fun setExcludedCategoryForSearch(categories: Array<String>): Configuration {
            this.excludedCategoryForSearch = categories
            return this
        }

        fun setShadowEnabled(shadowEnabled: Boolean): Configuration {
            this.shadowOptions = ShadowOptions(shadowEnabled)
            return this
        }

        fun setShadowEnabled(shadowOptions: ShadowOptions): Configuration {
            this.shadowOptions = shadowOptions
            return this
        }

        fun setDashboardThemingEnabled(dashboardThemingEnabled: Boolean): Configuration {
            this.isDashboardThemingEnabled = dashboardThemingEnabled
            return this
        }

        fun setWallpaperGridPreviewQuality(@IntRange(from = 1, to = 10) quality: Int): Configuration {
            this.wallpaperGridPreviewQuality = quality
            return this
        }

        fun setGenerateAppFilter(generateAppFilter: Boolean): Configuration {
            this.isGenerateAppFilter = generateAppFilter
            return this
        }

        fun setGenerateAppMap(generateAppMap: Boolean): Configuration {
            this.isGenerateAppMap = generateAppMap
            return this
        }

        fun setGenerateThemeResources(generateThemeResources: Boolean): Configuration {
            this.isGenerateThemeResources = generateThemeResources
            return this
        }

        fun setIncludeIconRequestToEmailBody(includeIconRequestToEmailBody: Boolean): Configuration {
            this.isIncludeIconRequestToEmailBody = includeIconRequestToEmailBody
            return this
        }

        fun setCrashReportEnabled(crashReportEnabled: Boolean): Configuration {
            this.isCrashReportEnabled = crashReportEnabled
            return this
        }

        fun setWallpaperJsonStructure(jsonStructure: JsonStructure): Configuration {
            this.wallpaperJsonStructure = jsonStructure
            return this
        }

        fun setOtherApps(otherApps: Array<OtherApp>): Configuration {
            this.otherApps = otherApps.toList()
            return this
        }

        fun setHighQualityPreviewEnabled(highQualityPreviewEnabled: Boolean): Configuration {
            this.isHighQualityPreviewEnabled = highQualityPreviewEnabled
            return this
        }
    }

    enum class NavigationIcon {
        DEFAULT,
        STYLE_1,
        STYLE_2,
        STYLE_3,
        STYLE_4
    }

    enum class NavigationViewHeader {
        NORMAL,
        MINI,
        NONE
    }

    enum class GridStyle {
        CARD,
        FLAT
    }

    enum class Style {
        PORTRAIT_FLAT_LANDSCAPE_CARD,
        PORTRAIT_FLAT_LANDSCAPE_FLAT
    }

    enum class IconColor {
        PRIMARY_TEXT,
        ACCENT
    }

    class ShadowOptions {
        var isToolbarEnabled: Boolean
            private set
        var isCardEnabled: Boolean
            private set
        var isFabEnabled: Boolean
            private set
        var isTapIntroEnabled: Boolean
            private set

        constructor() {
            isTapIntroEnabled = true
            isFabEnabled = isTapIntroEnabled
            isCardEnabled = isFabEnabled
            isToolbarEnabled = isCardEnabled
        }

        constructor(shadowEnabled: Boolean) {
            isTapIntroEnabled = shadowEnabled
            isFabEnabled = isTapIntroEnabled
            isCardEnabled = isFabEnabled
            isToolbarEnabled = isCardEnabled
        }

        fun setToolbarEnabled(toolbarEnabled: Boolean): ShadowOptions {
            isToolbarEnabled = toolbarEnabled
            return this
        }

        fun setCardEnabled(cardEnabled: Boolean): ShadowOptions {
            isCardEnabled = cardEnabled
            return this
        }

        fun setFabEnabled(fabEnabled: Boolean): ShadowOptions {
            isFabEnabled = fabEnabled
            return this
        }

        fun setTapIntroEnabled(tapIntroEnabled: Boolean): ShadowOptions {
            isTapIntroEnabled = tapIntroEnabled
            return this
        }
    }

    open class OtherApp(
        val icon: String,
        val title: String,
        val description: String,
        val url: String
    )

    class DonationLink(icon: String, title: String, description: String, url: String) :
        OtherApp(icon, title, description, url)

    companion object {
        @JvmField
        var sRequestProperty: Request.Property? = null

        @JvmField
        var sZipPath: String? = null

        @JvmField
        var mDrawableClass: Class<*>? = null

        // This replaces the static mConfiguration field
        // We use slightly different logic to ensure it's initialized
        // but Kotlin doesn't allow 'lateinit' on primitive/nullable types easily in all contexts
        // so we keep it simple.
        private var configuration: Configuration = Configuration()

        @JvmStatic
        fun getConfiguration(): Configuration {
            return configuration
        }
    }
}
