package candybar.lib.activities

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.Transition
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.palette.graphics.Palette
import candybar.lib.R
import candybar.lib.adapters.WallpapersAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.helpers.LocaleHelper
import candybar.lib.helpers.TapIntroHelper
import candybar.lib.helpers.ThemeHelper
import candybar.lib.items.PopupItem
import candybar.lib.items.Wallpaper
import candybar.lib.preferences.Preferences
import candybar.lib.tasks.WallpaperApplyTask
import candybar.lib.tasks.WallpaperPropertiesLoaderTask
import candybar.lib.utils.CandyBarGlideModule
import candybar.lib.utils.Extras
import candybar.lib.utils.Popup
import candybar.lib.utils.WallpaperDownloader
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.danimahardhika.android.helpers.animation.AnimationHelper
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import com.danimahardhika.android.helpers.permission.PermissionCode
import com.github.chrisbanes.photoview.PhotoView
import com.kogitune.activitytransition.ActivityTransition
import com.kogitune.activitytransition.ExitActivityTransition

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

class CandyBarWallpaperActivity : AppCompatActivity(), View.OnClickListener, View.OnLongClickListener,
    ActivityCompat.OnRequestPermissionsResultCallback, WallpaperPropertiesLoaderTask.Callback {

    private lateinit var mImageView: PhotoView
    private lateinit var mProgress: ProgressBar
    private lateinit var mBottomBar: LinearLayout
    private lateinit var mName: TextView
    private lateinit var mAuthor: TextView
    private lateinit var mBack: ImageView
    private lateinit var mMenuApply: ImageView
    private lateinit var mMenuSave: ImageView

    private var mIsEnter = false
    private var mIsResumed = false

    private var mWallpaper: Wallpaper? = null
    private var mWallpaperName: String? = null
    private var mRunnable: Runnable? = null
    private var mHandler: Handler? = null
    private var mAttacher: PhotoView? = null
    private var mExitTransition: ExitActivityTransition? = null

    private var prevIsDarkTheme = false

    override fun onCreate(savedInstanceState: Bundle?) {
        prevIsDarkTheme = ThemeHelper.isDarkTheme(this)
        super.setTheme(R.style.CandyBar_Theme_Wallpaper)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper)
        mIsEnter = true

        mImageView = findViewById(R.id.wallpaper)
        mProgress = findViewById(R.id.progress)
        mBottomBar = findViewById(R.id.bottom_bar)
        mName = findViewById(R.id.name)
        mAuthor = findViewById(R.id.author)
        mBack = findViewById(R.id.back)
        mMenuApply = findViewById(R.id.menu_apply)
        mMenuSave = findViewById(R.id.menu_save)

        mProgress.indeterminateDrawable.colorFilter = android.graphics.PorterDuffColorFilter(
            Color.parseColor("#CCFFFFFF"), PorterDuff.Mode.SRC_IN
        )
        mBack.setImageDrawable(
            DrawableHelper.getTintedDrawable(
                this, R.drawable.ic_toolbar_back, Color.WHITE
            )
        )
        mBack.setOnClickListener(this)
        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                closePreview()
            }
        })

        var url = ""
        if (savedInstanceState != null) {
            url = savedInstanceState.getString(Extras.EXTRA_URL, "")
        }

        val bundle = intent.extras
        if (bundle != null) {
            url = bundle.getString(Extras.EXTRA_URL, "")
        }

        mWallpaper = Database.get(this.applicationContext).getWallpaper(url)
        if (mWallpaper == null) {
            finish()
            return
        }

        mWallpaperName = mWallpaper!!.url.split("/").last()

        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "wallpaper",
            hashMapOf<String, Any>(
                "url" to mWallpaperName!!,
                "action" to "preview"
            )
        )

        initBottomBar()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootview)) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            if (mBack.layoutParams is CoordinatorLayout.LayoutParams) {
                (mBack.layoutParams as CoordinatorLayout.LayoutParams).topMargin = systemBars.top
            }

            val container = findViewById<LinearLayout>(R.id.bottom_bar_container)
            container.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)

            if (container.layoutParams is LinearLayout.LayoutParams) {
                val height = resources.getDimensionPixelSize(R.dimen.bottom_bar_height)
                (container.layoutParams as LinearLayout.LayoutParams).height = height + systemBars.bottom
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowInsetsCompat.CONSUMED
            } else {
                @Suppress("DEPRECATION")
                insets.consumeSystemWindowInsets()
            }
        }

        if (!mIsResumed) {
            mExitTransition = ActivityTransition
                .with(intent)
                .to(this, mImageView, Extras.EXTRA_IMAGE)
                .duration(300)
                .start(savedInstanceState)
        }

        if (mImageView.drawable == null) {
            var color = mWallpaper!!.color
            if (color == 0) {
                color = ColorHelper.getAttributeColor(this, R.attr.cb_cardBackground)
            }

            AnimationHelper.setBackgroundColor(findViewById(R.id.rootview), Color.TRANSPARENT, color).start()
            mProgress.indeterminateDrawable.colorFilter = android.graphics.PorterDuffColorFilter(
                ColorHelper.setColorAlpha(ColorHelper.getTitleTextColor(color), 0.7f),
                PorterDuff.Mode.SRC_IN
            )
        }

        if (savedInstanceState == null) {
            val transition = window.sharedElementEnterTransition

            if (transition != null) {
                transition.addListener(object : Transition.TransitionListener {
                    override fun onTransitionStart(transition: Transition) {}

                    override fun onTransitionEnd(transition: Transition) {
                        if (mIsEnter) {
                            mIsEnter = false

                            AnimationHelper.fade(mBottomBar).duration(400).start()
                            loadWallpaper()
                        }
                    }

                    override fun onTransitionCancel(transition: Transition) {}

                    override fun onTransitionPause(transition: Transition) {}

                    override fun onTransitionResume(transition: Transition) {}
                })

                return
            }
        }

        mRunnable = Runnable {
            AnimationHelper.fade(mBottomBar).duration(400).start()
            loadWallpaper()

            mRunnable = null
            mHandler = null
        }
        mHandler = Handler(Looper.getMainLooper())
        mHandler!!.postDelayed(mRunnable!!, 700)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (prevIsDarkTheme != ThemeHelper.isDarkTheme(this)) {
            recreate()
            return
        }
        LocaleHelper.setLocale(this)
    }

    override fun attachBaseContext(newBase: Context) {
        LocaleHelper.setLocale(newBase)
        super.attachBaseContext(newBase)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (mWallpaper != null) {
            outState.putString(Extras.EXTRA_URL, mWallpaper!!.url)
        }

        outState.putBoolean(Extras.EXTRA_RESUMED, true)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        if (Preferences.get(this).isCropWallpaper) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        }
        Glide.get(this).clearMemory()
        mAttacher = null
        super.onDestroy()
    }

    private fun closePreview() {
        WallpapersAdapter.sIsClickable = true
        if (mHandler != null && mRunnable != null)
            mHandler!!.removeCallbacks(mRunnable!!)

        mExitTransition?.exit(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            closePreview()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(view: View) {
        val id = view.id
        if (id == R.id.back) {
            closePreview()
        } else if (id == R.id.menu_apply) {
            val popup = Popup.Builder(this)
                .to(mMenuApply)
                .list(PopupItem.getApplyItems(this))
                .callback { p, position ->
                    val item = p.items[position]
                    if (item.type == PopupItem.Type.WALLPAPER_CROP) {
                        Preferences.get(this).isCropWallpaper = !item.checkboxValue
                        item.checkboxValue = Preferences.get(this).isCropWallpaper

                        p.updateItem(position, item)
                        if (Preferences.get(this).isCropWallpaper) {
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                            return@callback
                        }

                        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        return@callback
                    } else {
                        var rectF: RectF? = null
                        if (Preferences.get(this).isCropWallpaper) {
                            if (mAttacher != null)
                                rectF = mAttacher!!.displayRect
                        }


                        val task = WallpaperApplyTask(this, mWallpaper!!)
                            .crop(rectF)


                        if (item.type == PopupItem.Type.LOCKSCREEN) {
                            CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                                "wallpaper",
                                hashMapOf<String, Any>(
                                    "url" to mWallpaperName!!,
                                    "section" to "lockscreen",
                                    "action" to "apply"
                                )
                            )
                            task.to(WallpaperApplyTask.Apply.LOCKSCREEN)
                        } else if (item.type == PopupItem.Type.HOMESCREEN) {
                            CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                                "wallpaper",
                                hashMapOf<String, Any>(
                                    "url" to mWallpaperName!!,
                                    "section" to "homescreen",
                                    "action" to "apply"
                                )
                            )
                            task.to(WallpaperApplyTask.Apply.HOMESCREEN)
                        } else if (item.type == PopupItem.Type.HOMESCREEN_LOCKSCREEN) {
                            CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                                "wallpaper",
                                hashMapOf<String, Any>(
                                    "url" to mWallpaperName!!,
                                    "section" to "homescreen_and_lockscreen",
                                    "action" to "apply"
                                )
                            )
                            task.to(WallpaperApplyTask.Apply.HOMESCREEN_LOCKSCREEN)
                        }

                        task.executeOnThreadPool()
                    }

                    p.dismiss()
                }
                .build()

            if (resources.getBoolean(R.bool.enable_wallpaper_download)) {
                popup.removeItem(popup.items.size - 1)
            }
            popup.show()
        } else if (id == R.id.menu_save) {
            WallpaperDownloader.prepare(this)
                .wallpaper(mWallpaper!!)
                .start()
        }
    }

    override fun onLongClick(view: View): Boolean {
        val id = view.id
        var res = 0
        if (id == R.id.menu_apply) {
            res = R.string.wallpaper_apply
        } else if (id == R.id.menu_save) {
            res = R.string.wallpaper_save_to_device
        }

        if (res == 0) return false

        Toast.makeText(this, res, Toast.LENGTH_SHORT).show()
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionCode.STORAGE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                WallpaperDownloader.prepare(this).wallpaper(mWallpaper!!).start()
            } else {
                Toast.makeText(this, R.string.permission_storage_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onPropertiesReceived(wallpaper: Wallpaper?) {
        if (wallpaper == null) return

        mWallpaper?.setDimensions(wallpaper.dimensions)
        mWallpaper?.setSize(wallpaper.size)
        mWallpaper?.setMimeType(wallpaper.mimeType)
    }

    private fun initBottomBar() {
        mName.text = mWallpaper!!.name
        mName.setTextColor(Color.WHITE)
        mAuthor.text = mWallpaper!!.author
        mAuthor.setTextColor(ColorHelper.setColorAlpha(Color.WHITE, 0.7f))
        mMenuSave.setImageDrawable(
            DrawableHelper.getTintedDrawable(
                this, R.drawable.ic_toolbar_download, Color.WHITE
            )
        )
        mMenuApply.setImageDrawable(
            DrawableHelper.getTintedDrawable(
                this, R.drawable.ic_toolbar_apply_options, Color.WHITE
            )
        )

        if (resources.getBoolean(R.bool.enable_wallpaper_download)) {
            mMenuSave.visibility = View.VISIBLE
        }

        mMenuApply.setOnClickListener(this)
        mMenuSave.setOnClickListener(this)

        mMenuApply.setOnLongClickListener(this)
        mMenuSave.setOnLongClickListener(this)
    }

    private fun loadWallpaper() {
        mAttacher = null

        WallpaperPropertiesLoaderTask(this, mWallpaper, this)
            .executeOnThreadPool()

        if (CandyBarGlideModule.isValidContextForGlide(this)) {
            Glide.with(this)
                .asBitmap()
                .load(mWallpaper!!.url)
                .override(2000)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .timeout(10000)
                .listener(object : RequestListener<Bitmap> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (mWallpaper!!.color == 0) {
                            mWallpaper!!.color = ColorHelper.getAttributeColor(
                                this@CandyBarWallpaperActivity,
                                com.google.android.material.R.attr.colorSecondary
                            )
                        }

                        return true
                    }

                    override fun onResourceReady(
                        loadedImage: Bitmap?,
                        model: Any?,
                        target: Target<Bitmap>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        if (loadedImage != null && mWallpaper!!.color == 0) {
                            Palette.from(loadedImage).generate { palette ->
                                if (palette != null) {
                                    val accent = ColorHelper.getAttributeColor(
                                        this@CandyBarWallpaperActivity,
                                        com.google.android.material.R.attr.colorSecondary
                                    )
                                    var color = palette.getVibrantColor(accent)
                                    if (color == accent)
                                        color = palette.getMutedColor(accent)

                                    mWallpaper!!.color = color
                                    Database.get(this@CandyBarWallpaperActivity).updateWallpaper(mWallpaper)
                                }

                                onWallpaperLoaded()
                            }
                        } else {
                            onWallpaperLoaded()
                        }

                        return false
                    }
                })
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                    ) {
                        mImageView.setImageBitmap(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) { /* Do nothing */
                    }
                })

            if (Preferences.get(this).isCropWallpaper) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }

        AnimationHelper.fade(mProgress).start()
    }

    private fun onWallpaperLoaded() {
        mAttacher = PhotoView(mImageView.context)
        mAttacher!!.scaleType = ImageView.ScaleType.CENTER_CROP

        AnimationHelper.fade(mProgress).start()
        mRunnable = null
        mHandler = null
        mIsResumed = false

        if (this.resources.getBoolean(R.bool.show_intro)) {
            TapIntroHelper.showWallpaperPreviewIntro(this, mWallpaper!!.color)
        }
    }
}
