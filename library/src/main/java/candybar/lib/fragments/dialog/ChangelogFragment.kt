package candybar.lib.fragments.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.adapters.dialog.ChangelogAdapter
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.utils.listeners.HomeListener
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
class ChangelogFragment : DialogFragment {

    private var mOnPositive: Runnable? = null

    // Default constructor needed for fragment recreation
    constructor() : super()

    constructor(onPositive: Runnable?) : super() {
        mOnPositive = onPositive
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialDialog.Builder(requireActivity())
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .customView(R.layout.fragment_changelog, false)
            .positiveText(R.string.close)

        if (mOnPositive != null) {
            builder.onPositive { _, _ -> mOnPositive!!.run() }
        }

        val dialog = builder.build()
        dialog.show()

        val changelogList = dialog.findViewById(R.id.changelog_list) as ListView
        val changelogDate = dialog.findViewById(R.id.changelog_date) as TextView
        val changelogVersion = dialog.findViewById(R.id.changelog_version) as TextView

        val activity = requireActivity()
        try {
            val version = activity.packageManager.getPackageInfo(
                activity.packageName, 0
            ).versionName
            if (version != null && version.isNotEmpty()) {
                changelogVersion.text = activity.resources.getString(
                    R.string.changelog_version
                )
                changelogVersion.append(" $version")
            }
        } catch (ignored: Exception) {
        }

        val date = activity.resources.getString(R.string.changelog_date)
        if (date.isNotEmpty()) changelogDate.text = date
        else changelogDate.visibility = View.GONE

        val changelog = activity.resources.getStringArray(R.array.changelog)
        changelogList.adapter = ChangelogAdapter(requireActivity(), changelog)

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)

        val fm = requireActivity().supportFragmentManager
        val fragment = fm.findFragmentByTag("home")
        if (fragment != null) {
            val listener = fragment as HomeListener?
            listener?.onHomeIntroInit()
        }
    }

    companion object {
        const val TAG = "candybar.dialog.changelog"

        private fun newInstance(onPositive: Runnable?): ChangelogFragment {
            return ChangelogFragment(onPositive)
        }

        @JvmStatic
        fun showChangelog(fm: FragmentManager, onPositive: Runnable?) {
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }

            try {
                val dialog = newInstance(onPositive)
                dialog.show(ft, TAG)
            } catch (ignored: IllegalArgumentException) {
            } catch (ignored: IllegalStateException) {
            }
        }
    }
}
