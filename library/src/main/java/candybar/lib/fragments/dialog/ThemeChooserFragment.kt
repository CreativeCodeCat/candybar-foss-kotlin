package candybar.lib.fragments.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.adapters.dialog.ThemeAdapter
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.Theme
import candybar.lib.preferences.Preferences
import com.afollestad.materialdialogs.MaterialDialog
import java.util.Arrays

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
class ThemeChooserFragment : DialogFragment() {

    private var mChosenTheme: Theme? = null
    private var mCurrentTheme: Theme? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(requireActivity())
            .customView(R.layout.fragment_languages, false)
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .title(R.string.pref_theme_header)
            .negativeText(R.string.close)
            .build()
        dialog.show()

        val listView = dialog.findViewById(R.id.listview) as ListView
        mCurrentTheme = Preferences.get(requireActivity()).theme
        mChosenTheme = mCurrentTheme

        listView.adapter = ThemeAdapter(requireActivity(), Arrays.asList(*Theme.values()), mCurrentTheme!!.ordinal)

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (mChosenTheme != mCurrentTheme) {
            Preferences.get(requireActivity()).theme = mChosenTheme!!
            requireActivity().recreate()
        }
        super.onDismiss(dialog)
    }

    fun setTheme(theme: Theme) {
        mChosenTheme = theme
        dismiss()
    }

    companion object {
        const val TAG = "candybar.dialog.theme"

        private fun newInstance(): ThemeChooserFragment {
            return ThemeChooserFragment()
        }

        @JvmStatic
        fun showThemeChooser(fm: FragmentManager) {
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
