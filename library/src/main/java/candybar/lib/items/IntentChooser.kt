package candybar.lib.items

import android.content.pm.ResolveInfo

data class IntentChooser(val app: ResolveInfo, val type: Int) {
    companion object {
        const val TYPE_SUPPORTED: Int = 0
        const val TYPE_RECOMMENDED: Int = 1
        const val TYPE_NOT_SUPPORTED: Int = 2
    }
}
