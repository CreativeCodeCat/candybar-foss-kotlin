package candybar.lib.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import candybar.lib.R
import candybar.lib.applications.CandyBarApplication
import candybar.lib.items.FAQs
import candybar.lib.preferences.Preferences
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
class FAQsAdapter(private val context: Context, faqs: List<FAQs>) :
    RecyclerView.Adapter<FAQsAdapter.ViewHolder>() {

    private var faqs: MutableList<FAQs> = faqs.toMutableList()
    private val faqsAll: List<FAQs> = ArrayList(faqs)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layout = if (viewType == TYPE_CONTENT) {
            R.layout.fragment_faqs_item_list
        } else {
            R.layout.fragment_settings_item_footer
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return ViewHolder(view, viewType)
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (holder.holderId == TYPE_CONTENT) {
            holder.divider?.visibility = View.VISIBLE
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder.holderId == TYPE_CONTENT) {
            holder.question?.text = faqs[position].question
            holder.answer?.text = faqs[position].answer

            if (position == faqs.size - 1) {
                holder.divider?.visibility = View.GONE
            }
        }
    }

    override fun getItemCount(): Int = faqs.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == itemCount - 1) TYPE_FOOTER else TYPE_CONTENT
    }

    inner class ViewHolder(itemView: View, val holderId: Int) : RecyclerView.ViewHolder(itemView) {
        var question: TextView? = null
        var answer: TextView? = null
        var divider: View? = null

        init {
            if (holderId == TYPE_CONTENT) {
                question = itemView.findViewById(R.id.question)
                answer = itemView.findViewById(R.id.answer)
                divider = itemView.findViewById(R.id.divider)
            } else if (holderId == TYPE_FOOTER) {
                if (!Preferences.get(context).isCardShadowEnabled) {
                    itemView.findViewById<View>(R.id.shadow)?.visibility = View.GONE
                }
            }
        }
    }

    fun search(string: String) {
        val query = string.lowercase(Locale.getDefault()).trim { it <= ' ' }
        faqs.clear()
        if (query.isEmpty()) {
            faqs.addAll(faqsAll)
        } else {
            for (faq in faqsAll) {
                val question = faq.question.lowercase(Locale.getDefault())
                val answer = faq.answer.lowercase(Locale.getDefault())
                if (question.contains(query) || answer.contains(query)) {
                    faqs.add(faq)
                }
            }
        }

        val found = if (faqsCount == 0) "no" else "yes"
        val params = hashMapOf<String, Any>(
            "section" to "faq",
            "action" to "search",
            "found" to found,
            "query" to query
        )
        if (faqsCount > 0) {
            params["num_results"] = faqsCount
        }

        CandyBarApplication.getConfiguration().analyticsHandler?.logEvent("type", params)
        notifyDataSetChanged()
    }

    val faqsCount: Int
        get() = faqs.size

    companion object {
        private const val TYPE_CONTENT = 0
        private const val TYPE_FOOTER = 1
    }
}
