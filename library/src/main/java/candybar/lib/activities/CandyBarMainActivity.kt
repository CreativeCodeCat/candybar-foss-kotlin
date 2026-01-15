package candybar.lib.activities

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.URLUtil
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.fragments.AboutFragment
import candybar.lib.fragments.ApplyFragment
import candybar.lib.fragments.FAQsFragment
import candybar.lib.fragments.HomeFragment
import candybar.lib.fragments.IconsBaseFragment
import candybar.lib.fragments.PresetsFragment
import candybar.lib.fragments.RequestFragment
import candybar.lib.fragments.SettingsFragment
import candybar.lib.fragments.WallpapersFragment
import candybar.lib.fragments.dialog.ChangelogFragment
import candybar.lib.fragments.dialog.IntentChooserFragment
import candybar.lib.helpers.ConfigurationHelper
import candybar.lib.helpers.IntentHelper
import candybar.lib.helpers.JsonHelper
import candybar.lib.helpers.LocaleHelper
import candybar.lib.helpers.NavigationViewHelper
import candybar.lib.helpers.ThemeHelper
import candybar.lib.helpers.WallpaperHelper
import candybar.lib.items.Home
import candybar.lib.items.Icon
import candybar.lib.items.Request
import candybar.lib.items.Theme
import candybar.lib.items.Wallpaper
import candybar.lib.preferences.Preferences
import candybar.lib.services.CandyBarService
import candybar.lib.tasks.IconRequestTask
import candybar.lib.tasks.IconsLoaderTask
import candybar.lib.tasks.WallpaperThumbPreloaderTask
import candybar.lib.utils.CandyBarGlideModule
import candybar.lib.utils.Extras
import candybar.lib.utils.listeners.RequestListener
import candybar.lib.utils.listeners.SearchListener
import candybar.lib.utils.listeners.WallpapersListener
import candybar.lib.utils.views.HeaderView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import com.danimahardhika.android.helpers.core.FileHelper
import com.danimahardhika.android.helpers.core.SoftKeyboardHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.danimahardhika.android.helpers.permission.PermissionCode
import com.google.android.material.navigation.NavigationView
import java.io.IOException
import candybar.lib.helpers.DrawableHelper as CandyBarDrawableHelper

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

abstract class CandyBarMainActivity : AppCompatActivity(),
    ActivityCompat.OnRequestPermissionsResultCallback, RequestListener,
    SearchListener, WallpapersListener {

    private lateinit var mToolbarTitle: TextView
    private lateinit var mDrawerLayout: DrawerLayout
    private lateinit var mNavigationView: NavigationView

    private var mFragmentTag: Extras.Tag = Extras.Tag.HOME
    private var mPosition = 0
    private var mLastPosition = 0
    private lateinit var mDrawerToggle: ActionBarDrawerToggle
    private lateinit var mFragManager: FragmentManager

    private var mIsMenuVisible = true
    private var prevIsDarkTheme = false

    private lateinit var mConfig: ActivityConfiguration

    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (mFragManager.backStackEntryCount > 0) {
                clearBackStack()
                return
            }

            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawers()
                return
            }

            if (mFragmentTag != Extras.Tag.HOME) {
                mPosition = 0
                mLastPosition = 0
                setFragment(getFragment(mPosition))
            }
        }
    }

    abstract fun onInit(): ActivityConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        val isMaterialYou = Preferences.get(this).isMaterialYou
        val nightMode = when (Preferences.get(this).theme) {
            Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)

        LocaleHelper.setLocale(this)
        super.onCreate(savedInstanceState)
        setTheme(if (isMaterialYou) R.style.CandyBar_Theme_App_MaterialYou else R.style.CandyBar_Theme_App_DayNight)
        setContentView(R.layout.activity_main)

        mDrawerLayout = findViewById(R.id.drawer_layout)
        mNavigationView = findViewById(R.id.navigation_view)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        mToolbarTitle = findViewById(R.id.toolbar_title)

        toolbar.popupTheme = if (isMaterialYou) R.style.CandyBar_Theme_App_MaterialYou else R.style.CandyBar_Theme_App_DayNight
        toolbar.title = ""
        setSupportActionBar(toolbar)

        mFragManager = supportFragmentManager

        initNavigationView(toolbar)
        initNavigationViewHeader()
        registerBackPressHandler()

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val params = v.layoutParams as ViewGroup.MarginLayoutParams
            params.topMargin = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            findViewById<View>(R.id.inset_padding).layoutParams.height = params.topMargin
            WindowInsetsCompat.CONSUMED
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION or WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        )

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        val isLightStatus = ColorHelper.isLightColor(ColorHelper.getAttributeColor(this, R.attr.cb_colorPrimaryDark))
        controller.isAppearanceLightStatusBars = isLightStatus

        val isLightNav = ColorHelper.isLightColor(ColorHelper.getAttributeColor(this, R.attr.cb_navigationBar))
        controller.isAppearanceLightNavigationBars = isLightNav

        try {
            startService(Intent(this, CandyBarService::class.java))
        } catch (e: IllegalStateException) {
            LogUtil.e("Unable to start CandyBarService. App is probably running in background.")
        }

        //Todo: wait until google fix the issue, then enable wallpaper crop again on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Preferences.get(this).isCropWallpaper = false
        }

        mConfig = onInit()

        mPosition = 0
        mLastPosition = 0
        if (savedInstanceState != null) {
            mPosition = savedInstanceState.getInt(Extras.EXTRA_POSITION, 0)
            mLastPosition = mPosition
            onSearchExpanded(false)
        }

        val bundle = intent.extras
        if (bundle != null) {
            val position = bundle.getInt(Extras.EXTRA_POSITION, -1)
            if (position in 0..5) {
                mPosition = position
                mLastPosition = position
            }
        }

        IntentHelper.sAction = IntentHelper.getAction(intent)
        if (IntentHelper.sAction == IntentHelper.ACTION_DEFAULT) {
            setFragment(getFragment(mPosition))
        } else {
            setFragment(getActionFragment(IntentHelper.sAction))
        }

        checkWallpapers()
        WallpaperThumbPreloaderTask(this).execute()
        IconRequestTask(this).executeOnThreadPool()
        IconsLoaderTask(this).execute()

        if (Preferences.get(this).isNewVersion) {
            Preferences.get(this).isFirstRun = true
        }

        val askNotificationPermission = Runnable {
            val showToast = Runnable {
                Toast.makeText(this, resources.getString(R.string.permission_notification_denied_1), Toast.LENGTH_LONG).show()
                Toast.makeText(this, resources.getString(R.string.permission_notification_denied_2), Toast.LENGTH_LONG).show()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && CandyBarApplication.getConfiguration().isNotificationEnabled) {
                if (NotificationManagerCompat.from(this).areNotificationsEnabled()) {
                    CandyBarApplication.getConfiguration().notificationHandler?.setMode(Preferences.get(this).isNotificationsEnabled)
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val permissionState = ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                        if (permissionState != PackageManager.PERMISSION_GRANTED) {
                            if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                                showToast.run()
                            } else {
                                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_CODE)
                            }
                        }
                    } else {
                        showToast.run()
                    }
                }
            }
        }

        val onNewVersion = Runnable {
            ChangelogFragment.showChangelog(mFragManager, askNotificationPermission)
            val cache = cacheDir
            FileHelper.clearDirectory(cache)
        }

        if (Preferences.get(this).isFirstRun) {
            val onAllChecksCompleted = Runnable {
                Preferences.get(this).isFirstRun = false
                onNewVersion.run()
            }

            onAllChecksCompleted.run()
        }

        askNotificationPermission.run()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        mDrawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (prevIsDarkTheme != ThemeHelper.isDarkTheme(this)) {
            recreate()
            return
        }
        LocaleHelper.setLocale(this)
        if (mIsMenuVisible) mDrawerToggle.onConfigurationChanged(newConfig)
    }

    override fun attachBaseContext(newBase: Context) {
        LocaleHelper.setLocale(newBase)
        super.attachBaseContext(newBase)
    }

    override fun onNewIntent(intent: Intent) {
        val action = IntentHelper.getAction(intent)
        if (action != IntentHelper.ACTION_DEFAULT)
            setFragment(getActionFragment(action))
        super.onNewIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(Extras.EXTRA_POSITION, mPosition)
        Database.get(this.applicationContext).closeDatabase()
        super.onSaveInstanceState(outState)
    }

    private fun registerBackPressHandler() {
        onBackPressedDispatcher.addCallback(backPressedCallback)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionCode.STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                recreate()
                return
            }
            Toast.makeText(this, R.string.permission_storage_denied, Toast.LENGTH_LONG).show()
        }
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                CandyBarApplication.getConfiguration().notificationHandler?.setMode(Preferences.get(this).isNotificationsEnabled)
            } else {
                Toast.makeText(this, resources.getString(R.string.permission_notification_denied_1), Toast.LENGTH_LONG).show()
                Toast.makeText(this, resources.getString(R.string.permission_notification_denied_2), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestSelected(count: Int) {
        if (mFragmentTag == Extras.Tag.REQUEST) {
            var title = resources.getString(R.string.navigation_view_request)
            if (count > 0) title += " ($count)"
            mToolbarTitle.text = title
        }
    }

    override fun onRequestBuilt(intent: Intent?, type: Int) {
        if (type == IntentChooserFragment.ICON_REQUEST) {
            if (RequestFragment.sSelectedRequests == null)
                return

            if (Preferences.get(this).isPremiumRequest) {
                val count = Preferences.get(this).premiumRequestCount - RequestFragment.sSelectedRequests!!.size
                Preferences.get(this).premiumRequestCount = count
                if (count == 0) {
                    // Refresh?
                }
            } else {
                if (resources.getBoolean(R.bool.enable_icon_request_limit)) {
                    val used = Preferences.get(this).regularRequestUsed
                    Preferences.get(this).regularRequestUsed = used + RequestFragment.sSelectedRequests!!.size
                }
            }

            if (mFragmentTag == Extras.Tag.REQUEST) {
                val fragment = mFragManager.findFragmentByTag(Extras.Tag.REQUEST.value) as RequestFragment?
                fragment?.refreshIconRequest()
            }
        }

        if (intent != null) {
            try {
                startActivity(intent)
            } catch (e: IllegalArgumentException) {
                startActivity(
                    Intent.createChooser(
                        intent,
                        resources.getString(R.string.app_client)
                    )
                )
            }
        }

        CandyBarApplication.sRequestProperty = null
        CandyBarApplication.sZipPath = null
    }

    override fun onWallpapersChecked(wallpaperCount: Int) {
        Preferences.get(this).availableWallpapersCount = wallpaperCount

        if (mFragmentTag == Extras.Tag.HOME) {
            val fragment = mFragManager.findFragmentByTag(Extras.Tag.HOME.value) as HomeFragment?
            fragment?.resetWallpapersCount()
        }
    }

    override fun onSearchExpanded(expand: Boolean) {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        mIsMenuVisible = !expand

        if (expand) {
            val color = ColorHelper.getAttributeColor(this, R.attr.cb_toolbarIcon)
            toolbar.navigationIcon = DrawableHelper.getTintedDrawable(
                this, R.drawable.ic_toolbar_back, color
            )
        } else {
            SoftKeyboardHelper.closeKeyboard(this)
            ColorHelper.setStatusBarColor(this, Color.TRANSPARENT, true)
            if (CandyBarApplication.getConfiguration().navigationIcon == CandyBarApplication.NavigationIcon.DEFAULT) {
                mDrawerToggle.drawerArrowDrawable = DrawerArrowDrawable(this)
            } else {
                toolbar.navigationIcon = ConfigurationHelper.getNavigationIcon(
                    this,
                    CandyBarApplication.getConfiguration().navigationIcon
                )
            }

            toolbar.setNavigationOnClickListener { mDrawerLayout.openDrawer(GravityCompat.START) }
        }

        mDrawerLayout.setDrawerLockMode(
            if (expand) DrawerLayout.LOCK_MODE_LOCKED_CLOSED else DrawerLayout.LOCK_MODE_UNLOCKED
        )
        supportInvalidateOptionsMenu()
    }

    private fun initNavigationView(toolbar: Toolbar) {
        mDrawerToggle = object : ActionBarDrawerToggle(
            this, mDrawerLayout, toolbar, R.string.txt_open, R.string.txt_close
        ) {
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                SoftKeyboardHelper.closeKeyboard(this@CandyBarMainActivity)
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                selectPosition(mPosition)
            }
        }
        mDrawerToggle.isDrawerIndicatorEnabled = false
        toolbar.navigationIcon = ConfigurationHelper.getNavigationIcon(
            this,
            CandyBarApplication.getConfiguration().navigationIcon
        )
        toolbar.setNavigationOnClickListener { mDrawerLayout.openDrawer(GravityCompat.START) }

        if (CandyBarApplication.getConfiguration().navigationIcon == CandyBarApplication.NavigationIcon.DEFAULT) {
            val drawerArrowDrawable = DrawerArrowDrawable(this)
            drawerArrowDrawable.color = ColorHelper.getAttributeColor(this, R.attr.cb_toolbarIcon)
            drawerArrowDrawable.isSpinEnabled = true
            mDrawerToggle.drawerArrowDrawable = drawerArrowDrawable
            mDrawerToggle.isDrawerIndicatorEnabled = true
        }

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        mDrawerLayout.addDrawerListener(mDrawerToggle)

        NavigationViewHelper.initApply(mNavigationView)
        NavigationViewHelper.initIconRequest(mNavigationView)
        NavigationViewHelper.initWallpapers(mNavigationView)
        NavigationViewHelper.initPresets(mNavigationView)

        val itemStateList = ContextCompat.getColorStateList(
            this,
            R.color.navigation_view_item_highlight
        )
        mNavigationView.itemTextColor = itemStateList
        mNavigationView.itemIconTintList = itemStateList

        mNavigationView.setNavigationItemSelectedListener { item ->
            val id = item.itemId
            mPosition = when (id) {
                R.id.navigation_view_home -> Extras.Tag.HOME.idx
                R.id.navigation_view_apply -> Extras.Tag.APPLY.idx
                R.id.navigation_view_icons -> Extras.Tag.ICONS.idx
                R.id.navigation_view_request -> Extras.Tag.REQUEST.idx
                R.id.navigation_view_wallpapers -> Extras.Tag.WALLPAPERS.idx
                R.id.navigation_view_presets -> Extras.Tag.PRESETS.idx
                R.id.navigation_view_settings -> Extras.Tag.SETTINGS.idx
                R.id.navigation_view_faqs -> Extras.Tag.FAQS.idx
                R.id.navigation_view_about -> Extras.Tag.ABOUT.idx
                else -> mPosition
            }

            item.isChecked = true
            mDrawerLayout.closeDrawers()
            true
        }
    }

    private fun initNavigationViewHeader() {
        if (CandyBarApplication.getConfiguration().navigationViewHeader == CandyBarApplication.NavigationViewHeader.NONE) {
            mNavigationView.removeHeaderView(mNavigationView.getHeaderView(0))
            return
        }

        var imageUrl = resources.getString(R.string.navigation_view_header)
        val titleText = resources.getString(R.string.navigation_view_header_title)
        val header = mNavigationView.getHeaderView(0)
        val image = header.findViewById<HeaderView>(R.id.header_image)
        val container = header.findViewById<LinearLayout>(R.id.header_title_container)
        val title = header.findViewById<TextView>(R.id.header_title)
        val version = header.findViewById<TextView>(R.id.header_version)

        if (CandyBarApplication.getConfiguration().navigationViewHeader == CandyBarApplication.NavigationViewHeader.MINI) {
            image.setRatio(16, 9)
        }

        if (titleText.isEmpty()) {
            container.visibility = View.GONE
        } else {
            title.text = titleText
            try {
                val versionText = "v" + packageManager.getPackageInfo(packageName, 0).versionName
                version.text = versionText
            } catch (ignored: Exception) {
            }
        }

        if (ColorHelper.isValidColor(imageUrl)) {
            image.setBackgroundColor(Color.parseColor(imageUrl))
            return
        }

        if (!URLUtil.isValidUrl(imageUrl)) {
            imageUrl = "drawable://" + CandyBarDrawableHelper.getDrawableId(imageUrl)
        }

        if (CandyBarGlideModule.isValidContextForGlide(this)) {
            Glide.with(this)
                .load(imageUrl)
                .override(720)
                .optionalCenterInside()
                .diskCacheStrategy(
                    if (imageUrl.contains("drawable://")) DiskCacheStrategy.NONE else DiskCacheStrategy.RESOURCE
                )
                .into(image)
        }
    }

    private fun checkWallpapers() {
        if (Preferences.get(this).isConnectedToNetwork) {
            Thread {
                try {
                    if (WallpaperHelper.getWallpaperType(this) != WallpaperHelper.CLOUD_WALLPAPERS)
                        return@Thread

                    val stream = WallpaperHelper.getJSONStream(this)

                    if (stream != null) {
                        val list = JsonHelper.parseList(stream) ?: return@Thread

                        val wallpapers: MutableList<Wallpaper> = ArrayList()
                        for (i in list.indices) {
                            val wallpaper = JsonHelper.getWallpaper(list[i]!!)
                            if (wallpaper != null) {
                                if (!wallpapers.contains(wallpaper)) {
                                    wallpapers.add(wallpaper)
                                } else {
                                    LogUtil.e("Duplicate wallpaper found: " + wallpaper.url)
                                }
                            }
                        }

                        this.runOnUiThread { onWallpapersChecked(wallpapers.size) }
                    }
                } catch (e: IOException) {
                    LogUtil.e(Log.getStackTraceString(e))
                }
            }.start()
        }

        val size = Preferences.get(this).availableWallpapersCount
        if (size > 0) {
            onWallpapersChecked(size)
        }
    }

    private fun clearBackStack() {
        if (mFragManager.backStackEntryCount > 0) {
            mFragManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            onSearchExpanded(false)
        }
    }

    fun selectPosition(position: Int) {
        if (position == 3) {
            if (!resources.getBoolean(R.bool.enable_icon_request) &&
                resources.getBoolean(R.bool.enable_premium_request)
            ) {
                if (!Preferences.get(this).isPremiumRequestEnabled)
                    return
            }
        }

        if (position == 4) {
            if (WallpaperHelper.getWallpaperType(this) == WallpaperHelper.EXTERNAL_APP) {
                mPosition = mLastPosition
                mNavigationView.menu.getItem(mPosition).isChecked = true
                WallpaperHelper.launchExternalApp(this@CandyBarMainActivity)
                return
            }
        }

        if (position != mLastPosition) {
            mPosition = position
            mLastPosition = position
            setFragment(getFragment(position))
        }
    }

    private fun setFragment(fragment: Fragment) {
        clearBackStack()

        val ft = mFragManager.beginTransaction()
            .replace(R.id.container, fragment, mFragmentTag.value)
        try {
            ft.commit()
        } catch (e: Exception) {
            ft.commitAllowingStateLoss()
        }

        val menu = mNavigationView.menu
        menu.getItem(mPosition).isChecked = true
        mToolbarTitle.text = menu.getItem(mPosition).title

        backPressedCallback.isEnabled = mFragmentTag != Extras.Tag.HOME
    }

    private fun getFragment(position: Int): Fragment {
        return when (position) {
            Extras.Tag.HOME.idx -> {
                mFragmentTag = Extras.Tag.HOME
                HomeFragment()
            }

            Extras.Tag.APPLY.idx -> {
                mFragmentTag = Extras.Tag.APPLY
                ApplyFragment()
            }

            Extras.Tag.ICONS.idx -> {
                mFragmentTag = Extras.Tag.ICONS
                IconsBaseFragment()
            }

            Extras.Tag.REQUEST.idx -> {
                mFragmentTag = Extras.Tag.REQUEST
                RequestFragment()
            }

            Extras.Tag.WALLPAPERS.idx -> {
                mFragmentTag = Extras.Tag.WALLPAPERS
                WallpapersFragment()
            }

            Extras.Tag.PRESETS.idx -> {
                mFragmentTag = Extras.Tag.PRESETS
                PresetsFragment()
            }

            Extras.Tag.SETTINGS.idx -> {
                mFragmentTag = Extras.Tag.SETTINGS
                SettingsFragment()
            }

            Extras.Tag.FAQS.idx -> {
                mFragmentTag = Extras.Tag.FAQS
                FAQsFragment()
            }

            Extras.Tag.ABOUT.idx -> {
                mFragmentTag = Extras.Tag.ABOUT
                AboutFragment()
            }

            else -> {
                mFragmentTag = Extras.Tag.HOME
                HomeFragment()
            }
        }
    }

    private fun getActionFragment(action: Int): Fragment {
        return when (action) {
            IntentHelper.ICON_PICKER, IntentHelper.IMAGE_PICKER -> {
                mFragmentTag = Extras.Tag.ICONS
                mPosition = mFragmentTag.idx
                mLastPosition = mPosition
                IconsBaseFragment()
            }

            IntentHelper.WALLPAPER_PICKER -> {
                if (WallpaperHelper.getWallpaperType(this) == WallpaperHelper.CLOUD_WALLPAPERS) {
                    mFragmentTag = Extras.Tag.WALLPAPERS
                    mPosition = mFragmentTag.idx
                    mLastPosition = mPosition
                    WallpapersFragment()
                } else {
                    mFragmentTag = Extras.Tag.HOME
                    mPosition = mFragmentTag.idx
                    mLastPosition = mPosition
                    HomeFragment()
                }
            }

            else -> {
                mFragmentTag = Extras.Tag.HOME
                mPosition = mFragmentTag.idx
                mLastPosition = mPosition
                HomeFragment()
            }
        }
    }

    class ActivityConfiguration {
        var randomString: ByteArray? = null
            private set
        var donationProductsId: Array<String>? = null
            private set
        var premiumRequestProductsId: Array<String>? = null
            private set
        var premiumRequestProductsCount: IntArray? = null
            private set

        fun setRandomString(randomString: ByteArray): ActivityConfiguration {
            this.randomString = randomString
            return this
        }

        fun setDonationProductsId(productsId: Array<String>): ActivityConfiguration {
            this.donationProductsId = productsId
            return this
        }

        fun setPremiumRequestProducts(ids: Array<String>, counts: IntArray): ActivityConfiguration {
            this.premiumRequestProductsId = ids
            this.premiumRequestProductsCount = counts
            return this
        }
    }

    companion object {
        @JvmField
        var sMissedApps: MutableList<Request>? = null

        @JvmField
        var sSections: MutableList<Icon>? = null

        @JvmField
        var sHomeIcon: Home? = null

        @JvmField
        var sInstalledAppsCount = 0

        @JvmField
        var sIconsCount = 0

        private const val NOTIFICATION_PERMISSION_CODE = 10
    }
}
