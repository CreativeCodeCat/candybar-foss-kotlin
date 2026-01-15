package candybar.lib.fragments.dialog

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.adapters.dialog.CreditsAdapter
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.Credit
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
class CreditsFragment : DialogFragment() {

    private var mListView: ListView? = null
    private var mAsyncTask: AsyncTaskBase? = null
    private var mType = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(requireActivity())
            .customView(R.layout.fragment_credits, false)
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .title(getTitle(mType))
            .positiveText(R.string.close)
            .build()
        dialog.show()
        mListView = dialog.findViewById(R.id.listview) as ListView
        mAsyncTask = CreditsLoader().executeOnThreadPool()

        return dialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mType = requireArguments().getInt(TYPE)
        }
    }

    override fun onDestroyView() {
        if (mAsyncTask != null) {
            mAsyncTask!!.cancel(true)
        }
        super.onDestroyView()
    }

    private fun getTitle(type: Int): String {
        if (activity == null) return ""
        return when (type) {
            TYPE_ICON_PACK_CONTRIBUTORS -> requireActivity().resources.getString(
                R.string.about_contributors_title
            )

            TYPE_DASHBOARD_CONTRIBUTORS -> requireActivity().resources.getString(
                R.string.about_dashboard_contributors
            )

            TYPE_DASHBOARD_TRANSLATOR -> requireActivity().resources.getString(
                R.string.about_dashboard_translator
            )

            else -> ""
        }
    }

    private fun getResource(type: Int): Int {
        return when (type) {
            TYPE_ICON_PACK_CONTRIBUTORS -> R.xml.contributors
            TYPE_DASHBOARD_CONTRIBUTORS -> R.xml.dashboard_contributors
            TYPE_DASHBOARD_TRANSLATOR -> R.xml.dashboard_translator
            else -> -1
        }
    }

    private inner class CreditsLoader : AsyncTaskBase() {

        private var credits: MutableList<Credit>? = null

        override fun preRun() {
            credits = ArrayList()
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)
                    val res = getResource(mType)

                    val xpp = resources.getXml(res)

                    while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
                        if (xpp.eventType == XmlPullParser.START_TAG) {
                            if (xpp.name == "contributor") {
                                val credit = Credit(
                                    xpp.getAttributeValue(null, "name"),
                                    xpp.getAttributeValue(null, "contribution"),
                                    xpp.getAttributeValue(null, "image"),
                                    xpp.getAttributeValue(null, "link")
                                )
                                credits!!.add(credit)
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
                mListView!!.adapter = CreditsAdapter(requireActivity(), credits!!)
            } else {
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "candybar.dialog.credits"
        private const val TYPE = "type"

        const val TYPE_ICON_PACK_CONTRIBUTORS = 0
        const val TYPE_DASHBOARD_CONTRIBUTORS = 1
        const val TYPE_DASHBOARD_TRANSLATOR = 2

        private fun newInstance(type: Int): CreditsFragment {
            val fragment = CreditsFragment()
            val bundle = Bundle()
            bundle.putInt(TYPE, type)
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun showCreditsDialog(fm: FragmentManager, type: Int) {
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }

            try {
                val dialog = newInstance(type)
                dialog.show(ft, TAG)
            } catch (ignored: IllegalStateException) {
            } catch (ignored: IllegalArgumentException) {
            }
        }
    }
}
