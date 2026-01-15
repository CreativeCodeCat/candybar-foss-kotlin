package candybar.lib.fragments.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.databases.Database
import candybar.lib.fragments.IconsFragment
import candybar.lib.helpers.TypefaceHelper
import com.afollestad.materialdialogs.MaterialDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.danimahardhika.android.helpers.core.ColorHelper
import com.danimahardhika.android.helpers.core.DrawableHelper
import java.util.concurrent.atomic.AtomicBoolean

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
class IconPreviewFragment : DialogFragment() {

    private var mIconTitle: String? = null
    private var mDrawableName: String? = null
    private var mIconId = 0

    private var prevIsBookmarked = false
    private var currentIsBookmarked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (requireArguments().containsKey(TITLE)) {
            mIconTitle = requireArguments().getString(TITLE)
            mDrawableName = requireArguments().getString(DRAWABLE_NAME)
            mIconId = requireArguments().getInt(ID)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(requireActivity())
            .customView(R.layout.fragment_icon_preview, false)
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .positiveText(R.string.close)
            .build()

        dialog.show()

        if (savedInstanceState != null) {
            mIconTitle = savedInstanceState.getString(TITLE)
            mDrawableName = savedInstanceState.getString(DRAWABLE_NAME)
            mIconId = savedInstanceState.getInt(ID)
        }

        val name = dialog.findViewById(R.id.name) as TextView
        val icon = dialog.findViewById(R.id.icon) as ImageView
        val bookmark = dialog.findViewById(R.id.bookmark_button) as ImageView

        name.text = mIconTitle

        Glide.with(this)
            .load("drawable://$mIconId")
            .transition(DrawableTransitionOptions.withCrossFade(300))
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(icon)

        val params = HashMap<String, Any>()
        params["section"] = "icon_preview"
        params["action"] = "open_dialog"
        params["item"] = mDrawableName ?: ""

        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "click",
            params
        )

        if (mDrawableName == null) {
            bookmark.visibility = View.INVISIBLE
        } else {
            val isBookmarked = AtomicBoolean(
                Database.get(requireActivity()).isIconBookmarked(mDrawableName!!)
            )
            currentIsBookmarked = isBookmarked.get()
            prevIsBookmarked = currentIsBookmarked

            val updateBookmark = Runnable {
                @DrawableRes val drawableRes: Int
                @AttrRes val colorAttr: Int
                if (isBookmarked.get()) {
                    drawableRes = R.drawable.ic_bookmark_filled
                    colorAttr = com.google.android.material.R.attr.colorSecondary
                } else {
                    drawableRes = R.drawable.ic_bookmark
                    colorAttr = android.R.attr.textColorSecondary
                }
                bookmark.setImageDrawable(
                    DrawableHelper.getTintedDrawable(
                        requireActivity(),
                        drawableRes,
                        ColorHelper.getAttributeColor(requireActivity(), colorAttr)
                    )
                )
            }

            updateBookmark.run()

            bookmark.setOnClickListener {
                if (isBookmarked.get()) {
                    val params = HashMap<String, Any>()
                    params["section"] = "icon_preview"
                    params["action"] = "delete_bookmark"
                    params["item"] = mDrawableName!!
                    CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                        "click",
                        params
                    )
                    Database.get(requireActivity()).deleteBookmarkedIcon(mDrawableName!!)
                } else {
                    val params = HashMap<String, Any>()
                    params["section"] = "icon_preview"
                    params["action"] = "add_bookmark"
                    params["item"] = mDrawableName!!
                    CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                        "click",
                        params
                    )
                    Database.get(requireActivity()).addBookmarkedIcon(mDrawableName!!, mIconTitle!!)
                }
                isBookmarked.set(!isBookmarked.get())
                updateBookmark.run()
                currentIsBookmarked = isBookmarked.get()
            }
        }

        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(TITLE, mIconTitle)
        outState.putInt(ID, mIconId)
        if (mDrawableName != null) {
            outState.putString(DRAWABLE_NAME, mDrawableName)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (prevIsBookmarked != currentIsBookmarked) {
            try {
                IconsFragment.reloadBookmarks()
            } catch (exception: IllegalStateException) {
                CandyBarApplication.getConfiguration().analyticsHandler?.logException(exception)
            }
        }
    }

    companion object {
        const val TITLE = "title"
        const val DRAWABLE_NAME = "drawable_name"
        const val ID = "id"

        const val TAG = "candybar.dialog.icon.preview"

        private fun newInstance(
            title: String,
            id: Int,
            drawableName: String?
        ): IconPreviewFragment {
            val fragment = IconPreviewFragment()
            val bundle = Bundle()
            bundle.putString(TITLE, title)
            bundle.putString(DRAWABLE_NAME, drawableName)
            bundle.putInt(ID, id)
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun showIconPreview(
            fm: FragmentManager,
            title: String,
            id: Int,
            drawableName: String?
        ) {
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }

            try {
                val dialog = newInstance(title, id, drawableName)
                dialog.show(ft, TAG)
            } catch (ignored: IllegalArgumentException) {
            } catch (ignored: IllegalStateException) {
            }
        }
    }
}
