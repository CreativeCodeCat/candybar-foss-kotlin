package candybar.lib.fragments.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.adapters.dialog.LanguagesAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.LocaleHelper
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.Language
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import com.afollestad.materialdialogs.MaterialDialog
import com.danimahardhika.android.helpers.core.utils.LogUtil
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
class LanguagesFragment : DialogFragment() {

    private var mListView: ListView? = null
    private var mLocale: Locale? = null
    private var mAsyncTask: AsyncTaskBase? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(requireActivity())
            .customView(R.layout.fragment_languages, false)
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .title(R.string.pref_language_header)
            .negativeText(R.string.close)
            .onNegative { _, _ ->
                val params = HashMap<String, Any>()
                params["section"] = "settings"
                params["action"] = "cancel"
                params["item"] = "change_language"
                CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
                    "click",
                    params
                )
            }
            .build()
        dialog.show()

        mListView = dialog.findViewById(R.id.listview) as ListView
        mAsyncTask = LanguagesLoader().executeOnThreadPool()

        return dialog
    }

    override fun onDestroy() {
        if (mAsyncTask != null) {
            mAsyncTask?.cancel(true)
        }
        super.onDestroy()
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (mLocale != null) {
            Preferences.get(requireActivity()).setCurrentLocale(mLocale.toString())
            LocaleHelper.setLocale(requireActivity())
            requireActivity().recreate()
        }
        super.onDismiss(dialog)
    }

    fun setLanguage(locale: Locale) {
        val params = HashMap<String, Any>()
        params["section"] = "settings"
        params["action"] = "confirm"
        params["item"] = "change_language"
        params["locale"] = locale.displayName
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "click",
            params
        )
        mLocale = locale
        dismiss()
    }

    private inner class LanguagesLoader : AsyncTaskBase() {

        private var languages: List<Language>? = null
        private var index = 0

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)
                    languages = LocaleHelper.getAvailableLanguages(requireActivity())
                    val locale = Preferences.get(requireActivity()).currentLocale
                    for (i in languages!!.indices) {
                        val l = languages!![i].locale
                        if (l.toString() == locale.toString()) {
                            index = i
                            break
                        }
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
                mListView!!.adapter = LanguagesAdapter(requireActivity(), languages!!, index)
            } else {
                dismiss()
            }
        }
    }

    companion object {
        const val TAG = "candybar.dialog.languages"

        private fun newInstance(): LanguagesFragment {
            return LanguagesFragment()
        }

        @JvmStatic
        fun showLanguageChooser(fm: FragmentManager) {
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
