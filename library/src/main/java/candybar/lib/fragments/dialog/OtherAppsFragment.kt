package candybar.lib.fragments.dialog

import android.app.Dialog
import android.os.Bundle
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.adapters.dialog.OtherAppsAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.TypefaceHelper
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
class OtherAppsFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(requireActivity())
            .customView(R.layout.fragment_other_apps, false)
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .title(R.string.home_more_apps_header)
            .positiveText(R.string.close)
            .build()
        dialog.show()

        val listView = dialog.findViewById(R.id.listview) as ListView
        val otherApps = CandyBarApplication.getConfiguration().otherApps
        if (otherApps != null) {
            listView.adapter = OtherAppsAdapter(requireActivity(), otherApps)
        } else {
            dismiss()
        }

        return dialog
    }

    companion object {
        const val TAG = "candybar.dialog.otherapps"

        private fun newInstance(): OtherAppsFragment {
            return OtherAppsFragment()
        }

        @JvmStatic
        fun showOtherAppsDialog(fm: FragmentManager) {
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }

            try {
                val dialog = newInstance()
                dialog.show(ft, TAG)
            } catch (ignored: IllegalStateException) {
            } catch (ignored: IllegalArgumentException) {
            }
        }
    }
}
