package candybar.lib.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.method.LinkMovementMethod
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.text.HtmlCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.adapters.dialog.ChangelogAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.fragments.dialog.DonationLinksFragment
import candybar.lib.fragments.dialog.IconPreviewFragment
import candybar.lib.fragments.dialog.OtherAppsFragment
import candybar.lib.helpers.DrawableHelper.getDrawableId
import candybar.lib.helpers.LauncherHelper
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.helpers.ViewHelper
import candybar.lib.helpers.WallpaperHelper
import candybar.lib.items.Home
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import candybar.lib.utils.CandyBarGlideModule
import candybar.lib.utils.views.HeaderView
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.google.android.material.card.MaterialCardView
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

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
class HomeAdapter(
    private val context: Context,
    private val homes: MutableList<Home>,
    private var orientation: Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val imageStyle: Home.Style
    private var itemsCount: Int = 1
    private var showWallpapers: Boolean = false
    private var showIconRequest: Boolean = false
    private var showMoreApps: Boolean = false

    init {
        val viewStyle = context.resources.getString(R.string.home_image_style)
        imageStyle = ViewHelper.getHomeImageViewStyle(viewStyle)

        if (WallpaperHelper.getWallpaperType(context) == WallpaperHelper.CLOUD_WALLPAPERS) {
            itemsCount += 1
            showWallpapers = true
        }

        if (context.resources.getBoolean(R.bool.enable_icon_request) ||
            context.resources.getBoolean(R.bool.enable_premium_request)
        ) {
            itemsCount += 1
            showIconRequest = true
        }

        val link = context.resources.getString(R.string.google_play_dev)
        if (link.isNotEmpty()) {
            itemsCount += 1
            showMoreApps = true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val layout = if (imageStyle.type == Home.Style.Type.LANDSCAPE ||
                    imageStyle.type == Home.Style.Type.SQUARE
                ) {
                    R.layout.fragment_home_item_header_alt
                } else {
                    R.layout.fragment_home_item_header
                }
                HeaderViewHolder(LayoutInflater.from(context).inflate(layout, parent, false))
            }

            TYPE_CONTENT -> ContentViewHolder(
                LayoutInflater.from(context).inflate(R.layout.fragment_home_item_content, parent, false)
            )

            TYPE_ICON_REQUEST -> IconRequestViewHolder(
                LayoutInflater.from(context).inflate(R.layout.fragment_home_item_icon_request, parent, false)
            )

            TYPE_WALLPAPERS -> WallpapersViewHolder(
                LayoutInflater.from(context).inflate(R.layout.fragment_home_item_wallpapers, parent, false)
            )

            else -> GooglePlayDevViewHolder(
                LayoutInflater.from(context).inflate(R.layout.fragment_home_item_more_apps, parent, false)
            )
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is ContentViewHolder) {
            holder.title.isSingleLine = false
            holder.title.maxLines = 10
            TextViewCompat.setAutoSizeTextTypeWithDefaults(holder.title, TextViewCompat.AUTO_SIZE_TEXT_TYPE_NONE)
            holder.title.gravity = Gravity.CENTER_VERTICAL
            holder.title.includeFontPadding = true
            holder.title.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
            holder.subtitle.visibility = View.GONE
            holder.subtitle.gravity = Gravity.CENTER_VERTICAL
        }
    }

    @SuppressLint("StringFormatInvalid")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        try {
            (holder.itemView.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.let {
                it.isFullSpan = isFullSpan(holder.itemViewType)
            }
        } catch (e: Exception) {
            LogUtil.d(Log.getStackTraceString(e))
        }

        when (holder) {
            is HeaderViewHolder -> {
                val homeTitleText = context.resources.getString(R.string.home_title)
                if (homeTitleText.isNotEmpty()) {
                    holder.title.text = homeTitleText
                } else {
                    holder.title.visibility = View.GONE
                }

                holder.content.text = HtmlCompat.fromHtml(
                    context.resources.getString(R.string.home_description),
                    HtmlCompat.FROM_HTML_MODE_COMPACT
                )
                holder.content.movementMethod = LinkMovementMethod.getInstance()

                var uri = context.resources.getString(R.string.home_image)
                if (ColorHelper.isValidColor(uri)) {
                    holder.image.setBackgroundColor(Color.parseColor(uri))
                } else {
                    if (!URLUtil.isValidUrl(uri)) {
                        uri = "drawable://" + getDrawableId(uri)
                    }

                    if (CandyBarGlideModule.isValidContextForGlide(context)) {
                        Glide.with(context)
                            .load(uri)
                            .transition(DrawableTransitionOptions.withCrossFade(300))
                            .skipMemoryCache(true)
                            .diskCacheStrategy(
                                if (uri.contains("drawable://")) DiskCacheStrategy.NONE
                                else DiskCacheStrategy.RESOURCE
                            )
                            .into(holder.image)
                    }
                }
            }

            is ContentViewHolder -> {
                val finalPosition = position - 1
                val home = homes[finalPosition]
                val color = ColorHelper.getAttributeColor(context, android.R.attr.textColorPrimary)

                if (home.icon != -1) {
                    if (home.type == Home.Type.DIMENSION) {
                        if (CandyBarGlideModule.isValidContextForGlide(context)) {
                            Glide.with(context)
                                .asBitmap()
                                .load("drawable://" + home.icon)
                                .skipMemoryCache(true)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .listener(object : RequestListener<Bitmap> {
                                    override fun onLoadFailed(
                                        e: GlideException?,
                                        model: Any?,
                                        target: Target<Bitmap>,
                                        isFirstResource: Boolean
                                    ): Boolean = true

                                    override fun onResourceReady(
                                        bitmap: Bitmap,
                                        model: Any?,
                                        target: Target<Bitmap>,
                                        dataSource: DataSource,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        Handler(Looper.getMainLooper()).post {
                                            val drawable = RoundedBitmapDrawableFactory.create(
                                                context.resources, bitmap
                                            )
                                            drawable.cornerRadius = 0f
                                            holder.title.setCompoundDrawablesWithIntrinsicBounds(
                                                DrawableHelper.getResizedDrawable(context, drawable, 40f),
                                                null, null, null
                                            )
                                        }
                                        return true
                                    }
                                })
                                .submit()
                        }
                    } else {
                        holder.title.setCompoundDrawablesWithIntrinsicBounds(
                            DrawableHelper.getTintedDrawable(context, home.icon, color),
                            null, null, null
                        )
                    }
                }

                if (home.type == Home.Type.ICONS) {
                    if (home.isLoading && CandyBarMainActivity.sIconsCount == 0 &&
                        CandyBarApplication.getConfiguration().isAutomaticIconsCountEnabled
                    ) {
                        holder.progressBar.visibility = View.VISIBLE
                        holder.title.visibility = View.GONE
                    } else {
                        holder.progressBar.visibility = View.GONE
                        holder.title.visibility = View.VISIBLE
                    }

                    holder.title.setLines(1)
                    holder.title.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        context.resources.getDimension(R.dimen.text_max_size)
                    )
                    holder.title.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                    holder.title.includeFontPadding = false
                    TextViewCompat.setAutoSizeTextTypeWithDefaults(
                        holder.title,
                        TextViewCompat.AUTO_SIZE_TEXT_TYPE_UNIFORM
                    )
                    holder.subtitle.gravity = Gravity.END or Gravity.CENTER_VERTICAL
                } else {
                    holder.title.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        context.resources.getDimension(R.dimen.text_content_title)
                    )
                }

                holder.title.typeface = TypefaceHelper.getMedium(context)
                holder.title.text = home.title

                if (home.subtitle.orEmpty().isNotEmpty()) {
                    holder.subtitle.text = home.subtitle
                    holder.subtitle.visibility = View.VISIBLE
                } else {
                    holder.subtitle.visibility = View.GONE
                }
            }

            is IconRequestViewHolder -> {
                if (context.resources.getBoolean(R.bool.hide_missing_app_count)) {
                    holder.dataContainer.visibility = View.GONE
                    holder.progressBar.visibility = View.GONE
                } else if (CandyBarMainActivity.sMissedApps == null) {
                    holder.dataContainer.visibility = View.GONE
                    holder.progressBar.visibility = View.VISIBLE
                } else {
                    holder.dataContainer.visibility = View.VISIBLE
                    holder.progressBar.visibility = View.GONE
                }

                val installed = CandyBarMainActivity.sInstalledAppsCount
                val missed = CandyBarMainActivity.sMissedApps?.size ?: installed
                val themed = installed - missed

                holder.installedApps.text = context.resources.getString(
                    R.string.home_icon_request_installed_apps, installed
                )
                holder.missedApps.text = context.resources.getString(
                    R.string.home_icon_request_missed_apps, missed
                )
                holder.themedApps.text = context.resources.getString(
                    R.string.home_icon_request_themed_apps, themed
                )

                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "stats",
                    hashMapOf(
                        "section" to "home",
                        "installed" to installed,
                        "missed" to missed,
                        "themed" to themed
                    )
                )

                holder.progress.max = installed
                holder.progress.progress = themed
            }

            is WallpapersViewHolder -> {
                holder.title.text = context.resources.getString(
                    R.string.home_loud_wallpapers,
                    Preferences.get(context).availableWallpapersCount
                )
            }
        }
    }

    override fun getItemCount(): Int = homes.size + itemsCount

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return TYPE_HEADER
        if (position == homes.size + 1 && showIconRequest) return TYPE_ICON_REQUEST

        if (position == itemCount - 2 && showWallpapers && showMoreApps) return TYPE_WALLPAPERS

        if (position == itemCount - 1) {
            return if (showMoreApps) TYPE_GOOGLE_PLAY_DEV
            else if (showWallpapers) TYPE_WALLPAPERS
            else TYPE_CONTENT
        }
        return TYPE_CONTENT
    }

    private inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val image: HeaderView = itemView.findViewById(R.id.header_image)
        val title: TextView = itemView.findViewById(R.id.title)
        val content: TextView = itemView.findViewById(R.id.content)

        init {
            val rate: Button = itemView.findViewById(R.id.rate)
            val share: ImageView = itemView.findViewById(R.id.share)
            val update: ImageView = itemView.findViewById(R.id.update)
            val card: MaterialCardView = itemView.findViewById(R.id.card)

            if (CandyBarApplication.getConfiguration().homeGrid == CandyBarApplication.GridStyle.FLAT) {
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.let { params ->
                    card.radius = 0f
                    card.useCompatPadding = false
                    val margin = context.resources.getDimensionPixelSize(R.dimen.card_margin)
                    params.setMargins(0, 0, margin, margin)
                    params.marginEnd = margin
                } ?: (card.layoutParams as? LinearLayout.LayoutParams)?.let { params ->
                    card.radius = 0f
                    card.useCompatPadding = false
                    val margin = context.resources.getDimensionPixelSize(R.dimen.card_margin)
                    if (imageStyle.type == Home.Style.Type.LANDSCAPE ||
                        imageStyle.type == Home.Style.Type.SQUARE
                    ) {
                        params.setMargins(
                            margin,
                            context.resources.getDimensionPixelSize(R.dimen.content_padding_reverse),
                            margin, margin
                        )
                    }
                    params.marginEnd = margin
                }
            }

            if (context.resources.getBoolean(R.bool.use_flat_card)) {
                card.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                card.cardElevation = 0f
                card.useCompatPadding = false
                val marginTop = context.resources.getDimensionPixelSize(R.dimen.card_margin_top)
                val marginLeft = context.resources.getDimensionPixelSize(R.dimen.card_margin_left)
                val marginRight = context.resources.getDimensionPixelSize(R.dimen.card_margin_right)
                val marginBottom = context.resources.getDimensionPixelSize(R.dimen.card_margin_bottom)
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.setMargins(
                    marginLeft, marginTop, marginRight, marginBottom
                )
            }

            if (!Preferences.get(context).isCardShadowEnabled) {
                card.cardElevation = 0f
            }

            if (context.resources.getString(R.string.rate_and_review_link).isEmpty()) {
                rate.visibility = View.GONE
            }

            if (context.resources.getString(R.string.share_link).isEmpty()) {
                share.visibility = View.GONE
            }

            if (!context.resources.getBoolean(R.bool.enable_check_update) ||
                CandyBarApplication.getConfiguration().configHandler?.configJson(context)?.isEmpty() == true
            ) {
                update.visibility = View.GONE
            }

            val color = ColorHelper.getAttributeColor(context, android.R.attr.textColorSecondary)
            rate.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_rate, color),
                null, null, null
            )
            share.setImageDrawable(DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_share, color))
            update.setImageDrawable(DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_update, color))

            image.setRatio(imageStyle.point.x, imageStyle.point.y)

            rate.setOnClickListener(this)
            share.setOnClickListener(this)
            update.setOnClickListener(this)
        }

        @SuppressLint("StringFormatInvalid")
        override fun onClick(view: View) {
            when (view.id) {
                R.id.rate -> {
                    CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                        "click",
                        hashMapOf(
                            "section" to "home",
                            "action" to "open_dialog",
                            "item" to "rate_and_review"
                        )
                    )
                    val intent = Intent(
                        Intent.ACTION_VIEW, Uri.parse(
                            context.resources.getString(R.string.rate_and_review_link)
                                .replace("{{packageName}}", context.packageName)
                        )
                    )
                    intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                    context.startActivity(intent)
                }

                R.id.share -> {
                    CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                        "click",
                        hashMapOf(
                            "section" to "home",
                            "action" to "open_dialog",
                            "item" to "share"
                        )
                    )
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            context.resources.getString(
                                R.string.share_app_title,
                                context.resources.getString(R.string.app_name)
                            )
                        )
                        putExtra(
                            Intent.EXTRA_TEXT,
                            context.resources.getString(
                                R.string.share_app_body,
                                context.resources.getString(R.string.app_name),
                                "\n" + context.resources.getString(R.string.share_link)
                                    .replace("{{packageName}}", context.packageName)
                            )
                        )
                    }
                    context.startActivity(
                        Intent.createChooser(
                            intent,
                            context.resources.getString(R.string.app_client)
                        )
                    )
                }

                R.id.update -> {
                    CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                        "click",
                        hashMapOf(
                            "section" to "home",
                            "action" to "open_dialog",
                            "item" to "update"
                        )
                    )
                    UpdateChecker().execute()
                }
            }
        }
    }

    private inner class UpdateChecker : AsyncTaskBase() {
        private var loadingDialog: MaterialDialog? = null
        private var latestVersion: String? = null
        private var updateUrl: String? = null
        private var changelog: Array<String>? = null
        private var isUpdateAvailable: Boolean = false

        override fun preRun() {
            loadingDialog = MaterialDialog.Builder(context)
                .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
                .content(R.string.checking_for_update)
                .cancelable(false)
                .canceledOnTouchOutside(false)
                .progress(true, 0)
                .progressIndeterminateStyle(true)
                .show()
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                var isSuccess = true
                val configJsonUrl = CandyBarApplication.getConfiguration().configHandler?.configJson(context) ?: return false

                try {
                    val urlConnection = URL(configJsonUrl).openConnection()
                    val reader = BufferedReader(InputStreamReader(urlConnection.getInputStream()))
                    val stringBuilder = StringBuilder()
                    reader.use { r ->
                        var line: String?
                        while (r.readLine().also { line = it } != null) {
                            stringBuilder.append(line)
                        }
                    }

                    val configJson = JSONObject(stringBuilder.toString())
                    latestVersion = configJson.getString("latestVersion")

                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    updateUrl = if (configJson.isNull("url")) {
                        "https://play.google.com/store/apps/details?id=" + packageInfo.packageName
                    } else {
                        configJson.getString("url")
                    }

                    val latestVersionCode = configJson.getLong("latestVersionCode")
                    val appVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }

                    if (latestVersionCode > appVersionCode) {
                        isUpdateAvailable = true
                        val changelogArray = configJson.getJSONArray("releaseNotes")
                        changelog = Array(changelogArray.length()) { i ->
                            changelogArray.getString(i)
                        }
                    }
                } catch (e: Exception) {
                    LogUtil.e("Error loading Configuration JSON " + Log.getStackTraceString(e))
                    isSuccess = false
                }
                return isSuccess
            }
            return false
        }

        @SuppressLint("SetTextI18n")
        override fun postRun(ok: Boolean) {
            loadingDialog?.dismiss()
            loadingDialog = null

            if (ok) {
                val builder = MaterialDialog.Builder(context)
                    .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
                    .customView(R.layout.fragment_update, false)

                if (isUpdateAvailable) {
                    builder.positiveText(R.string.update)
                        .negativeText(R.string.close)
                        .onPositive { _, _ ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                            context.startActivity(intent)
                        }
                } else {
                    builder.positiveText(R.string.close)
                }

                val dialog = builder.build()
                val changelogVersion = dialog.findViewById(R.id.changelog_version) as TextView
                val changelogList = dialog.findViewById(R.id.changelog_list) as ListView

                if (isUpdateAvailable) {
                    changelogVersion.text = "${context.resources.getString(R.string.update_available)}\n" +
                            "${context.resources.getString(R.string.changelog_version)} $latestVersion"
                    changelogList.adapter = ChangelogAdapter(context, changelog ?: emptyArray())
                } else {
                    changelogVersion.text = context.resources.getString(R.string.no_update_available)
                    changelogList.visibility = View.GONE
                }
                dialog.show()
            } else {
                MaterialDialog.Builder(context)
                    .typeface(TypefaceHelper.getMedium(context), TypefaceHelper.getRegular(context))
                    .content(R.string.unable_to_load_config)
                    .positiveText(R.string.close)
                    .show()
            }
        }
    }

    private inner class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener, View.OnLongClickListener {
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        val title: TextView = itemView.findViewById(R.id.title)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val quickApply: Boolean = context.resources.getBoolean(R.bool.quick_apply)

        init {
            val container: LinearLayout = itemView.findViewById(R.id.container)
            val card: MaterialCardView = itemView.findViewById(R.id.card)

            if (CandyBarApplication.getConfiguration().homeGrid == CandyBarApplication.GridStyle.FLAT) {
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.let { params ->
                    card.radius = 0f
                    card.useCompatPadding = false
                    val margin = context.resources.getDimensionPixelSize(R.dimen.card_margin)
                    params.setMargins(0, 0, margin, margin)
                    params.marginEnd = margin
                }
            }

            if (context.resources.getBoolean(R.bool.use_flat_card)) {
                card.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                card.cardElevation = 0f
                card.useCompatPadding = false
                val marginTop = context.resources.getDimensionPixelSize(R.dimen.card_margin_top)
                val marginLeft = context.resources.getDimensionPixelSize(R.dimen.card_margin_left)
                val marginRight = context.resources.getDimensionPixelSize(R.dimen.card_margin_right)
                val marginBottom = context.resources.getDimensionPixelSize(R.dimen.card_margin_bottom)
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.setMargins(
                    marginLeft, marginTop, marginRight, marginBottom
                )
            }

            if (!Preferences.get(context).isCardShadowEnabled) {
                card.cardElevation = 0f
            }

            container.setOnClickListener(this)
            if (quickApply) container.setOnLongClickListener(this)
        }

        override fun onClick(view: View) {
            if (view.id == R.id.container) {
                val position = bindingAdapterPosition - 1
                if (position < 0 || position >= homes.size) return

                when (homes[position].type) {
                    Home.Type.APPLY -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "home",
                                "action" to "navigate",
                                "item" to "icon_apply"
                            )
                        )
                        if (!quickApply || !LauncherHelper.quickApply(context)) {
                            (context as? CandyBarMainActivity)?.selectPosition(1)
                        }
                    }

                    Home.Type.DONATE -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "home",
                                "action" to "open_dialog",
                                "item" to "donate"
                            )
                        )
                        if (context is CandyBarMainActivity) {
                            if (CandyBarApplication.getConfiguration().donationLinks != null) {
                                DonationLinksFragment.showDonationLinksDialog(
                                    (context as AppCompatActivity).supportFragmentManager
                                )
                            }
                        }
                    }

                    Home.Type.ICONS -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "home",
                                "action" to "navigate",
                                "item" to "icons"
                            )
                        )
                        (context as? CandyBarMainActivity)?.selectPosition(2)
                    }

                    Home.Type.DIMENSION -> {
                        val home = homes[position]
                        IconPreviewFragment.showIconPreview(
                            (context as AppCompatActivity).supportFragmentManager,
                            home.title ?: "", home.icon, null
                        )
                    }
                }
            }
        }

        override fun onLongClick(view: View): Boolean {
            if (view.id == R.id.container) {
                val position = bindingAdapterPosition - 1
                if (position < 0 || position >= homes.size) return false

                if (homes[position].type == Home.Type.APPLY) {
                    (context as? CandyBarMainActivity)?.selectPosition(1)
                    return true
                }
            }
            return false
        }
    }

    private inner class IconRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val installedApps: TextView = itemView.findViewById(R.id.installed_apps)
        val themedApps: TextView = itemView.findViewById(R.id.themed_apps)
        val missedApps: TextView = itemView.findViewById(R.id.missed_apps)
        val progress: ProgressBar = itemView.findViewById(R.id.progress)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        val dataContainer: LinearLayout = itemView.findViewById(R.id.dataContainer)

        init {
            val container: LinearLayout = itemView.findViewById(R.id.container)
            val card: MaterialCardView = itemView.findViewById(R.id.card)

            if (CandyBarApplication.getConfiguration().homeGrid == CandyBarApplication.GridStyle.FLAT) {
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.let { params ->
                    card.radius = 0f
                    card.useCompatPadding = false
                    val margin = context.resources.getDimensionPixelSize(R.dimen.card_margin)
                    params.setMargins(0, 0, margin, margin)
                    params.marginEnd = margin
                }
            }

            if (context.resources.getBoolean(R.bool.use_flat_card)) {
                card.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                card.cardElevation = 0f
                card.useCompatPadding = false
                val marginTop = context.resources.getDimensionPixelSize(R.dimen.card_margin_top)
                val marginLeft = context.resources.getDimensionPixelSize(R.dimen.card_margin_left)
                val marginRight = context.resources.getDimensionPixelSize(R.dimen.card_margin_right)
                val marginBottom = context.resources.getDimensionPixelSize(R.dimen.card_margin_bottom)
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.setMargins(
                    marginLeft, marginTop, marginRight, marginBottom
                )
            }

            if (!Preferences.get(context).isCardShadowEnabled) {
                card.cardElevation = 0f
            }

            val color = ColorHelper.getAttributeColor(context, android.R.attr.textColorPrimary)
            itemView.findViewById<TextView>(R.id.title).setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_icon_request, color),
                null, null, null
            )

            val accent = ColorHelper.getAttributeColor(context, com.google.android.material.R.attr.colorSecondary)
            progress.progressDrawable.colorFilter = PorterDuffColorFilter(accent, PorterDuff.Mode.SRC_IN)

            container.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view.id == R.id.container) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    hashMapOf(
                        "section" to "home",
                        "action" to "navigate",
                        "item" to "icon_request"
                    )
                )
                (context as? CandyBarMainActivity)?.selectPosition(3)
            }
        }
    }

    private inner class WallpapersViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val title: TextView = itemView.findViewById(R.id.title)

        init {
            val muzei: TextView = itemView.findViewById(R.id.muzei)
            val card: MaterialCardView = itemView.findViewById(R.id.card)

            if (CandyBarApplication.getConfiguration().homeGrid == CandyBarApplication.GridStyle.FLAT) {
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.let { params ->
                    card.radius = 0f
                    card.useCompatPadding = false
                    val margin = context.resources.getDimensionPixelSize(R.dimen.card_margin)
                    params.setMargins(0, 0, margin, margin)
                    params.marginEnd = margin
                }
            }

            if (context.resources.getBoolean(R.bool.use_flat_card)) {
                card.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                card.cardElevation = 0f
                card.useCompatPadding = false
                val marginTop = context.resources.getDimensionPixelSize(R.dimen.card_margin_top)
                val marginLeft = context.resources.getDimensionPixelSize(R.dimen.card_margin_left)
                val marginRight = context.resources.getDimensionPixelSize(R.dimen.card_margin_right)
                val marginBottom = context.resources.getDimensionPixelSize(R.dimen.card_margin_bottom)
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.setMargins(
                    marginLeft, marginTop, marginRight, marginBottom
                )
            }

            if (!Preferences.get(context).isCardShadowEnabled) {
                card.cardElevation = 0f
            }

            val color = ColorHelper.getAttributeColor(context, android.R.attr.textColorPrimary)
            title.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_wallpapers, color),
                null, null, null
            )

            muzei.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.get(context, R.drawable.ic_home_app_muzei),
                null, null, null
            )

            title.setOnClickListener(this)
            muzei.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view.id == R.id.title) {
                (context as? CandyBarMainActivity)?.selectPosition(4)
            } else if (view.id == R.id.muzei) {
                val intent = Intent(
                    Intent.ACTION_VIEW, Uri.parse(
                        "https://play.google.com/store/apps/details?id=net.nurik.roman.muzei"
                    )
                )
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                context.startActivity(intent)
            }
        }
    }

    private inner class GooglePlayDevViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        init {
            val container: LinearLayout = itemView.findViewById(R.id.container)
            val title: TextView = itemView.findViewById(R.id.title)
            val card: MaterialCardView = itemView.findViewById(R.id.card)

            if (CandyBarApplication.getConfiguration().homeGrid == CandyBarApplication.GridStyle.FLAT) {
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.let { params ->
                    card.radius = 0f
                    card.useCompatPadding = false
                    val margin = context.resources.getDimensionPixelSize(R.dimen.card_margin)
                    params.setMargins(0, 0, margin, margin)
                    params.marginEnd = margin
                }
            }

            if (context.resources.getBoolean(R.bool.use_flat_card)) {
                card.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.card_stroke_width)
                card.cardElevation = 0f
                card.useCompatPadding = false
                val marginTop = context.resources.getDimensionPixelSize(R.dimen.card_margin_top)
                val marginLeft = context.resources.getDimensionPixelSize(R.dimen.card_margin_left)
                val marginRight = context.resources.getDimensionPixelSize(R.dimen.card_margin_right)
                val marginBottom = context.resources.getDimensionPixelSize(R.dimen.card_margin_bottom)
                (card.layoutParams as? StaggeredGridLayoutManager.LayoutParams)?.setMargins(
                    marginLeft, marginTop, marginRight, marginBottom
                )
            }

            if (!Preferences.get(context).isCardShadowEnabled) {
                card.cardElevation = 0f
            }

            val color = ColorHelper.getAttributeColor(context, android.R.attr.textColorPrimary)
            title.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(context, R.drawable.ic_google_play_more_apps, color),
                null, null, null
            )

            container.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            if (view.id == R.id.container) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    hashMapOf(
                        "section" to "home",
                        "action" to "open_dialog",
                        "item" to "other_apps"
                    )
                )
                if (CandyBarApplication.getConfiguration().otherApps != null) {
                    OtherAppsFragment.showOtherAppsDialog(
                        (context as AppCompatActivity).supportFragmentManager
                    )
                    return
                }

                val link = context.resources.getString(R.string.google_play_dev)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                context.startActivity(intent)
            }
        }
    }

    private inner class ShadowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            if (!Preferences.get(context).isCardShadowEnabled) {
                itemView.findViewById<View>(R.id.shadow)?.visibility = View.GONE
            }
        }
    }

    val applyIndex: Int
        get() {
            var index = -1
            for (i in 0 until itemCount) {
                if (getItemViewType(i) == TYPE_CONTENT) {
                    val pos = i - 1
                    if (homes[pos].type == Home.Type.APPLY) {
                        index = i
                        break
                    }
                }
            }
            return index
        }

    fun getItem(position: Int): Home = homes[position - 1]

    val iconsIndex: Int
        get() {
            var index = -1
            for (i in 0 until itemCount) {
                if (getItemViewType(i) == TYPE_CONTENT) {
                    val pos = i - 1
                    if (homes[pos].type == Home.Type.ICONS) {
                        index = i
                        break
                    }
                }
            }
            return index
        }

    val dimensionsIndex: Int
        get() {
            var index = -1
            for (i in 0 until itemCount) {
                if (getItemViewType(i) == TYPE_CONTENT) {
                    val pos = i - 1
                    if (homes[pos].type == Home.Type.DIMENSION) {
                        index = i
                        break
                    }
                }
            }
            return index
        }

    val iconRequestIndex: Int
        get() {
            var index = -1
            for (i in 0 until itemCount) {
                if (getItemViewType(i) == TYPE_ICON_REQUEST) {
                    index = i
                    break
                }
            }
            return index
        }

    val wallpapersIndex: Int
        get() {
            var index = -1
            for (i in 0 until itemCount) {
                if (getItemViewType(i) == TYPE_WALLPAPERS) {
                    index = i
                    break
                }
            }
            return index
        }

    fun addNewContent(home: Home?) {
        home?.let {
            homes.add(it)
            notifyItemInserted(homes.size)
        }
    }

    fun setOrientation(orientation: Int) {
        this.orientation = orientation
        notifyDataSetChanged()
    }

    private fun isFullSpan(viewType: Int): Boolean {
        if (viewType == TYPE_HEADER) {
            return if (orientation == Configuration.ORIENTATION_PORTRAIT) {
                true
            } else {
                imageStyle.type == Home.Style.Type.SQUARE ||
                        imageStyle.type == Home.Style.Type.LANDSCAPE
            }
        }
        return false
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_CONTENT = 1
        private const val TYPE_ICON_REQUEST = 2
        private const val TYPE_WALLPAPERS = 3
        private const val TYPE_GOOGLE_PLAY_DEV = 4
    }
}
