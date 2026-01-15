package candybar.lib.fragments.dialog

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import candybar.lib.R
import candybar.lib.adapters.dialog.IntentAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.fragments.RequestFragment
import candybar.lib.helpers.TypefaceHelper
import candybar.lib.items.IntentChooser
import candybar.lib.utils.AsyncTaskBase
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.danimahardhika.android.helpers.core.utils.LogUtil
import java.io.File
import java.util.Collections

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
class IntentChooserFragment : DialogFragment() {

    private var mIntentList: ListView? = null
    private var mNoApp: TextView? = null

    private var mType = 0
    private var mAdapter: IntentAdapter? = null
    private var mAsyncTask: AsyncTaskBase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mType = requireArguments().getInt(TYPE)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(requireActivity())
            .customView(R.layout.fragment_intent_chooser, false)
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .positiveText(android.R.string.cancel)
            .build()

        dialog.getActionButton(DialogAction.POSITIVE).setOnClickListener {
            if (mAdapter == null || mAdapter!!.isAsyncTaskRunning) return@setOnClickListener

            if (CandyBarApplication.sZipPath != null) {
                val file = File(CandyBarApplication.sZipPath!!)
                if (file.exists()) {
                    if (file.delete()) {
                        LogUtil.e(String.format("Intent chooser cancel: %s deleted", file.name))
                    }
                }
            }

            RequestFragment.sSelectedRequests = null
            CandyBarApplication.sRequestProperty = null
            CandyBarApplication.sZipPath = null
            dialog.dismiss()
        }
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
        isCancelable = false

        mIntentList = dialog.findViewById(R.id.intent_list) as ListView
        mNoApp = dialog.findViewById(R.id.intent_noapp) as TextView
        mAsyncTask = IntentChooserLoader().execute()

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (mAsyncTask != null) {
            mAsyncTask!!.cancel(true)
        }
        super.onDismiss(dialog)
    }

    private inner class IntentChooserLoader : AsyncTaskBase() {

        private var apps: MutableList<IntentChooser>? = null

        override fun preRun() {
            apps = ArrayList()
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)

                    val nonMailingAppSend = resources.getBoolean(R.bool.enable_non_mail_app_request)
                    val intent: Intent

                    if (!nonMailingAppSend) {
                        intent = Intent(Intent.ACTION_SENDTO)
                        intent.data = Uri.parse("mailto:")
                    } else {
                        intent = Intent(Intent.ACTION_SEND)
                        intent.type = "application/zip"
                    }

                    val resolveInfos = requireActivity().packageManager
                        .queryIntentActivities(intent, 0)
                    try {
                        Collections.sort(
                            resolveInfos, ResolveInfo.DisplayNameComparator(
                                requireActivity().packageManager
                            )
                        )
                    } catch (ignored: Exception) {
                    }

                    for (resolveInfo in resolveInfos) {
                        when (resolveInfo.activityInfo.packageName) {
                            "com.google.android.gm" -> apps!!.add(
                                IntentChooser(
                                    resolveInfo,
                                    IntentChooser.TYPE_RECOMMENDED
                                )
                            )

                            "com.google.android.apps.inbox" -> {
                                try {
                                    val componentName = ComponentName(
                                        resolveInfo.activityInfo.applicationInfo.packageName,
                                        "com.google.android.apps.bigtop.activities.MainActivity"
                                    )
                                    val inbox = Intent(Intent.ACTION_SEND)
                                    inbox.component = componentName

                                    val list =
                                        requireActivity().packageManager.queryIntentActivities(
                                            inbox, PackageManager.MATCH_DEFAULT_ONLY
                                        )
                                    if (list.isNotEmpty()) {
                                        apps!!.add(
                                            IntentChooser(
                                                resolveInfo,
                                                IntentChooser.TYPE_SUPPORTED
                                            )
                                        )
                                        break
                                    }
                                } catch (e: ActivityNotFoundException) {
                                    LogUtil.e(Log.getStackTraceString(e))
                                }

                                apps!!.add(
                                    IntentChooser(
                                        resolveInfo,
                                        IntentChooser.TYPE_NOT_SUPPORTED
                                    )
                                )
                            }

                            "com.android.fallback", "com.paypal.android.p2pmobile", "com.lonelycatgames.Xplore" -> {
                            }

                            else -> apps!!.add(
                                IntentChooser(
                                    resolveInfo,
                                    IntentChooser.TYPE_SUPPORTED
                                )
                            )
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
            if (ok && apps != null) {
                mAdapter = IntentAdapter(activity!!, apps!!, mType)
                mIntentList!!.adapter = mAdapter

                if (apps!!.isEmpty()) {
                    mNoApp!!.visibility = View.VISIBLE
                    isCancelable = true
                }
            } else {
                dismiss()
                Toast.makeText(
                    activity, R.string.intent_email_failed,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    companion object {
        const val ICON_REQUEST = 0
        const val REBUILD_ICON_REQUEST = 1

        const val TAG = "candybar.dialog.intent.chooser"
        private const val TYPE = "type"

        private fun newInstance(type: Int): IntentChooserFragment {
            val fragment = IntentChooserFragment()
            val bundle = Bundle()
            bundle.putInt(TYPE, type)
            fragment.arguments = bundle
            return fragment
        }

        @JvmStatic
        fun showIntentChooserDialog(fm: FragmentManager, type: Int) {
            val ft = fm.beginTransaction()
            val prev = fm.findFragmentByTag(TAG)
            if (prev != null) {
                ft.remove(prev)
            }

            try {
                val dialog = newInstance(type)
                dialog.show(ft, TAG)
            } catch (ignored: IllegalArgumentException) {
            } catch (ignored: IllegalStateException) {
            }
        }
    }
}
