package candybar.lib.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.fragments.SettingsFragment
import candybar.lib.fragments.dialog.ChangelogFragment
import candybar.lib.fragments.dialog.LanguagesFragment
import candybar.lib.fragments.dialog.ThemeChooserFragment
import candybar.lib.helpers.ReportBugsHelper
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.Setting
import candybar.lib.preferences.Preferences
import candybar.lib.tasks.IconRequestTask
import com.afollestad.materialdialogs.MaterialDialog
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import com.danimahardhika.android.helpers.core.FileHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.DecimalFormat

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
class SettingsAdapter(
    private val mContext: Context,
    private val mSettings: List<Setting>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CONTENT) {
            val view = LayoutInflater.from(mContext).inflate(R.layout.fragment_settings_item_list, parent, false)
            ContentViewHolder(view)
        } else {
            val view = LayoutInflater.from(mContext).inflate(R.layout.fragment_settings_item_footer, parent, false)
            FooterViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ContentViewHolder) {
            val setting = mSettings[position]

            if (setting.title.isEmpty()) {
                holder.title.visibility = View.GONE
                holder.divider.visibility = View.GONE
                holder.container.visibility = View.VISIBLE

                holder.subtitle.text = setting.subtitle

                if (setting.content.isEmpty()) {
                    holder.content.visibility = View.GONE
                } else {
                    holder.content.text = setting.content
                    holder.content.visibility = View.VISIBLE
                }

                if (setting.footer.isEmpty()) {
                    holder.footer.visibility = View.GONE
                } else {
                    holder.footer.text = setting.footer
                    holder.footer.visibility = View.VISIBLE
                }
            } else {
                holder.container.visibility = View.GONE
                holder.title.visibility = View.VISIBLE
                holder.title.text = setting.title

                if (position > 0) {
                    holder.divider.visibility = View.VISIBLE
                } else {
                    holder.divider.visibility = View.GONE
                }

                if (setting.icon != -1) {
                    val color = ColorHelper.getAttributeColor(mContext, android.R.attr.textColorPrimary)
                    holder.title.setCompoundDrawablesWithIntrinsicBounds(
                        DrawableHelper.getTintedDrawable(mContext, setting.icon, color), null, null, null
                    )
                }
            }

            if (setting.type == Setting.Type.MATERIAL_YOU || setting.type == Setting.Type.NOTIFICATIONS) {
                holder.materialSwitch.visibility = View.VISIBLE
                holder.container.isClickable = false
                val pad = holder.container.paddingLeft
                holder.container.setPadding(pad, 0, pad, 0)
            } else {
                holder.materialSwitch.visibility = View.GONE
                holder.container.isClickable = true
            }

            if (setting.type == Setting.Type.MATERIAL_YOU) {
                holder.materialSwitch.isChecked = Preferences.get(mContext).isMaterialYou
            }

            if (setting.type == Setting.Type.NOTIFICATIONS) {
                val isPermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.O || NotificationManagerCompat.from(mContext).areNotificationsEnabled()
                holder.materialSwitch.isChecked = Preferences.get(mContext).isNotificationsEnabled && isPermissionGranted
                val pad = holder.container.paddingLeft
                holder.container.setPadding(pad, pad, pad, 0)
            }
        }
    }

    override fun getItemCount(): Int {
        return mSettings.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) TYPE_FOOTER else TYPE_CONTENT
    }

    private inner class ContentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {
        val title: TextView = itemView.findViewById(R.id.title)
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        val content: TextView = itemView.findViewById(R.id.content)
        val footer: TextView = itemView.findViewById(R.id.footer)
        val container: LinearLayout = itemView.findViewById(R.id.container)
        val divider: View = itemView.findViewById(R.id.divider)
        val materialSwitch: MaterialSwitch = itemView.findViewById(R.id.switch_key)

        init {
            container.setOnClickListener(this)
            materialSwitch.setOnCheckedChangeListener { _, isChecked ->
                val position = bindingAdapterPosition
                if (position in mSettings.indices) {
                    when (mSettings[position].type) {
                        Setting.Type.MATERIAL_YOU -> {
                            if (isChecked != Preferences.get(mContext).isMaterialYou) {
                                Preferences.get(mContext).isMaterialYou = isChecked
                                (mContext as? Activity)?.recreate()
                            }
                        }

                        Setting.Type.NOTIFICATIONS -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !NotificationManagerCompat.from(mContext).areNotificationsEnabled()) {
                                materialSwitch.isChecked = false
                                val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, mContext.packageName)
                                mContext.startActivity(settingsIntent)
                                return@setOnCheckedChangeListener
                            }
                            if (isChecked != Preferences.get(mContext).isNotificationsEnabled) {
                                Preferences.get(mContext).isNotificationsEnabled = isChecked
                                CandyBarApplication.getConfiguration().notificationHandler?.setMode(isChecked)
                            }
                        }

                        else -> {}
                    }
                }
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun onClick(view: View) {
            if (view.id == R.id.container) {
                val position = bindingAdapterPosition
                if (position !in mSettings.indices) return

                val setting = mSettings[position]
                when (setting.type) {
                    Setting.Type.CACHE -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "settings",
                                "action" to "open_dialog",
                                "item" to "clear_cache"
                            )
                        )
                        MaterialDialog.Builder(mContext)
                            .typeface(TypefaceHelper.getMedium(mContext), TypefaceHelper.getRegular(mContext))
                            .content(R.string.pref_data_cache_clear_dialog)
                            .positiveText(R.string.clear)
                            .negativeText(android.R.string.cancel)
                            .onPositive { _, _ ->
                                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                                    "click",
                                    hashMapOf(
                                        "section" to "settings",
                                        "action" to "confirm",
                                        "item" to "clear_cache"
                                    )
                                )
                                try {
                                    val cache = mContext.cacheDir
                                    FileHelper.clearDirectory(cache)

                                    val size = FileHelper.getDirectorySize(cache).toDouble() / FileHelper.MB
                                    val formatter = DecimalFormat("#0.00")

                                    setting.footer = mContext.resources.getString(
                                        R.string.pref_data_cache_size, formatter.format(size) + " MB"
                                    )
                                    notifyItemChanged(position)

                                    Toast.makeText(mContext, R.string.pref_data_cache_cleared, Toast.LENGTH_LONG).show()
                                } catch (e: Exception) {
                                    LogUtil.e(Log.getStackTraceString(e))
                                }
                            }
                            .onNegative { _, _ ->
                                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                                    "click",
                                    hashMapOf(
                                        "section" to "settings",
                                        "action" to "cancel",
                                        "item" to "clear_cache"
                                    )
                                )
                            }
                            .show()
                    }

                    Setting.Type.ICON_REQUEST -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "settings",
                                "action" to "open_dialog",
                                "item" to "clear_icon_request_data"
                            )
                        )
                        MaterialDialog.Builder(mContext)
                            .typeface(TypefaceHelper.getMedium(mContext), TypefaceHelper.getRegular(mContext))
                            .content(R.string.pref_data_request_clear_dialog)
                            .positiveText(R.string.clear)
                            .negativeText(android.R.string.cancel)
                            .onPositive { _, _ ->
                                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                                    "click",
                                    hashMapOf(
                                        "section" to "settings",
                                        "action" to "confirm",
                                        "item" to "clear_icon_request_data"
                                    )
                                )
                                Database.get(mContext).deleteIconRequestData()
                                CandyBarMainActivity.sMissedApps = null
                                IconRequestTask(mContext).executeOnThreadPool()

                                Toast.makeText(mContext, R.string.pref_data_request_cleared, Toast.LENGTH_LONG).show()
                            }
                            .onNegative { _, _ ->
                                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                                    "click",
                                    hashMapOf(
                                        "section" to "settings",
                                        "action" to "cancel",
                                        "item" to "clear_icon_request_data"
                                    )
                                )
                            }
                            .show()
                    }

                    Setting.Type.RESTORE -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "settings",
                                "item" to "restore_purchase_data",
                                "action" to "confirm_without_dialog"
                            )
                        )
                        // logic remains same as Java (empty try-catch)
                    }

                    Setting.Type.PREMIUM_REQUEST -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "settings",
                                "item" to "rebuild_premium_request",
                                "action" to "confirm_without_dialog"
                            )
                        )
                        val fm = (mContext as? AppCompatActivity)?.supportFragmentManager ?: return
                        val fragment = fm.findFragmentByTag("settings") as? SettingsFragment ?: return
                        fragment.rebuildPremiumRequest()
                    }

                    Setting.Type.THEME -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "settings",
                                "item" to "change_theme",
                                "action" to "open_dialog"
                            )
                        )
                        (mContext as? AppCompatActivity)?.supportFragmentManager?.let {
                            ThemeChooserFragment.showThemeChooser(it)
                        }
                    }

                    Setting.Type.LANGUAGE -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "settings",
                                "item" to "change_language",
                                "action" to "open_dialog"
                            )
                        )
                        (mContext as? AppCompatActivity)?.supportFragmentManager?.let {
                            LanguagesFragment.showLanguageChooser(it)
                        }
                    }

                    Setting.Type.REPORT_BUGS -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "settings",
                                "item" to "report_bugs",
                                "action" to "open_dialog"
                            )
                        )
                        ReportBugsHelper.prepareReportBugs(mContext)
                    }

                    Setting.Type.CHANGELOG -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "settings",
                                "item" to "changelog",
                                "action" to "open_dialog"
                            )
                        )
                        (mContext as? AppCompatActivity)?.supportFragmentManager?.let {
                            ChangelogFragment.showChangelog(it) {}
                        }
                    }

                    Setting.Type.RESET_TUTORIAL -> {
                        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                            "click",
                            hashMapOf(
                                "section" to "settings",
                                "item" to "reset_tutorial",
                                "action" to "confirm_without_dialog"
                            )
                        )
                        val prefs = Preferences.get(mContext)
                        prefs.isTimeToShowHomeIntro = true
                        prefs.isTimeToShowIconsIntro = true
                        prefs.isTimeToShowRequestIntro = true
                        prefs.isTimeToShowWallpapersIntro = true
                        prefs.isTimeToShowWallpaperPreviewIntro = true

                        Toast.makeText(mContext, R.string.pref_others_reset_tutorial_reset, Toast.LENGTH_LONG).show()
                    }

                    else -> {}
                }
            }
        }
    }

    private inner class FooterViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            if (!Preferences.get(mContext).isCardShadowEnabled) {
                itemView.findViewById<View>(R.id.shadow).visibility = View.GONE
            }
        }
    }

    companion object {
        private const val TYPE_CONTENT = 0
        private const val TYPE_FOOTER = 1
    }
}
