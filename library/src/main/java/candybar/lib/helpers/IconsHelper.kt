package candybar.lib.helpers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import candybar.lib.R
import candybar.lib.activities.CandyBarMainActivity
import candybar.lib.applications.CandyBarApplication
import candybar.lib.fragments.dialog.IconPreviewFragment
import candybar.lib.items.Icon
import candybar.lib.utils.CandyBarGlideModule
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.danimahardhika.android.helpers.core.FileHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
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

object IconsHelper {

    @JvmStatic
    @Throws(Exception::class)
    fun loadIcons(context: Context, sortIcons: Boolean) {
        // Load icons only if they are not loaded
        if (CandyBarMainActivity.sSections == null) {
            CandyBarMainActivity.sSections = getIconsList(context)

            CandyBarMainActivity.sSections?.let { sections ->
                for (section in sections) {
                    val icons = section.icons
                    computeTitles(context, icons)

                    if (sortIcons && context.resources.getBoolean(R.bool.enable_icons_sort)) {
                        Collections.sort(icons, Icon.TitleComparator)
                        section.icons = icons
                    }
                }

                if (CandyBarApplication.getConfiguration().isShowTabAllIcons) {
                    val icons = getTabAllIcons()
                    sections.add(
                        Icon(
                            CandyBarApplication.getConfiguration().tabAllIconsTitle, icons
                        )
                    )
                }
            }
        }
    }

    @JvmStatic
    @Throws(Exception::class)
    fun getIconsList(context: Context): MutableList<Icon> {
        val parser = context.resources.getXml(R.xml.drawable)
        var eventType = parser.eventType
        var sectionTitle = ""
        var icons = mutableListOf<Icon>()
        val sections = mutableListOf<Icon>()

        var count = 0
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "category") {
                    val title = parser.getAttributeValue(null, "title")
                    if (sectionTitle != title) {
                        if (sectionTitle.isNotEmpty() && icons.isNotEmpty()) {
                            count += icons.size
                            sections.add(Icon(sectionTitle, icons))
                        }
                    }
                    sectionTitle = title ?: ""
                    icons = mutableListOf()
                } else if (parser.name == "item") {
                    val drawableName = parser.getAttributeValue(null, "drawable")
                    val customName = parser.getAttributeValue(null, "name")
                    val id = DrawableHelper.getDrawableId(drawableName)
                    if (id > 0) {
                        icons.add(Icon(drawableName, customName, id))
                    }
                }
            }
            eventType = parser.next()
        }
        count += icons.size
        CandyBarMainActivity.sIconsCount = count
        if (!CandyBarApplication.getConfiguration().isAutomaticIconsCountEnabled &&
            CandyBarApplication.getConfiguration().customIconsCount == 0
        ) {
            CandyBarApplication.getConfiguration().setCustomIconsCount(count)
        }
        if (icons.isNotEmpty()) {
            sections.add(Icon(sectionTitle, icons))
        }
        parser.close()
        return sections
    }

    @JvmStatic
    fun getTabAllIcons(): MutableList<Icon> {
        val iconSet = mutableSetOf<Icon>()
        val categories = CandyBarApplication.getConfiguration().categoryForTabAllIcons

        if (categories != null && categories.isNotEmpty()) {
            for (category in categories) {
                CandyBarMainActivity.sSections?.let { sections ->
                    for (section in sections) {
                        if (section.title == category) {
                            iconSet.addAll(section.icons)
                            break
                        }
                    }
                }
            }
        } else {
            CandyBarMainActivity.sSections?.let { sections ->
                for (section in sections) {
                    iconSet.addAll(section.icons)
                }
            }
        }
        val icons = iconSet.toMutableList()
        Collections.sort(icons, Icon.TitleComparator)
        return icons
    }

    @JvmStatic
    fun computeTitles(context: Context, icons: List<Icon>) {
        val iconReplacer = context.resources.getBoolean(R.bool.enable_icon_name_replacer)
        for (icon in icons) {
            if (icon.title != null) {
                // Title is already computed, so continue
                continue
            }
            if (!icon.customName.isNullOrEmpty()) {
                icon.title = icon.customName
            } else {
                icon.title = replaceName(context, iconReplacer, icon.drawableName)
            }
        }
    }

    @JvmStatic
    fun replaceName(context: Context, iconReplacer: Boolean, name: String): String {
        var result = name
        if (iconReplacer) {
            val replacer = context.resources.getStringArray(R.array.icon_name_replacer)
            for (replace in replacer) {
                val strings = replace.split(",")
                if (strings.isNotEmpty()) {
                    result = result.replace(strings[0], if (strings.size > 1) strings[1] else "")
                }
            }
        }
        result = result.replace("_", " ")
        result = result.trim().replace("\\s+".toRegex(), " ")
        return capitalizeWord(result)
    }

    @JvmStatic
    fun capitalizeWord(str: String): String {
        val words = str.split("\\s".toRegex())
        val capitalizeWord = StringBuilder()
        for (w in words) {
            if (w.isNotEmpty()) {
                val first = w.substring(0, 1)
                val afterFirst = w.substring(1)
                capitalizeWord.append(first.uppercase(Locale.getDefault())).append(afterFirst).append(" ")
            }
        }
        return capitalizeWord.toString().trim()
    }

    @JvmStatic
    fun selectIcon(context: Context, action: Int, icon: Icon) {
        val params = HashMap<String, Any>()
        params["section"] = "icons"
        params["action"] = "pick_icon"
        params["item"] = icon.drawableName
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent("click", params)

        if (action == IntentHelper.ICON_PICKER && CandyBarGlideModule.isValidContextForGlide(context)) {
            Glide.with(context)
                .asBitmap()
                .load("drawable://" + icon.res)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .listener(object : RequestListener<Bitmap> {
                    fun handleResult(resource: Bitmap?) {
                        val intent = Intent()
                        intent.putExtra("icon", resource)

                        // Also add the direct icon resource ID to the intent for launchers that support it
                        @Suppress("DEPRECATION")
                        val iconRes = Intent.ShortcutIconResource.fromContext(context, icon.res)
                        @Suppress("DEPRECATION")
                        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconRes)
                        (context as AppCompatActivity).setResult(
                            if (resource != null) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                            intent
                        )
                        context.finish()
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        handleResult(null)
                        return true
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        handleResult(resource)
                        return true
                    }
                })
                .submit()
        } else if (action == IntentHelper.IMAGE_PICKER && CandyBarGlideModule.isValidContextForGlide(context)) {
            Glide.with(context)
                .asBitmap()
                .load("drawable://" + icon.res)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .listener(object : RequestListener<Bitmap> {
                    private fun handleResult(bitmap: Bitmap?) {
                        val intent = Intent()
                        if (bitmap != null) {
                            val file = File(context.cacheDir, icon.title + ".png")
                            try {
                                val outStream = FileOutputStream(file)
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                                outStream.flush()
                                outStream.close()

                                var uri = FileHelper.getUriFromFile(context, context.packageName, file)
                                if (uri == null) uri = Uri.fromFile(file)
                                intent.putExtra(Intent.EXTRA_STREAM, uri)
                                intent.data = uri
                                intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                            } catch (e: Exception) {
                                LogUtil.e(Log.getStackTraceString(e))
                            } catch (e: OutOfMemoryError) {
                                LogUtil.e(Log.getStackTraceString(e))
                            }
                            intent.putExtra("return-data", false)
                        }
                        (context as AppCompatActivity).setResult(
                            if (bitmap != null) Activity.RESULT_OK else Activity.RESULT_CANCELED,
                            intent
                        )
                        context.finish()
                    }

                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Bitmap>,
                        isFirstResource: Boolean
                    ): Boolean {
                        handleResult(null)
                        return false
                    }

                    override fun onResourceReady(
                        resource: Bitmap,
                        model: Any,
                        target: Target<Bitmap>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        handleResult(resource)
                        return true
                    }
                })
                .submit()
        } else {
            IconPreviewFragment.showIconPreview(
                (context as AppCompatActivity).supportFragmentManager,
                icon.title, icon.res, icon.drawableName
            )
        }
    }

    fun interface OnFileNameChange {
        fun call(newName: String)
    }

    @JvmStatic
    fun saveIcon(
        files: List<String>,
        directory: File,
        drawable: Drawable,
        name: String?,
        onFileNameChange: OnFileNameChange
    ): String? {
        val bitmap = DrawableHelper.toBitmap(drawable) ?: return null
        return saveBitmap(files, directory, bitmap, name, onFileNameChange)
    }

    @JvmStatic
    fun saveBitmap(
        files: List<String>,
        directory: File,
        bitmap: Bitmap,
        name: String?,
        onFileNameChange: OnFileNameChange
    ): String? {
        var fileName = (name ?: "icon") + ".png"
        var file = File(directory, fileName)
        try {
            Thread.sleep(2)

            if (files.contains(file.toString())) {
                fileName = fileName.replace(".png", "_" + System.currentTimeMillis() + ".png")
                file = File(directory, fileName)
                onFileNameChange.call(fileName)
                LogUtil.e("Duplicate File name, Renamed: $fileName")
            }

            val outStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
            outStream.flush()
            outStream.close()
            return directory.toString() + "/" + fileName
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        } catch (e: OutOfMemoryError) {
            LogUtil.e(Log.getStackTraceString(e))
        }
        return null
    }
}
