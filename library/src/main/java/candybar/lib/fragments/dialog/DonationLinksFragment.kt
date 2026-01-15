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

class DonationLinksFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = MaterialDialog.Builder(requireActivity())
            .customView(R.layout.fragment_other_apps, false)
            .typeface(
                TypefaceHelper.getMedium(requireActivity()),
                TypefaceHelper.getRegular(requireActivity())
            )
            .title(R.string.donate)
            .positiveText(R.string.close)
            .build()
        dialog.show()

        val listView = dialog.findViewById(R.id.listview) as ListView
        val donationLinks =
            CandyBarApplication.getConfiguration().donationLinks
        if (donationLinks != null) {
            listView.adapter = OtherAppsAdapter(requireActivity(), donationLinks)
        } else {
            dismiss()
        }

        return dialog
    }

    companion object {
        const val TAG = "candybar.dialog.donationlinks"

        private fun newInstance(): DonationLinksFragment {
            return DonationLinksFragment()
        }

        @JvmStatic
        fun showDonationLinksDialog(fm: FragmentManager) {
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
