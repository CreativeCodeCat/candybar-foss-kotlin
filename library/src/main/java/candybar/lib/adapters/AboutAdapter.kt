package candybar.lib.adapters

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.URLUtil
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import candybar.lib.BuildConfig
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.fragments.dialog.CreditsFragment
import candybar.lib.fragments.dialog.LicensesFragment
import candybar.lib.helpers.ConfigurationHelper
import candybar.lib.helpers.DrawableHelper.getDrawableId
import candybar.lib.preferences.Preferences
import candybar.lib.utils.CandyBarGlideModule
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.google.android.material.card.MaterialCardView

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
class AboutAdapter(private val context: Context, spanCount: Int) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var itemCount: Int = 2
    private val showExtraInfo: Boolean
    private val showContributors: Boolean
    private val showPrivacyPolicy: Boolean
    private val showTerms: Boolean

    init {
        val cardMode = spanCount > 1
        if (!cardMode) {
            itemCount += 1
        }

        showContributors = context.resources.getBoolean(R.bool.show_contributors_dialog)
        showPrivacyPolicy = context.resources.getString(R.string.privacy_policy_link).isNotEmpty()
        showTerms = context.resources.getString(R.string.terms_and_conditions_link).isNotEmpty()
        showExtraInfo = showContributors || showPrivacyPolicy || showTerms

        if (showExtraInfo) {
            itemCount += 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(
                LayoutInflater.from(context).inflate(R.layout.fragment_about_item_header, parent, false)
            )

            TYPE_EXTRA_INFO -> ExtraInfoViewHolder(
                LayoutInflater.from(context).inflate(R.layout.fragment_about_item_sub, parent, false)
            )

            TYPE_FOOTER -> {
                val layout = if (CandyBarApplication.getConfiguration().socialIconColor == CandyBarApplication.IconColor.ACCENT) {
                    R.layout.fragment_about_item_footer_accent
                } else {
                    R.layout.fragment_about_item_footer
                }
                FooterViewHolder(LayoutInflater.from(context).inflate(layout, parent, false))
            }

            else -> ShadowViewHolder(
                LayoutInflater.from(context).inflate(R.layout.fragment_settings_item_footer, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HeaderViewHolder) {
            val imageUri = context.getString(R.string.about_image)
            if (ColorHelper.isValidColor(imageUri)) {
                holder.image.setBackgroundColor(Color.parseColor(imageUri))
            } else {
                var finalImageUri = imageUri
                if (!URLUtil.isValidUrl(finalImageUri)) {
                    finalImageUri = "drawable://" + getDrawableId(finalImageUri)
                }

                if (CandyBarGlideModule.isValidContextForGlide(context)) {
                    Glide.with(context)
                        .load(finalImageUri)
                        .transition(DrawableTransitionOptions.withCrossFade(300))
                        .skipMemoryCache(true)
                        .diskCacheStrategy(
                            if (finalImageUri.contains("drawable://")) DiskCacheStrategy.NONE
                            else DiskCacheStrategy.RESOURCE
                        )
                        .into(holder.image)
                }
            }

            var profileUri = context.resources.getString(R.string.about_profile_image)
            if (!URLUtil.isValidUrl(profileUri)) {
                profileUri = "drawable://" + getDrawableId(profileUri)
            }

            if (CandyBarGlideModule.isValidContextForGlide(context)) {
                Glide.with(context)
                    .load(profileUri)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(
                        if (profileUri.contains("drawable://")) DiskCacheStrategy.NONE
                        else DiskCacheStrategy.RESOURCE
                    )
                    .into(holder.profile)
            }
        }
    }

    override fun getItemCount(): Int = itemCount

    override fun getItemViewType(position: Int): Int {
        if (position == 0) return TYPE_HEADER
        if (position == 1) {
            return if (showExtraInfo) TYPE_EXTRA_INFO else TYPE_FOOTER
        }

        if (position == 2 && showExtraInfo) return TYPE_FOOTER
        return TYPE_BOTTOM_SHADOW
    }

    private inner class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.image)
        val profile: ImageView = itemView.findViewById(R.id.profile)

        init {
            val subtitle: TextView = itemView.findViewById(R.id.subtitle)
            val recyclerView: RecyclerView = itemView.findViewById(R.id.recyclerview)
            recyclerView.itemAnimator = DefaultItemAnimator()
            recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, true)
            recyclerView.setHasFixedSize(true)

            val urls = context.resources.getStringArray(R.array.about_social_links)
            if (urls.isEmpty()) {
                recyclerView.visibility = View.GONE
                subtitle.setPadding(
                    subtitle.paddingLeft,
                    subtitle.paddingTop,
                    subtitle.paddingRight,
                    subtitle.paddingBottom + context.resources.getDimensionPixelSize(R.dimen.content_margin)
                )
            } else {
                val params = recyclerView.layoutParams as? LinearLayout.LayoutParams
                if (params != null && urls.size < 7) {
                    params.width = LinearLayout.LayoutParams.WRAP_CONTENT
                    params.gravity = Gravity.CENTER_HORIZONTAL
                    recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
                }
                recyclerView.adapter = AboutSocialAdapter(context, urls)
            }

            val card: MaterialCardView? = itemView.findViewById(R.id.card)
            if (card != null) {
                if (CandyBarApplication.getConfiguration().aboutStyle == CandyBarApplication.Style.PORTRAIT_FLAT_LANDSCAPE_FLAT) {
                    val params = card.layoutParams as? StaggeredGridLayoutManager.LayoutParams
                    if (params != null) {
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
                    val params = card.layoutParams as StaggeredGridLayoutManager.LayoutParams
                    params.setMargins(marginLeft, marginTop, marginRight, marginBottom)
                }

                if (!Preferences.get(context).isCardShadowEnabled) {
                    card.cardElevation = 0f
                    profile.elevation = 0f
                }
            }

            subtitle.text = HtmlCompat.fromHtml(
                context.resources.getString(R.string.about_desc),
                HtmlCompat.FROM_HTML_MODE_COMPACT
            )
            subtitle.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private inner class ExtraInfoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        init {
            val contributorsHolder: LinearLayout = itemView.findViewById(R.id.contributors)
            val contributorsTitle: TextView = itemView.findViewById(R.id.contributors_title)
            val privacyPolicyHolder: LinearLayout = itemView.findViewById(R.id.privacy_policy)
            val privacyPolicyTitle: TextView = itemView.findViewById(R.id.privacy_policy_title)
            val termsHolder: LinearLayout = itemView.findViewById(R.id.terms)
            val termsTitle: TextView = itemView.findViewById(R.id.terms_title)

            val card: MaterialCardView? = itemView.findViewById(R.id.card)
            if (card != null) {
                if (CandyBarApplication.getConfiguration().aboutStyle == CandyBarApplication.Style.PORTRAIT_FLAT_LANDSCAPE_FLAT) {
                    val params = card.layoutParams as? StaggeredGridLayoutManager.LayoutParams
                    if (params != null) {
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
                    val params = card.layoutParams as StaggeredGridLayoutManager.LayoutParams
                    params.setMargins(marginLeft, marginTop, marginRight, marginBottom)
                }

                if (!Preferences.get(context).isCardShadowEnabled) {
                    card.cardElevation = 0f
                }
            }

            if (!showContributors) contributorsHolder.visibility = View.GONE
            if (!showPrivacyPolicy) privacyPolicyHolder.visibility = View.GONE
            if (!showTerms) termsHolder.visibility = View.GONE

            val color = ColorHelper.getAttributeColor(context, android.R.attr.textColorPrimary)

            contributorsTitle.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_people, color),
                null, null, null
            )
            contributorsTitle.text = context.resources.getString(R.string.about_contributors_title)

            privacyPolicyTitle.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_link, color),
                null, null, null
            )
            privacyPolicyTitle.text = context.resources.getString(R.string.about_privacy_policy_title)

            termsTitle.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_link, color),
                null, null, null
            )
            termsTitle.text = context.resources.getString(R.string.about_terms_and_conditions_title)

            contributorsTitle.setOnClickListener(this)
            privacyPolicyTitle.setOnClickListener(this)
            termsTitle.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val id = view.id
            if (id == R.id.contributors_title) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    hashMapOf(
                        "section" to "about",
                        "action" to "open_dialog",
                        "item" to "contributors"
                    )
                )
                CreditsFragment.showCreditsDialog(
                    (context as AppCompatActivity).supportFragmentManager,
                    CreditsFragment.TYPE_ICON_PACK_CONTRIBUTORS
                )
            } else if (id == R.id.privacy_policy_title) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    hashMapOf(
                        "section" to "about",
                        "action" to "open_dialog",
                        "item" to "privacy_policy"
                    )
                )
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.resources.getString(R.string.privacy_policy_link)))
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                context.startActivity(intent)
            } else if (id == R.id.terms_title) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    hashMapOf(
                        "section" to "about",
                        "action" to "open_dialog",
                        "item" to "terms_and_conditions"
                    )
                )
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.resources.getString(R.string.terms_and_conditions_link)))
                intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
                context.startActivity(intent)
            }
        }
    }

    private inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        init {
            val github: ImageView = itemView.findViewById(R.id.about_dashboard_github)
            val title: TextView = itemView.findViewById(R.id.about_dashboard_title)
            val licenses: TextView = itemView.findViewById(R.id.about_dashboard_licenses)
            val contributors: TextView = itemView.findViewById(R.id.about_dashboard_contributors)
            val translator: TextView = itemView.findViewById(R.id.about_dashboard_translator)

            val card: MaterialCardView? = itemView.findViewById(R.id.card)
            if (card != null) {
                if (CandyBarApplication.getConfiguration().aboutStyle == CandyBarApplication.Style.PORTRAIT_FLAT_LANDSCAPE_FLAT) {
                    val params = card.layoutParams as? StaggeredGridLayoutManager.LayoutParams
                    if (params != null) {
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
                    val params = card.layoutParams as StaggeredGridLayoutManager.LayoutParams
                    params.setMargins(marginLeft, marginTop, marginRight, marginBottom)
                }

                if (!Preferences.get(context).isCardShadowEnabled) {
                    card.cardElevation = 0f
                }
            }

            var color = ColorHelper.getAttributeColor(context, android.R.attr.textColorPrimary)
            title.setCompoundDrawablesWithIntrinsicBounds(
                DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_dashboard, color),
                null, null, null
            )
            title.append(" v${BuildConfig.VERSION_NAME}")

            color = ConfigurationHelper.getSocialIconColor(
                context,
                CandyBarApplication.getConfiguration().socialIconColor
            )
            github.setImageDrawable(DrawableHelper.getTintedDrawable(context, R.drawable.ic_toolbar_github, color))

            github.setOnClickListener(this)
            licenses.setOnClickListener(this)
            contributors.setOnClickListener(this)
            translator.setOnClickListener(this)
        }

        override fun onClick(view: View) {
            val id = view.id
            if (id == R.id.about_dashboard_licenses) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    hashMapOf(
                        "section" to "about",
                        "action" to "open_dialog",
                        "item" to "licenses"
                    )
                )
                LicensesFragment.showLicensesDialog((context as AppCompatActivity).supportFragmentManager)
                return
            }

            if (id == R.id.about_dashboard_contributors) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    hashMapOf(
                        "section" to "about",
                        "action" to "open_dialog",
                        "item" to "contributors"
                    )
                )
                CreditsFragment.showCreditsDialog(
                    (context as AppCompatActivity).supportFragmentManager,
                    CreditsFragment.TYPE_DASHBOARD_CONTRIBUTORS
                )
                return
            }

            if (id == R.id.about_dashboard_translator) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    hashMapOf(
                        "section" to "about",
                        "action" to "open_dialog",
                        "item" to "translators"
                    )
                )
                CreditsFragment.showCreditsDialog(
                    (context as AppCompatActivity).supportFragmentManager,
                    CreditsFragment.TYPE_DASHBOARD_TRANSLATOR
                )
                return
            }

            var intent: Intent? = null
            if (id == R.id.about_dashboard_github) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    hashMapOf(
                        "section" to "about",
                        "action" to "open_dialog",
                        "item" to "github"
                    )
                )
                intent = Intent(Intent.ACTION_VIEW, Uri.parse(context.resources.getString(R.string.about_dashboard_github_url)))
            }

            try {
                if (intent != null) context.startActivity(intent)
            } catch (e: Exception) {
                LogUtil.e(Log.getStackTraceString(e))
            }
        }
    }

    private inner class ShadowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            if (!Preferences.get(context).isCardShadowEnabled) {
                val shadow: View? = itemView.findViewById(R.id.shadow)
                shadow?.visibility = View.GONE
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_EXTRA_INFO = 1
        private const val TYPE_FOOTER = 2
        private const val TYPE_BOTTOM_SHADOW = 3
    }
}
