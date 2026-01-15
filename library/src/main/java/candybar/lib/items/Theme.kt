package candybar.lib.items

import android.content.Context
import androidx.annotation.StringRes
import candybar.lib.R

enum class Theme(@StringRes val nameStringRes: Int) {
    AUTO(R.string.theme_name_auto),
    LIGHT(R.string.theme_name_light),
    DARK(R.string.theme_name_dark);

    fun displayName(context: Context): String = context.getString(nameStringRes)
}
