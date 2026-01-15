package candybar.lib.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.adapters.FAQsAdapter
import candybar.lib.applications.CandyBarApplication
import candybar.lib.helpers.ViewHelper.setFastScrollColor
import candybar.lib.items.FAQs
import candybar.lib.preferences.Preferences
import candybar.lib.utils.AsyncTaskBase
import com.danimahardhika.android.helpers.core.SoftKeyboardHelper
import com.danimahardhika.android.helpers.core.utils.LogUtil
import com.pluscubed.recyclerfastscroll.RecyclerFastScroller

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
class FAQsFragment : Fragment() {

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mSearchResult: TextView
    private lateinit var mFastScroll: RecyclerFastScroller

    private var mAdapter: FAQsAdapter? = null
    private var mAsyncTask: AsyncTaskBase? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_faqs, container, false)
        mRecyclerView = view.findViewById(R.id.faqs_list)
        mSearchResult = view.findViewById(R.id.search_result)
        mFastScroll = view.findViewById(R.id.fastscroll)

        if (!Preferences.get(requireActivity()).isToolbarShadowEnabled) {
            val shadow = view.findViewById<View>(R.id.shadow)
            shadow?.visibility = View.GONE
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val params = HashMap<String, Any>()
        params["section"] = "faq"
        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent(
            "view",
            params
        )

        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutManager = LinearLayoutManager(activity)

        setFastScrollColor(mFastScroll)
        mFastScroll.attachRecyclerView(mRecyclerView)

        mAsyncTask = FAQsLoader().executeOnThreadPool()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_search, menu)
        val search = menu.findItem(R.id.menu_search)

        val searchView = search.actionView!!
        val searchInput = searchView.findViewById<EditText>(R.id.search_input)
        val clearQueryButton = searchView.findViewById<View>(R.id.clear_query_button)

        searchInput.hint = requireActivity().resources.getString(R.string.search_faqs)

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {}

            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
                val query = charSequence.toString()
                filterSearch(query)
                clearQueryButton.visibility = if (query == "") View.GONE else View.VISIBLE
            }
        })

        clearQueryButton.setOnClickListener { searchInput.setText("") }

        search.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(menuItem: MenuItem): Boolean {
                searchInput.requestFocus()

                Handler(Looper.getMainLooper()).postDelayed({
                    if (activity != null) {
                        SoftKeyboardHelper.openKeyboard(requireActivity())
                    }
                }, 1000)

                return true
            }

            override fun onMenuItemActionCollapse(menuItem: MenuItem): Boolean {
                searchInput.setText("")
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onDestroy() {
        if (mAsyncTask != null) {
            mAsyncTask!!.cancel(true)
        }
        setHasOptionsMenu(false)
        super.onDestroy()
    }

    @SuppressLint("StringFormatInvalid")
    private fun filterSearch(query: String) {
        try {
            mAdapter?.search(query)
            if (mAdapter?.faqsCount == 0) {
                val text = requireActivity().resources.getString(R.string.search_noresult, query)
                mSearchResult.text = text
                mSearchResult.visibility = View.VISIBLE
            } else mSearchResult.visibility = View.GONE
        } catch (e: Exception) {
            LogUtil.e(Log.getStackTraceString(e))
        }
    }

    private inner class FAQsLoader : AsyncTaskBase() {

        private var faqs: MutableList<FAQs> = ArrayList()
        private var questions: Array<String> = emptyArray()
        private var answers: Array<String> = emptyArray()

        override fun preRun() {
            faqs = ArrayList()
            questions = resources.getStringArray(R.array.questions)
            answers = resources.getStringArray(R.array.answers)
        }

        override fun run(): Boolean {
            if (!isCancelled) {
                try {
                    Thread.sleep(1)
                    for (i in questions.indices) {
                        if (i < answers.size) {
                            val faq = FAQs(questions[i], answers[i])
                            faqs.add(faq)
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
                setHasOptionsMenu(true)
                mAdapter = FAQsAdapter(requireActivity(), faqs)
                mRecyclerView.adapter = mAdapter
            }
        }
    }
}
