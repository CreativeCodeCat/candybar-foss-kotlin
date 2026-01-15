package candybar.lib.fragments.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.adapters.dialog.IconShapeAdapter
import candybar.lib.fragments.IconsFragment
import candybar.lib.fragments.IconsSearchFragment
import candybar.lib.helpers.IconShapeHelper
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.preferences.Preferences
import com.afollestad.materialdialogs.MaterialDialog

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
class IconShapeChooserFragment : DialogFragment() {

    private var mShape: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(requireActivity())
            .customView(R.layout.fragment_languages, false)
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .title(R.string.icon_shape)
            .negativeText(R.string.close)
            .build()
        dialog.show()

        val listView = dialog.findViewById(R.id.listview) as ListView
        val iconShapes = IconShapeHelper.getShapes()
        mShape = Preferences.get(requireActivity()).iconShape
        val currentShape = mShape
        var currentShapeIndex = 0

        for (i in iconShapes.indices) {
            val shape = iconShapes[i].shape
            if (shape == currentShape) {
                currentShapeIndex = i
                break
            }
        }

        listView.adapter = IconShapeAdapter(requireActivity(), iconShapes, currentShapeIndex)

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        val prevShape = Preferences.get(requireActivity()).iconShape
        if (prevShape != mShape) {
            Preferences.get(requireActivity()).iconShape = mShape
            IconsFragment.reloadIcons()
            IconsSearchFragment.reloadIcons()
        }
        super.onDismiss(dialog)
    }

    fun setShape(shape: Int) {
        mShape = shape
    }

    companion object {
        const val TAG = "candybar.dialog.iconshapes"

        private fun newInstance(): IconShapeChooserFragment {
            return IconShapeChooserFragment()
        }

        @JvmStatic
        fun showIconShapeChooser(fm: FragmentManager) {
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }

            try {
                val dialog = newInstance()
                dialog.show(ft, TAG)
            } catch (ignored: IllegalArgumentException) {
            } catch (ignored: IllegalStateException) {
            }
        }
    }
}
