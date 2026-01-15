package candybar.lib.fragments.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.adapters.dialog.LicensesAdapter
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.License
import candybar.lib.utils.AsyncTaskBase
import com.afollestad.materialdialogs.MaterialDialog
import com.danimahardhika.android.helpers.core.utils.LogUtil
import org.xmlpull.v1.XmlPullParser

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
class LicensesFragment : DialogFragment() {

    private var mListView: ListView? = null
    private var mAsyncTask: AsyncTaskBase? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(requireActivity())
            .customView(R.layout.fragment_licenses, false)
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .title(R.string.about_open_source_licenses)
            .negativeText(R.string.close)
            .build()
        dialog.show()

        mListView = dialog.findViewById(R.id.licenses_list) as ListView

        mAsyncTask = LicensesLoader().executeOnThreadPool()

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (mAsyncTask != null) {
            mAsyncTask?.cancel(true)
        }
        super.onDismiss(dialog)
    }

    private inner class LicensesLoader : AsyncTaskBase() {

        private var licenses: MutableList<License>? = null

        override fun preRun() {
            licenses = ArrayList()
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)

                    val xpp = requireActivity().resources.getXml(R.xml.dashboard_licenses)
                    var licenseName = ""
                    var licenseText = ""

                    while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                        when (xpp.eventType) {
                            XmlPullParser.START_TAG -> if (xpp.name == "license") {
                                licenseName = xpp.getAttributeValue(null, "name")
                            }

                            XmlPullParser.TEXT -> {
                                val parts = xpp.text.split("\n".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                                for (part in parts) {
                                    licenseText += part.trim { it <= ' ' } + "\n"
                                }
                                licenseText = licenseText.trim { it <= ' ' }
                                licenseText = licenseText.replace("(.)\\n(.)".toRegex(), "$1 $2")
                            }

                            XmlPullParser.END_TAG -> if (xpp.name == "license") {
                                licenses!!.add(License(licenseName, licenseText))
                                licenseText = ""
                                licenseName = licenseText
                            }
                        }
                        xpp.next()
                    }
                    return true
                } catch (e: Exception) {
                    LogUtil.e(Log.getStackTraceString(e))
                    return false
                }
            }
            return false
        }

        override fun postRun(ok: Boolean) {
            if (activity == null) return
            if (requireActivity().isFinishing) return

            mAsyncTask = null
            if (ok) {
                mListView!!.adapter = LicensesAdapter(requireActivity(), licenses!!)
            } else {
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "candybar.dialog.licenses"

        private fun newInstance(): LicensesFragment {
            return LicensesFragment()
        }

        @JvmStatic
        fun showLicensesDialog(fm: FragmentManager) {
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
