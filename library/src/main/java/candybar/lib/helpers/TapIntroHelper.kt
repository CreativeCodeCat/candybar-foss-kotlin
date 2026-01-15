package candybar.lib.helpers

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import candybar.lib.R
import candybar.lib.adapters.HomeAdapter
import candybar.lib.preferences.Preferences
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.UnitHelper.toDp
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence
import com.getkeepsafe.taptargetview.TapTargetView

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

object TapIntroHelper {

    @JvmStatic
    @SuppressLint("StringFormatInvalid")
    fun showHomeIntros(
        @NonNull context: Context,
        @Nullable recyclerView: RecyclerView?,
        @Nullable manager: StaggeredGridLayoutManager?,
        position: Int
    ) {
        if (Preferences.get(context).isTimeToShowHomeIntro) {
            val activity = context as AppCompatActivity
            val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    var titleColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroTitle)
                    var descriptionColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroDescription)
                    var circleColorInner = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleInner)
                    var circleColorOuter = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleOuter)

                    if (context.resources.getBoolean(R.bool.use_legacy_intro_colors)) {
                        val primary = ColorHelper.getAttributeColor(context, R.attr.cb_toolbarIcon)
                        val secondary = ColorHelper.setColorAlpha(primary, 0.7f)
                        titleColor = primary
                        descriptionColor = ColorHelper.setColorAlpha(primary, 0.7f)
                        circleColorInner = secondary
                        circleColorOuter = 0
                    }

                    val tapTargetSequence = TapTargetSequence(activity)
                    tapTargetSequence.continueOnCancel(true)

                    val titleTypeface = TypefaceHelper.getMedium(context)

                    if (toolbar != null) {
                        val tapTarget = TapTarget.forToolbarNavigationIcon(
                            toolbar,
                            context.resources.getString(R.string.tap_intro_home_navigation),
                            context.resources.getString(R.string.tap_intro_home_navigation_desc)
                        )
                            .titleTextColorInt(titleColor)
                            .descriptionTextColorInt(descriptionColor)
                            .targetCircleColorInt(circleColorInner)
                            .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                        if (circleColorOuter != 0) {
                            tapTarget.outerCircleColorInt(circleColorOuter)
                        }

                        titleTypeface?.let {
                            tapTarget.textTypeface(it)
                        }

                        tapTargetSequence.target(tapTarget)
                    }

                    if (recyclerView != null) {
                        val adapter = recyclerView.adapter as? HomeAdapter
                        if (adapter != null) {
                            if (context.resources.getBoolean(R.bool.enable_apply)) {
                                if (position in 0 until adapter.itemCount) {
                                    val holder = recyclerView.findViewHolderForAdapterPosition(position)
                                    if (holder != null) {
                                        val view = holder.itemView
                                        val circleScale = 100.0f / context.resources.getInteger(R.integer.tap_intro_circle_scale_percent)
                                        val targetRadius = (toDp(context, view.measuredWidth.toFloat()) - 20f) * circleScale

                                        val desc = context.resources.getString(
                                            R.string.tap_intro_home_apply_desc,
                                            context.resources.getString(R.string.app_name)
                                        )
                                        val tapTarget = TapTarget.forView(
                                            view,
                                            context.resources.getString(R.string.tap_intro_home_apply),
                                            desc
                                        )
                                            .titleTextColorInt(titleColor)
                                            .descriptionTextColorInt(descriptionColor)
                                            .targetCircleColorInt(circleColorInner)
                                            .targetRadius(targetRadius.toInt())
                                            .tintTarget(false)
                                            .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                                        if (circleColorOuter != 0) {
                                            tapTarget.outerCircleColorInt(circleColorOuter)
                                        }

                                        titleTypeface?.let {
                                            tapTarget.textTypeface(it)
                                        }

                                        tapTargetSequence.target(tapTarget)
                                    }
                                }
                            }
                        }
                    }

                    tapTargetSequence.listener(object : TapTargetSequence.Listener {
                        override fun onSequenceFinish() {
                            Preferences.get(context).isTimeToShowHomeIntro = false
                        }

                        override fun onSequenceStep(tapTarget: TapTarget, b: Boolean) {
                            manager?.let {
                                if (position >= 0) {
                                    it.scrollToPosition(position)
                                }
                            }
                        }

                        override fun onSequenceCanceled(tapTarget: TapTarget) {}
                    })
                    tapTargetSequence.start()
                } catch (e: Exception) {
                    LogUtil.e(Log.getStackTraceString(e))
                }
            }, 100)
        }
    }

    @JvmStatic
    fun showIconsIntro(@NonNull context: Context) {
        if (Preferences.get(context).isTimeToShowIconsIntro) {
            val activity = context as AppCompatActivity
            val toolbar = activity.findViewById<Toolbar>(R.id.toolbar) ?: return

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    var titleColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroTitle)
                    var descriptionColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroDescription)
                    var circleColorInner = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleInner)
                    var circleColorOuter = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleOuter)

                    if (context.resources.getBoolean(R.bool.use_legacy_intro_colors)) {
                        val primary = ColorHelper.getAttributeColor(context, R.attr.cb_toolbarIcon)
                        val secondary = ColorHelper.setColorAlpha(primary, 0.7f)
                        titleColor = primary
                        descriptionColor = ColorHelper.setColorAlpha(primary, 0.7f)
                        circleColorInner = secondary
                        circleColorOuter = 0
                    }

                    val titleTypeface = TypefaceHelper.getMedium(context)

                    val tapTarget = TapTarget.forToolbarMenuItem(
                        toolbar, R.id.menu_search,
                        context.resources.getString(R.string.tap_intro_icons_search),
                        context.resources.getString(R.string.tap_intro_icons_search_desc)
                    )
                        .titleTextColorInt(titleColor)
                        .descriptionTextColorInt(descriptionColor)
                        .targetCircleColorInt(circleColorInner)
                        .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                    if (circleColorOuter != 0) {
                        tapTarget.outerCircleColorInt(circleColorOuter)
                    }

                    titleTypeface?.let {
                        tapTarget.textTypeface(it)
                    }

                    TapTargetView.showFor(
                        activity, tapTarget,
                        object : TapTargetView.Listener() {
                            override fun onTargetDismissed(view: TapTargetView?, userInitiated: Boolean) {
                                super.onTargetDismissed(view, userInitiated)
                                Preferences.get(context).isTimeToShowIconsIntro = false
                            }
                        })
                } catch (e: Exception) {
                    LogUtil.e(Log.getStackTraceString(e))
                }
            }, 100)
        }
    }

    @JvmStatic
    fun showRequestIntro(@NonNull context: Context, @Nullable recyclerView: RecyclerView?) {
        if (Preferences.get(context).isTimeToShowRequestIntro) {
            val activity = context as AppCompatActivity

            val requestOrientation = if (context.resources.configuration.orientation ==
                Configuration.ORIENTATION_PORTRAIT
            ) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            activity.requestedOrientation = requestOrientation

            val toolbar = activity.findViewById<Toolbar>(R.id.toolbar)

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    var titleColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroTitle)
                    var descriptionColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroDescription)
                    var circleColorInner = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleInner)
                    var circleColorOuter = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleOuter)

                    if (context.resources.getBoolean(R.bool.use_legacy_intro_colors)) {
                        val primary = ColorHelper.getAttributeColor(context, R.attr.cb_toolbarIcon)
                        val secondary = ColorHelper.setColorAlpha(primary, 0.7f)
                        titleColor = primary
                        descriptionColor = ColorHelper.setColorAlpha(primary, 0.7f)
                        circleColorInner = secondary
                        circleColorOuter = 0
                    }

                    val tapTargetSequence = TapTargetSequence(activity)
                    tapTargetSequence.continueOnCancel(true)

                    val titleTypeface = TypefaceHelper.getMedium(context)

                    if (recyclerView != null) {
                        var position = 0
                        if (Preferences.get(context).isPremiumRequestEnabled)
                            position += 1

                        recyclerView.adapter?.let {
                            if (position < it.itemCount) {
                                val holder = recyclerView.findViewHolderForAdapterPosition(position)
                                if (holder != null) {
                                    val view = holder.itemView.findViewById<View>(R.id.checkbox)
                                    if (view != null) {
                                        val tapTarget = TapTarget.forView(
                                            view,
                                            context.resources.getString(R.string.tap_intro_request_select),
                                            context.resources.getString(R.string.tap_intro_request_select_desc)
                                        )
                                            .titleTextColorInt(titleColor)
                                            .descriptionTextColorInt(descriptionColor)
                                            .targetCircleColorInt(circleColorInner)
                                            .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                                        if (circleColorOuter != 0) {
                                            tapTarget.outerCircleColorInt(circleColorOuter)
                                        }

                                        titleTypeface?.let {
                                            tapTarget.textTypeface(it)
                                        }

                                        tapTargetSequence.target(tapTarget)
                                    }
                                }
                            }
                        }
                    }

                    if (toolbar != null) {
                        val tapTarget = TapTarget.forToolbarMenuItem(
                            toolbar, R.id.menu_select_all,
                            context.resources.getString(R.string.tap_intro_request_select_all),
                            context.resources.getString(R.string.tap_intro_request_select_all_desc)
                        )
                            .titleTextColorInt(titleColor)
                            .descriptionTextColorInt(descriptionColor)
                            .targetCircleColorInt(circleColorInner)
                            .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                        if (circleColorOuter != 0) {
                            tapTarget.outerCircleColorInt(circleColorOuter)
                        }

                        titleTypeface?.let {
                            tapTarget.textTypeface(it)
                        }

                        tapTargetSequence.target(tapTarget)
                    }

                    val fab = activity.findViewById<View>(R.id.fab)
                    if (fab != null) {
                        val tapTarget = TapTarget.forView(
                            fab,
                            context.resources.getString(R.string.tap_intro_request_send),
                            context.resources.getString(R.string.tap_intro_request_send_desc)
                        )
                            .titleTextColorInt(titleColor)
                            .descriptionTextColorInt(descriptionColor)
                            .targetCircleColorInt(circleColorInner)
                            .tintTarget(false)
                            .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                        if (circleColorOuter != 0) {
                            tapTarget.outerCircleColorInt(circleColorOuter)
                        }

                        titleTypeface?.let {
                            tapTarget.textTypeface(it)
                        }

                        tapTargetSequence.target(tapTarget)
                    }

                    if (Preferences.get(context).isPremiumRequestEnabled) {
                        if (!Preferences.get(context).isPremiumRequest) {
                            if (recyclerView != null) {
                                val position = 0
                                recyclerView.adapter?.let {
                                    if (position < it.itemCount) {
                                        val holder = recyclerView.findViewHolderForAdapterPosition(position)
                                        if (holder != null) {
                                            val view = holder.itemView.findViewById<View>(R.id.buy)
                                            if (view != null) {
                                                val circleScale = 100.0f / context.resources.getInteger(R.integer.tap_intro_circle_scale_percent)
                                                val targetRadius = (toDp(context, view.measuredWidth.toFloat()) - 10f) * circleScale

                                                val tapTarget = TapTarget.forView(
                                                    view,
                                                    context.resources.getString(R.string.tap_intro_request_premium),
                                                    context.resources.getString(R.string.tap_intro_request_premium_desc)
                                                )
                                                    .titleTextColorInt(titleColor)
                                                    .descriptionTextColorInt(descriptionColor)
                                                    .targetCircleColorInt(circleColorInner)
                                                    .targetRadius(targetRadius.toInt())
                                                    .tintTarget(false)
                                                    .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                                                if (circleColorOuter != 0) {
                                                    tapTarget.outerCircleColorInt(circleColorOuter)
                                                }

                                                titleTypeface?.let {
                                                    tapTarget.textTypeface(it)
                                                }

                                                tapTargetSequence.target(tapTarget)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    tapTargetSequence.listener(object : TapTargetSequence.Listener {
                        override fun onSequenceFinish() {
                            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            Preferences.get(context).isTimeToShowRequestIntro = false
                        }

                        override fun onSequenceStep(tapTarget: TapTarget, b: Boolean) {}

                        override fun onSequenceCanceled(tapTarget: TapTarget) {}
                    })
                    tapTargetSequence.start()
                } catch (e: Exception) {
                    LogUtil.e(Log.getStackTraceString(e))
                }
            }, 100)
        }
    }

    @JvmStatic
    @SuppressLint("StringFormatInvalid")
    fun showWallpapersIntro(@NonNull context: Context, @Nullable recyclerView: RecyclerView?) {
        if (Preferences.get(context).isTimeToShowWallpapersIntro) {
            val activity = context as AppCompatActivity

            if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }

            Handler(Looper.getMainLooper()).postDelayed({
                var titleColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroTitle)
                var descriptionColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroDescription)
                var circleColorInner = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleInner)
                var circleColorOuter = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleOuter)

                if (context.resources.getBoolean(R.bool.use_legacy_intro_colors)) {
                    val primary = ColorHelper.getAttributeColor(context, R.attr.cb_toolbarIcon)
                    val secondary = ColorHelper.setColorAlpha(primary, 0.7f)
                    titleColor = primary
                    descriptionColor = ColorHelper.setColorAlpha(primary, 0.7f)
                    circleColorInner = secondary
                    circleColorOuter = 0
                }

                if (recyclerView != null) {
                    val tapTargetSequence = TapTargetSequence(activity)
                    tapTargetSequence.continueOnCancel(true)

                    val position = 0
                    if (recyclerView.adapter == null) return@postDelayed

                    if (position < recyclerView.adapter!!.itemCount) {
                        val holder = recyclerView.findViewHolderForAdapterPosition(position) ?: return@postDelayed

                        val view = holder.itemView.findViewById<View>(R.id.image)
                        if (view != null) {
                            val circleScale = 100.0f / context.resources.getInteger(R.integer.tap_intro_circle_scale_percent)
                            val targetRadius = (toDp(context, view.measuredWidth.toFloat()) - 10f) * circleScale

                            val titleTypeface = TypefaceHelper.getMedium(context)

                            val desc = context.resources.getString(
                                R.string.tap_intro_wallpapers_option_desc,
                                if (context.resources.getBoolean(R.bool.enable_wallpaper_download)) {
                                    context.resources.getString(R.string.tap_intro_wallpapers_option_desc_download)
                                } else ""
                            )

                            val tapTarget = TapTarget.forView(
                                view,
                                context.resources.getString(R.string.tap_intro_wallpapers_option),
                                desc
                            )
                                .titleTextColorInt(titleColor)
                                .descriptionTextColorInt(descriptionColor)
                                .targetCircleColorInt(circleColorInner)
                                .targetRadius(targetRadius.toInt())
                                .tintTarget(false)
                                .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                            val tapTarget1 = TapTarget.forView(
                                view,
                                context.resources.getString(R.string.tap_intro_wallpapers_preview),
                                context.resources.getString(R.string.tap_intro_wallpapers_preview_desc)
                            )
                                .titleTextColorInt(titleColor)
                                .descriptionTextColorInt(descriptionColor)
                                .targetCircleColorInt(circleColorInner)
                                .targetRadius(targetRadius.toInt())
                                .tintTarget(false)
                                .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                            if (circleColorOuter != 0) {
                                tapTarget.outerCircleColorInt(circleColorOuter)
                                tapTarget1.outerCircleColorInt(circleColorOuter)
                            }

                            titleTypeface?.let {
                                tapTarget.textTypeface(it)
                                tapTarget1.textTypeface(it)
                            }

                            tapTargetSequence.target(tapTarget)
                            tapTargetSequence.target(tapTarget1)

                            tapTargetSequence.listener(object : TapTargetSequence.Listener {
                                override fun onSequenceFinish() {
                                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    Preferences.get(context).isTimeToShowWallpapersIntro = false
                                }

                                override fun onSequenceStep(tapTarget: TapTarget, b: Boolean) {}

                                override fun onSequenceCanceled(tapTarget: TapTarget) {}
                            })
                            tapTargetSequence.start()
                        }
                    }
                }
            }, 200)
        }
    }

    @JvmStatic
    fun showWallpaperPreviewIntro(@NonNull context: Context, @ColorInt color: Int) {
        if (Preferences.get(context).isTimeToShowWallpaperPreviewIntro) {
            val activity = context as AppCompatActivity

            val rootView = activity.findViewById<View>(R.id.rootview) ?: return

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    var circleColorOuter = color
                    if (circleColorOuter == 0) {
                        circleColorOuter = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleOuter)
                    }

                    var titleColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroTitle)
                    var descriptionColor = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroDescription)
                    var circleColorInner = ColorHelper.getAttributeColor(context, R.attr.cb_tapIntroCircleInner)

                    if (context.resources.getBoolean(R.bool.use_legacy_intro_colors)) {
                        val primary = ColorHelper.getAttributeColor(context, R.attr.cb_toolbarIcon)
                        val secondary = ColorHelper.setColorAlpha(primary, 0.7f)
                        titleColor = primary
                        descriptionColor = ColorHelper.setColorAlpha(primary, 0.7f)
                        circleColorInner = secondary
                        circleColorOuter = ColorHelper.setColorAlpha(
                            ColorHelper.getAttributeColor(
                                context,
                                com.google.android.material.R.attr.colorSecondary
                            ), 0.7f
                        )
                    }

                    val tapTargetSequence = TapTargetSequence(activity)
                    tapTargetSequence.continueOnCancel(true)

                    val titleTypeface = TypefaceHelper.getMedium(context)

                    val apply = rootView.findViewById<View>(R.id.menu_apply)
                    val save = rootView.findViewById<View>(R.id.menu_save)

                    val tapTarget = TapTarget.forView(
                        apply,
                        context.resources.getString(R.string.tap_intro_wallpaper_preview_apply),
                        context.resources.getString(R.string.tap_intro_wallpaper_preview_apply_desc)
                    )
                        .titleTextColorInt(titleColor)
                        .descriptionTextColorInt(descriptionColor)
                        .targetCircleColorInt(circleColorInner)
                        .outerCircleColorInt(circleColorOuter)
                        .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                    val tapTarget1 = TapTarget.forView(
                        save,
                        context.resources.getString(R.string.tap_intro_wallpaper_preview_save),
                        context.resources.getString(R.string.tap_intro_wallpaper_preview_save_desc)
                    )
                        .titleTextColorInt(titleColor)
                        .descriptionTextColorInt(descriptionColor)
                        .targetCircleColorInt(circleColorInner)
                        .outerCircleColorInt(circleColorOuter)
                        .drawShadow(Preferences.get(context).isTapIntroShadowEnabled)

                    titleTypeface?.let {
                        tapTarget.textTypeface(it)
                        tapTarget1.textTypeface(it)
                    }

                    tapTargetSequence.target(tapTarget)
                    if (context.resources.getBoolean(R.bool.enable_wallpaper_download)) {
                        tapTargetSequence.target(tapTarget1)
                    }

                    tapTargetSequence.listener(object : TapTargetSequence.Listener {
                        override fun onSequenceFinish() {
                            Preferences.get(context).isTimeToShowWallpaperPreviewIntro = false
                        }

                        override fun onSequenceStep(tapTarget: TapTarget, b: Boolean) {}

                        override fun onSequenceCanceled(tapTarget: TapTarget) {}
                    })
                    tapTargetSequence.start()
                } catch (e: Exception) {
                    LogUtil.e(Log.getStackTraceString(e))
                }
            }, 100)
        }
    }
}
